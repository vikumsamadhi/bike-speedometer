package com.bikespeedo.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

class GpsSpeedTracker(private val context: Context, private val listener: GpsSpeedListener) {

    interface GpsSpeedListener {
        fun onSpeedUpdated(speedKmh: Float, accuracyMeters: Float, hasFix: Boolean)
    }

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var isTracking = false

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val hasSpeed = location.hasSpeed() && location.speed >= 0
            if (hasSpeed) {
                // location.speed is in meters/second -> convert to km/h
                val speedKmh = location.speed * 3.6f
                listener.onSpeedUpdated(speedKmh, location.accuracy, true)
            } else {
                listener.onSpeedUpdated(0f, location.accuracy, false)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {
            listener.onSpeedUpdated(0f, 0f, false)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true

        val minTimeMs = 500L      // 500ms update rate
        val minDistanceMeters = 0f // Update on any movement

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTimeMs,
                minDistanceMeters,
                locationListener
            )
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        locationManager.removeUpdates(locationListener)
    }
}
