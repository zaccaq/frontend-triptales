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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend_triptales.api.GroupInviteDTO
import com.example.frontend_triptales.api.ServizioApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitesListScreen(
    onBackClick: () -> Unit,
    onInviteAccepted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var invites by remember { mutableStateOf<List<GroupInviteDTO>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Carica gli inviti
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val api = ServizioApi.getAuthenticatedClient(context)
                Log.d("InvitesScreen", "Richiesta inviti in corso...")
                val response = api.getMyInvites()

                Log.d("InvitesScreen", "Response code: ${response.code()}")
                Log.d("InvitesScreen", "Response body: ${response.body()}")
                Log.d("InvitesScreen", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body() != null) {
                    invites = response.body()!!
                } else {
                    errorMessage = "Errore nel caricamento degli inviti"
                    Log.e("InvitesScreen", "Errore API: ${response.code()} ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("InvitesScreen", "Errore di connessione: ${e.message}", e)
                errorMessage = "Errore di connessione: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inviti ai gruppi") },
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
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF5AC8FA))
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (invites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Non hai inviti in attesa",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(invites) { invite ->
                        InviteCard(
                            invite = invite,
                            onAccept = {
                                coroutineScope.launch {
                                    try {
                                        val api = ServizioApi.getAuthenticatedClient(context)
                                        val response = api.acceptInvite(invite.id.toString())

                                        if (response.isSuccessful) {
                                            // Rimuovi l'invito dalla lista
                                            invites = invites.filter { it.id != invite.id }
                                            onInviteAccepted()
                                        } else {
                                            Log.e("InvitesScreen", "Errore nell'accettare l'invito: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("InvitesScreen", "Errore: ${e.message}")
                                    }
                                }
                            },
                            onDecline = {
                                coroutineScope.launch {
                                    try {
                                        val api = ServizioApi.getAuthenticatedClient(context)
                                        val response = api.declineInvite(invite.id.toString())

                                        if (response.isSuccessful) {
                                            // Rimuovi l'invito dalla lista
                                            invites = invites.filter { it.id != invite.id }
                                        } else {
                                            Log.e("InvitesScreen", "Errore nel rifiutare l'invito: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("InvitesScreen", "Errore: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InviteCard(
    invite: GroupInviteDTO,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
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
                        .background(Color(0xFF5AC8FA), CircleShape)
                ) {
                    Text(
                        text = invite.group.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invite.group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Invito da: ${invite.invited_by.username}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = formatInviteDate(invite.created_at),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = invite.group.description,
                fontSize = 14.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pulsante rifiuta
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rifiuta")
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Pulsante accetta
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5AC8FA)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accetta")
                }
            }
        }
    }
}