package com.example.workmanager03.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import com.example.workmanager03.databinding.ActivityMainBinding
import com.example.workmanager03.worker.MyWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            oneTimeBtn.setOnClickListener {
                myOneTimeWork()
            }

            periodicBtn.setOnClickListener {
                periodicTask()
            }

            cancelBtn.setOnClickListener {
                cancelTask()
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

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, myWorkRequest)
    }

    private fun cancelTask() {
        WorkManager.getInstance(this).cancelAllWork()
    }

//    private fun runService(){
//        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
//        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            //buildAlertMessageNoGps()
//        } else {
//            Intent(applicationContext, LocationService::class.java).apply {
//                startService(this)
//            }
//        }
//    }

    companion object{
        const val WORK_TAG = "my_work"
    }
}
