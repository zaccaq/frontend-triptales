package com.example.frontend_triptales.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun CreateGroupScreen(
    onBackClick: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var inviteMembers by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }

    val context = LocalContext.current

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

        // Nome gruppo
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Nome del gruppo") },
            placeholder = { Text("Es. Vacanza in Sicilia") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Descrizione
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

        Spacer(modifier = Modifier.height(16.dp))

        // Inviti (opzionale)
        OutlinedTextField(
            value = inviteMembers,
            onValueChange = { inviteMembers = it },
            label = { Text("Invita membri (opzionale)") },
            placeholder = { Text("Inserisci nomi utente separati da virgola") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bottone "Crea gruppo"
        Button(
            onClick = {
                if (groupName.isNotBlank()) {
                    val groupId = UUID.randomUUID().toString().take(8)
                    Toast.makeText(context, "Gruppo creato con successo!", Toast.LENGTH_SHORT).show()
                    onGroupCreated(groupId)
                } else {
                    Toast.makeText(context, "Inserisci un nome per il gruppo", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = groupName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crea gruppo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
