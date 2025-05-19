package com.example.frontend_triptales.ui.theme.services

import android.content.Context
import com.example.frontend_triptales.ui.theme.screens.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository che combina la geolocalizzazione e i dati meteo
 */
class WeatherRepository(private val context: Context) {

    // StateFlow per esporre i dati meteo alla UI
    private val _weatherData = MutableStateFlow<WeatherData?>(null)
    val weatherData: StateFlow<WeatherData?> = _weatherData.asStateFlow()

    // StateFlow per la posizione
    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData: StateFlow<LocationData?> = _locationData.asStateFlow()

    // StateFlow per stato di caricamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // StateFlow per errori
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Recupera la posizione e i dati meteo in un'unica operazione
     */
    suspend fun fetchLocationAndWeather() {
        try {
            _isLoading.value = true
            _error.value = null

            // Prima ottieni la posizione
            val location = LocationManager.getCurrentLocation(context)
            _locationData.value = location

            if (location != null) {
                // Poi ottieni i dati meteo basati sulla posizione
                val weather = WeatherService.getWeatherByCoordinates(
                    location.latitude,
                    location.longitude
                )

                // Aggiorna il nome della città nei dati meteo se necessario
                if (weather != null && weather.cityName == "Posizione rilevata" && location.placeName != "Posizione sconosciuta") {
                    _weatherData.value = weather.copy(cityName = location.placeName)
                } else {
                    _weatherData.value = weather
                }
            } else {
                _error.value = "Impossibile ottenere la posizione"
            }
        } catch (e: Exception) {
            _error.value = "Errore: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Aggiorna solo i dati meteo (utile per refresh)
     */
    suspend fun refreshWeatherData() {
        try {
            val location = _locationData.value ?: return

            _isLoading.value = true
            _error.value = null

            val weather = WeatherService.getWeatherByCoordinates(
                location.latitude,
                location.longitude
            )

            if (weather != null) {
                _weatherData.value = weather
            }
        } catch (e: Exception) {
            _error.value = "Errore nell'aggiornamento meteo: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
}