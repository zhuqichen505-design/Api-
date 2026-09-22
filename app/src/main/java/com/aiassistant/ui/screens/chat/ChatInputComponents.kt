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


@Composable
fun ChatInputBar(
    hazeState: dev.chrisbanes.haze.HazeState,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    quotedText: String? = null,
    onClearQuote: () -> Unit = {},
    isGenerating: Boolean,
    onStopGeneration: () -> Unit,
    attachments: List<Attachment>,
    onRemoveAttachment: (Attachment) -> Unit,
    isProcessingAttachments: Boolean,
    attachmentStatus: String?,
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onOcrImages: () -> Unit,
    enableWebSearch: Boolean,
    onWebSearchChange: (Boolean) -> Unit,
    enableThinking: Boolean = true,
    thinkingEffort: String = "medium",
    onThinkingChange: (Boolean, String) -> Unit = { _, _ -> },
    showThinkingPopover: Boolean = false,
    onThinkingPopoverChange: (Boolean) -> Unit = {},
    isRoleplay: Boolean = false,
    onPlotActionClick: () -> Unit = {},
    readableBackdrop: Color = Color.Unspecified,
    modelName: String = "",
    isBarsHidden: Boolean = false,
    onBarsHiddenChange: (Boolean) -> Unit = {}
) {
    var showToolMenu by remember { mutableStateOf(false) }
    var isInputExpanded by remember { mutableStateOf(false) }
    var customInputHeightDp by remember { mutableStateOf<Float?>(null) }
    val density = LocalDensity.current
    val effectiveMinHeight = customInputHeightDp?.dp ?: if (isInputExpanded) 180.dp else 42.dp
    val effectiveMaxHeight = if (customInputHeightDp != null) 360.dp else if (isInputExpanded) 320.dp else 112.dp
    val inputShape = RoundedCornerShape(22.dp)
    val resolvedReadableBackdrop = readableBackdrop.takeOrElse {
        MaterialTheme.colorScheme.background
    }
    val glass = echoGlassPalette()
    val inputTint = glass.input
    val inputTextColor = readableTextColorFor(
        background = inputTint,
        fallbackSurface = resolvedReadableBackdrop
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 深度思考向上展开渐变滑块气泡弹窗
        AnimatedVisibility(
            visible = showThinkingPopover && !isBarsHidden,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier.clip(RoundedCornerShape(22.dp))
        ) {
            ReasoningEffortPopupCard(
                enableThinking = enableThinking,
                thinkingEffort = thinkingEffort,
                modelName = modelName,
                onEffortSelected = { enabled, effort ->
                    onThinkingChange(enabled, effort)
                },
                onClose = { onThinkingPopoverChange(false) }
            )
        }

        AnimatedContent(
            targetState = isBarsHidden,
            transitionSpec = {
                (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(280)))
                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(200)))
            },
            label = "inputBarHiddenAnim"
        ) { hidden ->
            if (hidden) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp, bottom = 4.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    val bottomPulseTransition = rememberInfiniteTransition(label = "bottomPulse")
                    val bottomPulseScale by bottomPulseTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bottomPulseScale"
                    )
                    val bottomPulseAlpha by bottomPulseTransition.animateFloat(
                        initialValue = 0.55f,
                        targetValue = 0.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bottomPulseAlpha"
                    )
                    val bottomHaloColor = if (isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    val bottomSurfaceBorderColor = if (isGenerating) MaterialTheme.colorScheme.error.copy(alpha = 0.85f) else glass.outlineSelected
                    Box(
                        modifier = Modifier.size(34.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val strokeWidth = 1.2.dp.toPx()
                            val baseRadius = (size.minDimension - strokeWidth) / 2f
                            val currentRadius = baseRadius * bottomPulseScale
                            drawCircle(
                                color = bottomHaloColor.copy(alpha = bottomPulseAlpha),
                                radius = currentRadius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                        Surface(
                            onClick = { onBarsHiddenChange(false) },
                            shape = CircleShape,
                            color = if (isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            border = BorderStroke(1.2.dp, bottomSurfaceBorderColor),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = if (isGenerating) Icons.Default.Stop else Icons.Default.ArrowUpward,
                                    contentDescription = "取消隐藏并恢复输入栏",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoHazePanel(
                            hazeState = hazeState,
                            shape = inputShape,
                            tint = inputTint,
                            blurRadius = 16.dp,
                            highlightAlpha = 0.025f
                    ),
                    shape = inputShape,
                    color = Color.Transparent,
                    contentColor = inputTextColor,
            border = BorderStroke(1.dp, glass.outline),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (attachments.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            items(attachments) { attachment ->
                                AttachmentPreview(
                                    attachment = attachment,
                                    onRemove = { onRemoveAttachment(attachment) },
                                    readableBackdrop = resolvedReadableBackdrop
                                )
                            }
                        }
                    }

                    if (!quotedText.isNullOrBlank()) {
                        QuotedTextPreviewCard(
                            quotedText = quotedText,
                            onClear = onClearQuote,
                            textColor = inputTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = effectiveMinHeight,
                                max = effectiveMaxHeight
                            )
                            .background(Color.Transparent),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = inputTextColor,
                            background = Color.Transparent
                        ),
                        cursorBrush = SolidColor(inputTextColor),
                        maxLines = if (isInputExpanded || (customInputHeightDp ?: 0f) > 60f) 15 else 5,
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = effectiveMinHeight)
                                    .background(Color.Transparent)
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                if (inputText.isBlank()) {
                                    Text(
                                        text = if (isRoleplay) "输入剧情提示、行动或指令..." else "给 Echo 发送消息",
                                        color = inputTextColor.copy(alpha = 0.62f),
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(end = 28.dp)
                                    )
                                }
                                Box(modifier = Modifier.fillMaxWidth().padding(end = 28.dp)) {
                                    innerTextField()
                                }
                            }
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. 深度思考 按钮（排在第1位，点击向上展开档位弹窗）
                            item {
                                val effortText = when {
                                    !enableThinking -> "深度思考"
                                    thinkingEffort.equals("low", true) || thinkingEffort.equals("fast", true) -> "快速思考"
                                    thinkingEffort.equals("medium", true) || thinkingEffort.equals("balanced", true) -> "平衡思考"
                                    thinkingEffort.equals("high", true) || thinkingEffort.equals("deep", true) -> "深入思考"
                                    thinkingEffort.equals("ultra", true) || thinkingEffort.equals("max", true) -> "极高思考"
                                    else -> "平衡思考"
                                }
                                val effortAccentColor = when {
                                    !enableThinking -> glass.outline
                                    thinkingEffort.equals("low", true) || thinkingEffort.equals("fast", true) -> Color(0xFF60A5FA) // 柔和纯正天蓝
                                    thinkingEffort.equals("medium", true) || thinkingEffort.equals("balanced", true) -> Color(0xFF2563EB) // 蔚蓝
                                    thinkingEffort.equals("high", true) || thinkingEffort.equals("deep", true) -> Color(0xFF1D4ED8) // 深海蓝
                                    thinkingEffort.equals("ultra", true) || thinkingEffort.equals("max", true) -> Color(0xFF4338CA) // 靛青紫蓝
                                    else -> Color(0xFF2563EB)
                                }
                                InputPillButton(
                                    text = effortText,
                                    icon = null,
                                    trailingIcon = if (showThinkingPopover) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    selected = enableThinking,
                                    onClick = { onThinkingPopoverChange(!showThinkingPopover) },
                                    containerColor = if (enableThinking) {
                                        effortAccentColor.copy(alpha = 0.16f)
                                    } else {
                                        glass.control
                                    },
                                    contentColor = if (enableThinking) {
                                        effortAccentColor
                                    } else {
                                        inputTextColor
                                    },
                                    borderColor = if (enableThinking) {
                                        effortAccentColor.copy(alpha = 0.65f)
                                    } else {
                                        glass.outline
                                    }
                                )
                            }

                        // 2. 故事模式：剧情操作；非故事模式：智能搜索
                        if (isRoleplay) {
                            item {
                                InputPillButton(
                                    text = "剧情操作",
                                    icon = Icons.AutoMirrored.Filled.AltRoute,
                                    selected = false,
                                    onClick = onPlotActionClick,
                                    containerColor = glass.control,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    borderColor = glass.outline
                                )
                            }
                        } else {
                            item {
                                InputPillButton(
                                    text = "智能搜索",
                                    selected = enableWebSearch,
                                    onClick = { onWebSearchChange(!enableWebSearch) },
                                    containerColor = if (enableWebSearch) {
                                        glass.controlSelected
                                    } else {
                                        glass.control
                                    },
                                    contentColor = if (enableWebSearch) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        inputTextColor
                                    },
                                    borderColor = if (enableWebSearch) {
                                        glass.outlineSelected
                                    } else {
                                        glass.outline
                                    }
                                )
                            }
                        }

                        attachmentStatus?.let { status ->
                            item {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = glass.control,
                                    contentColor = inputTextColor,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (isProcessingAttachments) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = inputTextColor
                                            )
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        Text(status, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = glass.control,
                            contentColor = MaterialTheme.colorScheme.primary,
                            border = BorderStroke(1.2.dp, glass.outlineSelected),
                            modifier = Modifier
                                .size(34.dp)
                                .echoShapeClick(CircleShape, enabled = !isProcessingAttachments, onClick = { showToolMenu = true })
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.Add, contentDescription = "添加内容", modifier = Modifier.size(18.dp))
                            }
                        }
                        EchoGlassDropdownMenu(
                            expanded = showToolMenu,
                            onDismissRequest = { showToolMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("上传图片", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showToolMenu = false
                                    onPickImage()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("选取文件", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showToolMenu = false
                                    onPickFile()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("图片 OCR 识别", fontWeight = FontWeight.Medium) },
                                onClick = {
                                    showToolMenu = false
                                    onOcrImages()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }

                    val canSend = !isProcessingAttachments && (inputText.isNotBlank() || attachments.isNotEmpty() || !quotedText.isNullOrBlank())
                    if (isGenerating) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.85f)),
                                modifier = Modifier
                                    .size(34.dp)
                                    .echoShapeClick(CircleShape, onClick = onStopGeneration)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Stop, contentDescription = "停止当前生成", modifier = Modifier.size(18.dp))
                                }
                            }

                            if (canSend) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .echoShapeClick(CircleShape, enabled = true, onClick = onSend)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "加入排队",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        val sendBorderColor = if (canSend) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else glass.outlineSelected
                        Surface(
                            shape = CircleShape,
                            color = if (canSend) MaterialTheme.colorScheme.primary else glass.control,
                            contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            border = BorderStroke(1.2.dp, sendBorderColor),
                            modifier = Modifier
                                .size(34.dp)
                                .echoShapeClick(CircleShape, enabled = canSend, onClick = onSend)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "发送",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

                // 紧贴输入框右上角同心圆弧手柄 (与边框圆角同心贴合，尺寸固定为拖拽后较小尺寸 22dp/17dp，拖拽前后大小绝对统一)
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val cornerRadiusDp = 22f
                val handleBoxSize = 36.dp
                val outlineColor = MaterialTheme.colorScheme.outline
                val arcColor = if (isDark) {
                    Color.White.copy(alpha = 0.32f)
                } else {
                    outlineColor.copy(alpha = 0.44f)
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(handleBoxSize)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (isInputExpanded || (customInputHeightDp ?: 42f) > 60f) {
                                        customInputHeightDp = null
                                        isInputExpanded = false
                                    } else {
                                        customInputHeightDp = 180f
                                        isInputExpanded = true
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentH = customInputHeightDp ?: (if (isInputExpanded) 180f else 42f)
                                    if (currentH <= 46f && dragAmount > 8f) {
                                        onBarsHiddenChange(true)
                                    } else {
                                        val deltaDp = dragAmount / density.density
                                        val newH = (currentH - deltaDp).coerceIn(42f, 360f)
                                        customInputHeightDp = newH
                                        isInputExpanded = newH > 60f
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.TopEnd
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.0.dp.toPx()
                        val cornerR = cornerRadiusDp.dp.toPx()
                        val arcR = (cornerR - 5.dp.toPx()).coerceAtLeast(8.dp.toPx())
                        // 圆心位于右上角向左 cornerR，向下 cornerR
                        val centerX = size.width - cornerR
                        val centerY = cornerR
                        drawArc(
                            color = arcColor,
                            startAngle = 270f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(centerX - arcR, centerY - arcR),
                            size = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
    }
}
}
}

@Composable
internal fun QuotedTextPreviewCard(
    quotedText: String,
    onClear: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val glass = echoGlassPalette()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = glass.control.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, glass.outlineSelected.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.FormatQuote,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "引用内容",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = quotedText.replace('\n', ' '),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消引用",
                    tint = textColor.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}



data class ThinkingEffortLevel(
    val step: Int,
    val key: String,
    val enabled: Boolean,
    val name: String,
    val subtitle: String,
    val detail: String,
    val primaryColor: Color,
    val gradientColors: List<Color>
)

@Composable
internal fun ReasoningEffortPopupCard(
    enableThinking: Boolean,
    thinkingEffort: String,
    modelName: String = "",
    onEffortSelected: (Boolean, String) -> Unit,
    onClose: () -> Unit
) {
    var showParamsExplanationDialog by remember { mutableStateOf(false) }

    val levels = remember {
        listOf(
            ThinkingEffortLevel(
                step = 0,
                key = "none",
                enabled = false,
                name = "关闭思考",
                subtitle = "关闭思考 · 极速直答",
                detail = "跳过深度思维链推演，以模型最高速度直接生成最终回复（若模型强制要求思考则保持原生工作）。",
                primaryColor = Color(0xFF64748B),
                gradientColors = listOf(Color(0xFF64748B), Color(0xFF94A3B8))
            ),
            ThinkingEffortLevel(
                step = 1,
                key = "low",
                enabled = true,
                name = "low",
                subtitle = "low · 基础轻度思考",
                detail = "分配少量思考预算进行轻度推理，适合常规闲聊、基础问答与快速响应。",
                primaryColor = Color(0xFF60A5FA),
                gradientColors = listOf(Color(0xFF93C5FD), Color(0xFF60A5FA))
            ),
            ThinkingEffortLevel(
                step = 2,
                key = "medium",
                enabled = true,
                name = "medium",
                subtitle = "medium · 均衡标准思考",
                detail = "平衡逻辑严谨性与响应耗时，应对大多数日常工作、深度分析与创作场景（推荐）。",
                primaryColor = Color(0xFF2563EB),
                gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
            ),
            ThinkingEffortLevel(
                step = 3,
                key = "high",
                enabled = true,
                name = "high",
                subtitle = "high · 深度严密思考",
                detail = "投入充足思考预算进行多步论证、边界检查与严密推演，适合复杂技术任务与代码分析。",
                primaryColor = Color(0xFF1D4ED8),
                gradientColors = listOf(Color(0xFF1D4ED8), Color(0xFF1E3A8A))
            ),
            ThinkingEffortLevel(
                step = 4,
                key = "max",
                enabled = true,
                name = "max",
                subtitle = "max · 极限最大思考",
                detail = "释放最大思考预算上限，全力攻坚高难度逻辑推演、数学证明与复杂长文思考。",
                primaryColor = Color(0xFF4F46E5),
                gradientColors = listOf(Color(0xFF6366F1), Color(0xFF4338CA))
            )
        )
    }

    val maxStep = levels.lastIndex
    val currentStep = remember(enableThinking, thinkingEffort, maxStep) {
        if (!enableThinking) 0
        else when (thinkingEffort.lowercase()) {
            "low", "fast" -> 1
            "medium", "balanced" -> 2
            "high", "deep" -> 3
            "ultra", "max" -> 4
            else -> 2
        }.coerceIn(0, maxStep)
    }

    var sliderIndex by remember { mutableFloatStateOf(currentStep.toFloat()) }
    LaunchedEffect(currentStep) {
        if (sliderIndex.roundToInt() != currentStep) {
            sliderIndex = currentStep.toFloat()
        }
    }
    val currentLevel = levels[sliderIndex.roundToInt().coerceIn(0, maxStep)]
    val glass = echoGlassPalette()

    val popupShape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(popupShape),
        shape = popupShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, currentLevel.primaryColor.copy(alpha = 0.45f)),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶部 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(currentLevel.gradientColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "深度思考强度",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showParamsExplanationDialog = true },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "思考参数说明",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = currentLevel.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = currentLevel.primaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onClose,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("完成", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 胶囊美观滑块区域 (严格还原 media_1788845280823.jpg)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EchoPillSlider(
                    value = sliderIndex,
                    onValueChange = { newVal ->
                        sliderIndex = newVal
                        val stepInt = newVal.roundToInt().coerceIn(0, maxStep)
                        val target = levels[stepInt]
                        onEffortSelected(target.enabled, target.key)
                    },
                    valueRange = 0f..maxStep.toFloat(),
                    steps = (levels.size - 2).coerceAtLeast(0),
                    activeColor = currentLevel.primaryColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )

                // 快捷点选 Chips 行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    levels.forEach { lvl ->
                        val isSelected = currentLevel.step == lvl.step
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) lvl.primaryColor else glass.control,
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) lvl.primaryColor else glass.outline
                            ),
                            modifier = Modifier.clickable {
                                sliderIndex = lvl.step.toFloat()
                                onEffortSelected(lvl.enabled, lvl.key)
                            }
                        ) {
                            Text(
                                text = lvl.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showParamsExplanationDialog) {
        ThinkingParamsExplanationDialog(
            onDismiss = { showParamsExplanationDialog = false }
        )
    }
}

@Composable
internal fun ThinkingParamsExplanationDialog(
    onDismiss: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val contentColor = readableTextColorFor(containerColor, MaterialTheme.colorScheme.background)
    val secondaryColor = contentColor.copy(alpha = 0.72f)

    EchoGlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 440.dp),
        tint = containerColor,
        containerColor = containerColor,
        contentColor = contentColor,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "思考强度与实际参数说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = "各档位在不同模型与 API 架构中传入的底层参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThinkingParamCard(
                    title = "关闭思考 (none)",
                    badge = "无思考预算",
                    badgeColor = Color(0xFF64748B),
                    desc = "跳过思维链推演，以模型原生最高速度直接生成最终回复内容（强制思考模型保持原生工作）。",
                    params = listOf(
                        "OpenAI / o系列" to "不传 reasoning_effort",
                        "Claude / Anthropic" to "不启用 thinking 模块",
                        "DeepSeek 官方" to "切换为 deepseek-chat 或原生运行"
                    )
                )

                ThinkingParamCard(
                    title = "low",
                    badge = "精简推演",
                    badgeColor = Color(0xFF60A5FA),
                    desc = "分配精简思考预算进行关键逻辑检查，低延迟极速响应。",
                    params = listOf(
                        "OpenAI / o系列" to "reasoning_effort = \"low\"",
                        "Claude / Anthropic" to "thinking.budget_tokens = 2048",
                        "通用兼容 API" to "thinking_effort = \"low\""
                    )
                )

                ThinkingParamCard(
                    title = "medium",
                    badge = "推荐默认",
                    badgeColor = Color(0xFF2563EB),
                    desc = "投入适度思考预算，严密推演逻辑与代码设计（日常最佳平衡点）。",
                    params = listOf(
                        "OpenAI / o系列" to "reasoning_effort = \"medium\"",
                        "Claude / Anthropic" to "thinking.budget_tokens = 4096 / 8192",
                        "通用兼容 API" to "thinking_effort = \"medium\""
                    )
                )

                ThinkingParamCard(
                    title = "high",
                    badge = "深度推理",
                    badgeColor = Color(0xFF1D4ED8),
                    desc = "投入大量思考预算进行多步论证、边界检查与复杂代码推演。",
                    params = listOf(
                        "OpenAI / o系列" to "reasoning_effort = \"high\"",
                        "Claude / Anthropic" to "thinking.budget_tokens = 8192 / 16384",
                        "通用兼容 API" to "thinking_effort = \"high\""
                    )
                )

                ThinkingParamCard(
                    title = "max",
                    badge = "极限预算",
                    badgeColor = Color(0xFF4F46E5),
                    desc = "释放最大思考预算上限，全力攻坚高难算法、数学定理与复杂多维哲学推理。",
                    params = listOf(
                        "OpenAI / o系列" to "reasoning_effort = \"high\" (严格兼容不报错)",
                        "Claude / Anthropic" to "thinking.budget_tokens = 32768 / 64000 (满额)",
                        "通用兼容 API" to "thinking_effort = \"max\""
                    )
                )
            }
        },
        buttons = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("我知道了")
            }
        }
    )
}

@Composable
internal fun ThinkingParamCard(
    title: String,
    badge: String,
    badgeColor: Color,
    desc: String,
    params: List<Pair<String, String>>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                params.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun InputModelSelector(
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    onModelSelected: (ChatModelOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(currentOption, fallbackModel, availableOptions) {
        val fallback = currentOption ?: ChatModelOption(
            apiConfigId = 0,
            configName = "当前对话",
            provider = "",
            apiType = "",
            modelName = fallbackModel
        )
        (availableOptions + fallback)
            .filter { it.modelName.isNotBlank() }
            .distinctBy { "${it.apiConfigId}:${it.modelName}" }
    }
    val currentLabel = currentOption?.modelName ?: fallbackModel
    val isCurrentInvalid = remember(currentOption, availableOptions) {
        currentOption != null && (currentOption.apiConfigId == 0L || availableOptions.none { it.apiConfigId == currentOption.apiConfigId })
    }

    Box {
        InputPillButton(
            text = if (isCurrentInvalid) "${currentLabel.shortModelLabel()} ⚠️" else currentLabel.shortModelLabel(),
            selected = true,
            trailingIcon = if (isCurrentInvalid) Icons.Default.Warning else Icons.Default.ArrowDropDown,
            containerColor = if (isCurrentInvalid) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f) else null,
            contentColor = if (isCurrentInvalid) MaterialTheme.colorScheme.error else null,
            borderColor = if (isCurrentInvalid) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else null,
            onClick = { expanded = true }
        )

        EchoGlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            if (isCurrentInvalid) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "当前模型配置已失效，请在下方切换至可用模型",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    onClick = {}
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
            }

            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("暂无可切换模型") },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            ModelOptionText(option = option)
                        },
                        onClick = {
                            onModelSelected(option)
                            expanded = false
                        },
                        leadingIcon = {
                            if (option.sameModelOption(currentOption, currentLabel)) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ModelOptionText(option: ChatModelOption) {
    val cap = remember(option.modelName) {
        com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(option.modelName)
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = option.modelName.displayModelShortName(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (cap.contextWindowDisplay.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = cap.contextWindowDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        val subLine = listOf(option.configName, option.provider)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        val badges = buildList {
            if (subLine.isNotBlank()) add(subLine)
            if (cap.supportsVision) add("视觉")
            if (cap.supportsTools) add("工具")
            if (cap.supportsReasoning) add("思考")
        }.joinToString(" | ")

        if (badges.isNotBlank()) {
            Text(
                text = badges,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun InputPillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null
) {
    val pillShape = RoundedCornerShape(999.dp)
    val glass = echoGlassPalette()
    val resolvedContainerColor = containerColor ?: if (selected) {
        glass.controlSelected
    } else {
        glass.control
    }
    val resolvedContentColor = contentColor ?: if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        readableTextColorFor(
            background = resolvedContainerColor,
            fallbackSurface = MaterialTheme.colorScheme.background
        )
    }
    Surface(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .echoShapeClick(pillShape, onClick = onClick),
        shape = pillShape,
        color = resolvedContainerColor,
        contentColor = resolvedContentColor,
        border = BorderStroke(if (selected) 1.3.dp else 1.dp, borderColor ?: if (selected) glass.outlineSelected else glass.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = resolvedContentColor
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = resolvedContentColor
                )
            }
        }
    }
}

fun String.displayModelShortName(): String {
    val trimmed = this.trim()
    return if (trimmed.contains("/")) trimmed.substringAfterLast("/") else trimmed
}

internal fun String.shortModelLabel(): String {
    val clean = this.displayModelShortName()
    if (clean.isBlank()) return "选择模型"
    return when {
        clean.length <= 18 -> clean
        else -> clean.take(8) + "..." + clean.takeLast(7)
    }
}

internal fun String.imageSupportOverride(): Boolean? = when (this) {
    "text" -> false
    "multimodal" -> true
    else -> null
}

internal fun ChatModelOption.sameModelOption(
    current: ChatModelOption?,
    fallbackModel: String
): Boolean {
    return current?.let {
        apiConfigId == it.apiConfigId && modelName == it.modelName
    } ?: modelName == fallbackModel
}

@Composable
fun AttachmentPreview(
    attachment: Attachment,
    onRemove: () -> Unit,
    readableBackdrop: Color = Color.Unspecified
) {
    val isImage = FileUtils.isImage(attachment.mimeType, attachment.name)
    val hasOcr = !attachment.ocrText.isNullOrBlank() || attachment.processingNote?.contains("OCR") == true
    val resolvedReadableBackdrop = readableBackdrop.takeOrElse {
        MaterialTheme.colorScheme.background
    }
    val glass = echoGlassPalette()
    val attachmentTint = glass.control
    val attachmentTextColor = readableTextColorFor(
        background = attachmentTint,
        fallbackSurface = resolvedReadableBackdrop
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = attachmentTint
        ),
        border = BorderStroke(1.dp, glass.outline),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    hasOcr -> Icons.Default.DocumentScanner
                    isImage -> Icons.Default.Image
                    else -> Icons.Default.AttachFile
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = attachmentTextColor,
                    maxLines = 1
                )
                Text(
                    text = listOfNotNull(
                        FileUtils.formatFileSize(attachment.size),
                        attachment.processingNote
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = attachmentTextColor.copy(alpha = 0.72f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "移除",
                    modifier = Modifier.size(14.dp),
                    tint = attachmentTextColor
                )
            }
        }
    }
}

@Composable
fun EmptyChatPlaceholder(
    isRoleplay: Boolean = false,
    characterName: String? = null,
    scenarioTitle: String? = null,
    characterAvatarUri: String? = null,
    onTriggerOpening: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isRoleplay) 36.dp else 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isRoleplay) {
            val context = LocalContext.current
            val customAvatarBitmap = remember(characterAvatarUri) {
                characterAvatarUri?.let { uriStr ->
                    runCatching {
                        val uri = Uri.parse(uriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            android.graphics.BitmapFactory.decodeStream(stream)
                        }
                    }.getOrNull()
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                if (customAvatarBitmap != null) {
                    Image(
                        bitmap = customAvatarBitmap.asImageBitmap(),
                        contentDescription = "角色头像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = characterName ?: "角色扮演",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!scenarioTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "剧本：$scenarioTitle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "点击下方按钮让角色主动开场，或者直接输入剧情提示开始故事。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onTriggerOpening?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "让角色开场",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(18.dp)
                        .size(42.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "准备开始",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "有什么想法，直接开始吧。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
