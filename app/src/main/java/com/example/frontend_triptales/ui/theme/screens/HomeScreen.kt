package com.example.frontend_triptales.ui.theme.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.frontend_triptales.ui.theme.screens.WeatherRepository
import com.example.frontend_triptales.ui.theme.screens.WeatherResponse



@Composable
fun HomeScreen(userName: String = "Marco") {
    val location = rememberUserLocation()
    val coroutineScope = rememberCoroutineScope()
    var weather by remember { mutableStateOf<WeatherResponse?>(null) }

    val apiKey = "b052bace2ea6693b223b12ed2afea7c7" // 🔑 Sostituisci con la tua vera API Key

    // Quando otteniamo la posizione, recuperiamo il meteo
    LaunchedEffect(location) {
        if (location != null) {
            coroutineScope.launch {
                weather = WeatherRepository.fetchWeather(
                    lat = location.latitude,
                    lon = location.longitude,
                    apiKey = apiKey
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // HEADER
        Text(
            text = "Ciao $userName 👋",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Pronto a esplorare il mondo?",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // POSIZIONE + METEO REALE
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (location != null) {
                    Text("📍 Posizione attuale:", fontWeight = FontWeight.Bold)
                    Text("Lat: ${location.latitude}, Lng: ${location.longitude}")
                    Spacer(modifier = Modifier.height(8.dp))

                    weather?.let {
                        Text("🌤️ Meteo: ${it.weather.firstOrNull()?.description?.replaceFirstChar { c -> c.uppercase() }}")
                        Text("🌡️ Temperatura: ${it.main.temp}°C, Umidità: ${it.main.humidity}%")
                        Text("📍 Città: ${it.name}")
                    } ?: Text("🔄 Recupero dati meteo...")
                } else {
                    Text("🔍 Recupero posizione in corso...")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // RESTO DELLA UI (come prima)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            DashboardCard("📸", "12 contenuti", Color(0xFFFFF9C4))
            DashboardCard("🗺️", "3 luoghi visitati", Color(0xFFC8E6C9))
            DashboardCard("👥", "5 partecipanti", Color(0xFFBBDEFB))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Ultime attività", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        val activities = listOf(
            "Anna ha caricato una foto alla Fontana di Trevi",
            "Luca ha lasciato un commento al Colosseo",
            "Sara ha aggiunto un video del Museo"
        )
        activities.forEach {
            Text("• $it", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // WIDGET SUGGERITI
        DailySuggestionCard()
        Spacer(modifier = Modifier.height(16.dp))
        DailyGoalCard()
        Spacer(modifier = Modifier.height(16.dp))
        FunFactCard()
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// MINI DASHBOARD CARD
@Composable
fun DashboardCard(icon: String, label: String, backgroundColor: Color) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

// WIDGET: SUGGERIMENTO DEL GIORNO
@Composable
fun DailySuggestionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1C4E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📌 Suggerimento del giorno", fontWeight = FontWeight.Bold)
            Text("Visita il Parco Archeologico vicino a te!")
            Text("Distanza: 850 m", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

// WIDGET: OBIETTIVO DELLA GIORNATA
@Composable
fun DailyGoalCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECB3))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🎯 Obiettivo di oggi", fontWeight = FontWeight.Bold)
            Text("Scatta almeno 3 foto e aggiungi un commento ad un luogo.")
        }
    }
}

// WIDGET: CURIOSITÀ STORICA
@Composable
fun FunFactCard() {
    val funFacts = listOf(
        "Il Colosseo poteva contenere fino a 50.000 spettatori e veniva riempito in meno di 15 minuti.",
        "Il Pantheon ha la cupola in cemento non armato più grande del mondo.",
        "L’Acquedotto di Segovia è ancora in piedi dopo quasi 2.000 anni.",
        "Il Vesuvio ha seppellito Pompei nel 79 d.C. in poche ore.",
        "Le strade romane erano così ben fatte che alcune sono ancora in uso oggi.",
        "Il Foro Romano era il cuore pulsante della vita politica dell'antica Roma.",
        "Il primo centro commerciale della storia è considerato il Mercato di Traiano, costruito nel 100 d.C."
    )

    val randomFact = remember { funFacts.random() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🧐 Lo sapevi che...", fontWeight = FontWeight.Bold)
            Text(randomFact)
        }
    }
}

