// Constants matching ESP32 firmware config.h
const BLE_SERVICE_UUID        = "19b10000-e8f2-537e-4f6c-d104768a1214";
const BLE_CHARACTERISTIC_UUID = "19b10001-e8f2-537e-4f6c-d104768a1214";

// State
let bluetoothDevice = null;
let speedCharacteristic = null;
let gpsWatchId = null;
let sendInterval = null;

let currentSpeedKmh = 0.0;
let maxSpeedKmh = 0.0;
let gpsFixValid = false;
let accuracyMeters = null;
let packetsSent = 0;
let phoneBatteryLevel = 90;
let isSimulating = false;

// UI Elements
const speedDisplay = document.getElementById("speedDisplay");
const gpsBadge = document.getElementById("gpsBadge");
const gpsAccuracy = document.getElementById("gpsAccuracy");
const maxSpeedEl = document.getElementById("maxSpeed");
const packetCountEl = document.getElementById("packetCount");
const deviceStatus = document.getElementById("deviceStatus");
const connectBleBtn = document.getElementById("connectBleBtn");
const toggleGpsBtn = document.getElementById("toggleGpsBtn");
const simCheckbox = document.getElementById("simCheckbox");
const simControls = document.getElementById("simControls");
const simSpeedSlider = document.getElementById("simSpeedSlider");
const simSpeedVal = document.getElementById("simSpeedVal");

// Query Battery API if supported
if (navigator.getBattery) {
    navigator.getBattery().then(battery => {
        phoneBatteryLevel = Math.round(battery.level * 100);
        battery.addEventListener('levelchange', () => {
            phoneBatteryLevel = Math.round(battery.level * 100);
        });
    });
}

// 1. Web Bluetooth Connection
connectBleBtn.addEventListener("click", async () => {
    if (bluetoothDevice && bluetoothDevice.gatt.connected) {
        disconnectBle();
        return;
    }

    try {
        deviceStatus.textContent = "BLE: SCANNING...";
        bluetoothDevice = await navigator.bluetooth.requestDevice({
            filters: [{ namePrefix: "BikeSpeedo" }, { services: [BLE_SERVICE_UUID] }],
            optionalServices: [BLE_SERVICE_UUID]
        });

        bluetoothDevice.addEventListener('gattserverdisconnected', onDisconnected);

        deviceStatus.textContent = "BLE: CONNECTING...";
        const server = await bluetoothDevice.gatt.connect();
        const service = await server.getPrimaryService(BLE_SERVICE_UUID);
        speedCharacteristic = await service.getCharacteristic(BLE_CHARACTERISTIC_UUID);

        deviceStatus.textContent = "BLE: CONNECTED";
        deviceStatus.classList.add("connected");
        connectBleBtn.textContent = "Disconnect ESP32";
        connectBleBtn.classList.add("btn-danger");

        // Start transmission loop (send every 500ms)
        if (!sendInterval) {
            sendInterval = setInterval(sendSpeedPacket, 500);
        }
    } catch (err) {
        console.error("BLE Error:", err);
        deviceStatus.textContent = "BLE: ERROR";
        alert("Bluetooth Connection failed: " + err.message);
    }
});

function disconnectBle() {
    if (bluetoothDevice && bluetoothDevice.gatt.connected) {
        bluetoothDevice.gatt.disconnect();
    }
    onDisconnected();
}

function onDisconnected() {
    deviceStatus.textContent = "BLE: DISCONNECTED";
    deviceStatus.classList.remove("connected");
    connectBleBtn.textContent = "Connect ESP32 (BLE)";
    connectBleBtn.classList.remove("btn-danger");
    speedCharacteristic = null;
    if (sendInterval) {
        clearInterval(sendInterval);
        sendInterval = null;
    }
}

// 2. Geolocation Tracking
toggleGpsBtn.addEventListener("click", () => {
    if (gpsWatchId !== null) {
        // Stop GPS
        navigator.geolocation.clearWatch(gpsWatchId);
        gpsWatchId = null;
        gpsFixValid = false;
        toggleGpsBtn.textContent = "Start GPS Tracking";
        toggleGpsBtn.classList.remove("btn-danger");
        gpsBadge.textContent = "GPS: STOPPED";
        gpsBadge.classList.remove("active");
        currentSpeedKmh = 0.0;
        updateSpeedUI();
    } else {
        // Start GPS
        if (!("geolocation" in navigator)) {
            alert("Geolocation is not supported by your browser.");
            return;
        }

        toggleGpsBtn.textContent = "Stop GPS Tracking";
        toggleGpsBtn.classList.add("btn-danger");
        gpsBadge.textContent = "GPS: ACQUIRING FIX...";

        gpsWatchId = navigator.geolocation.watchPosition(
            (position) => {
                if (isSimulating) return;

                const coords = position.coords;
                accuracyMeters = coords.accuracy ? Math.round(coords.accuracy) : null;
                gpsAccuracy.textContent = accuracyMeters ? `${accuracyMeters} m` : "-- m";

                if (coords.speed !== null && coords.speed >= 0) {
                    // coords.speed is in meters/sec, multiply by 3.6 for km/h
                    currentSpeedKmh = Math.max(0, coords.speed * 3.6);
                    gpsFixValid = true;
                    gpsBadge.textContent = `GPS: LOCKED (±${accuracyMeters || 5}m)`;
                    gpsBadge.classList.add("active");
                } else {
                    gpsFixValid = false;
                    currentSpeedKmh = 0.0;
                    gpsBadge.textContent = "GPS: NO SPEED DATA";
                    gpsBadge.classList.remove("active");
                }

                updateSpeedUI();
            },
            (error) => {
                console.warn("GPS Error:", error);
                gpsFixValid = false;
                gpsBadge.textContent = "GPS: PERMISSION / SIGNAL ERROR";
                gpsBadge.classList.remove("active");
            },
            {
                enableHighAccuracy: true,
                maximumAge: 500,
                timeout: 5000
            }
        );
    }
});

// 3. Desk Simulation Mode
simCheckbox.addEventListener("change", (e) => {
    isSimulating = e.target.checked;
    simControls.style.display = isSimulating ? "flex" : "none";
    if (isSimulating) {
        gpsFixValid = true;
        gpsBadge.textContent = "SIMULATING SPEED";
        gpsBadge.classList.add("active");
        currentSpeedKmh = parseFloat(simSpeedSlider.value);
        updateSpeedUI();
    } else {
        gpsFixValid = false;
        gpsBadge.textContent = "GPS: WAITING FIX";
        gpsBadge.classList.remove("active");
        currentSpeedKmh = 0.0;
        updateSpeedUI();
    }
});

simSpeedSlider.addEventListener("input", (e) => {
    const val = parseFloat(e.target.value);
    simSpeedVal.textContent = val.toFixed(1) + " km/h";
    if (isSimulating) {
        currentSpeedKmh = val;
        updateSpeedUI();
    }
});

function updateSpeedUI() {
    speedDisplay.textContent = currentSpeedKmh.toFixed(1);
    if (currentSpeedKmh > maxSpeedKmh) {
        maxSpeedKmh = currentSpeedKmh;
        maxSpeedEl.textContent = maxSpeedKmh.toFixed(1) + " km/h";
    }
}

// 4. Send Packet via BLE
async function sendSpeedPacket() {
    if (!speedCharacteristic || !bluetoothDevice || !bluetoothDevice.gatt.connected) {
        return;
    }

    try {
        const buffer = new ArrayBuffer(4);
        const view = new DataView(buffer);

        let speedCenti = 0xFFFF; // Invalid speed fallback
        let statusByte = 0x00;   // 0 = no fix, 1 = valid

        if (gpsFixValid) {
            speedCenti = Math.min(65000, Math.round(currentSpeedKmh * 100));
            statusByte = 0x01;
        }

        // Byte 0-1: uint16 speed in centi-km/h (Little Endian)
        view.setUint16(0, speedCenti, true);
        // Byte 2: GPS fix status
        view.setUint8(2, statusByte);
        // Byte 3: Phone battery %
        view.setUint8(3, phoneBatteryLevel);

        await speedCharacteristic.writeValueWithoutResponse(buffer);

        packetsSent++;
        packetCountEl.textContent = packetsSent;
    } catch (err) {
        console.warn("BLE Write Packet failed:", err);
    }
}
