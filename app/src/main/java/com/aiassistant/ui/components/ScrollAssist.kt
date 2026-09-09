package com.aiassistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberLazyListControlsVisible(
    listState: LazyListState,
    hideDelayMillis: Long = 500L
): MutableState<Boolean> {
    val visible = remember { mutableStateOf(false) }

    LaunchedEffect(listState, hideDelayMillis) {
        snapshotFlow { listState.isScrollInProgress }
            .collectLatest { isScrolling ->
                if (isScrolling) {
                    visible.value = true
                } else {
                    delay(hideDelayMillis)
                    visible.value = false
                }
            }
    }

    return visible
}

@Composable
fun TransientLazyListScrollbar(
    listState: LazyListState,
    visible: Boolean,
    modifier: Modifier = Modifier,
    thumbColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
) {
    val scope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var heightPx by remember { mutableIntStateOf(0) }
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
    var isDragging by remember { mutableStateOf(false) }
    val shouldShow = (visible || isDragging) && totalItems > visibleItems
    var stableVisibleItems by remember(totalItems, heightPx) { mutableIntStateOf(0) }
    var dragProgress by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(shouldShow) {
        if (!shouldShow) {
            stableVisibleItems = 0
            dragProgress = null
            isDragging = false
        } else if (stableVisibleItems == 0) {
            stableVisibleItems = visibleItems
        }
    }

    val effectiveVisibleItems = stableVisibleItems.takeIf { it > 0 } ?: visibleItems

    val currentTotalItems by androidx.compose.runtime.rememberUpdatedState(totalItems)
    val currentEffectiveVisibleItems by androidx.compose.runtime.rememberUpdatedState(effectiveVisibleItems)
    val currentHeightPx by androidx.compose.runtime.rememberUpdatedState(heightPx)
    val currentListState by androidx.compose.runtime.rememberUpdatedState(listState)

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .width(40.dp)
            .fillMaxHeight()
            .onSizeChanged { heightPx = it.height }
            .pointerInput(Unit) {
                fun scrollTo(y: Float) {
                    val h = currentHeightPx
                    if (h <= 0) return
                    val progress = (y / h).coerceIn(0f, 1f)
                    dragProgress = progress
                    val maxIndex = (currentTotalItems - currentEffectiveVisibleItems).coerceAtLeast(0)
                    val exactIndex = progress * maxIndex
                    val targetIndex = exactIndex.toInt().coerceIn(0, maxIndex)
                    val offsetFraction = exactIndex - targetIndex
                    val estimatedOffsetPx = (offsetFraction * 150).roundToInt()
                    scrollJob?.cancel()
                    scrollJob = scope.launch {
                        currentListState.scrollToItem(targetIndex, estimatedOffsetPx)
                    }
                }

                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        scrollTo(offset.y)
                    },
                    onDragEnd = {
                        isDragging = false
                        scrollJob = null
                        dragProgress = null
                    },
                    onDragCancel = {
                        isDragging = false
                        scrollJob = null
                        dragProgress = null
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        scrollTo(change.position.y)
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            val activeThumbColor = if (isDragging) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
            } else {
                thumbColor
            }
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(22.dp)
                    .padding(vertical = 8.dp)
            ) {
                val trackWidth = 4.dp.toPx()
                val thumbWidth = 8.dp.toPx()
                val minThumbHeight = 44.dp.toPx()
                val thumbHeight = (size.height * (effectiveVisibleItems / totalItems.toFloat()))
                    .coerceIn(minThumbHeight, size.height)
                val maxFirstIndex = (totalItems - effectiveVisibleItems).coerceAtLeast(1)
                val progress = dragProgress
                    ?: (listState.firstVisibleItemIndex / maxFirstIndex.toFloat()).coerceIn(0f, 1f)
                val thumbTop = (size.height - thumbHeight) * progress
                val trackLeft = size.width - trackWidth
                val thumbLeft = size.width - thumbWidth

                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(trackLeft, 0f),
                    size = Size(trackWidth, size.height),
                    cornerRadius = CornerRadius(trackWidth, trackWidth)
                )
                drawRoundRect(
                    color = activeThumbColor,
                    topLeft = Offset(thumbLeft, thumbTop),
                    size = Size(thumbWidth, thumbHeight),
                    cornerRadius = CornerRadius(thumbWidth, thumbWidth)
                )
            }
        }
    }
}
