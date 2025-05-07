package com.example.frontend_triptales.api

import android.content.Context
import com.example.frontend_triptales.auth.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
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
    val access: String? = null,    // Cambia da token a access
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
// Interfaccia API
interface TripTalesApi {
    @POST("register/")
    suspend fun registrazione(@Body richiesta: RichiestaRegistrazione): Response<RispostaRegistrazione>

    @POST("api/token/")
    suspend fun login(@Body richiesta: RichiestaLogin): Response<RispostaLogin>

    @GET("api/users/me/")
    suspend fun getUserDetails(@Header("Authorization") token: String): Response<UserDetailsResponse>
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