package com.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@Composable
fun rememberEchoHazeState(): HazeState = remember { HazeState() }

@Composable
fun Modifier.echoHazeSource(
    hazeState: HazeState
): Modifier = haze(
    state = hazeState,
    style = HazeDefaults.style(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.54f),
        blurRadius = 26.dp,
        noiseFactor = 0.08f
    )
)

@Composable
fun Modifier.echoHazePanel(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
    blurRadius: Dp = 28.dp
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val liquidTint = tint.copy(alpha = tint.alpha.coerceAtLeast(0.34f))
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = colorScheme.primary.copy(alpha = 0.10f),
            spotColor = colorScheme.primary.copy(alpha = 0.16f)
        )
        .hazeChild(
            state = hazeState,
            shape = shape,
            style = HazeStyle(
                tint = liquidTint,
                blurRadius = blurRadius,
                noiseFactor = 0.10f
            )
        )
        .echoLiquidGlassOverlay(
            shape = shape,
            accent = colorScheme.primary,
            surface = colorScheme.surface,
            outline = colorScheme.outlineVariant
        )
}

@Composable
fun EchoLiquidGlassPanel(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
    blurRadius: Dp = 30.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.echoHazePanel(
            hazeState = hazeState,
            shape = shape,
            tint = tint,
            blurRadius = blurRadius
        ),
        content = content
    )
}

fun Modifier.echoLiquidGlassOverlay(
    shape: Shape,
    accent: Color,
    surface: Color,
    outline: Color
): Modifier = this
    .clip(shape)
    .drawWithCache {
        val topRefraction = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.34f),
                0.28f to Color.White.copy(alpha = 0.12f),
                0.62f to surface.copy(alpha = 0.05f),
                1.00f to accent.copy(alpha = 0.10f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        val sideGlow = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.24f),
                0.42f to accent.copy(alpha = 0.10f),
                1.00f to Color.Transparent
            ),
            center = Offset(size.width * 0.18f, size.height * 0.08f),
            radius = size.maxDimension * 0.92f
        )
        val lowerShade = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.70f to Color.Transparent,
                1.00f to accent.copy(alpha = 0.10f)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height)
        )

        onDrawWithContent {
            drawContent()
            drawRect(topRefraction, blendMode = BlendMode.Screen)
            drawRect(sideGlow, blendMode = BlendMode.Screen)
            drawRect(lowerShade, blendMode = BlendMode.Multiply)
            drawRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(size.width * 0.08f, 1.1.dp.toPx()),
                size = Size(size.width * 0.56f, 1.25.dp.toPx()),
                blendMode = BlendMode.Screen
            )
            drawRect(
                color = outline.copy(alpha = 0.24f),
                topLeft = Offset(size.width * 0.12f, size.height - 1.2.dp.toPx()),
                size = Size(size.width * 0.76f, 1.dp.toPx())
            )
        }
    }
    .border(
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.70f),
                    0.34f to Color.White.copy(alpha = 0.18f),
                    0.70f to outline.copy(alpha = 0.28f),
                    1.00f to accent.copy(alpha = 0.24f)
                )
            )
        ),
        shape
    )
