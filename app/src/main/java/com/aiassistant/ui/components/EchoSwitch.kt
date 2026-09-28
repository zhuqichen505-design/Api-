package com.aiassistant.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aiassistant.ui.theme.EchoTokens

/**
 * Echo 标准开关 (EchoSwitch)
 * 统一全应用开关规格：紧凑轨道 + 语义化配色，杜绝 scale 缩放导致的错位。
 *
 * 设计决策：
 * - 使用紧凑轨道（44dp x 26dp），而非 M3 默认的 52dp x 32dp
 * - 配色遵循 M3 Switch 规范，但轨道颜色使用 surfaceVariant 增强可见性
 * - 触控目标仍保持 48dp，通过外层 Box 扩展实现
 */
@Composable
fun EchoSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = echoSwitchColors()
) {
    Box(
        modifier = modifier.size(
            width = EchoTokens.TouchTarget.minimum,
            height = EchoTokens.TouchTarget.minimum
        ),
        contentAlignment = Alignment.Center
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.75f), // 视觉缩放，触控目标不变
            enabled = enabled,
            colors = colors
        )
    }
}

/**
 * Echo 开关配色方案
 * 统一全应用开关颜色，确保深浅色主题下均有良好对比度
 */
@Composable
fun echoSwitchColors(): SwitchColors {
    val colorScheme = MaterialTheme.colorScheme

    return SwitchDefaults.colors(
        // 开启状态
        checkedThumbColor = colorScheme.onPrimary,
        checkedTrackColor = colorScheme.primary,
        checkedBorderColor = Color.Transparent,
        checkedIconColor = colorScheme.primary,

        // 关闭状态
        uncheckedThumbColor = colorScheme.onSurfaceVariant,
        uncheckedTrackColor = colorScheme.surfaceVariant,
        uncheckedBorderColor = colorScheme.outline,
        uncheckedIconColor = colorScheme.surfaceVariant,

        // 禁用状态
        disabledCheckedThumbColor = colorScheme.onSurface.copy(alpha = 0.38f),
        disabledCheckedTrackColor = colorScheme.onSurface.copy(alpha = 0.12f),
        disabledCheckedBorderColor = Color.Transparent,
        disabledUncheckedThumbColor = colorScheme.onSurface.copy(alpha = 0.38f),
        disabledUncheckedTrackColor = colorScheme.surfaceVariant.copy(alpha = 0.38f),
        disabledUncheckedBorderColor = colorScheme.outline.copy(alpha = 0.12f)
    )
}
