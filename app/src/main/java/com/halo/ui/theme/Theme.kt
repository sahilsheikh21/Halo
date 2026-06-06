package com.halo.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Safely resolves the root [Activity] from a potentially wrapped [Context].
 * Returns null if the context chain does not contain an Activity
 * (e.g. in Compose Previews or Hilt's FragmentContextWrapper).
 */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private val HaloDarkColorScheme = darkColorScheme(
    // Primary brand
    primary = HaloPurple,
    onPrimary = Color.White,
    primaryContainer = HaloPurpleDark,
    onPrimaryContainer = HaloPurpleLight,

    // Secondary (coral accent)
    secondary = HaloCoral,
    onSecondary = Color.White,
    secondaryContainer = HaloCoralDark,
    onSecondaryContainer = HaloCoralLight,

    // Tertiary (gold accent)
    tertiary = HaloGold,
    onTertiary = Color.Black,
    tertiaryContainer = HaloGoldDark,
    onTertiaryContainer = HaloGoldLight,

    // Background & Surface
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    // Error
    error = ErrorRed,
    onError = Color.White,

    // Outline
    outline = BorderSubtle,
    outlineVariant = DividerColor,

    // Inverse (for snackbars, etc.)
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = HaloPurpleDark
)

@Composable
fun HaloTheme(
    useDynamicColor: Boolean = false, // Set true to use Material You colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(LocalContext.current)
        }
        else -> HaloDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                val window = activity.window
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HaloTypography,
        content = content
    )
}
