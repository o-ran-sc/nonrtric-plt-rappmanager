# Equipment Control Sequence Documentation

## Overview

The Threshold Control rApp implements a comprehensive equipment control sequence that properly manages both carrier states and energy saving modes in O-RAN Radio Units (RUs) through SDNC.

## Why the Full Sequence is Needed

Simply enabling energy-saving mode in SDNC is **not sufficient** to put the RU to sleep. The complete sequence ensures:

1. **Carriers are properly transitioned** to SLEEP state before energy saving
2. **Energy saving mode is enabled** only after carriers are asleep
3. **Verification steps** confirm each state transition succeeded
4. **Automatic restoration** follows the reverse sequence after a timer

## Control Sequence Details

### Sleep Sequence (Threshold Exceeded)

When a threshold is exceeded, the following sequence executes:

```
┌─────────────────────────────────────────────────────────────┐
│ SLEEP SEQUENCE - Put Equipment to Sleep                     │
└─────────────────────────────────────────────────────────────┘

[STEP 1/5] Check Carriers are ACTIVE
    ├─ GET /o-ran-uplane-conf:user-plane-configuration
    ├─ Verify all tx-array-carriers have active: "ACTIVE"
    └─ Verify all rx-array-carriers have active: "ACTIVE"

[STEP 2/5] Put Carriers to SLEEP
    ├─ PATCH /o-ran-uplane-conf:user-plane-configuration
    ├─ Set all tx-array-carriers active: "SLEEP"
    └─ Set all rx-array-carriers active: "SLEEP"
    └─ Retry: 5 attempts, 3s delay

[STEP 3/5] Verify Carriers are SLEEP
    ├─ GET /o-ran-uplane-conf:user-plane-configuration
    ├─ Verify all tx-array-carriers have active: "SLEEP"
    └─ Verify all rx-array-carriers have active: "SLEEP"
    └─ Retry: 5 attempts, 3s delay

[STEP 4/5] Check Energy Saving State
    ├─ GET /ietf-hardware:hardware/component={name}
    └─ Read o-ran-hardware:energy-saving-enabled

[STEP 5/5] Enable Energy Saving (if disabled)
    ├─ PATCH /ietf-hardware:hardware/component={name}
    ├─ Set o-ran-hardware:energy-saving-enabled: true
    └─ Retry: 5 attempts, 3s delay

[TIMER] Start countdown (default: 60 seconds)
    └─ Schedule automatic wake-up
```

### Wake Sequence (Timer Expired)

After the timer expires, the equipment is restored:

```
┌─────────────────────────────────────────────────────────────┐
│ WAKE SEQUENCE - Wake Equipment Up                           │
└─────────────────────────────────────────────────────────────┘

[STEP 1/5] Check Energy Saving State
    ├─ GET /ietf-hardware:hardware/component={name}
    └─ Read o-ran-hardware:energy-saving-enabled

[STEP 2/5] Disable Energy Saving (if enabled)
    ├─ PATCH /ietf-hardware:hardware/component={name}
    ├─ Set o-ran-hardware:energy-saving-enabled: false
    └─ Retry: 5 attempts, 3s delay

[STEP 3/5] Check Carriers are SLEEP
    ├─ GET /o-ran-uplane-conf:user-plane-configuration
    ├─ Verify all tx-array-carriers have active: "SLEEP"
    └─ Verify all rx-array-carriers have active: "SLEEP"

[STEP 4/5] Put Carriers to ACTIVE
    ├─ PATCH /o-ran-uplane-conf:user-plane-configuration
    ├─ Set all tx-array-carriers active: "ACTIVE"
    └─ Set all rx-array-carriers active: "ACTIVE"
    └─ Retry: 5 attempts, 3s delay

[STEP 5/5] Verify Carriers are ACTIVE
    ├─ GET /o-ran-uplane-conf:user-plane-configuration
    ├─ Verify all tx-array-carriers have active: "ACTIVE"
    └─ Verify all rx-array-carriers have active: "ACTIVE"
    └─ Retry: 5 attempts, 3s delay
```

## SDNC API Endpoints

### Hardware Component (Energy Saving)

**Endpoint:**
```
/rests/data/network-topology:network-topology/topology=topology-netconf/node={NODE_NAME}/yang-ext:mount/ietf-hardware:hardware/component={COMPONENT_NAME}
```

**GET Response Example:**
```json
{
  "ietf-hardware:component": {
    "name": "benetel_RU",
    "o-ran-hardware:energy-saving-enabled": false,
    "state": {
      "o-ran-hardware:power-state": "AWAKE",
      "oper-state": "enabled"
    }
  }
}
```

**PATCH Request Example:**
```json
{
  "ietf-hardware:component": {
    "name": "benetel_RU",
    "o-ran-hardware:energy-saving-enabled": true
  }
}
```

### User Plane Configuration (Carriers)

**Endpoint:**
```
/rests/data/network-topology:network-topology/topology=topology-netconf/node={NODE_NAME}/yang-ext:mount/o-ran-uplane-conf:user-plane-configuration
```

**GET Response Example:**
```json
{
  "o-ran-uplane-conf:user-plane-configuration": {
    "tx-array-carriers": [
      {
        "name": "TxArray0",
        "active": "ACTIVE",
        "center-of-channel-bandwidth": "3450720000",
        "channel-bandwidth": "100000000"
      },
      {
        "name": "TxArray1",
        "active": "ACTIVE",
        "center-of-channel-bandwidth": "3450720000",
        "channel-bandwidth": "100000000"
      }
    ],
    "rx-array-carriers": [
      {
        "name": "RxArray0",
        "active": "ACTIVE",
        "center-of-channel-bandwidth": "3450720000",
        "channel-bandwidth": "100000000"
      },
      {
        "name": "RxArray1",
        "active": "ACTIVE",
        "center-of-channel-bandwidth": "3450720000",
        "channel-bandwidth": "100000000"
      }
    ]
  }
}
```

**PATCH Request Example (Set to SLEEP):**
```json
{
  "o-ran-uplane-conf:user-plane-configuration": {
    "tx-array-carriers": [
      {"name": "TxArray0", "active": "SLEEP"},
      {"name": "TxArray1", "active": "SLEEP"}
    ],
    "rx-array-carriers": [
      {"name": "RxArray0", "active": "SLEEP"},
      {"name": "RxArray1", "active": "SLEEP"}
    ]
  }
}
```

## State Tracking

Each equipment is tracked with the following state structure:

```python
equipment_states[equipment_id] = {
    'overall_state': 'on' | 'sleeping' | 'transitioning',
    'energy_saving_enabled': True | False,
    'carriers': {
        'tx': [
            {'name': 'TxArray0', 'active': 'ACTIVE'},
            {'name': 'TxArray1', 'active': 'ACTIVE'}
        ],
        'rx': [
            {'name': 'RxArray0', 'active': 'ACTIVE'},
            {'name': 'RxArray1', 'active': 'ACTIVE'}
        ]
    },
    'timer': datetime | None,  # When equipment will wake up
    'reason': 'Threshold exceeded: DRB.MeanActiveUeDl=0.5'
}
```

### State Transitions

```
┌─────────┐  Threshold    ┌──────────┐   Timer      ┌─────────┐
│   ON    │  Exceeded  ──>│ SLEEPING │  Expired  ──>│   ON    │
└─────────┘               └──────────┘              └─────────┘
    │                          │                         │
    │ Carriers: ACTIVE         │ Carriers: SLEEP         │ Carriers: ACTIVE
    │ Energy Saving: false     │ Energy Saving: true     │ Energy Saving: false
```

## Retry Logic

All SDNC operations include retry logic:

- **Retry Attempts:** 5 (configurable via `SDNC_RETRY_ATTEMPTS`)
- **Retry Delay:** 3 seconds (configurable via `SDNC_RETRY_DELAY`)
- **Operations with Retry:**
  - Setting carrier states (PATCH)
  - Verifying carrier states (GET with validation)
  - Setting energy saving state (PATCH)

## Error Handling

### Logging

Each step produces detailed logs:

```
[SLEEP SEQUENCE] Starting for equipment ManagedElement=OAI-DU,DuFunction=0,CellId=0
[STEP 1/5] Checking if carriers are ACTIVE
[STEP 1/5] ✓ All carriers are ACTIVE
[STEP 2/5] Putting carriers to SLEEP
Attempt 1/5: Patching carriers to SLEEP
[STEP 2/5] ✓ Carriers set to SLEEP
[STEP 3/5] Verifying carriers are SLEEP
Verification attempt 1/5
✓ All carriers verified in SLEEP state
[STEP 3/5] ✓ All carriers verified SLEEP
[STEP 4/5] Checking energy saving state
[STEP 4/5] Current energy saving state: False
[STEP 5/5] Energy saving is disabled, enabling it
Attempt 1/5: Setting energy saving to enable
[STEP 5/5] ✓ Energy saving enabled
[SLEEP SEQUENCE] ✓ Successfully completed for equipment ManagedElement=OAI-DU,DuFunction=0,CellId=0
```

### Failure Scenarios

**Carrier State Verification Fails:**
```
[STEP 3/5] Verifying carriers are SLEEP
Verification attempt 1/5
TX Carrier TxArray0 is ACTIVE, expected SLEEP
Waiting 3s before retry...
Verification attempt 2/5
...
✗ Failed to verify carriers in SLEEP state after 5 attempts
[STEP 3/5] ✗ Failed to verify carriers are SLEEP
```

**SDNC Request Fails:**
```
Attempt 1/5: Patching carriers to SLEEP
Attempt 1 failed to patch carriers: {'success': False, 'error': 'Connection timeout'}
Waiting 3s before retry...
Attempt 2/5: Patching carriers to SLEEP
...
Failed to set carriers to SLEEP after 5 attempts
[STEP 2/5] ✗ Failed to set carriers to SLEEP
```

## Configuration

### SDNC Configuration

```json
{
  "SDNC": {
    "node_name": "Benetel_RU",
    "component_name": "benetel_RU",
    "username": "admin",
    "password": "your-password",
    "retry_attempts": 5,
    "retry_delay_seconds": 3
  }
}
```

### Helm Values

```yaml
sdnc:
  nodeName: "Benetel_RU"
  componentName: "benetel_RU"
  username: "admin"
  password: "your-password"
  retryAttempts: 5
  retryDelaySeconds: 3

timer:
  durationSeconds: 60  # Time equipment stays asleep
```

### Environment Variables

```bash
SDNC_NODE_NAME=Benetel_RU
COMPONENT_NAME=benetel_RU
SDNC_USERNAME=admin
SDNC_PASSWORD=your-password
SDNC_RETRY_ATTEMPTS=5
SDNC_RETRY_DELAY=3
TIMER_DURATION=60
```

## Monitoring

### Check Equipment State

```bash
curl http://threshold-control-rapp:8080/status
```

**Response:**
```json
{
  "equipment_states": {
    "ManagedElement=OAI-DU,DuFunction=0,CellId=0": {
      "overall_state": "sleeping",
      "energy_saving_enabled": true,
      "carriers": {
        "tx": [
          {"name": "TxArray0", "active": "SLEEP"},
          {"name": "TxArray1", "active": "SLEEP"}
        ],
        "rx": [
          {"name": "RxArray0", "active": "SLEEP"},
          {"name": "RxArray1", "active": "SLEEP"}
        ]
      },
      "timer": "2026-01-26T15:30:00",
      "reason": "Threshold exceeded: DRB.MeanActiveUeDl=0.5"
    }
  },
  "monitoring_active": true,
  "timestamp": "2026-01-26T15:29:00"
}
```

### Log Monitoring

```bash
kubectl logs -f -l app.kubernetes.io/name=threshold-control-rapp -n nonrtric | grep -E "\[STEP|SEQUENCE\]"
```

## Testing

### Manual Testing

1. **Trigger Sleep Sequence:**
   - Ensure InfluxDB data exceeds threshold
   - Monitor logs for sleep sequence execution
   - Verify carriers go to SLEEP
   - Verify energy saving is enabled

2. **Verify Timer:**
   - Wait for configured duration (default 60s)
   - Monitor logs for wake sequence execution

3. **Verify Wake Sequence:**
   - Check energy saving is disabled
   - Check carriers return to ACTIVE
   - Verify equipment is operational

### Direct SDNC Testing

Test individual SDNC calls:

```bash
# Get carrier states
curl -X GET "http://sdnc:8282/rests/data/network-topology:network-topology/topology=topology-netconf/node=Benetel_RU/yang-ext:mount/o-ran-uplane-conf:user-plane-configuration?content=config" \
  -u admin:password \
  -H "Accept: application/json"

# Get energy saving state
curl -X GET "http://sdnc:8282/rests/data/network-topology:network-topology/topology=topology-netconf/node=Benetel_RU/yang-ext:mount/ietf-hardware:hardware/component=benetel_RU" \
  -u admin:password \
  -H "Accept: application/json"
```

## Troubleshooting

### Issue: Carriers don't transition to SLEEP

**Check:**
- SDNC connectivity
- Node name and endpoint configuration
- Carrier names match actual RU configuration
- SDNC logs for errors

**Solution:**
- Verify SDNC endpoint is correct
- Check SDNC credentials
- Increase retry attempts/delay

### Issue: Energy saving doesn't enable

**Check:**
- Component name matches SDNC configuration
- Hardware component exists in SDNC
- SDNC supports energy saving feature

**Solution:**
- Verify component name in SDNC
- Check SDNC version supports O-RAN hardware module

### Issue: Verification fails repeatedly

**Check:**
- State transition timing (may need longer delay)
- SDNC processing time
- Network latency

**Solution:**
- Increase `retry_delay_seconds`
- Increase `retry_attempts`
- Check SDNC performance

## Performance Considerations

### Timing

- **Sleep Sequence:** ~15-30 seconds (with retries)
- **Wake Sequence:** ~15-30 seconds (with retries)
- **Total Cycle:** Timer duration + wake sequence time

### Resource Usage

- **Network Calls:** 8-10 SDNC API calls per cycle (more with retries)
- **Memory:** Minimal (state tracking per equipment)
- **CPU:** Low (mostly I/O bound)

## Future Enhancements

Potential improvements:

1. **Parallel Equipment Control:** Handle multiple equipment simultaneously
2. **Configurable Verification Logic:** Allow partial carrier state matches
3. **State Persistence:** Save state to survive rApp restarts
4. **Rollback on Failure:** Automatic rollback if sequence fails mid-way
5. **Adaptive Retry:** Exponential backoff for retries
6. **Health Monitoring:** Track success/failure rates per equipment
