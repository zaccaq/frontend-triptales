package com.example.frontend_triptales.ui.theme.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

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
    object Profile : Screen("profile", "Profilo", Icons.Default.Person)
    object Leaderboard : Screen("leaderboard", "Classifica", Icons.Default.Leaderboard)
    object GroupLeaderboard : Screen("group_leaderboard/{groupId}", "Classifica", Icons.Default.Leaderboard) {
        fun createRoute(groupId: String) = "group_leaderboard/$groupId"
    }
}
