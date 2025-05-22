// app/src/main/java/com/example/frontend_triptales/ui/theme/screens/GroupChatScreen.kt
package com.example.frontend_triptales.ui.theme.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.frontend_triptales.api.GroupMembershipDTO
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.api.UserDTO
import com.example.frontend_triptales.auth.SessionManager
import com.example.frontend_triptales.ui.theme.chat.ChatService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    onBackClick: () -> Unit,
    onInviteClick: (String) -> Unit,
    onMapClick: (String) -> Unit = {} // Nuovo parametro
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val chatService = remember { ChatService(sessionManager, context) }
    val coroutineScope = rememberCoroutineScope()
    val scope = rememberCoroutineScope()

    // Stato per i messaggi
    val messagesList = remember { mutableStateListOf<ChatService.ChatMessage>() }
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    // Debug: Aggiungi stato per tenere traccia dell'invio
    var isSending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }

    // Stato per il nome del gruppo
    var groupName by remember { mutableStateOf("Gruppo $groupId") }

    // Stato per dialog membri e lista membri
    var showMembersDialog by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<GroupMembershipDTO>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(false) }
    var membersError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        try {
            val api = ServizioApi.getAuthenticatedClient(context)
            val response = api.getGroupDetails(groupId)

            if (response.isSuccessful && response.body() != null) {
                val group = response.body()!!
                groupName = group.name
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Funzione per caricare i membri del gruppo
    fun loadGroupMembers() {
        coroutineScope.launch {
            try {
                isLoadingMembers = true
                membersError = null

                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.getGroupMembers(groupId)

                if (response.isSuccessful && response.body() != null) {
                    groupMembers = response.body()!!
                } else {
                    membersError = "Errore nel caricamento dei membri: ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("GroupChatScreen", "Errore caricamento membri: ${e.message}")
                membersError = "Errore nel caricamento dei membri: ${e.message}"
            } finally {
                isLoadingMembers = false
            }
        }
    }

    // Prova a caricare i messaggi dal database all'inizio
    LaunchedEffect(groupId) {
        try {
            val api = ServizioApi.getAuthenticatedClient(context)
            val response = api.getGroupMessages(groupId)

            if (response.isSuccessful && response.body() != null) {
                val messages = response.body()!!
                val currentUserId = sessionManager.getUserId()

                // Converti i messaggi dal formato DTO
                for (msg in messages) {
                    if (msg.is_chat_message) {  // Filtra solo i messaggi chat
                        val chatMsg = ChatService.ChatMessage(
                            id = msg.id.toString(),
                            senderId = msg.author.id.toString(),
                            senderName = msg.author.username,
                            content = msg.content,
                            timestamp = msg.created_at,
                            isCurrentUser = msg.author.id.toString() == currentUserId,
                            imageUrl = msg.media?.firstOrNull()?.media_url
                        )
                        messagesList.add(chatMsg)
                    }
                }

                // Scroll alla fine dopo aver caricato i messaggi
                if (messagesList.isNotEmpty()) {
                    listState.scrollToItem(messagesList.size - 1)
                }
            }
        } catch (e: Exception) {
            Log.e("GroupChatScreen", "Errore caricamento messaggi: ${e.message}")
        }
    }

    // Connessione al WebSocket per la chat in tempo reale
    LaunchedEffect(groupId) {
        try {
            Log.d("GroupChatScreen", "Tentativo connessione WebSocket...")
            chatService.connectToChat(groupId)
            Log.d("GroupChatScreen", "Connessione WebSocket riuscita")

            // Raccolta messaggi in tempo reale
            scope.launch {
                Log.d("GroupChatScreen", "Inizio raccolta messaggi WebSocket")
                chatService.messages.collectLatest { message ->
                    Log.d("GroupChatScreen", "Ricevuto messaggio: ${message.content}")

                    // Aggiungi solo se non è già presente
                    if (!messagesList.any { it.id == message.id }) {
                        messagesList.add(message)

                        // Scroll in fondo quando arriva un nuovo messaggio
                        listState.animateScrollToItem(messagesList.size - 1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GroupChatScreen", "Errore WebSocket: ${e.message}", e)
        }
    }

    // Pulisci la connessione WebSocket quando lasci la schermata
    DisposableEffect(Unit) {
        onDispose {
            chatService.disconnect()
        }
    }

    // Launcher per la fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri.value != null) {
            coroutineScope.launch {
                try {
                    // Usa il metodo corretto per caricare e inviare l'immagine
                    Log.d("GroupChatScreen", "Inizio upload immagine")
                    chatService.uploadAndSendImage(context, groupId, imageUri.value!!)
                    Log.d("GroupChatScreen", "Upload immagine completato")
                } catch (e: Exception) {
                    Log.e("GroupChatScreen", "Errore upload immagine: ${e.message}")
                }
            }
        }
    }

    // Funzione per creare un URI per l'immagine scattata
    fun createImageUri(context: Context): Uri {
        val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    // Mostra il dialog dei membri se richiesto
    if (showMembersDialog) {
        MembersDialog(
            members = groupMembers,
            isLoading = isLoadingMembers,
            error = membersError,
            onRefresh = { loadGroupMembers() },
            onDismiss = { showMembersDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF5AC8FA), CircleShape)
                    ) {
                        Text(
                            text = groupName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            },
            actions = {
                // Pulsante per vedere i membri del gruppo
                IconButton(
                    onClick = {
                        loadGroupMembers()
                        showMembersDialog = true
                    }
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Membri del gruppo",
                        tint = Color(0xFF5AC8FA)
                    )
                }

                // Pulsante per invitare utenti
                IconButton(onClick = { onInviteClick(groupId) }) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Invita utenti",
                        tint = Color(0xFF5AC8FA)
                    )
                }
            }
        )

        // DEBUGGING: Mostra stato invio messaggi
        if (isSending) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Mostra errori di invio
        if (sendError != null) {
            Text(
                text = sendError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(messagesList) { message ->
                ChatMessageItem(message)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Scrivi un messaggio...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val uri = createImageUri(context)
                        imageUri.value = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Gray, CircleShape)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Fotocamera", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pulsante invio con gestione alternativa
                IconButton(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            // MODIFICA CRITICA: Usa coroutineScope per gestire l'invio e aggiungi debug
                            coroutineScope.launch {
                                try {
                                    isSending = true
                                    sendError = null

                                    // Prova prima a utilizzare l'API HTTP per inviare il messaggio
                                    try {
                                        // Crea un messaggio locale temporaneo (con ID temporaneo)
                                        val tempId = "temp-${System.currentTimeMillis()}"
                                        val tempMessage = ChatService.ChatMessage(
                                            id = tempId,
                                            senderId = sessionManager.getUserId() ?: "",
                                            senderName = sessionManager.getUsername() ?: "Me",
                                            content = newMessage,
                                            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                                                .format(Date()),
                                            isCurrentUser = true
                                        )

                                        // Aggiungi messaggio temporaneo alla lista
                                        messagesList.add(tempMessage)

                                        // Scroll in fondo
                                        listState.animateScrollToItem(messagesList.size - 1)

                                        // Invia messaggio tramite API HTTP
                                        val api = ServizioApi.getAuthenticatedClient(context)
                                        val response = api.sendMessage(groupId, newMessage)

                                        if (response.isSuccessful) {
                                            Log.d("GroupChatScreen", "Messaggio inviato con successo via API HTTP")
                                        } else {
                                            Log.e("GroupChatScreen", "Errore API HTTP: ${response.code()}")
                                            // Fallback a WebSocket
                                            chatService.sendTextMessage(newMessage)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("GroupChatScreen", "Errore API HTTP, provo WebSocket: ${e.message}")
                                        // Fallback a WebSocket
                                        chatService.sendTextMessage(newMessage)
                                    }

                                    // Pulisci il campo input
                                    newMessage = ""
                                } catch (e: Exception) {
                                    Log.e("GroupChatScreen", "Errore invio messaggio: ${e.message}", e)
                                    sendError = "Errore invio: ${e.message}"
                                } finally {
                                    isSending = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF5AC8FA), CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Invia", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun MembersDialog(
    members: List<GroupMembershipDTO>,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Membri del Gruppo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi")
                    }
                }

                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = onRefresh) {
                            Text("Riprova")
                        }
                    }
                } else if (members.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nessun membro trovato",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(members) { membership ->
                            MemberItem(member = membership)

                            if (members.indexOf(membership) < members.size - 1) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItem(member: GroupMembershipDTO) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar utente
        MemberAvatar(user = member.user)

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = member.user.username,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )

            Text(
                text = if (member.user.first_name != null || member.user.last_name != null) {
                    "${member.user.first_name ?: ""} ${member.user.last_name ?: ""}".trim()
                } else {
                    ""
                },
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // Badge ruolo
        val roleColor = if (member.role == "admin") Color(0xFFFFD700) else Color(0xFF5AC8FA)
        val roleText = if (member.role == "admin") "Admin" else "Membro"

        Box(
            modifier = Modifier
                .background(
                    color = roleColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = roleText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = roleColor
            )
        }
    }
}

@Composable
fun MemberAvatar(user: UserDTO) {
    if (user.profile_picture != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(user.profile_picture)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar di ${user.username}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )
    } else {
        // Avatar con iniziale
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF5AC8FA), CircleShape)
        ) {
            Text(
                text = user.username.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatService.ChatMessage) {
    val alignment = if (message.isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isCurrentUser) Color(0xFF5AC8FA) else Color(0xFFEEEEEE)
    val textColor = if (message.isCurrentUser) Color.White else Color.Black

    // Formatta l'ora del messaggio
    val time = remember(message.timestamp) {
        try {
            val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
                .parse(message.timestamp)
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date ?: Date())
        } catch (e: Exception) {
            "ora"
        }
    }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (!message.isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isCurrentUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isCurrentUser) 0.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                message.imageUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (message.content.isNotBlank()) {
                    Text(text = message.content, color = textColor)
                }

                Text(
                    text = time,
                    color = if (message.isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}