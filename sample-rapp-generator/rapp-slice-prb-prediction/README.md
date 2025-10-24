# 5G RAN Slice PRB Prediction Rapp

Using 5G RAN Slice PRB Prediction Rapp, we can properly manage available RAN resources like PRBs and can avoid starvation of PRB by slices. And also we can avoid over-utilization of PRBs by slices.

## Directory Structure

- `src` - contains source code for the Rapp
  - `config.json` - configuration file for database, RAPP, and SME settings
  - `sme_client.py` - Service Management Environment client for service discovery
  - `models/` - directory for trained AI/ML models and scalers
- `data_generator.py` - generates data for training and testing the model
- `Dockerfile` - contains instructions to build a Docker image for the Rapp

## Data Generator (`data_generator.py`)

The `data_generator.py` script generates realistic performance monitoring data for 5G network slices, specifically designed for training and testing the PRB prediction model.

### Features

- **Multi-Slice Support**: Generates data for three main 5G slice types:
  - **eMBB (Enhanced Mobile Broadband)**: High-bandwidth services like video streaming
  - **URLLC (Ultra-Reliable Low-Latency Communication)**: Critical applications with strict latency requirements
  - **mMTC (Massive Machine Type Communications)**: IoT devices with bursty traffic patterns

- **Realistic Traffic Patterns**: Models time-based activity levels that reflect real-world usage:
  - eMBB: Low activity (0-6h), medium (6-17h), medium-high (17-23h)
  - URLLC: High activity during business hours (8-20h), low otherwise
  - mMTC: Burst patterns at 6-hour intervals (6, 12, 18) during quarter-hour marks

- **Key Performance Indicators (KPIs)**: Generates three main metrics for each slice:
  - `RRU.PrbDl.SNSSAI`: Physical Resource Block downlink usage percentage
  - `DRB.PdcpSduVolumeDL.SNSSAI`: Downlink data volume in MB
  - `RRC.ConnEstabSucc.Cause`: Number of successful RRC connections

### Usage

Run the script with interactive configuration:

```bash
python data_generator.py
```

The script will prompt for:
- **Time Range**: Start and end time for data generation (default: 2025-01-01 00:00:00 to 2025-01-02 00:00:00)
- **Interval**: Data collection interval in minutes (default: 15 minutes)
- **InfluxDB Configuration**: URL, token, organization, and bucket name

### Output

1. **InfluxDB Storage**: Performance data is automatically stored in the configured InfluxDB bucket

### Configuration

The script uses the following default configurations:

```python
# Time Configuration
DEFAULT_START = "2025-01-01 00:00:00"
DEFAULT_END = "2025-01-02 00:00:00"
DEFAULT_INTERVAL = 15  # minutes

# InfluxDB Configuration
DEFAULT_INFLUX_URL = "http://localhost:8086"
DEFAULT_INFLUX_BUCKET = "nssi_pm_bucket"
```

### Network Slice Configurations

The script generates data for 6 NSSI instances:
- 2 eMBB slices
- 2 URLLC slices  
- 2 mMTC slices

Each slice has unique identifiers and generates KPIs based on its specific traffic characteristics and activity patterns.

### Data Generation Process

1. **Activity Level Determination**: Based on the hour of day, determines traffic activity for each slice type
2. **KPI Calculation**: Generates realistic KPI values using statistical distributions (Poisson, Normal)
3. **Time Series Creation**: Creates data points at specified intervals across the time range
4. **Database Storage**: Writes data to InfluxDB using batch processing for efficiency

This generated data provides a comprehensive dataset for training the PRB prediction model with realistic network slice behavior patterns.

## Configuration (`src/config.json`)

The `config.json` file contains all necessary configuration parameters for the RAPP operation, organized into three main sections:

### Database Configuration (DB)
```json
{
  "DB": {
    "address": "http://localhost:8086",           // InfluxDB server URL
    "token": "",                                  // InfluxDB authentication token
    "org": "",                                    // InfluxDB organization name
    "bucket": "nssi_pm_bucket",                   // Database bucket name
    "time_range": "-0",                           // Time range for queries
    "measurements": "nssi_pm_bucket",             // Measurement name
    "window_size": 672,                           // Data window size for model
    "field_names": [...],                         // KPI field names to monitor
    "tag_slice_type": "sliceType",                // Tag for slice type filtering
    "tag_nssi_id": "measObjLdn"                   // Tag for NSSI identification
  }
}
```

### RAPP Configuration
```json
{
  "RAPP": {
    "interval": "1",                              // Processing interval
    "ran_nssmf_address": "http://localhost:8080", // RAN NSSMF endpoint
    "callback_uri": "http://localhost:8080/handleFileReadyNotification"
  }
}
```

### Service Management Environment (SME)
```json
{
  "SME": {
    "sme_discovery_endpoint": "http://localhost:31575/service-apis/v1/allServiceAPIs",
    "invoker_id": "6a965002-ed7c-4f69-855c-ab9196f86e61",
    "influxdb_api_name": "influxdb2-http",
    "influxdb_resource_name": "root",
    "ran_nssmf_api_name": "",
    "ran_nssmf_resource_name": ""
  }
}
```

## Service Discovery (`src/sme_client.py`)

The `sme_client.py` module implements a Service Management Environment (SME) client that enables dynamic service discovery in the O-RAN ecosystem. This component allows the RAPP to locate and connect to available services without hardcoding endpoint information.

### Key Features

- **Dynamic Service Discovery**: Automatically discovers service endpoints by querying the SME
- **O-RAN Compliance**: Follows O-RAN Service-Based Architecture (SBA) principles
- **Flexible Resource Access**: Supports discovery of specific API resources within services
- **Error Handling**: Robust error handling for network and parsing issues

### SMEClient Class

The main class provides the following functionality:

#### Initialization
```python
client = SMEClient(invoker_id, api_name, resource_name)
```

**Parameters:**
- `invoker_id`: Unique identifier for the service consumer
- `api_name`: Name of the API service to discover
- `resource_name`: Specific resource within the API to access

#### Service Discovery Process

1. **Query Construction**: Builds discovery query with API invoker ID and API name
2. **SME Request**: Sends HTTP GET request to SME discovery endpoint
3. **Response Parsing**: Extracts service information from nested JSON response
4. **URL Construction**: Builds complete HTTP URL for service access

#### Usage Example
```python
# Initialize SME client for InfluxDB service
sme_client = SMEClient(
    invoker_id="6a965002-ed7c-4f69-855c-ab9196f86e61",
    api_name="influxdb2-http",
    resource_name="root"
)

# Discover service endpoint
service_url = sme_client.discover_service()
if service_url:
    print(f"Discovered service at: {service_url}")
    # Use the service URL for API calls
else:
    print("Service discovery failed")
```

### SME Response Structure

The SME returns a nested JSON structure containing:
- `serviceAPIDescriptions`: Array of available services
- `aefProfiles`: Application Exposure Function profiles
- `versions`: API version information
- `resources`: Available resources within each version
- `interfaceDescriptions`: Network endpoint details (IP, port)