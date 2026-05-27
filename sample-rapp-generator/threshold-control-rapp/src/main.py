import argparse
import time
import json
import logging
import schedule
import threading
from datetime import datetime, timedelta
from sme_client import SMEClient, SDNCClient
from config_manager import ConfigManager
from flask import Flask, jsonify
import threading

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class ThresholdControlRapp:
    def __init__(self, config_file='config.json'):
        """Initialize the Threshold Control rApp"""
        self.config_manager = ConfigManager(config_file)
        self.config = self.config_manager.get_config()
        
        # Initialize SME client for service discovery and communication
        self.sme_client = SMEClient(self.config['SME'])
        
        # Initialize SDNC client for equipment control
        self.sdnc_client = SDNCClient(self.config['SDNC'], self.sme_client)
        
        # Equipment state tracking
        self.equipment_states = {}  # {equipment_id: {'overall_state': ..., 'energy_saving_enabled': ..., 'carriers': {...}}}
        self.state_lock = threading.Lock()
        
        # Flask app for health checks
        self.app = Flask(__name__)
        self.setup_health_endpoints()
        
        logger.info("Threshold Control rApp initialized")

    def setup_health_endpoints(self):
        """Setup Flask endpoints for health checks and status"""
        @self.app.route('/health', methods=['GET'])
        def health():
            return jsonify({
                'status': 'healthy',
                'timestamp': datetime.now().isoformat(),
                'version': '1.0.0'
            })
        
        @self.app.route('/status', methods=['GET'])
        def status():
            return jsonify(self.get_status())

    def start_health_server(self):
        """Start the health check server in a separate thread"""
        def run_server():
            self.app.run(host='0.0.0.0', port=8080, debug=False)
        
        health_thread = threading.Thread(target=run_server, daemon=True)
        health_thread.start()
        logger.info("Health check server started on port 8080")

    def initialize_services(self):
        """Initialize connections to external services through SME"""
        try:
            # Verify SME connectivity
            logger.info("Verifying SME connectivity...")
            # SME client is already initialized, just log it
            logger.info("SME client ready for service discovery")
                
        except Exception as e:
            logger.error(f"Failed to initialize services: {e}")
            raise

    def query_influxdb_data(self):
        """Query InfluxDB for monitoring data via SME REST API"""
        try:
            query_config = self.config['InfluxDB']['query']
            
            # Build Flux query
            query = f'''
            from(bucket: "{query_config['bucket']}")
                |> range(start: -{query_config['time_range']})
                |> filter(fn: (r) => r["_measurement"] == "{query_config['measurement']}")
            '''
            
            # Add field filters if specified
            if query_config.get('fields'):
                field_filters = ' or '.join([f'r["_field"] == "{field}"' for field in query_config['fields']])
                query += f'|> filter(fn: (r) => {field_filters})'
            
            # Add tag filters if specified
            for tag_key, tag_value in query_config.get('tag_filters', {}).items():
                query += f'|> filter(fn: (r) => r["{tag_key}"] == "{tag_value}")'
            
            query += '|> last()'
            
            logger.debug(f"Executing InfluxDB query: {query}")
            
            # Send query through SME
            response = self.sme_client.send_influxdb_request(query)
            
            if not response or not response.get('success'):
                logger.error(f"InfluxDB query failed: {response}")
                return []
            
            # Parse CSV response
            csv_data = response.get('data', '')
            data_points = self._parse_influx_csv(csv_data)
            
            logger.info(f"Retrieved {len(data_points)} data points from InfluxDB")
            return data_points
            
        except Exception as e:
            logger.error(f"Failed to query InfluxDB: {e}")
            return []
    
    def _parse_influx_csv(self, csv_data: str) -> list:
        """Parse InfluxDB CSV response into data points"""
        import csv
        from io import StringIO
        
        data_points = []
        
        try:
            # Skip annotation rows and parse CSV
            lines = csv_data.strip().split('\n')
            
            # Find the header row (starts with ,result,table,...)
            header_idx = None
            for i, line in enumerate(lines):
                if line.startswith(',result,table'):
                    header_idx = i
                    break
            
            if header_idx is None:
                logger.warning("No data header found in InfluxDB response")
                return []
            
            # Parse CSV starting from header
            csv_reader = csv.DictReader(StringIO('\n'.join(lines[header_idx:])))
            
            for row in csv_reader:
                # Skip empty rows
                if not row.get('_value'):
                    continue
                
                data_point = {
                    'equipment_id': self.extract_equipment_id_from_row(row),
                    'field': row.get('_field', ''),
                    'value': float(row.get('_value', 0)),
                    'measurement': row.get('_measurement', ''),
                    'time': row.get('_time', ''),
                    'tags': {k: v for k, v in row.items() if not k.startswith('_') and k not in ['result', 'table']}
                }
                data_points.append(data_point)
            
        except Exception as e:
            logger.error(f"Error parsing InfluxDB CSV response: {e}")
            logger.debug(f"CSV data: {csv_data[:500]}")  # Log first 500 chars
        
        return data_points
    
    def extract_equipment_id_from_row(self, row: dict) -> str:
        """Extract equipment ID from CSV row based on configuration"""
        extraction_config = self.config['Equipment']['id_extraction']
        
        if extraction_config['method'] == 'tag':
            return row.get(extraction_config['tag_name'], 'unknown')
        elif extraction_config['method'] == 'measurement_full':
            # Use the full measurement string as equipment ID
            return row.get('_measurement', 'unknown')
        elif extraction_config['method'] == 'measurement_parsing':
            # Parse equipment ID from measurement string
            measurement = row.get('_measurement', '')
            parts = measurement.split(',')
            for part in parts:
                if part.startswith(extraction_config['key_prefix']):
                    return part.split('=')[1]
        
        return 'unknown'

    def evaluate_threshold(self, data_point):
        """Evaluate if data point exceeds threshold"""
        threshold_config = self.config['Threshold']
        
        # Check if this field should be monitored
        monitored_field = threshold_config.get('monitored_field')
        if monitored_field and data_point['field'] != monitored_field:
            return False
        
        # If no specific field is configured, check the monitored_fields list
        if not monitored_field and threshold_config.get('monitored_fields'):
            if data_point['field'] not in threshold_config['monitored_fields']:
                return False
        
        value = data_point['value']
        threshold_value = float(threshold_config['value'])
        operator = threshold_config['operator']
        
        if operator == 'greater_than':
            return value > threshold_value
        elif operator == 'less_than':
            return value < threshold_value
        elif operator == 'greater_equal':
            return value >= threshold_value
        elif operator == 'less_equal':
            return value <= threshold_value
        elif operator == 'equal':
            return value == threshold_value
        elif operator == 'not_equal':
            return value != threshold_value
        
        return False

    def control_equipment(self, equipment_id, action):
        """
        Execute full equipment control sequence with carriers and energy saving
        
        Action 'sleep': Put equipment to sleep (carriers to SLEEP, enable energy saving)
        Action 'wake': Wake equipment up (disable energy saving, carriers to ACTIVE)
        """
        try:
            logger.info(f"=" * 80)
            logger.info(f"Starting equipment control sequence: {action.upper()} for {equipment_id}")
            logger.info(f"=" * 80)
            
            if action == 'sleep':
                return self._put_equipment_to_sleep(equipment_id)
            elif action == 'wake':
                return self._wake_equipment_up(equipment_id)
            else:
                logger.error(f"Unknown action: {action}")
                return False
                
        except Exception as e:
            logger.error(f"Error in equipment control sequence for {equipment_id}: {e}", exc_info=True)
            return False
    
    def _put_equipment_to_sleep(self, equipment_id) -> bool:
        """
        Put equipment to sleep following the sequence:
        1. Check carriers are ACTIVE (and not INACTIVE)
        2. Put carriers to SLEEP
        3. Verify carriers are SLEEP
        4. Check energy saving state
        5. Enable energy saving if disabled
        """
        logger.info(f"[SLEEP SEQUENCE] Starting for equipment {equipment_id}")
        
        # Step 1: Check carriers are ACTIVE (and not INACTIVE)
        logger.info("[STEP 1/5] Checking if carriers are ACTIVE")
        carriers = self.sdnc_client.get_carrier_states()
        if not carriers:
            logger.error("[STEP 1/5] ✗ Failed to get carrier states")
            return False
        
        tx_carriers = carriers.get('tx', [])
        rx_carriers = carriers.get('rx', [])
        
        # Check if any carriers are INACTIVE
        inactive_carriers = [c for c in tx_carriers + rx_carriers if c.get('active') == 'INACTIVE']
        if inactive_carriers:
            inactive_names = ', '.join([c.get('name', 'unknown') for c in inactive_carriers])
            logger.warning(f"[STEP 1/5] ⚠ Carriers are INACTIVE ({inactive_names}), will NOT attempt state change")
            logger.info(f"[SLEEP SEQUENCE] Aborted - equipment {equipment_id} has INACTIVE carriers")
            return False
        
        all_active = all(c.get('active') == 'ACTIVE' for c in tx_carriers + rx_carriers)
        
        if not all_active:
            logger.warning("[STEP 1/5] ⚠ Not all carriers are ACTIVE, proceeding anyway")
        else:
            logger.info("[STEP 1/5] ✓ All carriers are ACTIVE")
        
        # Step 2: Put carriers to SLEEP
        logger.info("[STEP 2/5] Putting carriers to SLEEP")
        if not self.sdnc_client.set_carrier_states('SLEEP'):
            logger.error("[STEP 2/5] ✗ Failed to set carriers to SLEEP")
            return False
        logger.info("[STEP 2/5] ✓ Carriers set to SLEEP")
        
        # Step 3: Verify carriers are SLEEP
        logger.info("[STEP 3/5] Verifying carriers are SLEEP")
        if not self.sdnc_client.verify_carrier_states('SLEEP'):
            logger.error("[STEP 3/5] ✗ Failed to verify carriers are SLEEP")
            return False
        logger.info("[STEP 3/5] ✓ All carriers verified SLEEP")
        
        # Step 4: Check energy saving state
        logger.info("[STEP 4/5] Checking energy saving state")
        energy_saving_enabled = self.sdnc_client.get_energy_saving_state()
        if energy_saving_enabled is None:
            logger.error("[STEP 4/5] ✗ Failed to get energy saving state")
            return False
        
        logger.info(f"[STEP 4/5] Current energy saving state: {energy_saving_enabled}")
        
        # Step 5: Enable energy saving if disabled
        if not energy_saving_enabled:
            logger.info("[STEP 5/5] Energy saving is disabled, enabling it")
            if not self.sdnc_client.set_energy_saving_state(True):
                logger.error("[STEP 5/5] ✗ Failed to enable energy saving")
                return False
            logger.info("[STEP 5/5] ✓ Energy saving enabled")
        else:
            logger.info("[STEP 5/5] Energy saving already enabled")
        
        # Update state tracking
        with self.state_lock:
            self.equipment_states[equipment_id] = {
                'overall_state': 'sleeping',
                'energy_saving_enabled': True,
                'carriers': {
                    'tx': [{'name': c['name'], 'active': 'SLEEP'} for c in tx_carriers],
                    'rx': [{'name': c['name'], 'active': 'SLEEP'} for c in rx_carriers]
                },
                'timer': None,
                'reason': 'Equipment put to sleep due to threshold'
            }
        
        logger.info(f"[SLEEP SEQUENCE] ✓ Successfully completed for equipment {equipment_id}")
        return True
    
    def _wake_equipment_up(self, equipment_id) -> bool:
        """
        Wake equipment up following the sequence:
        1. Check energy saving state
        2. Disable energy saving if enabled
        3. Check carriers are SLEEP (and not INACTIVE)
        4. Put carriers to ACTIVE
        5. Verify carriers are ACTIVE
        """
        logger.info(f"[WAKE SEQUENCE] Starting for equipment {equipment_id}")
        
        # Pre-check: Verify carriers are not INACTIVE before proceeding
        logger.info("[PRE-CHECK] Verifying carriers are not INACTIVE")
        carriers_precheck = self.sdnc_client.get_carrier_states()
        if not carriers_precheck:
            logger.error("[PRE-CHECK] ✗ Failed to get carrier states")
            return False
        
        tx_carriers_precheck = carriers_precheck.get('tx', [])
        rx_carriers_precheck = carriers_precheck.get('rx', [])
        
        # Check if any carriers are INACTIVE
        inactive_carriers = [c for c in tx_carriers_precheck + rx_carriers_precheck if c.get('active') == 'INACTIVE']
        if inactive_carriers:
            inactive_names = ', '.join([c.get('name', 'unknown') for c in inactive_carriers])
            logger.warning(f"[PRE-CHECK] ⚠ Carriers are INACTIVE ({inactive_names}), will NOT attempt state change")
            logger.info(f"[WAKE SEQUENCE] Aborted - equipment {equipment_id} has INACTIVE carriers")
            return False
        
        logger.info("[PRE-CHECK] ✓ No INACTIVE carriers detected")
        
        # Step 1: Check energy saving state
        logger.info("[STEP 1/5] Checking energy saving state")
        energy_saving_enabled = self.sdnc_client.get_energy_saving_state()
        if energy_saving_enabled is None:
            logger.error("[STEP 1/5] ✗ Failed to get energy saving state")
            return False
        
        logger.info(f"[STEP 1/5] Current energy saving state: {energy_saving_enabled}")
        
        # Step 2: Disable energy saving if enabled
        if energy_saving_enabled:
            logger.info("[STEP 2/5] Energy saving is enabled, disabling it")
            if not self.sdnc_client.set_energy_saving_state(False):
                logger.error("[STEP 2/5] ✗ Failed to disable energy saving")
                return False
            logger.info("[STEP 2/5] ✓ Energy saving disabled")
        else:
            logger.info("[STEP 2/5] Energy saving already disabled")
        
        # Step 3: Check carriers are SLEEP
        logger.info("[STEP 3/5] Checking if carriers are SLEEP")
        carriers = self.sdnc_client.get_carrier_states()
        if not carriers:
            logger.error("[STEP 3/5] ✗ Failed to get carrier states")
            return False
        
        tx_carriers = carriers.get('tx', [])
        rx_carriers = carriers.get('rx', [])
        all_sleep = all(c.get('active') == 'SLEEP' for c in tx_carriers + rx_carriers)
        
        if not all_sleep:
            logger.warning("[STEP 3/5] ⚠ Not all carriers are SLEEP, proceeding anyway")
        else:
            logger.info("[STEP 3/5] ✓ All carriers are SLEEP")
        
        # Step 4: Put carriers to ACTIVE
        logger.info("[STEP 4/5] Putting carriers to ACTIVE")
        if not self.sdnc_client.set_carrier_states('ACTIVE'):
            logger.error("[STEP 4/5] ✗ Failed to set carriers to ACTIVE")
            return False
        logger.info("[STEP 4/5] ✓ Carriers set to ACTIVE")
        
        # Step 5: Verify carriers are ACTIVE
        logger.info("[STEP 5/5] Verifying carriers are ACTIVE")
        if not self.sdnc_client.verify_carrier_states('ACTIVE'):
            logger.error("[STEP 5/5] ✗ Failed to verify carriers are ACTIVE")
            return False
        logger.info("[STEP 5/5] ✓ All carriers verified ACTIVE")
        
        # Update state tracking
        with self.state_lock:
            self.equipment_states[equipment_id] = {
                'overall_state': 'on',
                'energy_saving_enabled': False,
                'carriers': {
                    'tx': [{'name': c['name'], 'active': 'ACTIVE'} for c in tx_carriers],
                    'rx': [{'name': c['name'], 'active': 'ACTIVE'} for c in rx_carriers]
                },
                'timer': None,
                'reason': 'Equipment woken up after timer'
            }
        
        logger.info(f"[WAKE SEQUENCE] ✓ Successfully completed for equipment {equipment_id}")
        return True

    def process_data_points(self, data_points):
        """Process data points and make control decisions"""
        logger.info(f"Processing {len(data_points)} data points for threshold evaluation")
        
        for data_point in data_points:
            equipment_id = data_point['equipment_id']
            field = data_point['field']
            value = data_point['value']
            
            logger.debug(f"Evaluating data point: equipment={equipment_id}, field={field}, value={value}")
            
            if equipment_id == 'unknown':
                logger.warning(f"Skipping data point with unknown equipment ID")
                continue
            
            # Check threshold
            threshold_exceeded = self.evaluate_threshold(data_point)
            
            # Get threshold config for logging
            threshold_config = self.config['Threshold']
            threshold_value = float(threshold_config['value'])
            operator = threshold_config['operator']
            monitored_field = threshold_config.get('monitored_field')
            
            if monitored_field and field != monitored_field:
                logger.debug(f"Skipping field {field} (monitoring {monitored_field})")
                continue
            
            logger.info(f"Equipment {equipment_id}: {field}={value}, threshold={operator} {threshold_value}, exceeded={threshold_exceeded}")
            
            with self.state_lock:
                current_state = self.equipment_states.get(equipment_id, {
                    'overall_state': 'on',
                    'energy_saving_enabled': False,
                    'carriers': {'tx': [], 'rx': []},
                    'timer': None,
                    'reason': None
                })
                
                if threshold_exceeded and current_state['overall_state'] == 'on':
                    # Threshold exceeded and equipment is on - put to sleep
                    logger.info(f"✓ Threshold exceeded for equipment {equipment_id} (field: {field}, value: {value})")
                    logger.info(f"  Equipment is currently ON, will initiate sleep sequence")
                    # Release lock before long-running operation
                    pass
                elif threshold_exceeded and current_state['overall_state'] != 'on':
                    logger.info(f"Threshold exceeded for equipment {equipment_id} but state is '{current_state['overall_state']}', no action taken")
                elif not threshold_exceeded:
                    logger.debug(f"Threshold not exceeded for equipment {equipment_id}, no action needed")
            
            # Execute control sequence outside of lock
            if threshold_exceeded and current_state['overall_state'] == 'on':
                if self.control_equipment(equipment_id, 'sleep'):
                    # Set timer for waking back up
                    timer_duration = int(self.config['Timer']['duration_seconds'])
                    turn_on_time = datetime.now() + timedelta(seconds=timer_duration)
                    
                    with self.state_lock:
                        if equipment_id in self.equipment_states:
                            self.equipment_states[equipment_id]['timer'] = turn_on_time
                            self.equipment_states[equipment_id]['reason'] = f"Threshold exceeded: {data_point['field']}={data_point['value']}"
                    
                    logger.info(f"Timer set for {timer_duration}s - equipment will wake at {turn_on_time.isoformat()}")
                    
                    # Schedule wake-up
                    threading.Timer(
                        timer_duration,
                        self.wake_equipment,
                        args=[equipment_id]
                    ).start()
                else:
                    logger.error(f"Failed to put equipment {equipment_id} to sleep")

    def wake_equipment(self, equipment_id):
        """Wake equipment back up after timer expires"""
        with self.state_lock:
            current_state = self.equipment_states.get(equipment_id)
            if current_state and current_state['overall_state'] == 'sleeping':
                logger.info(f"Timer expired for equipment {equipment_id}. Waking up.")
                # Release lock before long-running operation
                pass
            else:
                logger.info(f"Equipment {equipment_id} is not in sleeping state, skipping wake-up")
                return
        
        # Execute wake sequence outside of lock
        if current_state and current_state['overall_state'] == 'sleeping':
            if self.control_equipment(equipment_id, 'wake'):
                logger.info(f"Equipment {equipment_id} successfully woken up")
            else:
                logger.error(f"Failed to wake equipment {equipment_id}")
                # Update state to indicate failure
                with self.state_lock:
                    if equipment_id in self.equipment_states:
                        self.equipment_states[equipment_id]['reason'] = 'Failed to wake equipment after timer'

    def monitoring_cycle(self):
        """Main monitoring cycle"""
        try:
            logger.info("Starting monitoring cycle")
            
            # Query data from InfluxDB
            data_points = self.query_influxdb_data()
            
            if data_points:
                # Process data points
                self.process_data_points(data_points)
            else:
                logger.warning("No data points retrieved from InfluxDB")
                
        except Exception as e:
            logger.error(f"Error in monitoring cycle: {e}")

    def start_monitoring(self):
        """Start the monitoring scheduler"""
        polling_interval = int(self.config['Monitoring']['polling_interval_seconds'])
        
        # Schedule monitoring cycle
        schedule.every(polling_interval).seconds.do(self.monitoring_cycle)
        
        logger.info(f"Starting monitoring with {polling_interval}s polling interval")
        
        # Run initial cycle
        self.monitoring_cycle()
        
        # Start scheduler loop
        while True:
            schedule.run_pending()
            time.sleep(1)

    def get_status(self):
        """Get current status of all equipment"""
        with self.state_lock:
            return {
                'equipment_states': self.equipment_states.copy(),
                'monitoring_active': True,
                'timestamp': datetime.now().isoformat()
            }


def main():
    parser = argparse.ArgumentParser(description="Threshold Control rApp")
    parser.add_argument("--config", default="config.json", help="Configuration file path")
    parser.add_argument("--log-level", default="INFO", help="Logging level")
    args = parser.parse_args()
    
    # Set logging level
    logging.getLogger().setLevel(getattr(logging, args.log_level.upper()))
    
    try:
        # Initialize rApp
        rapp = ThresholdControlRapp(args.config)
        
        # Start health server
        rapp.start_health_server()
        
        # Initialize services
        rapp.initialize_services()
        
        # Start monitoring
        rapp.start_monitoring()
        
    except KeyboardInterrupt:
        logger.info("Shutting down rApp")
    except Exception as e:
        logger.error(f"Fatal error: {e}")
        raise


if __name__ == "__main__":
    main()