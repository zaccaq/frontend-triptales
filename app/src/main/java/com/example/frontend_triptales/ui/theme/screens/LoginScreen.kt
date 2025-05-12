package com.example.frontend_triptales.ui.theme.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend_triptales.api.RichiestaLogin
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.auth.SessionManager
import com.example.frontend_triptales.ui.theme.components.AnimatedAppTitle
import com.example.frontend_triptales.ui.theme.components.GradientBackground
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    GradientBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Icon(
                Icons.Default.Email,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedAppTitle(fullText = "TripTales", typingSpeedMillis = 80L)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Esplora, racconta, ricorda.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = usernameOrEmail,
                        onValueChange = { usernameOrEmail = it },
                        label = { Text("Username o Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Aggiornamento nel click del pulsante di login nella funzione LoginScreen
                    Button(
                        onClick = {
                            if (usernameOrEmail.isBlank() || password.isBlank()) {
                                errorMessage = "Username e password sono obbligatori"
                                return@Button
                            }

                            // Effettua il login tramite API
                            coroutineScope.launch {
                                // Sostituisci il blocco try nella funzione di login
                                try {
                                    isLoading = true
                                    errorMessage = null

                                    val richiesta = RichiestaLogin(
                                        username = usernameOrEmail,
                                        password = password
                                    )

                                    val risposta = ServizioApi.getApi(context).login(richiesta)
                                    Log.d("LoginDebug", "Risposta: ${risposta.body()}")

                                    if (risposta.isSuccessful) {
                                        val tokenResponse = risposta.body()
                                        if (tokenResponse?.access != null) {
                                            // Salva il token di accesso
                                            val sessionManager = SessionManager(context)
                                            sessionManager.salvaLoginUtente(usernameOrEmail, tokenResponse)

                                            // Otteniamo i dettagli dell'utente
                                            try {
                                                // Usando il client autenticato che includerà automaticamente il token
                                                val authenticatedClient = ServizioApi.getAuthenticatedClient(context)
                                                val userResponse = authenticatedClient.getUserDetails()

                                                if (userResponse.isSuccessful && userResponse.body() != null) {
                                                    val userDetails = userResponse.body()!!
                                                    // Aggiorniamo le informazioni utente con l'ID e il nome
                                                    sessionManager.salvaInfoUtente(
                                                        userId = userDetails.id.toString(),  // Convertiamo l'ID numerico in stringa
                                                        username = userDetails.username,
                                                        firstName = userDetails.first_name ?: "",
                                                        rispostaLogin = tokenResponse
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                Log.e("LoginScreen", "Errore nel recupero dei dettagli utente", e)
                                                // Continuiamo comunque, poiché il login è riuscito
                                            }

                                            Toast.makeText(context, "Login effettuato con successo!", Toast.LENGTH_LONG).show()
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = "Token non valido nella risposta"
                                            Toast.makeText(context, "Errore: Token non valido", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        val errore = risposta.errorBody()?.string() ?: "Credenziali non valide"
                                        errorMessage = "Errore: $errore"
                                        Toast.makeText(context, "Errore: $errore", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Errore di connessione: ${e.message}"
                                    Toast.makeText(context, "Errore di connessione: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Entra", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Non hai un account? Registrati",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}