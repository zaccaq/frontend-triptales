// app/src/main/java/com/example/frontend_triptales/ui/theme/screens/GroupChatScreen.kt
package com.example.frontend_triptales.ui.theme.screens

import android.content.Context
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.frontend_triptales.api.ServizioApi
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
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val chatService = remember { ChatService(sessionManager, context) }
    val coroutineScope = rememberCoroutineScope()

    // Stato per i messaggi
    val messagesList = remember { mutableStateListOf<ChatService.ChatMessage>() }
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    // Stato per il nome del gruppo
    var groupName by remember { mutableStateOf("Gruppo $groupId") }

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

    // Connessione al WebSocket per la chat
    LaunchedEffect(groupId) {
        chatService.connectToChat(groupId)

        // Raccolta dei messaggi in arrivo
        coroutineScope.launch {
            chatService.messages.collectLatest { message ->
                messagesList.add(message)
                // Scroll automatico quando arriva un nuovo messaggio
                listState.animateScrollToItem(messagesList.size - 1)
            }
        }
    }

    // Effetto di pulizia quando si lascia la schermata
    DisposableEffect(Unit) {
        onDispose {
            chatService.disconnect()
        }
    }

    // Scorrimento automatico quando si aggiungono messaggi
    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.scrollToItem(messagesList.size - 1)
        }
    }

    // Launcher per la fotocamera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri.value != null) {
            // Upload dell'immagine e invio tramite chat
            coroutineScope.launch {
                // Qui andrebbe implementato l'upload dell'immagine
                // Per ora, inviamo solo l'URI locale
                chatService.sendImageMessage(imageUri.value.toString())
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
            }
        )

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

                IconButton(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            chatService.sendTextMessage(newMessage)
                            newMessage = ""
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