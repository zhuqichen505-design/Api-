package com.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun EchoGlassBackground(
    modifier: Modifier = Modifier,
    textureAlpha: Float = 0.18f,
    showBlurBands: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val baseBrush = Brush.verticalGradient(
        colors = listOf(
            colors.background,
            colors.surfaceVariant.copy(alpha = 0.38f),
            colors.background
        )
    )
    val lineColor = colors.outlineVariant.copy(alpha = textureAlpha)

    Box(
        modifier = modifier
            .background(baseBrush)
            .drawBehind {
                val step = 28.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += step
                }
            }
    ) {
        if (showBlurBands) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-96).dp, y = 36.dp)
                    .width(540.dp)
                    .height(96.dp)
                    .graphicsLayer { rotationZ = -12f }
                    .blur(24.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 84.dp, y = (-120).dp)
                    .size(width = 480.dp, height = 82.dp)
                    .graphicsLayer { rotationZ = -15f }
                    .blur(26.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.tertiary.copy(alpha = 0.08f),
                                colors.primary.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    color: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            content = content
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            border = border,
            content = content
        )
    }
}
