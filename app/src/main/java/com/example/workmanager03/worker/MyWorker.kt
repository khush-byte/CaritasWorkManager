package com.example.workmanager03.worker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.*
import com.example.workmanager03.R
import com.example.workmanager03.database.LocationDao
import com.example.workmanager03.database.LocationDatabase
import com.example.workmanager03.database.LocationModel
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
    lateinit var locationDao: LocationDao
    private var checkLoc: String = ""

    private lateinit var phoneManager: TelephonyManager

    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->
            Log.d("MyTag", "doWork: Success")
            CoroutineScope(Dispatchers.Main).launch {
                currentLocation(completer)
            }
//            getLocation(completer)
//            if(LOCATION!=null) insertLocationToDatabase(LOCATION!!, getSignal())
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
                    Log.d(
                        TAG,
                        "Location: (${location.latitude}, ${location.longitude}, ${getSignal()})"
                    )
                    newLocation = location
                    setNotification()
                    insertLocationToDatabase(location, getSignal())
                    completer.set(Result.success())
                    stopLocationUpdates()
                }
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(completer: CallbackToFutureAdapter.Completer<Result>) {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val loc = "${location.latitude},\n${location.longitude}"
                Log.d("MyTag", loc)
                //newLocation = location
                setNotification()
                //LOCATION = location
                completer.set(Result.success())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            }

            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        //Looper.myLooper()?.quit()
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000,
                0F,
                locationListener,
                Looper.getMainLooper()
            )
            //locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.d("MyTag", e.toString())
        }
    }

    private fun insertLocationToDatabase(location: Location, signal: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = Room.databaseBuilder(
                applicationContext,
                LocationDatabase::class.java, "location_database"
            ).build()
            locationDao = db.locationDao()

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val currentTime: String = LocalDateTime.now().format(formatter)
            val currentLocation =
                "${location.latitude.toString().take(7)},${location.longitude.toString().take(7)}"

            if (currentLocation != checkLoc) {
                locationDao.insertLocation(
                    LocationModel(
                        0,
                        currentTime,
                        location.latitude.toString().take(8),
                        location.longitude.toString().take(8),
                        location.altitude.toString().take(7),
                        signal
                    )
                )
                checkLoc = currentLocation
            }
        }
    }

    private fun getSignal(): Int {
        phoneManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return phoneManager.signalStrength?.getGsmSignalStrength() ?: 0
    }

    companion object {
        const val CHANNEL_ID = "location"
        const val CHANNEL_NAME = "Location"
        const val NOTIFICATION_ID = 1
        const val TAG = "MyTag"
        //var LOCATION: Location? = null
    }
}