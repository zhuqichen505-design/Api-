package com.aiassistant.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape

@Composable
fun Modifier.echoShapeClick(
    shape: Shape,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    return this
        .clip(shape)
        .drawWithContent {
            drawContent()
            if (pressed) {
                val overlay = Color.White.copy(alpha = 0.16f)
                when (val outline = shape.createOutline(size, layoutDirection, this)) {
                    is Outline.Rectangle -> drawRect(overlay)
                    is Outline.Rounded -> {
                        val path = Path().apply { addRoundRect(outline.roundRect) }
                        drawPath(path, overlay)
                    }
                    is Outline.Generic -> drawPath(outline.path, overlay)
                }
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun Modifier.echoPlainClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}
