// File: app/src/main/java/com/example/frontend_triptales/ui/theme/screens/LeaderboardScreen.kt

package com.example.frontend_triptales.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.BadgeDTO
import com.example.frontend_triptales.api.LeaderboardEntryDTO
import com.example.frontend_triptales.api.ServizioApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    groupId: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var leaderboard by remember { mutableStateOf<List<LeaderboardEntryDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Carica la classifica
    LaunchedEffect(groupId) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.getLeaderboard(groupId)

                if (response.isSuccessful && response.body() != null) {
                    leaderboard = response.body()!!
                } else {
                    errorMessage = "Errore nel caricamento della classifica"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = if (groupId != null) "Classifica del gruppo" else "Classifica globale") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Errore sconosciuto",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else if (leaderboard.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nessun dato disponibile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Inizia a condividere per apparire in classifica!",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Top 3 podio
                    item {
                        if (leaderboard.size >= 3) {
                            LeaderboardPodium(
                                first = leaderboard[0],
                                second = leaderboard[1],
                                third = leaderboard[2]
                            )

                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                    }

                    // Lista completa
                    itemsIndexed(leaderboard) { index, entry ->
                        LeaderboardItem(
                            position = index + 1,
                            entry = entry
                        )

                        if (index < leaderboard.size - 1) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Spazio alla fine
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardPodium(
    first: LeaderboardEntryDTO,
    second: LeaderboardEntryDTO,
    third: LeaderboardEntryDTO
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "🏆 Top 3",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Secondo posto
            PodiumItem(
                entry = second,
                position = 2,
                height = 160.dp,
                badgeColor = Color(0xFFAEA9BC)
            )

            // Primo posto
            PodiumItem(
                entry = first,
                position = 1,
                height = 200.dp,
                badgeColor = Color(0xFFFFD700)
            )

            // Terzo posto
            PodiumItem(
                entry = third,
                position = 3,
                height = 130.dp,
                badgeColor = Color(0xFFCD7F32)
            )
        }
    }
}

@Composable
fun PodiumItem(
    entry: LeaderboardEntryDTO,
    position: Int,
    height: androidx.compose.ui.unit.Dp,
    badgeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar con badge di posizione
        Box(contentAlignment = Alignment.Center) {
            // Avatar utente
            if (entry.profile_picture != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.profile_picture)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar di ${entry.username}",
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5AC8FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.username.first().uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Badge posizione
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = position.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Username
        Text(
            text = entry.username,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )

        // Punteggio
        Text(
            text = "${entry.total_score} pt",
            fontSize = 12.sp,
            color = Color.Gray
        )

        // Mostra i badge in miniatura (max 3)
        if (entry.badges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                entry.badges.take(3).forEach { badge ->
                    MicroBadge(badge)
                }

                if (entry.badges.size > 3) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${entry.badges.size - 3}",
                            fontSize = 8.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MicroBadge(badge: BadgeDTO) {
    if (badge.icon_url != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(badge.icon_url)
                .crossfade(true)
                .build(),
            contentDescription = badge.name,
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD700)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun LeaderboardItem(
    position: Int,
    entry: LeaderboardEntryDTO
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Posizione
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when (position) {
                        1 -> Color(0xFFFFD700) // Oro
                        2 -> Color(0xFFAEA9BC) // Argento
                        3 -> Color(0xFFCD7F32) // Bronzo
                        else -> Color.LightGray
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = position.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Avatar utente
        if (entry.profile_picture != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(entry.profile_picture)
                    .crossfade(true)
                    .build(),
                contentDescription = "Avatar di ${entry.username}",
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5AC8FA)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.username.first().uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info utente
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.username,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = " ${entry.post_count}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Gray
                )
                Text(
                    text = " ${entry.like_count}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Punteggio
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${entry.total_score}",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5AC8FA),
                fontSize = 18.sp
            )
            Text(
                text = "punti",
                fontSize = 12.sp,
                color = Color.Gray
            )// Badges
            if (entry.badges.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    entry.badges.take(3).forEach { badge ->
                        MicroBadge(badge)
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    if (entry.badges.size > 3) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${entry.badges.size - 3}",
                                fontSize = 8.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}