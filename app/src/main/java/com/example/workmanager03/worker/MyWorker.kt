package com.example.workmanager03.worker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.workmanager03.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyWorker(context: Context, workerParameter: WorkerParameters) :
    Worker(context, workerParameter) {

    var currentLocation: Location? = null

    override fun doWork(): Result {
        Log.d(TAG, "doWork: Success")
        setNotification()
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun setNotification() {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val time = LocalDateTime.now().format(formatter)
        val notification = NotificationCompat.Builder(applicationContext, "location")
            .setContentTitle("Background Task")
            .setContentText("Location: $time (${currentLocation?.latitude.toString().take(8)}, " +
                    "${currentLocation?.longitude.toString().take(8)})")
            .setSmallIcon(R.drawable.baseline_my_location)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(NOTIFICATION_ID, notification.build())
        }
    }

    companion object {
        const val CHANNEL_ID = "location"
        const val CHANNEL_NAME = "Location"
        const val NOTIFICATION_ID = 1
        const val TAG = "MyTag"
    }
}