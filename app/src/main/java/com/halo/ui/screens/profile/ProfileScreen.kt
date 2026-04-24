package com.halo.ui.screens.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.halo.ui.components.Avatar
import com.halo.ui.theme.BorderSubtle
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DarkSurfaceVariant
import com.halo.ui.theme.HaloGradients
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import com.halo.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onBackClick: () -> Unit = {},
    onMessageClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val userProfile by viewModel.observeUser(userId).collectAsState(initial = null)
    val isFollowing = userProfile?.isFollowing == true
    val userPosts by viewModel.getPostsForUser(userId).collectAsState(initial = emptyList())

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ─── Profile header (full width) ──────────────────────────
        item(span = { GridItemSpan(3) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Back button row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
                    }
                    Text(
                        text = userProfile?.username ?: userId.substringAfter("@").substringBefore(":"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // Avatar + stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with story ring
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .border(
                                width = 2.5.dp,
                                brush = HaloGradients.storyRing,
                                shape = CircleShape
                            )
                            .padding(3.dp)
                    ) {
                        AsyncImage(
                            model = userProfile?.avatarUrl,
                            contentDescription = "Profile picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Stats
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatColumn(
                            count = userProfile?.postCount ?: userPosts.size,
                            label = "Posts"
                        )
                        ProfileStatColumn(
                            count = userProfile?.followerCount ?: 0,
                            label = "Followers"
                        )
                        ProfileStatColumn(
                            count = userProfile?.followingCount ?: 0,
                            label = "Following"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bio block
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = userProfile?.displayName ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (userProfile?.bio?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userProfile?.bio ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Follow / Message buttons
                if (userProfile?.isCurrentUser == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Edit Profile", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Share Profile", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                } else {
                    val followBg by animateColorAsState(
                        targetValue = if (isFollowing) Color.Transparent else HaloPurple,
                        animationSpec = tween(200),
                        label = "followBg"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.toggleFollow(userId, isFollowing) },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = followBg,
                                contentColor = if (isFollowing) TextPrimary else Color.White
                            ),
                            border = if (isFollowing) androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle) else null
                        ) {
                            Text(
                                if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        OutlinedButton(
                            onClick = { 
                                viewModel.startDM(userId) { roomId ->
                                    onMessageClick(roomId)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text("Message", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Grid icon header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.GridOn,
                        contentDescription = "Grid",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
            }
        }

        // ─── User posts grid ──────────────────────────────────────
        if (userPosts.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No posts yet", color = TextPrimary, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Share your first moment", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(userPosts) { post ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(DarkSurfaceVariant)
                ) {
                    val imageUrl = post.mediaUrls.firstOrNull()?.url
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                post.caption.take(60),
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when {
                count >= 1_000_000 -> "${count / 1_000_000}M"
                count >= 1_000 -> "${count / 1_000}K"
                else -> count.toString()
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
