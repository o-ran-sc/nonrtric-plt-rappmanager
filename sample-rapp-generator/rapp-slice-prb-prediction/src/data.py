import logging
import pandas as pd

import json
import os

logger = logging.getLogger(__name__)

class DATABASE(object):

    def __init__(self):
        self.address = None
        self.token = None
        self.org = None
        self.bucket = None
        self.client = None
        
        self.time_range = None
        self.measurements = None
        self.field_names = None
        self.window_size = None
        self.tag_slice_type = None
        self.tag_nssi_id = None

        self.config()

        # Set pandas options to display all rows and columns
        pd.set_option('display.max_rows', None)  # Show all rows
        pd.set_option('display.max_columns', None)  # Show all columns
        pd.set_option('display.width', 1000)  # Adjust the width to avoid line breaks
        pd.set_option('display.colheader_justify', 'left')  # Align column headers to the left

    def config(self):

        with open('config.json', 'r') as f:
            config = json.load(f)

        # Load the SME configuration from the JSON file
        sme_config = config.get("SME", {})
        self.invoker_id = sme_config.get("invoker_id")
        self.influx_api_name = sme_config.get("influxdb_api_name")
        self.influx_resource_name = sme_config.get("influxdb_resource_name")

        # Initialize the InfluxDB client
        influx_config = config.get("DB", {})

        if os.getenv('INFLUX_TOKEN'):
            self.token = os.getenv('INFLUX_TOKEN')
        else:
            logger.info("INFLUX_TOKEN environment variable is not set.")
            self.token = influx_config.get("token")

        self.address = influx_config.get("address")
        self.org = influx_config.get("org")
        self.bucket = influx_config.get("bucket")

        self.field_names = influx_config.get("field_names")
        self.time_range = influx_config.get("time_range")
        self.measurements = influx_config.get("measurements")

        window_size_str = influx_config.get("window_size", "900") # Default to "10" string
        self.window_size = int(window_size_str)
        self.tag_slice_type = influx_config.get("tag_slice_type")
        self.tag_nssi_id = influx_config.get("tag_nssi_id")
