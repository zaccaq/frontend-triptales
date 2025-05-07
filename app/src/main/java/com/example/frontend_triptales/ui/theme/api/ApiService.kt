package com.example.frontend_triptales.api

import android.content.Context
import com.example.frontend_triptales.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path

// Modelli di dati
data class RichiestaRegistrazione(
    val username: String,
    val email: String,
    val password: String,
    val first_name: String,
    val last_name: String
)

data class RispostaRegistrazione(
    val message: String? = null,
    val error: String? = null
)

data class RichiestaLogin(
    val username: String,
    val password: String
)

data class RispostaLogin(
    val access: String? = null,    // Cambiato da token a access
    val refresh: String? = null,
    val error: String? = null
)

data class UserDetailsResponse(
    val id: Int,
    val username: String,
    val email: String,
    val first_name: String,
    val last_name: String
)

data class PostMediaResponse(
    val id: Int,
    val media_url: String,
    val media_type: String
)

data class GruppoDTO(
    val id: Int,
    val name: String,
    val description: String,
    val cover_image: String?,
    val start_date: String,
    val end_date: String,
    val location: String,
    val created_by: UserDTO,
    val created_at: String,
    val member_count: Int,
    val memberCount: Int = member_count, // Aggiungiamo questo alias
    val lastActivityDate: String? = null
)

data class UserDTO(
    val id: Int,
    val username: String,
    val email: String?,
    val profile_picture: String?,
    val first_name: String?,  // Assicurati che sia nullable
    val last_name: String?
)



data class GroupMembershipDTO(
    val id: Int,
    val user: UserDTO,
    val group: GruppoDTO,
    val join_date: String,
    val role: String
)

data class MessageDTO(
    val id: Int,
    val group: Int,
    val author: UserDTO,
    val content: String,
    val created_at: String,
    val media: List<PostMediaDTO>?,
    val is_chat_message: Boolean
)

data class PostMediaDTO(
    val id: Int,
    val media_type: String,
    val media_url: String,
    val created_at: String
)

// Aggiungi queste interfacce all'interfaccia TripTalesApi
interface TripTalesApi {
    // ... codice esistente ...
    @POST("api/token/")
    suspend fun login(@Body richiesta: RichiestaLogin): Response<RispostaLogin>

    @GET("api/users/me/")
    suspend fun getUserDetails(): Response<UserDTO>  // Rimuovi il parametro token

    @GET("api/trip-groups/")
    suspend fun getMyGroups(): Response<List<GruppoDTO>>

    @GET("api/trip-groups/{id}/")
    suspend fun getGroupDetails(@Path("id") groupId: String): Response<GruppoDTO>

    @GET("api/trip-groups/{id}/messages/")
    suspend fun getGroupMessages(@Path("id") groupId: String): Response<List<MessageDTO>>

    @GET("api/trip-groups/{id}/members/")
    suspend fun getGroupMembers(@Path("id") groupId: String): Response<List<GroupMembershipDTO>>

    @FormUrlEncoded
    @POST("api/trip-groups/{id}/send_message/")
    suspend fun sendMessage(
        @Path("id") groupId: String,
        @Field("content") content: String
    ): Response<MessageDTO>

    @Multipart
    @POST("api/post-media/upload_chat_image/")
    suspend fun uploadChatImage(
        @Part("group_id") groupId: RequestBody,
        @Part image: MultipartBody.Part
    ): Response<PostMediaDTO>

    @POST("api/trip-groups/")
    suspend fun createGroup(@Body groupData: Map<String, Any>): Response<GruppoDTO>

    @POST("api/trip-groups/{id}/join/")
    suspend fun joinGroup(@Path("id") groupId: String): Response<GroupMembershipDTO>
}

// Singleton del servizio API
object ServizioApi {
    // Aggiorna questo con l'URL effettivo del tuo backend quando testi con un server reale
    // Per lo sviluppo locale, utilizza l'indirizzo IP del tuo computer (non localhost o 127.0.0.1)
    // Esempio: "http://192.168.1.100:8000/"
    // 10.0.2.2 è l'indirizzo speciale che l'emulatore Android usa per accedere al localhost del computer host
    private const val URL_BASE = "http://10.0.2.2:8000/"

    // Client HTTP di base, senza autenticazione (per login e registrazione)
    private val httpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(URL_BASE)
        .addConverterFactory(GsonConverterFactory.create())
        .client(httpClient)
        .build()

    val api: TripTalesApi = retrofit.create(TripTalesApi::class.java)

    // Crea un client HTTP con l'autenticazione JWT per le richieste autenticate
    fun getAuthenticatedClient(context: Context): TripTalesApi {
        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken()

        // Se non c'è un token, restituisci il client non autenticato
        if (token == null) {
            return api
        }

        // Crea un interceptor che aggiunge l'header di autenticazione
        val authInterceptor = Interceptor { chain ->
            val newRequest: Request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        }

        // Crea un nuovo client HTTP con l'interceptor di autenticazione
        val authenticatedClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

        // Crea e restituisce una nuova istanza di Retrofit con il client autenticato
        return Retrofit.Builder()
            .baseUrl(URL_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .client(authenticatedClient)
            .build()
            .create(TripTalesApi::class.java)
    }
}