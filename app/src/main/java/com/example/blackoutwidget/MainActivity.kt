package com.example.blackoutwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = PeriodicWorkRequestBuilder<BlackoutWorker>(15, TimeUnit.MINUTES)
            .addTag("blackout_worker")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "blackout_schedule_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )

        setContent {
            MaterialTheme {
                BlackoutScreen(context = this)
            }
        }
    }
}