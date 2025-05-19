package com.example.frontend_triptales.ui.theme.services

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modello per i dati di posizione
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val placeName: String
)

/**
 * Modello per i dati meteo
 */
data class WeatherData(
    val temperature: Int,
    val condition: String,
    val humidity: Int,
    val cityName: String,
    val icon: ImageVector
)