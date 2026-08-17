package com.aiassistant.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aiassistant.ui.theme.EchoTokens
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

val EchoGlassDialogShape = EchoTokens.Radius.shapeXl
val EchoGlassPagePanelShape = EchoTokens.Radius.shapeLg
val EchoGlassControlShape = EchoTokens.Radius.shapePill

data class EchoGlassPalette(
    val panel: Color,
    val panelStrong: Color,
    val panelSoft: Color,
    val control: Color,
    val controlSelected: Color,
    val input: Color,
    val userBubble: Color,
    val assistantBubble: Color,
    val outline: Color,
    val outlineSelected: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val iconPrimary: Color,
    val iconSecondary: Color
)

@Composable
fun echoGlassPalette(): EchoGlassPalette {
    val colors = MaterialTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f

    // 提高非透明度，确保输入框、卡片胶囊与按钮清晰扎实、高对比
    val panelAlpha = if (isDark) 0.88f else 0.92f
    val strongAlpha = if (isDark) 0.94f else 0.96f
    val softAlpha = if (isDark) 0.70f else 0.76f
    val controlAlpha = if (isDark) 0.78f else 0.84f
    val inputAlpha = if (isDark) 0.90f else 0.94f
    val selectedAlpha = if (isDark) 0.88f else 0.92f

    return EchoGlassPalette(
        panel = colors.surface.copy(alpha = panelAlpha),
        panelStrong = colors.surface.copy(alpha = strongAlpha),
        panelSoft = colors.surface.copy(alpha = softAlpha),
        control = colors.surface.copy(alpha = controlAlpha),
        controlSelected = colors.primaryContainer.copy(alpha = selectedAlpha),
        input = colors.surface.copy(alpha = inputAlpha),
        userBubble = if (isDark) {
            colors.primaryContainer.copy(alpha = 0.90f)
        } else {
            Color(0xFFE0F2FE).copy(alpha = 0.94f)
        },
        assistantBubble = Color.Transparent, // 模型回复不使用气泡背景
        outline = if (isDark) Color.White.copy(alpha = 0.12f) else colors.outlineVariant.copy(alpha = 0.18f),
        outlineSelected = colors.primary.copy(alpha = if (isDark) 0.65f else 0.55f),
        textPrimary = colors.onSurface,
        textSecondary = colors.onSurfaceVariant,
        textMuted = colors.onSurfaceVariant.copy(alpha = if (isDark) 0.75f else 0.65f),
        iconPrimary = colors.primary,
        iconSecondary = colors.onSurfaceVariant.copy(alpha = if (isDark) 0.82f else 0.72f)
    )
}

@Composable
fun rememberEchoHazeState(): HazeState = remember { HazeState() }

@Composable
fun Modifier.echoHazeSource(
    hazeState: HazeState
): Modifier = haze(
    state = hazeState,
    style = HazeDefaults.style(
        backgroundColor = MaterialTheme.colorScheme.surface,
        tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f),
        blurRadius = EchoTokens.Glass.blurRadiusStandard,
        noiseFactor = 0f
    )
)

@Composable
fun Modifier.echoHazePanel(
    hazeState: HazeState? = null,
    shape: Shape = EchoTokens.Radius.shapeLg,
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
    blurRadius: Dp = EchoTokens.Glass.blurRadiusStandard,
    highlightAlpha: Float = 0.08f,
    showBorder: Boolean = true
): Modifier {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    var mod = this
        .shadow(
            elevation = EchoTokens.Elevation.subtle,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = if (isDark) 0.10f else 0.03f),
            spotColor = colorScheme.primary.copy(alpha = if (isDark) 0.05f else 0.02f)
        )

    if (hazeState != null) {
        mod = mod.hazeChild(
            state = hazeState,
            shape = shape,
            style = HazeStyle(
                tint = tint,
                blurRadius = blurRadius,
                noiseFactor = 0f
            )
        )
    }

    mod = mod
        .background(tint, shape)
        .clip(shape)
        .drawBehind {
            if (highlightAlpha > 0f) {
                // 在背景层绘制微光折射高光线，绝不覆盖子组件文本与图标
                val highlightColor = Color.White.copy(alpha = if (isDark) highlightAlpha * 0.7f else highlightAlpha)
                drawRoundRect(
                    color = highlightColor,
                    topLeft = Offset(size.width * 0.06f, 1.dp.toPx()),
                    size = Size(size.width * 0.60f, 1.2.dp.toPx()),
                    cornerRadius = CornerRadius(999.dp.toPx(), 999.dp.toPx())
                )
            }
        }

    if (showBorder) {
        mod = mod.border(
            BorderStroke(
                EchoTokens.Glass.borderWidth,
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0.00f to Color.White.copy(alpha = if (isDark) 0.18f else 0.32f),
                        0.40f to colorScheme.outlineVariant.copy(alpha = if (isDark) 0.06f else 0.12f),
                        1.00f to colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.20f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            ),
            shape
        )
    }

    return mod
}

@Composable
fun EchoLiquidGlassPanel(
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier,
    shape: Shape = EchoTokens.Radius.shapeLg,
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
    blurRadius: Dp = EchoTokens.Glass.blurRadiusStandard,
    showBorder: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.echoHazePanel(
            hazeState = hazeState,
            shape = shape,
            tint = tint,
            blurRadius = blurRadius,
            showBorder = showBorder
        ),
        content = content
    )
}

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
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
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
        }
        content()
    }
}

@Composable
fun EchoGlassDialog(
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = EchoTokens.Radius.shapeXl,
    tint: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    title: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    buttons: @Composable ColumnScope.() -> Unit
) {
    val glass = echoGlassPalette()
    val resolvedTint = if (tint != Color.Unspecified) tint else glass.panel
    val resolvedContainerColor = if (containerColor != Color.Unspecified) containerColor else glass.panelStrong
    val resolvedContentColor = if (contentColor != Color.Unspecified) contentColor else glass.textPrimary

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = androidx.compose.ui.platform.LocalView.current
        androidx.compose.runtime.SideEffect {
            var parent = view.parent
            while (parent != null) {
                if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                    parent.window.setBackgroundDrawableResource(android.R.color.transparent)
                    parent.window.setDimAmount(0f)
                    break
                }
                parent = parent.parent
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E293B).copy(alpha = 0.26f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 520.dp)
                    .heightIn(max = 680.dp)
                    .padding(horizontal = 8.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 阻止点击弹窗内容冒泡触发关闭
                    )
                    .echoHazePanel(
                        hazeState = hazeState,
                        shape = shape,
                        tint = resolvedTint,
                        blurRadius = EchoTokens.Glass.blurRadiusHeavy
                    ),
                shape = shape,
                color = resolvedContainerColor,
                contentColor = resolvedContentColor,
                tonalElevation = EchoTokens.Elevation.none,
                shadowElevation = EchoTokens.Elevation.none
            ) {
                Column(
                    modifier = Modifier.padding(EchoTokens.Spacing.lg)
                ) {
                    title()
                    Spacer(modifier = Modifier.height(EchoTokens.Spacing.sm))
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            content()
                        }
                    }
                    Spacer(modifier = Modifier.height(EchoTokens.Spacing.md))
                    buttons()
                }
            }
        }
    }
}

@Composable
fun EchoGlassDialog(
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = EchoTokens.Radius.shapeXl,
    tint: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    icon: (@Composable ColumnScope.() -> Unit)? = null,
    title: @Composable ColumnScope.() -> Unit,
    text: @Composable ColumnScope.() -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    dismissButton: (@Composable RowScope.() -> Unit)? = null
) {
    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = shape,
        tint = tint,
        contentColor = contentColor,
        containerColor = containerColor,
        title = {
            icon?.invoke(this)
            title()
        },
        content = text,
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                dismissButton?.invoke(this)
                Spacer(modifier = Modifier.width(8.dp))
                confirmButton()
            }
        }
    )
}

@Composable
fun EchoGlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: androidx.compose.ui.unit.DpOffset = androidx.compose.ui.unit.DpOffset(0.dp, 0.dp),
    properties: androidx.compose.ui.window.PopupProperties = androidx.compose.ui.window.PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = echoGlassPalette()
    val menuShape = RoundedCornerShape(18.dp)
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            surface = glass.panelStrong,
            surfaceContainer = glass.panelStrong,
            surfaceContainerHigh = glass.panelStrong,
            surfaceContainerHighest = glass.panelStrong
        ),
        shapes = MaterialTheme.shapes.copy(
            extraSmall = menuShape,
            small = menuShape,
            medium = menuShape
        )
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier.border(BorderStroke(1.dp, glass.outline), menuShape),
            offset = offset,
            properties = properties,
            content = content
        )
    }
}
