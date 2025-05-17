package com.example.frontend_triptales.ui.theme.services

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.frontend_triptales.api.WeatherService
import com.example.frontend_triptales.ui.theme.screens.WeatherData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.*

/**
 * Servizio centralizzato per gestire le operazioni relative al meteo
 * Implementa pattern singleton per essere accessibile facilmente
 */
object WeatherManager {
    private const val TAG = "WeatherManager"
    private const val TIMEOUT_MS = 5000L // Timeout per le chiamate API (5 secondi)

    /**
     * Ottiene i dati meteo in base alle coordinate geografiche
     * Con gestione degli errori e fallback automatico
     */
    suspend fun getWeatherData(lat: Double, lon: Double, cityName: String? = null): WeatherData {
        return try {
            // Prova a ottenere i dati meteo entro un timeout specificato
            val response = withTimeoutOrNull(TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    WeatherService.getWeatherByCoordinates(lat, lon)
                }
            }

            if (response != null) {
                // Conversione dati API
                val temperature = response.main.temp.toInt()
                val condition = response.weather.firstOrNull()?.description?.capitalizeFirst() ?: "Sconosciuto"
                val humidity = response.main.humidity

                // Crea oggetto WeatherData
                WeatherData(
                    temperature = temperature,
                    condition = condition,
                    humidity = humidity,
                    cityName = cityName ?: response.name,
                    icon = selectWeatherIcon(condition)
                ).also {
                    Log.d(TAG, "Dati meteo da API: $temperature°C, $condition")
                }
            } else {
                Log.w(TAG, "API meteo timeout o risposta nulla, uso dati fallback")
                generateFallbackWeatherData(lat, lon, cityName)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel recupero dati meteo: ${e.message}", e)
            generateFallbackWeatherData(lat, lon, cityName)
        }
    }

    /**
     * Genera dati meteo fallback deterministici in base alle coordinate
     */
    private fun generateFallbackWeatherData(lat: Double, lon: Double, customCityName: String? = null): WeatherData {
        // Hash basato sulle coordinate per ottenere risultati deterministici
        val hash = (lat.toInt() * 31 + lon.toInt()).absoluteValue

        val conditions = listOf(
            "Soleggiato", "Nuvoloso", "Parzialmente nuvoloso",
            "Piovoso", "Temporale", "Ventoso"
        )

        // Seleziona condizione meteo in base all'hash
        val randomIndex = hash % conditions.size
        val condition = conditions[randomIndex]

        // Calcola una temperatura realistica basata sulla latitudine
        val baseTemp = 30 - (lat / 10).toInt()
        val temp = baseTemp + (hash % 10) - 5

        // Umidità realistica (30-80%)
        val humidity = 30 + (hash % 50)

        // Icona appropriata in base alla condizione
        val icon = when (condition) {
            "Soleggiato" -> Icons.Default.WbSunny
            "Nuvoloso" -> Icons.Default.Cloud
            "Parzialmente nuvoloso" -> Icons.Default.FilterDrama
            "Piovoso" -> Icons.Default.Grain
            "Temporale" -> Icons.Default.FlashOn
            else -> Icons.Default.Air // Ventoso
        }

        return WeatherData(
            temperature = temp.toInt(),
            condition = condition,
            humidity = humidity,
            cityName = customCityName ?: "Posizione rilevata",
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

    /**
     * Extensions utili
     */
    private fun String.capitalizeFirst(): String {
        return if (isNotEmpty()) {
            this[0].uppercase() + substring(1)
        } else {
            this
        }
    }

    private fun String.containsAny(vararg substrings: String): Boolean {
        return substrings.any { this.contains(it, ignoreCase = true) }
    }

    private val Int.absoluteValue: Int
        get() = if (this < 0) -this else this
}