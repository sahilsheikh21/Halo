package com.halo.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.halo.ui.screens.activity.ActivityScreen
import com.halo.ui.screens.auth.LoginScreen
import com.halo.ui.screens.auth.RegisterScreen
import com.halo.ui.screens.chat.ChatScreen
import com.halo.ui.screens.create.CreateScreen
import com.halo.ui.screens.explore.ExploreScreen
import com.halo.ui.screens.home.HomeScreen
import com.halo.ui.screens.messages.MessageListScreen
import com.halo.ui.screens.profile.ProfileScreen
import com.halo.ui.screens.splash.SplashScreen
import com.halo.ui.screens.story.StoryViewerScreen

@Composable
fun HaloNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        // ─── Splash ───────────────────────────────────
        composable(
            route = Route.Splash.path,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut(tween(300)) }
        ) {
            SplashScreen(
                onAlreadyLoggedIn = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                },
                onNeedsLogin = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            )
        }

        // ─── Auth ────────────────────────────────────
        composable(
            route = Route.Login.path,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Route.Register.path)
                }
            )
        }

        composable(
            route = Route.Register.path,
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ─── Main Tabs ──────────────────────────────
        composable(Route.Home.path) {
            HomeScreen(
                onStoryClick = { userId ->
                    navController.navigate(Route.StoryViewer.createRoute(userId))
                },
                onProfileClick = { userId ->
                    navController.navigate(Route.Profile.createRoute(userId))
                }
            )
        }

        composable(Route.Explore.path) {
            ExploreScreen(
                onUserClick = { userId ->
                    navController.navigate(Route.Profile.createRoute(userId))
                }
            )
        }

        composable(Route.Create.path) {
            CreateScreen(
                onPostCreated = {
                    navController.popBackStack()
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.Activity.path) {
            ActivityScreen(
                onProfileClick = { userId ->
                    navController.navigate(Route.Profile.createRoute(userId))
                }
            )
        }

        composable(Route.Messages.path) {
            MessageListScreen(
                onChatClick = { roomId ->
                    navController.navigate(Route.Chat.createRoute(roomId))
                }
            )
        }

        // ─── Detail Screens ─────────────────────────
        composable(
            route = Route.Profile.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ProfileScreen(
                userId = userId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Chat ────────────────────────────────────
        composable(
            route = Route.Chat.path,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            ChatScreen(
                roomId = roomId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ─── Story Viewer ────────────────────────────
        composable(
            route = Route.StoryViewer.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            StoryViewerScreen(
                userId = userId,
                onClose = { navController.popBackStack() }
            )
        }
    }
}
