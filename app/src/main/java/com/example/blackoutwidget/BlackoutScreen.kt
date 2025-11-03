package com.example.blackoutwidget

import androidx.core.content.edit
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlackoutScreen(context: Context) {
    var todayIntervals by remember { mutableStateOf(listOf("Завантаження...")) }
    var tomorrowIntervals by remember { mutableStateOf(emptyList<String>()) }
    var updatedAt by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val prefs = context.getSharedPreferences("blackout_prefs", Context.MODE_PRIVATE)
            val oldJson = prefs.getString("last_schedule", null)
            val oldData = oldJson?.let { BlackoutData.fromJson(it) }

            val (json, success) = withContext(Dispatchers.IO) { fetchScheduleJson() }
            if (!success || json == null) {
                todayIntervals = listOf("Помилка завантаження")
                return@LaunchedEffect
            }

            val newData = parseBlackoutData(json)
            updatedAt = newData.updatedAt.replace("T", " ")
            todayIntervals = newData.days.firstOrNull()?.intervals ?: listOf("Немає даних")
            tomorrowIntervals = newData.days.getOrNull(1)?.intervals ?: emptyList()

            if (oldData != null && newData != oldData) {
                NotificationHelper.show(context, "Графік оновлено", "З'явились нові відключення")
            }

            prefs.edit { putString("last_schedule", newData.toJson()) }
        } catch (e: Exception) {
            todayIntervals = listOf("Помилка: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (updatedAt.isNotEmpty()) {
            Text("🗓 Оновлено: $updatedAt", style = MaterialTheme.typography.labelSmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = "Сьогодні", tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Сьогодні", style = MaterialTheme.typography.titleLarge)
        }

        if (todayIntervals.isEmpty()) {
            Text("Немає запланованих відключень", style = MaterialTheme.typography.bodyMedium)
        } else {
            todayIntervals.forEach {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (tomorrowIntervals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, contentDescription = "Завтра", tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Завтра", style = MaterialTheme.typography.titleLarge)
            }

            tomorrowIntervals.forEach {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}