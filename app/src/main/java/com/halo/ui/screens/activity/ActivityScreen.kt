package com.halo.ui.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DarkSurfaceVariant
import com.halo.ui.theme.DividerColor
import com.halo.ui.theme.HaloCoral
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.HaloTeal
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import com.halo.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.halo.ui.components.EmptyState

@Composable
fun ActivityScreen(
    onProfileClick: (String) -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // Header
        Text(
            text = "Activity",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

        if (activities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Favorite,
                    title = "No activity yet",
                    description = "Interact with posts or follow people to see what's happening.",
                    buttonText = "Explore people and posts",
                    onButtonClick = onNavigateToExplore
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activities, key = { it.id }) { item ->
                    ActivityRow(
                        item = item,
                        onProfileClick = { onProfileClick(item.actorId) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 78.dp)
                            .height(0.5.dp)
                            .background(DividerColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(
    item: ActivityItem,
    onProfileClick: () -> Unit
) {
    val (icon, iconTint) = when (item.type) {
        ActivityType.LIKE -> Pair(Icons.Filled.Favorite, HaloCoral)
        ActivityType.COMMENT -> Pair(Icons.Outlined.ChatBubbleOutline, HaloPurple)
        ActivityType.FOLLOW -> Pair(Icons.Filled.PersonAdd, HaloTeal)
        ActivityType.MENTION -> Pair(Icons.Outlined.AlternateEmail, HaloCoral)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfileClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = item.actorAvatarUrl,
                contentDescription = item.actorName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            )
            // Icon badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(iconTint)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) {
                        append(item.actorName)
                    }
                    append(" ")
                    withStyle(SpanStyle(color = TextSecondary)) {
                        append(item.text)
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTime(item.timestampMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary
            )
        }

        // Post thumbnail if applicable
        if (item.postImageUrl != null) {
            Spacer(modifier = Modifier.width(12.dp))
            AsyncImage(
                model = item.postImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurfaceVariant)
            )
        }
    }
}

private fun formatTime(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        diff < 604_800_000 -> "${diff / 86_400_000}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
