package com.example.workmanager03.activity

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.workmanager03.databinding.ActivityMainBinding
import com.example.workmanager03.service.LocationService
import com.example.workmanager03.worker.MyWorker
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        askPermission()

        binding.apply {
            oneTimeBtn.setOnClickListener {
                val manager = getSystemService(LOCATION_SERVICE) as LocationManager
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps()
                } else {
                    if(checkSinglePermission()) {
                        myOneTimeWork()
                    }else{
                        askPermission()
                    }
                }
            }

            periodicBtn.setOnClickListener {
                val manager = getSystemService(LOCATION_SERVICE) as LocationManager
                if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps()
                } else {
                    if(checkSinglePermission()) {
                        periodicTask()
                    }else{
                        askPermission()
                    }
                }
            }

            cancelBtn.setOnClickListener {
                cancelTask()
                Intent(applicationContext, LocationService::class.java).apply {
                    action = LocationService.ACTION_STOP
                    startService(this)
                }
            }
        }
    }

    private fun myOneTimeWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(true)
            .build()

        val myWorkRequest: WorkRequest = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueue(myWorkRequest)
    }

    private fun periodicTask() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val myWorkRequest = PeriodicWorkRequest.Builder(
            MyWorker::class.java,
            15,
            TimeUnit.MINUTES
        ).setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, myWorkRequest)
    }

    private fun cancelTask() {
        WorkManager.getInstance(this).cancelAllWork()
    }

    private fun askPermission() {
        if (checkSinglePermission()) return
        AlertDialog.Builder(this)
            .setTitle("Permission")
            .setMessage("Please accept all necessary permissions!")
            .setPositiveButton("OK") { _, _ ->
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", packageName, null)
                })
            }
            .create()
            .show()
    }

    private fun checkSinglePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            val permission1 = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            var res: Int = applicationContext.checkCallingOrSelfPermission(permission1)
            if (res == PackageManager.PERMISSION_GRANTED) {
                val permission2 = Manifest.permission.POST_NOTIFICATIONS
                res = applicationContext.checkCallingOrSelfPermission(permission2)
                if (res == PackageManager.PERMISSION_GRANTED) {
                    val permission3 = Manifest.permission.READ_PHONE_STATE
                    res = applicationContext.checkCallingOrSelfPermission(permission3)
                    res == PackageManager.PERMISSION_GRANTED
                } else {
                    false
                }
            } else {
                false
            }
        } else {
            val permission1 = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            var res: Int = applicationContext.checkCallingOrSelfPermission(permission1)
            if (res == PackageManager.PERMISSION_GRANTED) {
                val permission2 = Manifest.permission.READ_PHONE_STATE
                res = applicationContext.checkCallingOrSelfPermission(permission2)
                res == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        }
    }

    private fun buildAlertMessageNoGps() {
        val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Location")
            .setMessage("GPS is disabled, please turn it on!")
            .setCancelable(false)
            .setPositiveButton("OK",
                DialogInterface.OnClickListener { _, _ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) })
        val alert: androidx.appcompat.app.AlertDialog = builder.create()
        alert.show()
    }

    companion object {
        const val WORK_TAG = "my_work"
    }
}
