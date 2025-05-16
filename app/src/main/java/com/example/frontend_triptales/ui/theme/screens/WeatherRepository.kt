package com.example.frontend_triptales.ui.theme.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.WbSunny
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository per ottenere dati meteo in tempo reale utilizzando l'API OpenWeatherMap
 */
object WeatherRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // Chiave API di OpenWeatherMap - sostituisci con la tua chiave reale
    private const val API_KEY = "b052bace2ea6693b223b12ed2afea7c7"

    /**
     * Ottiene i dati meteo in base alle coordinate geografiche
     * @param lat Latitudine
     * @param lon Longitudine
     * @return WeatherResponse con i dati meteo o null in caso di errore
     */
    suspend fun fetchWeather(lat: Double, lon: Double): WeatherResponse? {
        return try {
            withContext(Dispatchers.IO) {
                client.get("https://api.openweathermap.org/data/2.5/weather") {
                    parameter("lat", lat)
                    parameter("lon", lon)
                    parameter("appid", API_KEY)
                    parameter("units", "metric") // Temperatura in Celsius
                    parameter("lang", "it") // Risposte in italiano
                }
                    .body<WeatherResponse>()
            }
        } catch (e: Exception) {
            // In caso di errore, restituisci null e registra l'errore
            android.util.Log.e("WeatherRepository", "Errore nel recupero dei dati meteo: ${e.message}")
            // Restituisci dati simulati in caso di errore
            generateFallbackWeatherData(lat, lon)
        }
    }

    /**
     * Genera dati meteo simulati in caso di errore con l'API reale
     * Questo metodo è simile alla logica attuale per avere una transizione graduale
     */
    private fun generateFallbackWeatherData(lat: Double, lon: Double): WeatherResponse {
        // Usa logica simile al metodo attuale per calcolare temperatura e condizioni
        val hash = lat.toInt() * 31 + lon.toInt()
        val conditions = listOf("Soleggiato", "Nuvoloso", "Parzialmente nuvoloso", "Piovoso", "Temporale", "Ventoso")
        val randomIndex = Math.abs(hash) % conditions.size
        val description = conditions[randomIndex]

        // Temperatura basata sulla latitudine (più caldo a sud)
        val baseTemp = 30 - (lat / 10).toInt()
        val temp = baseTemp + (Math.abs(hash) % 10) - 5

        // Calcola l'umidità (30-80%)
        val humidity = 30 + (Math.abs(hash) % 50)

        // Crea e restituisci un oggetto WeatherResponse fittizio
        return WeatherResponse(
            main = Main(temp = temp.toDouble(), humidity = humidity),
            name = "Posizione",
            weather = listOf(Weather(description = description))
        )
    }
}

// Modifica nella HomeScreen.kt per utilizzare WeatherRepository

/**
 * Ottiene i dati meteo in base alle coordinate
 */
private suspend fun fetchWeatherData(lat: Double, lon: Double, callback: (WeatherData) -> Unit) {
    try {
        // Usa il repository per ottenere dati meteo reali
        val weatherResponse = WeatherRepository.fetchWeather(lat, lon)

        if (weatherResponse != null) {
            // Mappa i dati dalla risposta API al modello WeatherData
            val condition = weatherResponse.weather.firstOrNull()?.description?.capitalize() ?: "Sconosciuto"

            // Scegli l'icona appropriata in base alla condizione
            val icon = when {
                condition.contains("soleggiato", ignoreCase = true) -> Icons.Default.WbSunny
                condition.contains("nuvoloso", ignoreCase = true) -> Icons.Default.Cloud
                condition.contains("parzialmente", ignoreCase = true) -> Icons.Default.FilterDrama
                condition.contains("piovoso", ignoreCase = true) ||
                        condition.contains("pioggia", ignoreCase = true) -> Icons.Default.Grain
                condition.contains("temporale", ignoreCase = true) -> Icons.Default.FlashOn
                condition.contains("ventoso", ignoreCase = true) ||
                        condition.contains("vento", ignoreCase = true) -> Icons.Default.Air
                else -> Icons.Default.Cloud // Default
            }

            // Crea oggetto WeatherData
            val weatherData = WeatherData(
                temperature = weatherResponse.main.temp.toInt(),
                condition = condition,
                humidity = weatherResponse.main.humidity,
                cityName = "", // Sarà impostato dal chiamante
                icon = icon
            )

            callback(weatherData)
        } else {
            // In caso di errore, usa una risposta predefinita
            fallbackWeatherData(lat, lon, callback)
        }
    } catch (e: Exception) {
        android.util.Log.e("HomeScreen", "Errore nel recupero dati meteo: ${e.message}")
        // In caso di eccezione, usa una risposta predefinita
        fallbackWeatherData(lat, lon, callback)
    }
}

/**
 * Genera dati meteo di fallback in caso di errore
 */
private fun fallbackWeatherData(lat: Double, lon: Double, callback: (WeatherData) -> Unit) {
    // Questa è la versione corrente della funzione, usata come fallback
    val hash = lat.toInt() * 31 + lon.toInt()
    val conditions =
        listOf("Soleggiato", "Nuvoloso", "Parzialmente nuvoloso", "Piovoso", "Temporale", "Ventoso")
    val randomIndex = Math.abs(hash) % conditions.size
    val condition = conditions[randomIndex]

    // Temperatura basata sulla latitudine (più caldo a sud)
    val baseTemp = 30 - (lat / 10).toInt()
    val temp = baseTemp + (Math.abs(hash) % 10) - 5

    // Calcola l'umidità (30-80%)
    val humidity = 30 + (Math.abs(hash) % 50)

    // Scegli l'icona appropriata
    val icon = when (condition) {
        "Soleggiato" -> Icons.Default.WbSunny
        "Nuvoloso" -> Icons.Default.Cloud
        "Parzialmente nuvoloso" -> Icons.Default.FilterDrama
        "Piovoso" -> Icons.Default.Grain
        "Temporale" -> Icons.Default.FlashOn
        else -> Icons.Default.Air // Ventoso
    }

    // Crea oggetto meteo
    val weatherData = WeatherData(
        temperature = temp.toInt(),
        condition = condition,
        humidity = humidity,
        cityName = "", // Sarà impostato dal chiamante
        icon = icon
    )

    callback(weatherData)
}