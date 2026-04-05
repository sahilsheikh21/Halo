package com.halo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.halo.data.matrix.SessionState
import com.halo.ui.navigation.BottomNavBar
import com.halo.ui.navigation.BottomNavTab
import com.halo.ui.navigation.HaloNavGraph
import com.halo.ui.navigation.Route
import com.halo.ui.screens.auth.AuthViewModel
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.HaloTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HaloTheme {
                HaloApp()
            }
        }
    }
}

@Composable
fun HaloApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val sessionState by authViewModel.sessionState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Always start at splash
    val startDestination = Route.Splash.path

    // Routes that show the bottom nav bar
    val mainTabs = setOf(
        Route.Home.path,
        Route.Explore.path,
        Route.Create.path,
        Route.Activity.path,
        Route.Messages.path
    )
    val showBottomBar = currentRoute in mainTabs

    // Routes where the system nav bar should be hidden (immersive)
    val immersiveRoutes = setOf(Route.StoryViewer.path)
    val isImmersive = currentRoute in immersiveRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavBar(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    navController.navigate(tab.route.path) {
                        popUpTo(Route.Home.path) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                isVisible = showBottomBar
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        HaloNavGraph(
            navController = navController,
            startDestination = startDestination,
            modifier = if (isImmersive) Modifier else Modifier.padding(innerPadding)
        )
    }
}
