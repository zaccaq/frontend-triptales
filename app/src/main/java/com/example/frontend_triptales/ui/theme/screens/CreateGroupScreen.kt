// app/src/main/java/com/example/frontend_triptales/ui/theme/screens/CreateGroupScreen.kt
package com.example.frontend_triptales.ui.theme.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.frontend_triptales.api.ServizioApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBackClick: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Formattazione date
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Selettori di data
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Se sono mostrati i selettori di data
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            startDate = dateFormat.format(date)
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Date(millis)
                            endDate = dateFormat.format(date)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Barra superiore
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
            }
            Text(
                "Crea nuovo gruppo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Form di creazione gruppo
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Nome del gruppo") },
            placeholder = { Text("Es. Vacanza in Sicilia") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = groupDescription,
            onValueChange = { groupDescription = it },
            label = { Text("Descrizione (opzionale)") },
            placeholder = { Text("Aggiungi una breve descrizione") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selettori di date e luogo
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Luogo") },
            placeholder = { Text("Es. Roma, Italia") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = if (startDate.isEmpty()) "" else displayDateFormat.format(dateFormat.parse(startDate)!!),
                onValueChange = { },
                label = { Text("Data inizio") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showStartDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleziona data")
                    }
                }
            )

            OutlinedTextField(
                value = if (endDate.isEmpty()) "" else displayDateFormat.format(dateFormat.parse(endDate)!!),
                onValueChange = { },
                label = { Text("Data fine") },
                modifier = Modifier.weight(1f),
                readOnly = true,
                leadingIcon = {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showEndDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Seleziona data")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Switch gruppo privato/pubblico
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Gruppo privato",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isPrivate,
                onCheckedChange = { isPrivate = it }
            )
        }

        Text(
            if (isPrivate) "Solo le persone invitate potranno unirsi"
            else "Chiunque con il codice può unirsi",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottone "Crea gruppo"
        Button(
            onClick = {
                if (groupName.isBlank()) {
                    Toast.makeText(context, "Inserisci un nome per il gruppo", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (location.isBlank()) {
                    Toast.makeText(context, "Inserisci un luogo", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (startDate.isBlank() || endDate.isBlank()) {
                    Toast.makeText(context, "Seleziona le date di inizio e fine", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Crea il gruppo
                coroutineScope.launch {
                    try {
                        isLoading = true

                        val groupData = mapOf(
                            "name" to groupName,
                            "description" to groupDescription,
                            "location" to location,
                            "start_date" to startDate,
                            "end_date" to endDate,
                            "is_private" to isPrivate
                        )

                        val api = ServizioApi.getAuthenticatedClient(context)
                        val response = api.createGroup(groupData)

                        if (response.isSuccessful && response.body() != null) {
                            Toast.makeText(context, "Gruppo creato con successo!", Toast.LENGTH_SHORT).show()
                            onGroupCreated(response.body()!!.id.toString())
                        } else {
                            Toast.makeText(context, "Errore nella creazione del gruppo", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White
                )
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crea gruppo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}