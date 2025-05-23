// app/src/main/java/com/example/frontend_triptales/ui/theme/screens/GroupChatScreen.kt
package com.example.frontend_triptales.ui.theme.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.frontend_triptales.api.*
import com.example.frontend_triptales.auth.SessionManager
import com.example.frontend_triptales.ui.theme.chat.ChatService
import com.example.frontend_triptales.ui.theme.utils.DateUtils
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
    onCreatePostClick: (String) -> Unit,
    onMapClick: (String) -> Unit = {},
    onShowLocationOnMap: (Double, Double) -> Unit = { _, _ -> },
    onNavigateToComments: (String, String) -> Unit = { _, _ -> } // AGGIUNTO
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val coroutineScope = rememberCoroutineScope()

    // Stati per i messaggi e post
    var messages by remember { mutableStateOf<List<MessageDTO>>(emptyList()) }
    var posts by remember { mutableStateOf<List<PostResponse>>(emptyList()) }
    var newMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Stati per l'interfaccia
    var groupName by remember { mutableStateOf("Caricamento...") }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    // Stati per membri del gruppo
    var showMembersDialog by remember { mutableStateOf(false) }
    var groupMembers by remember { mutableStateOf<List<GroupMembershipDTO>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(false) }
    var membersError by remember { mutableStateOf<String?>(null) }

    // WebSocket - inizializza ma non bloccare l'UI se fallisce
    val chatService = remember {
        try {
            ChatService(sessionManager, context)
        } catch (e: Exception) {
            Log.e("GroupChat", "Errore inizializzazione ChatService: ${e.message}")
            null
        }
    }

    // Launcher per fotocamera
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri.value != null) {
            coroutineScope.launch {
                try {
                    chatService?.uploadAndSendImage(context, groupId, imageUri.value!!)
                } catch (e: Exception) {
                    Log.e("GroupChat", "Errore upload immagine: ${e.message}")
                }
            }
        }
    }

    // Funzione per creare URI immagine
    fun createImageUri(): Uri {
        val file = File(context.cacheDir, "${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    // Carica i dati del gruppo all'avvio
    LaunchedEffect(groupId) {
        coroutineScope.launch {
            try {
                isLoading = true
                error = null

                val api = ServizioApi.getAuthenticatedClient(context)
                Log.d("GroupChat", "Caricamento dati per gruppo: $groupId")

                // 1. Carica dettagli gruppo
                val groupResponse = api.getGroupDetails(groupId)
                if (groupResponse.isSuccessful && groupResponse.body() != null) {
                    groupName = groupResponse.body()!!.name
                    Log.d("GroupChat", "Nome gruppo: $groupName")
                } else {
                    Log.e("GroupChat", "Errore caricamento gruppo: ${groupResponse.code()}")
                    error = "Gruppo non trovato"
                    return@launch
                }

                // 2. Carica messaggi e post in parallelo
                try {
                    // Carica messaggi chat
                    val messagesResponse = api.getGroupMessages(groupId)
                    if (messagesResponse.isSuccessful && messagesResponse.body() != null) {
                        messages = messagesResponse.body()!!.filter { it.is_chat_message }
                        Log.d("GroupChat", "Caricati ${messages.size} messaggi")
                    }

                    // Carica post del gruppo
                    val postsResponse = api.getGroupPosts(groupId)
                    if (postsResponse.isSuccessful && postsResponse.body() != null) {
                        posts = postsResponse.body()!!.filter { !it.is_chat_message }
                        Log.d("GroupChat", "Caricati ${posts.size} post")
                    }
                } catch (e: Exception) {
                    Log.e("GroupChat", "Errore caricamento contenuti: ${e.message}")
                    // Non bloccare l'UI per questo errore
                }

            } catch (e: Exception) {
                Log.e("GroupChat", "Errore generale: ${e.message}", e)
                error = "Errore di connessione: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Inizializza WebSocket (non bloccante)
    LaunchedEffect(groupId) {
        chatService?.let { service ->
            try {
                Log.d("GroupChat", "Tentativo connessione WebSocket...")
                service.connectToChat(groupId)

                // Raccolta messaggi in tempo reale
                coroutineScope.launch {
                    service.messages.collectLatest { newMsg ->
                        Log.d("GroupChat", "Nuovo messaggio WebSocket: ${newMsg.content}")
                        // Aggiungi alla lista se non già presente
                        // (Implementazione più sofisticata se necessario)
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupChat", "WebSocket non disponibile: ${e.message}")
                // Continua senza WebSocket
            }
        }
    }

    // Cleanup WebSocket
    DisposableEffect(Unit) {
        onDispose {
            try {
                chatService?.disconnect()
            } catch (e: Exception) {
                Log.e("GroupChat", "Errore disconnect WebSocket: ${e.message}")
            }
        }
    }

    // Funzione per inviare messaggi
    fun sendMessage() {
        if (newMessage.isBlank() || isSending) return

        coroutineScope.launch {
            try {
                isSending = true
                val messageToSend = newMessage
                newMessage = ""

                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.sendMessage(groupId, messageToSend)

                if (response.isSuccessful && response.body() != null) {
                    Log.d("GroupChat", "Messaggio inviato con successo")
                    // Aggiungi il nuovo messaggio alla lista
                    val newMsg = response.body()!!
                    if (newMsg.is_chat_message) {
                        messages = messages + newMsg
                        listState.animateScrollToItem(messages.size - 1)
                    }
                } else {
                    Log.e("GroupChat", "Errore invio messaggio: ${response.code()}")
                    // Fallback a WebSocket se disponibile
                    chatService?.sendTextMessage(messageToSend)
                }

            } catch (e: Exception) {
                Log.e("GroupChat", "Errore invio: ${e.message}")
                // Fallback a WebSocket se disponibile
                try {
                    chatService?.sendTextMessage(newMessage)
                    newMessage = ""
                } catch (wsError: Exception) {
                    Log.e("GroupChat", "Anche WebSocket fallito: ${wsError.message}")
                }
            } finally {
                isSending = false
            }
        }
    }

    // Funzione per gestire like sui post
    fun handleLikePost(postId: String) {
        coroutineScope.launch {
            try {
                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.likePost(postId)

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    // Aggiorna la lista dei post
                    posts = posts.map { post ->
                        if (post.id.toString() == postId) {
                            post.copy(
                                likes_count = result.total_likes,
                                user_has_liked = result.liked
                            )
                        } else post
                    }
                }
            } catch (e: Exception) {
                Log.e("GroupChat", "Errore like: ${e.message}")
            }
        }
    }

    // Funzione per caricare membri
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
                    membersError = "Errore nel caricamento: ${response.code()}"
                }
            } catch (e: Exception) {
                membersError = "Errore: ${e.message}"
            } finally {
                isLoadingMembers = false
            }
        }
    }

    // UI principale
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF5AC8FA))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Caricamento chat...")
            }
        }
        return
    }

    if (error != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(onClick = {
                    error = null
                    isLoading = true
                }) {
                    Text("Riprova")
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = onBackClick) {
                    Text("Indietro")
                }
            }
        }
        return
    }

    // Interfaccia principale
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
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
                IconButton(onClick = { onCreatePostClick(groupId) }) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Crea post", tint = Color(0xFF5AC8FA))
                }

                IconButton(onClick = {
                    loadGroupMembers()
                    showMembersDialog = true
                }) {
                    Icon(Icons.Default.Group, contentDescription = "Membri", tint = Color(0xFF5AC8FA))
                }

                IconButton(onClick = { onInviteClick(groupId) }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Invita", tint = Color(0xFF5AC8FA))
                }

                IconButton(onClick = { onMapClick(groupId) }) {
                    Icon(Icons.Default.Map, contentDescription = "Mappa", tint = Color(0xFF5AC8FA))
                }
            }
        )

        // Indicatore invio messaggio
        if (isSending) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF5AC8FA)
            )
        }

        // Lista contenuti (messaggi + post)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            // Mostra prima i post
            if (posts.isNotEmpty()) {
                item {
                    Text(
                        "📸 Post del gruppo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(posts) { post ->
                    PostItem(
                        post = post,
                        onLikeClick = { handleLikePost(it) },
                        onLocationClick = { lat, lng ->
                            onShowLocationOnMap(lat, lng)
                        },
                        onCommentClick = { postId -> // AGGIUNTO
                            val postTitle = post.content.take(50).let {
                                if (it.length == 50) "$it..." else it
                            }
                            onNavigateToComments(postId, postTitle)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = Color.Gray.copy(alpha = 0.3f)
                    )
                    Text(
                        "💬 Chat del gruppo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Messaggi chat
            if (messages.isEmpty() && posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nessun contenuto ancora.\nInizia la conversazione o crea un post!",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(messages) { message ->
                ChatMessageItem(message, sessionManager.getUserId())
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Barra di input
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Scrivi un messaggio...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3,
                    enabled = !isSending
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val uri = createImageUri()
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
                    onClick = { sendMessage() },
                    enabled = newMessage.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (newMessage.isNotBlank() && !isSending) Color(0xFF5AC8FA) else Color.Gray,
                            CircleShape
                        )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Invia", tint = Color.White)
                    }
                }
            }
        }
    }

    // Dialog membri
    if (showMembersDialog) {
        MembersDialog(
            members = groupMembers,
            isLoading = isLoadingMembers,
            error = membersError,
            onRefresh = { loadGroupMembers() },
            onDismiss = { showMembersDialog = false }
        )
    }
}

@Composable
fun PostItem(
    post: PostResponse,
    onLikeClick: (String) -> Unit,
    onLocationClick: ((Double, Double) -> Unit)? = null,
    onCommentClick: ((String) -> Unit)? = null // AGGIUNTO
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header del post
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar autore
                if (post.author.profile_picture != null) {
                    AsyncImage(
                        model = post.author.profile_picture,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF5AC8FA), CircleShape)
                    ) {
                        Text(
                            text = post.author.username.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.author.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = DateUtils.formatPostDate(post.created_at),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenuto del post
            Text(
                text = post.content,
                fontSize = 14.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Immagine se presente
            post.media?.firstOrNull()?.let { media ->
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(media.media_url)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Azioni
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like
                IconButton(
                    onClick = { onLikeClick(post.id.toString()) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Like",
                        tint = if (post.user_has_liked) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${post.likes_count}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Commenti - AGGIUNTO
                IconButton(
                    onClick = { onCommentClick?.invoke(post.id.toString()) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = "Commenti",
                        tint = Color(0xFF5AC8FA),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Mostra posizione se disponibile (cliccabile)
                if (post.latitude != null && post.longitude != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            onLocationClick?.invoke(post.latitude, post.longitude)
                        }
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Mostra sulla mappa",
                            tint = Color(0xFF5AC8FA),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = post.location_name ?: "Posizione",
                            fontSize = 12.sp,
                            color = Color(0xFF5AC8FA),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (post.location_name != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = post.location_name,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: MessageDTO, currentUserId: String?) {
    val isCurrentUser = message.author.id.toString() == currentUserId
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isCurrentUser) Color(0xFF5AC8FA) else Color(0xFFEEEEEE)
    val textColor = if (isCurrentUser) Color.White else Color.Black

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.author.username,
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
                        bottomStart = if (isCurrentUser) 16.dp else 0.dp,
                        bottomEnd = if (isCurrentUser) 0.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                message.media?.firstOrNull()?.let { media ->
                    AsyncImage(
                        model = media.media_url,
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
                    text = DateUtils.formatPostDate(message.created_at),
                    color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
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

                Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nessun membro trovato", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(members) { membership ->
                            MemberItem(member = membership)
                            if (members.indexOf(membership) < members.size - 1) {
                                Divider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
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
        // Avatar
        if (member.user.profile_picture != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(member.user.profile_picture)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF5AC8FA), CircleShape)
            ) {
                Text(
                    text = member.user.username.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.user.username,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            if (member.user.first_name != null || member.user.last_name != null) {
                Text(
                    text = "${member.user.first_name ?: ""} ${member.user.last_name ?: ""}".trim(),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
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