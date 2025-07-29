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
import random
import requests
import logging
from sme_client import SMEClient

logger = logging.getLogger(__name__)

class ASSIST(object):

    def __init__(self):
        self.kserve_url = None
        self.invoker_id = None
        self.api_name = None
        self.resource_name = None
        self.config()
        self.get_url_from_sme()

    def config(self):
        with open('config.json', 'r') as f:
            config = json.load(f)

        assist_config = config.get("SME", {})
        self.invoker_id = assist_config.get("kserve_invoker_id")
        self.api_name = assist_config.get("kserve_api_name")
        self.resource_name = assist_config.get("kserve_resource_name")

    def get_url_from_sme(self):
        sme_client = SMEClient(
            invoker_id=self.invoker_id,
            api_name=self.api_name,
            resource_name=self.resource_name
        )

        self.kserve_url = sme_client.discover_service()

        if self.kserve_url is None:
            logger.error("Failed to discover KServe URL.")
            return None
        logger.info(f"Discovered KServe URL: {self.kserve_url}")

    def send_request_to_server(self, json_data, randomize=False):

        if(not randomize):

            if isinstance(json_data, dict) and "instances" in json_data:
                json_data["instances"] = [
                    [
                        [float(val) for val in sublist]
                        for sublist in instance
                    ]
                    for instance in json_data["instances"]
                ]


            with open('input.json', 'w') as f:
                json.dump(json_data, f)

            # Commented out for local testing
            # url = 'http://localhost:8080/v1/models/es-aiml-model:predict'
            # headers = {'Host': 'es-aiml-model-predictor-default.default.svc.cluster.local'}
            url = self.kserve_url + 'v1/models/es-aiml-model:predict'
            host = self.kserve_url.split('/')[2].split(':')[0]
            headers = {'Host': host }

            response = requests.post(url, headers=headers, json=json_data)
            logger.info("Prediction result")
            logger.info(response.text)
            return response.status_code, response.text
        else:
            data = json.dumps({
                "predictions": self.random_predictions()
            })

            logger.info("Prediction result")
            logger.info(data)

            return 200, data

    def random_predictions(self):
        predictions = []
        # Generate random decision for all rows
        power_value = random.random()
        for _ in range(3):  # Generate 3 rows of predictions
            if power_value < 0.5:  # 50% chance for all values < 0.4
                row = [random.uniform(0.0, 0.039) for _ in range(3)]
            else:  # 50% chance for all values >= 0.4
                row = [random.uniform(0.4, 1.0) for _ in range(3)]
            predictions.append(row)

        return predictions