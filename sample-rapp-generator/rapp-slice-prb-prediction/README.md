# 5G RAN Slice PRB Prediction Rapp

Using 5G RAN Slice PRB Prediction Rapp, we can properly manage available RAN resources like PRBs and can avoid starvation of PRB by slices. And also we can avoid over-utilization of PRBs by slices.

## Directory Structure

- `src` - contains source code for the Rapp
  - `config.json` - configuration file for database, RAPP, and SME settings
  - `data.py` - database configuration and management class for InfluxDB connections
  - `sme_client.py` - Service Management Environment client for service discovery
  - `ran_nssmf_client.py` - RAN Network Slice Subnet Management Function client
  - `models/` - directory for trained AI/ML models and scalers
- `data_generator.py` - generates data for training and testing the model
- `slice-prb-prediction-rapp/` - Kubernetes deployment artifacts
  - `Artifacts/Deployment/HELM/slice-prb-prediction-rapp/` - Helm chart for containerized deployment
    - `Chart.yaml` - Helm chart metadata
    - `values.yaml` - Default configuration values
    - `templates/` - Kubernetes resource templates
      - `deployment.yaml` - Pod deployment configuration
      - `service.yaml` - Service exposure configuration
      - `configmap.yaml` - Configuration management
      - `serviceaccount.yaml` - Service account configuration

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

## Database Management (`src/data.py`)

The `data.py` module implements a comprehensive database configuration and management class that centralizes all InfluxDB connection settings and configuration loading for the RAPP. This component provides a unified interface for database operations and configuration management with enhanced connection handling and robust error management.

**Key Import Enhancements:**
- **`influxdb_client`**: Direct import of the InfluxDB client library for database connectivity
- **`time`**: Added for implementing retry logic and connection delay mechanisms
- **`RequestException`**: Specific exception handling for HTTP and network-related errors

### Key Features

- **Centralized Configuration**: Loads all database and SME settings from a single config.json file
- **Environment Variable Support**: Supports INFLUX_TOKEN environment variable for secure credential management
- **Flexible Configuration**: Handles both static configuration and dynamic service discovery parameters
- **Pandas Integration**: Configures pandas display options for optimal data visualization
- **Connection Management**: Robust InfluxDB connection handling with automatic reconnection and retry logic
- **Error Handling**: Comprehensive error handling for configuration loading, connection issues, and database operations
- **Version Verification**: Automatic InfluxDB server version detection for compatibility checking

### DATABASE Class

The main class provides the following functionality:

#### Initialization
```python
db = DATABASE()
```

The class automatically initializes all configuration parameters from `config.json` and sets up pandas display options for better data visualization.

#### Configuration Loading Process

1. **JSON Configuration Loading**: Reads and parses the `config.json` file
2. **SME Configuration**: Extracts Service Management Environment settings
3. **InfluxDB Configuration**: Loads database connection parameters
4. **Environment Variable Override**: Checks for INFLUX_TOKEN environment variable
5. **Parameter Validation**: Converts string parameters to appropriate data types
6. **Pandas Configuration**: Sets up display options for data analysis

#### Configuration Parameters

The DATABASE class loads the following configuration sections from `config.json`:

**SME Configuration:**
```python
self.invoker_id = sme_config.get("invoker_id")
self.influx_api_name = sme_config.get("influxdb_api_name")
self.influx_resource_name = sme_config.get("influxdb_resource_name")
```

**Database Configuration:**
```python
self.address = influx_config.get("address")           # InfluxDB server URL
self.token = influx_config.get("token")               # Authentication token
self.org = influx_config.get("org")                   # Organization name
self.bucket = influx_config.get("bucket")             # Database bucket
self.time_range = influx_config.get("time_range")     # Query time range
self.measurements = influx_config.get("measurements") # Measurement name
self.window_size = int(influx_config.get("window_size", "900"))  # Data window size
self.field_names = influx_config.get("field_names")   # KPI field names
self.tag_slice_type = influx_config.get("tag_slice_type")    # Slice type tag
self.tag_nssi_id = influx_config.get("tag_nssi_id")          # NSSI ID tag
```

#### Environment Variable Support

The class supports secure credential management through environment variables:

```python
if os.getenv('INFLUX_TOKEN'):
    self.token = os.getenv('INFLUX_TOKEN')
else:
    logger.info("INFLUX_TOKEN environment variable is not set.")
    self.token = influx_config.get("token")
```

#### Pandas Display Configuration

The class automatically configures pandas for optimal data visualization:

```python
pd.set_option('display.max_rows', None)        # Show all rows
pd.set_option('display.max_columns', None)     # Show all columns
pd.set_option('display.width', 1000)           # Adjust width to avoid line breaks
pd.set_option('display.colheader_justify', 'left')  # Align headers left
```

#### Connection Management

The DATABASE class provides robust connection management with automatic reconnection and error handling:

```python
# Initialize database configuration
db = DATABASE()

# Establish connection to InfluxDB
if db.connect():
    logger.info("Successfully connected to InfluxDB")
    # Perform database operations
else:
    logger.error("Failed to connect to InfluxDB")
```

**Error Handling:**
- Catches `RequestException` and `ConnectionError` for network-related issues
- Provides detailed error logging for troubleshooting
- Implements automatic retry delay to handle temporary network issues
- Logs InfluxDB version information for debugging compatibility issues

#### Data Query Methods

##### read_data()
The `read_data()` method enables fetching performance monitoring data from InfluxDB using Flux query language. This method constructs dynamic queries based on configuration parameters and retrieves time-series data for PRB prediction analysis.

**Usage Example:**
```python
# Initialize database and connect
db = DATABASE()
if db.connect():
    # Fetch performance data
    data = db.read_data()
    # Process the retrieved data for model training or prediction
    print(f"Retrieved {len(data)} records")
else:
    logger.error("Cannot fetch data - database connection failed")
```

**Expected Data Structure:**
The method returns a pandas DataFrame with the following structure:
- `_time`: Timestamp of the measurement
- `{tag_slice_type}`: Slice type (eMBB, URLLC, mMTC)
- `{tag_nssi_id}`: Network Slice Subnet Instance identifier
- KPI fields as configured in `field_names` (PRB usage, data volume, RRC connections)

##### get_url_from_sme()
The `get_url_from_sme()` method enables dynamic discovery of InfluxDB endpoint through the Service Management Environment (SME). This method allows the DATABASE class to automatically locate and configure the InfluxDB connection without hardcoded endpoints.

**Usage Example:**
```python
# Initialize database and discover InfluxDB endpoint
db = DATABASE()
db.get_url_from_sme()

if db.address:
    logger.info(f"Discovered InfluxDB at: {db.address}")
    # Connect and use the discovered endpoint
    if db.connect():
        data = db.read_data()
```

##### query()
The `query()` method provides robust database query execution with automatic retry logic for handling temporary connection failures. This method ensures reliable data retrieval by implementing exponential backoff retry mechanism.

**Features:**
- **Automatic Retry**: Retries failed queries with 60-second delays
- **Error Handling**: Catches `RequestException` and `ConnectionError` for network issues
- **Logging**: Detailed error logging for troubleshooting connection issues
- **Data Frame Return**: Returns results as pandas DataFrame for easy analysis

**Usage Example:**
```python
# Execute custom query with retry logic
db = DATABASE()
if db.connect():
    query = 'from(bucket:"test") |> range(start:-1h)'
    result = db.query(query)
    # Process query results
```

#### Integration with RAPP Architecture

The DATABASE class serves as the foundation for all data operations in the RAPP:

1. **Model Training**: Provides configuration for fetching training data from InfluxDB
2. **Real-time Prediction**: Supplies connection parameters for live data queries
3. **Performance Monitoring**: Enables KPI data collection and analysis
4. **Service Discovery**: Supports SME integration for dynamic endpoint resolution

#### Benefits

- **Centralized Management**: All database configuration in one place
- **Security**: Support for environment variable-based credential management
- **Flexibility**: Handles both development and production deployment scenarios
- **Maintainability**: Easy to update and extend configuration parameters
- **Consistency**: Ensures all components use the same configuration source

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
- `interfaceDescriptions`: Network endpoint details (IP, port securityMethods)

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

##### modify_network_slice_subnet(subnet_id, new_prb_dl)
Modifies the RRU.PrbDl (Physical Resource Block Downlink) value of an existing Network Slice Subnet in the RAN NSSMF. This method enables dynamic resource allocation adjustments based on PRB predictions and optimization algorithms.

**Parameters:**
- `subnet_id` (str): The unique identifier of the Network Slice Subnet to modify
- `new_prb_dl` (int): The new RRU.PrbDl value to set (represents PRB allocation percentage)

**Returns:**
- `requests.Response`: The response object from the PUT request if successful, None otherwise

**Method Workflow:**
1. Fetches the current network slice subnet data using `get_network_slice_subnet()`
2. Validates the data structure and locates the RRU.PrbDl field
3. Updates the RRU.PrbDl value within the slice profile
4. Sends a PUT request to the RAN NSSMF with the modified configuration
5. Handles various error conditions (404, connection issues, malformed data)

**Example:**
```python
# Modify PRB allocation for a specific network slice subnet
response = ran_client.modify_network_slice_subnet(
    "9090d36f-6af5-4cfd-8bda-7a3c88fa82fa", 
    new_prb_dl=75
)
if response and response.status_code == 200:
    print("Successfully modified PRB allocation")
else:
    print("Failed to modify PRB allocation")

# Example usage in PRB optimization workflow
predicted_prb_usage = model.predict_next_prb_usage()
if predicted_prb_usage > threshold:
    # Increase PRB allocation to prevent congestion
    new_allocation = min(current_allocation + 10, 100)
    ran_client.modify_network_slice_subnet(slice_id, new_allocation)
elif predicted_prb_usage < low_threshold:
    # Decrease PRB allocation to free up resources
    new_allocation = max(current_allocation - 5, 10)
    ran_client.modify_network_slice_subnet(slice_id, new_allocation)
```

**Data Structure Path:**
The method updates the RRU.PrbDl value at the following path in the NetworkSliceSubnetDTO:
```
attributes.sliceProfileList[0].ransliceSubnetProfile.RRU.PrbDl
```

**Error Handling:**
- Validates RAN NSSMF address configuration
- Handles 404 errors for non-existent subnet IDs
- Provides detailed logging for debugging data structure issues
- Includes comprehensive exception handling for network and parsing errors

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

## Main Application (`src/main.py`)

The `main.py` file implements a comprehensive Flask-based web application that provides real-time PRB prediction and automated resource optimization for 5G RAN slices. This application serves as the core runtime component that integrates machine learning predictions with RAN network management.

### Key Features

- **Real-time PRB Prediction**: Uses trained LSTM models to predict future PRB demand for each network slice
- **Automated Resource Optimization**: Automatically adjusts slice PRB allocations based on prediction results
- **Flask Web Service**: Provides RESTful API endpoints for external integration and notification handling
- **RAN NSSMF Integration**: Seamlessly integrates with RAN Network Slice Subnet Management Function for slice control
- **Thread-Safe Operations**: Implements locking mechanisms to prevent concurrent inference conflicts
- **Service Discovery Support**: Optional SME integration for dynamic endpoint discovery
- **Comprehensive Logging**: Detailed logging for monitoring, debugging, and operational visibility

### Architecture Overview

The application follows a modular architecture with the following key components:

#### SlicePRBPrediction Class

The main prediction engine that orchestrates the entire PRB prediction and optimization workflow.

**Initialization Parameters:**
- `use_sme` (bool): Enable Service Management Environment for dynamic service discovery

**Core Components:**
- **Database Integration**: Uses `DATABASE` class for InfluxDB connectivity and data retrieval
- **RAN NSSMF Client**: Integrates with `RAN_NSSMF_CLIENT` for slice management operations
- **ML Model Loading**: Loads pre-trained LSTM models and preprocessing artifacts
- **Configuration Management**: Centralized configuration loading from `config.json`

#### Flask Web Service

The application exposes a RESTful API endpoint for handling notifications and triggering inference:

**Endpoint: `POST /handleFileReadyNotification`**
- Receives notifications from RAN NSSMF and external systems
- Triggers the PRB prediction and optimization workflow
- Returns JSON responses with operation status

### Core Workflow

#### 1. Initialization Process
```python
# Initialize the prediction application
rapp = SlicePRBPrediction(use_sme=True)

# Automatic initialization includes:
# - Database connection setup
# - Model and preprocessing artifacts loading
# - RAN NSSMF client configuration
# - Service discovery (if enabled)
```

#### 2. Notification-Driven Inference
The application operates on a notification-driven model:

1. **Notification Reception**: Receives HTTP POST notifications at `/handleFileReadyNotification`
2. **Data Retrieval**: Fetches latest performance data from InfluxDB
3. **Prediction Execution**: Runs LSTM model inference for each network slice
4. **Resource Optimization**: Compares predictions with current allocations and adjusts as needed
5. **Response Generation**: Returns operation status and results

#### 3. Prediction Pipeline

**Data Processing:**
- Retrieves time-series performance data from InfluxDB
- Standardizes column names and data types
- Filters and sorts data by slice type and NSSI ID
- Handles missing data and edge cases

**Feature Engineering:**
- One-hot encoding for slice types and NSSI IDs
- MinMax scaling for numerical features (PRB, data volume, RRC connections)
- Time series window creation with configurable window size
- Feature concatenation for model input preparation

**Model Inference:**
- LSTM neural network prediction for each slice
- Inverse transformation of scaled predictions
- Confidence interval estimation (optional)

#### 4. Automated Resource Optimization

**Decision Logic:**
```python
# Compare predicted vs current PRB usage
if predicted_prb > current_prb_allocation:
    # Increase allocation to prevent congestion
    modify_network_slice_subnet(nssi_id, new_prb_allocation=int(predicted_prb))
else:
    # Current allocation sufficient, no action needed
    logger.info("No modification required")
```

**Optimization Features:**
- Proactive resource allocation based on ML predictions
- Automatic slice configuration updates via RAN NSSMF
- Safety thresholds to prevent over-allocation
- Detailed logging of all optimization actions

### Configuration

The application uses `src/config.json` for runtime configuration:

```json
{
  "RAPP": {
    "interval": "672",                                    // Processing interval
    "ran_nssmf_address": "http://localhost:8080",         // RAN NSSMF endpoint
    "callback_uri": "http://localhost:8080/handleFileReadyNotification"
  },
  "DB": {
    "window_size": 672,                                   // Time series window size
    "field_names": ["RRU.PrbDl.SNSSAI", "DRB.PdcpSduVolumeDL.SNSSAI", "RRC.ConnEstabSucc.Cause"]
  }
}
```

### Model Artifacts

The application loads the following ML artifacts from the `models/` directory:

- `best_prb_lstm.keras`: Trained LSTM model for PRB prediction
- `slice_onehot.joblib`: One-hot encoder for slice types
- `nssi_onehot.joblib`: One-hot encoder for NSSI IDs
- `scaler_*.joblib`: Feature scaling transformers for different metrics
- `scaler_y.joblib`: Target variable scaler for prediction inverse transformation

### API Endpoints

#### POST /handleFileReadyNotification

Receives notifications and triggers the PRB prediction workflow.

**Request Format:**
```json
{
  "fileInfoList": [
    {
      "fileId": "performance_data_12345",
      "fileSize": 1024000,
      "fileLocation": "http://data-server/files/perf_data.csv"
    }
  ]
}
```

**Response Format:**
```json
{
  "status": "success",
  "message": "Notification received and inference triggered"
}
```

**Error Responses:**
```json
{
  "status": "error",
  "message": "Application not properly initialized"
}
```

### Running the Application

#### Command Line Options

```bash
# Run with static configuration
python src/main.py

# Run with Service Management Environment discovery
python src/main.py --use_sme True

# Run without SME (default)
python src/main.py --use_sme False
```

#### Startup Process

1. **Argument Parsing**: Parse command line arguments for SME configuration
2. **Application Initialization**: Create `SlicePRBPrediction` instance
3. **Service Discovery**: Optionally discover endpoints via SME
4. **Notification Subscription**: Subscribe to RAN NSSMF notifications
5. **Web Server Start**: Launch Flask application on port 8080


### Operational Features

#### Logging and Monitoring

Detailed logging at multiple levels:

```python
# Configuration and status logging
logger.info("Successfully subscribed to RAN NSSMF notifications.")
logger.warning("Previous inference still running, skipping this iteration")
logger.error("Failed to fetch details for NSSI ID: {nssi}")

# Debug logging for detailed troubleshooting
logger.debug(f"NSSI Details: {json.dumps(nssi_details, indent=2)}")
```

### Integration Examples

#### External System Integration

```python
import requests

# Trigger inference from external system
response = requests.post(
    "http://localhost:8080/handleFileReadyNotification",
    json={
        "fileInfoList": [
            {
                "fileId": "trigger_001",
                "fileSize": 1000,
                "fileLocation": "http://external-system/trigger"
            }
        ]
    }
)

if response.status_code == 200:
    print("Inference triggered successfully")
else:
    print(f"Failed to trigger inference: {response.text}")
```

## Deployment and Usage

### Prerequisites

- Python 3.8+
- InfluxDB 2.x for time series data storage
- TensorFlow/Keras for model inference
- Flask web framework for API service
- O-RAN Service Management Environment (optional for service discovery)
- RAN Network Slice Subnet Management Function (for slice control)

### Dependencies

Install required Python packages:

```bash
pip install -r src/requirements.txt
```

**Required Packages:**
- `influxdb_client`: InfluxDB 2.x client library
- `pandas`: Data manipulation and analysis
- `requests`: HTTP client library
- `tensorflow`: Machine learning framework
- `numpy`: Numerical computing
- `joblib`: Model serialization
- `scikit-learn`: Machine learning utilities
- `Flask`: Web framework for API service

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
   - Ensure model artifacts are present in `models/` directory

4. **Start the Prediction Service**:
   ```bash
   # Run with static configuration
   python src/main.py
   
   # Run with Service Management Environment discovery
   python src/main.py --use_sme True
   ```

## Kubernetes Deployment

### Helm Chart Deployment

The application includes a complete Helm chart for containerized deployment in Kubernetes clusters.

#### Prerequisites
- Kubernetes cluster (v1.16+)
- Helm 3.x installed
- Container registry access

#### Deployment Steps

1. **Build and Push Docker Image**:
   ```bash
   # Build container image
   docker build -t slice-prb-prediction-rapp:1.0.0 .
   
   # Push to registry
   docker push slice-prb-prediction-rapp:1.0.0
   ```

2. **Configure Helm Values**:
   Edit `slice-prb-prediction-rapp/Artifacts/Deployment/HELM/slice-prb-prediction-rapp/values.yaml`:
   ```yaml
   image:
     repository: your-registry.com
     tag: "1.0.0"
   
   influxdb:
     address: "http://influxdb2.smo:8086"
     token: "your-influxdb-token"
     org: "your-org"
     bucket: "nssi_pm_bucket"
   
   environment:
     smeDiscoveryEndpoint: "http://servicemanager.nonrtric.svc.cluster.local:8095/service-apis/v1/allServiceAPIs"
   ```

3. **Deploy with Helm**:
   ```bash
   # Install the Helm chart
   helm install slice-prb-prediction-rapp \
     ./slice-prb-prediction-rapp/Artifacts/Deployment/HELM/slice-prb-prediction-rapp \
     --namespace nonrtric \
     --create-namespace
   
   # Upgrade existing deployment
   helm upgrade slice-prb-prediction-rapp \
     ./slice-prb-prediction-rapp/Artifacts/Deployment/HELM/slice-prb-prediction-rapp
   ```

#### Kubernetes Resources

The Helm chart creates the following resources:
- **Deployment**: Pod with the PRB prediction application
- **Service**: ClusterIP service exposing port 8080
- **ConfigMap**: Application configuration from `values.yaml`
- **ServiceAccount**: Dedicated service account for the application
- **Secret**: InfluxDB token management (if configured)

#### Configuration Management

- **ConfigMap**: Automatically generated from Helm values
- **Secrets**: Secure management of InfluxDB tokens
- **Environment Variables**: Runtime configuration via Kubernetes secrets
- **Volume Mounts**: Configuration file mounted at `/app/config.json`

#### Monitoring and Scaling

- **Health Checks**: Configurable liveness and readiness probes
- **Resource Limits**: CPU and memory constraints
- **Auto-scaling**: Horizontal Pod Autoscaler support (optional)
- **Logging**: Structured logs for Kubernetes monitoring
