package com.example.frontend_triptales.ui.theme.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager as AndroidLocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.frontend_triptales.ui.theme.*
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Gestore centralizzato per tutte le operazioni di geolocalizzazione
 */
object LocationManager {
    private const val TAG = "LocationManager"
    private const val LOCATION_TIMEOUT_MS = 15000L // 15 secondi di timeout

    /**
     * Ottiene la posizione attuale dell'utente con geocoding per ottenere il nome della località
     * @return LocationData con coordinate e nome della località, o null in caso di errore
     */
    suspend fun getCurrentLocation(context: Context): LocationData? {
        return try {
            // Verifica se abbiamo i permessi per la localizzazione
            if (!hasLocationPermissions(context)) {
                Log.w(TAG, "Permessi di localizzazione non concessi")
                return null
            }

            // Verifica se i servizi di localizzazione sono abilitati
            if (!isLocationEnabled(context)) {
                Log.w(TAG, "Servizi di localizzazione disabilitati")
                return null
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Prima prova con l'ultima posizione conosciuta
            val lastLocation = getLastKnownLocation(fusedLocationClient)
            if (lastLocation != null) {
                Log.d(TAG, "Utilizzando ultima posizione conosciuta: ${lastLocation.latitude}, ${lastLocation.longitude}")
                val placeName = getPlaceNameFromCoordinates(context, lastLocation.latitude, lastLocation.longitude)
                return LocationData(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    placeName = placeName ?: "Posizione sconosciuta"
                )
            }

            // Se non c'è una posizione recente, richiedi una nuova posizione
            Log.d(TAG, "Richiedendo nuova posizione...")
            val newLocation = requestNewLocation(fusedLocationClient)

            if (newLocation == null) {
                Log.w(TAG, "Impossibile ottenere la posizione")
                return null
            }

            // Geocoding per ottenere il nome della località
            val placeName = getPlaceNameFromCoordinates(context, newLocation.latitude, newLocation.longitude)

            LocationData(
                latitude = newLocation.latitude,
                longitude = newLocation.longitude,
                placeName = placeName ?: "Posizione sconosciuta"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Errore generale nella gestione della posizione: ${e.message}", e)
            null
        }
    }

    /**
     * Verifica se l'app ha i permessi di localizzazione
     */
    private fun hasLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica se i servizi di localizzazione sono abilitati
     */
    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as AndroidLocationManager
        return locationManager.isProviderEnabled(AndroidLocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(AndroidLocationManager.NETWORK_PROVIDER)
    }

    /**
     * Ottiene l'ultima posizione conosciuta
     */
    private suspend fun getLastKnownLocation(fusedLocationClient: FusedLocationProviderClient): android.location.Location? {
        return withTimeoutOrNull(5000L) {
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            Log.d(TAG, "Ultima posizione: ${location?.let { "${it.latitude}, ${it.longitude}" } ?: "null"}")
                            continuation.resume(location)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Errore nel recupero dell'ultima posizione", e)
                            continuation.resume(null)
                        }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Errore di sicurezza nell'accesso alla posizione", e)
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Richiede una nuova posizione in tempo reale
     */
    private suspend fun requestNewLocation(fusedLocationClient: FusedLocationProviderClient): android.location.Location? {
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(500L)
                    .setMaxUpdateDelayMillis(2000L)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        Log.d(TAG, "Nuova posizione ricevuta")
                        fusedLocationClient.removeLocationUpdates(this)
                        val location = locationResult.lastLocation
                        if (location != null) {
                            Log.d(TAG, "Posizione ottenuta: ${location.latitude}, ${location.longitude}")
                            continuation.resume(location)
                        } else {
                            Log.w(TAG, "LocationResult senza posizione valida")
                            continuation.resume(null)
                        }
                    }

                    override fun onLocationAvailability(availability: LocationAvailability) {
                        if (!availability.isLocationAvailable) {
                            Log.w(TAG, "Localizzazione non disponibile")
                            fusedLocationClient.removeLocationUpdates(this)
                            continuation.resume(null)
                        }
                    }
                }

                try {
                    Log.d(TAG, "Avviando richiesta di localizzazione...")
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )

                    // Cleanup quando la coroutine viene cancellata
                    continuation.invokeOnCancellation {
                        Log.d(TAG, "Rimozione location updates per cancellazione")
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Errore di sicurezza nella richiesta di posizione", e)
                    continuation.resume(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Errore generico nella richiesta di posizione", e)
                    continuation.resume(null)
                }
            }
        }
    }

    /**
     * Ottiene il nome della località dalle coordinate
     */
    private suspend fun getPlaceNameFromCoordinates(context: Context, latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // Verifica se il geocoder è disponibile
                if (!Geocoder.isPresent()) {
                    Log.w(TAG, "Geocoder non disponibile su questo dispositivo")
                    return@withContext null
                }

                // API diversa per Android 13 (API 33) e successivi
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    return@withContext suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val placeName = address.locality ?:
                                address.subAdminArea ?:
                                address.adminArea ?:
                                address.countryName
                                Log.d(TAG, "Geocoding riuscito: $placeName")
                                continuation.resume(placeName)
                            } else {
                                Log.w(TAG, "Nessun indirizzo trovato per le coordinate")
                                continuation.resume(null)
                            }
                        }
                    }
                } else {
                    // Versioni precedenti di Android
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val placeName = address.locality ?:
                        address.subAdminArea ?:
                        address.adminArea ?:
                        address.countryName
                        Log.d(TAG, "Geocoding riuscito: $placeName")
                        return@withContext placeName
                    }
                }

                Log.w(TAG, "Geocoding fallito - nessun risultato")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel geocoding: ${e.message}", e)
                null
            }
        }
    }
}