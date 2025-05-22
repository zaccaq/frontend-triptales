package com.example.frontend_triptales.ui.theme.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay

private const val TAG = "LocationUtils"

/**
 * Stati possibili per la posizione dell'utente
 */
sealed class LocationState {
    object Loading : LocationState()
    data class Success(val location: LatLng) : LocationState()
    data class Fallback(val defaultLocation: LatLng) : LocationState()
    data class Error(val message: String) : LocationState()
}

// Posizione predefinita (Roma)
val DEFAULT_LOCATION = LatLng(41.9028, 12.4964)

/**
 * Hook componibile per ottenere la posizione attuale dell'utente
 * Versione semplificata e robusta
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberUserLocation(): State<LocationState> {
    val context = LocalContext.current
    val locationState = remember { mutableStateOf<LocationState>(LocationState.Loading) }
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Richiedi permesso se non concesso
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    // Gestisci la localizzazione
    LaunchedEffect(permissionState.status.isGranted) {
        // Se il permesso è negato, usa la posizione di fallback
        if (!permissionState.status.isGranted) {
            Log.w(TAG, "Permesso di localizzazione negato, uso posizione di fallback")
            locationState.value = LocationState.Fallback(DEFAULT_LOCATION)
            return@LaunchedEffect
        }

        // Verifica se il GPS è attivo
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            Log.w(TAG, "GPS non attivo, uso posizione di fallback")
            locationState.value = LocationState.Error("GPS non attivo")
            return@LaunchedEffect
        }

        // Usa entrambi i metodi per avere maggiori probabilità di successo
        try {
            // 1. Prova con l'ultima posizione conosciuta (più veloce ma potrebbe essere null)
            tryGetLastLocation(context, locationClient) { lastLocation ->
                if (lastLocation != null) {
                    Log.d(TAG, "Posizione ottenuta da lastLocation: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    locationState.value = LocationState.Success(lastLocation)
                } else {
                    Log.d(TAG, "lastLocation è null, continuo con locationUpdates")
                }
            }

            // 2. Avvia comunque gli aggiornamenti di posizione per ottenere dati freschi
            requestLocationUpdates(context, locationClient) { newLocation ->
                Log.d(TAG, "Nuova posizione da updates: ${newLocation.latitude}, ${newLocation.longitude}")
                locationState.value = LocationState.Success(newLocation)
            }

            // 3. Se dopo 10 secondi ancora non abbiamo una posizione, usa il fallback
            delay(10000)
            if (locationState.value is LocationState.Loading) {
                Log.w(TAG, "Timeout nell'ottenere la posizione, uso fallback")
                locationState.value = LocationState.Fallback(DEFAULT_LOCATION)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore nella gestione della posizione: ${e.message}", e)
            locationState.value = LocationState.Fallback(DEFAULT_LOCATION)
        }
    }

    return locationState
}

/**
 * Tenta di ottenere l'ultima posizione conosciuta
 */
@SuppressLint("MissingPermission")
private fun tryGetLastLocation(
    context: Context,
    client: FusedLocationProviderClient,
    onResult: (LatLng?) -> Unit
) {
    // Verifica se il permesso è ancora valido
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
        onResult(null)
        return
    }

    client.lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(LatLng(location.latitude, location.longitude))
            } else {
                Log.d(TAG, "L'ultima posizione conosciuta è null")
                onResult(null)
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Errore nell'ottenere l'ultima posizione: ${e.message}")
            onResult(null)
        }
}

/**
 * Richiede aggiornamenti costanti della posizione
 */
@SuppressLint("MissingPermission")
private fun requestLocationUpdates(
    context: Context,
    client: FusedLocationProviderClient,
    onNewLocation: (LatLng) -> Unit
) {
    // Verifica se il permesso è ancora valido
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    // Configura una richiesta di posizione molto aggressiva
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setMinUpdateIntervalMillis(1000)
        .setMaxUpdateDelayMillis(2000)
        .setWaitForAccurateLocation(false) // Non attendere alta precisione
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                onNewLocation(LatLng(location.latitude, location.longitude))
            }
        }
    }

    client.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    )
}

/**
 * Versione semplificata che restituisce direttamente la LatLng invece che uno stato.
 * Usa una posizione di fallback se la posizione reale non è disponibile.
 */
@Composable
fun rememberCurrentLocation(): LatLng {
    val locationState = rememberUserLocation()

    return when (val state = locationState.value) {
        is LocationState.Success -> state.location
        is LocationState.Fallback -> state.defaultLocation
        is LocationState.Error -> DEFAULT_LOCATION
        LocationState.Loading -> DEFAULT_LOCATION
    }
}