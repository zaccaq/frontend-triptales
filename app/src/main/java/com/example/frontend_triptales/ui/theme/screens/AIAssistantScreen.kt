package com.example.frontend_triptales.ui.theme.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AIMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // Stato per i messaggi
    var messages by remember { mutableStateOf(listOf<AIMessage>()) }
    var userInput by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    // Messaggio di benvenuto iniziale
    LaunchedEffect(Unit) {
        val welcomeMessage = AIMessage(
            "Ciao! Sono TripBuddy, l'assistente AI di TripTales. Posso aiutarti con suggerimenti per il tuo viaggio, indicarti attrazioni, consigliarti ristoranti o rispondere a qualsiasi altra domanda. Come posso aiutarti oggi?",
            isFromUser = false
        )
        messages = listOf(welcomeMessage)
    }

    // Funzione per simulare la risposta dell'AI
    fun generateAIResponse(query: String) {
        val lowercase = query.toLowerCase()

        // Aggiungi la domanda dell'utente ai messaggi
        val userMessage = AIMessage(query, isFromUser = true)
        messages = messages + userMessage

        // Indica che l'AI sta digitando
        isTyping = true

        // Scroll alla fine della lista
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }

        // Simula il ritardo di digitazione dell'AI
        coroutineScope.launch {
            delay(1000) // Simula il tempo di risposta

            // Ottieni dati in tempo reale quando necessario
            val response = when {
                // ORARIO E DATA
                lowercase.contains("che ora") || lowercase.contains("che ore") ||
                        lowercase.contains("orario") || lowercase.contains("adesso che ore") -> {
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    "Sono le $currentTime."
                }

                lowercase.contains("che giorno") || lowercase.contains("data oggi") ||
                        lowercase.contains("oggi che giorno") -> {
                    val currentDate = SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALIAN).format(Date())
                    "Oggi è $currentDate."
                }

                // METEO E TEMPERATURA
                lowercase.contains("temperatura") || lowercase.contains("che temp") ||
                        lowercase.contains("quanto fa freddo") || lowercase.contains("quanto fa caldo") -> {
                    val currentTemp = (15..30).random() // Simuliamo una temperatura casuale
                    "In questa posizione la temperatura attuale è di $currentTemp°C."
                }

                lowercase.contains("meteo") || lowercase.contains("tempo") ||
                        lowercase.contains("previsioni") -> {
                    val conditions = listOf("soleggiato", "nuvoloso", "parzialmente nuvoloso", "piovoso", "ventoso")
                    val condition = conditions.random()
                    "Attualmente il tempo è $condition. Le previsioni indicano che rimarrà così per le prossime ore."
                }

                // INFORMAZIONI GENERALI
                lowercase.contains("chi sei") || lowercase.contains("come ti chiami") -> {
                    "Sono TripBuddy, l'assistente AI di TripTales. Sono qui per aiutarti con informazioni sui viaggi, suggerimenti, e qualsiasi altra domanda tu possa avere!"
                }

                lowercase.contains("cosa puoi fare") || lowercase.contains("come puoi aiutarmi") -> {
                    "Posso aiutarti in molti modi! Posso fornirti informazioni su destinazioni di viaggio, suggerirti attrazioni turistiche, ristoranti, e hotel. Posso anche dirti l'ora attuale, le condizioni meteo, rispondere a domande generali e molto altro ancora. Chiedimi pure ciò che ti serve!"
                }

                // DOMANDE PERSONALI
                lowercase.contains("come stai") -> {
                    "Sto bene, grazie! Sono sempre pronto ad aiutarti. E tu come stai oggi?"
                }

                lowercase.startsWith("grazie") || lowercase == "grazie" -> {
                    "Di nulla! Sono qui per aiutarti. C'è altro che posso fare per te?"
                }

                // VIAGGIO - CITTÀ
                lowercase.contains("roma") -> {
                    "Roma è una città straordinaria! Non puoi perderti il Colosseo, la Fontana di Trevi, i Musei Vaticani e Piazza Navona. Per il cibo, ti consiglio Trastevere per un'autentica esperienza culinaria romana."
                }

                lowercase.contains("firenze") -> {
                    "Firenze è la culla del Rinascimento! Da vedere: la Cattedrale di Santa Maria del Fiore, la Galleria degli Uffizi, il Ponte Vecchio e Piazzale Michelangelo per una vista panoramica. Assaggia la bistecca alla fiorentina!"
                }

                lowercase.contains("venezia") -> {
                    "Venezia è unica al mondo! Esplora Piazza San Marco, il Ponte di Rialto, fai un giro in gondola e perdirti tra i canali e le calli. Non dimenticare di provare i cicchetti, piccoli spuntini veneziani."
                }

                lowercase.contains("napoli") -> {
                    "Napoli è vibrante e autentica! Visita il centro storico, Spaccanapoli, il Museo Archeologico e certo, la pizza - Napoli è la patria della pizza più buona del mondo. Da non perdere anche gli scavi di Pompei nelle vicinanze."
                }

                lowercase.contains("milano") -> {
                    "Milano è il centro della moda e del design! Visita il Duomo, il Castello Sforzesco, ammira L'Ultima Cena di Leonardo da Vinci e fai shopping nella Galleria Vittorio Emanuele II. Milano è anche famosa per l'aperitivo!"
                }

                // CIBO E RISTORANTI
                lowercase.contains("mangiare") || lowercase.contains("cibo") ||
                        lowercase.contains("ristorante") || lowercase.contains("ristoranti") -> {
                    "L'Italia è famosa per la sua cucina! Ogni regione ha specialità uniche: pasta e pizza a Napoli, risotto a Milano, bistecca a Firenze, cicchetti a Venezia. Ti consiglio di provare i ristoranti locali piuttosto che quelli turistici."
                }

                // ALLOGGIO
                lowercase.contains("hotel") || lowercase.contains("albergo") ||
                        lowercase.contains("dormire") || lowercase.contains("ostello") -> {
                    "Per l'alloggio, dipende dal tuo budget e preferenze. Gli hotel nel centro città sono comodi ma costosi. B&B e agriturismi offrono un'esperienza più autentica. Considera anche Airbnb per soggiorni più lunghi. Prenota con anticipo, soprattutto in alta stagione!"
                }

                // TRASPORTI
                lowercase.contains("treno") || lowercase.contains("treni") -> {
                    "I treni in Italia sono un ottimo modo per spostarsi tra le città. Trenitalia e Italo offrono collegamenti frequenti tra i principali centri urbani. Puoi acquistare i biglietti online o in stazione, ma è consigliabile prenotare in anticipo per i treni ad alta velocità."
                }

                lowercase.contains("noleggiare") || lowercase.contains("auto") || lowercase.contains("macchina") -> {
                    "Noleggiare un'auto è una buona opzione per esplorare le zone rurali e i piccoli borghi. Tieni presente che nelle grandi città il traffico può essere caotico e i parcheggi costosi. Inoltre, in molti centri storici esistono zone a traffico limitato (ZTL) dove non puoi entrare senza permesso."
                }

                // LINGUA
                lowercase.contains("italiano") || lowercase.contains("lingua") || lowercase.contains("parlare") -> {
                    "Anche se nelle località turistiche molte persone parlano inglese, imparare qualche frase in italiano può aiutarti a connetterti con i locali. Frasi utili: 'Buongiorno' (buon giorno), 'Grazie' (grazie), 'Per favore' (per favore), 'Quanto costa?' (quanto costa?)."
                }

                // FUSO ORARIO
                lowercase.contains("fuso orario") || lowercase.contains("ora locale") -> {
                    "L'Italia segue il fuso orario dell'Europa Centrale (CET), che è UTC+1. Durante l'ora legale (da fine marzo a fine ottobre), l'orario diventa UTC+2 (CEST)."
                }

                // MONETA
                lowercase.contains("euro") || lowercase.contains("moneta") || lowercase.contains("valuta") ||
                        lowercase.contains("soldi") || lowercase.contains("cambio") -> {
                    "La valuta in Italia è l'Euro (€). Puoi prelevare contanti dagli sportelli ATM (chiamati 'Bancomat'), che sono ampiamente disponibili. Le carte di credito sono accettate nei negozi e ristoranti più grandi, ma è sempre utile avere un po' di contanti per i piccoli acquisti e nelle aree rurali."
                }

                // SICUREZZA
                lowercase.contains("sicurezza") || lowercase.contains("sicuro") || lowercase.contains("pericoloso") -> {
                    "L'Italia è generalmente un paese sicuro per i turisti. Come in ogni destinazione turistica, è consigliabile fare attenzione ai borseggiatori nelle aree affollate. Tieni i tuoi oggetti di valore in luoghi sicuri e sii particolarmente vigile nelle stazioni ferroviarie e sui mezzi pubblici affollati."
                }

                // DOMANDE SULL'APP
                lowercase.contains("come funziona") && lowercase.contains("app") -> {
                    "TripTales è un'app per condividere le tue esperienze di viaggio. Puoi creare gruppi, pubblicare post con foto, commentare e mettere mi piace ai post degli altri utenti. Puoi anche guadagnare badge completando determinate attività. Se hai domande specifiche su una funzionalità, fammi sapere!"
                }

                lowercase.contains("badge") -> {
                    "In TripTales puoi guadagnare badge completando diverse attività. Alcuni badge includono: 'Esploratore' (visita 5+ luoghi diversi), 'Fotografo' (carica 20+ foto), 'Traduttore' (usa OCR in 3+ post), 'Osservatore' (riconosci oggetti in 10+ post) e 'Social' (ottieni 15+ like sui tuoi post)."
                }

                // Conversazioni generali
                lowercase.startsWith("ciao") || lowercase.startsWith("salve") ||
                        lowercase.startsWith("buongiorno") || lowercase.startsWith("buonasera") -> {
                    "Ciao! Come posso aiutarti oggi con i tuoi piani di viaggio o con qualsiasi altra informazione?"
                }

                // RICERCHE COMPLESSE - SIMULAZIONE
                lowercase.contains("migliore periodo") || lowercase.contains("quando andare") ||
                        lowercase.contains("stagione migliore") -> {
                    "Il periodo migliore per visitare l'Italia dipende dalla regione e dalle tue preferenze. In generale:\n" +
                            "- Primavera (aprile-giugno): clima mite e meno turisti\n" +
                            "- Estate (giugno-agosto): alta stagione, caldo e affollato\n" +
                            "- Autunno (settembre-ottobre): clima ancora piacevole e meno folla\n" +
                            "- Inverno (novembre-marzo): ideale per città d'arte e sci sulle Alpi"
                }

                // RISPOSTA DEFAULT PER QUALSIASI ALTRA DOMANDA
                else -> {
                    "Mi dispiace, non ho informazioni specifiche su questo argomento. Posso aiutarti con dettagli sulle destinazioni turistiche in Italia, consigli di viaggio, informazioni sul meteo, orari e altre questioni pratiche. Come posso esserti utile?"
                }
            }

            // Aggiungi la risposta dell'AI
            val aiMessage = AIMessage(response, isFromUser = false)
            messages = messages + aiMessage

            // L'AI ha finito di digitare
            isTyping = false

            // Scroll alla fine della lista
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TripBuddy AI Assistant") },
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
                                    .background(Color(0xFF5AC8FA))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF5AC8FA))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF5AC8FA))
                            )
                        }
                    }
                }
            }

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
                                generateAIResponse(userInput)
                                userInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5AC8FA))
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
        Color(0xFF5AC8FA)
    } else {
        Color(0xFFE8E8E8)
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

// Alcune caratteristiche suggerite di viaggio per una futura implementazione
data class TravelSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String
)

@Composable
fun SuggestionChips(suggestions: List<TravelSuggestion>, onSuggestionClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Domande suggerite:",
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionClick(suggestion.title) },
                    label = { Text(suggestion.title) }
                )
            }
        }
    }
}

// Lista di suggerimenti di viaggio predefiniti che potrebbero essere mostrati all'utente
val DEFAULT_SUGGESTIONS = listOf(
    TravelSuggestion(
        id = "1",
        title = "Cosa vedere a Roma?",
        description = "Attrazioni principali da visitare a Roma",
        imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5"
    ),
    TravelSuggestion(
        id = "2",
        title = "Migliori ristoranti a Firenze",
        description = "Consigli su dove mangiare a Firenze",
        imageUrl = "https://images.unsplash.com/photo-1564501049559-0b54b6f0dc1b"
    ),
    TravelSuggestion(
        id = "3",
        title = "Trasporti a Venezia",
        description = "Come muoversi a Venezia",
        imageUrl = "https://images.unsplash.com/photo-1514890547357-a9ee288728e0"
    ),
    TravelSuggestion(
        id = "4",
        title = "Hotel economici a Napoli",
        description = "Dove alloggiare a Napoli con un budget limitato",
        imageUrl = "https://images.unsplash.com/photo-1580655653885-65763b2597d0"
    )
)