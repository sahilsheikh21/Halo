package com.halo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.halo.data.matrix.MatrixClientManager
import com.halo.data.matrix.SessionState
import com.halo.data.matrix.SlidingSyncManager
import com.halo.data.matrix.SyncEventProcessor
import com.halo.ui.navigation.BottomNavBar
import com.halo.ui.navigation.HaloNavGraph
import com.halo.ui.navigation.Route
import com.halo.ui.screens.auth.AuthViewModel
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.HaloTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var slidingSyncManager: SlidingSyncManager
    @Inject lateinit var syncEventProcessor: SyncEventProcessor

    @Inject lateinit var matrixClientManager: MatrixClientManager
    @Inject lateinit var mockDataSeeder: com.halo.data.util.MockDataSeeder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Seed mock data for first launch
        lifecycleScope.launch {
            mockDataSeeder.seedIfNeeded()
        }

        // Start event processing pipeline (waits until sync is actually running)
        syncEventProcessor.startProcessing(lifecycleScope)

        setContent {
            HaloTheme {
                HaloApp(
                    slidingSyncManager = slidingSyncManager,
                    matrixClientManager = matrixClientManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // SlidingSyncManager.stopSync() is called via sessionState observation in HaloApp
    }
}

@Composable
fun HaloApp(
    slidingSyncManager: SlidingSyncManager,
    matrixClientManager: MatrixClientManager
) {
    val navController = rememberNavController()
    val sessionState by matrixClientManager.sessionState.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val startDestination = Route.Splash.path

    val mainTabs = setOf(
        Route.Home.path,
        Route.Explore.path,
        Route.Create.path,
        Route.Activity.path,
        Route.Messages.path
    )
    val showBottomBar = currentRoute in mainTabs

    val immersiveRoutes = setOf(Route.StoryViewer.path)
    val isImmersive = currentRoute in immersiveRoutes

    // Start/stop Matrix sync based on session state
    LaunchedEffect(sessionState) {
        when (sessionState) {
            is SessionState.LoggedIn -> slidingSyncManager.startSync()
            is SessionState.NotLoggedIn -> slidingSyncManager.stopSync()
            else -> Unit
        }
    }

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
