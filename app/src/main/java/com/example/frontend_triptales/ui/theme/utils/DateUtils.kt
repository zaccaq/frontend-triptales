package com.example.frontend_triptales.ui.theme.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility per la formattazione delle date
 */
object DateUtils {

    /**
     * Formatta la data di un post per la visualizzazione
     */
    fun formatPostDate(dateString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val now = Calendar.getInstance()
            val postTime = Calendar.getInstance()
            postTime.time = date ?: return "Data sconosciuta"

            // Calcola la differenza
            val diffInMillis = now.timeInMillis - postTime.timeInMillis
            val diffInMinutes = diffInMillis / (60 * 1000)
            val diffInHours = diffInMillis / (60 * 60 * 1000)
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

            return when {
                diffInMinutes < 60 -> {
                    if (diffInMinutes < 1) "Adesso" else "$diffInMinutes min fa"
                }
                diffInHours < 24 -> {
                    if (diffInHours == 1L) "1 ora fa" else "$diffInHours ore fa"
                }
                diffInDays < 7 -> {
                    if (diffInDays == 1L) "Ieri" else "$diffInDays giorni fa"
                }
                else -> {
                    SimpleDateFormat("dd MMM", Locale.ITALIAN).format(date)
                }
            }
        } catch (e: Exception) {
            return "Data sconosciuta"
        }
    }

    /**
     * Formatta la data per la visualizzazione completa
     */
    fun formatFullDate(dateString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN)
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            return "Data sconosciuta"
        }
    }
}