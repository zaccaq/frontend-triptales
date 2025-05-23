package com.example.frontend_triptales.ui.theme.screens

import FilterOptions
import PostItem
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.frontend_triptales.api.ServizioApi
import com.example.frontend_triptales.api.CreatePostRequest
import com.example.frontend_triptales.ui.theme.services.LocationData
import com.example.frontend_triptales.ui.theme.services.LocationManager
import com.example.frontend_triptales.ui.theme.mlkit.MLKitResult
import com.example.frontend_triptales.ui.theme.mlkit.MLKitService
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Componente FilterBottomSheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    currentFilters: FilterOptions,
    availableGroups: List<String>,
    onFiltersChanged: (FilterOptions) -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxHeight(0.8f)
        ) {
            var tempFilters by remember { mutableStateOf(currentFilters) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtri",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        TextButton(onClick = {
                            tempFilters = FilterOptions()
                            onFiltersChanged(tempFilters)
                        }) {
                            Text("Reset")
                        }

                        Button(
                            onClick = {
                                onFiltersChanged(tempFilters)
                                onDismiss()
                            }
                        ) {
                            Text("Applica")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Barra di ricerca
                    item {
                        OutlinedTextField(
                            value = tempFilters.searchQuery,
                            onValueChange = { tempFilters = tempFilters.copy(searchQuery = it) },
                            label = { Text("Cerca nel contenuto") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (tempFilters.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { tempFilters = tempFilters.copy(searchQuery = "") }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Cancella")
                                    }
                                }
                            }
                        )
                    }

                    // Filtro per tipo di post
                    item {
                        Column {
                            Text(
                                text = "Tipo di contenuto",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(FilterType.values()) { type ->
                                    FilterChip(
                                        selected = tempFilters.filterType == type,
                                        onClick = { tempFilters = tempFilters.copy(filterType = type) },
                                        label = {
                                            Text(
                                                when(type) {
                                                    FilterType.ALL -> "Tutti"
                                                    FilterType.MY_POSTS -> "I miei post"
                                                    FilterType.WITH_MEDIA -> "Con foto"
                                                    FilterType.WITH_LOCATION -> "Con posizione"
                                                    FilterType.RECENT -> "Recenti"
                                                }
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                when(type) {
                                                    FilterType.ALL -> Icons.Default.SelectAll
                                                    FilterType.MY_POSTS -> Icons.Default.Person
                                                    FilterType.WITH_MEDIA -> Icons.Default.Image
                                                    FilterType.WITH_LOCATION -> Icons.Default.LocationOn
                                                    FilterType.RECENT -> Icons.Default.Schedule
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Filtro per data
                    item {
                        Column {
                            Text(
                                text = "Periodo",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(DateRange.values()) { range ->
                                    FilterChip(
                                        selected = tempFilters.dateRange == range,
                                        onClick = { tempFilters = tempFilters.copy(dateRange = range) },
                                        label = { Text(range.displayName) }
                                    )
                                }
                            }
                        }
                    }

                    // Filtro per gruppi
                    if (availableGroups.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "Gruppi",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    items(availableGroups) { group ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    tempFilters = if (group in tempFilters.selectedGroups) {
                                                        tempFilters.copy(
                                                            selectedGroups = tempFilters.selectedGroups - group
                                                        )
                                                    } else {
                                                        tempFilters.copy(
                                                            selectedGroups = tempFilters.selectedGroups + group
                                                        )
                                                    }
                                                }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = group in tempFilters.selectedGroups,
                                                onCheckedChange = { checked ->
                                                    tempFilters = if (checked) {
                                                        tempFilters.copy(
                                                            selectedGroups = tempFilters.selectedGroups + group
                                                        )
                                                    } else {
                                                        tempFilters.copy(
                                                            selectedGroups = tempFilters.selectedGroups - group
                                                        )
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = group,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Spazio finale
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// Componente FilterIndicator
@Composable
fun FilterIndicator(
    filters: FilterOptions,
    onClearFilter: (String) -> Unit
) {
    val activeFilters = mutableListOf<Pair<String, String>>()

    if (filters.filterType != FilterType.ALL) {
        activeFilters.add("type" to when(filters.filterType) {
            FilterType.MY_POSTS -> "I miei post"
            FilterType.WITH_MEDIA -> "Con foto"
            FilterType.WITH_LOCATION -> "Con posizione"
            FilterType.RECENT -> "Recenti"
            else -> ""
        })
    }

    if (filters.dateRange != DateRange.ALL) {
        activeFilters.add("date" to filters.dateRange.displayName)
    }

    if (filters.selectedGroups.isNotEmpty()) {
        activeFilters.add("groups" to "${filters.selectedGroups.size} gruppi")
    }

    if (filters.searchQuery.isNotEmpty()) {
        activeFilters.add("search" to "\"${filters.searchQuery}\"")
    }

    if (activeFilters.isNotEmpty()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeFilters) { (key, label) ->
                AssistChip(
                    onClick = { onClearFilter(key) },
                    label = { Text(label) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Rimuovi filtro",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFF5AC8FA).copy(alpha = 0.1f),
                        labelColor = Color(0xFF5AC8FA)
                    )
                )
            }
        }
    }
}

// Funzioni di utilità per il filtro
fun filterPosts(posts: List<PostItem>, filters: FilterOptions): List<PostItem> {
    return posts.filter { post ->
        // Filtro per ricerca nel contenuto
        if (filters.searchQuery.isNotEmpty()) {
            val query = filters.searchQuery.lowercase()
            val matchesContent = post.content.lowercase().contains(query)
            val matchesUser = post.userName.lowercase().contains(query)
            val matchesLocation = post.location.lowercase().contains(query)
            if (!matchesContent && !matchesUser && !matchesLocation) return@filter false
        }

        // Filtro per tipo
        when (filters.filterType) {
            FilterType.MY_POSTS -> if (!post.isMyPost) return@filter false
            FilterType.WITH_MEDIA -> if (post.mediaUrl.isNullOrEmpty()) return@filter false
            FilterType.WITH_LOCATION -> if (post.location.isBlank()) return@filter false
            FilterType.RECENT -> {
                // Considera recenti i post degli ultimi 7 giorni (più permissivo)
                val postDate = parsePostDate(post.timestamp)
                if (postDate != null) {
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }
                    if (postDate.before(sevenDaysAgo.time)) return@filter false
                }
                // Se non riusciamo a parsare la data, consideriamo il post come recente
            }
            FilterType.ALL -> { /* Nessun filtro */ }
        }

        // Filtro per data
        when (filters.dateRange) {
            DateRange.TODAY -> {
                val postDate = parsePostDate(post.timestamp)
                if (postDate != null) {
                    val today = Calendar.getInstance()
                    val postCal = Calendar.getInstance()
                    postCal.time = postDate

                    if (postCal.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) ||
                        postCal.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
                        return@filter false
                    }
                } else {
                    // Se non riusciamo a parsare la data, proviamo con il timestamp relativo
                    if (!isToday(post.timestamp)) return@filter false
                }
            }
            DateRange.WEEK -> {
                val postDate = parsePostDate(post.timestamp)
                if (postDate != null) {
                    val weekAgo = Calendar.getInstance().apply {
                        add(Calendar.WEEK_OF_YEAR, -1)
                    }
                    if (postDate.before(weekAgo.time)) return@filter false
                } else {
                    // Se non riusciamo a parsare la data, proviamo con il timestamp relativo
                    if (!isWithinWeek(post.timestamp)) return@filter false
                }
            }
            DateRange.MONTH -> {
                val postDate = parsePostDate(post.timestamp)
                if (postDate != null) {
                    val monthAgo = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -1)
                    }
                    if (postDate.before(monthAgo.time)) return@filter false
                } else {
                    // Se non riusciamo a parsare la data, proviamo con il timestamp relativo
                    if (!isWithinMonth(post.timestamp)) return@filter false
                }
            }
            DateRange.ALL -> { /* Nessun filtro */ }
        }

        // Filtro per gruppi
        if (filters.selectedGroups.isNotEmpty()) {
            if (post.groupName !in filters.selectedGroups) return@filter false
        }

        true
    }
}

private fun parsePostDate(timestamp: String): Date? {
    return try {
        // Prova diversi formati di data
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        )

        for (format in formats) {
            try {
                return format.parse(timestamp)
            } catch (e: Exception) {
                continue
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

// Funzioni helper per gestire i timestamp relativi (es. "2 ore fa", "ieri")
private fun isToday(timestamp: String): Boolean {
    val lowerTimestamp = timestamp.lowercase()
    return lowerTimestamp.contains("adesso") ||
            lowerTimestamp.contains("min fa") ||
            lowerTimestamp.contains("ora fa") ||
            lowerTimestamp.contains("ore fa")
}

private fun isWithinWeek(timestamp: String): Boolean {
    val lowerTimestamp = timestamp.lowercase()
    return lowerTimestamp.contains("adesso") ||
            lowerTimestamp.contains("min fa") ||
            lowerTimestamp.contains("ora fa") ||
            lowerTimestamp.contains("ore fa") ||
            lowerTimestamp.contains("ieri") ||
            lowerTimestamp.contains("giorni fa")
}

private fun isWithinMonth(timestamp: String): Boolean {
    val lowerTimestamp = timestamp.lowercase()
    return lowerTimestamp.contains("adesso") ||
            lowerTimestamp.contains("min fa") ||
            lowerTimestamp.contains("ora fa") ||
            lowerTimestamp.contains("ore fa") ||
            lowerTimestamp.contains("ieri") ||
            lowerTimestamp.contains("giorni fa") ||
            lowerTimestamp.contains("settimane fa")
}

// Funzione di debug per capire cosa sta succedendo
fun debugFilterPosts(posts: List<PostItem>, filters: FilterOptions): List<PostItem> {
    Log.d("FilterDebug", "=== INIZIO DEBUG FILTRI ===")
    Log.d("FilterDebug", "Totale post: ${posts.size}")
    Log.d("FilterDebug", "Filtri attivi:")
    Log.d("FilterDebug", "- Search: '${filters.searchQuery}'")
    Log.d("FilterDebug", "- Type: ${filters.filterType}")
    Log.d("FilterDebug", "- Date: ${filters.dateRange}")
    Log.d("FilterDebug", "- Groups: ${filters.selectedGroups}")

    val result = posts.filterIndexed { index, post ->
        Log.d("FilterDebug", "--- Post $index ---")
        Log.d("FilterDebug", "Contenuto: '${post.content.take(50)}...'")
        Log.d("FilterDebug", "Utente: ${post.userName}")
        Log.d("FilterDebug", "Gruppo: ${post.groupName}")
        Log.d("FilterDebug", "Timestamp: ${post.timestamp}")
        Log.d("FilterDebug", "isMyPost: ${post.isMyPost}")
        Log.d("FilterDebug", "hasMedia: ${!post.mediaUrl.isNullOrEmpty()}")
        Log.d("FilterDebug", "hasLocation: ${post.location.isNotBlank()}")

        // Filtro per ricerca nel contenuto
        if (filters.searchQuery.isNotEmpty()) {
            val query = filters.searchQuery.lowercase()
            val matchesContent = post.content.lowercase().contains(query)
            val matchesUser = post.userName.lowercase().contains(query)
            val matchesLocation = post.location.lowercase().contains(query)
            val passesSearch = matchesContent || matchesUser || matchesLocation
            Log.d("FilterDebug", "Passa ricerca: $passesSearch (query: '$query')")
            if (!passesSearch) {
                Log.d("FilterDebug", "RESPINTO per ricerca")
                return@filterIndexed false
            }
        }

        // Filtro per tipo
        val passesType = when (filters.filterType) {
            FilterType.MY_POSTS -> post.isMyPost
            FilterType.WITH_MEDIA -> !post.mediaUrl.isNullOrEmpty()
            FilterType.WITH_LOCATION -> post.location.isNotBlank()
            FilterType.RECENT -> true // Per ora consideriamo tutti recenti
            FilterType.ALL -> true
        }
        Log.d("FilterDebug", "Passa tipo: $passesType")
        if (!passesType) {
            Log.d("FilterDebug", "RESPINTO per tipo")
            return@filterIndexed false
        }

        // Filtro per gruppi
        val passesGroups = if (filters.selectedGroups.isNotEmpty()) {
            post.groupName in filters.selectedGroups
        } else true
        Log.d("FilterDebug", "Passa gruppi: $passesGroups")
        if (!passesGroups) {
            Log.d("FilterDebug", "RESPINTO per gruppi")
            return@filterIndexed false
        }

        Log.d("FilterDebug", "ACCETTATO")
        true
    }

    Log.d("FilterDebug", "Risultato finale: ${result.size} post")
    Log.d("FilterDebug", "=== FINE DEBUG FILTRI ===")

    return result
}