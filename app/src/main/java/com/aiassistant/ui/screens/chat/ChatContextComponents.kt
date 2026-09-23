@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.aiassistant.ui.screens.chat

import android.net.Uri
import android.graphics.BitmapFactory
import com.aiassistant.ui.components.ImageCropEditDialog
import com.aiassistant.ui.components.CropShapeMode
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import com.aiassistant.domain.model.ToolCallRecord
import com.aiassistant.domain.model.QueuedMessage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aiassistant.utils.TimelineMemoryHelper
import com.aiassistant.utils.TimelineReconcileResult
import com.aiassistant.utils.TimelineEventItem
import com.aiassistant.utils.TimelineCategory
import com.aiassistant.utils.AtemporalSettingItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalUriHandler
import com.aiassistant.ui.components.EchoTextToolbar
import com.aiassistant.ui.components.EchoTextToolbarHost
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Brush
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.aiassistant.AiAssistantApp
import com.aiassistant.R
import com.aiassistant.data.repository.AiRepository
import com.aiassistant.domain.model.Attachment
import com.aiassistant.domain.model.ChatModelOption
import com.aiassistant.domain.model.ConversationContextUsage
import com.aiassistant.domain.model.Message
import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.MarkdownText
import com.aiassistant.ui.components.SideAnchorItem
import com.aiassistant.ui.components.SideAnchorNavigator
import com.aiassistant.ui.components.TransientLazyListScrollbar
import com.aiassistant.ui.components.EchoPillSlider
import androidx.compose.runtime.CompositionLocalProvider
import com.aiassistant.ui.components.EchoGlassDialog
import com.aiassistant.ui.components.EchoGlassDropdownMenu
import com.aiassistant.ui.components.echoFilterChipBorder
import com.aiassistant.ui.components.echoFilterChipColors
import com.aiassistant.ui.components.rememberSmoothReorderState
import com.aiassistant.ui.components.reorderItem
import com.aiassistant.ui.components.reorderDragHandle
import com.aiassistant.ui.components.echoFilterChipElevation
import com.aiassistant.ui.components.echoGlassPalette
import com.aiassistant.ui.components.echoSegmentedButtonBorder
import com.aiassistant.ui.components.echoSegmentedButtonColors
import com.aiassistant.ui.components.echoShapeClick
import com.aiassistant.ui.components.echoHazePanel
import com.aiassistant.ui.components.echoHazeSource
import com.aiassistant.ui.components.readableTextColorFor
import com.aiassistant.ui.components.rememberReadableBackdropColors
import com.aiassistant.ui.components.rememberEchoHazeState
import com.aiassistant.ui.components.rememberLazyListControlsVisible
import com.aiassistant.utils.AvatarManager
import com.aiassistant.utils.BackgroundImageManager
import com.aiassistant.utils.FileUtils
import com.aiassistant.utils.RoleplaySmartAnalyzer
import com.aiassistant.utils.RoleplaySmartParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.aiassistant.domain.model.CharacterProfile
import com.aiassistant.domain.model.NarrativeMode
import com.aiassistant.domain.model.PlotAction
import com.aiassistant.domain.model.RoleplayScenario
import com.aiassistant.domain.model.RoleplaySession
import com.aiassistant.ui.components.EchoGlassCard
import com.aiassistant.ui.components.EchoPrimaryButton
import com.aiassistant.ui.components.EchoGlassButton
import com.aiassistant.ui.screens.roleplay.ConflictAction
import com.aiassistant.ui.theme.EchoTokens


internal fun buildChatAnchorItems(displayMessages: List<DisplayMessageItem>): List<SideAnchorItem> {
    return displayMessages.mapIndexedNotNull { index, item ->
        val message = item.message
        if (message.role != "user") return@mapIndexedNotNull null
        SideAnchorItem(
            title = anchorTitle(message.content),
            itemIndex = index
        )
    }
}

internal fun anchorTitle(value: String): String {
    return value
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.take(28)
        ?: "我的提问"
}

@Composable
internal fun ChatScrollJumpButtons(
    visible: Boolean,
    onJumpToTop: () -> Unit,
    onJumpToPrevInput: () -> Unit,
    onJumpToNextInput: () -> Unit,
    onJumpToBottom: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // 1. 滑动到顶：双条线向上箭头，极浅天蓝半透明
            Surface(
                onClick = onJumpToTop,
                shape = CircleShape,
                color = Color(0xFFBAE6FD).copy(alpha = 0.72f),
                contentColor = Color(0xFF0369A1),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                shadowElevation = 0.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowUp,
                        contentDescription = "滑动到顶",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // 2. 滑动到上一条输入：单条线向上箭头，柔和浅蓝半透明
            Surface(
                onClick = onJumpToPrevInput,
                shape = CircleShape,
                color = Color(0xFF93C5FD).copy(alpha = 0.75f),
                contentColor = Color(0xFF1D4ED8),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                shadowElevation = 0.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "滑动到上一条输入",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // 3. 滑动到下一条输入：单条线向下箭头，纯正天蓝半透明
            Surface(
                onClick = onJumpToNextInput,
                shape = CircleShape,
                color = Color(0xFF60A5FA).copy(alpha = 0.78f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                shadowElevation = 0.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "滑动到下一条输入",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // 4. 滑动到底：双条线向下箭头，蔚蓝半透明
            Surface(
                onClick = onJumpToBottom,
                shape = CircleShape,
                color = Color(0xFF3B82F6).copy(alpha = 0.82f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                shadowElevation = 0.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowDown,
                        contentDescription = "滑动到底",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ContextUsageButton(
    usage: ConversationContextUsage?,
    canCompress: Boolean,
    onClick: () -> Unit
) {
    val usagePercent = usage?.usagePercent ?: 0f
    val accent = contextUsageColor(usagePercent)
    val buttonShape = CircleShape

    Surface(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(40.dp)
            .echoShapeClick(buttonShape, onClick = onClick),
        shape = buttonShape,
        color = Color.Transparent,
        contentColor = accent
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContextUsageRing(
                progress = usagePercent,
                color = accent,
                modifier = Modifier.size(24.dp)
            )
            if (canCompress && usagePercent >= 0.60f) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )
            }
        }
    }
}

@Composable
internal fun ContextUsageRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
    Canvas(modifier = modifier) {
        val strokeWidth = 3.5.dp.toPx()
        drawCircle(
            color = trackColor,
            style = Stroke(width = strokeWidth)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = progress.coerceIn(0f, 1f) * 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
internal fun ContextUsageDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    state: ContextUsageUiState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onCompress: () -> Unit,
    onGenerateRollingSummary: () -> Unit = {},
    onEditRollingSummary: (() -> Unit)? = null
) {
    val usage = state.usage

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("上下文使用情况", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "模型窗口预算、时间线梳理与上下文压缩管理",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        content = {
            if (usage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ContextUsageOverview(usage = usage)
                    }
                    item {
                        ContextOptimizationActions(
                            usage = usage,
                            state = state,
                            onGenerateRollingSummary = onGenerateRollingSummary,
                            onEditRollingSummary = onEditRollingSummary,
                            onCompress = onCompress
                        )
                    }
                    item {
                        ContextUsageDetails(
                            usage = usage,
                            onGenerateRollingSummary = onGenerateRollingSummary,
                            onEditRollingSummary = onEditRollingSummary
                        )
                    }
                    item {
                        ContextUsageStatus(
                            usage = usage,
                            statusMessage = state.statusMessage
                        )
                    }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isCompressing && !state.isGeneratingSummary
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("刷新状态")
                }
                Button(onClick = onDismiss) {
                    Text("完成")
                }
            }
        }
    )
}

@Composable
internal fun ContextOptimizationActions(
    usage: ConversationContextUsage,
    state: ContextUsageUiState,
    onGenerateRollingSummary: () -> Unit,
    onEditRollingSummary: (() -> Unit)?,
    onCompress: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 卡片 1：时间线梳理与记忆沉淀
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "时间线与会话记忆 (Timeline & Memory)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "已沉淀 · ${usage.memoryItemCount} 条记忆",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "过往超出最近未压缩窗口的历史对话，已自动梳理沉淀为时间线节点与会话专属记忆。最近十几次对话（16+ 条）无条件无损保留，剧情自然向前推移，绝不在陈旧过去时间点原地踏步。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onGenerateRollingSummary,
                        enabled = !state.isGeneratingSummary,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isGeneratingSummary) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("梳理中...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("梳理时间线与提炼记忆", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // 卡片 2：主动上下文压缩
        val isCompressed = usage.compressedThroughMessageId != null
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Compress,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "主动上下文压缩 (Compression)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isCompressed) "已裁剪至 #${usage.compressedThroughMessageId}" else "尚未压缩",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = "【释放 Token 空间】基于时间线梳理与会话专属记忆，将早期历史对话沉淀归档，无条件完整保留最近十几次活跃对话（16+ 条），释放窗口空间同时确保剧情连贯推动。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onCompress,
                        enabled = !state.isCompressing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isCompressing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("正在压缩...", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isCompressed) "重新压缩 / 更新水线" else "立即压缩释放空间", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ContextUsageOverview(usage: ConversationContextUsage) {
    val progress = usage.usagePercent.coerceIn(0f, 1f)
    val accent = contextUsageColor(progress)
    val percentText = "${(progress * 100).toInt().coerceIn(0, 100)}%"
    val contextLimit = usage.contextWindowTokens.takeIf { it > 0 } ?: usage.promptBudgetTokens

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "最大上下文限制",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${formatTokenCount(usage.estimatedInputTokens)} / ${formatTokenCount(contextLimit)} tokens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = percentText,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
internal fun ContextUsageDetails(
    usage: ConversationContextUsage,
    onGenerateRollingSummary: (() -> Unit)? = null,
    onEditRollingSummary: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ContextUsageRow(
                label = "可用输入预算",
                value = "${formatTokenCount(usage.promptBudgetTokens)} tokens"
            )
            ContextUsageRow(
                label = "近期原文",
                value = "${usage.recentMessageCount} 条 · ${formatTokenCount(usage.recentTokens)} tokens"
            )
            ContextUsageRow(
                label = "较早历史消息",
                value = "${usage.olderMessageCount} 条"
            )
            ContextUsageRow(
                label = "时间线/记忆成果",
                value = "${usage.memoryItemCount} 条记忆 · ${formatTokenCount(usage.memoryTokens)} tokens"
            )
            ContextUsageRow(
                label = "压缩沉淀水线",
                value = usage.compressedThroughMessageId?.let { "已沉淀至 #$it" } ?: "未触发压缩"
            )
            ContextUsageRow(
                label = "沉淀梳理时间",
                value = usage.summaryUpdatedAt?.let(::formatContextTimestamp) ?: "暂无"
            )
        }
    }
}

@Composable
internal fun ContextUsageStatus(
    usage: ConversationContextUsage,
    statusMessage: String?
) {
    val isHighPressure = usage.usagePercent >= 0.60f
    val message = statusMessage ?: if (usage.canCompress && isHighPressure) {
        "上下文负载较高，可主动梳理时间线与提炼记忆以释放容量。"
    } else if (usage.canCompress) {
        "检测到较多早期历史对话，可按需梳理时间线与提炼记忆。"
    } else {
        "当前上下文容量充裕，最近十几次历史对话完整保留。"
    }
    val icon = if (isHighPressure) Icons.Default.Warning else Icons.Default.CheckCircle
    val color = if (isHighPressure) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun ContextUsageRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun contextUsageColor(usagePercent: Float): Color {
    return when {
        usagePercent >= 0.85f -> MaterialTheme.colorScheme.error
        usagePercent >= 0.65f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

internal fun formatTokenCount(value: Int): String {
    return if (value >= 1000) {
        String.format(Locale.getDefault(), "%.1fk", value / 1000f)
    } else {
        value.toString()
    }
}

internal fun formatContextTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatHeaderTitle(
    title: String,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            ),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun RollingSummaryEditDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    initialSummary: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var text by remember(initialSummary) { mutableStateOf(initialSummary) }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text("会话历史梳理与记忆", style = MaterialTheme.typography.titleLarge)
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "过往历史对话已梳理沉淀为时间线节点与会话专属记忆。您可以直接审阅或按需微调修改：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 460.dp),
                    maxLines = 30,
                    placeholder = { Text("暂无历史梳理内容...") },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onClear()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清除摘要")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onSave(text)
                            onDismiss()
                        }
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    )
}

