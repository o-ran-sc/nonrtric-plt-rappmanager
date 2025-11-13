# 5G RAN Slice PRB Prediction Rapp

Using 5G RAN Slice PRB Prediction Rapp, we can properly manage available RAN resources like PRBs and can avoid starvation of PRB by slices. And also we can avoid over-utilization of PRBs by slices.

## Directory Structure

- `src` - contains source code for the Rapp
  - `config.json` - configuration file for database, RAPP, and SME settings
  - `sme_client.py` - Service Management Environment client for service discovery
  - `ran_nssmf_client.py` - RAN Network Slice Subnet Management Function client
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

## RAN NSSMF Client (`src/ran_nssmf_client.py`)

The `ran_nssmf_client.py` module implements a client for interacting with the RAN Network Slice Subnet Management Function (NSSMF). This component enables the RAPP to communicate with RAN slice management services for resource optimization and slice lifecycle management.

### Key Features

- **Dynamic Service Discovery**: Uses SME to automatically discover RAN NSSMF endpoints
- **Configuration-Driven**: Loads all settings from centralized config.json file
- **O-RAN Integration**: Seamlessly integrates with O-RAN RAN architecture
- **Flexible Endpoint Management**: Supports both static and dynamic endpoint configuration

### RAN_NSSMF_CLIENT Class

The main class provides the following functionality:

#### Initialization
```python
client = RAN_NSSMF_CLIENT()
```

The client automatically loads configuration from `config.json` and initializes:
- RAN NSSMF endpoint address from RAPP configuration
- SME discovery parameters for dynamic endpoint resolution
- Service identification credentials

#### Service Discovery Integration

The client integrates with the SME client for dynamic endpoint discovery:

```python
def get_url_from_sme(self):
    sme_client = SMEClient(
        invoker_id=self.invoker_id,
        api_name=self.ran_nssmf_api_name,
        resource_name=self.ran_nssmf_resource_name
    )
    
    self.ran_nssmf_address = sme_client.discover_service()
```

#### Configuration Parameters

The client uses the following configuration sections from `config.json`:

**RAPP Configuration:**
```json
{
  "RAPP": {
    "ran_nssmf_address": "http://localhost:8080",  // Static fallback endpoint
    "callback_uri": "http://localhost:8080/handleFileReadyNotification"
  }
}
```

**SME Configuration:**
```json
{
  "SME": {
    "invoker_id": "6a965002-ed7c-4f69-855c-ab9196f86e61",
    "ran_nssmf_api_name": "",                    // API name for SME discovery
    "ran_nssmf_resource_name": ""                // Resource name for SME discovery
  }
}
```

#### Available Methods

##### subscribe_to_file_ready_notifications(callback_uri)
Subscribes to file-ready notifications from the RAN NSSMF for receiving performance data and configuration updates.

**Parameters:**
- `callback_uri` (str): The URI where the RAN NSSMF should send notifications

**Returns:**
- `requests.Response`: The response object from the POST request, or None on failure

**Example:**
```python
response = ran_client.subscribe_to_file_ready_notifications(
    "http://localhost:8080/handleFileReadyNotification"
)
if response and response.status_code == 201:
    print("Successfully subscribed to notifications")
```

##### get_network_slice_subnet(subnet_id)
Retrieves detailed information about a specific Network Slice Subnet from the RAN NSSMF. This method enables the RAPP to query slice configuration and status information for resource optimization decisions.

**Parameters:**
- `subnet_id` (str): The unique identifier of the Network Slice Subnet to retrieve

**Returns:**
- `dict`: A dictionary representing the NetworkSliceSubnetDTO if successful, None otherwise

**Example:**
```python
# Get details for a specific network slice subnet
subnet_details = ran_client.get_network_slice_subnet("9090d36f-6af5-4cfd-8bda-7a3c88fa82fa")
if subnet_details:
    print(f"Slice Name: {subnet_details.get('name')}")
    print(f"Slice Status: {subnet_details.get('operationalStatus')}")
    print(f"PRB Allocation: {subnet_details.get('prbAllocation')}")
else:
    print("Network slice subnet not found or error occurred")
```

#### Usage Example
```python
# Initialize RAN NSSMF client
ran_client = RAN_NSSMF_CLIENT()

# Discover service endpoint dynamically (if SME configured)
ran_client.get_url_from_sme()

# Use the discovered or configured endpoint
if ran_client.ran_nssmf_address:
    print(f"RAN NSSMF available at: {ran_client.ran_nssmf_address}")
    
    # Subscribe to file-ready notifications
    response = ran_client.subscribe_to_file_ready_notifications(
        "http://localhost:8080/handleFileReadyNotification"
    )
    
    # Get network slice subnet details
    subnet_info = ran_client.get_network_slice_subnet("slice-subnet-id")
    
    # Make additional API calls to RAN NSSMF for slice management
else:
    print("RAN NSSMF endpoint not available")
```

### Integration with RAPP Architecture

The RAN NSSMF client serves as a bridge between the PRB prediction system and the RAN slice management infrastructure:

1. **Resource Optimization**: Provides interface to adjust slice resource allocations based on PRB predictions
2. **Slice Lifecycle Management**: Enables programmatic control over slice creation, modification, and termination
3. **Performance Monitoring**: Facilitates collection of real-time RAN performance metrics
4. **Policy Enforcement**: Allows implementation of resource allocation policies based on ML predictions

### Error Handling and Logging

The client includes comprehensive error handling:
- Configuration validation and error reporting
- Service discovery failure handling with fallback to static endpoints
- Detailed logging for debugging and monitoring
- Graceful degradation when services are unavailable

## Model Training (Jupyter Notebook)

The `RAN_Slice_PRB_Prediction_Rapp_Model_Generator.ipynb` notebook provides a complete pipeline for training the LSTM model used for PRB prediction.

### Training Pipeline

1. **Data Fetching**: Retrieves NSSAI performance data from InfluxDB using Flux queries
2. **Data Preprocessing**: 
   - One-hot encoding for slice types and NSSI IDs
   - MinMax scaling for numerical features
   - Time series sequence creation with sliding windows
3. **Model Architecture**: LSTM neural network with dropout layers for time series prediction
4. **Training Process**: 
   - Time-based train/validation split
   - Early stopping and learning rate scheduling
   - Model checkpointing for best performance
5. **Evaluation**: Calculates MAE, RMSE, and R² scores with visualization
6. **Artifact Saving**: Stores trained models, encoders, and scalers for deployment

### Key Model Parameters

- **Window Size**: 672 time steps (approximately 7 days at 15-minute intervals)
- **Horizon**: 1-step ahead prediction
- **Features**: One-hot encoded slice types/NSSI IDs + scaled PRB/data/RRC metrics
- **Target**: PRB downlink usage percentage
- **Loss Function**: Huber loss for robust training
- **Optimizer**: Adam with learning rate scheduling

### Model Artifacts

After training, the following artifacts are saved to the `models/` directory:
- `best_prb_lstm.keras`: Best performing model checkpoint
- `final_prb_lstm.keras`: Final trained model
- `slice_onehot.joblib`: Slice type encoder
- `nssi_onehot.joblib`: NSSI ID encoder
- `scaler_*.joblib`: Feature and target scalers
- `meta.json`: Model metadata and configuration

### Usage

To train a new model:

1. Ensure InfluxDB is running and contains performance data
2. Update database connection parameters in the notebook
3. Run all cells sequentially to execute the training pipeline
4. Monitor training progress and evaluation metrics
5. Use the saved artifacts for deployment in the RAPP

## Deployment and Usage

### Prerequisites

- Python 3.8+
- InfluxDB 2.x for time series data storage
- TensorFlow/Keras for model inference
- O-RAN Service Management Environment (optional for service discovery)

### Running the Application

1. **Generate Training Data**:
   ```bash
   python data_generator.py
   ```

2. **Train ML Model**:
   ```bash
   jupyter notebook RAN_Slice_PRB_Prediction_Rapp_Model_Generator.ipynb
   ```

3. **Configure Application**:
   - Update `src/config.json` with your environment settings
   - Configure InfluxDB connection parameters
   - Set up SME endpoints if using service discovery
