package com.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SelectableChipElevation
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun echoFilterChipColors(): SelectableChipColors {
    val glass = echoGlassPalette()
    return FilterChipDefaults.filterChipColors(
        containerColor = glass.control,
        labelColor = glass.textSecondary,
        iconColor = glass.iconSecondary,
        selectedContainerColor = glass.controlSelected,
        selectedLabelColor = MaterialTheme.colorScheme.primary,
        selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
        selectedTrailingIconColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = glass.control.copy(alpha = 0.42f),
        disabledLabelColor = glass.textMuted.copy(alpha = 0.56f),
        disabledLeadingIconColor = glass.iconSecondary.copy(alpha = 0.44f),
        disabledTrailingIconColor = glass.iconSecondary.copy(alpha = 0.44f)
    )
}

@Composable
fun echoFilterChipBorder(selected: Boolean): BorderStroke {
    val glass = echoGlassPalette()
    return BorderStroke(
        width = if (selected) 1.4.dp else 1.dp,
        color = if (selected) glass.outlineSelected else glass.outline
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun echoFilterChipElevation(): SelectableChipElevation =
    FilterChipDefaults.filterChipElevation(
        elevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        draggedElevation = 0.dp,
        disabledElevation = 0.dp
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun echoSegmentedButtonColors(): SegmentedButtonColors {
    val glass = echoGlassPalette()
    return SegmentedButtonDefaults.colors(
        activeContainerColor = glass.controlSelected,
        activeContentColor = MaterialTheme.colorScheme.primary,
        inactiveContainerColor = glass.control,
        inactiveContentColor = glass.textSecondary,
        disabledActiveContainerColor = glass.controlSelected.copy(alpha = 0.44f),
        disabledActiveContentColor = glass.textMuted.copy(alpha = 0.52f),
        disabledInactiveContainerColor = glass.control.copy(alpha = 0.36f),
        disabledInactiveContentColor = glass.textMuted.copy(alpha = 0.46f)
    )
}

@Composable
fun echoSegmentedButtonBorder(selected: Boolean): BorderStroke {
    val glass = echoGlassPalette()
    return BorderStroke(
        width = if (selected) 1.3.dp else 1.dp,
        color = if (selected) glass.outlineSelected else glass.outline
    )
}

val EchoCompactButtonPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)

@Composable
fun Color.echoReadableOn(fallbackSurface: Color = MaterialTheme.colorScheme.background): Color =
    readableTextColorFor(background = this, fallbackSurface = fallbackSurface)
