package com.halo.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.halo.data.matrix.SessionState
import com.halo.ui.screens.auth.AuthViewModel
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.HaloCoral
import com.halo.ui.theme.HaloGold
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash screen with the Halo logo scaling in.
 *
 * During the animation the [AuthViewModel] attempts to restore a previous
 * session from encrypted storage.  When the animation completes (≥ 1.8 s)
 * one of two callbacks fires:
 *  - [onAlreadyLoggedIn] — valid session was restored → navigate to Home
 *  - [onNeedsLogin]      — no session or expired token → navigate to Login
 */
@Composable
fun SplashScreen(
    onAlreadyLoggedIn: () -> Unit,
    onNeedsLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val sessionState by viewModel.sessionState.collectAsState()

    val scale        = remember { Animatable(0.4f) }
    val alpha        = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    // Kick off session restore + animation in parallel
    LaunchedEffect(Unit) {
        // Logo scale-in
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = EaseOutBack)
            )
        }
        // Logo fade-in
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(500, easing = EaseOutCubic)
            )
        }
        // Tagline delayed fade-in
        launch {
            delay(400)
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(400)
            )
        }

        // Minimum display time — give the session restore time to finish
        delay(1800)

        when (sessionState) {
            is SessionState.LoggedIn -> onAlreadyLoggedIn()
            else                     -> onNeedsLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Text(
                text = "Halo",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.linearGradient(
                        colors = listOf(HaloGold, HaloCoral, HaloPurple)
                    )
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Share your world",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
