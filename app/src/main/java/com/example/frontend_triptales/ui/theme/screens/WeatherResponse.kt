package com.example.frontend_triptales.ui.theme.screens

import kotlinx.serialization.Serializable

@Serializable
data class WeatherResponse(
    val main: Main,
    val name: String,
    val weather: List<Weather>
)

@Serializable
data class Main(
    val temp: Double,
    val humidity: Int
)

@Serializable
data class Weather(val description: String)
