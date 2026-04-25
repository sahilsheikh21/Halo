package com.halo.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val path: String) {
    data object Splash       : Route("splash")
    data object Login        : Route("login")
    data object Register     : Route("register")
    data object Home         : Route("home")
    data object Explore      : Route("explore")
    data object Create       : Route("create")
    data object Activity     : Route("activity")
    data object Messages     : Route("messages")

    data object Profile : Route("profile/{userId}") {
        // Matrix user IDs look like @user:server — must be URL-encoded
        fun createRoute(userId: String) = "profile/${Uri.encode(userId)}"
    }
    data object Chat : Route("chat/{roomId}") {
        // Matrix room IDs look like !abc123:matrix.org — must be URL-encoded
        fun createRoute(roomId: String) = "chat/${Uri.encode(roomId)}"
    }
    data object StoryViewer : Route("story/{userId}") {
        // Encode for safety — user IDs contain @ and : characters
        fun createRoute(userId: String) = "story/${Uri.encode(userId)}"
    }
}

enum class BottomNavTab(
    val route: Route,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME    (Route.Home,     "Home",     Icons.Filled.Home,              Icons.Outlined.Home),
    EXPLORE (Route.Explore,  "Explore",  Icons.Filled.Search,            Icons.Outlined.Search),
    CREATE  (Route.Create,   "",         Icons.Filled.AddCircleOutline,  Icons.Outlined.AddCircleOutline),
    ACTIVITY(Route.Activity, "Activity", Icons.Filled.FavoriteBorder,    Icons.Outlined.FavoriteBorder),
    MESSAGES(Route.Messages, "Messages", Icons.Filled.ChatBubbleOutline, Icons.Outlined.ChatBubbleOutline),
}
