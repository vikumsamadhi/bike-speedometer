package com.bikespeedo.app.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class BleSpeedManager(private val context: Context, private val listener: BleEventListener) {

    companion object {
        private const val TAG = "BleSpeedManager"
        val SERVICE_UUID: UUID = UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214")
    }

    interface BleEventListener {
        fun onStatusChanged(status: String, isConnected: Boolean)
        fun onPacketSent(speedKmh: Float)
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var speedCharacteristic: BluetoothGattCharacteristic? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isScanning = false

    @SuppressLint("MissingPermission")
    fun startScanAndConnect() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: run {
            listener.onStatusChanged("Bluetooth Not Supported", false)
            return
        }

        if (isScanning) return
        isScanning = true
        listener.onStatusChanged("Scanning for BikeSpeedo...", false)

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = device.name ?: result.scanRecord?.deviceName
                if (deviceName != null && deviceName.contains("BikeSpeedo", ignoreCase = true)) {
                    Log.d(TAG, "Found target device: $deviceName (${device.address})")
                    scanner.stopScan(this)
                    isScanning = false
                    connectToDevice(device)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isScanning = false
                listener.onStatusChanged("Scan failed (Error $errorCode)", false)
            }
        }

        scanner.startScan(scanCallback)

        // Stop scanning after 15 seconds if not found
        mainHandler.postDelayed({
            if (isScanning) {
                scanner.stopScan(scanCallback)
                isScanning = false
                listener.onStatusChanged("BikeSpeedo not found", false)
            }
        }, 15000)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        listener.onStatusChanged("Connecting to ${device.name ?: "ESP32"}...", false)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        speedCharacteristic = null
        listener.onStatusChanged("Disconnected", false)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mainHandler.post { listener.onStatusChanged("Discovering Services...", true) }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                speedCharacteristic = null
                mainHandler.post { listener.onStatusChanged("Disconnected from ESP32", false) }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                speedCharacteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (speedCharacteristic != null) {
                    mainHandler.post { listener.onStatusChanged("Connected & Ready", true) }
                } else {
                    mainHandler.post { listener.onStatusChanged("Characteristic not found", false) }
                }
            }
        }
    }

    /**
     * Sends the 4-byte speed payload matching docs/ble-protocol.md
     */
    @SuppressLint("MissingPermission")
    fun sendSpeed(speedKmh: Float, hasGpsFix: Boolean, batteryPercent: Int) {
        val characteristic = speedCharacteristic ?: return
        val gatt = bluetoothGatt ?: return

        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)

        val speedCenti: Short = if (hasGpsFix) {
            (speedKmh * 100).toInt().coerceIn(0, 65000).toShort()
        } else {
            0xFFFF.toShort()
        }

        val statusByte: Byte = if (hasGpsFix) 0x01 else 0x00
        val battByte: Byte = batteryPercent.coerceIn(0, 100).toByte()

        buffer.putShort(speedCenti)
        buffer.put(statusByte)
        buffer.put(battByte)

        characteristic.value = buffer.array()
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        gatt.writeCharacteristic(characteristic)

        mainHandler.post { listener.onPacketSent(if (hasGpsFix) speedKmh else -1f) }
    }
}
