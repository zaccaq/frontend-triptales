import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.auth.SessionManager
import com.example.frontend_triptales.ui.theme.components.AIAssistantButton
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.ui.theme.services.HomeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data classes esterne
data class PostItem(
    val id: String,
    val userName: String,
    val userAvatar: String? = null,
    val groupName: String,
    val content: String,
    val location: String = "",
    val timestamp: String,
    val mediaUrl: String? = null,
    val likesCount: Int = 0,
    val isMyPost: Boolean = false
)

data class StatItem(
    val label: String,
    val value: Int,
    val color: Color
)

data class PlaceItem(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val distance: String,
    val rating: Float
)

// Componente PostCard esterno
@Composable
fun PostCard(post: PostItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (post.isMyPost) Color(0xFFF5F9FF) else Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Intestazione del post
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar utente
                if (post.userAvatar != null) {
                    AsyncImage(
                        model = post.userAvatar,
                        contentDescription = "Avatar di ${post.userName}",
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
                            text = post.userName.first().toString().uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Informazioni post
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = post.userName,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "in ",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = post.groupName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF5AC8FA)
                        )
                        Text(
                            text = " • ${post.timestamp}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    if (post.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = post.location,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }

                // Badge "Il tuo post" se è dell'utente corrente
                if (post.isMyPost) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF5AC8FA).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Il tuo post",
                            fontSize = 12.sp,
                            color = Color(0xFF5AC8FA),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Contenuto del post
            if (post.content.isNotBlank()) {
                Text(
                    text = post.content,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Immagine del post
            post.mediaUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Immagine del post",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            // Barra delle azioni
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Implementazione like */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Mi piace",
                        tint = Color(0xFFFF4081)
                    )
                }

                Text(
                    text = "${post.likesCount}",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { /* Implementazione commento */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Commenta"
                    )
                }

                IconButton(
                    onClick = { /* Implementazione condivisione */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Condividi"
                    )
                }
            }
        }
    }
}

// StatCard Componente
@Composable
fun StatCard(stat: StatItem, modifier: Modifier = Modifier, animDelay: Int = 0) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animDelay.toLong())
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Card(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = scale
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(stat.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when(stat.label) {
                        "Post" -> "📸"
                        "Luoghi" -> "🗺️"
                        "Gruppi" -> "👥"
                        else -> "📊"
                    },
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stat.value.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = stat.color
            )

            Text(
                text = stat.label,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

// PlaceCard Composable
@Composable
fun PlaceCard(place: PlaceItem) {
    Card(
        modifier = Modifier
            .width(180.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Immagine del luogo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(place.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Immagine di ${place.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradiente scuro in basso
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 50f
                            )
                        )
                )

                // Informazioni sul luogo
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = place.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = place.distance,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = place.rating.toString(),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Pulsante per visitare
            Button(
                onClick = { /* Implementazione navigazione */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5AC8FA)
                )
            ) {
                Text("Visita", fontSize = 14.sp)
            }
        }
    }
}

// Funzioni di utilità
fun formatPostDate(dateString: String): String {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val now = Calendar.getInstance()
        val postTime = Calendar.getInstance()
        postTime.time = date ?: return "Data sconosciuta"

        // Calcola la differenza
        val diffInMillis = now.timeInMillis - postTime.timeInMillis
        val diffInMinutes = diffInMillis / (60 * 1000)
        val diffInHours = diffInMillis / (60 * 60 * 1000)
        val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

        return when {
            diffInMinutes < 60 -> {
                if (diffInMinutes < 1) "Adesso" else "$diffInMinutes min fa"
            }
            diffInHours < 24 -> {
                if (diffInHours == 1L) "1 ora fa" else "$diffInHours ore fa"
            }
            diffInDays < 7 -> {
                if (diffInDays == 1L) "Ieri" else "$diffInDays giorni fa"
            }
            else -> {
                SimpleDateFormat("dd MMM", Locale.ITALIAN).format(date)
            }
        }
    } catch (e: Exception) {
        return "Data sconosciuta"
    }
}

private fun getDefaultNearbyPlaces(): List<PlaceItem> {
    return listOf(
        PlaceItem(
            id = 1,
            name = "Caricamento...",
            imageUrl = "https://images.unsplash.com/photo-1516483638261-f4dbaf036963",
            distance = "...",
            rating = 4.5f
        ),
        PlaceItem(
            id = 2,
            name = "Caricamento...",
            imageUrl = "https://images.unsplash.com/photo-1523906834658-6e24ef2386f9",
            distance = "...",
            rating = 4.3f
        ),
        PlaceItem(
            id = 3,
            name = "Caricamento...",
            imageUrl = "https://images.unsplash.com/photo-1519502358834-4cf4bb3740e1",
            distance = "...",
            rating = 4.0f
        )
    )
}

private fun getNearbyPlaces(lat: Double, lon: Double, city: String, callback: (List<PlaceItem>) -> Unit) {
    // Definisci luoghi per diverse città italiane
    val placesByCity = mapOf(
        "Roma" to listOf(
            PlaceItem(1, "Colosseo", "https://images.unsplash.com/photo-1552832230-c0197dd311b5", "1.2 km", 4.9f),
            PlaceItem(2, "Fontana di Trevi", "https://images.unsplash.com/photo-1525874684015-58379d421a52", "0.8 km", 4.8f),
            PlaceItem(3, "Pantheon", "https://images.unsplash.com/photo-1552484604-541f2d423d46", "1.5 km", 4.7f)
        ),
        "Milano" to listOf(
            PlaceItem(1, "Duomo di Milano", "https://images.unsplash.com/photo-1603788397410-5e108c93be53", "0.5 km", 4.9f),
            PlaceItem(2, "Galleria Vittorio Emanuele", "https://images.unsplash.com/photo-1595870811635-1b043d4cf5ee", "0.7 km", 4.7f),
            PlaceItem(3, "Castello Sforzesco", "https://images.unsplash.com/photo-1574411863833-5e85a998c55c", "1.8 km", 4.6f)
        ),
        "Venezia" to listOf(
            PlaceItem(1, "Piazza San Marco", "https://images.unsplash.com/photo-1566019422381-1f89201e845a", "0.3 km", 4.9f),
            PlaceItem(2, "Ponte di Rialto", "https://images.unsplash.com/photo-1580413787283-3a4bef61919d", "0.9 km", 4.8f),
            PlaceItem(3, "Canal Grande", "https://images.unsplash.com/photo-1560426774-5cf70690a1e8", "0.5 km", 4.7f)
        )
    )

    // Trova la città più vicina
    val matchedCity = placesByCity.keys.find {
        city.contains(it, ignoreCase = true) || it.contains(city, ignoreCase = true)
    }

    if (matchedCity != null && placesByCity.containsKey(matchedCity)) {
        callback(placesByCity[matchedCity]!!)
        return
    }

    // Genera luoghi dinamici se non trova corrispondenze
    val placeNames = listOf("Piazza $city", "Parco Comunale", "Museo Civico", "Ponte $city")
    val distances = listOf("0.3 km", "0.7 km", "1.2 km", "0.8 km")
    val placeImages = listOf(
        "https://images.unsplash.com/photo-1519502358834-4cf4bb3740e1",
        "https://images.unsplash.com/photo-1577334928618-652def2c98ad",
        "https://images.unsplash.com/photo-1580414057403-c5f451f30e1c"
    )

    val dynamicPlaces = List(3) { idx ->
        PlaceItem(
            id = idx + 1,
            name = placeNames[idx % placeNames.size].replace("$city", city),
            imageUrl = placeImages[idx % placeImages.size],
            distance = distances[idx % distances.size],
            rating = 4.0f + (idx * 0.3f).coerceAtMost(1.0f)
        )
    }

    callback(dynamicPlaces)
}

// HomeScreen principale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onProfileClick: () -> Unit = {},
    onAIAssistantClick: () -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
    // Stati dal ViewModel
    val weatherData by viewModel.weatherData.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Stati per i contenuti dell'utente
    var userPosts by remember { mutableStateOf<List<PostItem>>(emptyList()) }
    var isPostsLoading by remember { mutableStateOf(true) }
    var postsError by remember { mutableStateOf<String?>(null) }

    // Stati per le animazioni
    var isWeatherLoaded by remember { mutableStateOf(false) }
    var isPostsLoaded by remember { mutableStateOf(false) }
    var isSuggestionsLoaded by remember { mutableStateOf(false) }

    // Stati per luoghi vicini
    var nearbyPlaces by remember { mutableStateOf<List<PlaceItem>>(emptyList()) }

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scrollState = rememberLazyListState()
    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val coroutineScope = rememberCoroutineScope()

    val firstName = remember {
        val name = sessionManager.getFirstName()
        if (name.isNotBlank()) name else sessionManager.getUsername() ?: "Utente"
    }

    // Carica i dati dell'utente all'avvio
    LaunchedEffect(Unit) {
        // Carica meteo e posizione
        viewModel.loadLocationAndWeather()

        // Carica i post recenti nei gruppi a cui l'utente partecipa
        coroutineScope.launch {
            try {
                isPostsLoading = true
                postsError = null

                // Utilizza il client API autenticato
                val api = ServizioApi.getAuthenticatedClient(context)

                // Ottieni i gruppi dell'utente
                val groupsResponse = api.getMyGroups()
                if (!groupsResponse.isSuccessful) {
                    postsError = "Errore nel caricamento dei gruppi: ${groupsResponse.code()}"
                    return@launch
                }

                val groups = groupsResponse.body() ?: emptyList()
                val allPosts = mutableListOf<PostItem>()

                // Per ogni gruppo, ottieni i post più recenti (inclusi quelli di altri utenti)
                for (group in groups) {
                    try {
                        // Utilizza il metodo posts disponibile dall'API
                        val postsResponse = api.getGroupPosts(group.id.toString())

                        if (postsResponse.isSuccessful && postsResponse.body() != null) {
                            // Converti i post dal formato API al formato UI
                            postsResponse.body()?.forEach { post ->
                                // Filtra solo i post normali, non i messaggi chat
                                if (!post.is_chat_message) {
                                    allPosts.add(
                                        PostItem(
                                            id = post.id.toString(),
                                            userName = post.author.username,
                                            userAvatar = post.author.profile_picture,
                                            groupName = group.name,
                                            content = post.content,
                                            location = "", // Lascia vuoto se non disponibile
                                            timestamp = formatPostDate(post.created_at),
                                            mediaUrl = post.media?.firstOrNull()?.media_url,
                                            likesCount = 0, // Valore di default se non disponibile
                                            isMyPost = post.author.id.toString() == sessionManager.getUserId()
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("HomeScreen", "Errore nel caricamento dei post per il gruppo ${group.id}: ${e.message}")
                    }
                }

                // Ordina i post per data (i più recenti prima)
                userPosts = allPosts.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Errore generale: ${e.message}")
                postsError = "Errore di connessione: ${e.message}"
            } finally {
                isPostsLoading = false
            }
        }

        // Attiva le animazioni
        launch {
            kotlinx.coroutines.delay(300)
            isWeatherLoaded = true
            kotlinx.coroutines.delay(200)
            isPostsLoaded = true
            kotlinx.coroutines.delay(200)
            isSuggestionsLoaded = true
        }
    }

    // Aggiorna i luoghi vicini quando otteniamo la posizione
    LaunchedEffect(locationData) {
        locationData?.let { location ->
            getNearbyPlaces(location.latitude, location.longitude, location.placeName) { places ->
                nearbyPlaces = places
            }
        }
    }

    // Statistiche dell'utente (da aggiornare con dati reali)
    val stats = remember(userPosts) {
        listOf(
            StatItem("Post", userPosts.size, Color(0xFF5AC8FA)),
            StatItem("Luoghi", userPosts.mapNotNull { it.location }.distinct().size, Color(0xFFFF9500)),
            StatItem("Gruppi", userPosts.mapNotNull { it.groupName }.distinct().size, Color(0xFF34C759))
        )
    }

    // Header height per l'effetto parallasse
    val headerHeight = 180.dp
    val headerScrollProgress = remember {
        derivedStateOf { (scrollOffset.value / 600f).coerceIn(0f, 1f) }
    }
    val headerAlpha = remember {
        derivedStateOf { 1f - (headerScrollProgress.value * 0.6f) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Header con effetto parallasse e gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .graphicsLayer {
                    alpha = headerAlpha.value
                    translationY = -scrollOffset.value * 0.5f
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF5AC8FA),
                            Color(0xFF4A66E0)
                        )
                    )
                )
        ) {
            // Effetto texture sovrapposto
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.15f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0f)
                            )
                        )
                    )
            )

            // Pulsante di profilo nell'header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profilo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Contenuto scrollabile
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Titolo e informazioni utente
            item {
                Box(
                    modifier = Modifier
                        .height(headerHeight)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Ciao, ${firstName} 👋",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Ecco le attività recenti dai tuoi gruppi",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Avatar utente cliccabile per navigare al profilo
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onProfileClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstName.first().toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Indicatore di caricamento per meteo
            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        color = Color(0xFF5AC8FA)
                    )
                }
            }

            // Scheda meteo
            weatherData?.let { weather ->
                item {
                    AnimatedVisibility(
                        visible = isWeatherLoaded,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                    ) {// Continuazione della scheda meteo
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .offset(y = (-20).dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = weather.cityName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "📍 ${locationData?.placeName ?: "Posizione attuale"}",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Icona meteo
                                    Icon(
                                        imageVector = weather.icon,
                                        contentDescription = weather.condition,
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF5AC8FA)
                                    )

                                    Column {
                                        Text(
                                            text = "${weather.temperature}°",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF5AC8FA)
                                        )
                                        Text(
                                            text = weather.condition,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Umidità: ${weather.humidity}%",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    // Pulsante refresh
                                    IconButton(
                                        onClick = { viewModel.refreshWeather() }
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Aggiorna meteo",
                                            tint = Color(0xFF5AC8FA)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Statistiche
            item {
                AnimatedVisibility(
                    visible = isWeatherLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stats.forEachIndexed { index, stat ->
                            StatCard(
                                stat = stat,
                                modifier = Modifier.weight(1f),
                                animDelay = index * 100
                            )
                        }
                    }
                }
            }

            // Pulsante AI Assistant
            item {
                AnimatedVisibility(
                    visible = isWeatherLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    AIAssistantButton(onClick = onAIAssistantClick)
                }
            }

            // Titolo dei messaggi recenti
            item {
                AnimatedVisibility(
                    visible = isPostsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Messaggi recenti",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Pulsante di filtro (opzionale)
                        IconButton(onClick = { /* Implementazione filtro */ }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filtra",
                                tint = Color(0xFF5AC8FA)
                            )
                        }
                    }
                }
            }

            // Indicatore di caricamento per post
            if (isPostsLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF5AC8FA))
                    }
                }
            }

            // Messaggio di errore per post
            if (!isPostsLoading && postsError != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = postsError ?: "Errore nel caricamento dei messaggi",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    // Riprova a caricare i post
                                    isPostsLoading = true
                                    postsError = null
                                    // Qui inserisci la logica per ricaricare i post
                                }
                            ) {
                                Text("Riprova")
                            }
                        }
                    }
                }
            }

            // Nessun post trovato
            if (!isPostsLoading && postsError == null && userPosts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFF5AC8FA),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nessun messaggio recente",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Non ci sono messaggi recenti nei gruppi a cui partecipi. Unisciti a più gruppi per iniziare a comunicare!",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { /* Navigare alla ricerca gruppi */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5AC8FA)
                                )
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trova gruppi")
                            }
                        }
                    }
                }
            }

            // Lista dei post
            if (!isPostsLoading && userPosts.isNotEmpty()) {
                items(userPosts) { post ->
                    AnimatedVisibility(
                        visible = isPostsLoaded,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                    ) {
                        PostCard(
                            post = post,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onPostClick(post.id) }
                        )
                    }
                }
            }

            // Intestazione luoghi vicini
            item {
                AnimatedVisibility(
                    visible = isSuggestionsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Text(
                        text = "Da non perdere vicino a te",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )
                }
            }

            // Luoghi vicini (orizzontale)
            item {
                AnimatedVisibility(
                    visible = isSuggestionsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (nearbyPlaces.isEmpty()) {
                            items(getDefaultNearbyPlaces()) { place ->
                                PlaceCard(place = place)
                            }
                        } else {
                            items(nearbyPlaces) { place ->
                                PlaceCard(place = place)
                            }
                        }
                    }
                }
            }

            // Bottone di creazione nuovo post
            item {
                AnimatedVisibility(
                    visible = isPostsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = { /* Implementazione creazione nuovo post */ },
                            modifier = Modifier.padding(vertical = 24.dp),
                            containerColor = Color(0xFF5AC8FA)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Crea nuovo post",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}