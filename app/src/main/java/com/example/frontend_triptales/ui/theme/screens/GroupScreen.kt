package com.example.frontend_triptales.ui.theme.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupScreen(
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit
) {
    // Gruppi fittizi per la demo
    val groups = remember {
        listOf(
            GroupItem("1", "Vacanza in Sicilia", "Ultima attività: 2 ore fa", 5),
            GroupItem("2", "Weekend in montagna", "Ultima attività: ieri", 3),
            GroupItem("3", "Gita scolastica Parigi", "Ultima attività: 3 giorni fa", 15)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "I tuoi gruppi",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (groups.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text(
                    "Non sei ancora in nessun gruppo.\nCrea o unisciti a un gruppo per iniziare!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(groups) { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCreateGroupClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crea nuovo gruppo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onJoinGroupClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Unisciti a un gruppo", fontSize = 16.sp)
        }
    }
}

data class GroupItem(
    val id: String,
    val name: String,
    val lastActivity: String,
    val memberCount: Int
)

@Composable
fun GroupCard(group: GroupItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF5AC8FA), CircleShape)
            ) {
                Text(
                    text = group.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
                Text(
                    text = group.lastActivity,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "${group.memberCount} 👤",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }
    }
}
