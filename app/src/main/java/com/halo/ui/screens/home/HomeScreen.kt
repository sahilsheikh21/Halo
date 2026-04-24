package com.halo.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.halo.ui.components.AddStoryItem
import com.halo.ui.components.CommentSheet
import com.halo.ui.components.PostCard
import com.halo.ui.components.ShimmerPostCard
import com.halo.ui.components.ShimmerStoryItem
import com.halo.ui.components.StoryItem
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DividerColor
import com.halo.ui.theme.HaloCoral
import com.halo.ui.theme.HaloGold
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextSecondary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

import androidx.compose.material.icons.outlined.People
import com.halo.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStoryClick: (String) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val posts by viewModel.feedPosts.collectAsState()
    val storyGroups by viewModel.storyGroups.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var commentPostId by remember { mutableStateOf<String?>(null) }

    // Trigger initial data load
    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ─── Offline banner ───────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = !isOnline,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(color = Color(0xFFB00020)) {
                        Text(
                            text = "You're offline — showing cached content",
                            color = Color.White,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp)
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Halo",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            brush = Brush.linearGradient(
                                colors = listOf(HaloGold, HaloCoral, HaloPurple)
                            )
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Activity",
                            tint = TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.Send,
                            contentDescription = "Messages",
                            tint = TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // ─── Story bar ────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    item {
                        AddStoryItem(
                            avatarUrl = null,
                            onClick = { onStoryClick("me") }
                        )
                    }
                    if (storyGroups.isEmpty()) {
                        items(5) { ShimmerStoryItem() }
                    } else {
                        items(storyGroups) { group ->
                            StoryItem(
                                storyGroup = group,
                                onClick = { onStoryClick(group.authorId) }
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(DividerColor)
                )
            }

            // ─── Feed ─────────────────────────────────────────────
            if (posts.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.People,
                        title = "Your feed is empty",
                        description = "Follow people to see their latest posts and stories here.",
                        buttonText = "Connect with other people",
                        onButtonClick = onNavigateToExplore,
                        modifier = Modifier.padding(top = 40.dp)
                    )
                }
            } else {
                items(posts, key = { it.eventId }) { post ->
                    PostCard(
                        post = post,
                        onLikeClick = { viewModel.toggleLike(it) },
                        onProfileClick = onProfileClick,
                        onCommentClick = {
                            commentPostId = it
                            onCommentClick(it)
                        }
                    )
                }
            }
        }

        // Refresh spinner
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .size(28.dp),
                color = HaloPurple,
                strokeWidth = 2.5.dp
            )
        }

        // Comment Sheet
        if (commentPostId != null) {
            CommentSheet(
                postId = commentPostId!!,
                onDismiss = { commentPostId = null }
            )
        }
    }
}
