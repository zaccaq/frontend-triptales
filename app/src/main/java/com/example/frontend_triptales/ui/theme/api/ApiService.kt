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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Modelli di dati
data class BadgeDTO(
    val id: Int,
    val name: String,
    val description: String,
    val icon_url: String?
)

data class UserStatsDTO(
    val postCount: Int,
    val likesCount: Int,
    val commentsCount: Int
)

data class LeaderboardEntryDTO(
    val id: Int,
    val username: String,
    val profile_picture: String?,
    val post_count: Int,
    val like_count: Int,
    val comment_count: Int,
    val total_score: Int,
    val badges: List<BadgeDTO>
)

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
    val access: String? = null,
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
    val memberCount: Int = member_count,
    val lastActivityDate: String? = null,
    val user_role: String? = null
)

data class UserDTO(
    val id: Int,
    val username: String,
    val email: String?,
    val profile_picture: String?,
    val first_name: String?,
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

data class CreateGroupRequest(
    val name: String,
    val description: String,
    val location: String,
    val start_date: String,
    val end_date: String,
    val is_private: Boolean
)

data class GroupInviteRequest(
    val username_or_email: String
)

data class GroupInviteDTO(
    val id: Int,
    val group: GruppoDTO,
    val invited_by: UserDTO,
    val invited_user: UserDTO,
    val status: String,
    val created_at: String
)

interface TripTalesApi {

    @POST("api/trip-groups/{id}/invite_user/")
    suspend fun inviteUserToGroup(
        @Path("id") groupId: String,
        @Body request: GroupInviteRequest
    ): Response<GroupInviteDTO>

    @GET("api/group-invites/my_invites/")
    suspend fun getMyInvites(): Response<List<GroupInviteDTO>>

    @POST("api/trip-groups/accept_invite/{id}/")
    suspend fun acceptInvite(@Path("id") inviteId: String): Response<GroupMembershipDTO>

    @POST("api/trip-groups/decline_invite/{id}/")
    suspend fun declineInvite(@Path("id") inviteId: String): Response<Any>

    @POST("register/") // Corretto l'endpoint di registrazione
    suspend fun registrazione(@Body richiesta: RichiestaRegistrazione): Response<RispostaRegistrazione>

    @POST("api/token/")
    suspend fun login(@Body richiesta: RichiestaLogin): Response<RispostaLogin>

    @GET("api/users/me/")
    suspend fun getUserDetails(): Response<UserDTO>

    @GET("api/trip-groups/my/")
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
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<GruppoDTO>

    @GET("api/users/me/badges/")
    suspend fun getUserBadges(): Response<List<BadgeDTO>>

    @GET("api/users/me/stats/")
    suspend fun getUserStats(): Response<UserStatsDTO>

    @GET("api/users/leaderboard/")
    suspend fun getLeaderboard(@Query("group_id") groupId: String? = null): Response<List<LeaderboardEntryDTO>>

    @Multipart
    @POST("api/post-media/upload_media/")
    suspend fun uploadMedia(
        @Part("post_id") postId: RequestBody,
        @Part media: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody? = null,
        @Part("longitude") longitude: RequestBody? = null,
        @Part("detected_objects") detectedObjects: RequestBody? = null,
        @Part("ocr_text") ocrText: RequestBody? = null,
        @Part("caption") caption: RequestBody? = null
    ): Response<PostMediaDTO>

    @POST("api/trip-groups/{id}/join/")
    suspend fun joinGroup(@Path("id") groupId: String): Response<GroupMembershipDTO>
}

object ServizioApi {
    // URL per diversi ambienti
    private const val EMULATOR_URL = "http://10.0.2.2:8000/"
    private const val LOCAL_DEVICE_URL = "http://10.0.2.2:8000/"
    private const val PRODUCTION_URL = "https://api.triptales.example.com/"

    // Tempo di timeout per le richieste
    private const val TIMEOUT_MS = 15000L

    // Funzione per determinare l'URL base appropriato
    fun getBaseUrl(context: Context): String {
        return when {
            isEmulator() -> EMULATOR_URL
            isDebugBuild(context) -> LOCAL_DEVICE_URL
            else -> PRODUCTION_URL
        }
    }

    // Controlla se l'app è in esecuzione su un emulatore
    private fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic") ||
                "google_sdk" == android.os.Build.PRODUCT)
    }

    // Controlla se è una build di debug
    private fun isDebugBuild(context: Context): Boolean {
        return 0 != context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
    }

    // Client HTTP di base con timeout
    private fun getBaseHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()
    }

    // Ottieni l'API client per il contesto specifico
    fun getApi(context: Context): TripTalesApi {
        val baseUrl = getBaseUrl(context)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getBaseHttpClient())
            .build()
            .create(TripTalesApi::class.java)
    }

    // Per compatibilità con il codice esistente
    val api: TripTalesApi by lazy {
        Retrofit.Builder()
            .baseUrl(EMULATOR_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getBaseHttpClient())
            .build()
            .create(TripTalesApi::class.java)
    }

    // Crea un client HTTP con l'autenticazione JWT per le richieste autenticate
    fun getAuthenticatedClient(context: Context): TripTalesApi {
        val sessionManager = SessionManager(context)
        val token = sessionManager.getToken()
        val baseUrl = getBaseUrl(context)

        // Se non c'è un token, restituisci il client non autenticato
        if (token == null) {
            return getApi(context)
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
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .build()

        // Crea e restituisce una nuova istanza di Retrofit con il client autenticato
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(authenticatedClient)
            .build()
            .create(TripTalesApi::class.java)
    }
}