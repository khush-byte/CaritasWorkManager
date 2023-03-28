package com.example.workmanager03.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.workmanager03.R
import com.example.workmanager03.worker.MyWorker
import com.example.workmanager03.worker.MyWorker.Companion.LOCATION

class LocationService : Service() {
    lateinit var notification: NotificationCompat.Builder
    lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        start()
    }

    private fun start() {
        notification = NotificationCompat.Builder(this, MyWorker.CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.baseline_my_location)
            .setAutoCancel(true)
        startTracking()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForeground(NOTIFICATION_ID, notification.build())
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        val locationManager =
            applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            10000,
            0f,
            locationListener
        )
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            LOCATION = location
//            val loc = "${location.latitude},\n${location.longitude}"
//            Log.d("MyTag", loc)
//            val lat = location.latitude.toString().take(8)
//            val long = location.longitude.toString().take(8)
//            val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
//            val time = LocalDateTime.now().format(formatter)
//            val newLocation = "$lat, $long"
//
//            val updateNotification = notification.setContentText(
//                "Location: $time ($newLocation)"
//            )
//            notificationManager.notify(2, updateNotification.build())
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}