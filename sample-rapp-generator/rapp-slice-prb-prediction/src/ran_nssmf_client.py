import json
import logging

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
            