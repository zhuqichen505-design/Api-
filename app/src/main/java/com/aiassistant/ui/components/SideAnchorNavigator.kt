package com.aiassistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class SideAnchorItem(
    val title: String,
    val itemIndex: Int
)

@Composable
fun SideAnchorNavigator(
    items: List<SideAnchorItem>,
    listState: LazyListState,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (items.size < 2) return

    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val currentIndex by remember(items, listState) {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            items.lastOrNull { it.itemIndex <= first }?.itemIndex ?: items.first().itemIndex
        }
    }

    AnimatedVisibility(
        visible = visible || expanded,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .echoPlainClick { expanded = false }
                )
            }

            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                CollapsedAnchorRail(
                    items = items,
                    currentIndex = currentIndex,
                    onClick = { expanded = true }
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + scaleIn(initialScale = 0.96f),
                exit = fadeOut() + scaleOut(targetScale = 0.96f),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                ExpandedAnchorPanel(
                    items = items,
                    currentIndex = currentIndex,
                    onDismiss = { expanded = false },
                    onSelected = { item ->
                        expanded = false
                        scope.launch {
                            listState.animateScrollToItem(item.itemIndex)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CollapsedAnchorRail(
    items: List<SideAnchorItem>,
    currentIndex: Int,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
    val inactive = MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
    val visibleCount = items.size.coerceAtMost(24)
    val railHeight = (28 + (visibleCount - 1).coerceAtLeast(0) * 8)
        .coerceIn(48, 220)
        .dp
    Surface(
        modifier = Modifier
            .width(38.dp)
            .height(railHeight)
            .echoShapeClick(RoundedCornerShape(999.dp), onClick = onClick),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(railHeight)
        ) {
            val activePosition = items.indexOfLast { it.itemIndex <= currentIndex }
                .coerceAtLeast(0)
            val activeMarker = if (items.size <= 1) {
                0
            } else {
                (activePosition * (visibleCount - 1) / (items.size - 1).coerceAtLeast(1))
            }
            val step = 8.dp.toPx()
            val startY = (size.height - step * (visibleCount - 1).coerceAtLeast(0)) / 2f
            repeat(visibleCount) { marker ->
                val y = if (visibleCount <= 1) size.height / 2f else startY + marker * step
                val isActive = marker == activeMarker
                val width = if (isActive) 14.dp.toPx() else 10.dp.toPx()
                val height = if (isActive) 5.dp.toPx() else 3.dp.toPx()
                drawRoundRect(
                    color = if (isActive) primary else inactive,
                    topLeft = Offset(size.width - width - 7.dp.toPx(), y - height / 2f),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(999.dp.toPx(), 999.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun ExpandedAnchorPanel(
    items: List<SideAnchorItem>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelected: (SideAnchorItem) -> Unit
) {
    val panelShape = RoundedCornerShape(28.dp)
    Surface(
        modifier = Modifier
            .padding(end = 4.dp)
            .widthIn(min = 242.dp, max = 316.dp)
            .heightIn(max = 470.dp)
            .shadow(
                elevation = 12.dp,
                shape = panelShape,
                ambientColor = Color.Black.copy(alpha = 0.08f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(panelShape),
        shape = panelShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        LazyColumn(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
                    shape = panelShape
                )
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(items = items, key = { "${it.itemIndex}_${it.title}" }) { item ->
                val selected = item.itemIndex == currentIndex
                val itemShape = RoundedCornerShape(16.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                Color.Transparent
                            },
                            shape = itemShape
                        )
                        .echoShapeClick(itemShape) { onSelected(item) }
                        .padding(start = 16.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .width(if (selected) 13.dp else 11.dp)
                            .size(height = 3.dp, width = if (selected) 13.dp else 11.dp)
                            .background(
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.62f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.46f)
                                },
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
            item {
                val dismissShape = RoundedCornerShape(999.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(dismissShape, onClick = onDismiss)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "收起",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
