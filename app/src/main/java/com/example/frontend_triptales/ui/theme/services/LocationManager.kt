package com.example.frontend_triptales.ui.theme.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.frontend_triptales.ui.theme.*
import com.google.android.gms.location.LocationServices
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
    private const val LOCATION_TIMEOUT_MS = 10000L // 10 secondi di timeout

    /**
     * Ottiene la posizione attuale dell'utente con geocoding per ottenere il nome della località
     * @return LocationData con coordinate e nome della località, o null in caso di errore
     */
    suspend fun getCurrentLocation(context: Context): LocationData? {
        return try {
            // Verifica se abbiamo i permessi per la localizzazione
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Permessi di localizzazione non concessi")
                return null
            }

            // Ottiene la posizione attuale
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Utilizza coroutine con timeout per evitare attese infinite
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                Log.d(TAG, "Posizione ottenuta: ${location.latitude}, ${location.longitude}")
                                continuation.resume(location)
                            } else {
                                Log.w(TAG, "Posizione null, potrebbe essere necessario attendere")
                                continuation.resume(null)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Errore nel recupero della posizione", e)
                            continuation.resumeWithException(e)
                        }
                }
            }

            if (location == null) {
                Log.w(TAG, "Timeout o posizione non disponibile")
                return null
            }

            // Geocoding per ottenere il nome della località
            val placeName = getPlaceNameFromCoordinates(context, location.latitude, location.longitude)

            LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                placeName = placeName ?: "Posizione sconosciuta"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Errore generale nella gestione della posizione: ${e.message}", e)
            null
        }
    }

    /**
     * Ottiene il nome della località dalle coordinate
     */
    private suspend fun getPlaceNameFromCoordinates(context: Context, latitude: Double, longitude: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                // API diversa per Android 13 (API 33) e successivi
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    var result: String? = null

                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            result = addresses[0].locality ?:
                                    addresses[0].subAdminArea ?:
                                    addresses[0].adminArea
                        }
                    }

                    return@withContext result
                } else {
                    // Versioni precedenti di Android
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)

                    if (addresses != null && addresses.isNotEmpty()) {
                        return@withContext addresses[0].locality ?:
                        addresses[0].subAdminArea ?:
                        addresses[0].adminArea
                    }
                }

                null
            } catch (e: Exception) {
                Log.e(TAG, "Errore nel geocoding: ${e.message}", e)
                null
            }
        }
    }
}