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
            