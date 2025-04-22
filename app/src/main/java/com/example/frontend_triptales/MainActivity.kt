package com.example.frontend_triptales

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frontend_triptales.ui.theme.FrontendtriptalesTheme
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TripTales", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            // login logic → più avanti
            onLoginSuccess()
        }) {
            Text("Login")
        }
    }
}
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Group : Screen("group")
    object Map : Screen("map")

}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen {
                navController.navigate(Screen.Home.route)
            }
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onGroupClick = { navController.navigate(Screen.Group.route) },
                onMapClick = { navController.navigate(Screen.Map.route) }
            )
        }
        composable(Screen.Group.route) {
            GroupScreen {
                // Dopo aver creato/unito un gruppo, vai alla bacheca (per ora → Home)
                navController.navigate(Screen.Home.route)
            }
        }
        composable(Screen.Map.route) {
            MapScreen()
        }
    }
}
@Composable
fun HomeScreen(onGroupClick: () -> Unit, onMapClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Benvenuto su TripTales!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onGroupClick) {
            Text("Crea o entra in un gruppo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // TODO: Vai alla bacheca
        }) {
            Text("Vai alla bacheca")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
           onMapClick()
        }) {
            Text("Mappa del viaggio")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // TODO: Vai alla classifica
        }) {
            Text("Classifica")
        }
    }
}
@Composable
fun GroupScreen(onGroupJoin: () -> Unit) {
    var groupName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gruppi di Gita", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Nome del gruppo") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            // TODO: logica creazione gruppo → backend
            onGroupJoin()
        }) {
            Text("Crea gruppo")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // TODO: logica unione gruppo → backend
            onGroupJoin()
        }) {
            Text("Unisciti a un gruppo")
        }
    }
}


@Composable
fun MapScreen() {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.9028, 12.4964), 5f) // Roma zoomato
    }

    // Marker simulati per ora
    val luoghi = listOf(
        Luogo("Colosseo", LatLng(41.8902, 12.4922)),
        Luogo("Fontana di Trevi", LatLng(41.9009, 12.4833)),
        Luogo("Vaticano", LatLng(41.9029, 12.4534))
    )

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        luoghi.forEach { luogo ->
            Marker(
                state = MarkerState(position = luogo.latLng),
                title = luogo.nome,
                snippet = "Luogo condiviso dal gruppo"
            )
        }
    }
}

data class Luogo(val nome: String, val latLng: LatLng)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FrontendtriptalesTheme {
        Greeting("Android")
    }
}