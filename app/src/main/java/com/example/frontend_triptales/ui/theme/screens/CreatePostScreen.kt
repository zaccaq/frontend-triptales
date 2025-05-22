// app/src/main/java/com/example/frontend_triptales/ui/theme/screens/CreatePostScreen.kt
package com.example.frontend_triptales.ui.theme.screens

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.api.CreatePostRequest  // Usa quella dal pacchetto api
import com.example.frontend_triptales.ui.theme.services.LocationData
import com.example.frontend_triptales.ui.theme.services.LocationManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    groupId: String,
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Stati del form
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Stati per la geolocalizzazione
    var currentLocation by remember { mutableStateOf<LocationData?>(null) }
    var isLoadingLocation by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var useCurrentLocation by remember { mutableStateOf(true) }

    // Stati per le immagini
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher per fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            selectedImages = selectedImages + imageUri!!
        }
    }

    // Launcher per galleria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = selectedImages + uris
    }

    // Funzione per creare URI per fotocamera
    fun createImageUri(): Uri {
        val file = File(context.cacheDir, "post_image_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    // Carica la posizione attuale all'avvio
    LaunchedEffect(Unit) {
        if (useCurrentLocation) {
            isLoadingLocation = true
            locationError = null

            try {
                val location = LocationManager.getCurrentLocation(context)
                if (location != null) {
                    currentLocation = location
                    Log.d("CreatePost", "Posizione ottenuta: ${location.placeName}")
                } else {
                    locationError = "Impossibile ottenere la posizione attuale"
                }
            } catch (e: Exception) {
                locationError = "Errore nel recupero della posizione: ${e.message}"
                Log.e("CreatePost", "Errore posizione", e)
            } finally {
                isLoadingLocation = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo Post") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Pulsante per pubblicare
                    TextButton(
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Il titolo è obbligatorio"
                                return@TextButton
                            }

                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null

                                    val api = ServizioApi.getAuthenticatedClient(context)

                                    // 1. Crea il post usando la classe dal pacchetto api
                                    val postRequest = CreatePostRequest(
                                        group = groupId,
                                        title = title,
                                        content = content,
                                        latitude = if (useCurrentLocation) currentLocation?.latitude else null,
                                        longitude = if (useCurrentLocation) currentLocation?.longitude else null,
                                        location_name = if (useCurrentLocation) currentLocation?.placeName else null
                                    )

                                    // Converti in JSON e invia
                                    val response = api.createPost(postRequest)

                                    if (response.isSuccessful && response.body() != null) {
                                        val createdPost = response.body()!!
                                        Log.d("CreatePost", "Post creato con ID: ${createdPost.id}")

                                        // 2. Carica le immagini se presenti
                                        if (selectedImages.isNotEmpty()) {
                                            uploadImages(context, createdPost.id.toString(), selectedImages, currentLocation)
                                        }

                                        Toast.makeText(context, "Post pubblicato con successo!", Toast.LENGTH_SHORT).show()
                                        onPostCreated()
                                    } else {
                                        errorMessage = "Errore nella creazione del post: ${response.code()}"
                                        Log.e("CreatePost", "Errore API: ${response.errorBody()?.string()}")
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Errore: ${e.message}"
                                    Log.e("CreatePost", "Eccezione", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && title.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("Pubblica", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo titolo
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo del post") },
                    placeholder = { Text("Descrivi brevemente la tua esperienza") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF5AC8FA)
                    )
                )
            }

            // Campo contenuto
            item {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenuto") },
                    placeholder = { Text("Racconta i dettagli della tua avventura...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color(0xFF5AC8FA)
                    )
                )
            }

            // Sezione geolocalizzazione
            item {
                LocationSection(
                    currentLocation = currentLocation,
                    useCurrentLocation = useCurrentLocation,
                    isLoadingLocation = isLoadingLocation,
                    locationError = locationError,
                    onToggleLocation = { useCurrentLocation = it },
                    onRefreshLocation = {
                        coroutineScope.launch {
                            isLoadingLocation = true
                            locationError = null
                            try {
                                val location = LocationManager.getCurrentLocation(context)
                                if (location != null) {
                                    currentLocation = location
                                } else {
                                    locationError = "Impossibile ottenere la posizione"
                                }
                            } catch (e: Exception) {
                                locationError = "Errore: ${e.message}"
                            } finally {
                                isLoadingLocation = false
                            }
                        }
                    }
                )
            }

            // Sezione immagini
            item {
                ImageSection(
                    selectedImages = selectedImages,
                    onRemoveImage = { uri ->
                        selectedImages = selectedImages.filter { it != uri }
                    },
                    onTakePhoto = {
                        val uri = createImageUri()
                        imageUri = uri
                        cameraLauncher.launch(uri)
                    },
                    onSelectFromGallery = {
                        galleryLauncher.launch("image/*")
                    }
                )
            }

            // Messaggio di errore
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Spazio finale
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun LocationSection(
    currentLocation: LocationData?,
    useCurrentLocation: Boolean,
    isLoadingLocation: Boolean,
    locationError: String?,
    onToggleLocation: (Boolean) -> Unit,
    onRefreshLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF5AC8FA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Aggiungi posizione",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Switch(
                    checked = useCurrentLocation,
                    onCheckedChange = onToggleLocation,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF5AC8FA)
                    )
                )
            }

            if (useCurrentLocation) {
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingLocation) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF5AC8FA)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rilevamento posizione...", color = Color.Gray)
                    }
                } else if (locationError != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                locationError,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Riprova",
                                tint = Color(0xFF5AC8FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (currentLocation != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                currentLocation.placeName,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Lat: ${String.format("%.4f", currentLocation.latitude)}, " +
                                        "Lon: ${String.format("%.4f", currentLocation.longitude)}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Aggiorna posizione",
                                tint = Color(0xFF5AC8FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageSection(
    selectedImages: List<Uri>,
    onRemoveImage: (Uri) -> Unit,
    onTakePhoto: () -> Unit,
    onSelectFromGallery: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color(0xFF5AC8FA),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Foto (${selectedImages.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pulsanti per aggiungere foto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulsante fotocamera
                OutlinedButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF5AC8FA)
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fotocamera")
                }

                // Pulsante galleria
                OutlinedButton(
                    onClick = onSelectFromGallery,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF5AC8FA)
                    )
                ) {
                    Icon(Icons.Default.Photo, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Galleria")
                }
            }

            // Griglia delle immagini selezionate
            if (selectedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        Box {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            // Pulsante per rimuovere
                            IconButton(
                                onClick = { onRemoveImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Rimuovi",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Funzione helper per caricare immagini
private suspend fun uploadImages(
    context: android.content.Context,
    postId: String,
    images: List<Uri>,
    location: LocationData?
) {
    try {
        val api = ServizioApi.getAuthenticatedClient(context)

        images.forEach { uri ->
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")

            // Copia l'immagine nel file temporaneo
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Prepara la richiesta multipart
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("media_file", file.name, requestFile)
            val postIdPart = postId.toRequestBody("text/plain".toMediaTypeOrNull())

            // Aggiungi dati di posizione se disponibili
            val latitudePart = location?.latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val longitudePart = location?.longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadMedia(
                postId = postIdPart,
                media = imagePart,
                latitude = latitudePart,
                longitude = longitudePart
            )

            if (!response.isSuccessful) {
                Log.e("CreatePost", "Errore nell'upload dell'immagine: ${response.code()}")
            }

            // Pulisci il file temporaneo
            file.delete()
        }
    } catch (e: Exception) {
        Log.e("CreatePost", "Errore nell'upload delle immagini", e)
    }
}