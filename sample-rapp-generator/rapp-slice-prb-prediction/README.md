# 5G RAN Slice PRB Prediction Rapp

Using 5G RAN Slice PRB Prediction Rapp, we can properly manage available RAN resources like PRBs and can avoid starvation of PRB by slices. And also we can avoid over-utilization of PRBs by slices.

## Directory Structure

- `src` - contains source code for the Rapp
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
