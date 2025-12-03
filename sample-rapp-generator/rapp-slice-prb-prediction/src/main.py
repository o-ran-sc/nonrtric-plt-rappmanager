import argparse
import os
from data import DATABASE

from threading import Lock
import logging

from joblib import load

import tensorflow as tf

import numpy as np
import pandas as pd
import json

from typing import Optional

from ran_nssmf_client import RAN_NSSMF_CLIENT
from flask import Flask, request, jsonify

logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

# Initialize Flask App
app = Flask(__name__)

class SlicePRBPrediction():
    def __init__(self, use_sme=False):
        self.interval = None

        # Initialize the database and prediction client
        self.db = DATABASE()
        self.ran_nssmf_client = RAN_NSSMF_CLIENT()

        if use_sme:
            # Get the InfluxDB URL from SME
            self.db.get_url_from_sme()
            # self.ran_nssmf_client.get_url_from_sme()

        self.db.connect()

        self.inference_lock = Lock()
        self._running = False

        self.features = ["sliceType_enc", "RRU.PrbDl.SNSSAI","DRB.PdcpSduVolumeDL.SNSSAI","RRC.ConnEstabSucc.Cause"]

        self.enc = load(os.path.join("models", "slice_onehot.joblib"))
        self.nssi_enc = load(os.path.join("models", "nssi_onehot.joblib"))
        self.scalers = {
            "prb": load(os.path.join("models", "scaler_prb.joblib")),
            "data": load(os.path.join("models", "scaler_data.joblib")),
            "rrc": load(os.path.join("models", "scaler_rrc.joblib")),
            "y": load(os.path.join("models", "scaler_y.joblib"))
        }
        self.model = tf.keras.models.load_model(os.path.join("models", "best_prb_lstm.keras"))

        self.callback_uri = None

        self.config()

    def config(self):

        with open('config.json', 'r') as f:
            config = json.load(f)

        # Load RAPP configuration, with a default interval of 10 seconds
        rapp_config = config.get("RAPP", {}) # Renamed for clarity

        # Get interval from config, or use 10 as a default if not found
        interval_str = rapp_config.get("interval", "672") # Default to "10" string
        self.interval = int(interval_str)
        self.callback_uri = rapp_config.get("callback_uri", "http://localhost:8080/handleFileReadyNotification")

    def subscribe_to_notifications(self):
        # This method will be called after the app is created to subscribe to notifications
        # The callback URI must point to this running Flask app's endpoint
        # Assuming the rApp will be accessible on port 8080
        
        logger.info(f"Attempting to subscribe to RAN NSSMF notifications with callback: {self.callback_uri}")
        response = self.ran_nssmf_client.subscribe_to_file_ready_notifications(self.callback_uri)
        if response and response.status_code == 201:
            logger.info("Successfully subscribed to RAN NSSMF notifications.")
            # You might want to store the subscription ID or location from response.headers
        else:
            logger.error(f"Failed to subscribe to RAN NSSMF notifications. Status: {response.status_code if response else 'N/A'}")
            if response:
                logger.error(f"Response: {response.text}")
    
    def safe_inference(self):
        if not self.inference_lock.acquire(blocking=False):
            logger.warning("Previous inference still running, skipping this iteration")
            return

        try:
            self.inference()
        finally:
            self.inference_lock.release()

    def inference(self):
        logger.info("Starting inference process...")
        df = self.db.read_data()

        # Standardize column names
        df = df.rename(columns={
        "_time": "time",
        "measObjLdn": "nssi_id",
        "sliceType": "slice_type",
        "RRU.PrbDl.SNSSAI": "prb_dl",
        "DRB.PdcpSduVolumeDL.SNSSAI": "data_dl",
        "RRC.ConnEstabSucc.Cause": "rrc_succ"
        })

        if df.empty:
            logger.info("No data to process... skipping this iteration of inference.")
            return

        # Ensure types
        df["time"] = pd.to_datetime(df["time"], utc=True)
        df = df.sort_values(["slice_type", "nssi_id", "time"]).reset_index(drop=True)

        # Drop rows with any NA in core columns
        df = df.dropna(subset=["slice_type", "nssi_id", "time", "prb_dl", "data_dl", "rrc_succ"])

        target_groups = sorted(df[["slice_type", "nssi_id"]].drop_duplicates().values.tolist())
        results = []
        for st, nssi in target_groups:
            X_latest = self.build_window_for_latest_slice(
            df, 
            target_slice_type=st, 
            target_nssi_id=nssi,
            window=self.db.window_size
            )
            if X_latest is None:
                logger.warning(f"Not enough recent points for slice_type='{st}' and nssi='{nssi}' to build a window of {self.db.window_size}. Skipping.")
                continue
        
            y_pred_scaled = self.model.predict(X_latest).reshape(-1, 1)
            y_pred = self.scalers["y"].inverse_transform(y_pred_scaled)[0, 0]
            results.append({"slice_type": st, "nssi_id": nssi, "predicted_prb_dl_next": float(y_pred)})

            # Fetch NSSI details from RAN NSSMF simulator
            logger.info(f"Fetching details for NSSI ID: {nssi} from RAN NSSMF simulator.")
            nssi_details = self.ran_nssmf_client.get_network_slice_subnet(subnet_id=nssi)
            if nssi_details:
                logger.info(f"Successfully fetched details for NSSI ID {nssi}.")
                # logger.debug(f"NSSI Details: {json.dumps(nssi_details, indent=2)}") # Uncomment for verbose details

                # Extract current_prb_dl from nssi_details
                # Path: nssi_details["attributes"]["sliceProfileList"][0]["ransliceSubnetProfile"]["RRU.PrbDl"]
                try:
                    current_prb_dl = nssi_details.get("attributes", {}) \
                                                .get("sliceProfileList", [{}])[0] \
                                                .get("ransliceSubnetProfile", {}) \
                                                .get("RRU.PrbDl")
                    
                    if current_prb_dl is not None:
                        logger.info(f"Current PRB DL for NSSI ID {nssi}: {current_prb_dl}. Predicted PRB DL: {y_pred:.2f}")
                        if current_prb_dl < y_pred:
                            logger.info(f"Current PRB DL ({current_prb_dl}) is less than predicted ({y_pred:.2f}). Sending modification request.")
                            modification_response = self.ran_nssmf_client.modify_network_slice_subnet(
                                subnet_id=nssi, 
                                new_prb_dl=int(y_pred) # Cast to int as RRU.PrbDl is an integer
                            )
                            if modification_response:
                                logger.info(f"Successfully sent modification request for NSSI ID {nssi}. Status: {modification_response.status_code}")
                            else:
                                logger.warning(f"Failed to send modification request for NSSI ID {nssi}.")
                        else:
                            logger.info(f"Current PRB DL ({current_prb_dl}) is not less than predicted ({y_pred:.2f}). No modification needed.")
                    else:
                        logger.warning(f"Could not find RRU.PrbDl in NSSI details for NSSI ID {nssi}.")
                except (IndexError, TypeError) as e:
                    logger.error(f"Error parsing RRU.PrbDl from NSSI details for NSSI ID {nssi}: {e}. NSSI Details: {json.dumps(nssi_details, indent=2)}")

            else:
                logger.warning(f"Failed to fetch details for NSSI ID: {nssi}.")
        
        logger.info(f"Inference results: {json.dumps({'results': results}, indent=2)}")
        
    
    def build_window_for_latest_slice(
        self,
        df: pd.DataFrame,
        target_slice_type: str,
        target_nssi_id: str,
        window: int
    ) -> Optional[np.ndarray]:
        g = df[(df["slice_type"] == target_slice_type) & (df["nssi_id"] == target_nssi_id)].sort_values("time").reset_index(drop=True)
        if len(g) < window:
            return None

        oh = self.enc.transform(np.array([[target_slice_type]]))
        oh_row = np.repeat(oh, window, axis=0)

        nssi_oh = self.nssi_enc.transform(np.array([[target_nssi_id]]))
        nssi_oh_row = np.repeat(nssi_oh, window, axis=0)

        prb = self.scalers["prb"].transform(g[["prb_dl"]].iloc[-window:])
        data = self.scalers["data"].transform(g[["data_dl"]].iloc[-window:])
        rrc = self.scalers["rrc"].transform(g[["rrc_succ"]].iloc[-window:])

        feat = np.concatenate([oh_row, nssi_oh_row, prb, data, rrc], axis=1).astype(np.float32)
        return feat[np.newaxis, :, :]  # shape (1, window, features)

# Global instance of SlicePRBPrediction to be used by Flask routes
# This instance will be initialized after parsing arguments.
rapp_instance = None

@app.route('/handleFileReadyNotification', methods=['POST'])
def handle_file_ready_notification():
    logger.info("Received POST request on /handleFileReadyNotification")
    if not rapp_instance:
        logger.error("rapp_instance not initialized. Cannot process notification.")
        return jsonify({"status": "error", "message": "Application not properly initialized"}), 500

    notification_data = request.get_json()
    if not notification_data:
        logger.warning("No JSON data received in notification.")
        return jsonify({"status": "error", "message": "Invalid JSON payload"}), 400

    logger.info(f"Notification received: {json.dumps(notification_data, indent=2)}")

    # Trigger the inference process
    # The notification_data (e.g., fileInfoList) could be used to tailor the inference
    # For now, we'll trigger the general inference process.
    try:
        rapp_instance.safe_inference()
        logger.info("Inference process triggered successfully by notification.")
        return jsonify({"status": "success", "message": "Notification received and inference triggered"}), 200
    except Exception as e:
        logger.error(f"Error during inference triggered by notification: {str(e)}", exc_info=True)
        return jsonify({"status": "error", "message": f"Inference failed: {str(e)}"}), 500

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

    parser = argparse.ArgumentParser(description="Run SlicePRBPrediction rApp")
    parser.add_argument("--use_sme", type=str2bool, default=False, help="Set to True use SME url for DB.")
    args = parser.parse_args()

    # Instantiate the SlicePRBPrediction class
    rapp_instance = SlicePRBPrediction(use_sme=args.use_sme)
    logger.debug("Slice PRB Prediction rApp initialized")

    # Subscribe to RAN NSSMF notifications at startup
    if rapp_instance:
        rapp_instance.subscribe_to_notifications()
    else:
        logger.error("rapp_instance not initialized. Cannot subscribe to notifications.")

    # Run the Flask app
    # The host is set to '0.0.0.0' to make it accessible from outside the container (if applicable)
    # The port is set to 8080 as per the callback URI used in subscription.
    logger.info("Starting Flask server on port 8080...")
    app.run(host='0.0.0.0', port=8080)
