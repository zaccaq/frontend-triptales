package com.example.frontend_triptales

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import coil.compose.rememberImagePainter
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrontendtriptalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TripTalesApp()
                }
            }
        }
    }
}

// --- AGGIORNA LA CLASSE SCREEN PER LE NUOVE ROTTE ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Person)
    object Register : Screen("register", "Registrati", Icons.Default.PersonAdd)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Group : Screen("group", "Gruppo", Icons.Default.Group)
    object Map : Screen("map", "Mappa", Icons.Default.Map)
    object CreateGroup : Screen("create_group", "Crea Gruppo", Icons.Default.Add)
    object GroupChat : Screen("group_chat/{groupId}", "Chat", Icons.Default.Send) {
        fun createRoute(groupId: String) = "group_chat/$groupId"
    }
}

@Composable
fun TripTalesApp() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val bottomBarScreens = listOf(Screen.Home, Screen.Group, Screen.Map)

    Scaffold(
        bottomBar = {
            if (currentRoute !in listOf(Screen.Login.route, Screen.Register.route,
                    Screen.CreateGroup.route,
                    "group_chat/")) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Home.route) },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            composable(Screen.Register.route) {
                RegistrationScreen(
                    onRegistrationSuccess = { navController.navigate(Screen.Home.route) },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Group.route) {
                GroupScreen(
                    onCreateGroupClick = { navController.navigate(Screen.CreateGroup.route) },
                    onJoinGroupClick = { /* gestire l'unione a un gruppo */ },
                    onGroupClick = { groupId -> navController.navigate(Screen.GroupChat.createRoute(groupId)) }
                )
            }
            composable(Screen.Map.route) { MapScreen() }

            // Nuove schermate
            composable(Screen.CreateGroup.route) {
                CreateGroupScreen(
                    onBackClick = { navController.popBackStack() },
                    onGroupCreated = { groupId ->
                        navController.navigate(Screen.GroupChat.createRoute(groupId)) {
                            popUpTo(Screen.Group.route)
                        }
                    }
                )
            }
            composable(
                route = Screen.GroupChat.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: "unknown"
                GroupChatScreen(
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onNavigateToRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    GradientBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Icon(
                Icons.Default.TravelExplore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedAppTitle(fullText = "TripTales", typingSpeedMillis = 80L)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Esplora, racconta, ricorda.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Username o Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    Button(
                        onClick = onLoginSuccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
                    ) {
                        Text("Entra", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text(
                    "Non hai un account? Registrati",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RegistrationScreen(onRegistrationSuccess: () -> Unit, onNavigateToLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    GradientBackground {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Icon(
                Icons.Default.PersonAdd,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedAppTitle(fullText = "Crea Account", typingSpeedMillis = 70L)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Unisciti a TripTales!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Conferma Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    errorMessage?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (password == confirmPassword && password.isNotBlank()) {
                                onRegistrationSuccess()
                            } else {
                                errorMessage = "Le password non coincidono o sono vuote"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5AC8FA))
                    ) {
                        Text("Registrati", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    "Hai già un account? Accedi",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF5AC8FA),
                        Color(0xFF007AFF),
                        Color(0xFF5856D6)
                    )
                )
            )
    ) {
        content()
    }
}
@Composable
fun AnimatedAppTitle(fullText: String, typingSpeedMillis: Long = 100L) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(fullText) {
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            delay(typingSpeedMillis)
        }
    }

    Text(
        text = displayedText,
        fontSize = 36.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
}

@Composable
fun HomeScreen() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Benvenuto su TripTales!", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun GroupScreen(
    onCreateGroupClick: () -> Unit,
    onJoinGroupClick: () -> Unit,
    onGroupClick: (String) -> Unit
) {
    // Simula alcuni gruppi per la demo
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

// Modello dati per i gruppi
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

// --- SCHERMATA CREAZIONE GRUPPO ---
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
        // Barra superiore con pulsante indietro
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

        // Campi per creare il gruppo
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

        // Toggle per gruppo privato/pubblico
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
            if (isPrivate) "Solo le persone invitate potranno unirsi" else "Chiunque con il codice può unirsi",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inviteMembers,
            onValueChange = { inviteMembers = it },
            label = { Text("Invita membri (opzionale)") },
            placeholder = { Text("Inserisci nomi utente separati da virgola") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (groupName.isNotBlank()) {
                    // Qui potresti aggiungere la logica per creare il gruppo sul backend
                    // Per ora generiamo un ID di gruppo fittizio
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

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isCurrentUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    onBackClick: () -> Unit
) {
    // In un'app reale, recupereresti queste informazioni dal backend
    val groupName = remember {
        when (groupId) {
            "1" -> "Vacanza in Sicilia"
            "2" -> "Weekend in montagna"
            "3" -> "Gita scolastica Parigi"
            else -> "Gruppo $groupId"
        }
    }

    // Messaggi di esempio
    val messages = remember {
        listOf(
            ChatMessage("1", "user1", "Marco", "Ciao a tutti! Chi è pronto per il viaggio?", System.currentTimeMillis() - 3600000, false),
            ChatMessage("2", "user2", "Laura", "Io sono pronta! Ho già fatto le valigie.", System.currentTimeMillis() - 3500000, false),
            ChatMessage("3", "currentUser", "Tu", "Io devo ancora organizzarmi, ma ci sarò!", System.currentTimeMillis() - 3400000, true),
            ChatMessage("4", "user3", "Giovanni", "Qualcuno ha controllato le previsioni meteo?", System.currentTimeMillis() - 3300000, false),
            ChatMessage("5", "currentUser", "Tu", "Sembra che ci sarà bel tempo per tutto il weekend! 😎", System.currentTimeMillis() - 3200000, true),
            ChatMessage("6", "user1", "Marco", "Perfetto! Non vedo l'ora di partire!", System.currentTimeMillis() - 3100000, false)
        )
    }

    var newMessage by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Scorrimento automatico in fondo alla chat all'avvio
    LaunchedEffect(Unit) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra superiore della chat
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF5AC8FA), CircleShape)
                    ) {
                        Text(
                            text = groupName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Lista messaggi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            items(messages) { message ->
                ChatMessageItem(message)
            }
        }

        // Area invio messaggi
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    placeholder = { Text("Scrivi un messaggio...") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        // Qui aggiungeresti la logica per inviare il messaggio
                        if (newMessage.isNotBlank()) {
                            // In un'app reale, questo invierebbe il messaggio al backend
                            newMessage = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF5AC8FA), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Invia",
                        tint = Color.White
                    )
                }
            }
        }
    }
}


@Composable
fun ChatMessageItem(message: ChatMessage) {
    val alignment = if (message.isCurrentUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isCurrentUser) Color(0xFF5AC8FA) else Color(0xFFEEEEEE)
    val textColor = if (message.isCurrentUser) Color.White else Color.Black

    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { dateFormat.format(Date(message.timestamp)) }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (!message.isCurrentUser) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .background(
                    color = bubbleColor,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isCurrentUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isCurrentUser) 0.dp else 16.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = timeString,
                    color = if (message.isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var location by remember { mutableStateOf<LatLng?>(null) }
    val permissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        permissionState.launchPermissionRequest()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { location = LatLng(it.latitude, it.longitude) }
            }
        }
    }

    location?.let { currentLocation ->
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(currentLocation, 15f)
            },
            properties = MapProperties(isMyLocationEnabled = true)
        ) {
            Marker(
                state = MarkerState(position = currentLocation),
                title = "La tua posizione"
            )
        }
    } ?: Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("Sto cercando la tua posizione...", style = MaterialTheme.typography.bodyLarge)
    }
}
@Composable
fun CameraScreen() {
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // Open camera to capture image
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            // Do something with the captured photo (display preview or share)
        }
    }

    // Create temporary file to store the photo
    val imageFile = createImageFile()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        // Show photo preview if exists
        photoUri?.let {
            Image(
                painter = rememberImagePainter(it),
                contentDescription = "Photo preview",
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Button to take a photo
        Button(
            onClick = {
                imageFile?.let { file ->
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    launcher.launch(uri)
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scatta Foto", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Share button (simulate the sharing with the group)
        Button(
            onClick = {
                photoUri?.let {
                    // Implement sharing logic (example: upload to backend or share to group)
                    Toast.makeText(context, "Foto condivisa con il gruppo!", Toast.LENGTH_SHORT).show()
                } ?: Toast.makeText(context, "Nessuna foto scattata!", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Condividi con il gruppo", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun createImageFile(): File? {
    // Create an image file to store the photo
    val imageFileName = "JPEG_${System.currentTimeMillis()}_.jpg"
    val storageDir = File("/path/to/save/images") // Replace with actual directory path
    return try {
        File.createTempFile(imageFileName, ".jpg", storageDir)
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FrontendtriptalesTheme {
        CameraScreen()
    }
}

