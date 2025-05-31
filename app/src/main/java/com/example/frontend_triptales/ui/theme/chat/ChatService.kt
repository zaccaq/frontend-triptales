package com.example.frontend_triptales.ui.theme.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.auth.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class ChatService(private val sessionManager: SessionManager, private val context: Context) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Nessun timeout per WebSocket
        .connectTimeout(30000, TimeUnit.MILLISECONDS)
        .build()

    private val _messages = Channel<ChatMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    // Ottiene l'URL di base dinamicamente
    private fun getWebSocketBaseUrl(): String {
        return "ws://costaalberto.duckdns.org:8005"
    }

    fun connectToChat(groupId: String) {
        val token = sessionManager.getToken() ?: return

        val wsBaseUrl = getWebSocketBaseUrl()
        val wsUrl = "$wsBaseUrl/ws/chat/$groupId/"

        Log.d("ChatService", "Connecting to WebSocket: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatService", "Connected to WebSocket: ${response.message}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("ChatService", "Received message: $text")
                    val jsonObject = JSONObject(text)

                    when (jsonObject.getString("type")) {
                        "message" -> {
                            val message = ChatMessage(
                                id = jsonObject.optString("id", "temp-${System.currentTimeMillis()}"),
                                senderId = jsonObject.getString("user_id"),
                                senderName = jsonObject.getString("username"),
                                content = jsonObject.getString("message"),
                                timestamp = jsonObject.getString("timestamp"),
                                isCurrentUser = jsonObject.getString("user_id") == sessionManager.getUserId()
                            )
                            _messages.trySend(message)
                        }
                        "image" -> {
                            // Gestione immagini
                            val message = ChatMessage(
                                id = jsonObject.optString("id", "temp-${System.currentTimeMillis()}"),
                                senderId = jsonObject.getString("user_id"),
                                senderName = jsonObject.getString("username"),
                                content = "",
                                timestamp = jsonObject.getString("timestamp"),
                                isCurrentUser = jsonObject.getString("user_id") == sessionManager.getUserId(),
                                imageUrl = jsonObject.getString("url")
                            )
                            _messages.trySend(message)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatService", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatService", "WebSocket error: ${t.message}", t)
                Log.e("ChatService", "WebSocket response: ${response?.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("ChatService", "WebSocket closed: $reason")
            }
        })
    }

    fun sendTextMessage(message: String) {
        val jsonObject = JSONObject().apply {
            put("type", "message")
            put("message", message)
        }

        webSocket?.send(jsonObject.toString())
    }

    fun sendImageMessage(imageUrl: String) {
        val jsonObject = JSONObject().apply {
            put("type", "image")
            put("url", imageUrl)
        }

        webSocket?.send(jsonObject.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    data class ChatMessage(
        val id: String,
        val senderId: String,
        val senderName: String,
        val content: String,
        val timestamp: String,
        val isCurrentUser: Boolean,
        val imageUrl: String? = null
    )

    suspend fun uploadAndSendImage(context: Context, groupId: String, imageUri: Uri) {
        try {
            // Ottieni il contentResolver
            val contentResolver = context.contentResolver

            // Crea un file temporaneo
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")

            // Copia l'immagine nel file temporaneo
            contentResolver.openInputStream(imageUri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Prepara la multipart request
            val requestFile = RequestBody.create(
                "image/jpeg".toMediaTypeOrNull(),
                file
            )

            val groupIdPart = RequestBody.create(
                "text/plain".toMediaTypeOrNull(),
                groupId
            )

            val imagePart = MultipartBody.Part.createFormData(
                "media_file",
                file.name,
                requestFile
            )

            // Effettua la richiesta API con l'API client dinamico
            val api = ServizioApi.getAuthenticatedClient(context)
            val response = api.uploadChatImage(groupIdPart, imagePart)

            if (response.isSuccessful && response.body() != null) {
                // Ottieni l'URL dell'immagine caricata
                val mediaUrl = response.body()!!.media_url

                // Invia il messaggio con l'immagine
                sendImageMessage(mediaUrl)
            } else {
                Log.e("ChatService", "Error uploading image: ${response.errorBody()?.string()}")
            }

        } catch (e: Exception) {
            Log.e("ChatService", "Error uploading image", e)
        }
    }
}