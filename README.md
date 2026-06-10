# NEC MultiSync Display Integration - Capabilities & Configuration
This document covers NEC MultiSync Display Adapter Capabilities and Configuration.

Symphony integrates with NEC MultiSync displays to provide monitoring and control via NEC's External Control protocol.
Main features are: power status monitoring, input source tracking, temperature sensor data, and hardware diagnosis status.

## NEC MultiSync - Main use cases
- **Monitor** display power state, active input source, temperature sensor readings, and hardware diagnosis status
- **Control** display power state and active input source
- **Track** hardware health via self-diagnosis — fan abnormalities, power rail faults, temperature warnings, and signal issues

## NEC MultiSync Device Configuration

The NEC MultiSync Adapter communicates with the display over TCP using NEC's External Control protocol (not HTTP/HTTPS).

The device must be configured in Symphony with the following values:

| Field | Description |
|---|---|
| Device Type | AV Devices |
| Category | Monitors |
| Manufacturer | NEC |
| Model | Any supported model |
| Monitoring Service | Advanced Monitoring |
| Monitoring Source | Direct |
| Management Address | IP address of the NEC display |
| Protocol | TELNET |
| Username | Leave blank |
| Password | External Control password |
| Port Number | 7142 (default) |

### NEC MultiSync - Adapter configuration properties

| Property | Description |
|---|---|
| historicalProperties | Comma-separated list of properties to track historically. Supported value: `temperature`. Default: blank (no historical tracking) |

For detailed information on the adapter and its configuration, please refer to our knowledgebase -> https://symphony.knowledgeowl.com/help/nec-multisync-display

## NEC MultiSync - Available Monitored Data

| Property | Description |
|---|---|
| Power | Current power state: ON, STANDBY, SUSPEND, or OFF |
| Input | Active input source: HDMI1, HDMI2, HDMI3, DVI1, DVI2, DisplayPort1, DisplayPort2, Option, Compute Module, or No Source |
| Temperature | Temperature sensor reading (historicaly graphable) |
| Diagnosis | Self-diagnosis result — NORMAL, or a specific fault code (e.g., FAN1_ABNORMALITY, TEMP_ABNORMALITY_SHUTDOWN, NO_SIGNAL, MAX_TEMP_REACHED, and others) |

## NEC MultiSync - Troubleshooting

**Link Error / Ping Timeout / Connection Refused**
- Verify the Management Address is the correct IP of the NEC display
- Confirm the Protocol is set to TELNET and Port is 7142
- Verify network reachability between the Cloud Connector and the display
- Check the Ping Protocol in the Symphony device settings; try switching between ICMP/TCP modes

**Login / Connection Error**
- Confirm the Protocol is set to TELNET and Port is 7142
- Ensure the External Control password matches the password configured on the display
- Verify the display has External Control enabled

**API Error**
- Verify the Management Address is the correct IP of the NEC display
- Confirm the Protocol is set to TELNET and Port is 7142
- Confirm the display supports NEC External Control features

**No Data / Empty Properties**
- Confirm the display supports NEC External Control features
- Check that the Cloud Connector can reach the display IP on port 7142

If none of the recommended steps help, please enter an SOS ticket at {https://avi-spl.atlassian.net/servicedesk/customer/portals}

## NEC MultiSync - What AI Assistant can do with it:
- Find NEC MultiSync display devices (AV Devices | Monitors | NEC) in Symphony
- Verify NEC MultiSync adapter configuration
- Report on display power state, input source, temperature, and diagnosis status

## NEC MultiSync - What AI Assistant cannot do with it:
- Provision devices
- Enable or configure External Control on the display itself
- Push firmware updates
