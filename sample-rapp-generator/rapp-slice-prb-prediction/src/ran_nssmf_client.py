import json
import logging

import requests

from sme_client import SMEClient


logger = logging.getLogger(__name__)

class RAN_NSSMF_CLIENT(object):
    
    def __init__(self):
        with open('config.json', 'r') as f:
            config = json.load(f)

        rapp_config = config.get("RAPP", {})
        self.ran_nssmf_address = rapp_config.get("ran_nssmf_address")

        # Load the SME configuration from the JSON file
        sme_config = config.get("SME", {})
        self.invoker_id = sme_config.get("invoker_id")
        self.ran_nssmf_api_name = sme_config.get("ran_nssmf_api_name")
        self.ran_nssmf_resource_name = sme_config.get("ran_nssmf_resource_name")
    
    def get_url_from_sme(self):
        sme_client = SMEClient(
            invoker_id=self.invoker_id,
            api_name=self.ran_nssmf_api_name,
            resource_name=self.ran_nssmf_resource_name
        )

        self.ran_nssmf_address = sme_client.discover_service()

        logger.info("RAN NSSMF URL: {}".format(self.ran_nssmf_address))

        if self.ran_nssmf_address is not None:
            self.address = self.ran_nssmf_address.rstrip('/')
            logger.debug(f"InfluxDB Address: {self.address}")
        else:
            logger.error("Failed to discover RAN NSSMF URL from SME.")

    def subscribe_to_file_ready_notifications(self, callback_uri: str):
        """
        Subscribes to file-ready notifications from the RAN NSSMF.

        Args:
            callback_uri (str): The URI where the RAN NSSMF should send notifications.

        Returns:
            requests.Response: The response object from the POST request.
        """
        base_url = self.ran_nssmf_address
        
        if not base_url:
            logger.error("RAN NSSMF address is not configured. Cannot subscribe.")
            return None

        # Ensure base_url does not have a trailing slash
        base_url = base_url.rstrip('/')
        subscription_url = f"{base_url}/3GPPManagement/FileDataReportingMnS/v17.0.0/subscriptions"
        
        payload = {
            "consumerReference": callback_uri
        }
        
        headers = {
            "Content-Type": "application/json"
        }
        
        logger.info(f"Subscribing to file-ready notifications at: {subscription_url}")
        logger.debug(f"Payload: {payload}")
        
        try:
            response = requests.post(subscription_url, json=payload, headers=headers, timeout=10)
            response.raise_for_status()  # Raise an exception for HTTP errors (4xx or 5xx)
            
            logger.info(f"Successfully subscribed to notifications. Status: {response.status_code}")
            if response.headers.get("Location"):
                logger.info(f"Subscription Location: {response.headers.get('Location')}")
            
            return response
            
        except requests.exceptions.HTTPError as http_err:
            logger.error(f"HTTP error occurred while subscribing: {http_err} - Response: {http_err.response.text}")
        except requests.exceptions.ConnectionError as conn_err:
            logger.error(f"Connection error occurred while subscribing: {conn_err}")
        except requests.exceptions.Timeout as timeout_err:
            logger.error(f"Timeout error occurred while subscribing: {timeout_err}")
        except requests.exceptions.RequestException as req_err:
            logger.error(f"An unexpected error occurred while subscribing: {req_err}")
            
        return None

    def get_network_slice_subnet(self, subnet_id: str):
        """
        Retrieves details of a specific Network Slice Subnet from the RAN NSSMF.

        Args:
            subnet_id (str): The unique identifier of the Network Slice Subnet.

        Returns:
            dict: A dictionary representing the NetworkSliceSubnetDTO if successful,
                  None otherwise.
        """
        base_url = self.ran_nssmf_address
        
        if not base_url:
            logger.error("RAN NSSMF address is not configured. Cannot get network slice subnet.")
            return None

        # Ensure base_url does not have a trailing slash
        base_url = base_url.rstrip('/')
        # The version v17.0.0 is used in other endpoints of the simulator
        get_subnet_url = f"{base_url}/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/{subnet_id}"
        
        headers = {
            "Accept": "application/json"
        }
        
        logger.info(f"Getting details for Network Slice Subnet ID: {subnet_id} from: {get_subnet_url}")
        
        try:
            response = requests.get(get_subnet_url, headers=headers, timeout=10)
            
            # Check for 404 Not Found specifically, as the simulator returns this for unknown IDs
            if response.status_code == 404:
                logger.warning(f"Network Slice Subnet with ID '{subnet_id}' not found. Status: {response.status_code}")
                return None
            
            response.raise_for_status()  # Raise an exception for other HTTP errors (4xx or 5xx)
            
            logger.info(f"Successfully retrieved details for Network Slice Subnet ID: {subnet_id}. Status: {response.status_code}")
            # logger.debug(f"Response Body: {response.json()}") # Uncomment for detailed debugging
            return response.json() # Return the parsed JSON response (NetworkSliceSubnetDTO)
            
        except requests.exceptions.HTTPError as http_err:
            # This will catch errors from response.raise_for_status() for non-404 codes
            logger.error(f"HTTP error occurred while getting network slice subnet '{subnet_id}': {http_err} - Response: {http_err.response.text}")
        except requests.exceptions.ConnectionError as conn_err:
            logger.error(f"Connection error occurred while getting network slice subnet '{subnet_id}': {conn_err}")
        except requests.exceptions.Timeout as timeout_err:
            logger.error(f"Timeout error occurred while getting network slice subnet '{subnet_id}': {timeout_err}")
        except requests.exceptions.RequestException as req_err:
            logger.error(f"An unexpected error occurred while getting network slice subnet '{subnet_id}': {req_err}")
        except json.JSONDecodeError as json_err:
            logger.error(f"Failed to decode JSON response for network slice subnet '{subnet_id}': {json_err} - Response text: {response.text if 'response' in locals() else 'N/A'}")
            
        return None

    def modify_network_slice_subnet(self, subnet_id: str, new_prb_dl: int):
        """
        Modifies the RRU.PrbDl value of an existing Network Slice Subnet in the RAN NSSMF.
        It first fetches the current subnet data, updates the RRU.PrbDl, and then sends the modification.

        Args:
            subnet_id (str): The unique identifier of the Network Slice Subnet to modify.
            new_prb_dl (int): The new RRU.PrbDl value to set.

        Returns:
            requests.Response: The response object from the PUT request if successful,
                              None otherwise.
        """
        base_url = self.ran_nssmf_address
        
        if not base_url:
            logger.error("RAN NSSMF address is not configured. Cannot modify network slice subnet.")
            return None

        logger.info(f"Attempting to modify Network Slice Subnet ID: {subnet_id}. Fetching current data first.")
        
        # Fetch the current network slice subnet data
        current_subnet_data = self.get_network_slice_subnet(subnet_id)
        
        if current_subnet_data is None:
            logger.error(f"Failed to retrieve current data for Network Slice Subnet ID: {subnet_id}. Cannot modify.")
            return None

        # Ensure base_url does not have a trailing slash
        base_url = base_url.rstrip('/')
        modify_subnet_url = f"{base_url}/3GPPManagement/ProvMnS/v17.0.0/NetworkSliceSubnets/{subnet_id}"
        
        # Update the RRU.PrbDl in the fetched data.
        # The path to RRU.PrbDl is based on the structure returned by get_network_slice_subnet
        # and the expected payload for PUT.
        try:
            if "attributes" not in current_subnet_data or \
               "sliceProfileList" not in current_subnet_data["attributes"] or \
               not isinstance(current_subnet_data["attributes"]["sliceProfileList"], list) or \
               len(current_subnet_data["attributes"]["sliceProfileList"]) == 0 or \
               "ransliceSubnetProfile" not in current_subnet_data["attributes"]["sliceProfileList"][0] or \
               "RRU.PrbDl" not in current_subnet_data["attributes"]["sliceProfileList"][0]["ransliceSubnetProfile"]:
                logger.error(f"Unexpected structure in current subnet data for ID: {subnet_id}. Cannot update RRU.PrbDl.")
                logger.debug(f"Current subnet data: {json.dumps(current_subnet_data, indent=2)}")
                return None

            current_subnet_data["attributes"]["sliceProfileList"][0]["ransliceSubnetProfile"]["RRU.PrbDl"] = new_prb_dl
            payload = current_subnet_data # Use the entire modified data as payload

        except (KeyError, IndexError, TypeError) as e:
            logger.error(f"Error updating RRU.PrbDl in fetched data for subnet ID '{subnet_id}': {e}")
            logger.debug(f"Current subnet data: {json.dumps(current_subnet_data, indent=2)}")
            return None
        
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        
        logger.info(f"Modifying Network Slice Subnet ID: {subnet_id} with new PRB DL: {new_prb_dl}. URL: {modify_subnet_url}")
        logger.debug(f"Payload for modification (based on fetched data): {json.dumps(payload, indent=2)}")
        
        try:
            response = requests.put(modify_subnet_url, json=payload, headers=headers, timeout=10)
            
            # Check for 404 Not Found specifically
            if response.status_code == 404:
                logger.warning(f"Network Slice Subnet with ID '{subnet_id}' not found for modification during PUT. Status: {response.status_code}")
                return None
            
            response.raise_for_status()  # Raise an exception for other HTTP errors (4xx or 5xx)
            
            logger.info(f"Successfully sent modification request for Network Slice Subnet ID: {subnet_id}. Status: {response.status_code}")
            return response
            
        except requests.exceptions.HTTPError as http_err:
            logger.error(f"HTTP error occurred while modifying network slice subnet '{subnet_id}': {http_err} - Response: {http_err.response.text}")
        except requests.exceptions.ConnectionError as conn_err:
            logger.error(f"Connection error occurred while modifying network slice subnet '{subnet_id}': {conn_err}")
        except requests.exceptions.Timeout as timeout_err:
            logger.error(f"Timeout error occurred while modifying network slice subnet '{subnet_id}': {timeout_err}")
        except requests.exceptions.RequestException as req_err:
            logger.error(f"An unexpected error occurred while modifying network slice subnet '{subnet_id}': {req_err}")
            
        return None
            