package com.sviridova.nexus.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.sviridova.nexus.Prefs
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Weather via Open-Meteo (no API key, no account).
 * Coordinates come from the last known fix; if the user has not granted
 * location access the widget stays quiet instead of guessing.
 */
object WeatherRepository {

    data class Weather(
        val tempC: Int,
        val apparentC: Int,
        val code: Int,
        val windKmh: Int,
        val humidity: Int,
        val isDay: Boolean,
        val place: String
    )

    private val executor = Executors.newSingleThreadExecutor()

    fun refresh(context: Context, callback: (Weather?) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val permission = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (permission != PackageManager.PERMISSION_GRANTED) {
                callback(null)
                return@execute
            }
            val location = lastKnown(app) ?: run {
                callback(null)
                return@execute
            }
            val weather = runCatching { fetch(location) }.getOrNull()
            if (weather != null) {
                Prefs.weatherTemp = weather.tempC
                Prefs.weatherCode = weather.code
                Prefs.weatherAt = System.currentTimeMillis()
            }
            callback(weather)
        }
    }

    private fun lastKnown(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        return providers.asSequence()
            .mapNotNull { provider -> runCatching { lm.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun fetch(location: Location): Weather {
        val lat = "%.4f".format(location.latitude)
        val lon = "%.4f".format(location.longitude)
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m,relative_humidity_2m" +
                "&timezone=auto"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "NexusLauncher/1.0")
        }
        try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            return Weather(
                tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
                apparentC = Math.round(current.getDouble("apparent_temperature")).toInt(),
                code = current.getInt("weather_code"),
                windKmh = Math.round(current.getDouble("wind_speed_10m")).toInt(),
                humidity = current.getInt("relative_humidity_2m"),
                isDay = current.optInt("is_day", 1) == 1,
                place = ""
            )
        } finally {
            connection.disconnect()
        }
    }

    /** Maps WMO weather codes to a short Russian label. */
    fun describe(code: Int): String = when (code) {
        0 -> "Ясно"
        1, 2 -> "Переменно"
        3 -> "Облачно"
        45, 48 -> "Туман"
        in 51..57 -> "Морось"
        in 61..67 -> "Дождь"
        in 71..77 -> "Снег"
        in 80..82 -> "Ливень"
        in 85..86 -> "Снегопад"
        in 95..99 -> "Гроза"
        else -> "—"
    }
}
