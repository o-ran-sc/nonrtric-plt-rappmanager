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

import argparse
import time
import pandas as pd
import schedule
from threading import Lock
import logging
from data import DATABASE
from assist import ASSIST
from ncmp_client import NCMP_CLIENT
from teiv_client import TEIV_CLIENT
import json

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class ESrapp():
    def __init__(self, generate_db_data=True, use_sme_db=False, random_predictions=False):

        # Initialize the local storage of cell status
        self.cell_power_status = {}
        self.teiv_cells = {}

        # Initialize the database and prediction client
        self.db = DATABASE()
        self.assist=ASSIST()

        self.random_predictions = random_predictions

        if use_sme_db:
            # Get the InfluxDB URL from SME
            self.db.get_url_from_sme()

        self.db.connect()

        if generate_db_data:
            # Use local InfluxDB and generate synthetic data - only for local testing
            self.db.generate_synthetic_data()


        self.threshold = 50
        # Initialize the NCMP client - allows us to query the cells from the RAN and power them on/off
        self.ncmp_client = NCMP_CLIENT()
        self.index = 1

        # Get the ODU function ID from the database
        self.teiv_client = TEIV_CLIENT()
        #self.get_teiv_cells()
        # Create Policy Manager instance
        #self.policy_manager = PolicyManager(base_url="http://192.168.8.111:32080/a1mediator/A1-P/v2", policy_type_id=20008)

        # Create policy type and policy instance
        #self.policy_manager.create_policy_type()
        self.inference_lock = Lock()
        self._running = False


    def entry(self):
        if self._running:
            logger.warning("ES rApp is already running")
            return

        self._running = True
        self.job = schedule.every(10).seconds.do(self.safe_inference)
        last_run = 0

        try:
            while self._running:
                now = time.time()
                if now - last_run >= 10:  # 10 second interval
                    self.safe_inference()
                    last_run = now
                time.sleep(1)

        except KeyboardInterrupt:
            logger.info("ES rApp shutting down gracefully")
        except Exception as e:
            logger.error(f"Error in entry loop: {str(e)}", exc_info=True)
        finally:
            try:
                schedule.cancel_job(self.job)
            except:
                pass
            self._running = False
            try:
                if self.inference_lock.locked():
                    self.inference_lock.release()
            except:
                pass

    def safe_inference(self):
        if not self.inference_lock.acquire(blocking=False):
            logger.warning("Previous inference still running, skipping this iteration")
            return

        try:
            self.inference()
        finally:
            self.inference_lock.release()
    # Send data to ML rApp
    def inference(self):
        data = self.db.read_data()

        if data.empty:
            logger.info("No data to process... skipping this iteration of inference.")
            return

        data_mapping = self.mapping(data)
        # Group the data by CellID and _measurement. This means that even if cell ids are the same, but the measurement is different, they will be processed separately.
        groups = data_mapping.groupby(["CellID", "_measurement"])
        for group_name, group_data in groups:
            json_data = self.generate_json_data(group_data)
            logger.info(f"Send data to ML rApp {group_name}: {json_data}")
            status_code, response_text = self.assist.send_request_to_server(json_data, randomize=self.random_predictions)
            if not self.check_and_perform_action(response_text):
                cell_id_name = group_data['CellID'].iloc[0]
                # Check if the cell is in TEIV
                self.check_cell_in_teiv(cell_id_name)
                du_name = self.extract_managed_element(group_data['_measurement'].iloc[0])
                cell_with_node = cell_id_name + "_" + du_name
                logger.info(f"Turn on the cell {group_name}")
                # Wait for 3 seconds before performing the action
                time.sleep(3)

                if cell_with_node not in self.cell_power_status:
                    logger.debug(f"Cell {cell_with_node} not in local cache. Adding it...")
                    self.cell_power_status[cell_with_node] = "off"
                # Check if the cell is already powered on
                if self.cell_power_status[cell_with_node] == "on":
                    logger.debug(f"Cell {cell_with_node} is already powered on.")
                    # continue
                else:
                    self.ncmp_client.power_on_cell(cell_with_node)
                    self.cell_power_status[cell_with_node] = "on"
            else:
                du_name = self.extract_managed_element(group_data['_measurement'].iloc[0])
                cell_id_name = group_data['CellID'].iloc[0]
                # Check if the cell is in TEIV
                self.check_cell_in_teiv(cell_id_name)
                cell_with_node = cell_id_name + "_" + du_name
                logger.info(f"Turn off the cell {group_name}")
                # Wait for 3 seconds before performing the action
                time.sleep(3)

                if cell_with_node not in self.cell_power_status:
                    logger.debug(f"Cell {cell_with_node} not in local cache. Adding it...")
                    self.cell_power_status[cell_with_node] = "on"

                if self.cell_power_status[cell_with_node] == "off":
                    logger.debug(f"Cell {cell_with_node} is already powered off.")
                    # continue
                else:
                    if self.ncmp_client.power_off_cell(cell_with_node):
                        self.cell_power_status[cell_with_node] = "off"

    def extract_managed_element(self, measurement):
        if '=' not in measurement or ',' not in measurement:
            return measurement

        parts = measurement.split(',')
        for part in parts:
            if part.startswith('ManagedElement='):
                return part.split('=')[1]

        return measurement
    # Generate the input data for ML rApp
    def generate_json_data(self, data):
        # rrc_conn_mean_values = data["RRC.ConnMean"].tolist()
        drb_ue_thp_ul_values = data["DRB.UEThpUl"].tolist()
        rru_prb_used_ul_values = data["RRU.PrbUsedUl"].tolist()
        pee_avg_power_values = data["PEE.AvgPower"].tolist()

        instances = [
            [
                [drb, rru, pee]
                for drb, rru, pee in zip(
                drb_ue_thp_ul_values,
                rru_prb_used_ul_values,
                pee_avg_power_values
            )
            ]
        ]

        json_data = {"signature_name": "serving_default", "instances": instances}
        logger.debug(f'Generated JSON data: {json_data}')
        return json_data
    # Mapping CellID and Cell name
    def mapping(self, data):
        data = pd.DataFrame(data)
        # TODO: This regex is not likely to match all cell IDs. Will need to be improved.
        data[['S', 'B', 'C']] = data['CellID'].str.extract(r'S(\d+)-[BN](\d+)-C(\d+)')
        data[['S', 'B', 'C']] = data[['S', 'B', 'C']].astype(int)
        data = data.sort_values(by=['B', 'S', 'C'])
        data['cellidnumber'] = data.groupby(['B', 'S', 'C']).ngroup().add(1)
        data = data.drop(['S', 'B', 'C'], axis=1)
        return data


    def check_and_perform_action(self, data):
        response_obj = json.loads(data)
        predictions = response_obj.get('predictions')
        if predictions:
            for prediction in predictions:
                if all(pred < 0.04 for pred in prediction):
                    return True
                elif all(pred >= 0.04 for pred in prediction):
                    return False
        return False

    # Check if the cell is in TEIV cell inventory
    def check_cell_in_teiv(self, cell_id):
        # Check if the cell ID is in the TEIV cell inventory
        self.teiv_cells = self.teiv_client.get_nrcelldus()

        if cell_id in self.teiv_cells:
            logger.info(f"Cell {cell_id} is in the TEIV cell inventory.")
        else:
            logger.info(f"Cell {cell_id} is not in the TEIV cell inventory.")


if __name__ == "__main__":

    def str2bool(v):
        if isinstance(v, bool):
            return v
        if v.lower() in ('yes', 'true', 't', 'y', '1'):
            return True
        elif v.lower() in ('no', 'false', 'f', 'n', '0'):
            return False
        else:
            raise argparse.ArgumentTypeError('Boolean value expected.')

    parser = argparse.ArgumentParser(description="Run ESrapp with optional localdb data generation.")
    parser.add_argument("--generate_db_data", type=str2bool, default=True, help="Set to True to generate data in db.")
    parser.add_argument("--use_sme_db", type=str2bool, default=False, help="Set to True use SME url for DB.")
    parser.add_argument("--random_predictions", type=str2bool, default=False, help="Set to True to generate random predictions.")
    args = parser.parse_args()

    rapp = ESrapp(generate_db_data=args.generate_db_data, use_sme_db=args.use_sme_db, random_predictions=args.random_predictions)
    logger.debug("ES xApp starting")

    rapp.entry()
