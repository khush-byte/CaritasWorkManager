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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.workmanager03.R
import com.example.workmanager03.worker.MyWorker
import com.example.workmanager03.worker.MyWorker.Companion.LOCATION

class LocationService : Service() {
    lateinit var notification: NotificationCompat.Builder
    lateinit var notificationManager: NotificationManager
    private lateinit var locationManager: LocationManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MyTag", "Start Service")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
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

    private fun stop() {
        locationManager.removeUpdates(locationListener);
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        locationManager =
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
            val loc = "${location.latitude},\n${location.longitude}"
            Log.d("MyTag", loc)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}