package com.halo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halo.ui.theme.DarkSurface
import com.halo.ui.theme.HaloGradients

/**
 * Circular avatar with optional gradient ring (story indicator).
 *
 * @param imageUrl URL of the avatar image
 * @param size Size of the avatar
 * @param showStoryRing Whether to show the gradient story ring
 * @param isSeen Whether all stories have been seen (grey ring)
 * @param ringWidth Width of the gradient ring
 */
@Composable
fun Avatar(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    showStoryRing: Boolean = false,
    isSeen: Boolean = false,
    ringWidth: Dp = 3.dp,
    contentDescription: String? = null
) {
    val ringBrush: Brush? = when {
        showStoryRing && !isSeen -> HaloGradients.storyRing
        showStoryRing && isSeen -> HaloGradients.storyRingSeen
        else -> null
    }

    Box(
        modifier = modifier
            .then(
                if (ringBrush != null) {
                    Modifier
                        .size(size + ringWidth * 2 + 4.dp)
                        .border(
                            width = ringWidth,
                            brush = ringBrush,
                            shape = CircleShape
                        )
                        .padding(ringWidth + 2.dp)
                } else {
                    Modifier.size(size)
                }
            )
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(DarkSurface, CircleShape)
        )
    }
}
