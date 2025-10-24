"""
Service Management Environment (SME) Client

This module implements a client for discovering services in an O-RAN Service Management Environment.
The SME provides service discovery capabilities that allow RAPP applications to dynamically locate
and connect to available services without hardcoding endpoint information.
"""

import requests
import logging
import json

# Configure logger for this module
logger = logging.getLogger(__name__)

class SMEClient:
    """
    Service Management Environment Client for O-RAN service discovery.
    
    This client enables RAPP applications to dynamically discover and connect to services
    in an O-RAN ecosystem by querying the SME (Service Management Environment) for
    service endpoint information.
    
    Attributes:
        invoker_id (str): Identifier for the API invoker/service consumer
        api_name (str): Name of the API to discover
        resource_name (str): Specific resource name within the API
        sme_discovery_endpoint (str): URL endpoint for SME service discovery
    """
    
    def __init__(self, invoker_id, api_name, resource_name):
        """
        Initialize the SME Client with service discovery parameters.
        
        Args:
            invoker_id (str): Unique identifier for the service consumer
            api_name (str): Name of the API service to discover
            resource_name (str): Specific resource within the API to access
            
        Raises:
            FileNotFoundError: If config.json file is not found
            json.JSONDecodeError: If config.json contains invalid JSON
            KeyError: If required SME configuration is missing
        """
        # Store service identification parameters
        self.invoker_id = invoker_id
        self.api_name = api_name
        self.resource_name = resource_name

        # Load SME configuration from config.json file
        with open('config.json', 'r') as f:
            config = json.load(f)

        # Extract SME discovery endpoint from configuration
        sme_config = config.get("SME", {})
        self.sme_discovery_endpoint = sme_config.get("sme_discovery_endpoint")

    def discover_service(self):
        """
        Discover service endpoint by querying the SME discovery service.
        
        This method constructs a query with the API invoker ID and API name,
        sends it to the SME discovery endpoint, and parses the response
        to extract the service URI.
        
        Returns:
            str: Complete HTTP URL for the discovered service, or None if discovery fails
            
        Raises:
            requests.RequestException: If network request fails (handled internally)
        """
        # Construct query parameters for service discovery
        query = f"api-invoker-id=api_invoker_id_{self.invoker_id}&api-name={self.api_name}"
        full_url = f"{self.sme_discovery_endpoint}?{query}"
        logger.info(f"Full URL for service discovery: {full_url}")

        try:
            # Make HTTP GET request to SME discovery endpoint
            response = requests.get(full_url, headers={"Content-Type": "application/json"})
            
            if response.status_code == 200:
                logger.info("Service discovery successful.")
                # Parse the JSON response to extract service URI
                return self.parse_uri(response.json())
            else:
                # Log error details for failed requests
                logger.error(f"Failed to discover service. Status code: {response.status_code}")
                logger.error(response.text)
                return None
        except requests.RequestException as e:
            # Handle network-related errors (connection timeout, DNS resolution, etc.)
            logger.error(f"Error during service discovery: {e}")
            return None

    def parse_uri(self, response):
        """
        Parse the SME service discovery response to extract the service URI.
        
        The SME response follows a nested structure containing service descriptions,
        AEF (Application Exposure Function) profiles, versions, and resources.
        This method navigates through this structure to find the specific resource
        and construct the complete service URL.
        
        Args:
            response (dict): JSON response from SME service discovery
            
        Returns:
            str: Complete HTTP URL for the service resource, or None if parsing fails
            
        Expected Response Structure:
            {
                "serviceAPIDescriptions": [
                    {
                        "aefProfiles": [
                            {
                                "versions": [
                                    {
                                        "resources": [
                                            {
                                                "resourceName": "resource_name",
                                                "uri": "/api/endpoint"
                                            }
                                        ]
                                    }
                                ],
                                "interfaceDescriptions": [
                                    {
                                        "ipv4Addr": "",
                                        "port": 0
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        """
        try:
            logger.debug("Parsing SME response to extract URI.")
            
            # Navigate through the nested response structure
            service = response["serviceAPIDescriptions"][0]  # Get first service description
            profile = service["aefProfiles"][0]              # Get first AEF profile
            version = profile["versions"][0]                 # Get first API version
            
            # Find the specific resource by name within the version's resources
            resource = next(
                (res for res in version["resources"] if res["resourceName"] == self.resource_name),
                None
            )
            uri = resource["uri"] if resource else None      # Extract resource URI if found

            # Extract network interface information
            interface = profile["interfaceDescriptions"][0]  # Get first interface description
            ipv4_addr = interface.get("ipv4Addr")           # Get IPv4 address
            port = interface.get("port")                     # Get port number

            # Construct complete URL: http://ipv4:port/uri (or just http://ipv4:port if no URI)
            return f"http://{ipv4_addr}:{port}{uri}" if uri else f"http://{ipv4_addr}:{port}"
            
        except (KeyError, IndexError, TypeError) as e:
            # Handle various parsing errors (missing keys, empty lists, wrong data types)
            logger.error(f"Error parsing URI: {e}")
            return None
