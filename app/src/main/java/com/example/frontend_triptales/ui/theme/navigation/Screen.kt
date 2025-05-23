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
    object GroupMap : Screen("group_map/{groupId}", "Mappa Gruppo", Icons.Default.Map) {
        fun createRoute(groupId: String) = "group_map/$groupId"
    }
    object Profile : Screen("profile", "Profilo", Icons.Default.Person)
    object Leaderboard : Screen("leaderboard", "Classifica", Icons.Default.Leaderboard)
    object GroupLeaderboard : Screen("group_leaderboard/{groupId}", "Classifica", Icons.Default.Leaderboard) {
        fun createRoute(groupId: String) = "group_leaderboard/$groupId"
    }
    object InvitesList : Screen("invites_list", "Inviti", Icons.Default.Notifications)

    object InviteToGroup : Screen("invite_to_group/{groupId}", "Invita", Icons.Default.PersonAdd) {
        fun createRoute(groupId: String) = "invite_to_group/$groupId"
    }
    // In Screen.kt, aggiungi questa definizione
    object JoinGroup : Screen("join_group", "Unisciti", Icons.Default.GroupAdd)
    object AIAssistant : Screen("ai_assistant", "Assistente AI", Icons.Default.Assistant)
    object CreatePost : Screen("create_post/{groupId}", "Nuovo Post", Icons.Default.Add) {
        fun createRoute(groupId: String) = "create_post/$groupId"
    }
    object Comments : Screen("comments/{postId}/{postTitle}", "Commenti", Icons.Default.Comment) {
        fun createRoute(postId: String, postTitle: String): String {
            // Encode del titolo per evitare problemi con caratteri speciali
            val encodedTitle = java.net.URLEncoder.encode(postTitle, "UTF-8")
            return "comments/$postId/$encodedTitle"
        }
    }
}
