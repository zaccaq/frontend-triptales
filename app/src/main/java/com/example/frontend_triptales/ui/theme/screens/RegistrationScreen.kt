package com.example.frontend_triptales.ui.theme.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import com.example.frontend_triptales.api.RichiestaRegistrazione
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.auth.SessionManager
import com.example.frontend_triptales.ui.theme.components.AnimatedAppTitle
import com.example.frontend_triptales.ui.theme.components.GradientBackground
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
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
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedAppTitle(fullText = "Crea Account", typingSpeedMillis = 70L)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Unisciti a TripTales!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 24.dp)
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Nome") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Cognome") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Conferma Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (password != confirmPassword) {
                                errorMessage = "Le password non coincidono"
                                return@Button
                            }

                            if (username.isBlank() || firstName.isBlank() || lastName.isBlank() ||
                                email.isBlank() || password.isBlank()) {
                                errorMessage = "Tutti i campi sono obbligatori"
                                return@Button
                            }

                            // Controlla la lunghezza minima della password
                            if (password.length < 8) {
                                errorMessage = "La password deve contenere almeno 8 caratteri"
                                return@Button
                            }

                            // Verifica formato email
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Formato email non valido"
                                return@Button
                            }

                            // Effettua la registrazione tramite API
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null

                                    val richiesta = RichiestaRegistrazione(
                                        username = username,
                                        email = email,
                                        password = password,
                                        first_name = firstName,
                                        last_name = lastName
                                    )

                                    // Log per debug
                                    Log.d("Registrazione", "Inizio registrazione per utente: $username")

                                    // Usa il client non autenticato per la registrazione
                                    val api = ServizioApi.getApi(context)
                                    val risposta = api.registrazione(richiesta)

                                    // Log della risposta per debug
                                    Log.d("Registrazione", "Codice risposta: ${risposta.code()}")

                                    if (risposta.isSuccessful) {
                                        // Registrazione completata con successo, ora effettua il login
                                        try {
                                            val loginRichiesta = RichiestaLogin(
                                                username = username,
                                                password = password
                                            )

                                            val loginRisposta = api.login(loginRichiesta)

                                            if (loginRisposta.isSuccessful && loginRisposta.body()?.access != null) {
                                                // Salva il token
                                                val sessionManager = SessionManager(context)
                                                sessionManager.salvaLoginUtente(username, loginRisposta.body()!!)

                                                // Recupera i dati dell'utente
                                                val authenticatedClient = ServizioApi.getAuthenticatedClient(context)
                                                val userResponse = authenticatedClient.getUserDetails()

                                                if (userResponse.isSuccessful && userResponse.body() != null) {
                                                    // Salva i dati dell'utente
                                                    sessionManager.salvaInfoUtente(
                                                        userId = userResponse.body()!!.id.toString(),
                                                        username = username,
                                                        firstName = firstName,
                                                        rispostaLogin = loginRisposta.body()!!
                                                    )
                                                }

                                                Toast.makeText(context, "Registrazione e login completati!", Toast.LENGTH_LONG).show()
                                                onRegistrationSuccess()
                                            } else {
                                                // Login fallito dopo registrazione
                                                Toast.makeText(context, "Registrazione completata, ma il login automatico è fallito. Prova ad accedere manualmente.", Toast.LENGTH_LONG).show()
                                                onNavigateToLogin()
                                            }
                                        } catch (e: Exception) {
                                            // Errore nel login automatico
                                            Toast.makeText(context, "Registrazione completata, ma il login automatico è fallito: ${e.message}", Toast.LENGTH_LONG).show()
                                            onNavigateToLogin()
                                        }
                                    } else {
                                        val errorBody = risposta.errorBody()?.string()
                                        Log.e("Registrazione", "Errore: $errorBody, Status: ${risposta.code()}")

                                        // Prova a parsare la risposta di errore come JSON
                                        try {
                                            errorBody?.let { body ->
                                                if (body.isNotEmpty()) {
                                                    val jsonObject = JSONObject(body)
                                                    errorMessage = when {
                                                        jsonObject.has("error") -> jsonObject.getString("error")
                                                        jsonObject.has("detail") -> jsonObject.getString("detail")
                                                        jsonObject.has("username") -> "Username: " + jsonObject.getJSONArray("username").getString(0)
                                                        jsonObject.has("email") -> "Email: " + jsonObject.getJSONArray("email").getString(0)
                                                        jsonObject.has("password") -> "Password: " + jsonObject.getJSONArray("password").getString(0)
                                                        else -> "Errore durante la registrazione: ${risposta.code()}"
                                                    }
                                                } else {
                                                    errorMessage = "Errore durante la registrazione: ${risposta.code()}"
                                                }
                                            } ?: run {
                                                errorMessage = "Errore durante la registrazione: ${risposta.code()}"
                                            }
                                        } catch (e: Exception) {
                                            Log.e("Registrazione", "Errore nel parsing JSON: ${e.message}")
                                            errorMessage = "Errore durante la registrazione. Riprova più tardi."
                                        }

                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("Registrazione", "Eccezione: ${e.message}", e)
                                    errorMessage = "Errore di connessione: ${e.message}"
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text("Registrati", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    "Hai già un account? Accedi",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Funzione di utilità per il debug
private fun debugResponse(response: retrofit2.Response<*>) {
    Log.d("API_DEBUG", "Status Code: ${response.code()}")
    Log.d("API_DEBUG", "Headers: ${response.headers()}")
    Log.d("API_DEBUG", "Is Successful: ${response.isSuccessful}")
    Log.d("API_DEBUG", "Error Body: ${response.errorBody()?.string()}")
    Log.d("API_DEBUG", "Response Body: ${response.body()}")
}