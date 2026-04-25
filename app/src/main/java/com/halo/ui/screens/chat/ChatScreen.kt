package com.halo.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.halo.domain.model.ChatMessage
import com.halo.domain.model.MessageStatus
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DarkSurface
import com.halo.ui.theme.DarkSurfaceElevated
import com.halo.ui.theme.DarkSurfaceVariant
import com.halo.ui.theme.HaloCoral
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.HaloPurpleDark
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import com.halo.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    roomId: String,
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatRoom by viewModel.getRoomDetails(roomId).collectAsStateWithLifecycle(initialValue = null)
    val roomName = chatRoom?.name ?: "Chat"
    val avatarUrl = chatRoom?.avatarUrl

    var inputText by remember { mutableStateOf("") }
    val messages by viewModel.getRoomTimeline(roomId).collectAsStateWithLifecycle(initialValue = emptyList())
    val listState = rememberLazyListState()

    // Scroll to bottom whenever a new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .imePadding()
    ) {
        // ─── Header ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            AsyncImage(
                model = avatarUrl
                    ?: "https://ui-avatars.com/api/?name=$roomName&background=1A1A1F&color=8B5CF6",
                contentDescription = roomName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = roomName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Active now",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }

        // ─── Messages ─────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onRetry = {
                        viewModel.retryMessage(
                            roomId    = roomId,
                            messageId = message.id,
                            body      = message.body
                        )
                    }
                )
            }
        }

        // ─── Input bar ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.CameraAlt,
                    "Camera",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Image,
                    "Gallery",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message…", color = TextTertiary, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    cursorColor             = HaloPurple
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(roomId, inputText.trim())
                        inputText = ""
                    }
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    "Send",
                    tint = if (inputText.isNotBlank()) HaloPurple else TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ─── Message bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val isFailed  = message.status == MessageStatus.FAILED
    val isSending = message.status == MessageStatus.SENDING

    val alignment  = if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor    = when {
        isFailed     -> HaloCoral.copy(alpha = 0.25f)          // red tint for failed
        message.isMe -> HaloPurpleDark
        else         -> DarkSurfaceElevated
    }
    val textColor  = if (message.isMe) Color.White else TextPrimary
    val bubbleShape = if (message.isMe) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start
        ) {
            // B13: Show sender ID above bubbles for messages from other people.
            // In a 1-to-1 DM the room name already identifies the sender, so this
            // is most useful in group rooms. We show the local-part of the Matrix ID
            // (everything before the first ':') to keep the label short.
            if (!message.isMe && message.senderId.isNotBlank()) {
                Text(
                    text  = senderDisplayName(message.senderId),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            // Bubble itself — tappable only when FAILED (triggers retry)
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(bgColor, bubbleShape)
                    .then(
                        if (isFailed) Modifier.clickable(onClick = onRetry) else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text  = message.body,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = if (isFailed) HaloCoral else textColor,
                    modifier = Modifier.padding(
                        end = if (isFailed) 24.dp else 0.dp  // leave room for the error icon
                    )
                )

                // B3: Error icon inside the bubble for FAILED messages
                if (isFailed) {
                    Icon(
                        imageVector        = Icons.Default.ErrorOutline,
                        contentDescription = "Send failed — tap to retry",
                        tint               = HaloCoral,
                        modifier           = Modifier
                            .size(18.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }

            // Timestamp / status row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text  = when {
                        isFailed  -> "Failed · Tap to retry"
                        isSending -> "Sending…"
                        else      -> formatMsgTime(message.timestamp)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFailed) HaloCoral else TextTertiary
                )
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Returns a human-friendly label for a Matrix user ID.
 *
 * "@alice:matrix.org" → "alice"
 * "alice"             → "alice"  (already short)
 */
private fun senderDisplayName(matrixUserId: String): String {
    val localPart = matrixUserId
        .removePrefix("@")
        .substringBefore(":")
    return localPart.ifBlank { matrixUserId }
}

private fun formatMsgTime(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    return when {
        diff < 60_000      -> "now"
        diff < 3_600_000   -> "${diff / 60_000}m"
        diff < 86_400_000  -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMs))
        else               -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMs))
    }
}
