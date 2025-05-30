#  ============LICENSE_START===============================================
#  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

import requests
import logging
import json

logger = logging.getLogger(__name__)

class SMEClient:
    def __init__(self, invoker_id, api_name, resource_name):
        self.host = None
        self.port = None
        self.invoker_id = invoker_id
        self.api_name = api_name
        self.resource_name = resource_name

        with open('config.json', 'r') as f:
            config = json.load(f)

        sme_config = config.get("SME", {})
        self.host = sme_config.get("host")
        self.port = sme_config.get("port")
        self.sme_discovery_endpoint = sme_config.get("sme_discovery_endpoint")

    def discover_service(self):
        #url = f"http://{self.host}:{self.port}/service-apis/v1/allServiceAPIs"
        #url = f"http://{self.sme_discovery_endpoint}"
        query = f"api-invoker-id=api_invoker_id_{self.invoker_id}&api-name={self.api_name}"
        full_url = f"{self.sme_discovery_endpoint}?{query}"
        logger.info(f"Full URL for service discovery: {full_url}")

        try:
            response = requests.get(full_url, headers={"Content-Type": "application/json"})
            if response.status_code == 200:
                logger.info("Service discovery successful.")
                return self.parse_uri(response.json())
            else:
                logger.error(f"Failed to discover service. Status code: {response.status_code}")
                logger.error(response.text)
                return None
        except requests.RequestException as e:
            logger.error(f"Error during service discovery: {e}")
            return None

    def parse_uri(self, response):
        try:
            logger.debug("Parsing SME response to extract URI.")
            service = response["serviceAPIDescriptions"][0]
            profile = service["aefProfiles"][0]
            version = profile["versions"][0]
            resource = next(
                (res for res in version["resources"] if res["resourceName"] == self.resource_name),
                None
            )
            uri = resource["uri"] if resource else None

            interface = profile["interfaceDescriptions"][0]
            ipv4_addr = interface.get("ipv4Addr")
            port = interface.get("port")

            return f"http://{ipv4_addr}:{port}{uri}" if uri else f"http://{ipv4_addr}:{port}"
        except (KeyError, IndexError, TypeError) as e:
            logger.error(f"Error parsing URI: {e}")
            return None