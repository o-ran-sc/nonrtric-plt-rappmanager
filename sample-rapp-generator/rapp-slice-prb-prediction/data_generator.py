from datetime import timedelta
import numpy as np
import pandas as pd
import time

from influxdb_client import InfluxDBClient,WriteOptions,Point, WritePrecision

from influxdb_client.rest import ApiException

DEFAULT_START = "2025-01-01 00:00:00"
DEFAULT_END = "2025-01-02 00:00:00"
DEFAULT_INTERVAL = 15

START_TIME_STRING = input(f"Enter START_TIME (YYYY-MM-DD HH:MM:SS) [default: {DEFAULT_START}]: ") or DEFAULT_START
END_TIME_STRING = input(f"Enter END_TIME (YYYY-MM-DD HH:MM:SS) [default: {DEFAULT_END}]: ") or DEFAULT_END
INTERVAL_MIN = int(input(f"Enter INTERVAL_MIN (minutes) [default: {DEFAULT_INTERVAL}]: ") or DEFAULT_INTERVAL)

START_TIME = pd.Timestamp(START_TIME_STRING)
END_TIME = pd.Timestamp(END_TIME_STRING)

# Default InfluxDB configuration
DEFAULT_INFLUX_URL = "http://localhost:8086"
DEFAULT_INFLUX_TOKEN = ""
DEFAULT_INFLUX_ORG = ""
DEFAULT_INFLUX_BUCKET = "nssi_pm_bucket"

# Get user input or use defaults
INFLUX_URL = input(f"Enter InfluxDB URL [default: {DEFAULT_INFLUX_URL}]: ") or DEFAULT_INFLUX_URL
INFLUX_TOKEN = input(f"Enter InfluxDB Token [default: {DEFAULT_INFLUX_TOKEN}]: ") or DEFAULT_INFLUX_TOKEN
INFLUX_ORG = input(f"Enter InfluxDB Organization [default: {DEFAULT_INFLUX_ORG}]: ") or DEFAULT_INFLUX_ORG
INFLUX_BUCKET = input(f"Enter InfluxDB Bucket [default: {DEFAULT_INFLUX_BUCKET}]: ") or DEFAULT_INFLUX_BUCKET

if END_TIME <= START_TIME:
    raise ValueError("END_TIME must be after START_TIME")

NSSIS = [
    {"measObjLdn": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fa", "sliceType": "embb"},
    {"measObjLdn": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fb", "sliceType": "embb"},
    {"measObjLdn": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fc", "sliceType": "urllc"},
    {"measObjLdn": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fd", "sliceType": "urllc"},
    {"measObjLdn": "9090d36f-6af5-4cfd-8bda-7a3c88fa82fe", "sliceType": "mmtc"},
    {"measObjLdn": "9090d36f-6af5-4cfd-8bda-7a3c88fa82ff", "sliceType": "mmtc"}
]

MEAS_TYPES = [
    "RRU.PrbDl.SNSSAI",
    "DRB.PdcpSduVolumeDL.SNSSAI",
    "RRC.ConnEstabSucc.Cause"
]

def embb_activity_level(hour):
    """
    Determine the activity level for eMBB (Enhanced Mobile Broadband) slice based on hour of day.
    
    eMBB typically has higher usage during daytime hours and lower usage during night hours.
    This function models realistic traffic patterns for broadband services.
    
    Args:
        hour (int): Hour of the day (0-23)
        
    Returns:
        str: Activity level - "low" (0-6h), "medium" (6-17h), or "medhigh" (17-23h)
    """
    if 0 <= hour <= 6:
        return "low"
    elif 6 < hour <= 17:
        return "medium"
    else:
        return "medhigh"
    
def urllc_activity_level(hour):
    """
    Determine the activity level for URLLC (Ultra-Reliable Low-Latency Communication) slice based on hour of day.
    
    URLLC services are typically used for critical applications during business hours.
    This function models high activity during daytime (8-20h) and low activity otherwise.
    
    Args:
        hour (int): Hour of the day (0-23)
        
    Returns:
        str: Activity level - "high" (8-20h) or "low" (otherwise)
    """
    return "high" if 8 <= hour <= 20 else "low"

def mmtc_is_burst(ts):
    """
    Determine if the given timestamp represents a burst period for mMTC (Massive Machine Type Communications).
    
    mMTC devices typically communicate in bursts at specific intervals to conserve power.
    This function models burst behavior at 6-hour intervals (6, 12, 18) during quarter-hour marks.
    
    Args:
        ts (datetime): Timestamp to check for burst condition
        
    Returns:
        bool: True if it's a burst time, False otherwise
    """
    return (ts.hour in [6,12,18]) and (ts.minute in [0,15,30,45])

rng = np.random.default_rng(42)

def gen_embb_kpis(ts):
    """
    Generate Key Performance Indicators (KPIs) for eMBB slice at a given timestamp.
    
    eMBB KPIs include downlink volume, RRC connection success rate, and PRB usage.
    The values are generated based on the activity level for the given hour.
    
    Args:
        ts (datetime): Timestamp for which to generate KPIs
        
    Returns:
        tuple: (dl_volume, rrc_success, prb_usage)
            - dl_volume (float): Downlink data volume in MB
            - rrc_success (int): Number of successful RRC connections
            - prb_usage (float): Physical Resource Block usage percentage
    """
    lvl = embb_activity_level(ts.hour)
    ue_count=0
    sigma_limit=50
    if lvl == "low":
        ue_count = 10+np.random.poisson(6)*10
    elif lvl == "medium":
        ue_count = 500 + np.random.poisson(10)*30
    else:
        ue_count = 1300 + np.random.poisson(10)*40

    mean_vol = 2*ue_count
    dl_vol =max(0, np.random.normal(mean_vol,min(0.1*mean_vol,sigma_limit)))
    prb_dl = dl_vol*3
    
    return round(dl_vol, 2), max(int(ue_count * 0.6), 0), prb_dl

def gen_urllc_kpis(ts):
    """
    Generate Key Performance Indicators (KPIs) for URLLC slice at a given timestamp.
    
    URLLC KPIs prioritize reliability and low latency, with higher data volume per UE
    compared to eMBB. The values are generated based on the activity level for the given hour.
    
    Args:
        ts (datetime): Timestamp for which to generate KPIs
        
    Returns:
        tuple: (dl_volume, rrc_success, prb_usage)
            - dl_volume (float): Downlink data volume in MB
            - rrc_success (int): Number of successful RRC connections
            - prb_usage (float): Physical Resource Block usage percentage
    """
    lvl = urllc_activity_level(ts.hour)
    ue_count=0
    sigma_limit=50
    if lvl == "high":
        ue_count = 500 + np.random.poisson(10)*30
    else:
        ue_count = 100 + np.random.poisson(10)*5

    mean_vol = 7*ue_count
    dl_vol =max(0, np.random.normal(mean_vol,min(sigma_limit,0.1*mean_vol )))
    prb_dl = dl_vol*3
    return round(dl_vol, 2), max(int(ue_count * 0.6), 0), prb_dl

def gen_mmtc_kpis(ts):
    """
    Generate Key Performance Indicators (KPIs) for mMTC slice at a given timestamp.
    
    mMTC KPIs model massive device connectivity with bursty traffic patterns.
    High device count with low individual data volume, except during burst periods.
    
    Args:
        ts (datetime): Timestamp for which to generate KPIs
        
    Returns:
        tuple: (dl_volume, rrc_success, prb_usage)
            - dl_volume (float): Downlink data volume in MB
            - rrc_success (int): Number of successful RRC connections
            - prb_usage (float): Physical Resource Block usage percentage
    """
    ue_count = np.random.uniform(1000,1100)
    sigma_limit=50
    if mmtc_is_burst(ts):
        mean_vol = 1*ue_count
    else:
        mean_vol = 0.1*ue_count
    
    dl_vol =max(0, np.random.normal(mean_vol,min(sigma_limit,0.1*mean_vol)))
    prb_dl = dl_vol*2
    return round(dl_vol, 2), max(int(ue_count * 0.6), 0), prb_dl


def push_to_influxdb(data_points):
    """
    Push performance data points to InfluxDB time series database.
    
    Uses batch writing for efficient data ingestion with configurable batch size
    and flush interval. Properly closes connections to ensure data integrity.
    
    Args:
        data_points (list): List of InfluxDB Point objects containing performance data
    """
    with InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG) as client:
        write_api = client.write_api(write_options=WriteOptions(batch_size=500, flush_interval=10000))

        for point in data_points:
            write_api.write(bucket=INFLUX_BUCKET, record=point)
        
        write_api.flush()
        write_api.close()
        client.close()

                       
def generate_nssi_pm(
        start_time=START_TIME,
        end_time=END_TIME,
        interval_min=INTERVAL_MIN,
        nssis=NSSIS
):
    """
    Generate Network Slice Subnet Instance (NSSI) performance monitoring data.
    
    Creates time-series performance data for different network slice types (eMBB, URLLC, mMTC)
    over a specified time period. Generates KPIs for each slice at regular intervals and
    formats the data for both InfluxDB storage and CSV export.
    
    Args:
        start_time (pd.Timestamp): Start time for data generation
        end_time (pd.Timestamp): End time for data generation
        interval_min (int): Time interval in minutes between data points
        nssis (list): List of NSSI configurations with slice types and IDs
        
    Returns:
        list: List of InfluxDB Point objects containing performance data
    """
    delta = end_time - start_time
    total_minutes = int(delta.total_seconds() // 60)
    periods = (total_minutes // interval_min) + 1
    times = pd.date_range(start_time, periods=periods, freq=f"{interval_min}min")

    records = []
    data_points = []
    for ts in times:
        for n in nssis:
            nssi_id = n["measObjLdn"]
            slice_type = n["sliceType"].lower()

            if slice_type == "embb":
                pdcp_mb, rrc_succ, prb_pct = gen_embb_kpis(ts)
            elif slice_type == "urllc":
                pdcp_mb, rrc_succ, prb_pct = gen_urllc_kpis(ts)
            elif slice_type == "mmtc":
                pdcp_mb, rrc_succ, prb_pct = gen_mmtc_kpis(ts)
            else:
                pdcp_mb, rrc_succ, prb_pct = rng.normal(300, 80), int(rng.poisson(20))
            
            record = {
                "measObjLdn": nssi_id,
                "beginTime": ts.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "endTime": (ts + timedelta(minutes=interval_min)).strftime("%Y-%m-%dT%H:%M:%SZ"),
                "sliceType": slice_type,
                "measTypes": MEAS_TYPES,
                "measValue": [
                    round(prb_pct, 2),
                    round(float(pdcp_mb), 2),
                    int(rrc_succ)
            ] 
            }
            records.append(record)
            p = (Point("nssi_pm_bucket")
                    .tag("measObjLdn", nssi_id)
                    .tag("sliceType", slice_type)
                    .field("RRU.PrbDl.SNSSAI", round(prb_pct, 2))
                    .field("DRB.PdcpSduVolumeDL.SNSSAI", round(float(pdcp_mb), 2))
                    .field("RRC.ConnEstabSucc.Cause", int(rrc_succ))
                    .time(ts.strftime("%Y-%m-%dT%H:%M:%S.%fZ"), WritePrecision.NS)
            )
            data_points.append(p)
    
    return data_points

def create_bucket_if_not_exists():
    """
    Create a bucket in InfluxDB if it doesn't already exist.
    
    Connects to InfluxDB and checks if the specified bucket exists.
    If the bucket doesn't exist, creates it with infinite retention period.
    Handles various API errors and provides informative error messages.
    
    Returns:
        bool: True if bucket exists or was created successfully, False otherwise
        
    Raises:
        ApiException: If there are issues with InfluxDB API communication
        Exception: For other unexpected errors during bucket creation
    """
    client = None
    try:
        # Initialize InfluxDB client
        client = InfluxDBClient(
            url=INFLUX_URL,
            token=INFLUX_TOKEN,
            org=INFLUX_ORG
        )
        
        # Get buckets API
        buckets_api = client.buckets_api()
        
        # Check if bucket already exists
        print(f"Checking if bucket '{INFLUX_BUCKET}' exists...")
        buckets = buckets_api.find_buckets().buckets
        
        bucket_exists = False
        for bucket in buckets:
            if bucket.name == INFLUX_BUCKET:
                bucket_exists = True
                print(f"Bucket '{INFLUX_BUCKET}' already exists.")
                break
        
        # Create bucket if it doesn't exist
        if not bucket_exists:
            print(f"Bucket '{INFLUX_BUCKET}' does not exist. Creating...")
            
            # Create bucket with default retention (infinite)
            bucket = buckets_api.create_bucket(
                bucket_name=INFLUX_BUCKET,
                org=INFLUX_ORG
            )
            
            print(f"Bucket '{INFLUX_BUCKET}' created successfully.")
            print(f"Bucket ID: {bucket.id}")
            print(f"Organization: {INFLUX_ORG}")
            print(f"Retention period: Infinite (default)")
        
        return True
        
    except ApiException as e:
        print(f"InfluxDB API Error: {e}")
        if e.status == 401:
            print("Error: Unauthorized - Check your token")
        elif e.status == 403:
            print("Error: Forbidden - Check your permissions")
        elif e.status == 404:
            print("Error: Not Found - Check your organization")
        else:
            print(f"HTTP Status: {e.status}")
        return False
        
    except Exception as e:
        print(f"Unexpected error: {e}")
        return False
        
    finally:
        # Close the client connection
        if client:
            client.close()
            print("InfluxDB connection closed.")

if __name__ == "__main__":
    create_bucket_if_not_exists()

    gerated_data = generate_nssi_pm()

    push_to_influxdb(gerated_data)

    time.sleep(5)
