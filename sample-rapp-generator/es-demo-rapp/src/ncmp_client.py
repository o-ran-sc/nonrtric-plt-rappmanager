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

import json
import logging
import requests
from sme_client import SMEClient

logger = logging.getLogger(__name__)

class NCMP_CLIENT(object):
    def __init__(self):
        with open('config.json', 'r') as f:
            config = json.load(f)

        sme_config = config.get("SME", {})
        self.host = sme_config.get("host")
        self.port = sme_config.get("port")
        self.ncmp_invoker_id = sme_config.get("ncmp_invoker_id")
        self.ncmp_api_name = sme_config.get("ncmp_api_name")
        self.ncmp_resource_name = sme_config.get("ncmp_resource_name")
        self.ncmp_me = sme_config.get("ncmp_managed_element_id", "ManagedElement-002")
        self.ncmp_gnb = sme_config.get("ncmp_gnbdufunction_id", "GNBDUFunction-001")
        self.resourse_identifier = sme_config.get("resource_id")
        self.ncmp_uri = None

        sme_client = SMEClient(
            invoker_id=self.ncmp_invoker_id,
            api_name=self.ncmp_api_name,
            resource_name=self.ncmp_resource_name
        )

        self.ncmp_uri = sme_client.discover_service()

        print("Discovered NCMP URI: ", self.ncmp_uri)

    def power_off_cell(self, cell_with_node):

        passthrough_request = self.make_passthrough_request(cell_with_node)
        # This log is all it does in testing
        logger.info("Powering-off cell " + str(cell_with_node) + " in progress...")

        # It expects the SME ncmp endpoint to call power off
        # endpoint_with_query = f"{endpoint}?resourceIdentifier={self.resourse_identifier}"
        #
        headers = {
            "Content-Type": "application/json"
        }

        body = {
            "attributes": {
                "administrativeState": "LOCKED"
            }
        }

        response = requests.patch(passthrough_request, json=body, headers=headers)

        if response.status_code == 200:
            logger.info("Power-off successful. " + response.text)
            return True
        else:
            logger.error(f"Error in connection to NCMP for power off: {response.status_code}")
            logger.error(response.text)
            return False

    def power_on_cell(self, cell_with_node):

        # This log is all it does in testing
        passthrough_request = self.make_passthrough_request(cell_with_node)
        logger.info("Powering-on cell " + str(cell_with_node) + " in progress...")

        # It expects the SME ncmp endpoint to call power on
        # endpoint_with_query = f"{endpoint}?resourceIdentifier={self.resourse_identifier}"

        headers = {
            "Content-Type": "application/json"
        }

        body = {
            "attributes": {
                "administrativeState": "UNLOCKED"
            }
        }

        response = requests.patch(passthrough_request, json=body, headers=headers)

        if response.status_code == 200:
            logger.info("Power-on successful. " + response.text)
            return True
        else:
            logger.error(f"Error in connection to NCMP for power on: {response.status_code}")
            logger.error(response.text)
            return False

    def make_passthrough_request(self, cell_with_node):
        node_id = cell_with_node.split('_')[1]
        cell_id = cell_with_node.split('_')[0]

        endpoint = f"ncmp/v1/ch/{node_id}/data/ds/ncmp-datastore%3Apassthrough-running"
        query_param = (f"?resourceIdentifier=/_3gpp-common-managed-element:ManagedElement={self.ncmp_me}"
                       f"/_3gpp-nr-nrm-gnbdufunction:GNBDUFunction={self.ncmp_gnb}"
                       f"/_3gpp-nr-nrm-nrcelldu:NRCellDU={cell_id}/attributes")

        return f"{self.ncmp_uri}{endpoint}{query_param}"

# if __name__ == "__main__":
#     logging.basicConfig(level=logging.INFO)  # Set up logging for better visibility
#
#     # Instantiate the NcmpClient
#     ncmp_client = NCMP_CLIENT()

# Test the discover_ncmp_via_sme method
# Discover service

# Test the power_off_cell method
#if discovery_result:
#    power_off_result = ncmp_client.power_off_cell(discovery_result)
#    print(f"Power Off Result: {power_off_result}")

# Test the power_on_cell method
#if discovery_result:
#    power_on_result = ncmp_client.power_on_cell(discovery_result)
#    print(f"Power On Result: {power_on_result}")

