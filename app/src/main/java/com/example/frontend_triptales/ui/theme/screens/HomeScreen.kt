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
import com.example.frontend_triptales.ui.theme.screens.FilterBottomSheet
import com.example.frontend_triptales.ui.theme.screens.FilterIndicator
import com.example.frontend_triptales.ui.theme.screens.filterPosts
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
    val isMyPost: Boolean = false,
    val userHasLiked: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class StatItem(
    val label: String,
    val value: Int,
    val color: Color
)

// Enum per i tipi di filtro
enum class FilterType {
    ALL, MY_POSTS, WITH_MEDIA, WITH_LOCATION, RECENT
}

enum class DateRange(val displayName: String) {
    ALL("Tutti"),
    TODAY("Oggi"),
    WEEK("Questa settimana"),
    MONTH("Questo mese")
}

// Data class per le opzioni di filtro
data class FilterOptions(
    val selectedGroups: Set<String> = emptySet(),
    val filterType: FilterType = FilterType.ALL,
    val dateRange: DateRange = DateRange.ALL,
    val searchQuery: String = ""
)

// Componente PostCard esterno
@Composable
fun PostCard(
    post: PostItem,
    modifier: Modifier = Modifier,
    onLocationClick: ((Double, Double) -> Unit)? = null,
    onLikeClick: ((String) -> Unit)? = null,
    onCommentClick: ((String) -> Unit)? = null
) {
    var likesCount by remember { mutableStateOf(post.likesCount) }
    var isLiked by remember { mutableStateOf(post.userHasLiked) }
    var isLiking by remember { mutableStateOf(false) }

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

                    // Mostra posizione se disponibile
                    if (post.location.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable {
                                    // Se abbiamo le coordinate, chiama il callback
                                    post.latitude?.let { lat ->
                                        post.longitude?.let { lng ->
                                            onLocationClick?.invoke(lat, lng)
                                        }
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF5AC8FA)
                            )
                            Text(
                                text = post.location,
                                fontSize = 12.sp,
                                color = Color(0xFF5AC8FA),
                                modifier = Modifier.padding(start = 2.dp),
                                fontWeight = FontWeight.Medium
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
                // Pulsante Like
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!isLiking) {
                                onLikeClick?.invoke(post.id)
                            }
                        },
                        enabled = !isLiking,
                        modifier = Modifier.size(36.dp)
                    ) {
                        if (isLiking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFFFF4081)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Mi piace",
                                tint = if (isLiked) Color(0xFFFF4081) else Color.Gray
                            )
                        }
                    }

                    Text(
                        text = "$likesCount",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp),
                        color = if (isLiked) Color(0xFFFF4081) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Pulsante Commenti
                IconButton(
                    onClick = { onCommentClick?.invoke(post.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Comment,
                        contentDescription = "Commenta",
                        tint = Color(0xFF5AC8FA)
                    )
                }

                // Pulsante mappa se c'è una posizione
                if (post.location.isNotBlank() && post.latitude != null && post.longitude != null) {
                    IconButton(
                        onClick = { onLocationClick?.invoke(post.latitude, post.longitude) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Mostra sulla mappa",
                            tint = Color(0xFF5AC8FA)
                        )
                    }
                }
            }
        }
    }

    // Aggiorna gli stati locali quando cambiano le props
    LaunchedEffect(post.likesCount, post.userHasLiked) {
        likesCount = post.likesCount
        isLiked = post.userHasLiked
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

// HomeScreen principale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onProfileClick: () -> Unit = {},
    onAIAssistantClick: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onShowLocationOnMap: (Double, Double) -> Unit = { _, _ -> },
    onNavigateToComments: (String, String) -> Unit = { _, _ -> }
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

    //Stati per il filtro
    var currentFilters by remember { mutableStateOf(FilterOptions()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var availableGroups by remember { mutableStateOf<List<String>>(emptyList()) }

    // Post filtrati
    val filteredPosts = remember(userPosts, currentFilters) {
        filterPosts(userPosts, currentFilters)
    }

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scrollState = rememberLazyListState()
    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val coroutineScope = rememberCoroutineScope()

    val firstName = remember {
        val name = sessionManager.getFirstName()
        if (name.isNotBlank()) name else sessionManager.getUsername() ?: "Utente"
    }

    fun handleLikePost(postId: String) {
        coroutineScope.launch {
            try {
                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.likePost(postId)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!

                    // Aggiorna la lista dei post
                    userPosts = userPosts.map { post ->
                        if (post.id == postId) {
                            post.copy(
                                likesCount = result.total_likes,
                                userHasLiked = result.liked
                            )
                        } else {
                            post
                        }
                    }
                } else {
                    Log.e("HomeScreen", "Errore like: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("HomeScreen", "Errore like: ${e.message}")
            }
        }
    }

    fun handleShowComments(postId: String) {
        // Trova il post per ottenere il titolo
        val post = userPosts.find { it.id == postId }
        val postTitle = post?.content?.take(50)?.let {
            if (it.length == 50) "$it..." else it
        } ?: "Post"

        // Naviga alla schermata commenti
        onNavigateToComments(postId, postTitle)
    }

    // Funzioni per il filtro
    fun clearFilter(filterKey: String) {
        currentFilters = when (filterKey) {
            "type" -> currentFilters.copy(filterType = FilterType.ALL)
            "date" -> currentFilters.copy(dateRange = DateRange.ALL)
            "groups" -> currentFilters.copy(selectedGroups = emptySet())
            "search" -> currentFilters.copy(searchQuery = "")
            else -> currentFilters
        }
    }

    fun resetAllFilters() {
        currentFilters = FilterOptions()
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

                val api = ServizioApi.getAuthenticatedClient(context)

                // Ottieni i gruppi dell'utente
                val groupsResponse = api.getMyGroups()
                if (!groupsResponse.isSuccessful) {
                    postsError = "Errore nel caricamento dei gruppi: ${groupsResponse.code()}"
                    return@launch
                }

                val groups = groupsResponse.body() ?: emptyList()
                availableGroups = groups.map { it.name }
                val allPosts = mutableListOf<PostItem>()

                // Per ogni gruppo, ottieni i post più recenti
                for (group in groups) {
                    try {
                        val postsResponse = api.getGroupPosts(group.id.toString())

                        if (postsResponse.isSuccessful && postsResponse.body() != null) {
                            postsResponse.body()?.forEach { post ->
                                if (!post.is_chat_message) {
                                    allPosts.add(
                                        PostItem(
                                            id = post.id.toString(),
                                            userName = post.author.username,
                                            userAvatar = post.author.profile_picture,
                                            groupName = group.name,
                                            content = post.content,
                                            location = post.location_name ?: "",
                                            timestamp = formatPostDate(post.created_at),
                                            mediaUrl = post.media?.firstOrNull()?.media_url,
                                            likesCount = post.likes_count,
                                            isMyPost = post.author.id.toString() == sessionManager.getUserId(),
                                            userHasLiked = post.user_has_liked,
                                            latitude = post.latitude,
                                            longitude = post.longitude
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
        }
    }

    // Statistiche dell'utente
    val stats = remember(filteredPosts) {
        listOf(
            StatItem("Post", filteredPosts.size, Color(0xFF5AC8FA)),
            StatItem("Luoghi", filteredPosts.mapNotNull { it.location }.distinct().size, Color(0xFFFF9500)),
            StatItem("Gruppi", filteredPosts.mapNotNull { it.groupName }.distinct().size, Color(0xFF34C759))
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
                    ) {
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
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        // Prima riga: Titolo e conteggio
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Messaggi recenti",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Mostra conteggio filtrato se diverso dal totale
                            if (filteredPosts.size != userPosts.size) {
                                Text(
                                    text = "${filteredPosts.size} di ${userPosts.size} messaggi",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seconda riga: Controlli filtro
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Lato sinistro: Badge indicatore filtri attivi
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (currentFilters != FilterOptions()) {
                                    Badge(
                                        modifier = Modifier.padding(end = 8.dp),
                                        containerColor = Color(0xFF5AC8FA)
                                    ) {
                                        val activeFiltersCount = listOf(
                                            currentFilters.filterType != FilterType.ALL,
                                            currentFilters.dateRange != DateRange.ALL,
                                            currentFilters.selectedGroups.isNotEmpty(),
                                            currentFilters.searchQuery.isNotEmpty()
                                        ).count { it }

                                        Text(
                                            text = activeFiltersCount.toString(),
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = "filtri attivi",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Lato destro: Pulsanti di controllo
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pulsante reset filtri (visibile solo se ci sono filtri attivi)
                                if (currentFilters != FilterOptions()) {
                                    TextButton(
                                        onClick = { resetAllFilters() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Reset filtri",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reset",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Pulsante filtro - sempre visibile e prominente
                                Button(
                                    onClick = { showFilterSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (currentFilters != FilterOptions())
                                            Color(0xFF5AC8FA) else Color(0xFFF0F0F0),
                                        contentColor = if (currentFilters != FilterOptions())
                                            Color.White else Color.Gray
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = "Filtra",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Filtri",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (currentFilters != FilterOptions()) {
                item {
                    FilterIndicator(
                        filters = currentFilters,
                        onClearFilter = { filterKey -> clearFilter(filterKey) }
                    )
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
                                }
                            ) {
                                Text("Riprova")
                            }
                        }
                    }
                }
            }

            // Nessun post trovato
            if (!isPostsLoading && filteredPosts.isNotEmpty()) {
                items(filteredPosts) { post ->
                    AnimatedVisibility(
                        visible = isPostsLoaded,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                    ) {
                        PostCard(
                            post = post,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { onPostClick(post.id) },
                            onLocationClick = { lat, lng ->
                                onShowLocationOnMap(lat, lng)
                            },
                            onLikeClick = { postId -> handleLikePost(postId) },
                            onCommentClick = { postId -> handleShowComments(postId) }
                        )
                    }
                }
            }

            if (!isPostsLoading && userPosts.isNotEmpty() && filteredPosts.isEmpty()) {
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
                                Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color(0xFF5AC8FA),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nessun risultato",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nessun messaggio corrisponde ai filtri selezionati. Prova a modificare i criteri di ricerca.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { resetAllFilters() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF5AC8FA)
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset filtri")
                            }
                        }
                    }
                }
            }
        }
    }

    FilterBottomSheet(
        isVisible = showFilterSheet,
        onDismiss = { showFilterSheet = false },
        currentFilters = currentFilters,
        availableGroups = availableGroups,
        onFiltersChanged = { newFilters ->
            currentFilters = newFilters
        }
    )
}