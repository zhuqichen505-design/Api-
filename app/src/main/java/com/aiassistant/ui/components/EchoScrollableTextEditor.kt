package com.aiassistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Echo 大文本编辑器（v2.7.2 需求 1/2/3）：
 * 供「编辑模型回复 / 仅修改消息内容 / 系统提示词」这类包含大量文本的内容框统一使用。
 *
 * - 需求 3（滚动跳顶修复）：此前 OutlinedTextField 高度被 heightIn 截断后由其内部滚动接管，
 *   文本变化时内部滚动位置会被重置，表现为「在中间输入时页面跳回文字顶端」。
 *   本组件改为 BasicTextField 在无高度约束环境整体布局，滚动由外层 verticalScroll 承担，
 *   光标可见性经 bringIntoView 沿外层滚动解析——只在光标移出可视区时最小距离滚动，不再跳顶。
 * - 需求 2（右侧滑块）：内容溢出时右缘出现细轨道 + 圆角拇指滑块，支持拖动/点按直接定位。
 * - 高度随内容自适应（minHeight~maxHeight），溢出后固定在 maxHeight 内部滚动。
 */
@Composable
fun EchoScrollableTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: Dp = 140.dp,
    maxHeight: Dp = 340.dp,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    /** 外部可注入 ScrollState（如「跳转末尾」按钮需要滚动到底） */
    scrollState: ScrollState = rememberScrollState()
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    // 内容真实高度（BasicTextField 在无限高约束内整体布局，onSizeChanged 即全文高度）
    var contentHeightPx by remember { mutableIntStateOf(0) }
    val minPx = with(density) { minHeight.roundToPx() }
    val maxPx = with(density) { maxHeight.roundToPx() }
    val boxHeightPx = contentHeightPx.coerceIn(minPx, maxPx)
    val boxHeight = with(density) { boxHeightPx.toDp() }

    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (focused) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
    } else {
        secondaryColor.copy(alpha = 0.28f)
    }

    Row(modifier = modifier.height(boxHeight)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(shape)
                .background(containerColor)
                .border(BorderStroke(0.8.dp, borderColor), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
                    cursorBrush = SolidColor(contentColor),
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { contentHeightPx = it.height }
                )
            }
            if (value.text.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }

        // 固定宽度槽位：滑块隐藏时布局不跳动
        Box(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
        ) {
            EchoVerticalScrollSlider(
                scrollState = scrollState,
                viewportPx = boxHeightPx,
                contentPx = contentHeightPx,
                modifier = Modifier.fillMaxHeight(),
                accentColor = contentColor
            )
        }
    }
}

/**
 * 竖向滚动滑块（需求 2）：内容溢出视口时淡入，拇指位置/长度按滚动比例实时绘制；
 * 支持拇指/轨道拖动与点按定位。拖动期间以手势进度为准，避免滚动值回写滞后造成拇指抖动。
 */
@Composable
fun EchoVerticalScrollSlider(
    scrollState: ScrollState,
    viewportPx: Int,
    contentPx: Int,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val scope = rememberCoroutineScope()
    val scrollable = viewportPx > 0 && contentPx > viewportPx
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    var trackHeightPx by remember { mutableIntStateOf(0) }
    val currentScrollState by rememberUpdatedState(scrollState)
    val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)

    fun scrollToFraction(fraction: Float) {
        val h = currentTrackHeightPx
        if (h <= 0) return
        val max = currentScrollState.maxValue
        if (max <= 0) return
        dragProgress = fraction.coerceIn(0f, 1f)
        scope.launch {
            currentScrollState.scrollTo((fraction.coerceIn(0f, 1f) * max).roundToInt())
        }
    }

    AnimatedVisibility(
        visible = scrollable,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .onSizeChanged { trackHeightPx = it.height }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            scrollToFraction(offset.y / size.height.toFloat())
                        },
                        onDragEnd = { dragProgress = null },
                        onDragCancel = { dragProgress = null },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            scrollToFraction(change.position.y / size.height.toFloat())
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            scrollToFraction(offset.y / size.height.toFloat())
                        }
                    )
                }
        ) {
            val max = scrollState.maxValue.toFloat()
            if (max <= 0f || contentPx <= 0) return@Canvas
            val progress = dragProgress
                ?: (scrollState.value / max).coerceIn(0f, 1f)
            val thumbHeight = (size.height * (viewportPx.toFloat() / contentPx))
                .coerceIn(36.dp.toPx(), size.height)
            val thumbTop = (size.height - thumbHeight) * progress
            val trackWidth = 3.dp.toPx()
            val thumbWidth = 5.dp.toPx()
            val x = size.width - thumbWidth

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x + (thumbWidth - trackWidth) / 2f, 0f),
                size = Size(trackWidth, size.height),
                cornerRadius = CornerRadius(trackWidth, trackWidth)
            )
            drawRoundRect(
                color = accentColor.copy(alpha = 0.55f),
                topLeft = Offset(x, thumbTop),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2f, thumbWidth / 2f)
            )
        }
    }
}
