package com.example.frontend_triptales.ui.theme.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend_triptales.api.GruppoDTO
import com.example.frontend_triptales.api.ServizioApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onBackClick: () -> Unit,
    onGroupJoined: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Stati
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<GruppoDTO>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Stati per l'unione al gruppo
    var joiningGroupId by remember { mutableStateOf<String?>(null) }
    var joiningError by remember { mutableStateOf<String?>(null) }

    // Funzione per cercare gruppi
    fun searchGroups() {
        if (searchQuery.isBlank()) {
            searchError = "Inserisci un nome di gruppo per cercare"
            return
        }

        coroutineScope.launch {
            try {
                isSearching = true
                searchError = null
                searchResults = emptyList()

                val api = ServizioApi.getAuthenticatedClient(context)
                // Assumiamo che ci sia un endpoint di ricerca per i gruppi
                val response = api.searchGroups(searchQuery)

                if (response.isSuccessful && response.body() != null) {
                    // Filtra i gruppi privati, a meno che l'API non lo faccia già
                    searchResults = response.body()!!.filter { !it.is_private }

                    if (searchResults.isEmpty()) {
                        searchError = "Nessun gruppo pubblico trovato con questo nome"
                    }
                } else {
                    searchError = "Errore nella ricerca: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("JoinGroupScreen", "Errore nella ricerca: ${e.message}")
                searchError = "Errore nella ricerca: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    // Funzione per unirsi a un gruppo
    fun joinGroup(groupId: String) {
        coroutineScope.launch {
            try {
                joiningGroupId = groupId
                joiningError = null

                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.joinGroup(groupId)

                if (response.isSuccessful) {
                    // Unione al gruppo riuscita, naviga alla chat del gruppo
                    onGroupJoined(groupId)
                } else {
                    joiningError = "Errore nell'unirsi al gruppo: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("JoinGroupScreen", "Errore nell'unirsi al gruppo: ${e.message}")
                joiningError = "Errore nell'unirsi al gruppo: ${e.message}"
            } finally {
                joiningGroupId = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unisciti a un Gruppo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Barra di ricerca
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Nome del gruppo") },
                placeholder = { Text("Inserisci il nome del gruppo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancella")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsante di ricerca
            Button(
                onClick = { searchGroups() },
                enabled = !isSearching && searchQuery.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerca Gruppo")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Messaggio di errore di ricerca
            if (searchError != null) {
                Text(
                    text = searchError!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Risultati della ricerca
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isSearching) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (searchResults.isNotEmpty()) {
                    items(searchResults) { group ->
                        GroupSearchResultItem(
                            group = group,
                            isJoining = joiningGroupId == group.id.toString(),
                            onJoinClick = { joinGroup(group.id.toString()) }
                        )
                    }
                } else if (searchQuery.isNotBlank() && !isSearching && searchError == null) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            Text(
                                text = "Nessun gruppo trovato con questo nome.\nProva a cercare un altro gruppo.",
                                textAlign = TextAlign.Center,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Messaggio di errore per l'unione al gruppo
            if (joiningError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = joiningError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun GroupSearchResultItem(
    group: GruppoDTO,
    isJoining: Boolean,
    onJoinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar del gruppo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5AC8FA))
                ) {
                    Text(
                        text = group.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Creato da: ${group.created_by.username}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "Membri: ${group.member_count}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                // Pulsante per unirsi
                Button(
                    onClick = onJoinClick,
                    enabled = !isJoining,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5AC8FA)
                    )
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Unisciti")
                    }
                }
            }

            // Descrizione del gruppo (opzionale, mostrata solo se presente)
            if (group.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = group.description,
                    fontSize = 14.sp,
                    maxLines = 3,
                    color = Color.DarkGray
                )
            }

            // Informazioni aggiuntive
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = group.location,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${group.start_date} - ${group.end_date}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}