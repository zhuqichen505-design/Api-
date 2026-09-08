package com.aiassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class EchoTextToolbarState(
    val rect: Rect,
    val onCopy: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?
)

class EchoTextToolbar : TextToolbar {
    var activeMenu by mutableStateOf<EchoTextToolbarState?>(null)

    override val status: TextToolbarStatus
        get() = if (activeMenu != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    override fun hide() {
        activeMenu = null
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        val current = activeMenu
        if (current != null && current.rect == rect && current.onCopy == onCopyRequested && current.onSelectAll == onSelectAllRequested) {
            return
        }
        activeMenu = EchoTextToolbarState(
            rect = rect,
            onCopy = onCopyRequested,
            onSelectAll = onSelectAllRequested
        )
    }
}

@Composable
fun EchoTextToolbarHost(
    toolbar: EchoTextToolbar,
    onQuoteSelected: (String) -> Unit
) {
    val menu = toolbar.activeMenu ?: return
    val clipboardManager = LocalClipboardManager.current
    val density = LocalDensity.current
    val glass = echoGlassPalette()
    val coroutineScope = rememberCoroutineScope()

    val popupPositionProvider = remember(menu.rect, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                val x = ((menu.rect.left + menu.rect.right) / 2f - popupContentSize.width / 2f)
                    .roundToInt()
                    .coerceIn(16, (windowSize.width - popupContentSize.width - 16).coerceAtLeast(16))

                val yAbove = (menu.rect.top - popupContentSize.height - with(density) { 10.dp.toPx() }).roundToInt()
                val yBelow = (menu.rect.bottom + with(density) { 10.dp.toPx() }).roundToInt()
                val y = if (yAbove >= 70) yAbove else yBelow
                return IntOffset(x, y.coerceIn(16, (windowSize.height - popupContentSize.height - 16).coerceAtLeast(16)))
            }
        }
    }

    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = { toolbar.hide() },
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        val pillShape = RoundedCornerShape(999.dp)
        Surface(
            shape = pillShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, glass.outline),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (menu.onCopy != null) {
                    TextToolbarActionItem(
                        icon = Icons.Default.ContentCopy,
                        text = "复制",
                        onClick = {
                            menu.onCopy.invoke()
                            toolbar.hide()
                        }
                    )
                    TextToolbarActionItem(
                        icon = Icons.Default.FormatQuote,
                        text = "引用",
                        onClick = {
                            menu.onCopy.invoke()
                            coroutineScope.launch {
                                delay(60)
                                val copiedText = clipboardManager.getText()?.text.orEmpty()
                                if (copiedText.isNotBlank()) {
                                    onQuoteSelected(copiedText)
                                }
                                toolbar.hide()
                            }
                        }
                    )
                }
                if (menu.onSelectAll != null) {
                    TextToolbarActionItem(
                        icon = Icons.Default.SelectAll,
                        text = "全选",
                        onClick = {
                            menu.onSelectAll.invoke()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextToolbarActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
