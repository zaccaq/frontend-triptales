// File: app/src/main/java/com/example/frontend_triptales/ui/theme/screens/ProfileScreen.kt

package com.example.frontend_triptales.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.BadgeDTO
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.api.UserStatsDTO
import com.example.frontend_triptales.auth.SessionManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(sessionManager.getUsername() ?: "Utente") }
    var userBadges by remember { mutableStateOf<List<BadgeDTO>>(emptyList()) }
    var userStats by remember { mutableStateOf<UserStatsDTO?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Carica i dati dell'utente
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val api = ServizioApi.getAuthenticatedClient(context)

                // Ottieni i dati del profilo
                val userDetailsResponse = api.getUserDetails()
                if (userDetailsResponse.isSuccessful && userDetailsResponse.body() != null) {
                    val userDetails = userDetailsResponse.body()!!
                    userName = userDetails.username
                }

                // Ottieni i badge dell'utente
                val badgesResponse = api.getUserBadges()
                if (badgesResponse.isSuccessful && badgesResponse.body() != null) {
                    userBadges = badgesResponse.body()!!
                }

                // Ottieni le statistiche dell'utente
                val statsResponse = api.getUserStats()
                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                    userStats = statsResponse.body()
                }

            } catch (e: Exception) {
                errorMessage = "Errore nel caricamento dei dati: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Il mio profilo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Errore sconosciuto",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            // Riprova a caricare i dati
                            try {
                                val api = ServizioApi.getAuthenticatedClient(context)

                                // Reload data
                                val userDetailsResponse = api.getUserDetails()
                                if (userDetailsResponse.isSuccessful && userDetailsResponse.body() != null) {
                                    val userDetails = userDetailsResponse.body()!!
                                    userName = userDetails.username
                                }

                                val badgesResponse = api.getUserBadges()
                                if (badgesResponse.isSuccessful && badgesResponse.body() != null) {
                                    userBadges = badgesResponse.body()!!
                                }

                                val statsResponse = api.getUserStats()
                                if (statsResponse.isSuccessful && statsResponse.body() != null) {
                                    userStats = statsResponse.body()
                                }
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "Errore nel caricamento dei dati: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Text("Riprova")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header del profilo
                item {
                    ProfileHeader(
                        userName = userName,
                        postCount = userStats?.postCount ?: 0,
                        likesCount = userStats?.likesCount ?: 0,
                        commentsCount = userStats?.commentsCount ?: 0
                    )
                }

                // Sezione badge
                item {
                    Text(
                        "I miei badge",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    if (userBadges.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE0E0E0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Non hai ancora ottenuto badge",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Text(
                                    "Continua ad esplorare e condividere per guadagnare badge!",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(userBadges) { badge ->
                                BadgeCard(badge = badge)
                            }
                        }
                    }
                }

                // Come ottenere badge
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                "Come ottenere i badge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BadgeHintItem(
                                name = "Esploratore",
                                description = "Visita 5 o più luoghi diversi",
                                icon = Icons.Default.Explore
                            )
                            BadgeHintItem(
                                name = "Fotografo",
                                description = "Carica 20 o più foto",
                                icon = Icons.Default.PhotoCamera
                            )
                            BadgeHintItem(
                                name = "Traduttore",
                                description = "Usa l'OCR per tradurre in 3 o più post",
                                icon = Icons.Default.Translate
                            )
                            BadgeHintItem(
                                name = "Osservatore",
                                description = "Riconosci oggetti in 10 o più post",
                                icon = Icons.Default.Visibility
                            )
                            BadgeHintItem(
                                name = "Social",
                                description = "Ottieni 15 o più like sui tuoi post",
                                icon = Icons.Default.Favorite
                            )
                        }
                    }
                }

                // Visualizza classifica
                item {
                    Button(
                        onClick = onLeaderboardClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5AC8FA)
                        )
                    ) {
                        Icon(Icons.Default.Leaderboard, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Visualizza classifica")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(
    userName: String,
    postCount: Int,
    likesCount: Int,
    commentsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5AC8FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.first().uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nome utente
            Text(
                text = userName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistiche
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(count = postCount, label = "Post", icon = Icons.Default.PhotoCamera)
                ProfileStat(count = likesCount, label = "Like", icon = Icons.Default.Favorite)
                ProfileStat(count = commentsCount, label = "Commenti", icon = Icons.Default.Comment)
            }
        }
    }
}

@Composable
fun ProfileStat(
    count: Int,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF5AC8FA),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun BadgeCard(badge: BadgeDTO) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icona del badge
            if (badge.icon_url != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(badge.icon_url)
                        .crossfade(true)
                        .build(),
                    contentDescription = badge.name,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Icona predefinita se non disponibile
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD700)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = badge.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = badge.description,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BadgeHintItem(
    name: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF5AC8FA),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = name,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}