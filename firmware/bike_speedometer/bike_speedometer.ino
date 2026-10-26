#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#include "config.h"

// Hardware instances
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
BLEServer* pServer = nullptr;
BLECharacteristic* pSpeedCharacteristic = nullptr;

// State tracking
bool deviceConnected = false;
bool oldDeviceConnected = false;
unsigned long lastPacketTime = 0;

// Current telemetry values
float currentSpeedKmh = 0.0f;
bool hasValidGps = false;
uint8_t phoneBattery = 0;
bool isDataStale = true;

// BLE Server Callbacks
class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) override {
        deviceConnected = true;
    }

    void onDisconnect(BLEServer* pServer) override {
        deviceConnected = false;
    }
};

// BLE Characteristic Callbacks (Receiving speed packets from phone)
class SpeedDataCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) override {
        std::string rxValue = pCharacteristic->getValue();
        if (rxValue.length() >= 4) {
            uint16_t rawSpeedCenti = (uint8_t)rxValue[0] | ((uint8_t)rxValue[1] << 8);
            uint8_t gpsStatus      = (uint8_t)rxValue[2];
            uint8_t phoneBatt      = (uint8_t)rxValue[3];

            if (rawSpeedCenti == 0xFFFF || gpsStatus == 0x00) {
                hasValidGps = false;
                currentSpeedKmh = 0.0f;
            } else {
                hasValidGps = true;
                currentSpeedKmh = (float)rawSpeedCenti / 100.0f;
            }

            phoneBattery = phoneBatt;
            lastPacketTime = millis();
            isDataStale = false;
        }
    }
};

void drawUI() {
    display.clearDisplay();
    display.setTextColor(SSD1306_WHITE);

    // ==========================================
    // TOP STATUS BAR (0 to 12 px)
    // ==========================================
    display.setTextSize(1);
    display.setCursor(0, 0);

    // BLE status icon/text
    if (deviceConnected) {
        display.print(F("BLE:OK "));
    } else {
        display.print(F("BLE:-- "));
    }

    // GPS status
    if (deviceConnected && hasValidGps && !isDataStale) {
        display.print(F("GPS:OK "));
    } else if (deviceConnected && !hasValidGps) {
        display.print(F("GPS:NO "));
    } else {
        display.print(F("GPS:-- "));
    }

    // Phone Battery %
    if (deviceConnected && phoneBattery > 0 && phoneBattery <= 100) {
        display.setCursor(85, 0);
        display.print(phoneBattery);
        display.print(F("%"));
    }

    // Horizontal divider
    display.drawLine(0, 10, SCREEN_WIDTH, 10, SSD1306_WHITE);

    // ==========================================
    // MAIN SPEED AREA (Center)
    // ==========================================
    if (!deviceConnected) {
        // Disconnected state
        display.setTextSize(1);
        display.setCursor(18, 22);
        display.print(F("WAITING FOR APP"));
        display.setCursor(15, 36);
        display.print(F("Connect '"));
        display.print(F(DEVICE_NAME));
        display.print(F("'"));

        // Blinking indicator
        if ((millis() / 500) % 2 == 0) {
            display.fillCircle(64, 54, 3, SSD1306_WHITE);
        }
    } else if (isDataStale || !hasValidGps) {
        // Connected but no fresh GPS fix
        display.setTextSize(3);
        display.setCursor(20, 20);
        display.print(F("---"));

        display.setTextSize(1);
        display.setCursor(80, 28);
        display.print(DISPLAY_MPH ? F("mph") : F("km/h"));

        display.setCursor(16, 52);
        if (!hasValidGps) {
            display.print(F("ACQUIRING GPS FIX"));
        } else {
            display.print(F("SIGNAL TIMEOUT"));
        }
    } else {
        // Connected and Valid GPS Speed
        float displaySpeed = currentSpeedKmh;
        if (DISPLAY_MPH) {
            displaySpeed = displaySpeed * 0.621371f;
        }

        display.setTextSize(3);
        display.setCursor(10, 18);
        
        // Print formatted speed
        if (displaySpeed < 10.0f) {
            display.print(F(" "));
        }
        display.print(displaySpeed, 1);

        display.setTextSize(1);
        display.setCursor(95, 22);
        display.print(DISPLAY_MPH ? F("mph") : F("km/h"));

        // Bottom speed gauge / status line
        display.setCursor(22, 52);
        if (displaySpeed > 0.5f) {
            display.print(F("STATUS: RIDING"));
        } else {
            display.print(F("STATUS: STOPPED"));
        }
    }

    display.display();
}

void setup() {
    Serial.begin(115200);
    delay(200);
    Serial.println(F("[BikeSpeedo] Starting System..."));

    // 1. Initialize I2C with configured pins
    Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);

    // 2. Initialize OLED
    if (!display.begin(SSD1306_SWITCHCAPVCC, SCREEN_ADDRESS)) {
        Serial.println(F("[BikeSpeedo] ERROR: SSD1306 allocation failed!"));
        // Fallback or retry loop
        for (;;) {
            delay(1000);
            Serial.println(F("[BikeSpeedo] Checking I2C OLED connection..."));
        }
    }

    display.clearDisplay();
    display.setTextSize(2);
    display.setTextColor(SSD1306_WHITE);
    display.setCursor(8, 14);
    display.print(F("BIKE SPEEDO"));
    display.setTextSize(1);
    display.setCursor(20, 40);
    display.print(F("ESP32 SuperMini"));
    display.display();
    delay(1500);

    // 3. Initialize BLE Peripheral
    BLEDevice::init(DEVICE_NAME);
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService* pService = pServer->createService(SERVICE_UUID);

    pSpeedCharacteristic = pService->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ   |
        BLECharacteristic::PROPERTY_WRITE  |
        BLECharacteristic::PROPERTY_NOTIFY
    );

    pSpeedCharacteristic->setCallbacks(new SpeedDataCallbacks());
    pSpeedCharacteristic->addDescriptor(new BLE2902());

    pService->start();

    // Start advertising
    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();

    Serial.println(F("[BikeSpeedo] BLE Server Active. Advertising as BikeSpeedo..."));
}

void loop() {
    // Check for stale data timeout
    if (deviceConnected && (millis() - lastPacketTime > BLE_DATA_TIMEOUT_MS)) {
        isDataStale = true;
    }

    // Handle BLE reconnect advertising
    if (!deviceConnected && oldDeviceConnected) {
        delay(500); // give the bluetooth stack the chance to get things ready
        pServer->startAdvertising(); // restart advertising
        Serial.println(F("[BikeSpeedo] Device disconnected. Restarting advertising..."));
        oldDeviceConnected = deviceConnected;
    }

    // Connecting state transition
    if (deviceConnected && !oldDeviceConnected) {
        Serial.println(F("[BikeSpeedo] Device connected!"));
        oldDeviceConnected = deviceConnected;
        lastPacketTime = millis();
    }

    // Refresh OLED screen (~5 FPS update rate)
    drawUI();
    delay(200);
}
