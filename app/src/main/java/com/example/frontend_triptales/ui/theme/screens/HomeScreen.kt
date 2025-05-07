package com.example.frontend_triptales.ui.theme.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.auth.SessionManager
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(userName: String = "Marco") {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val location = rememberUserLocation()
    val scrollState = rememberLazyListState()
    val scrollOffset = remember { derivedStateOf { scrollState.firstVisibleItemScrollOffset } }
    val coroutineScope = rememberCoroutineScope()

    val firstName = remember {
        val name = sessionManager.getFirstName()
        if (name.isNotBlank()) name else sessionManager.getUsername() ?: "Utente"
    }

    // Stati per posizione e meteo
    var cityName by remember { mutableStateOf("Rilevamento...") }
    var weatherData by remember {
        mutableStateOf(
            WeatherData(
                temperature = 24,
                condition = "Soleggiato",
                humidity = 65,
                cityName = "Rilevamento...",
                icon = Icons.Default.WbSunny
            )
        )
    }

    // Stati per animazioni
    var isWeatherLoaded by remember { mutableStateOf(false) }
    var isActivitiesLoaded by remember { mutableStateOf(false) }
    var isSuggestionsLoaded by remember { mutableStateOf(false) }

    // Attivatori di animazione dopo un breve ritardo
    LaunchedEffect(Unit) {
        launch {
            kotlinx.coroutines.delay(300)
            isWeatherLoaded = true
            kotlinx.coroutines.delay(200)
            isActivitiesLoaded = true
            kotlinx.coroutines.delay(200)
            isSuggestionsLoaded = true
        }
    }

    // Aggiorna la città quando otteniamo la posizione
    LaunchedEffect(location) {
        if (location != null) {
            // Verifica se abbiamo i permessi per la localizzazione
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasLocationPermission) {
                try {
                    // Ottieni il nome della città usando il Geocoder
                    val geocoder = Geocoder(context, Locale.getDefault())

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        // Per API 33+
                        geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        ) { addresses ->
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val city = address.locality ?:
                                address.subAdminArea ?:
                                address.adminArea ?:
                                "Posizione rilevata"

                                cityName = city

                                // Aggiorna l'oggetto meteo con la nuova città
                                weatherData = weatherData.copy(cityName = city)
                            }
                        }
                    } else {
                        // Per API < 33
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?:
                            address.subAdminArea ?:
                            address.adminArea ?:
                            "Posizione rilevata"

                            cityName = city

                            // Aggiorna l'oggetto meteo con la nuova città
                            weatherData = weatherData.copy(cityName = city)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback in caso di errore
                    cityName = "Lat: ${location.latitude.toInt()}, Lng: ${location.longitude.toInt()}"
                    weatherData = weatherData.copy(cityName = cityName)
                }
            } else {
                // Mostra messaggio di permesso mancante
                cityName = "Permesso di localizzazione necessario"
                weatherData = weatherData.copy(cityName = cityName)
            }
        }
    }

    // Statistiche utente
    val stats = remember {
        listOf(
            StatItem("Contenuti", 12, Color(0xFF5AC8FA)),
            StatItem("Luoghi", 5, Color(0xFFFF9500)),
            StatItem("Amici", 8, Color(0xFF34C759))
        )
    }

    // Attività recenti
    val activities = remember {
        listOf(
            ActivityItem(
                id = 1,
                userName = "Anna",
                userAvatar = "https://randomuser.me/api/portraits/women/12.jpg",
                actionText = "ha scattato una foto alla",
                location = "Fontana di Trevi",
                timestamp = "1 ora fa",
                mediaUrl = "https://images.unsplash.com/photo-1525874684015-58379d421a52",
                likesCount = 24
            ),
            ActivityItem(
                id = 2,
                userName = "Luca",
                userAvatar = "https://randomuser.me/api/portraits/men/22.jpg",
                actionText = "ha aggiunto un commento al",
                location = "Colosseo",
                timestamp = "3 ore fa",
                mediaUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5",
                likesCount = 18
            ),
            ActivityItem(
                id = 3,
                userName = "Sara",
                userAvatar = "https://randomuser.me/api/portraits/women/33.jpg",
                actionText = "ha condiviso un video dei",
                location = "Musei Vaticani",
                timestamp = "Ieri",
                mediaUrl = "https://images.unsplash.com/photo-1555852095-64e7428df0fa",
                likesCount = 32
            )
        )
    }

    // Luoghi suggeriti
    val suggestedPlaces = remember {
        listOf(
            PlaceItem(
                id = 1,
                name = "Parco Archeologico",
                imageUrl = "https://images.unsplash.com/photo-1516483638261-f4dbaf036963",
                distance = "850 m",
                rating = 4.8f
            ),
            PlaceItem(
                id = 2,
                name = "Piazza di Spagna",
                imageUrl = "https://images.unsplash.com/photo-1523906834658-6e24ef2386f9",
                distance = "1.2 km",
                rating = 4.9f
            ),
            PlaceItem(
                id = 3,
                name = "Villa Borghese",
                imageUrl = "https://images.unsplash.com/photo-1519502358834-4cf4bb3740e1",
                distance = "2 km",
                rating = 4.7f
            )
        )
    }

    // Fatti interessanti
    val funFacts = remember {
        listOf(
            "Il Colosseo poteva contenere fino a 50.000 spettatori e veniva riempito in meno di 15 minuti.",
            "Il Pantheon ha la cupola in cemento non armato più grande del mondo.",
            "L'Acquedotto di Segovia è ancora in piedi dopo quasi 2.000 anni.",
            "Il Vesuvio ha seppellito Pompei nel 79 d.C. in poche ore.",
            "Le strade romane erano così ben fatte che alcune sono ancora in uso oggi."
        )
    }

    // Seleziona un fatto casuale
    val randomFact = remember { funFacts.random() }

    // Stato dell'obiettivo giornaliero
    val dailyGoalProgress = remember { 0.33f } // 33% completato

    // Calcola l'effetto di parallasse per l'header
    val headerHeight = 180.dp
    val headerScrollProgress = remember { derivedStateOf {
        (scrollOffset.value / 600f).coerceIn(0f, 1f)
    }}
    val headerAlpha = remember { derivedStateOf { 1f - (headerScrollProgress.value * 0.6f) } }

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
                                text = "Pronto a esplorare il mondo?",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Avatar utente
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.first().toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Scheda meteo
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
                                    text = weatherData.cityName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "📍 Posizione attuale",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${weatherData.temperature}°",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5AC8FA)
                                )
                                Column(
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = weatherData.condition,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Umidità: ${weatherData.humidity}%",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
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

            // Intestazione attività recenti
            item {
                AnimatedVisibility(
                    visible = isActivitiesLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Text(
                        text = "Attività recenti",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            }

            // Attività recenti
            items(activities) { activity ->
                AnimatedVisibility(
                    visible = isActivitiesLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    ActivityCard(
                        activity = activity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Intestazione luoghi suggeriti
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

            // Luoghi suggeriti (orizzontale)
            item {
                AnimatedVisibility(
                    visible = isSuggestionsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(suggestedPlaces) { place ->
                            PlaceCard(place = place)
                        }
                    }
                }
            }

            // Sezione "Lo sapevi che..."
            item {
                AnimatedVisibility(
                    visible = isSuggestionsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🧐 Lo sapevi che...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = randomFact,
                                fontSize = 14.sp,
                                color = Color(0xFF37474F)
                            )
                        }
                    }
                }
            }

            // Obiettivo giornaliero
            item {
                AnimatedVisibility(
                    visible = isSuggestionsLoaded,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessLow))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF8E1)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🎯 Obiettivo di oggi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scatta almeno 3 foto e aggiungi un commento ad un luogo.",
                                fontSize = 14.sp,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Barra di progresso
                            val animatedProgress by animateFloatAsState(
                                targetValue = dailyGoalProgress,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "progress"
                            )

                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFFFF9800),
                                trackColor = Color(0xFFFFE0B2)
                            )

                            Text(
                                text = "1/3 completato",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

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
                        "Contenuti" -> "📸"
                        "Luoghi" -> "🗺️"
                        else -> "👥"
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

@Composable
fun ActivityCard(activity: ActivityItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Intestazione dell'attività
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar utente
                AsyncImage(
                    model = activity.userAvatar,
                    contentDescription = "Avatar di ${activity.userName}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )

                // Informazioni attività
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Row {
                        Text(
                            text = activity.userName,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " ${activity.actionText}",
                        )
                    }

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
                            text = activity.location,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Text(
                            text = " • ${activity.timestamp}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Immagine dell'attività
            activity.mediaUrl?.let { imageUrl ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Immagine di ${activity.location}",
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
                    text = "${activity.likesCount}",
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

// Classi dati per la HomeScreen
data class WeatherData(
    val temperature: Int,
    val condition: String,
    val humidity: Int,
    val cityName: String,
    val icon: ImageVector
)

data class StatItem(
    val label: String,
    val value: Int,
    val color: Color
)

data class ActivityItem(
    val id: Int,
    val userName: String,
    val userAvatar: String,
    val actionText: String,
    val location: String,
    val timestamp: String,
    val mediaUrl: String? = null,
    val likesCount: Int = 0
)

data class PlaceItem(
    val id: Int,
    val name: String,
    val imageUrl: String,
    val distance: String,
    val rating: Float
)