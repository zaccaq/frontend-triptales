package com.example.frontend_triptales

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme
import com.google.accompanist.permissions.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrontendtriptalesTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    TripTalesApp()
                }
            }
        }
    }
}

// --- SCHERMATE DISPONIBILI ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Person)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Group : Screen("group", "Gruppo", Icons.Default.Group)
    object Map : Screen("map", "Mappa", Icons.Default.Map)
}

@Composable
fun TripTalesApp() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    val bottomBarScreens = listOf(Screen.Home, Screen.Group, Screen.Map)

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Login.route) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
                LoginScreen { navController.navigate(Screen.Home.route) }
            }
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable(Screen.Group.route) {
                GroupScreen()
            }
            composable(Screen.Map.route) {
                MapScreen()
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0061FF),
                        Color(0xFF60EFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Logo or Icon
            Icon(
                imageVector = Icons.Default.TravelExplore,
                contentDescription = "TripTales Logo",
                tint = Color.White,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                "TripTales",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Esplora, racconta, ricorda.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Card to hold form
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = onLoginSuccess,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Entra", fontSize = 18.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { /* TODO: aggiungi registrazione */ }) {
                Text(
                    "Non hai un account? Registrati",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}


@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Benvenuto su TripTales!", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun GroupScreen() {
    var groupName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gruppi di Gita", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold))
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Nome del gruppo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {}, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Crea gruppo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {}, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Unisciti a un gruppo")
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(onGranted: () -> Unit) {
    val permissionState = rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) { permissionState.launchPermissionRequest() }
    if (permissionState.status.isGranted) onGranted()
}

@Composable
fun MapScreen() {
    var location by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    RequestLocationPermission {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) location = LatLng(loc.latitude, loc.longitude)
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
    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Sto cercando la tua posizione...", style = MaterialTheme.typography.bodyLarge)
    }
}
