package com.example.frontend_triptales.ui.theme.services

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frontend_triptales.ui.theme.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val weatherRepository = WeatherRepository(application)

    // Esponi i flussi di dati dal repository
    val weatherData: StateFlow<WeatherData?> = weatherRepository.weatherData
    val locationData: StateFlow<LocationData?> = weatherRepository.locationData
    val isLoading: StateFlow<Boolean> = weatherRepository.isLoading
    val error: StateFlow<String?> = weatherRepository.error

    init {
        // Carica i dati all'avvio del ViewModel
        loadLocationAndWeather()
    }

    /**
     * Avvia il caricamento della posizione e dei dati meteo
     */
    fun loadLocationAndWeather() {
        viewModelScope.launch {
            weatherRepository.fetchLocationAndWeather()
        }
    }

    /**
     * Aggiorna solo i dati meteo
     */
    fun refreshWeather() {
        viewModelScope.launch {
            weatherRepository.refreshWeatherData()
        }
    }
}