package com.aiassistant.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

private val ReadableDark = Color.Black
private val ReadableLight = Color(0xFFFFFFFF)

fun readableTextColorFor(
    background: Color,
    fallbackSurface: Color = Color.White
): Color {
    val resolvedBackground = if (background.alpha < 1f) {
        background.compositeOver(fallbackSurface)
    } else {
        background
    }
    return if (contrastRatio(ReadableDark, resolvedBackground) >= contrastRatio(ReadableLight, resolvedBackground)) {
        ReadableDark
    } else {
        ReadableLight
    }
}

fun readableTextColorFor(
    background: Color,
    darkColor: Color,
    lightColor: Color,
    fallbackSurface: Color = Color.White
): Color {
    val resolvedBackground = if (background.alpha < 1f) {
        background.compositeOver(fallbackSurface)
    } else {
        background
    }
    return if (contrastRatio(darkColor, resolvedBackground) >= contrastRatio(lightColor, resolvedBackground)) {
        darkColor
    } else {
        lightColor
    }
}

private fun contrastRatio(foreground: Color, background: Color): Float {
    val lighter = maxOf(foreground.luminance(), background.luminance())
    val darker = minOf(foreground.luminance(), background.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
