package com.example.frontend_triptales.ui.theme.services

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.frontend_triptales.ui.theme.services.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Servizio per ottenere dati meteo in tempo reale
 */
object WeatherService {
    private const val TAG = "WeatherService"
    private const val API_KEY = "b052bace2ea6693b223b12ed2afea7c7" // La tua chiave OpenWeatherMap
    private const val WEATHER_API_URL = "https://api.openweathermap.org/data/2.5/weather"
    private const val TIMEOUT_MS = 5000 // 5 secondi di timeout

    /**
     * Recupera i dati meteo in base alle coordinate geografiche
     */
    suspend fun getWeatherByCoordinates(latitude: Double, longitude: Double): WeatherData? {
        return try {
            val weatherData = withTimeoutOrNull(TIMEOUT_MS.toLong()) {
                withContext(Dispatchers.IO) {
                    // Costruisci l'URL con i parametri
                    val url = URL("$WEATHER_API_URL?lat=$latitude&lon=$longitude&units=metric&lang=it&appid=$API_KEY")

                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = TIMEOUT_MS
                    connection.readTimeout = TIMEOUT_MS

                    try {
                        val responseCode = connection.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            // Leggi la risposta
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            parseWeatherResponse(response)
                        } else {
                            Log.e(TAG, "Errore HTTP: $responseCode")
                            null
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }

            if (weatherData != null) {
                return weatherData
            } else {
                Log.w(TAG, "Timeout o risposta nulla, generando dati fallback")
                generateFallbackWeatherData(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel recupero dati meteo: ${e.message}", e)
            generateFallbackWeatherData(latitude, longitude)
        }
    }

    /**
     * Parsing della risposta JSON dell'API meteo
     */
    private fun parseWeatherResponse(jsonResponse: String): WeatherData {
        try {
            val json = JSONObject(jsonResponse)

            // Estrai i dati principali
            val main = json.getJSONObject("main")
            val temperature = main.getDouble("temp").toInt()
            val humidity = main.getInt("humidity")

            // Estrai la condizione meteo
            val weatherArray = json.getJSONArray("weather")
            val weatherObject = weatherArray.getJSONObject(0)
            val condition = weatherObject.getString("description").capitalize()

            // Nome della località
            val cityName = json.getString("name")

            // Scegli l'icona appropriata in base alla condizione
            val icon = selectWeatherIcon(condition)

            return WeatherData(
                temperature = temperature,
                condition = condition,
                humidity = humidity,
                cityName = cityName,
                icon = icon
            )
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel parsing JSON: ${e.message}", e)
            throw e
        }
    }

    /**
     * Genera dati meteo fallback deterministici in base alle coordinate
     */
    private fun generateFallbackWeatherData(latitude: Double, longitude: Double): WeatherData {
        // Hash basato sulle coordinate per ottenere risultati deterministici
        val hash = Math.abs((latitude.toInt() * 31 + longitude.toInt()))

        val conditions = listOf(
            "Soleggiato", "Nuvoloso", "Parzialmente nuvoloso",
            "Piovoso", "Temporale", "Ventoso"
        )

        // Seleziona condizione meteo in base all'hash
        val randomIndex = hash % conditions.size
        val condition = conditions[randomIndex]

        // Calcola una temperatura realistica basata sulla latitudine
        val baseTemp = 30 - (latitude / 10).toInt()
        val temp = baseTemp + (hash % 10) - 5

        // Umidità realistica (30-80%)
        val humidity = 30 + (hash % 50)

        // Icona appropriata in base alla condizione
        val icon = selectWeatherIcon(condition)

        return WeatherData(
            temperature = temp.toInt(),
            condition = condition,
            humidity = humidity,
            cityName = "Posizione rilevata",
            icon = icon
        ).also {
            Log.d(TAG, "Generati dati meteo fallback: $temp°C, $condition")
        }
    }

    /**
     * Seleziona l'icona meteo appropriata in base alla descrizione
     */
    private fun selectWeatherIcon(condition: String): ImageVector {
        return when {
            condition.containsAny("sereno", "sole", "soleggiato") -> Icons.Default.WbSunny
            condition.containsAny("nuvoloso", "nuvole", "coperto", "nebbia", "foschia") -> Icons.Default.Cloud
            condition.containsAny("pioggia", "pioviggine") -> Icons.Default.Grain
            condition.containsAny("temporale") -> Icons.Default.FlashOn
            condition.containsAny("neve") -> Icons.Default.AcUnit
            condition.containsAny("vento", "ventoso") -> Icons.Default.Air
            else -> Icons.Default.WbSunny // Default soleggiato
        }
    }

    // Extension per controllare se una stringa contiene una delle sottostringhe specificate
    private fun String.containsAny(vararg substrings: String): Boolean {
        return substrings.any { this.contains(it, ignoreCase = true) }
    }

    // Extension per capitalizzare la prima lettera
    private fun String.capitalize(): String {
        return if (this.isNotEmpty()) {
            this[0].uppercase() + this.substring(1)
        } else {
            this
        }
    }
}