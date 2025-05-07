package com.example.frontend_triptales.ui.theme.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.frontend_triptales.auth.SessionManager
import com.example.frontend_triptales.ui.theme.screens.*

@Composable
fun TripTalesApp() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    val bottomBarScreens = listOf(Screen.Home, Screen.Group, Screen.Map)

    Scaffold(
        bottomBar = {
            if (currentRoute !in listOf(
                    Screen.Login.route,
                    Screen.Register.route,
                    Screen.CreateGroup.route,
                    "group_chat/"
                )) {
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
            composable(Screen.Home.route) {
                // Otteniamo il nome dell'utente dal SessionManager
                val userName = sessionManager.getFirstName().ifEmpty {
                    sessionManager.getUsername() ?: "Utente"
                }

                HomeScreen(userName = userName)
            }
            composable(Screen.Group.route) {
                GroupScreen(
                    onCreateGroupClick = { navController.navigate(Screen.CreateGroup.route) },
                    onJoinGroupClick = {},
                    onGroupClick = { groupId ->
                        navController.navigate(Screen.GroupChat.createRoute(groupId))
                    }
                )
            }
            composable(Screen.Map.route) { MapScreen() }
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