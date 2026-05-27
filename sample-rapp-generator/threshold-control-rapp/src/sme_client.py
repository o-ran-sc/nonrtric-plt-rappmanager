import requests
import json
import logging
import time
from typing import Dict, Optional, Any, List, Tuple

logger = logging.getLogger(__name__)


class SDNCClient:
    """Client for SDNC operations with retry logic"""
    
    def __init__(self, sdnc_config: Dict[str, Any], sme_client):
        self.config = sdnc_config
        self.sme_client = sme_client
        self.node_name = sdnc_config.get('node_name', 'Benetel_RU')
        self.component_name = sdnc_config.get('component_name', 'benetel_RU')
        self.retry_attempts = int(sdnc_config.get('retry_attempts', 5))
        self.retry_delay = int(sdnc_config.get('retry_delay_seconds', 3))
        
        # Build endpoints
        self.hardware_endpoint = f"/rests/data/network-topology:network-topology/topology=topology-netconf/node={self.node_name}/yang-ext:mount/ietf-hardware:hardware/component={self.component_name}"
        self.carriers_endpoint = f"/rests/data/network-topology:network-topology/topology=topology-netconf/node={self.node_name}/yang-ext:mount/o-ran-uplane-conf:user-plane-configuration"
        
        logger.info(f"SDNC Client initialized for node: {self.node_name}, component: {self.component_name}")
    
    def get_energy_saving_state(self) -> Optional[bool]:
        """Get current energy saving state from SDNC"""
        try:
            logger.info(f"Getting energy saving state for {self.component_name}")
            
            response = self.sme_client.send_sdnc_request(
                endpoint=self.hardware_endpoint,
                payload={},
                method='GET'
            )
            
            if response and response.get('success'):
                data = response.get('data', {})
                components = data.get('ietf-hardware:component', [])
                
                # Handle both list and dict responses
                if isinstance(components, list):
                    # Find the component by name
                    for component in components:
                        if component.get('name') == self.component_name:
                            energy_saving = component.get('o-ran-hardware:energy-saving-enabled')
                            logger.info(f"Energy saving state: {energy_saving}")
                            return energy_saving
                    logger.error(f"Component {self.component_name} not found in response")
                    return None
                elif isinstance(components, dict):
                    # Single component as dict
                    energy_saving = components.get('o-ran-hardware:energy-saving-enabled')
                    logger.info(f"Energy saving state: {energy_saving}")
                    return energy_saving
                else:
                    logger.error(f"Unexpected component format: {type(components)}")
                    return None
            else:
                logger.error(f"Failed to get energy saving state: {response}")
                return None
                
        except Exception as e:
            logger.error(f"Error getting energy saving state: {e}")
            return None
    
    def set_energy_saving_state(self, enabled: bool) -> bool:
        """Set energy saving state in SDNC with retry logic"""
        action = "enable" if enabled else "disable"
        
        for attempt in range(1, self.retry_attempts + 1):
            try:
                logger.info(f"Attempt {attempt}/{self.retry_attempts}: Setting energy saving to {action}")
                
                # SDNC expects the component as a single object, not a list
                payload = {
                    "ietf-hardware:component": {
                        "name": self.component_name,
                        "o-ran-hardware:energy-saving-enabled": enabled
                    }
                }
                
                response = self.sme_client.send_sdnc_request(
                    endpoint=self.hardware_endpoint,
                    payload=payload,
                    method='PATCH'
                )
                
                if response and response.get('success'):
                    logger.info(f"Successfully set energy saving to {action}")
                    return True
                else:
                    logger.warning(f"Attempt {attempt} failed to set energy saving: {response}")
                    
            except Exception as e:
                logger.error(f"Attempt {attempt} error setting energy saving: {e}")
            
            if attempt < self.retry_attempts:
                logger.info(f"Waiting {self.retry_delay}s before retry...")
                time.sleep(self.retry_delay)
        
        logger.error(f"Failed to set energy saving to {action} after {self.retry_attempts} attempts")
        return False
    
    def get_carrier_states(self) -> Optional[Dict[str, List[Dict[str, Any]]]]:
        """Get current carrier states from SDNC"""
        try:
            logger.info("Getting carrier states")
            
            endpoint_with_params = f"{self.carriers_endpoint}?content=config"
            
            response = self.sme_client.send_sdnc_request(
                endpoint=endpoint_with_params,
                payload={},
                method='GET'
            )
            
            if response and response.get('success'):
                data = response.get('data', {})
                uplane_config = data.get('o-ran-uplane-conf:user-plane-configuration', {})
                
                tx_carriers = uplane_config.get('tx-array-carriers', [])
                rx_carriers = uplane_config.get('rx-array-carriers', [])
                
                logger.info(f"Retrieved {len(tx_carriers)} TX carriers and {len(rx_carriers)} RX carriers")
                
                # Log current states
                for carrier in tx_carriers:
                    logger.debug(f"TX Carrier {carrier.get('name')}: {carrier.get('active')}")
                for carrier in rx_carriers:
                    logger.debug(f"RX Carrier {carrier.get('name')}: {carrier.get('active')}")
                
                return {
                    'tx': tx_carriers,
                    'rx': rx_carriers
                }
            else:
                logger.error(f"Failed to get carrier states: {response}")
                return None
                
        except Exception as e:
            logger.error(f"Error getting carrier states: {e}")
            return None
    
    def verify_carrier_states(self, expected_state: str) -> bool:
        """Verify all carriers are in expected state (ACTIVE or SLEEP) with retry logic"""
        logger.info(f"Verifying all carriers are in {expected_state} state")
        
        for attempt in range(1, self.retry_attempts + 1):
            logger.info(f"Verification attempt {attempt}/{self.retry_attempts}")
            
            carriers = self.get_carrier_states()
            if not carriers:
                logger.warning(f"Attempt {attempt}: Could not retrieve carrier states")
                if attempt < self.retry_attempts:
                    time.sleep(self.retry_delay)
                continue
            
            # Check all TX carriers
            tx_carriers = carriers.get('tx', [])
            tx_all_match = all(c.get('active') == expected_state for c in tx_carriers)
            
            # Check all RX carriers
            rx_carriers = carriers.get('rx', [])
            rx_all_match = all(c.get('active') == expected_state for c in rx_carriers)
            
            if tx_all_match and rx_all_match:
                logger.info(f"✓ All carriers verified in {expected_state} state")
                return True
            else:
                # Log which carriers don't match
                for carrier in tx_carriers:
                    if carrier.get('active') != expected_state:
                        logger.warning(f"TX Carrier {carrier.get('name')} is {carrier.get('active')}, expected {expected_state}")
                for carrier in rx_carriers:
                    if carrier.get('active') != expected_state:
                        logger.warning(f"RX Carrier {carrier.get('name')} is {carrier.get('active')}, expected {expected_state}")
                
                if attempt < self.retry_attempts:
                    logger.info(f"Waiting {self.retry_delay}s before retry...")
                    time.sleep(self.retry_delay)
        
        logger.error(f"✗ Failed to verify carriers in {expected_state} state after {self.retry_attempts} attempts")
        return False
    
    def set_carrier_states(self, state: str) -> bool:
        """Set all carriers to specified state (ACTIVE or SLEEP) with retry logic"""
        logger.info(f"Setting all carriers to {state}")
        
        # First, get current carriers to know their names
        carriers = self.get_carrier_states()
        if not carriers:
            logger.error("Cannot set carrier states: failed to retrieve current carriers")
            return False
        
        tx_carriers = carriers.get('tx', [])
        rx_carriers = carriers.get('rx', [])
        
        # Build payload with only name and active fields
        tx_payload = [{'name': c['name'], 'active': state} for c in tx_carriers]
        rx_payload = [{'name': c['name'], 'active': state} for c in rx_carriers]
        
        payload = {
            "o-ran-uplane-conf:user-plane-configuration": {
                "tx-array-carriers": tx_payload,
                "rx-array-carriers": rx_payload
            }
        }
        
        for attempt in range(1, self.retry_attempts + 1):
            try:
                logger.info(f"Attempt {attempt}/{self.retry_attempts}: Patching carriers to {state}")
                logger.debug(f"Payload: {json.dumps(payload, indent=2)}")
                
                response = self.sme_client.send_sdnc_request(
                    endpoint=self.carriers_endpoint,
                    payload=payload,
                    method='PATCH'
                )
                
                if response and response.get('success'):
                    logger.info(f"Successfully patched carriers to {state}")
                    return True
                else:
                    logger.warning(f"Attempt {attempt} failed to patch carriers: {response}")
                    
            except Exception as e:
                logger.error(f"Attempt {attempt} error patching carriers: {e}")
            
            if attempt < self.retry_attempts:
                logger.info(f"Waiting {self.retry_delay}s before retry...")
                time.sleep(self.retry_delay)
        
        logger.error(f"Failed to set carriers to {state} after {self.retry_attempts} attempts")
        return False


class SMEClient:
    """Client for Service Management & Exposure (SME) integration"""
    
    def __init__(self, sme_config: Dict[str, Any]):
        self.config = sme_config
        self.base_url = f"http://{sme_config['host']}:{sme_config['port']}"
        self.discovery_endpoint = sme_config['sme_discovery_endpoint']
        self.invoker_id = sme_config.get('invoker_id', 'threshold-control-rapp-invoker')
        self.session = requests.Session()
        
        # Service configurations
        self.services = {}
        
        logger.info(f"SME Client initialized with base URL: {self.base_url}")
        logger.info(f"Invoker ID: {self.invoker_id}")

    def discover_services(self) -> bool:
        """Discover available services through SME"""
        try:
            # Note: Discovery without api-name returns all services
            # For specific service discovery, use get_service_config with api-name
            response = self.session.get(self.discovery_endpoint, timeout=10)
            response.raise_for_status()
            
            services_data = response.json()
            logger.info(f"Discovered {len(services_data)} services")
            
            # Parse and store service information
            for service in services_data:
                service_name = service.get('apiName', 'unknown')
                self.services[service_name] = service
                logger.debug(f"Registered service: {service_name}")
            
            return True
            
        except Exception as e:
            logger.error(f"Failed to discover services: {e}")
            return False

    def discover_service_by_name(self, api_name: str) -> Optional[Dict[str, Any]]:
        """Discover a specific service by API name with required query parameters"""
        try:
            # Build query parameters as required by SME
            # Check if invoker_id already has the prefix
            invoker_id_param = self.invoker_id
            if not invoker_id_param.startswith('api_invoker_id_'):
                invoker_id_param = f"api_invoker_id_{invoker_id_param}"
            
            query_params = {
                'api-invoker-id': invoker_id_param,
                'api-name': api_name
            }
            
            # Construct full URL with query parameters
            query_string = '&'.join([f"{k}={v}" for k, v in query_params.items()])
            full_url = f"{self.discovery_endpoint}?{query_string}"
            
            logger.info(f"Discovering service: {api_name}")
            logger.debug(f"Full discovery URL: {full_url}")
            
            response = self.session.get(full_url, headers={"Content-Type": "application/json"}, timeout=10)
            response.raise_for_status()
            
            service_data = response.json()
            logger.info(f"Successfully discovered service: {api_name}")
            
            return service_data
            
        except Exception as e:
            logger.error(f"Failed to discover service {api_name}: {e}")
            return None

    def get_service_config(self, service_type: str) -> Optional[Dict[str, Any]]:
        """Get configuration for a specific service type"""
        try:
            # Map service types to SME service names
            service_mapping = {
                'influxdb': self.config.get('influxdb_api_name', 'influxdb2-http'),
                'sdnc': self.config.get('sdnc_api_name', 'sdnc-rest')
            }
            
            service_name = service_mapping.get(service_type)
            if not service_name:
                logger.warning(f"Unknown service type: {service_type}")
                return None
            
            # Check for local override (for testing)
            override_key = f'{service_type}_url_override'
            if self.config.get(override_key):
                override_url = self.config[override_key]
                logger.info(f"Using URL override for {service_type}: {override_url}")
                return {
                    'url': override_url,
                    'service_name': service_name,
                    'api_version': '',
                }
            
            # Discover specific service with required query parameters
            service_data = self.discover_service_by_name(service_name)
            if not service_data:
                logger.warning(f"Service {service_name} not found")
                return None
            
            # Parse the service data
            try:
                service_descriptions = service_data.get('serviceAPIDescriptions', [])
                if not service_descriptions:
                    logger.warning(f"No service descriptions found for {service_name}")
                    return None
                
                service_info = service_descriptions[0]
                
                # Extract service configuration
                aef_profiles = service_info.get('aefProfiles', [])
                if not aef_profiles:
                    logger.warning(f"No AEF profiles found for service {service_name}")
                    return None
                
                # Get the first available interface
                interface_desc = aef_profiles[0].get('interfaceDescriptions', [])
                if not interface_desc:
                    logger.warning(f"No interface descriptions found for service {service_name}")
                    return None
                
                interface = interface_desc[0]
                base_url = f"http://{interface['ipv4Addr']}:{interface['port']}"
                
                # Get the URI path from resources
                uri_path = ""
                versions = aef_profiles[0].get('versions', [])
                if versions:
                    resources = versions[0].get('resources', [])
                    if resources:
                        uri_path = resources[0].get('uri', '')
                
                # Combine base URL with URI path
                discovered_url = base_url + uri_path
                
                logger.debug(f"Base URL: {base_url}")
                logger.debug(f"URI Path: {uri_path}")
                logger.debug(f"Full URL: {discovered_url}")
                
                # Check for URL rewrite (for local testing with port-forwarding)
                url_rewrite = self.config.get('url_rewrite', {})
                final_url = discovered_url
                
                for pattern, replacement in url_rewrite.items():
                    if pattern in discovered_url:
                        final_url = discovered_url.replace(pattern, replacement)
                        logger.info(f"Rewrote URL: {discovered_url} -> {final_url}")
                        break
                
                service_config = {
                    'url': final_url,
                    'service_name': service_name,
                    'api_version': aef_profiles[0].get('versions', [{}])[0].get('apiVersion', ''),
                }
                
                logger.info(f"Retrieved config for {service_type}: {service_config['url']}")
                return service_config
                
            except (KeyError, IndexError, TypeError) as e:
                logger.error(f"Error parsing service config: {e}")
                return None
            
        except Exception as e:
            logger.error(f"Failed to get service config for {service_type}: {e}")
            return None

    def send_influxdb_request(self, query: str) -> Optional[Dict[str, Any]]:
        """Send Flux query request to InfluxDB through SME"""
        try:
            influx_config = self.get_service_config('influxdb')
            if not influx_config:
                logger.error("Could not get InfluxDB service config from SME")
                return None
            
            # Prepare request - handle trailing slashes properly
            base_url = influx_config['url'].rstrip('/')
            url = f"{base_url}/api/v2/query"
            
            # Get org from config
            org = self.config.get('influxdb_org', 'est')
            
            # Add org as query parameter
            url_with_params = f"{url}?org={org}"
            
            # Get token from config
            token = self.config.get('influxdb_token', '')
            
            # Debug logging to see what token we got
            if token:
                # Log first and last 4 chars for security
                token_preview = f"{token[:4]}...{token[-4:]}" if len(token) > 8 else "***"
                logger.info(f"Using InfluxDB token: {token_preview}")
            else:
                logger.error("InfluxDB token not configured or empty")
                return {'success': False, 'error': 'InfluxDB token not configured'}
            
            # Check if token still has template syntax (not substituted)
            if token.startswith('${'):
                logger.error(f"InfluxDB token not substituted from environment variable: {token}")
                return {'success': False, 'error': 'InfluxDB token contains unsubstituted variable'}
            
            headers = {
                'Authorization': f"Token {token}",
                'Content-Type': 'application/vnd.flux',
                'Accept': 'application/csv'
            }
            
            # Add invoker ID if configured
            if self.config.get('influxdb_invoker_id'):
                headers['X-Invoker-Id'] = self.config['influxdb_invoker_id']
            
            logger.debug(f"Sending InfluxDB query to: {url_with_params}")
            logger.debug(f"Query: {query}")
            
            response = self.session.post(url_with_params, data=query, headers=headers, timeout=30)
            response.raise_for_status()
            
            logger.debug(f"InfluxDB request successful: {response.status_code}")
            return {'success': True, 'data': response.text}
            
        except Exception as e:
            logger.error(f"InfluxDB request failed: {e}")
            return {'success': False, 'error': str(e)}

    def send_sdnc_request(self, endpoint: str, payload: Dict[str, Any], method: str = 'POST') -> Optional[Dict[str, Any]]:
        """Send request to SDNC through SME"""
        try:
            sdnc_config = self.get_service_config('sdnc')
            if not sdnc_config:
                return None
            
            # Prepare request - handle double slashes
            base_url = sdnc_config['url'].rstrip('/')
            endpoint = endpoint if endpoint.startswith('/') else f"/{endpoint}"
            url = f"{base_url}{endpoint}"
            
            headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
            
            # Add invoker ID if configured
            if self.config.get('sdnc_invoker_id'):
                headers['X-Invoker-Id'] = self.config['sdnc_invoker_id']
            
            # Add authentication if configured
            auth = None
            if self.config.get('sdnc_username') and self.config.get('sdnc_password'):
                auth = (self.config['sdnc_username'], self.config['sdnc_password'])
            
            # Send request
            if method.upper() == 'POST':
                response = self.session.post(url, json=payload, headers=headers, auth=auth, timeout=30)
            elif method.upper() == 'PUT':
                response = self.session.put(url, json=payload, headers=headers, auth=auth, timeout=30)
            elif method.upper() == 'PATCH':
                response = self.session.patch(url, json=payload, headers=headers, auth=auth, timeout=30)
            elif method.upper() == 'DELETE':
                response = self.session.delete(url, headers=headers, auth=auth, timeout=30)
            else:
                response = self.session.get(url, headers=headers, auth=auth, timeout=30)
            
            response.raise_for_status()
            
            logger.info(f"SDNC {method} request successful: {response.status_code}")
            
            # Try to parse JSON response
            try:
                response_data = response.json()
            except:
                response_data = {'raw_response': response.text}
            
            return {'success': True, 'data': response_data, 'status_code': response.status_code}
            
        except Exception as e:
            logger.error(f"SDNC request failed: {e}")
            return {'success': False, 'error': str(e)}

    def register_as_invoker(self, invoker_info: Dict[str, Any]) -> Optional[str]:
        """Register this rApp as an API invoker"""
        try:
            url = f"{self.base_url}/api-invoker-management/v1/onboardedInvokers"
            
            response = self.session.post(url, json=invoker_info, timeout=30)
            response.raise_for_status()
            
            result = response.json()
            invoker_id = result.get('apiInvokerId')
            
            if invoker_id:
                logger.info(f"Successfully registered as invoker: {invoker_id}")
                return invoker_id
            else:
                logger.error("No invoker ID returned from registration")
                return None
                
        except Exception as e:
            logger.error(f"Failed to register as invoker: {e}")
            return None

    def health_check(self) -> bool:
        """Check if SME is accessible"""
        try:
            response = self.session.get(f"{self.base_url}/health", timeout=10)
            return response.status_code == 200
        except:
            return False