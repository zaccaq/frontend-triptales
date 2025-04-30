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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isCurrentUser: Boolean,
    val imageUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    onBackClick: () -> Unit
) {
    val groupName = remember(groupId) {
        when (groupId) {
            "1" -> "Vacanza in Sicilia"
            "2" -> "Weekend in montagna"
            "3" -> "Gita scolastica Parigi"
            else -> "Gruppo $groupId"
        }
    }

    val context = LocalContext.current
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val messagesList = remember {
        mutableStateListOf(
            ChatMessage("1", "user1", "Marco", "Ciao a tutti! Chi è pronto per il viaggio?", System.currentTimeMillis() - 3600000, false),
            ChatMessage("2", "user2", "Laura", "Io sono pronta! Ho già fatto le valigie.", System.currentTimeMillis() - 3500000, false),
            ChatMessage("3", "me", "Tu", "Io devo ancora organizzarmi, ma ci sarò!", System.currentTimeMillis() - 3400000, true)
        )
    }

    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            listState.scrollToItem(messagesList.size - 1)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri.value != null) {
            messagesList.add(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = "me",
                    senderName = "Tu",
                    content = "",
                    timestamp = System.currentTimeMillis(),
                    isCurrentUser = true,
                    imageUri = imageUri.value
                )
            )
        }
    }

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
                            messagesList.add(
                                ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    senderId = "me",
                                    senderName = "Tu",
                                    content = newMessage,
                                    timestamp = System.currentTimeMillis(),
                                    isCurrentUser = true
                                )
                            )
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
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isCurrentUser) Color(0xFF5AC8FA) else Color(0xFFEEEEEE)
    val textColor = if (message.isCurrentUser) Color.White else Color.Black

    val time = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
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
                message.imageUri?.let {
                    Image(
                        painter = rememberAsyncImagePainter(it),
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
