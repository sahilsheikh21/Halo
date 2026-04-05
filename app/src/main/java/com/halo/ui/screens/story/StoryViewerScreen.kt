package com.halo.ui.screens.story

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.halo.data.mock.MockData
import com.halo.domain.model.Story
import com.halo.domain.model.StoryGroup
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen story viewer with:
 * - Segmented progress bar at top
 * - Tap left/right to go prev/next
 * - Auto-advance timer
 * - Author header overlay
 */
@Composable
fun StoryViewerScreen(
    userId: String,
    onClose: () -> Unit
) {
    // Find the story group for the given user
    val storyGroup = MockData.storyGroups.find { it.authorId == userId }
    val stories = storyGroup?.stories ?: emptyList()

    if (stories.isEmpty()) {
        // No stories — show placeholder and auto-close
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No stories to show", color = TextSecondary)
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1000)
            onClose()
        }
        return
    }

    // storyGroup is guaranteed non-null here (we returned above if stories is empty)
    val group = storyGroup!!

    var currentIndex by remember { mutableIntStateOf(0) }
    val story = stories[currentIndex]

    // Progress animation (5s per story)
    val progress = remember(currentIndex) { Animatable(0f) }
    LaunchedEffect(currentIndex) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = story.durationMs.toInt(),
                easing = LinearEasing
            )
        )
        // Auto advance when progress completes
        if (currentIndex < stories.lastIndex) {
            currentIndex++
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onTap = { offset ->
                        val screenWidth = size.width
                        if (offset.x < screenWidth / 3f) {
                            // Tap left — previous
                            if (currentIndex > 0) currentIndex--
                        } else {
                            // Tap right — next
                            if (currentIndex < stories.lastIndex) {
                                currentIndex++
                            } else {
                                onClose()
                            }
                        }
                    }
                )
            }
    ) {
        // ─── Full-screen story image ──────────────────────────────
        AsyncImage(
            model = story.mediaUrl,
            contentDescription = "Story",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // ─── Top gradient overlay ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
        )

        // ─── Bottom gradient overlay ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
                .align(Alignment.BottomCenter)
        )

        // ─── Progress bars + header ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // Segmented progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress.value
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Author header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = group.authorAvatarUrl,
                    contentDescription = group.authorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.DarkGray, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = group.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatStoryTime(story.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // ─── Caption overlay at bottom ────────────────────────────
        if (!story.caption.isNullOrBlank()) {
            Text(
                text = story.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            )
        }
    }
}

private fun formatStoryTime(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
