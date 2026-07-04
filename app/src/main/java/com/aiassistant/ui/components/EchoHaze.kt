package com.aiassistant.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

val EchoGlassDialogShape = RoundedCornerShape(30.dp)
val EchoGlassPagePanelShape = RoundedCornerShape(28.dp)

@Composable
fun rememberEchoHazeState(): HazeState = remember { HazeState() }

@Composable
fun Modifier.echoHazeSource(
    hazeState: HazeState
): Modifier = haze(
    state = hazeState,
    style = HazeDefaults.style(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        blurRadius = 22.dp,
        noiseFactor = 0.018f
    )
)

@Composable
fun Modifier.echoHazePanel(
    hazeState: HazeState,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
    blurRadius: Dp = 22.dp
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val liquidTint = tint.copy(alpha = tint.alpha.coerceIn(0.22f, 0.55f))
    return this
        .shadow(
            elevation = 8.dp,
            shape = shape,
            ambientColor = colorScheme.primary.copy(alpha = 0.07f),
            spotColor = colorScheme.primary.copy(alpha = 0.11f)
        )
        .hazeChild(
            state = hazeState,
            shape = shape,
            style = HazeStyle(
                tint = liquidTint,
                blurRadius = blurRadius,
                noiseFactor = 0.018f
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
    tint: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
    blurRadius: Dp = 22.dp,
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
                0.00f to Color.White.copy(alpha = 0.08f),
                0.30f to surface.copy(alpha = 0.05f),
                0.62f to accent.copy(alpha = 0.045f),
                1.00f to accent.copy(alpha = 0.075f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
        val sideGlow = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.White.copy(alpha = 0.06f),
                0.42f to accent.copy(alpha = 0.055f),
                1.00f to Color.Transparent
            ),
            center = Offset(size.width * 0.18f, size.height * 0.08f),
            radius = size.maxDimension * 0.92f
        )
        val lowerShade = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to Color.Transparent,
                0.70f to Color.Transparent,
                1.00f to accent.copy(alpha = 0.065f)
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height)
        )

        onDrawWithContent {
            drawContent()
            drawRect(topRefraction, blendMode = BlendMode.Screen)
            drawRect(sideGlow, blendMode = BlendMode.Screen)
            drawRect(lowerShade, blendMode = BlendMode.Multiply)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.07f),
                topLeft = Offset(size.width * 0.08f, 1.1.dp.toPx()),
                size = Size(size.width * 0.56f, 1.25.dp.toPx()),
                cornerRadius = CornerRadius(999.dp.toPx(), 999.dp.toPx()),
                blendMode = BlendMode.Screen
            )
            drawRoundRect(
                color = outline.copy(alpha = 0.14f),
                topLeft = Offset(size.width * 0.12f, size.height - 1.2.dp.toPx()),
                size = Size(size.width * 0.76f, 1.dp.toPx()),
                cornerRadius = CornerRadius(999.dp.toPx(), 999.dp.toPx())
            )
        }
    }
    .border(
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                colorStops = arrayOf(
                    0.00f to Color.White.copy(alpha = 0.26f),
                    0.34f to Color.White.copy(alpha = 0.07f),
                    0.70f to outline.copy(alpha = 0.18f),
                    1.00f to accent.copy(alpha = 0.18f)
                )
            )
        ),
        shape
    )

@Composable
fun EchoWallpaperBackground(
    backgroundBitmap: Bitmap?,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .echoHazeSource(hazeState)
    ) {
        backgroundBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}

@Composable
fun EchoGlassDialog(
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = EchoGlassDialogShape,
    title: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(horizontal = 18.dp)
                .echoHazePanel(
                    hazeState = hazeState,
                    shape = shape,
                    tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                    blurRadius = 28.dp
                ),
            shape = shape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                title()
                content()
                buttons()
            }
        }
    }
}
