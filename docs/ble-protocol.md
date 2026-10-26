# Bike Speedometer BLE Protocol Specification

## 1. Overview
This document specifies the Bluetooth Low Energy (BLE) GATT communication between the mobile phone (BLE Central / GATT Client) and the ESP32 SuperMini bike speedometer (BLE Peripheral / GATT Server).

- **Peripheral Device Name**: `BikeSpeedo`
- **Primary Service UUID**: `19B10000-E8F2-537E-4F6C-D104768A1214`
- **Speed Characteristic UUID**: `19B10001-E8F2-537E-4F6C-D104768A1214`
  - **Properties**: `READ`, `WRITE`, `WRITE_WITHOUT_RESPONSE`, `NOTIFY`

---

## 2. Speed Packet Format (4 Bytes)

The mobile phone transmits a 4-byte little-endian packet every 250ms–1000ms (recommended: 500ms) whenever GPS data is sampled.

| Byte Offset | Field | Type | Description |
|---|---|---|---|
| 0 .. 1 | `speed_centi_kmh` | `uint16_t` (Little Endian) | Speed in centi-km/h ($0.01\text{ km/h}$). E.g., `2540` = $25.40\text{ km/h}$. `0xFFFF` (65535) indicates invalid/no GPS speed. |
| 2 | `gps_status` | `uint8_t` | Fix status: <br>• `0x00`: No Fix / Acquiring GPS<br>• `0x01`: GPS Fix Valid (Speed reliable)<br>• `0x02`: Location paused / degraded |
| 3 | `phone_battery_or_seq` | `uint8_t` | Phone battery percentage ($0–100$) or incrementing sequence packet counter. |

### Example Payloads:
- **Stopped with GPS fix**: `[0x00, 0x00, 0x01, 0x55]` $\rightarrow 0.00\text{ km/h}$, Fix OK, Battery 85%.
- **Riding at 24.5 km/h**: `[0x92, 0x09, 0x01, 0x54]` $\rightarrow 2450\text{ centi-km/h} = 24.50\text{ km/h}$, Fix OK, Battery 84%.
- **Searching for satellites (No fix)**: `[0xFF, 0xFF, 0x00, 0x53]` $\rightarrow$ Invalid Speed, No Fix.

---

## 3. Timeout and Failsafe Rules

1. **Stale Data Timeout (2.5 seconds)**:
   - If the ESP32 receives no valid BLE packet for $\ge 2500\text{ ms}$, it displays `--- km/h` and flashes `NO DATA`.
2. **Disconnect Handling**:
   - Upon BLE disconnect event, the ESP32 immediately switches the display to `DISCONNECTED` / `SEARCHING...` and restarts BLE advertising.
3. **No Fix Handling**:
   - When `gps_status == 0x00` or speed is `0xFFFF`, the ESP32 shows `GPS SEARCH` or `--.- km/h` rather than showing a misleading $0\text{ km/h}$.
