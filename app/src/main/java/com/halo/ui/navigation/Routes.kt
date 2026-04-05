package com.halo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the app.
 */
sealed class Route(val path: String) {
    // Splash
    data object Splash : Route("splash")

    // Auth
    data object Login : Route("login")
    data object Register : Route("register")

    // Main tabs
    data object Home : Route("home")
    data object Explore : Route("explore")
    data object Create : Route("create")
    data object Activity : Route("activity")
    data object Messages : Route("messages")

    // Detail screens
    data object Profile : Route("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    data object Chat : Route("chat/{roomId}") {
        fun createRoute(roomId: String) = "chat/$roomId"
    }
    data object StoryViewer : Route("story/{userId}") {
        fun createRoute(userId: String) = "story/$userId"
    }
}

/**
 * Bottom navigation tab definitions.
 */
enum class BottomNavTab(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = Route.Home,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    EXPLORE(
        route = Route.Explore,
        label = "Explore",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    ),
    CREATE(
        route = Route.Create,
        label = "",
        selectedIcon = Icons.Filled.AddCircleOutline,
        unselectedIcon = Icons.Outlined.AddCircleOutline
    ),
    ACTIVITY(
        route = Route.Activity,
        label = "Activity",
        selectedIcon = Icons.Filled.FavoriteBorder,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    ),
    MESSAGES(
        route = Route.Messages,
        label = "Messages",
        selectedIcon = Icons.Filled.ChatBubbleOutline,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline
    )
}
