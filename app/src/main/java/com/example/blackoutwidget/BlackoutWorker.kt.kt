package com.example.blackoutwidget

import androidx.core.content.edit
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class BlackoutWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("blackout_prefs", Context.MODE_PRIVATE)
            val oldData = prefs.getString("last_schedule", null)?.let { BlackoutData.fromJson(it) }

            val (json, success) = fetchScheduleJson()
            if (!success || json == null) return Result.retry()

            val newData = parseBlackoutData(json)
            if (oldData != null && newData != oldData) {
                NotificationHelper.show(applicationContext, "Графік оновлено", "З'явились нові відключення")
            }

            prefs.edit { putString("last_schedule", newData.toJson()) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}