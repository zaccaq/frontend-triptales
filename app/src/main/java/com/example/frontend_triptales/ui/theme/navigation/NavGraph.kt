package com.example.frontend_triptales.ui.theme.navigation

import HomeScreen
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
                    Screen.CreatePost.route,
                    "group_chat/{groupId}", // ✅ Corretto
                    "create_post/{groupId}", // ✅ Corretto
                    "group_map/{groupId}", // ✅ Aggiungi questo
                    Screen.InvitesList.route,
                    "invite_to_group/{groupId}", // ✅ Corretto
                    Screen.AIAssistant.route
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
            // ===== AUTH SCREENS =====
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

            // ===== MAIN SCREENS =====
            composable(Screen.Home.route) {
                val userName = sessionManager.getFirstName().ifEmpty {
                    sessionManager.getUsername() ?: "Utente"
                }
                HomeScreen(
                    onProfileClick = { navController.navigate(Screen.Profile.route) },
                    onAIAssistantClick = { navController.navigate(Screen.AIAssistant.route) }
                )
            }

            composable(Screen.Group.route) {
                GroupScreen(
                    onCreateGroupClick = { navController.navigate(Screen.CreateGroup.route) },
                    onJoinGroupClick = { navController.navigate(Screen.JoinGroup.route) },
                    onGroupClick = { groupId ->
                        navController.navigate(Screen.GroupChat.createRoute(groupId))
                    },
                    onInvitesClick = {
                        navController.navigate(Screen.InvitesList.route)
                    }
                )
            }

            composable(Screen.Map.route) {
                MapScreen()
            }

            // ===== GROUP SCREENS =====
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

            composable(Screen.JoinGroup.route) {
                JoinGroupScreen(
                    onBackClick = { navController.popBackStack() },
                    onGroupJoined = { groupId ->
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
                    onBackClick = { navController.popBackStack() },
                    onInviteClick = { gId ->
                        navController.navigate(Screen.InviteToGroup.createRoute(gId))
                    },
                    onCreatePostClick = { gId ->
                        navController.navigate(Screen.CreatePost.createRoute(gId))
                    },
                    onMapClick = { gId ->
                        navController.navigate(Screen.GroupMap.createRoute(gId))
                    }
                )
            }

            // ===== GROUP MAP SCREEN =====
            composable(
                route = Screen.GroupMap.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: "unknown"
                GroupMapScreen(
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ===== POST SCREENS =====
            composable(
                route = Screen.CreatePost.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: "unknown"
                CreatePostScreen(
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() },
                    onPostCreated = {
                        navController.navigate(Screen.GroupChat.createRoute(groupId)) {
                            popUpTo(Screen.GroupChat.createRoute(groupId)) { inclusive = true }
                        }
                    }
                )
            }

            // ===== INVITE SCREENS =====
            composable(Screen.InvitesList.route) {
                InvitesListScreen(
                    onBackClick = { navController.popBackStack() },
                    onInviteAccepted = {
                        navController.navigate(Screen.Group.route) {
                            popUpTo(Screen.Group.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.InviteToGroup.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: "unknown"
                GroupInviteScreen(
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() },
                    onInviteSent = {
                        navController.popBackStack()
                    }
                )
            }

            // ===== PROFILE & LEADERBOARD =====
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onBackClick = { navController.popBackStack() },
                    onLeaderboardClick = { navController.navigate(Screen.Leaderboard.route) }
                )
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.GroupLeaderboard.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: "unknown"
                LeaderboardScreen(
                    groupId = groupId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // ===== AI ASSISTANT =====
            composable(Screen.AIAssistant.route) {
                AIAssistantScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}