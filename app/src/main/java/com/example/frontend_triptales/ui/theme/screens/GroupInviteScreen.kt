package com.example.frontend_triptales.ui.theme.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.frontend_triptales.api.GroupInviteRequest
import com.example.frontend_triptales.api.ServizioApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteScreen(
    groupId: String,
    onBackClick: () -> Unit,
    onInviteSent: () -> Unit
) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invita Utente") },
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
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Invita un amico al tuo gruppo",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = { usernameOrEmail = it },
                label = { Text("Username o Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = Color.Green,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (usernameOrEmail.isBlank()) {
                        errorMessage = "Inserisci un username o email"
                        return@Button
                    }

                    coroutineScope.launch {
                        try {
                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            val api = ServizioApi.getAuthenticatedClient(context)
                            val response = api.inviteUserToGroup(
                                groupId = groupId,
                                request = GroupInviteRequest(usernameOrEmail)
                            )

                            if (response.isSuccessful) {
                                successMessage = "Invito inviato con successo!"
                                usernameOrEmail = ""
                                onInviteSent()
                            } else {
                                val error = response.errorBody()?.string() ?: "Errore sconosciuto"
                                errorMessage = "Errore: $error"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Errore di connessione: ${e.message}"
                            Log.e("GroupInviteScreen", "Errore: ${e.message}")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Invia Invito")
                }
            }
        }
    }
}

// Funzioni di utilità

fun formatInviteDate(dateString: String): String {
    try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
        return outputFormat.format(date)
    } catch (e: Exception) {
        return "Data sconosciuta"
    }
}