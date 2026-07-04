package com.aiassistant.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
    blurRadius: androidx.compose.ui.unit.Dp = 28.dp
): Modifier = hazeChild(
    state = hazeState,
    shape = shape,
    style = HazeStyle(
        tint = tint,
        blurRadius = blurRadius,
        noiseFactor = 0.06f
    )
)
