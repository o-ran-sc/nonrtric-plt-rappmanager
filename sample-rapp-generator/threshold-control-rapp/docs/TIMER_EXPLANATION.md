# Timer Mechanism - Detailed Explanation

## Overview

The Threshold Control rApp uses Python's `threading.Timer` to automatically restore equipment to normal operation after a configurable period of energy-saving mode. This document explains exactly how the timer works.

## Timer Workflow

```
Time: 0s                    30s                   60s                   90s
      |                      |                     |                     |
      v                      v                     v                     v
   Monitor ──> Threshold ──> Equipment ──────────> Timer ──────────────> Equipment
   Cycle       Exceeded      Turned OFF            Expires               Turned ON
                             (Energy Save ON)                            (Energy Save OFF)
                             
                             [Timer Running: 60 seconds]
```

## Step-by-Step Process

### Step 1: Normal Monitoring (Time: 0s)
```python
# rApp polls InfluxDB every 30 seconds (configurable)
data_points = query_influxdb_data()

# Equipment state:
equipment_states['gNB-001'] = {
    'state': 'on',
    'timer': None,
    'reason': None
}
```

### Step 2: Threshold Exceeded (Time: 0s)
```python
# Metric value exceeds threshold
# Example: DRB.MaxActiveUeDl = 10 (threshold = 5)

if value > threshold_value:  # 10 > 5 = True
    # Threshold exceeded!
```

### Step 3: Turn OFF Equipment (Time: 0s)
```python
# Send PATCH request to SDNC
payload = {
    "ietf-hardware:component": {
        "name": "Benetel_RU",
        "o-ran-hardware:energy-saving-enabled": true  # Turn ON energy saving
    }
}

# Equipment enters energy-saving mode
```

### Step 4: Start Timer (Time: 0s)
```python
# Calculate when equipment should turn back on
turn_on_time = datetime.now() + timedelta(seconds=60)  # 60 seconds from now

# Update equipment state
equipment_states['gNB-001'] = {
    'state': 'off',
    'timer': turn_on_time,  # e.g., "2026-01-20T14:35:00"
    'reason': 'Threshold exceeded: DRB.MaxActiveUeDl=10'
}

# Start background timer thread
threading.Timer(
    60,                          # Duration in seconds
    self.turn_on_equipment,      # Function to call when timer expires
    args=['gNB-001']             # Equipment ID to turn back on
).start()

# Timer is now running in a separate thread!
# Main monitoring loop continues normally
```

### Step 5: During Timer Period (Time: 1s - 59s)
```python
# Main monitoring continues every 30 seconds
# If threshold is STILL exceeded during this period:

if threshold_exceeded and current_state['state'] == 'off':
    # Do nothing! Equipment is already off
    # No new timer is started
    # Existing timer continues counting down
    pass
```

**Important**: The rApp will NOT take any action on equipment that is already in the 'off' state, even if the threshold is still exceeded. This prevents:
- Multiple timers for the same equipment
- Resetting the timer countdown
- Conflicting control commands

### Step 6: Timer Expires (Time: 60s)
```python
# Timer thread automatically calls turn_on_equipment('gNB-001')

def turn_on_equipment(self, equipment_id):
    with self.state_lock:  # Thread-safe access
        current_state = self.equipment_states.get(equipment_id)
        
        if current_state and current_state['state'] == 'off':
            logger.info(f"Timer expired for equipment {equipment_id}. Turning back on.")
            
            # Send PATCH request to SDNC
            payload = {
                "ietf-hardware:component": {
                    "name": "Benetel_RU",
                    "o-ran-hardware:energy-saving-enabled": false  # Turn OFF energy saving
                }
            }
            
            # Update state
            self.equipment_states[equipment_id] = {
                'state': 'on',
                'timer': None,
                'reason': 'Timer-based restoration'
            }
```

### Step 7: Equipment Back Online (Time: 60s+)
```python
# Equipment is now back in normal operation
# If threshold is exceeded again, the cycle repeats

equipment_states['gNB-001'] = {
    'state': 'on',
    'timer': None,
    'reason': 'Timer-based restoration'
}
```

## Multiple Equipment Handling

The rApp can manage multiple pieces of equipment simultaneously, each with its own independent timer:

```python
# Example: Three pieces of equipment with different timers

equipment_states = {
    'gNB-001': {
        'state': 'off',
        'timer': datetime(2026, 1, 20, 14, 35, 0),  # Turns on at 14:35:00
        'reason': 'Threshold exceeded: DRB.MaxActiveUeDl=10'
    },
    'gNB-002': {
        'state': 'off',
        'timer': datetime(2026, 1, 20, 14, 35, 30),  # Turns on at 14:35:30
        'reason': 'Threshold exceeded: DRB.MaxActiveUeUl=8'
    },
    'gNB-003': {
        'state': 'on',
        'timer': None,
        'reason': None
    }
}

# Each timer runs independently in its own thread
# Timer for gNB-001 expires → turns on gNB-001
# Timer for gNB-002 expires → turns on gNB-002
# gNB-003 remains on (no timer)
```

## Thread Safety

The implementation uses thread locks to prevent race conditions:

```python
self.state_lock = threading.Lock()

# When accessing equipment states:
with self.state_lock:
    # Only one thread can access equipment_states at a time
    current_state = self.equipment_states.get(equipment_id)
    # ... modify state ...
```

This ensures:
- No conflicts between monitoring thread and timer threads
- Consistent state updates
- No lost updates or corrupted data

## Configuration

### Via Environment Variables
```bash
export TIMER_DURATION=120  # 2 minutes
```

### Via Helm Values
```yaml
timer:
  durationSeconds: 120  # 2 minutes
```

### Via config.json
```json
{
  "Timer": {
    "duration_seconds": 120
  }
}
```

## Monitoring Timer Status

You can check the current timer status via the `/status` endpoint:

```bash
curl http://localhost:8080/status
```

Response:
```json
{
  "equipment_states": {
    "gNB-001": {
      "state": "off",
      "timer": "2026-01-20T14:35:00",
      "reason": "Threshold exceeded: DRB.MaxActiveUeDl=10"
    }
  },
  "monitoring_active": true,
  "timestamp": "2026-01-20T14:34:30"
}
```

## Key Behaviors

1. **One Timer Per Equipment**: Each equipment has at most one active timer
2. **Non-Blocking**: Timers run in background threads; monitoring continues
3. **Automatic Execution**: No manual intervention needed; timer fires automatically
4. **Thread-Safe**: State management prevents race conditions
5. **No Re-triggering**: Equipment already off won't get a new timer
6. **Independent Timers**: Multiple equipment can have different timer schedules
7. **Guaranteed Restoration**: Equipment will turn back on after timer expires (unless rApp crashes)

## Example Timeline

```
14:30:00 - Monitoring cycle runs
14:30:00 - Threshold exceeded for gNB-001 (value=10, threshold=5)
14:30:00 - Turn OFF gNB-001 (energy-saving-enabled: true)
14:30:00 - Start 60-second timer for gNB-001
14:30:30 - Monitoring cycle runs (gNB-001 still off, no action)
14:31:00 - Timer expires for gNB-001
14:31:00 - Turn ON gNB-001 (energy-saving-enabled: false)
14:31:00 - gNB-001 back to normal operation
14:31:30 - Monitoring cycle runs (gNB-001 is on, can be turned off again if needed)
```

## Error Handling

If the SDNC request fails when turning equipment back on:
- The timer still expires and is cleared
- Equipment state remains 'off'
- Error is logged
- Next monitoring cycle may retry if threshold is no longer exceeded

## Testing the Timer

Use the core logic test to verify timer behavior:

```bash
cd threshold-control-rapp
python3 test_core_logic.py
```

Or test with a short timer duration:

```bash
export TIMER_DURATION=10  # 10 seconds for quick testing
python3 src/main.py --config src/config.json
```

## Summary

The timer mechanism provides automatic, hands-off restoration of equipment after energy-saving periods. It's:
- **Automatic**: No manual intervention required
- **Reliable**: Uses Python's built-in threading.Timer
- **Scalable**: Handles multiple equipment independently
- **Safe**: Thread-safe state management
- **Configurable**: Easy to adjust timer duration
- **Observable**: Status endpoint shows timer state

This ensures equipment doesn't stay in energy-saving mode indefinitely, balancing energy efficiency with service availability.
