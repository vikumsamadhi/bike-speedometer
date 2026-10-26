#ifndef CONFIG_H
#define CONFIG_H

// ==========================================
// 1. PINOUT CONFIGURATION
// ==========================================
// Default I2C pins for ESP32-C3 SuperMini:
// SDA = GPIO 8, SCL = GPIO 9
// (Change to 4 and 5 if using ESP32-S3 or other ESP32 SuperMini variations)
#define I2C_SDA_PIN        8
#define I2C_SCL_PIN        9

// ==========================================
// 2. OLED DISPLAY CONFIGURATION
// ==========================================
#define SCREEN_WIDTH       128
#define SCREEN_HEIGHT      64    // Set to 32 if you have a 128x32 OLED
#define OLED_RESET         -1    // Reset pin # (or -1 if sharing Arduino reset pin)
#define SCREEN_ADDRESS     0x3C  // Common I2C addresses are 0x3C or 0x3D

// ==========================================
// 3. BLE GATT CONFIGURATION
// ==========================================
#define DEVICE_NAME         "BikeSpeedo"
#define SERVICE_UUID        "19B10000-E8F2-537E-4F6C-D104768A1214"
#define CHARACTERISTIC_UUID "19B10001-E8F2-537E-4F6C-D104768A1214"

// ==========================================
// 4. UNIT & SAFETY SETTINGS
// ==========================================
// Set to true if you prefer mph instead of km/h
#define DISPLAY_MPH        false

// Time after which missing BLE data is marked stale/lost (milliseconds)
#define BLE_DATA_TIMEOUT_MS 2500

#endif // CONFIG_H
