# Threshold Control rApp

A flexible rApp that monitors InfluxDB metrics and controls equipment through SDNC based on configurable thresholds.

**DISCLAIMER:** This is a sample rApp and is not intended for production use. It was deployed for demonstration purposes
at a conference in Rome and was intended for that specific environment. Sole responsibility for the use of this rApp
rests with the user. The demo video is available on [YouTube](https://www.youtube.com/watch?v=1zN-ZZh-mwo). There is
a file intended as a guide for that Rome demo here: [Rome Demo Guide](docs/Rome-Demo.md).

## Overview

The Threshold Control rApp is designed to:

1. **Monitor Data**: Continuously query InfluxDB for specified metrics through the Service Manager gateway
2. **Evaluate Thresholds**: Compare metric values against configurable thresholds using various operators
3. **Control Equipment**: Send commands to SDNC to turn equipment on/off based on threshold evaluations
4. **Automatic Restoration**: Automatically turn equipment back on after a configurable timer period

## Key Features

- **Flexible Data Source**: Configurable InfluxDB queries with support for various data formats
- **Configurable Thresholds**: Support for multiple comparison operators (>, <, >=, <=, ==, !=)
- **Equipment Identification**: Multiple methods for extracting equipment IDs from data
- **Service Manager Integration**: All external communications go through SME for proper service discovery
- **Automatic Timer-Based Restoration**: Configurable restoration timer for turned-off equipment (default: 60 seconds)
- **Thread-Safe State Management**: Concurrent equipment state tracking with thread locks
- **Health Monitoring**: Built-in health checks and status endpoints (`/health`, `/status`)
- **Helm Configuration**: Extensive configuration through Helm values and environment variables

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   InfluxDB      │    │  Service Mgr    │    │     SDNC        │
│   (Data Source) │◄───┤   (Gateway)     ├───►│ (Equipment Ctrl)│
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              ▲
                              │
                    ┌─────────────────┐
                    │ Threshold Ctrl  │
                    │     rApp        │
                    └─────────────────┘
```

## Configuration

The rApp supports extensive configuration through Helm values and environment variables. Configuration is managed through the `config.json` file with environment variable substitution support.

### Key Configuration Sections

#### SME (Service Management & Exposure)
```json
"SME": {
  "invoker_id": "${APP_ID:threshold-control}",
  "host": "${SME_HOST:servicemanager.nonrtric.svc.cluster.local}",
  "port": "${SME_PORT:8080}",
  "influxdb_api_name": "${INFLUXDB_API_NAME:influxdb2-http}",
  "sdnc_api_name": "${SDNC_API_NAME:sdnc-rest}"
}
```

**Important:** 
- `invoker_id` is used for SME service discovery queries
- Service names must match those registered in SME
- SME discovery requires `api-invoker-id` and `api-name` query parameters

#### Data Source Configuration
```yaml
influxdb:
  url: "http://influxdb.nonrtric.svc.cluster.local:8086"
  token: "your-token"
  org: "est"
  bucket: "pm-logg-bucket"
  measurement: "ManagedElement=OAI-DU,DuFunction=0,CellId=0"
  timeRange: "5m"
  monitoredFields:
    - "DRB.MaxActiveUeDl"
    - "DRB.MaxActiveUeUl"
```

#### Threshold Configuration
```yaml
threshold:
  value: 1
  operator: "less_than"  # Options: greater_than, less_than, greater_equal, less_equal, equal, not_equal
  monitoredField: "DRB.MeanActiveUeDl"  # Single field to monitor
```

**Note:** Values from environment variables are strings and are automatically converted to appropriate types (float for threshold, int for timers).

#### Equipment Control
```yaml
equipment:
  idMethod: "measurement_full"  # How to identify equipment from data
  
sdnc:
  endpoint: "/rests/data/network-topology:network-topology/topology=topology-netconf/node=Benetel_RU/yang-ext:mount/ietf-hardware:hardware/component=benetel_RU"
  method: "PATCH"
  username: "admin"
  password: "your-password"
  componentName: "benetel_RU"
```

#### Timer Configuration
```yaml
timer:
  durationSeconds: 60  # Equipment will turn back on after 60 seconds
```

### Configuration via Helm

Configuration can be overridden during deployment via ACM instance `overrideParams`:

```json
"overrideParams": {
  "environment.appId": "DO_NOT_CHANGE_THIS_RAPP_INSTANCE_ID",
  "environment.smeDiscoveryEndpoint": "DO_NOT_CHANGE_THIS_SME_DISCOVERY_ENDPOINT",
  "threshold.value": "1",
  "threshold.operator": "less_than",
  "timer.durationSeconds": "60",
  "monitoring.pollingIntervalSeconds": "30"
}
```

See `values.yaml` for all available configuration options.

## Deployment

### Prerequisites
- Kubernetes cluster with O-RAN SMO platform deployed
- rApp Manager service exposed and accessible
- InfluxDB service registered in Service Manager (SME) as `influxdb2-http`
- SDNC service registered in Service Manager (SME) as `sdnc-rest`
- Service Manager (SME) running at `servicemanager.nonrtric.svc.cluster.local:8080`

### Steps

1. **Generate rApp Package**:
   ```bash
   ./generate.sh threshold-control-rapp/rapp-threshold-control/
   ```
   This will package the Helm chart and create `rapp-threshold-control.csar`

2. **Deploy using rApp Manager**:
   - Use the provided Postman collection (`Rome Threshold Control rApp.postman_collection.json`)
   - Or use rApp Manager REST API directly (see workflow below)

3. **Monitor Deployment**:
   ```bash
   kubectl logs -f -l app.kubernetes.io/name=threshold-control-rapp -n nonrtric
   ```

### Deployment Workflow

1. **Onboard**: Upload CSAR to rApp Manager
2. **Prime**: Initialize rApp resources (registers with SME, prepares ACM)
3. **Create Instance**: Create a specific instance with configuration
4. **Deploy**: Deploy the instance (ACM deploys Helm chart, SME registers invoker)

## API Endpoints

The rApp exposes the following endpoints:

- `GET /health` - Health check endpoint
- `GET /status` - Current status and equipment states

## Data Format Support

The rApp is designed to handle flexible InfluxDB data formats. Example supported data from your system:

```
_start                        _stop                         _time                    _value  _field              _measurement
2025-12-20T13:24:01.734515909Z 2026-01-19T13:24:01.734515909Z 2026-01-17T04:00:00Z    1       DRB.MaxActiveUeDl   ManagedElement=gNB-Eurecom-5GNRBox-00001,DuFunction=3584,CellId=0
2025-12-20T13:24:01.734515909Z 2026-01-19T13:24:01.734515909Z 2026-01-17T16:00:00Z    1       DRB.MaxActiveUeUl   ManagedElement=gNB-Eurecom-5GNRBox-00001,DuFunction=3584,CellId=0
2025-12-20T13:24:01.734515909Z 2026-01-19T13:24:01.734515909Z 2026-01-17T04:00:00Z    1       DRB.MeanActiveUeDl  ManagedElement=gNB-Eurecom-5GNRBox-00001,DuFunction=3584,CellId=0
```

Equipment IDs are extracted using configurable methods:
- **Full Measurement**: Use the complete measurement string as equipment ID (default)
  - Example: `ManagedElement=gNB-Eurecom-5GNRBox-00001,DuFunction=3584,CellId=0`
- **Measurement Parsing**: Extract specific parts from measurement string
- **Tag-based**: Use specific tag values

### Configuration Examples

**Single Field Monitoring:**
```yaml
threshold:
  value: 1
  operator: "greater_than"
  monitoredField: "DRB.MaxActiveUeDl"  # Monitor only this field
```

**Multiple Field Monitoring:**
```yaml
threshold:
  value: 5
  operator: "less_equal"
  monitoredFields:  # Monitor any of these fields
    - "DRB.MaxActiveUeDl"
    - "DRB.MaxActiveUeUl"
    - "DRB.MeanActiveUeDl"
```

**Equipment ID Configuration:**
```yaml
equipment:
  idMethod: "measurement_full"  # Use full measurement string
  # Result: "ManagedElement=gNB-Eurecom-5GNRBox-00001,DuFunction=3584,CellId=0"
```

## Timer-Based Equipment Restoration

### How the Timer Works

When a threshold is exceeded, the rApp follows this workflow:

1. **Threshold Detection**: The monitoring cycle detects that a metric value exceeds the configured threshold
2. **Equipment Shutdown**: The rApp sends a PATCH request to SDNC to enable energy saving mode
3. **Timer Activation**: A Python `threading.Timer` is started with the configured duration (default: 60 seconds)
4. **State Tracking**: Equipment state is updated to `'off'` with the scheduled turn-on time stored
5. **Automatic Restoration**: When the timer expires, the callback function `turn_on_equipment()` is automatically invoked
6. **Equipment Restart**: The rApp sends another PATCH request to SDNC to disable energy saving mode
7. **State Update**: Equipment state is updated back to `'on'` with timer cleared

### Timer Implementation Details

```python
# When threshold is exceeded:
threading.Timer(
    duration_seconds,           # e.g., 60 seconds
    self.turn_on_equipment,     # Callback function
    args=[equipment_id]         # Equipment to turn back on
).start()

# Equipment state tracking:
self.equipment_states[equipment_id] = {
    'state': 'off',
    'timer': datetime.now() + timedelta(seconds=60),  # When it will turn back on
    'reason': 'Threshold exceeded: DRB.MaxActiveUeDl=10'
}
```

### Important Timer Behaviors

- **One Timer Per Equipment**: Each piece of equipment has its own independent timer
- **Thread-Safe**: State management uses locks to prevent race conditions
- **Non-Blocking**: Timers run in separate threads, so monitoring continues normally
- **Automatic Cleanup**: Once the timer fires and equipment is turned back on, the timer is cleared
- **No Re-triggering During Off Period**: If threshold is still exceeded during the off period, no new action is taken (equipment stays off until timer expires)
- **Configurable Duration**: Timer duration can be set via environment variable `TIMER_DURATION` or Helm values

### Configuration Example

```yaml
# Helm values.yaml
timer:
  durationSeconds: 60  # Equipment will turn back on after 60 seconds
```

Or via environment variable:
```bash
export TIMER_DURATION=120  # 2 minutes
```

**For a detailed explanation of the timer mechanism with examples and timelines, see [TIMER_EXPLANATION.md](docs/TIMER_EXPLANATION.md)**

## SDNC Integration

The rApp integrates with SDNC to control equipment energy saving features. When a threshold is exceeded:

1. **Turn OFF Equipment**: Sends `PATCH` request with `"o-ran-hardware:energy-saving-enabled": true`
2. **Timer Countdown**: Waits for configured duration (default: 60 seconds)
3. **Turn ON Equipment**: After timer expires, sends `PATCH` request with `"o-ran-hardware:energy-saving-enabled": false`

### SDNC API Details

**Endpoint**: `/rests/data/network-topology:network-topology/topology=topology-netconf/node=benetel-o-ru/yang-ext:mount/ietf-hardware:hardware/component=Benetel_RU`

**Method**: `PATCH`

**Payload Example**:
```json
{
  "ietf-hardware:component": {
    "name": "Benetel_RU",
    "o-ran-hardware:energy-saving-enabled": true
  }
}
```

### Configuration

```yaml
sdnc:
  endpoint: "/rests/data/network-topology:network-topology/topology=topology-netconf/node=benetel-o-ru/yang-ext:mount/ietf-hardware:hardware/component=Benetel_RU"
  method: "PATCH"
  componentName: "Benetel_RU"  # Hardware component name
  username: USERNAME_FOR_SDNC_API
  password: PASSWORD_FOR_SDNC_API
```

## Troubleshooting

### Common Issues

1. **Service Discovery Failures**
   - Check SME connectivity: `kubectl logs servicemanager -n nonrtric`
   - Verify service registrations in SME

2. **InfluxDB Connection Issues**
   - Verify InfluxDB token and credentials
   - Check network connectivity to InfluxDB service

3. **SDNC Communication Failures**
   - Verify SDNC credentials and endpoints
   - Check SDNC service availability

4. **Deployment Issues**
   - Check ACM logs: `kubectl logs -l app=policy-clamp-runtime-acm -n onap`
   - Verify Helm chart values and templates

### Logs and Monitoring

```bash
# rApp logs
kubectl logs -f app.kubernetes.io/name=threshold-control-rapp -n nonrtric

# Service Manager logs  
kubectl logs -f servicemanager -n nonrtric

# ACM logs
kubectl logs -f -l app=policy-clamp-runtime-acm -n onap
```

## Development

### Local Development
1. Install dependencies: `pip install -r src/requirements.txt`
2. Configure `src/config.json` with local service endpoints
3. Run: `python src/main.py --config src/config.json --log-level INFO`

### Building Container
```bash
docker build -t threshold-control-rapp:latest .
```

### End-to-End Testing
Use the provided Postman collection (`Rome Threshold Control rApp.postman_collection.json`) for complete rApp lifecycle testing including deployment via rApp Manager.

## License

Licensed under the Apache License, Version 2.0.