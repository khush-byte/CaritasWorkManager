package com.example.workmanager03.worker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.workmanager03.R
import com.google.android.gms.location.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyWorker(private val context: Context, workerParameter: WorkerParameters) :
    ListenableWorker(context, workerParameter) {
    var newLocation: Location? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->
            Log.d("MyTag", "doWork: Success")
            CoroutineScope(Dispatchers.Main).launch {
                currentLocation(completer)
            }
            //getLocation(completer)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setNotification() {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val time = LocalDateTime.now().format(formatter)
        val notification = NotificationCompat.Builder(applicationContext, "location")
            .setContentTitle("Background Task")
            .setContentText(
                "Location: $time (${newLocation?.latitude.toString().take(7)}, " +
                        "${newLocation?.longitude.toString().take(7)})"
            )
            .setSmallIcon(R.drawable.baseline_attach_file)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(NOTIFICATION_ID, notification.build())
        }
    }

    @SuppressLint("MissingPermission")
    private fun currentLocation(completer: CallbackToFutureAdapter.Completer<Result>) {
        Log.d(TAG, "Location")
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(applicationContext)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .apply {
                setWaitForAccurateLocation(false)
                setMinUpdateIntervalMillis(500)
                setMaxUpdateDelayMillis(60000)
            }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.lastOrNull()?.let { location ->
                    Log.d(TAG, "Location: (${location.latitude}, ${location.longitude})")
                    newLocation = location
                    setNotification()
                    completer.set(Result.success())
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(completer: CallbackToFutureAdapter.Completer<Result>) {
        var isLocationFine= true
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if(isLocationFine) {
                    val loc = "${location.latitude},\n${location.longitude}"
                    Log.d("MyTag", loc)
                    newLocation = location
                    setNotification()
                    completer.set(Result.success())
                    isLocationFine = false
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            30000,
            0F,
            locationListener,
            Looper.myLooper()
        )
    }

    companion object {
        const val CHANNEL_ID = "location"
        const val CHANNEL_NAME = "Location"
        const val NOTIFICATION_ID = 1
        const val TAG = "MyTag"
    }
}