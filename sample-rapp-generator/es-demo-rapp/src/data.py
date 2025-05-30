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

import time
import logging
from influxdb.exceptions import InfluxDBClientError, InfluxDBServerError
from requests.exceptions import RequestException, ConnectionError
import influxdb_client
from datetime import datetime, timedelta
import random
import json
from sme_client import SMEClient
from influxdb_client.client.write_api import SYNCHRONOUS
import pandas as pd

logger = logging.getLogger(__name__)

class DATABASE(object):

    def __init__(self, dbname='Timeseries', user='user', password='password', host="influxdb_ip", port='influxdb_port', path='', ssl=False):
        self.token = None
        self.org = None
        self.bucket = None
        self.data = None
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.ssl = ssl
        self.dbname = dbname
        self.client = None
        self.address = None
        self.influx_invoker_id = None
        self.influx_api_name = None
        self.influx_resource_name = None
        self.config()

        # Set pandas options to display all rows and columns
        pd.set_option('display.max_rows', None)  # Show all rows
        pd.set_option('display.max_columns', None)  # Show all columns
        pd.set_option('display.width', 1000)  # Adjust the width to avoid line breaks
        pd.set_option('display.colheader_justify', 'left')  # Align column headers to the left

    def get_url_from_sme(self):
        sme_client = SMEClient(
            invoker_id=self.influx_invoker_id,
            api_name=self.influx_api_name,
            resource_name=self.influx_resource_name
        )

        self.influx_url = sme_client.discover_service()

        logger.info("InfluxDB URL: {}".format(self.influx_url))

        if self.influx_url is not None:
            self.address = self.influx_url
            logger.debug(f"InfluxDB URL: {self.influx_url}")
            self.host = self.influx_url.split(":")[1].replace("//", "")
            logger.debug(f"InfluxDB Host: {self.host}")
            self.port = self.influx_url.split(":")[2].split("/")[0]
            logger.debug(f"InfluxDB Port: {self.port}")
            self.address = self.influx_url.rstrip('/')
            logger.debug(f"InfluxDB Address: {self.address}")
        else:
            logger.error("Failed to discover InfluxDB URL from SME.")

    # Connect with influxdb
    def connect(self):
        if self.client is not None:
            self.client.close()

        try:
            self.client = influxdb_client.InfluxDBClient(url=self.address, org=self.org, token=self.token)
            version = self.client.version()
            logger.info("Connected to Influx Database, InfluxDB version : {}".format(version))
            return True

        except (RequestException, InfluxDBClientError, InfluxDBServerError, ConnectionError):
            logger.error("Failed to establish a new connection with InflulxDB, Please check your url/hostname")
            time.sleep(120)

    # Query information
    def read_data(self, train=False, valid=False, limit=False):

        self.data = None
        query = 'from(bucket:"{}")'.format(self.bucket)
        query += '|> range(start: -10m) '
        query += ' |> filter(fn: (r) => r["_measurement"] == "o-ran-pm")'
        query += ' |> filter(fn: (r) => r["_field"] == "CellID" or r["_field"] == "DRB.UEThpUl" or r["_field"] == "RRU.PrbUsedUl" or r["_field"] == "PEE.AvgPower") '
        #query += ' |> filter(fn: (r) => r["_field"] == "CellID" or r["_field"] == "RRC.ConnMean" or r["_field"] == "DRB.UEThpUl" or r["_field"] == "RRU.PrbUsedUl" or r["_field"] == "PEE.AvgPower") '
        query += ' |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value") '
        result = self.query(query)
        self.data = result
        return result

    # Query data
    def query(self, query):
        while True:
            try:
                query_api = self.client.query_api()
                result = query_api.query_data_frame(org=self.org, query=query)
                logger.debug(f'Cell data : {result}')
                return result
            except (RequestException, InfluxDBClientError, InfluxDBServerError, ConnectionError) as e:
                logger.error(f'Failed to query influxdb: {e}, retrying in 60 seconds...')
                time.sleep(60)

    def mapping(self, data):
        data[['S', 'B', 'C']] = data['CellID'].str.extract(r'S(\d+)/[BN](\d+)/C(\d+)')
        data[['S', 'B', 'C']] = data[['S', 'B', 'C']].astype(int)
        data = data.sort_values(by=['B', 'S', 'C'])
        data['cellidnumber'] = data.groupby(['B', 'S', 'C']).ngroup().add(1)
        data = data.drop(['S', 'B', 'C'], axis=1)
        return data

    def generate_synthetic_data(self):
        data = []
        fields = ["CellID", "DRB.UEThpUl", "RRU.PrbUsedUl", "PEE.AvgPower"]
        # fields = ["CellID", "DRB.UEThpUl", "RRU.PrbUsedUl", "PEE.AvgPower", "RRC.ConnMean"]
        other_fields = ["TestFilterField1", "TestFilterField2", "TestFilterField3", "TestFilterField4"]
        measurements = ["o-ran-pm", "test-filter-measurement1", "test-filter-measurement2", "test-filter-measurement3", "test-filter-measurement4"]

        # Generate matching records (synchronized _time for each group of 4)
        for _ in range(50):  # 50 records, each with 4 rows sharing the same time
            common_time = datetime.now() - timedelta(minutes=random.randint(0, 60))
            iso_time = common_time.isoformat()

            for field in fields:
                value = (
                    f"S{random.randint(1,9)}/B{random.randint(1,9)}/C{random.randint(1,9)}"
                    if field == "CellID"
                    else round(random.uniform(1, 100), 2)
                )
                record = {
                    "_time": iso_time,
                    "_measurement": "o-ran-pm",
                    "_field": field,
                    "_value": value
                }
                data.append(record)

        # Generate non-matching records (randomized)
        for _ in range(50):
            record = {
                "_time": (datetime.now() - timedelta(minutes=random.randint(0, 60))).isoformat(),
                "_measurement": random.choice(measurements),
                "_field": random.choice(other_fields),
                "_value": str(round(random.uniform(0, 100), 2))
            }
            data.append(record)

        # Log data to be written
        print(pd.DataFrame(data).to_string())

        self.write_synthetic_data_to_db(data)


    def write_synthetic_data_to_db(self, data):

        write_api = self.client.write_api(write_options=SYNCHRONOUS)
        for record in data:
            point = influxdb_client.Point(record["_measurement"]) \
                .field(record["_field"], record["_value"]) \
                .time(datetime.fromisoformat(record["_time"]))
            write_api.write(bucket=self.bucket, org=self.org, record=point)
        write_api.flush()
        write_api.close()
        logger.info("Synthetic data successfully written to InfluxDB.")

    def config(self):

        with open('config.json', 'r') as f:
            config = json.load(f)

        # Load the SME configuration from the JSON file
        sme_config = config.get("SME", {})
        self.influx_invoker_id = sme_config.get("influxdb_invoker_id")
        self.influx_api_name = sme_config.get("influxdb_api_name")
        self.influx_resource_name = sme_config.get("influxdb_resource_name")

        # Initialize the InfluxDB client
        influx_config = config.get("DB", {})
        self.token = influx_config.get("token")
        self.org = influx_config.get("org")
        self.bucket = influx_config.get("bucket")
        self.address = influx_config.get("address")
        self.host = influx_config.get("host")
        self.port = influx_config.get("port")
        self.ssl = influx_config.get("ssl")
        self.dbname = influx_config.get("database")
        self.meas = influx_config.get("measurement")
        self.user = influx_config.get("user")
        self.password = influx_config.get("password")
