package com.example.frontend_triptales.ui.theme.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
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
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.auth.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class GroupItem(
    val id: String,
    val name: String,
    val lastActivity: String,
    val memberCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit,
    onInvitesClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Stato per i gruppi
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingInvites by remember { mutableStateOf(0) }

    // Carica i gruppi dell'utente e gli inviti in attesa
    LaunchedEffect(Unit) {
        val token = sessionManager.getToken()
        Log.d("GroupScreen", "Token: ${token?.take(20)}...")

        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Carica i gruppi
                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.getMyGroups()

                if (response.isSuccessful && response.body() != null) {
                    groups = response.body()!!.map { group ->
                        Log.d("GroupScreen", "Gruppo: ${group.name}, Membri: ${group.memberCount}")
                        GroupItem(
                            id = group.id.toString(),
                            name = group.name,
                            lastActivity = formatLastActivity(group.lastActivityDate),
                            memberCount = group.memberCount
                        )
                    }
                    Log.d("GroupScreen", "Caricati ${groups.size} gruppi")
                } else {
                    errorMessage = "Errore nel caricamento dei gruppi"
                    val errorBody = response.errorBody()?.string()
                    Log.e("GroupScreen", "Errore API: ${response.code()} $errorBody")
                }

                // Carica gli inviti in attesa
                try {
                    val invitesResponse = api.getMyInvites()
                    if (invitesResponse.isSuccessful && invitesResponse.body() != null) {
                        pendingInvites = invitesResponse.body()!!.size
                        Log.d("GroupScreen", "Inviti in attesa: $pendingInvites")
                    } else {
                        Log.e("GroupScreen", "Errore nel caricamento degli inviti: ${invitesResponse.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("GroupScreen", "Errore inviti: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e("GroupScreen", "Errore di connessione: ${e.message}", e)
                errorMessage = "Errore di connessione: ${e.message}"
                groups = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I tuoi gruppi") },
                actions = {
                    // Badge con icona delle notifiche
                    BadgedBox(
                        badge = {
                            if (pendingInvites > 0) {
                                Badge { Text(pendingInvites.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onInvitesClick) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Inviti",
                                tint = Color(0xFF5AC8FA)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CircularProgressIndicator(color = Color(0xFF5AC8FA))
                }
            } else if (errorMessage != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage!!,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Ricarica i dati
                                coroutineScope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = null
                                        val api = ServizioApi.getAuthenticatedClient(context)
                                        val response = api.getMyGroups()
                                        if (response.isSuccessful && response.body() != null) {
                                            groups = response.body()!!.map { group ->
                                                GroupItem(
                                                    id = group.id.toString(),
                                                    name = group.name,
                                                    lastActivity = formatLastActivity(group.lastActivityDate),
                                                    memberCount = group.memberCount
                                                )
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Errore di connessione: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Text("Riprova")
                        }
                    }
                }
            } else if (groups.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Text(
                        "Non sei ancora in nessun gruppo.\nCrea o unisciti a un gruppo per iniziare!",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(groups) { group ->
                        GroupCard(
                            group = group,
                            onClick = { onGroupClick(group.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateGroupClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crea nuovo gruppo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onJoinGroupClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Unisciti a un gruppo", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Formatta la data dell'ultima attività in un formato leggibile
private fun formatLastActivity(dateString: String?): String {
    if (dateString == null) return "Nessuna attività"

    return try {
        // Prova diversi formati di data
        val inputFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        )

        var date: Date? = null
        for (format in inputFormats) {
            try {
                date = format.parse(dateString)
                break
            } catch (e: Exception) {
                // Continua con il prossimo formato
                continue
            }
        }

        if (date == null) {
            return "Data sconosciuta"
        }

        val now = Calendar.getInstance()
        val activityTime = Calendar.getInstance()
        activityTime.time = date

        when {
            now.get(Calendar.DATE) == activityTime.get(Calendar.DATE) &&
                    now.get(Calendar.MONTH) == activityTime.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == activityTime.get(Calendar.YEAR) -> {
                val hourDiff = now.get(Calendar.HOUR_OF_DAY) - activityTime.get(Calendar.HOUR_OF_DAY)
                when {
                    hourDiff == 0 -> {
                        val minuteDiff = now.get(Calendar.MINUTE) - activityTime.get(Calendar.MINUTE)
                        if (minuteDiff < 5) "Pochi minuti fa" else "$minuteDiff minuti fa"
                    }
                    hourDiff == 1 -> "1 ora fa"
                    hourDiff < 12 -> "$hourDiff ore fa"
                    else -> "Oggi"
                }
            }
            now.get(Calendar.DATE) - activityTime.get(Calendar.DATE) == 1 &&
                    now.get(Calendar.MONTH) == activityTime.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == activityTime.get(Calendar.YEAR) -> "Ieri"
            now.get(Calendar.WEEK_OF_YEAR) == activityTime.get(Calendar.WEEK_OF_YEAR) &&
                    now.get(Calendar.YEAR) == activityTime.get(Calendar.YEAR) -> {
                val dayFormat = SimpleDateFormat("EEEE", Locale.ITALIAN)
                dayFormat.format(date)
            }
            else -> {
                val dateFormat = SimpleDateFormat("dd MMM", Locale.ITALIAN)
                dateFormat.format(date)
            }
        }
    } catch (e: Exception) {
        Log.e("GroupScreen", "Errore nel parsing della data: $dateString", e)
        "Data sconosciuta"
    }
}

@Composable
fun GroupCard(group: GroupItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF5AC8FA), CircleShape)
            ) {
                Text(
                    text = group.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Ultima attività: ${group.lastActivity}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "${group.memberCount} 👤",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = if (group.memberCount > 0) Color.Black else Color.Red
            )
        }
    }
}