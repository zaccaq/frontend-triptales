package com.example.frontend_triptales.ui.theme.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Modello di dati per i messaggi
data class AIMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// ViewModel per gestire l'interazione con Gemini
class GeminiChatViewModel : ViewModel() {
    private val apiKey = "AIzaSyDyuW48jdaRef8ZZW1YS_JmV-VOfHH357s"

    // Modello generativo Gemini
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash",
            apiKey = apiKey
        )
    }

    // Stato dei messaggi
    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages: StateFlow<List<AIMessage>> = _messages.asStateFlow()

    // Stato di digitazione dell'AI
    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    // Inizializza la chat con un messaggio di benvenuto
    init {
        val welcomeMessage = AIMessage(
            "Ciao! Sono TripBuddy, il tuo assistente di viaggio alimentato da Gemini AI. Posso aiutarti con suggerimenti per la tua prossima avventura, consigliarti luoghi da visitare, ristoranti locali, e molto altro. Dove vorresti andare oggi?",
            isFromUser = false
        )
        _messages.value = listOf(welcomeMessage)
    }

    // Invio un messaggio a Gemini e ricevere una risposta
    fun sendMessage(userMessage: String) {
        // Aggiungi il messaggio dell'utente alla lista
        _messages.value = _messages.value + AIMessage(userMessage, isFromUser = true)

        // Imposta lo stato di digitazione
        _isTyping.value = true

        viewModelScope.launch {
            try {
                // Prepara il contesto per Gemini con istruzioni specifiche sui viaggi
                val prompt = """
                    Sei TripBuddy, un assistente di viaggio esperto e amichevole in TripTales. Fornisci informazioni accurate e aggiornate su destinazioni di viaggio, consigli su attrazioni, ristoranti, hotel, trasporti, e tutto ciò che riguarda i viaggi. Rispondi in modo conciso, informativo e personalizzato.
                    
                    Quando ti vengono richieste informazioni specifiche su una destinazione, includi:
                    - Attrazioni principali da visitare
                    - Consigli su cibo e ristoranti locali
                    - Informazioni pratiche per i viaggiatori
                    
                    Se non conosci una risposta specifica, fornisci informazioni generali utili sull'argomento richiesto senza inventare dettagli falsi.
                    
                    La tua risposta deve essere in italiano e in tono conversazionale. Mantieni le risposte concise ma informative.
                    
                    La domanda dell'utente è: $userMessage
                """.trimIndent()

                Log.d("GeminiChat", "Inviando richiesta a Gemini: $userMessage")

                val response = try {
                    generativeModel.generateContent(prompt)
                } catch (e: Exception) {
                    Log.e("GeminiChat", "Errore nella generazione del contenuto", e)
                    null
                }

                if (response != null && response.text != null) {
                    val responseText = response.text!!.trim()
                        .ifEmpty { "Mi dispiace, ho ricevuto una risposta vuota. Posso aiutarti con qualcos'altro riguardo i tuoi viaggi?" }

                    // Aggiungi la risposta alla lista dei messaggi
                    _messages.value = _messages.value + AIMessage(responseText, isFromUser = false)
                    Log.d("GeminiChat", "Risposta ricevuta: $responseText")
                } else {
                    _messages.value = _messages.value + AIMessage(
                        "Mi dispiace, non sono riuscito a elaborare la tua richiesta. Puoi formularla in modo diverso?",
                        isFromUser = false
                    )
                    Log.e("GeminiChat", "Risposta nulla da Gemini")
                }
            } catch (e: Exception) {
                Log.e("GeminiChat", "Errore nell'elaborazione della richiesta", e)
                _messages.value = _messages.value + AIMessage(
                    "Mi dispiace, sto riscontrando dei problemi tecnici. Riprova tra poco o verifica la tua connessione internet.",
                    isFromUser = false
                )
            } finally {
                // Disattiva lo stato di digitazione
                _isTyping.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onBackClick: () -> Unit,
    viewModel: GeminiChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Recupera lo stato dal ViewModel
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()

    // Input utente
    var userInput by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TripBuddy Gemini AI") },
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
        ) {
            // Lista messaggi
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Indicatore "sta scrivendo..."
                item {
                    AnimatedVisibility(visible = isTyping) {
                        Row(
                            modifier = Modifier
                                .padding(8.dp)
                                .padding(start = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34A853)) // Verde Google per Gemini
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4285F4)) // Blu Google per Gemini
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEA4335)) // Rosso Google per Gemini
                            )
                        }
                    }
                }
            }

            // Suggerimenti di domande rapide
            TravelSuggestionChips(
                suggestions = travelSuggestions,
                onSuggestionClick = { suggestion ->
                    userInput = suggestion
                }
            )

            // Barra di input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Input campo di testo
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = { Text("Chiedi a TripBuddy...") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            containerColor = Color.Transparent
                        ),
                        maxLines = 3
                    )

                    // Pulsante invio
                    IconButton(
                        onClick = {
                            if (userInput.isNotBlank() && !isTyping) {
                                viewModel.sendMessage(userInput)
                                userInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4)) // Colore principale di Google
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Invia",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: AIMessage) {
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isFromUser) {
        Color(0xFF4285F4) // Blu Google per messaggi utente
    } else {
        Color(0xFFE8E8E8) // Grigio chiaro per messaggi AI
    }
    val textColor = if (message.isFromUser) Color.White else Color.Black

    // Formatta l'ora
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isFromUser) 0.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .padding(12.dp, 8.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = time,
                    color = if (message.isFromUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

// Classe per i suggerimenti di viaggio
data class TravelSuggestion(val id: String, val text: String)

// Lista di suggerimenti predefiniti relativi ai viaggi
val travelSuggestions = listOf(
    TravelSuggestion("1", "Cosa vedere a Roma?"),
    TravelSuggestion("2", "I migliori ristoranti di Firenze"),
    TravelSuggestion("3", "Spiagge più belle della Sardegna"),
    TravelSuggestion("4", "Come muoversi a Venezia"),
    TravelSuggestion("5", "Quando visitare la Sicilia"),
    TravelSuggestion("6", "Consigli per viaggiare in Europa")
)

@Composable
fun TravelSuggestionChips(
    suggestions: List<TravelSuggestion>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "Suggerimenti:",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.height(80.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            val rows = suggestions.chunked(2)
            items(rows) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onSuggestionClick(suggestion.text) },
                            label = { Text(suggestion.text) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Se c'è un numero dispari di elementi, aggiungi uno spazio vuoto
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}