# Hardware Wiring & Setup Guide

## 1. Components
1. **ESP32 SuperMini** (most commonly ESP32-C3 SuperMini with USB-C).
2. **0.96" or 1.3" I2C OLED Display** (128x64 or 128x32 resolution, SSD1306 or SH1106 driver, 4 pins).
3. **Smartphone** (Android with Chrome or the native Android app).
4. **Power Source**: 5V USB power bank, 18650 battery shield, or 1S LiPo with a 3.3V/5V boost regulator.

---

## 2. Wiring Diagram (ESP32-C3 SuperMini to I2C OLED)

The ESP32-C3 SuperMini typically uses **GPIO 8** for SDA and **GPIO 9** for SCL:

| OLED Pin | ESP32-C3 SuperMini Pin | Note |
|---|---|---|
| **VCC** | **3V3** (or 5V/VBUS) | Most OLED modules have an onboard 3.3V LDO and accept 3.3V–5V. 3.3V is safest. |
| **GND** | **GND** | Common ground |
| **SCL** | **GPIO 9** | Default I2C Clock (Configurable in `config.h`) |
| **SDA** | **GPIO 8** | Default I2C Data (Configurable in `config.h`) |

*(Note: If your board is an ESP32-S3 SuperMini or original dual-core ESP32 Mini, GPIO 4 = SDA and GPIO 5 = SCL or GPIO 21/22. You can adjust the pin numbers in `config.h` in just one line).*

---

## 3. Power Consumption & Bike Mounting Tips
- The ESP32 SuperMini running BLE and driving an OLED consumes approx **40mA–70mA**.
- A tiny 500mAh 1S LiPo or a small lipstick power bank will run the speedometer for **8 to 12 hours** continuously.
- Put the ESP32 and OLED in a small 3D-printed enclosure or clear heat shrink / tic-tac box sealed against moisture before mounting to handlebars.
