package com.aiassistant.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 平滑拖拽与重排序状态控制器 (SmoothReorderState)
 * 支持：
 * 1. 按住六点手柄实时位移跟随手指 (translationY 连续渲染)
 * 2. 拖拽条目放大与悬浮阴影 (scale = 1.02f, zIndex = 10f)
 * 3. 跨越相邻项时平滑无缝数据交换，手势不中断并伴随触觉反馈
 * 4. 相邻被挤开项弹簧动画平滑让位 (spring animation)
 * 5. 松手/取消时弹簧平滑吸附归位 (spring snap on release)
 * 6. 箭头按钮点击时双向对流平滑换位动画
 */
@Stable
class SmoothReorderState(
    private val coroutineScope: CoroutineScope
) {
    // 当前正在被拖动的项目索引
    var draggingIndex by mutableStateOf<Int?>(null)
        private set

    // 正在拖动的项目的实时垂直位移
    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    // 正在释放回弹的 Animatable
    private val releaseAnim = Animatable(0f)
    var isReleasing by mutableStateOf(false)
        private set

    // 记录各条目由于交换产生的位置平滑补间动画（由 itemKey 索引）
    private val itemAnimMap = mutableMapOf<Any, Animatable<Float, AnimationVector1D>>()

    // 记录各条目的实测高度（像素）
    private val itemHeightMap = mutableMapOf<Int, Float>()

    fun setItemHeight(index: Int, height: Float) {
        if (height > 0f) {
            itemHeightMap[index] = height
        }
    }

    fun getItemHeight(index: Int): Float {
        return itemHeightMap[index] ?: 180f
    }

    /**
     * 获取指定 key 和 index 的平滑位移动画偏移
     */
    fun getItemOffsetY(key: Any, index: Int): Float {
        if (draggingIndex == index) {
            return if (isReleasing) releaseAnim.value else dragOffsetY
        }
        return itemAnimMap[key]?.value ?: 0f
    }

    /**
     * 是否处于拖拽或释放状态
     */
    fun isItemActive(index: Int): Boolean {
        return draggingIndex == index
    }

    /**
     * 手势开始
     */
    fun onDragStart(index: Int) {
        coroutineScope.launch {
            releaseAnim.snapTo(0f)
            isReleasing = false
            dragOffsetY = 0f
            draggingIndex = index
        }
    }

    /**
     * 手势拖拽中
     */
    fun onDragDelta(
        deltaY: Float,
        listSize: Int,
        keys: List<Any>,
        haptic: HapticFeedback?,
        onMove: (fromIndex: Int, toIndex: Int) -> Unit
    ) {
        val currentIndex = draggingIndex ?: return
        dragOffsetY += deltaY

        val itemHeight = getItemHeight(currentIndex)
        val threshold = itemHeight * 0.42f

        // 向下拖动超过半个身位，且未到底部
        if (dragOffsetY > threshold && currentIndex < listSize - 1) {
            val targetIndex = currentIndex + 1
            val targetKey = keys.getOrNull(targetIndex)

            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            onMove(currentIndex, targetIndex)

            // 正在拖拽项补偿位移，视觉位置无缝保持在手指下方
            dragOffsetY -= itemHeight
            draggingIndex = targetIndex

            // 被交换项启动平滑滑动动画：视觉位置瞬间保持在原处，然后平滑弹簧滑向 0f
            if (targetKey != null) {
                animateItemTransition(targetKey, itemHeight)
            }
        }
        // 向上拖动超过半个身位，且未到顶部
        else if (dragOffsetY < -threshold && currentIndex > 0) {
            val targetIndex = currentIndex - 1
            val targetKey = keys.getOrNull(targetIndex)

            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
            onMove(currentIndex, targetIndex)

            // 正在拖拽项补偿位移，视觉位置无缝保持在手指下方
            dragOffsetY += itemHeight
            draggingIndex = targetIndex

            // 被交换项启动平滑滑动动画：视觉位置瞬间保持在原处，然后平滑弹簧滑向 0f
            if (targetKey != null) {
                animateItemTransition(targetKey, -itemHeight)
            }
        }
    }

    /**
     * 手势松开或取消
     */
    fun onDragFinish() {
        val currentIndex = draggingIndex ?: return
        isReleasing = true
        coroutineScope.launch {
            releaseAnim.snapTo(dragOffsetY)
            releaseAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            draggingIndex = null
            dragOffsetY = 0f
            isReleasing = false
        }
    }

    /**
     * 点击按钮（如箭头）触发的相邻项平滑换位动画
     */
    fun onAnimateSwap(
        fromKey: Any,
        toKey: Any,
        fromIndex: Int,
        toIndex: Int
    ) {
        val itemHeight = getItemHeight(fromIndex)
        val direction = if (toIndex > fromIndex) 1f else -1f
        // fromKey 移向新位置
        animateItemTransition(fromKey, -direction * itemHeight)
        // toKey 移向新位置
        animateItemTransition(toKey, direction * itemHeight)
    }

    private fun animateItemTransition(key: Any, startOffset: Float) {
        coroutineScope.launch {
            val anim = itemAnimMap.getOrPut(key) { Animatable(0f) }
            anim.snapTo(startOffset)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
}

@Composable
fun rememberSmoothReorderState(): SmoothReorderState {
    val coroutineScope = rememberCoroutineScope()
    return remember { SmoothReorderState(coroutineScope) }
}

/**
 * 拖拽项的外层容器修饰符
 */
fun Modifier.reorderItem(
    state: SmoothReorderState,
    index: Int,
    key: Any,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier = this
    .onSizeChanged { size ->
        state.setItemHeight(index, size.height.toFloat())
    }
    .zIndex(if (state.isItemActive(index)) 10f else 0f)
    .graphicsLayer {
        val offsetY = state.getItemOffsetY(key, index)
        translationY = offsetY
        if (state.isItemActive(index)) {
            scaleX = 1.02f
            scaleY = 1.02f
            shadowElevation = 8.dp.toPx()
            this.shape = shape
            this.clip = false
        }
    }

/**
 * 六点拖动手柄修饰符
 */
fun Modifier.reorderDragHandle(
    state: SmoothReorderState,
    index: () -> Int,
    key: () -> Any,
    keys: () -> List<Any>,
    listSize: () -> Int,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    this.pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val currentIndex = index()
            state.onDragStart(currentIndex)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            var hasConsumedMove = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                val deltaY = change.position.y - change.previousPosition.y
                if (kotlin.math.abs(deltaY) > 0.5f || hasConsumedMove) {
                    change.consume()
                    hasConsumedMove = true
                    state.onDragDelta(
                        deltaY = deltaY,
                        listSize = listSize(),
                        keys = keys(),
                        haptic = haptic,
                        onMove = onMove
                    )
                }
            }
            state.onDragFinish()
        }
    }
}
