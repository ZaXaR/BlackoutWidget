package com.example.blackoutwidget

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

fun fetchScheduleJson(): Pair<String?, Boolean> {
    val endpoint = "https://kiroe.com.ua/electricity-blackout/websearch/v2/140791?ajax=1"

    return try {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0")
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("Network", "HTTP error: ${connection.responseCode}")
            return Pair(null, false)
        }

        val result = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        Log.d("Network", "Успешный ответ: ${result.take(200)}...")
        Pair(result, true)
    } catch (e: Exception) {
        Log.e("Network", "Ошибка запроса: ${e.message}", e)
        Pair(null, false)
    }
}