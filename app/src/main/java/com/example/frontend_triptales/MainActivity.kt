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

// --- SCHERMATE DISPONIBILI ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Person)
    object Register : Screen("register", "Registrati", Icons.Default.PersonAdd)
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
            if (currentRoute !in listOf(Screen.Login.route, Screen.Register.route)) {
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
            composable(Screen.Group.route) { GroupScreen() }
            composable(Screen.Map.route) { MapScreen() }
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
                        label = { Text("Nome") },
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
fun GroupScreen() {
    var groupName by remember { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(32.dp)
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
        Button(onClick = { }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text("Crea gruppo")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text("Unisciti a un gruppo")
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

