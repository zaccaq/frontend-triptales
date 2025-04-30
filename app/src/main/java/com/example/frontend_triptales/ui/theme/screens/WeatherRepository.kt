package com.example.frontend_triptales.ui.theme.screens

import com.example.frontend_triptales.ui.theme.screens.WeatherResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object WeatherRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchWeather(lat: Double, lon: Double, apiKey: String): WeatherResponse? {
        return try {
            client.get("https://api.openweathermap.org/data/2.5/weather") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("appid", apiKey)
                parameter("units", "metric")
                parameter("lang", "it")
            }.body()
        } catch (e: Exception) {
            null
        }
    }
}