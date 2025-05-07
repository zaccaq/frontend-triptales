package com.example.frontend_triptales.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.frontend_triptales.api.RispostaLogin

/**
 * Classe per gestire la sessione utente (salvataggio token, controllo login, ecc.)
 */
class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private var editor: SharedPreferences.Editor = prefs.edit()

    companion object {
        const val PREF_NAME = "TripTalesPrefs"
        const val IS_LOGIN = "IsLoggedIn"
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH_TOKEN = "refreshToken"
        const val KEY_USERNAME = "username"
        const val KEY_FIRST_NAME = "firstName"
        const val KEY_USER_ID = "userId"  // Aggiunta questa costante
    }

    fun salvaLoginUtente(username: String, rispostaLogin: RispostaLogin) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_TOKEN, rispostaLogin.access)
        editor.putString(KEY_REFRESH_TOKEN, rispostaLogin.refresh)
        editor.apply()
    }


    /**
     * Controlla se l'utente è attualmente loggato
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGIN, false)
    }

    /**
     * Restituisce il token JWT per le richieste autenticate
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    /**
     * Restituisce il refresh token
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Restituisce lo username dell'utente loggato
     */
    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    /**
     * Effettua il logout cancellando i dati di sessione
     */
    fun logout() {
        editor.clear()
        editor.apply()
    }

    fun salvaInfoUtente(userId: String, username: String, firstName: String, rispostaLogin: RispostaLogin) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_USER_ID, userId)  // Salviamo l'ID utente
        editor.putString(KEY_USERNAME, username)
        editor.putString(KEY_FIRST_NAME, firstName ?: "")
        editor.putString(KEY_TOKEN, rispostaLogin.access)
        editor.putString(KEY_REFRESH_TOKEN, rispostaLogin.refresh)
        editor.apply()
    }

    fun getFirstName(): String {
        return prefs.getString(KEY_FIRST_NAME, "") ?: ""
    }
    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }
}