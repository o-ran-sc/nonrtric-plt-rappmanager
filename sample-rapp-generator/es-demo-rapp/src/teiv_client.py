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
import urllib.parse
from sme_client import SMEClient

logger = logging.getLogger(__name__)

class TEIV_CLIENT(object):
    def __init__(self):
        with open('config.json', 'r') as f:
            config = json.load(f)

        sme_config = config.get("SME", {})
        self.host = sme_config.get("host")
        self.port = sme_config.get("port")
        self.teiv_invoker_id = sme_config.get("teiv_invoker_id")
        self.teiv_api_name = sme_config.get("teiv_api_name")
        self.teiv_resource_name = sme_config.get("teiv_resource_name")
        self.odufunction_id = sme_config.get("odufunction_id")
        self.teiv_uri = None

        sme_client = SMEClient(
            invoker_id=self.teiv_invoker_id,
            api_name=self.teiv_api_name,
            resource_name=self.teiv_resource_name
        )

        self.teiv_uri = sme_client.discover_service() + "topology-inventory/v1alpha11/"

        print("Discovered TEIV URI: ", self.teiv_uri)

    def get_nrcelldus(self):
        odufunction_id = self.odufunction_id
        scope_filter = f"/provided-by-oduFunction[@id=\"{odufunction_id}\"]"
        encoded_scope_filter = urllib.parse.quote(scope_filter)
        endpoint = (
            f"{self.teiv_uri}domains/RAN/entity-types/NRCellDU/entities?"
            f"scopeFilter={encoded_scope_filter}&targetFilter=/attributes;/sourceIds"
        )
        logger.info("TEIV full endpoint: " + endpoint)
        response = requests.get(endpoint)
        
        if response.status_code == 200:
            nrcelldu_ids = self.search_entity_data_for_ids(response.json())
            logger.info(f"Retrieved NRCellDU IDs form TEIV: {nrcelldu_ids}")
            return nrcelldu_ids
        else:
            logger.error(f"Error in connection to TEIV: {response.status_code}")
            logger.error(response.text)
            return None

    def search_entity_data_for_ids(self, json_data):
        items = json_data.get("items", [])
        ids = []
        for item in items:
            for key in item:
                ids.extend(
                    entity.get('id').split(':')[-1]
                    for entity in item[key]
                    if 'id' in entity
                )
        return ids

# if __name__ == "__main__":
#     logging.basicConfig(level=logging.INFO)  # Set up logging for better visibility
#
#     # Instantiate the TEIVClient
#     teiv_client = TEIV_CLIENT()
#
#     nrcelldu_json = teiv_client.get_nrcelldus()
