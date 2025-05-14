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
import com.example.frontend_triptales.auth.SessionManager
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
    var retryCount by remember { mutableStateOf(0) }

    // Funzione per caricare gli inviti
    fun loadInvites() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val api = ServizioApi.getAuthenticatedClient(context)

                // Log detailed diagnostic info
                val baseUrl = ServizioApi.getBaseUrl(context)
                Log.d("InvitesScreen", "Base URL: $baseUrl")
                Log.d("InvitesScreen", "Token available: ${context.let { SessionManager(it).getToken() != null }}")
                Log.d("InvitesScreen", "Making request to fetch invites - attempt: ${retryCount + 1}")

                val response = api.getMyInvites()
                Log.d("InvitesScreen", "Response code: ${response.code()}")

                when (response.code()) {
                    200 -> {
                        // Success case
                        val responseBody = response.body()
                        if (responseBody != null) {
                            Log.d("InvitesScreen", "Found ${responseBody.size} invites")
                            invites = responseBody
                        } else {
                            // Empty but successful response (likely no invites)
                            Log.d("InvitesScreen", "No invites found (empty response)")
                            invites = emptyList()
                        }
                    }
                    401 -> {
                        // Authentication error (token expired or invalid)
                        Log.e("InvitesScreen", "Authentication error (401): ${response.errorBody()?.string()}")
                        errorMessage = "Session expired. Please log in again to continue."
                    }
                    403 -> {
                        // Permission error
                        Log.e("InvitesScreen", "Permission error (403): ${response.errorBody()?.string()}")
                        errorMessage = "You don't have permission to access this resource."
                    }
                    404 -> {
                        // Endpoint not found
                        Log.e("InvitesScreen", "Endpoint not found (404)")
                        errorMessage = "Resource not found. Please contact support."
                    }
                    else -> {
                        // Other errors
                        val errorBody = response.errorBody()?.string()
                        Log.e("InvitesScreen", "API Error (${response.code()}): $errorBody")
                        errorMessage = "Error loading invites (${response.code()}): ${errorBody?.take(100)}"
                    }
                }
            } catch (e: Exception) {
                // Network or unexpected errors
                Log.e("InvitesScreen", "Connection error: ${e.message}", e)
                errorMessage = "Connection error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Carica gli inviti
    LaunchedEffect(retryCount) {
        loadInvites()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inviti ai gruppi") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    // Aggiungi un pulsante di aggiornamento
                    IconButton(onClick = {
                        retryCount++  // Incrementa il contatore per forzare il ricaricamento
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { retryCount++ }
                        ) {
                            Text("Riprova")
                        }
                    }
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
                                            errorMessage = "Errore nell'accettare l'invito"
                                        }
                                    } catch (e: Exception) {
                                        Log.e("InvitesScreen", "Errore: ${e.message}")
                                        errorMessage = "Errore nell'accettare l'invito: ${e.message}"
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
                                            errorMessage = "Errore nel rifiutare l'invito"
                                        }
                                    } catch (e: Exception) {
                                        Log.e("InvitesScreen", "Errore: ${e.message}")
                                        errorMessage = "Errore nel rifiutare l'invito: ${e.message}"
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
                        text = formatDate(invite.created_at),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (invite.group.description.isNotBlank()) {
                Text(
                    text = invite.group.description,
                    fontSize = 14.sp,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

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

// Renamed function to avoid conflicting overloads
private fun formatDate(dateString: String): String {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
        return outputFormat.format(date)
    } catch (e: Exception) {
        // Prova un formato alternativo se il primo fallisce
        try {
            val alternativeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val date = alternativeFormat.parse(dateString)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
            return outputFormat.format(date)
        } catch (e2: Exception) {
            return "Data sconosciuta"
        }
    }
}