package com.halo.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.halo.ui.theme.BorderSubtle
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.DarkSurfaceVariant
import com.halo.ui.theme.HaloCoral
import com.halo.ui.theme.HaloGold
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextSecondary
import com.halo.ui.theme.TextTertiary

@Composable
fun CreateScreen(
    onPostCreated: () -> Unit = {},
    onDismiss: () -> Unit = {},
    viewModel: CreateViewModel = hiltViewModel()
) {
    val caption          by viewModel.caption.collectAsState()
    val selectedMediaUri by viewModel.selectedMediaUri.collectAsState()
    val location         by viewModel.location.collectAsState()
    val isPosting        by viewModel.isPosting.collectAsState()
    val error            by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // B7: Show a Snackbar whenever the ViewModel emits an upload error.
    // We clear the error state after showing it so it doesn't re-show on
    // recomposition (e.g. after a config change).
    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onMediaSelected(it) }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = HaloCoral,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ─── Header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                }
                Text(
                    text = "New Post",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                // Post button in header
                Button(
                    onClick = { viewModel.createPost { onPostCreated() } },
                    enabled = !isPosting && selectedMediaUri != null,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HaloPurple,
                        disabledContainerColor = HaloPurple.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Share", fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            // ─── Image picker area ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .then(
                        if (selectedMediaUri == null)
                            Modifier
                                .background(DarkSurfaceVariant)
                                .border(1.dp, BorderSubtle)
                        else Modifier
                    )
                    .clickable { galleryLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (selectedMediaUri != null) {
                    AsyncImage(
                        model = selectedMediaUri,
                        contentDescription = "Selected image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Change photo button overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { galleryLauncher.launch("image/*") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Select photo",
                            tint = TextTertiary,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tap to choose a photo",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "or",
                            color = TextTertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { /* camera intent */ },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HaloPurple),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HaloPurple)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Camera")
                        }
                    }
                }
            }

            // ─── Caption field ────────────────────────────────────────
            TextField(
                value = caption,
                onValueChange = viewModel::updateCaption,
                placeholder = { Text("Write a caption…", color = TextTertiary) },
                maxLines = 6,
                shape = RoundedCornerShape(0.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = HaloPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            // ─── Location field ───────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = HaloCoral,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = location.ifBlank { "Add location" },
                    color = if (location.isBlank()) TextTertiary else TextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Story vs Post toggle ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.createStory { onPostCreated() } },
                    enabled = !isPosting && selectedMediaUri != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HaloCoral),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(listOf(HaloGold, HaloCoral))
                    )
                ) {
                    Text("Add to Story", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
