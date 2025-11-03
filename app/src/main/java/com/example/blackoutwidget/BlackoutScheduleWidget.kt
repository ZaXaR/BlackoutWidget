package com.example.blackoutwidget

import androidx.core.content.edit
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlackoutScheduleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        val intent = Intent(context, BlackoutScheduleWidget::class.java).apply {
            action = "com.example.blackoutwidget.REFRESH"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val views = RemoteViews(context.packageName, R.layout.blackout_schedule_widget)
        views.setOnClickPendingIntent(R.id.refresh_button, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetIds[0], views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.example.blackoutwidget.REFRESH" ||
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, BlackoutScheduleWidget::class.java))
            for (id in ids) {
                updateAppWidget(context, manager, id)
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.blackout_schedule_widget)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val prefs = context.getSharedPreferences("blackout_prefs", Context.MODE_PRIVATE)
            val oldData = prefs.getString("last_schedule", null)?.let { BlackoutData.fromJson(it) }

            val (json, success) = fetchScheduleJson()
            if (!success || json == null) {
                views.setTextViewText(R.id.today_schedule, "Помилка завантаження")
                views.setTextViewText(R.id.tomorrow_schedule, "")
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return@launch
            }

            val blackoutData = parseBlackoutData(json)
            val today = blackoutData.days.firstOrNull()
            val tomorrow = blackoutData.days.getOrNull(1)

            val todayText = today?.let {
                "${it.dayName}:\n" + if (it.intervals.isEmpty()) "Немає відключень" else it.intervals.joinToString("\n")
            } ?: "Немає даних"

            val tomorrowText = tomorrow?.let {
                "${it.dayName}:\n" + if (it.intervals.isEmpty()) "Немає відключень" else it.intervals.joinToString("\n")
            } ?: ""

            views.setTextViewText(R.id.today_schedule, todayText)
            views.setTextViewText(R.id.tomorrow_schedule, tomorrowText)

            if (oldData != null && blackoutData != oldData) {
                NotificationHelper.show(context, "Графік оновлено", "З'явились нові відключення")
            }

            prefs.edit { putString("last_schedule", blackoutData.toJson()) }
        } catch (e: Exception) {
            views.setTextViewText(R.id.today_schedule, "Помилка: ${e.message}")
            views.setTextViewText(R.id.tomorrow_schedule, "")
            Log.e("WidgetUpdate", "Ошибка: ${e.message}", e)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}