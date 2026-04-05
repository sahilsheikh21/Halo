package com.halo.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.halo.ui.theme.DarkBackground
import com.halo.ui.theme.HaloGradients
import com.halo.ui.theme.HaloPurple
import com.halo.ui.theme.TextPrimary
import com.halo.ui.theme.TextTertiary

/**
 * Halo bottom navigation bar with gradient top border.
 *
 * Features:
 * - 5 tabs: Home, Explore, Create (+), Activity, Messages
 * - Create tab has a larger icon (no label)
 * - Subtle gradient line at the top
 * - Animates in/out based on visibility
 */
@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(modifier = modifier) {
            // Gradient top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HaloGradients.brandHorizontal)
                    .align(Alignment.TopCenter)
            )

            NavigationBar(
                containerColor = DarkBackground.copy(alpha = 0.95f),
                tonalElevation = 0.dp
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val isSelected = currentRoute == tab.route.path
                    val isCreateTab = tab == BottomNavTab.CREATE

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                modifier = if (isCreateTab) {
                                    Modifier.size(32.dp) // Larger create button
                                } else {
                                    Modifier.size(24.dp)
                                }
                            )
                        },
                        label = if (tab.label.isNotEmpty()) {
                            {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        } else null,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isCreateTab) HaloPurple else TextPrimary,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
