package com.example.workmanager03.worker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.workmanager03.R
import com.example.workmanager03.service.LocationService
import com.example.workmanager03.service.LocationService.Companion.NOTIFICATION_ID
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyWorker(private val context: Context, workerParameter: WorkerParameters) :
    Worker(context, workerParameter) {

    override fun doWork(): Result {
        Log.d("MyTag", "doWork: Success")
        val serviceState = isMyServiceRunning(LocationService::class.java)

        if (serviceState) {
            Log.d(
                "MyTag", "doWork: Service activated\n" +
                        "Location: (${LOCATION?.latitude.toString().take(8)}, " +
                        "${LOCATION?.longitude.toString().take(8)})"
            )
            setNotification()
        } else {
            val manager =
                context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Intent(context, LocationService::class.java).apply {
                    context.startService(this)
                }
            }
        }

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun setNotification() {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val time = LocalDateTime.now().format(formatter)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText(
                "Location: $time (${LOCATION?.latitude.toString().take(8)}, " +
                        "${LOCATION?.longitude.toString().take(8)})"
            )
            .setSmallIcon(R.drawable.baseline_my_location)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(NOTIFICATION_ID, notification.build())
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager =
            context.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager

        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }

        return false
    }

    companion object {
        const val CHANNEL_ID = "location"
        const val CHANNEL_NAME = "Location"
        var LOCATION: Location? = null
    }
}