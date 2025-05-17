package com.example.frontend_triptales.ui.theme.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "LocationUtils"
private const val LOCATION_TIMEOUT_MS = 5000L // 5 secondi timeout per ottenere la posizione

/**
 * Hook componibile per ottenere la posizione attuale dell'utente
 * con gestione delle autorizzazioni e timeout
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberUserLocation(): LatLng? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<LatLng?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        // Richiedi il permesso di localizzazione
        permissionState.launchPermissionRequest()

        // Verifica se il permesso è stato concesso
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                // Ottieni posizione con timeout
                val locationResult = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                    try {
                        val lastLocation = fusedLocationClient.lastLocation.await()
                        if (lastLocation != null) {
                            LatLng(lastLocation.latitude, lastLocation.longitude)
                        } else {
                            // La lastLocation potrebbe essere null se il GPS è stato appena acceso
                            // o se il dispositivo è stato appena avviato
                            Log.w(TAG, "La posizione attuale è null, potrebbe essere necessario attendere")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Errore nell'ottenere la posizione: ${e.message}", e)
                        null
                    }
                }

                // Aggiorna lo stato con la posizione ottenuta
                if (locationResult != null) {
                    Log.d(TAG, "Posizione ottenuta: ${locationResult.latitude}, ${locationResult.longitude}")
                    location = locationResult
                } else {
                    // Se il timeout è scaduto o la posizione è null, usa una posizione di fallback
                    Log.w(TAG, "Timeout o posizione non disponibile, utilizzo posizione di fallback")
                    location = LatLng(41.9028, 12.4964) // Roma come fallback
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore generale nella gestione della posizione: ${e.message}", e)
                location = LatLng(41.9028, 12.4964) // Roma come fallback
            }
        } else {
            Log.w(TAG, "Permesso di localizzazione non concesso")
            location = LatLng(41.9028, 12.4964) // Roma come fallback
        }
    }

    return location
}
