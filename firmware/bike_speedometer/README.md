# ESP32 Bike Speedometer Firmware

## Required Arduino Libraries
Install these from the Arduino IDE Library Manager (**Tools -> Manage Libraries...**):
1. **Adafruit SSD1306** (by Adafruit)
2. **Adafruit GFX Library** (by Adafruit)
3. ESP32 BLE libraries (bundled automatically with the official **esp32** board package by Espressif).

## Arduino IDE Board Settings (for ESP32-C3 SuperMini)
- **Board**: `ESP32C3 Dev Module` (or `AirM2M CORE ESP32C3`)
- **USB CDC On Boot**: `Enabled` (important for serial output over USB-C)
- **Flash Mode**: `QIO`
- **Flash Size**: `4MB`
- **Upload Speed**: `921600` or `460800`

*(If using an ESP32-S3 SuperMini, choose `ESP32S3 Dev Module` and adjust pins in `config.h` if needed).*

## Flashing Instructions
1. Connect your ESP32 SuperMini to your computer with a USB-C data cable.
2. If uploading doesn't start automatically: hold down the **BOOT** button, press and release the **RESET** button, then release **BOOT** to enter download mode.
3. Click **Upload** in the Arduino IDE.
4. Open the Serial Monitor at **115200 baud** to see debug logs.
