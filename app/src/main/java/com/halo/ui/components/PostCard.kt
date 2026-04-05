package com.halo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.halo.domain.model.Post
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DarkSurface
import com.halo.ui.theme.DividerColor
import com.halo.ui.theme.HaloCoral
import com.halo.ui.theme.HaloGradients
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import com.halo.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full Instagram-style post card.
 * Features: double-tap to like, image carousel/pager, like animation, actions row.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    onLikeClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onShareClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onBookmarkClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLiked by remember(post.eventId) { mutableStateOf(post.isLikedByMe) }
    var likeCount by remember(post.eventId) { mutableStateOf(post.likeCount) }
    var isBookmarked by remember { mutableStateOf(false) }
    var showHeartAnimation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Heart animation scale
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartAnimation) 1.2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "heartScale"
    )
    val heartAlpha by animateFloatAsState(
        targetValue = if (showHeartAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = if (showHeartAnimation) 100 else 400),
        label = "heartAlpha"
    )

    // Like icon color
    val likeIconColor by animateColorAsState(
        targetValue = if (isLiked) HaloCoral else TextSecondary,
        animationSpec = tween(200),
        label = "likeColor"
    )

    val pagerState = rememberPagerState(pageCount = { maxOf(1, post.mediaUrls.size) })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
    ) {
        // ─── Header ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                imageUrl = post.authorAvatarUrl,
                size = 36.dp,
                showStoryRing = false,
                modifier = Modifier.clickable { onProfileClick(post.authorId) }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.authorName.ifBlank { post.authorId.substringAfter("@").substringBefore(":") },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                if (!post.locationName.isNullOrBlank()) {
                    Text(
                        text = post.locationName,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextSecondary)
            }
        }

        // ─── Media ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            if (post.mediaUrls.isEmpty()) {
                // Text-only post
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val mediaItem = post.mediaUrls[page]
                    AsyncImage(
                        model = mediaItem.url,
                        contentDescription = "Post image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(post.eventId) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (!isLiked) {
                                            isLiked = true
                                            likeCount++
                                            onLikeClick(post.eventId)
                                        }
                                        scope.launch {
                                            showHeartAnimation = true
                                            delay(800)
                                            showHeartAnimation = false
                                        }
                                    }
                                )
                            }
                    )
                }

                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(HaloGradients.cardOverlay)
                        .align(Alignment.BottomCenter)
                )

                // Floating heart animation on double-tap
                if (heartScale > 0f) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = heartAlpha),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(90.dp)
                            .scale(heartScale)
                    )
                }

                // Page indicators for carousel
                if (post.mediaUrls.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        repeat(post.mediaUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == pagerState.currentPage) Color.White
                                        else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // ─── Actions ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like
            IconButton(
                onClick = {
                    isLiked = !isLiked
                    likeCount += if (isLiked) 1 else -1
                    if (isLiked) onLikeClick(post.eventId)
                }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = likeIconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Comment
            IconButton(onClick = { onCommentClick(post.eventId) }) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = TextSecondary,
                    modifier = Modifier.size(25.dp)
                )
            }

            // Share
            IconButton(onClick = { onShareClick(post.eventId) }) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(25.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bookmark
            IconButton(
                onClick = {
                    isBookmarked = !isBookmarked
                    onBookmarkClick(post.eventId)
                }
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) HaloPurple else TextSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // ─── Like count ──────────────────────────────────────────
        if (likeCount > 0) {
            Text(
                text = "$likeCount ${if (likeCount == 1) "like" else "likes"}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ─── Caption ─────────────────────────────────────────────
        if (post.caption.isNotBlank()) {
            val authorShort = post.authorName.ifBlank {
                post.authorId.substringAfter("@").substringBefore(":")
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) {
                        append(authorShort)
                    }
                    append("  ")
                    withStyle(SpanStyle(color = TextPrimary)) {
                        append(post.caption)
                    }
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ─── Comment count + timestamp ────────────────────────────
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (post.commentCount > 0) {
                Text(
                    text = "View all ${post.commentCount} comments",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Text(
            text = formatTimestamp(post.createdAt),
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DividerColor)
        )
    }
}

private fun formatTimestamp(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
