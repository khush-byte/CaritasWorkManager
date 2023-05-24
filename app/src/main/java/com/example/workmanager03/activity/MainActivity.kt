package com.example.workmanager03.activity

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.workmanager03.databinding.ActivityMainBinding
import com.example.workmanager03.service.LocationService
import com.example.workmanager03.worker.MyWorker
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        askPermission()
        checkStatus()

        binding.apply {
            pinBoard.visibility = View.GONE
            oneTimeBtn.visibility = View.GONE

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
                        binding.mainInfoField.text = "Программа работает\nПожалуйста, не трогайте кнопки!"
                        binding.mainInfoField.setTextColor(Color.parseColor("#15DC43"));
                    }else{
                        askPermission()
                    }
                }
            }

            cancelBtn.setOnClickListener {
                pinCheck()
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
        WorkManager.getInstance(this).cancelAllWork()
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

    private fun pinCheck() {
        binding.pinBoard.visibility = View.VISIBLE

        binding.btnPinCancel.setOnClickListener {
            binding.pinEnterField.text.clear()
            binding.pinBoard.visibility = View.GONE
        }

        binding.btnPinOk.setOnClickListener {
            if (binding.pinEnterField.text.isNotEmpty()) {
                Log.d("MyTag", "${md5(binding.pinEnterField.text.toString())}, $PIN")
                if (md5(binding.pinEnterField.text.toString()) == PIN) {
                    cancelTask()
                    Intent(applicationContext, LocationService::class.java).apply {
                        action = LocationService.ACTION_STOP
                        startService(this)
                    }
                    binding.pinEnterField.text.clear()
                    binding.pinBoard.visibility = View.GONE
                } else {
                    Toast.makeText(this@MainActivity, "Incorrect PIN code!", Toast.LENGTH_SHORT)
                        .show()
                    binding.pinEnterField.text.clear()
                }
            } else {
                Toast.makeText(this@MainActivity, "Enter PIN code please!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    private fun checkStatus(){
        val serviceState = isMyServiceRunning(LocationService::class.java)

        if(serviceState && isWorkScheduled(WORK_TAG)) {
            binding.mainInfoField.text = "Программа работает\nПожалуйста, не трогайте кнопки!"
            binding.mainInfoField.setTextColor(Color.parseColor("#15DC43"));
        }else{
            binding.mainInfoField.text = "Программа отключена\nНажмите кнопку СТАРТ, чтобы запустить программу!"
            binding.mainInfoField.setTextColor(Color.parseColor("#F22727"));
        }
    }

    private fun isWorkScheduled(tag: String): Boolean {
        val instance = WorkManager.getInstance()
        val statuses = instance.getWorkInfosByTag(tag)
        return try {
            var running = false
            val workInfoList = statuses.get()
            for (workInfo in workInfoList) {
                val state = workInfo.state
                running = (state == WorkInfo.State.RUNNING) or (state == WorkInfo.State.ENQUEUED)
            }
            running
        } catch (e: ExecutionException) {
            e.printStackTrace()
            false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        const val PIN = "eed7116087a9e84bb0609f459bdd57e0"
        const val WORK_TAG = "my_work"
    }
}
