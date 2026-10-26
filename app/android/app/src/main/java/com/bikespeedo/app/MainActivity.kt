package com.bikespeedo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bikespeedo.app.ble.BleSpeedManager
import com.bikespeedo.app.location.GpsSpeedTracker

class MainActivity : AppCompatActivity(), BleSpeedManager.BleEventListener, GpsSpeedTracker.GpsSpeedListener {

    private lateinit var bleManager: BleSpeedManager
    private lateinit var gpsTracker: GpsSpeedTracker

    private var currentSpeed: Float = 0f
    private var hasGpsFix: Boolean = false
    private var isConnected: Boolean = false

    private val periodicHandler = Handler(Looper.getMainLooper())
    private val sendRunnable = object : Runnable {
        override fun run() {
            if (isConnected) {
                bleManager.sendSpeed(currentSpeed, hasGpsFix, 85)
            }
            periodicHandler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bleManager = BleSpeedManager(this, this)
        gpsTracker = GpsSpeedTracker(this, this)

        val btnConnect = findViewById<Button>(R.id.btnConnectBle)
        val btnGps = findViewById<Button>(R.id.btnToggleGps)

        btnConnect.setOnClickListener {
            if (!hasRequiredPermissions()) {
                requestRequiredPermissions()
                return@setOnClickListener
            }

            if (isConnected) {
                bleManager.disconnect()
            } else {
                bleManager.startScanAndConnect()
            }
        }

        btnGps.setOnClickListener {
            if (!hasRequiredPermissions()) {
                requestRequiredPermissions()
                return@setOnClickListener
            }
            gpsTracker.startTracking()
            findViewById<TextView>(R.id.tvGpsStatus).text = "GPS: Acquiring Fix..."
        }

        periodicHandler.post(sendRunnable)
    }

    override fun onStatusChanged(status: String, isConnected: Boolean) {
        this.isConnected = isConnected
        findViewById<TextView>(R.id.tvBleStatus).text = "BLE: $status"
        findViewById<Button>(R.id.btnConnectBle).text = if (isConnected) "Disconnect ESP32" else "Connect ESP32"
    }

    override fun onPacketSent(speedKmh: Float) {
        // Updated on transmission
    }

    override fun onSpeedUpdated(speedKmh: Float, accuracyMeters: Float, hasFix: Boolean) {
        this.currentSpeed = speedKmh
        this.hasGpsFix = hasFix

        findViewById<TextView>(R.id.tvSpeed).text = if (hasFix) String.format("%.1f", speedKmh) else "---"
        findViewById<TextView>(R.id.tvGpsStatus).text = if (hasFix) "GPS: Locked (±${accuracyMeters.toInt()}m)" else "GPS: Searching..."
    }

    private fun hasRequiredPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bleScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val bleConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            return fineLocation && bleScan && bleConnect
        }
        return fineLocation
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
    }

    override fun onDestroy() {
        super.onDestroy()
        periodicHandler.removeCallbacks(sendRunnable)
        gpsTracker.stopTracking()
        bleManager.disconnect()
    }
}
