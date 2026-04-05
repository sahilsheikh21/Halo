package com.halo.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Halo signature gradient brushes for use across the app.
 */
object HaloGradients {

    /**
     * The signature Halo gradient: Gold → Coral → Purple
     * Used for: story rings, primary buttons, brand accents
     */
    val storyRing: Brush
        get() = Brush.sweepGradient(
            colors = listOf(
                StoryGradientStart,   // Gold
                HaloCoral,            // Coral
                StoryGradientEnd,     // Purple
                HaloPurpleLight,      // Light Purple
                StoryGradientStart    // Back to Gold (complete the ring)
            )
        )

    /**
     * Linear version of the brand gradient.
     * Used for: buttons, header accents, text gradients
     */
    val brandLinear: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                HaloGold,
                HaloCoral,
                HaloPurple
            )
        )

    /**
     * Horizontal brand gradient.
     * Used for: bottom borders, nav indicators
     */
    val brandHorizontal: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(
                HaloPurple,
                HaloCoral,
                HaloGold
            )
        )

    /**
     * Vertical subtle gradient for card backgrounds.
     * Used for: card overlays, bottom fade on images
     */
    val cardOverlay: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.3f),
                Color.Black.copy(alpha = 0.8f)
            )
        )

    /**
     * Radial glow effect for interactive elements.
     * Used for: like animation glow, button hover states
     */
    val glowPurple: Brush
        get() = Brush.radialGradient(
            colors = listOf(
                HaloPurple.copy(alpha = 0.4f),
                HaloPurple.copy(alpha = 0.1f),
                Color.Transparent
            )
        )

    val glowCoral: Brush
        get() = Brush.radialGradient(
            colors = listOf(
                HaloCoral.copy(alpha = 0.4f),
                HaloCoral.copy(alpha = 0.1f),
                Color.Transparent
            )
        )

    /**
     * Dark surface gradient for screen backgrounds.
     * Adds depth to the pure black background.
     */
    val screenBackground: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                DarkBackground,
                DarkSurface,
                DarkBackground
            )
        )

    /**
     * Shimmer gradient for loading states.
     */
    fun shimmer(translateX: Float): Brush {
        return Brush.linearGradient(
            colors = listOf(
                DarkSurfaceVariant,
                DarkSurfaceElevated,
                DarkSurfaceVariant
            ),
            start = Offset(translateX - 500f, 0f),
            end = Offset(translateX, 0f)
        )
    }

    /**
     * Seen story ring (muted grey).
     */
    val storyRingSeen: Brush
        get() = Brush.sweepGradient(
            colors = listOf(StorySeenRing, StorySeenRing)
        )
}
