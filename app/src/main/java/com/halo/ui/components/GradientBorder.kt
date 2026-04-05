package com.halo.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.halo.ui.theme.HaloGradients

/**
 * Wrapper that applies the Halo signature gradient border to its content.
 *
 * @param brush The gradient brush to use (defaults to brand gradient)
 * @param borderWidth Width of the border
 * @param shape Shape of the border (default: rounded corners)
 * @param innerPadding Padding between border and content
 */
@Composable
fun GradientBorder(
    modifier: Modifier = Modifier,
    brush: Brush = HaloGradients.brandLinear,
    borderWidth: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    innerPadding: Dp = 2.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .border(
                width = borderWidth,
                brush = brush,
                shape = shape
            )
            .padding(innerPadding)
    ) {
        content()
    }
}

/**
 * Circular gradient border variant, perfect for avatars and story rings.
 */
@Composable
fun CircleGradientBorder(
    modifier: Modifier = Modifier,
    brush: Brush = HaloGradients.storyRing,
    borderWidth: Dp = 3.dp,
    innerPadding: Dp = 3.dp,
    content: @Composable () -> Unit
) {
    GradientBorder(
        modifier = modifier,
        brush = brush,
        borderWidth = borderWidth,
        shape = CircleShape,
        innerPadding = innerPadding,
        content = content
    )
}
