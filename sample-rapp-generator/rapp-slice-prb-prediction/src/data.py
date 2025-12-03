import logging
import time
import influxdb_client
import pandas as pd

import json
import os

from requests import RequestException

from sme_client import SMEClient

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

    # Connect with influxdb
    def connect(self):
        if self.client is not None:
            self.client.close()

        try:
            self.client = influxdb_client.InfluxDBClient(url=self.address, org=self.org, token=self.token)
            version = self.client.version()
            logger.info("Connected to Influx Database, InfluxDB version : {}".format(version))
            return True

        except (RequestException, ConnectionError):
            logger.error("Failed to establish a new connection with InflulxDB, Please check your url/hostname")
            time.sleep(120)

    def read_data(self):
        # Fetch Data from InfluxDB
        fields_filter = " or ".join([f'r["_field"] == "{f}"' for f in self.field_names])
        query = f'''
            from(bucket: "{self.bucket}")
            |> range(start: {self.time_range})
            |> filter(fn: (r) => r["_measurement"] == "{self.measurements}")
            |> filter(fn: (r) => {fields_filter})
            |> tail(n:{self.window_size})
            |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
            |> keep(columns: ["_time", "{self.tag_slice_type}", "{self.tag_nssi_id}", "{'","'.join(self.field_names)}"])
            |> sort(columns: ["_time"])
        '''

        result = self.query(query)
        return result

    # Query data
    def query(self, query):
        while True:
            try:
                query_api = self.client.query_api()
                result = query_api.query_data_frame(org=self.org, query=query)
                return result
            except (RequestException, ConnectionError) as e:
                logger.error(f'Failed to query influxdb: {e}, retrying in 60 seconds...')
                time.sleep(60)

    def get_url_from_sme(self):
        sme_client = SMEClient(
            invoker_id=self.invoker_id,
            api_name=self.influx_api_name,
            resource_name=self.influx_resource_name
        )

        self.influx_url = sme_client.discover_service()

        logger.info("InfluxDB URL: {}".format(self.influx_url))

        if self.influx_url is not None:
            self.address = self.influx_url.rstrip('/')
            logger.debug(f"InfluxDB Address: {self.address}")
        else:
            logger.error("Failed to discover InfluxDB URL from SME.")