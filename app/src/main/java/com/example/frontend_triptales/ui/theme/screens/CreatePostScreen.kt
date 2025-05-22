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
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.api.CreatePostRequest
import com.example.frontend_triptales.ui.theme.services.LocationData
import com.example.frontend_triptales.ui.theme.services.LocationManager
import com.example.frontend_triptales.ui.theme.mlkit.MLKitResult
import com.example.frontend_triptales.ui.theme.mlkit.MLKitService
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

    // Stati per ML Kit
    var mlResults by remember { mutableStateOf<Map<Uri, MLKitResult>>(emptyMap()) }
    var isProcessingML by remember { mutableStateOf(false) }
    var showMLResults by remember { mutableStateOf(false) }

    fun processImageWithMLKit(uri: Uri) {
        coroutineScope.launch {
            try {
                isProcessingML = true
                Log.d("CreatePost", "Inizio processamento ML Kit per $uri")

                val inputImage = MLKitService.createInputImageFromUri(context, uri)
                if (inputImage != null) {
                    val result = MLKitService.processImage(inputImage)
                    mlResults = mlResults + (uri to result)

                    // Auto-aggiungi la caption se generata e il contenuto è vuoto
                    if (result.generatedCaption.isNotEmpty() && content.isBlank()) {
                        content = result.generatedCaption
                    }

                    Log.d("CreatePost", "ML Kit processing completato per $uri")
                    Toast.makeText(context, "Analisi AI completata!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("CreatePost", "Impossibile creare InputImage da $uri")
                    Toast.makeText(context, "Errore nell'analisi dell'immagine", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CreatePost", "Errore ML Kit", e)
                Toast.makeText(context, "Errore nell'analisi AI: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isProcessingML = false
            }
        }
    }

    // Launcher per fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            selectedImages = selectedImages + imageUri!!
            // Processa automaticamente con ML Kit
            processImageWithMLKit(imageUri!!)
        }
    }

    // Launcher per galleria
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImages = selectedImages + uris
        // Processa automaticamente tutte le nuove immagini con ML Kit
        uris.forEach { uri -> processImageWithMLKit(uri) }
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

                                    // 1. Crea il post
                                    val postRequest = CreatePostRequest(
                                        group = groupId,
                                        title = title,
                                        content = content,
                                        latitude = if (useCurrentLocation) currentLocation?.latitude else null,
                                        longitude = if (useCurrentLocation) currentLocation?.longitude else null,
                                        location_name = if (useCurrentLocation) currentLocation?.placeName else null
                                    )

                                    val response = api.createPost(postRequest)

                                    if (response.isSuccessful && response.body() != null) {
                                        val createdPost = response.body()!!
                                        Log.d("CreatePost", "Post creato con ID: ${createdPost.id}")

                                        // 2. Carica le immagini se presenti
                                        if (selectedImages.isNotEmpty()) {
                                            uploadImagesWithMLData(
                                                context,
                                                createdPost.id.toString(),
                                                selectedImages,
                                                mlResults,
                                                currentLocation
                                            )
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

            // Sezione immagini con ML Kit
            item {
                EnhancedImageSection(
                    selectedImages = selectedImages,
                    mlResults = mlResults,
                    isProcessingML = isProcessingML,
                    onRemoveImage = { uri ->
                        selectedImages = selectedImages.filter { it != uri }
                        mlResults = mlResults - uri
                    },
                    onTakePhoto = {
                        val uri = createImageUri()
                        imageUri = uri
                        cameraLauncher.launch(uri)
                    },
                    onSelectFromGallery = {
                        galleryLauncher.launch("image/*")
                    },
                    onProcessML = { uri -> processImageWithMLKit(uri) },
                    onShowMLResults = { showMLResults = true }
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

    // Dialog per mostrare risultati ML Kit
    if (showMLResults) {
        MLResultsDialog(
            results = mlResults,
            onDismiss = { showMLResults = false },
            onApplyCaption = { caption ->
                content = caption
                showMLResults = false
            }
        )
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

                when {
                    isLoadingLocation -> {
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
                    }
                    locationError != null -> {
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
                    }
                    currentLocation != null -> {
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
}

@Composable
fun EnhancedImageSection(
    selectedImages: List<Uri>,
    mlResults: Map<Uri, MLKitResult>,
    isProcessingML: Boolean,
    onRemoveImage: (Uri) -> Unit,
    onTakePhoto: () -> Unit,
    onSelectFromGallery: () -> Unit,
    onProcessML: (Uri) -> Unit,
    onShowMLResults: () -> Unit
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF5AC8FA),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Foto con AI (${selectedImages.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (mlResults.isNotEmpty()) {
                    Row {
                        Text(
                            "${mlResults.size} analizzate",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onShowMLResults,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = "Risultati AI",
                                tint = Color(0xFF5AC8FA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Indicatore processing ML Kit
            if (isProcessingML) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF5AC8FA)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analisi AI in corso...", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pulsanti per aggiungere foto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

            // Griglia immagini con analisi ML
            if (selectedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImages) { uri ->
                        ImageWithMLKit(
                            uri = uri,
                            mlResult = mlResults[uri],
                            onRemove = { onRemoveImage(uri) },
                            onProcessML = { onProcessML(uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageWithMLKit(
    uri: Uri,
    mlResult: MLKitResult?,
    onRemove: () -> Unit,
    onProcessML: () -> Unit
) {
    Box {
        Column {
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

            // Indicatori ML Kit
            Row(
                modifier = Modifier.width(80.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                if (mlResult != null) {
                    // Mostra badge se ha risultati ML
                    if (mlResult.detectedObjects.isNotEmpty()) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Oggetti rilevati",
                            tint = Color.Green,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (mlResult.extractedText.isNotEmpty()) {
                        Icon(
                            Icons.Default.TextFields,
                            contentDescription = "Testo rilevato",
                            tint = Color.Blue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    if (mlResult.generatedCaption.isNotEmpty()) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "Caption generata",
                            tint = Color.Red,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                } else {
                    // Pulsante per processare con ML Kit
                    IconButton(
                        onClick = onProcessML,
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = "Analizza con AI",
                            tint = Color(0xFF5AC8FA),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Pulsante rimuovi
        IconButton(
            onClick = onRemove,
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

@Composable
fun MLResultsDialog(
    results: Map<Uri, MLKitResult>,
    onDismiss: () -> Unit,
    onApplyCaption: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Risultati Analisi AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    results.forEach { (_, result) ->
                        item {
                            MLResultItem(
                                result = result,
                                onApplyCaption = onApplyCaption
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MLResultItem(
    result: MLKitResult,
    onApplyCaption: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F7FF)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Oggetti rilevati
            if (result.detectedObjects.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Oggetti rilevati:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                result.detectedObjects.forEach { obj ->
                    Text(
                        "• ${obj.label} (${(obj.confidence * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Testo estratto
            if (result.extractedText.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TextFields,
                        contentDescription = null,
                        tint = Color.Blue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Testo estratto:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    result.extractedText,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )

                if (result.translatedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Translate,
                            contentDescription = null,
                            tint = Color.Magenta,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Traduzione:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        result.translatedText,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Caption generata
            if (result.generatedCaption.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Caption suggerita:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    result.generatedCaption,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 20.dp, top = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onApplyCaption(result.generatedCaption) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5AC8FA)
                    )
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Usa questa caption")
                }
            }
        }
    }
}

// Funzione helper per caricare immagini con dati ML Kit
private suspend fun uploadImagesWithMLData(
    context: android.content.Context,
    postId: String,
    images: List<Uri>,
    mlResults: Map<Uri, MLKitResult>,
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

            // Aggiungi dati ML Kit se disponibili
            val mlResult = mlResults[uri]
            val detectedObjectsPart = if (mlResult?.detectedObjects?.isNotEmpty() == true) {
                // Converti la lista di oggetti in JSON string
                val objectsJson = mlResult.detectedObjects.joinToString(",") {
                    """{"label":"${it.label}","confidence":${it.confidence}}"""
                }
                "[$objectsJson]".toRequestBody("text/plain".toMediaTypeOrNull())
            } else null

            val ocrTextPart = mlResult?.extractedText?.takeIf { it.isNotEmpty() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            val captionPart = mlResult?.generatedCaption?.takeIf { it.isNotEmpty() }
                ?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadMedia(
                postId = postIdPart,
                media = imagePart,
                latitude = latitudePart,
                longitude = longitudePart,
                detectedObjects = detectedObjectsPart,
                ocrText = ocrTextPart,
                caption = captionPart
            )

            if (response.isSuccessful) {
                Log.d("CreatePost", "Upload completato per $uri con dati ML Kit")
            } else {
                Log.e("CreatePost", "Errore nell'upload dell'immagine: ${response.code()}")
            }

            // Pulisci il file temporaneo
            file.delete()
        }

        Log.d("CreatePost", "Upload di tutte le immagini completato")
    } catch (e: Exception) {
        Log.e("CreatePost", "Errore nell'upload delle immagini con ML Kit", e)
    }
}