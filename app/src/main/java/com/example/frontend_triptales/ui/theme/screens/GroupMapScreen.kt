package com.example.frontend_triptales.ui.theme.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.*
import com.example.frontend_triptales.ui.theme.screens.rememberUserLocation
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import com.example.frontend_triptales.ui.theme.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMapScreen(
    groupId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Stati
    var mapPosts by remember { mutableStateOf<List<MapPostDTO>>(emptyList()) }
    var groupName by remember { mutableStateOf("Gruppo") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Stati per aggiungere nuovo post
    var showAddPostDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }

    // Stati per visualizzare dettagli post
    var selectedPost by remember { mutableStateOf<MapPostDTO?>(null) }

    // Posizione utente
    val userLocation = rememberUserLocation()

    // Camera position iniziale
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(41.9028, 12.4964), // Roma come fallback
            12f
        )
    }

    // Carica i post della mappa
    LaunchedEffect(groupId) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.getGroupMapPosts(groupId)

                if (response.isSuccessful && response.body() != null) {
                    val mapResponse = response.body()!!
                    groupName = mapResponse.group_name
                    mapPosts = mapResponse.posts

                    // Se ci sono post, centra la mappa sul primo post
                    if (mapPosts.isNotEmpty()) {
                        val firstPost = mapPosts.first()
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                            LatLng(firstPost.latitude, firstPost.longitude),
                            12f
                        )
                    }
                } else {
                    errorMessage = "Errore nel caricamento della mappa: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione: ${e.message}"
                Log.e("GroupMapScreen", "Errore: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mappa di $groupName") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Ricarica i dati
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    val api = ServizioApi.getAuthenticatedClient(context)
                                    val response = api.getGroupMapPosts(groupId)
                                    if (response.isSuccessful && response.body() != null) {
                                        mapPosts = response.body()!!.posts
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Errore nell'aggiornamento"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPostDialog = true },
                containerColor = Color(0xFF5AC8FA)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi post", tint = Color.White)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF5AC8FA)
                )
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        // Riprova a caricare
                        errorMessage = null
                        isLoading = true
                    }) {
                        Text("Riprova")
                    }
                }
            } else {
                // Mappa
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        compassEnabled = true
                    ),
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        showAddPostDialog = true
                    }
                ) {
                    // Marker per ogni post
                    mapPosts.forEach { post ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(post.latitude, post.longitude)
                            ),
                            title = post.title,
                            snippet = post.content.take(50) + if (post.content.length > 50) "..." else "",
                            onClick = {
                                selectedPost = post
                                true
                            }
                        )
                    }
                }

                // Statistiche in basso
                if (mapPosts.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatisticItem(
                                icon = Icons.Default.LocationOn,
                                value = mapPosts.size.toString(),
                                label = "Luoghi"
                            )
                            StatisticItem(
                                icon = Icons.Default.Search,
                                value = mapPosts.count { it.image_url != null }.toString(),
                                label = "Foto"
                            )
                            StatisticItem(
                                icon = Icons.Default.Favorite,
                                value = mapPosts.sumOf { it.likes_count }.toString(),
                                label = "Like"
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog per aggiungere nuovo post
    if (showAddPostDialog) {
        AddLocationPostDialog(
            groupId = groupId,
            selectedLocation = selectedLocation,
            onDismiss = {
                showAddPostDialog = false
                selectedLocation = null
            },
            onPostAdded = {
                showAddPostDialog = false
                selectedLocation = null
                // Ricarica i post
                coroutineScope.launch {
                    try {
                        val api = ServizioApi.getAuthenticatedClient(context)
                        val response = api.getGroupMapPosts(groupId)
                        if (response.isSuccessful && response.body() != null) {
                            mapPosts = response.body()!!.posts
                        }
                    } catch (e: Exception) {
                        Log.e("GroupMapScreen", "Errore nel ricaricare: ${e.message}")
                    }
                }
            }
        )
    }

    // Dialog per visualizzare dettagli post
    selectedPost?.let { post ->
        PostDetailsDialog(
            post = post,
            onDismiss = { selectedPost = null },
            onLike = { postId ->
                // Implementa il like
                coroutineScope.launch {
                    try {
                        // Implementa chiamata API per like
                        // Poi aggiorna la lista
                    } catch (e: Exception) {
                        Log.e("GroupMapScreen", "Errore like: ${e.message}")
                    }
                }
            }
        )
    }
}

@Composable
fun StatisticItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF5AC8FA),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun AddLocationPostDialog(
    groupId: String,
    selectedLocation: LatLng?,
    onDismiss: () -> Unit,
    onPostAdded: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // Launcher per selezionare immagine
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    // Launcher per fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // L'URI è già impostato
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Aggiungi Post con Posizione",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Posizione selezionata
                selectedLocation?.let { location ->
                    Text(
                        text = "📍 Lat: ${String.format("%.4f", location.latitude)}, Lon: ${String.format("%.4f", location.longitude)}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Campi del form
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Descrizione") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = locationName,
                    onValueChange = { locationName = it },
                    label = { Text("Nome del luogo (opzionale)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsanti per aggiungere immagine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Galleria")
                    }

                    OutlinedButton(
                        onClick = {
                            val uri = createImageUri(context)
                            selectedImageUri = uri
                            cameraLauncher.launch(uri)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fotocamera")
                    }
                }

                // Mostra anteprima immagine
                selectedImageUri?.let { uri ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = uri,
                        contentDescription = "Anteprima",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsanti azione
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) {
                                return@Button
                            }

                            val location = selectedLocation ?: return@Button

                            coroutineScope.launch {
                                try {
                                    isUploading = true

                                    val api = ServizioApi.getAuthenticatedClient(context)

                                    // Prepara i dati
                                    val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val latBody = location.latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val lonBody = location.longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                                    val locationNameBody = locationName.toRequestBody("text/plain".toMediaTypeOrNull())

                                    // Prepara l'immagine se presente
                                    var imagePart: MultipartBody.Part? = null
                                    selectedImageUri?.let { uri ->
                                        val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                                        context.contentResolver.openInputStream(uri)?.use { input ->
                                            file.outputStream().use { output ->
                                                input.copyTo(output)
                                            }
                                        }

                                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                        imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)
                                    }

                                    // Invia la richiesta
                                    val response = api.addLocationPost(
                                        groupId = groupId,
                                        title = titleBody,
                                        content = contentBody,
                                        latitude = latBody,
                                        longitude = lonBody,
                                        locationName = locationNameBody,
                                        image = imagePart
                                    )

                                    if (response.isSuccessful) {
                                        onPostAdded()
                                    } else {
                                        Log.e("AddLocationPost", "Errore: ${response.code()}")
                                    }
                                } catch (e: Exception) {
                                    Log.e("AddLocationPost", "Errore: ${e.message}")
                                } finally {
                                    isUploading = false
                                }
                            }
                        },
                        enabled = !isUploading && title.isNotBlank() && content.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Pubblica")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostDetailsDialog(
    post: MapPostDTO,
    onDismiss: () -> Unit,
    onLike: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header con autore
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (post.author.profile_picture != null) {
                        AsyncImage(
                            model = post.author.profile_picture,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5AC8FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.author.username.first().toString().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = post.author.username,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DateUtils.formatPostDate(post.created_at),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Titolo del post
                Text(
                    text = post.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Posizione
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF5AC8FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = post.location_name,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Immagine se presente
                post.image_url?.let { imageUrl ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Immagine del post",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Contenuto del post
                Text(
                    text = post.content,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Azioni (like, commenti, ecc.)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onLike(post.id) }
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Mi piace",
                                tint = if (post.user_has_liked) Color.Red else Color.Gray
                            )
                        }
                        Text(
                            text = "${post.likes_count}",
                            fontSize = 14.sp
                        )
                    }

                    // Coordinate
                    Text(
                        text = "${String.format("%.4f", post.latitude)}, ${String.format("%.4f", post.longitude)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// Funzione di utilità per creare URI immagine
fun createImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "temp_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}