package com.example.blackoutwidget

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class BlackoutDay(val dayName: String, val intervals: List<String>) {
    override fun equals(other: Any?) =
        other is BlackoutDay && dayName == other.dayName && intervals == other.intervals

    override fun hashCode() = dayName.hashCode() + intervals.hashCode()
}

data class BlackoutData(val updatedAt: String, val days: List<BlackoutDay>) {
    override fun equals(other: Any?) =
        other is BlackoutData && updatedAt == other.updatedAt && days == other.days

    override fun hashCode() = updatedAt.hashCode() + days.hashCode()

    fun toJson(): String {
        val root = JSONObject().apply {
            put("updatedAt", updatedAt)
            put("days", JSONArray(days.map {
                JSONObject().apply {
                    put("dayName", it.dayName)
                    put("intervals", JSONArray(it.intervals))
                }
            }))
        }
        return root.toString()
    }

    companion object {
        fun fromJson(json: String): BlackoutData? = try {
            val root = JSONObject(json)
            val updatedAt = root.optString("updatedAt", "")
            val days = root.optJSONArray("days")?.let { array ->
                List(array.length()) { i ->
                    val obj = array.getJSONObject(i)
                    val name = obj.optString("dayName", "Невідомо")
                    val intervals = obj.optJSONArray("intervals")?.let { ia ->
                        List(ia.length()) { j -> ia.optString(j, "") }.filter { it.isNotBlank() }
                    } ?: emptyList()
                    BlackoutDay(name, intervals)
                }
            } ?: emptyList()
            BlackoutData(updatedAt, days)
        } catch (e: Exception) {
            Log.e("Parser", "Ошибка восстановления: ${e.message}", e)
            null
        }
    }
}

fun parseBlackoutData(json: String): BlackoutData {
    val result = mutableListOf<BlackoutDay>()
    var updatedAt = ""

    try {
        val root = JSONObject(json)
        val dataArray = root.getJSONArray("data")
        if (dataArray.length() == 0) return BlackoutData(updatedAt, result)

        val data = dataArray.getJSONObject(0)
        updatedAt = data.optString("SearchDate", "")

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentDayNo = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1

        val scheduleArray = data.optJSONArray("Shedule") ?: return BlackoutData(updatedAt, result)

        for (i in 0 until scheduleArray.length()) {
            val day = scheduleArray.getJSONObject(i)
            val dayName = day.optString("DayName", "Невідомо")
            val dayNo = day.optInt("DayNo", -1)
            val intervals = mutableListOf<String>()
            var hasKnownData = false

            for (h in 1..24) {
                val value = day.optInt("H%02d".format(h), -1)
                if (value == 0 && (dayNo != currentDayNo || h > currentHour)) {
                    intervals.add("%02d:00 – %02d:00".format(h - 1, h))
                    hasKnownData = true
                } else if (value == 1) {
                    hasKnownData = true
                }
            }

            if (intervals.isNotEmpty() && hasKnownData) {
                result.add(BlackoutDay(dayName, intervals))
            }
        }
    } catch (e: Exception) {
        Log.e("Parser", "Ошибка парсинга: ${e.message}", e)
    }

    return BlackoutData(updatedAt, result)
}