package com.example.frontend_triptales.ui.theme.screens

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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.*
import com.example.frontend_triptales.ui.theme.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    postId: String,
    postTitle: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Stati
    var comments by remember { mutableStateOf<List<CommentDTO>>(emptyList()) }
    var newComment by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Carica i commenti all'avvio
    LaunchedEffect(postId) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val api = ServizioApi.getAuthenticatedClient(context)
                val response = api.getPostComments(postId)

                if (response.isSuccessful && response.body() != null) {
                    comments = response.body()!!
                } else {
                    errorMessage = "Errore nel caricamento dei commenti: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Errore di connessione: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Commenti")
                        Text(
                            text = postTitle,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        bottomBar = {
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
                        value = newComment,
                        onValueChange = { newComment = it },
                        placeholder = { Text("Scrivi un commento...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = {
                            if (newComment.isNotBlank() && !isSending) {
                                coroutineScope.launch {
                                    try {
                                        isSending = true

                                        val api = ServizioApi.getAuthenticatedClient(context)
                                        val response = api.addComment(
                                            postId,
                                            CreateCommentRequest(newComment.trim())
                                        )

                                        if (response.isSuccessful && response.body() != null) {
                                            val newCommentDto = response.body()!!
                                            comments = comments + newCommentDto
                                            newComment = ""

                                            // Scroll al nuovo commento
                                            listState.animateScrollToItem(comments.size - 1)
                                        } else {
                                            errorMessage = "Errore nell'invio del commento"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Errore: ${e.message}"
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        },
                        enabled = newComment.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (newComment.isNotBlank()) Color(0xFF5AC8FA) else Color.Gray,
                                shape = CircleShape
                            )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                        } else {
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF5AC8FA)
                    )
                }

                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                // Ricarica i commenti
                                coroutineScope.launch {
                                    try {
                                        isLoading = true
                                        errorMessage = null
                                        val api = ServizioApi.getAuthenticatedClient(context)
                                        val response = api.getPostComments(postId)
                                        if (response.isSuccessful) {
                                            comments = response.body() ?: emptyList()
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Errore: ${e.message}"
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

                comments.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nessun commento",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Sii il primo a commentare!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(comments) { comment ->
                            CommentItem(comment = comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItem(comment: CommentDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
                verticalAlignment = Alignment.Top
        ) {
            // Avatar dell'utente
            if (comment.author.profile_picture != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(comment.author.profile_picture)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Avatar di ${comment.author.username}",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5AC8FA)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.author.username.first().toString().uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenuto del commento
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = comment.author.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Text(
                        text = DateUtils.formatPostDate(comment.created_at),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = comment.content,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}