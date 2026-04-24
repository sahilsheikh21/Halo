package com.halo.ui.screens.messages

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.halo.domain.model.ChatRoom
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DarkSurface
import com.halo.ui.theme.DarkSurfaceVariant
import com.halo.ui.theme.DividerColor
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import com.halo.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.outlined.Chat
import com.halo.ui.components.EmptyState

@Composable
fun MessageListScreen(
    onChatClick: (String) -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    viewModel: MessageViewModel = hiltViewModel()
) {
    val chatRooms by viewModel.chatRooms.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        // ─── Header ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onNavigateToExplore) {
                Icon(Icons.Default.Edit, contentDescription = "New Message", tint = TextSecondary)
            }
        }

        // ─── Chat list ─────────────────────────────────────────────
        if (chatRooms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.Chat,
                    title = "No messages yet",
                    description = "Start a conversation with someone by searching for their username.",
                    buttonText = "Find people to message",
                    onButtonClick = onNavigateToExplore
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chatRooms, key = { it.roomId }) { room ->
                    ChatRoomRow(room = room, onClick = { onChatClick(room.roomId) })
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 80.dp)
                            .height(0.5.dp)
                            .background(DividerColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatRoomRow(
    room: ChatRoom,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with unread dot
        Box {
            AsyncImage(
                model = room.avatarUrl ?: "https://ui-avatars.com/api/?name=${room.name}&background=1A1A1F&color=8B5CF6",
                contentDescription = room.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            )
            if (room.unreadCount > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(HaloPurple)
                ) {
                    Text(
                        text = if (room.unreadCount > 9) "9+" else room.unreadCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (room.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTime(room.lastMessageAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (room.unreadCount > 0) HaloPurple else TextTertiary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = room.lastMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = if (room.unreadCount > 0) TextSecondary else TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (room.unreadCount > 0) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

private fun formatTime(epochMs: Long): String {
    if (epochMs == 0L) return ""
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 3_600_000 -> "${diff / 60_000}m"
        diff < 86_400_000 -> "${diff / 3_600_000}h"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
