package com.example.workmanager03.worker

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.workmanager03.R
import com.example.workmanager03.connection.*
import com.example.workmanager03.database.LocationDao
import com.example.workmanager03.database.LocationDatabase
import com.example.workmanager03.database.LocationModel
import com.example.workmanager03.service.LocationService
import com.example.workmanager03.service.LocationService.Companion.NOTIFICATION_ID
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyWorker(private val context: Context, workerParameter: WorkerParameters) :
    Worker(context, workerParameter) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var phoneManager: TelephonyManager
    lateinit var locationDao: LocationDao
    private lateinit var sharedPreference: SharedPreferences

    override fun doWork(): Result {
        Log.d(TAG, "doWork: Success")
        val serviceState = isMyServiceRunning(LocationService::class.java)
        sharedPreference = context.getSharedPreferences("LocalMemory", Context.MODE_PRIVATE)

        if (serviceState) {
            Log.d(
                TAG, "doWork: Service activated\n" +
                        "Location: (${LOCATION?.latitude.toString().take(8)}, " +
                        "${LOCATION?.longitude.toString().take(8)})"
            )
            setNotification()
            LOCATION?.let { insertLocationToDatabase(it, getSignal()) }
        } else {
            val manager =
                context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Intent(context, LocationService::class.java).apply {
                    action = LocationService.ACTION_START
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
        val trackerId = sharedPreference.getString("phone", "")

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Tracking location...")
            .setContentText(
                "Location $trackerId: $time (${LOCATION?.latitude.toString().take(8)}, " +
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

    private fun insertLocationToDatabase(location: Location, signal: Int) {
        coroutineScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                LocationDatabase::class.java, "location_database"
            ).build()
            locationDao = db.locationDao()

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val currentTime: String = LocalDateTime.now().format(formatter)

            var distance = -1
            val point = Location("locationA")

            if (sharedPreference.getString("longitude", "") != "") {
                val lat = sharedPreference.getString("latitude", "")
                val long = sharedPreference.getString("longitude", "")
                if (lat != null && long != null) {
                    point.latitude = lat.toDouble()
                    point.longitude = long.toDouble()
                    Log.d(TAG, "location: ${point.latitude}, ${point.longitude}")
                    if (LOCATION != null) {
                        distance = LOCATION!!.distanceTo(point).toInt()
                    }
                }
            }

            Log.d(TAG, "distance: $distance")

            if (distance == -1) {
                Log.d(TAG, "database updated")
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
            } else {
                if (distance > 10) {
                    Log.d(TAG, "database updated")
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
                }
            }

            val editor = sharedPreference.edit()
            editor.putString("longitude", LOCATION!!.longitude.toString().take(8))
            editor.putString("latitude", LOCATION!!.latitude.toString().take(8))
            editor.apply()
            sendLocationToServer(db.locationDao())
        }
    }

    private fun getSignal(): Int {
        phoneManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return phoneManager.signalStrength?.getGsmSignalStrength() ?: 0
    }

    private suspend fun sendLocationToServer(locationDao: LocationDao) {
        val locations = locationDao.getAllLocations()
        if (locations.isNotEmpty()) {
            val location = locationDao.getLocation()
            sendNewLocation(location, locationDao)
        }
    }
    private fun sendNewLocation(location: LocationModel, locationDao: LocationDao){
        val response = ServiceBuilder.buildService(ApiInterface::class.java)
        val phone = sharedPreference.getString("phone", "") ?: ""
        if(phone.isNotEmpty()) {
            val requestModel = RequestModel(
                md5("$phone${location.datetime}bCctS9eqoYaZl21a"),
                location.datetime,
                location.latitude,
                location.longitude,
                location.altitude,
                location.signal,
                phone
            )
            Log.i("myTAG", "$requestModel ")

            if (CheckConnection.isOnline(context)) {
                response.sendReq(requestModel).enqueue(
                    object : Callback<ResponseModel> {
                        override fun onResponse(
                            call: Call<ResponseModel>,
                            response: Response<ResponseModel>
                        ) {
                            Log.d("myTag", response.body()?.status.toString())
                            if (response.body()?.status == 200) {
                                Log.d("myTag", "Good: $location")
                                deleteData(location, locationDao)
                            } else {
                                Log.d("myTag", "Bad connection!")
                            }
                        }

                        override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                            Log.d("myTag", t.toString())
                        }
                    }
                )
            }
        }
    }

    private fun deleteData(location: LocationModel, locationDao: LocationDao) {
        coroutineScope.launch {
            locationDao.deleteLocation(location)
            Log.d("myTag", "Location deleted")
            delay(1000L)
            sendLocationToServer(locationDao)
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    companion object {
        const val CHANNEL_ID = "location"
        const val CHANNEL_NAME = "Location"
        var LOCATION: Location? = null
        const val TAG = "MyTag"
    }
}