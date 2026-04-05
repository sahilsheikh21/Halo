package com.halo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.halo.domain.model.StoryGroup
import com.halo.ui.theme.DarkSurface
import com.halo.ui.theme.DarkSurfaceVariant
import com.halo.ui.theme.HaloGradients
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.StorySeenRing
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary

/**
 * Story ring item in the home screen horizontal strip.
 * Shows gradient ring for unseen stories, grey for seen.
 */
@Composable
fun StoryItem(
    storyGroup: StoryGroup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringBrush = if (storyGroup.hasUnseenStories) {
        HaloGradients.storyRing
    } else {
        HaloGradients.storyRingSeen
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Gradient ring border
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .border(
                        width = if (storyGroup.hasUnseenStories) 2.5.dp else 1.5.dp,
                        brush = ringBrush,
                        shape = CircleShape
                    )
            )

            // Avatar image
            AsyncImage(
                model = storyGroup.authorAvatarUrl,
                contentDescription = storyGroup.authorName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(DarkSurface, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = storyGroup.authorName.take(10),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (storyGroup.hasUnseenStories) TextPrimary else TextSecondary,
            fontWeight = if (storyGroup.hasUnseenStories) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * "Your Story" add button — first item in the strip.
 */
@Composable
fun AddStoryItem(
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Avatar
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Your story",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant, CircleShape)
            )

            // Plus badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(HaloPurple)
                    .border(2.dp, Color(0xFF0A0A0B), CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add story",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = "Your Story",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondary,
            maxLines = 1
        )
    }
}
