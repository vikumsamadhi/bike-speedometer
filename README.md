# ESP32 Bike Speedometer

A lightweight, Bluetooth Low Energy (BLE) bicycle speedometer powered by an **ESP32 SuperMini** and an **I2C OLED display**. Real-time GPS speed is acquired from a smartphone (via Web Bluetooth or native Android app) and broadcast over BLE to the handlebars.

---

## Features

- **Handlebar Display**: High-contrast OLED display (0.96\" or 1.3\" SSD1306/SH1106) showing real-time speed, GPS fix status, and battery.
- **Ultra Compact Hardware**: Runs on an ESP32-C3 / ESP32-S3 SuperMini board with minimal power draw (~40-70mA).
- **Multiple Companions**:
  - **Web Companion**: Instant zero-install browser app using the Web Bluetooth API (works on Android Chrome).
  - **Android Native App**: Kotlin + Jetpack Compose / Foreground Service for continuous GPS tracking with phone screen locked.
- **Fail-Safe BLE Protocol**: Automatically detects signal loss, displays disconnect state, and handles GPS acquisition seamlessly.

---

## Repository Structure

`	ext
├── app/
│   └── android/              # Native Android application (Kotlin, BLE & GPS tracking)
├── docs/
│   ├── ble-protocol.md       # 4-byte GATT packet specification & UUIDs
│   └── wiring.md             # Pinouts, wiring diagram & battery recommendations
├── firmware/
│   └── bike_speedometer/     # Arduino/ESP-IDF firmware sketch and config.h
└── web_companion/            # Web Bluetooth PWA companion (HTML/JS/CSS)
`

---

## Quick Start

### 1. Hardware Setup & Wiring
Connect the OLED display to the ESP32-C3 SuperMini:
- **VCC** -> 3.3V
- **GND** -> GND
- **SDA** -> GPIO 8
- **SCL** -> GPIO 9

*For full pinouts and battery sizing, see [docs/wiring.md](docs/wiring.md).*

### 2. Flashing the Firmware
1. Open [irmware/bike_speedometer/bike_speedometer.ino](firmware/bike_speedometer/bike_speedometer.ino) in Arduino IDE.
2. Install required libraries: Adafruit SSD1306 and Adafruit GFX.
3. Select board: ESP32C3 Dev Module (Enable **USB CDC On Boot**).
4. Flash the board.

### 3. Connect via Web Companion
1. Open [web_companion/index.html](web_companion/index.html) in Chrome on an Android phone (or host via GitHub Pages).
2. Tap **Connect Speedometer** and select BikeSpeedo.
3. Allow Location permissions to start streaming real-time GPS speed to your handlebars!

---

## BLE Protocol
Communicates via standard GATT service:
- **Service UUID**: 19B10000-E8F2-537E-4F6C-D104768A1214
- **Speed Characteristic**: 19B10001-E8F2-537E-4F6C-D104768A1214
- Payload: 4-byte little-endian packet containing speed_centi_kmh, gps_status, and battery level.

See [docs/ble-protocol.md](docs/ble-protocol.md) for complete details.

---

## License
MIT License. Created by [Vikum Samadhi](https://github.com/vikumsamadhi).
