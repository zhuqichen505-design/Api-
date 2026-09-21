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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Long) -> Unit = {},
    onNavigateToRoleplayMemory: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val chatBackgroundBitmap = remember(context) {
        BackgroundImageManager.getChatBackgroundBitmap(context)
    }
    val scope = rememberCoroutineScope()
    val viewModel: ChatViewModel = viewModel(
        key = "chat_$conversationId",
        factory = ChatViewModel.factory(conversationId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentResponse by viewModel.currentResponse.collectAsState()
    val currentThinking by viewModel.currentThinking.collectAsState()
    val error by viewModel.error.collectAsState()
    val availableModelOptions by viewModel.availableModelOptions.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val currentModelOption by viewModel.currentModelOption.collectAsState()
    val tempSettings by viewModel.tempSettings.collectAsState()
    val contextUsage by viewModel.contextUsage.collectAsState()
    val messageModelMap by viewModel.messageModelMap.collectAsState()
    val pendingMemoryCandidate by viewModel.pendingMemoryCandidate.collectAsState()
    val reconnectStatus by viewModel.reconnectStatus.collectAsState()

    val hazeState = rememberEchoHazeState()
    val readableBackdrops = rememberReadableBackdropColors(chatBackgroundBitmap)
    val listState = rememberLazyListState()
    val scrollControlsVisibilityState = rememberLazyListControlsVisible(listState)
    val showScrollControls by scrollControlsVisibilityState
    val clipboardManager = LocalClipboardManager.current
    val promptTemplates by viewModel.promptTemplates.collectAsState()
    val translatingMessageIds by viewModel.translatingMessageIds.collectAsState()
    val sessionMemories by viewModel.sessionMemories.collectAsState()
    val isReconcilingTimeline by viewModel.isReconcilingTimeline.collectAsState()
    val timelineReconcileProgress by viewModel.timelineReconcileProgress.collectAsState()
    val timelineReconcileResult by viewModel.timelineReconcileResult.collectAsState()
    val showTimelineReconcileDialog by viewModel.showTimelineReconcileDialog.collectAsState()
    val timelineUpdateNotice by viewModel.timelineUpdateNotice.collectAsState()

    val roleplayRepo = remember { com.aiassistant.AiAssistantApp.instance.roleplayRepository }
    val allAvailableCharacters by roleplayRepo.getAllCharacters().collectAsState(initial = emptyList())
    val allAvailableScenarios by roleplayRepo.getAllScenarios().collectAsState(initial = emptyList())

    var inputText by remember(conversationId) { mutableStateOf(ChatViewModel.getDraft(conversationId)) }
    var activeQuotedText by remember { mutableStateOf<String?>(null) }
    var messagePendingDelete by remember { mutableStateOf<Message?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(uiState.conversationTitle) { mutableStateOf(uiState.conversationTitle) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showConvertToRoleplayDialog by remember { mutableStateOf(false) }
    var showContextUsageDialog by remember { mutableStateOf(false) }
    var showRollingSummaryDialog by remember { mutableStateOf(false) }
    var showStoryManagerDialog by remember { mutableStateOf(false) }
    var showStorySmartAnalyzeDialog by remember { mutableStateOf(false) }
    var showPlotActionDialog by remember { mutableStateOf(false) }
    var showThinkingPopover by remember { mutableStateOf(false) }
    var selectedAttachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var isProcessingAttachments by remember { mutableStateOf(false) }
    var attachmentStatus by remember { mutableStateOf<String?>(null) }
    var modelAvatarRevision by remember { mutableIntStateOf(0) }
    var pendingEditSource by remember { mutableStateOf<Message?>(null) }
    var preserveScrollForBranchGeneration by remember { mutableStateOf(false) }
    var streamingBranchGroupId by remember { mutableStateOf<String?>(null) }
    var autoFollowOutput by remember { mutableStateOf(true) }
    var isBarsHidden by remember { mutableStateOf(false) }
    var branchSuccessDialog by remember { mutableStateOf<BranchSuccessDialogState?>(null) }
    val messageQueue by viewModel.messageQueue.collectAsState()
    val isQueuePaused by viewModel.isQueuePaused.collectAsState()
    var editingQueueItem by remember { mutableStateOf<QueuedMessage?>(null) }
    var editingAssistantMessage by remember { mutableStateOf<Message?>(null) }
    var editingAssistantContent by remember { mutableStateOf("") }

    var lastStreamScrollAt by remember { mutableLongStateOf(0L) }
    val variantSelections = remember { mutableStateMapOf<String, Int>() }
    val variantSelectionSnapshot = variantSelections.toMap()
    val displayMessages = remember(messages, variantSelectionSnapshot) {
        buildDisplayMessages(messages, variantSelectionSnapshot)
    }
    val chatNavItems = remember(displayMessages) {
        buildChatAnchorItems(displayMessages)
    }

    DisposableEffect(conversationId) {
        onDispose {
            ChatViewModel.saveDraft(conversationId, inputText)
        }
    }

    BackHandler {
        when {
            isBarsHidden -> {
                isBarsHidden = false
            }
            showThinkingPopover -> {
                showThinkingPopover = false
            }
            else -> {
                viewModel.leaveConversation(onNavigateBack)
            }
        }
    }

    fun addAttachments(uris: List<Uri>, forceOcr: Boolean = false) {
        if (uris.isEmpty()) return
        scope.launch {
            isProcessingAttachments = true
            attachmentStatus = "正在处理附件..."
            val modelName = currentModel ?: uiState.modelName
            val supportsImageOverride = currentModelOption?.capability?.imageSupportOverride()
            val newAttachments = uris.mapNotNull { uri ->
                FileUtils.prepareAttachment(
                    context = context,
                    uri = uri,
                    modelName = modelName,
                    forceOcr = forceOcr,
                    supportsImageInputOverride = supportsImageOverride
                )
            }
            selectedAttachments = selectedAttachments + newAttachments
            attachmentStatus = when {
                newAttachments.isEmpty() -> "附件处理失败"
                newAttachments.any { it.processingNote?.contains("OCR") == true } -> "已添加 ${newAttachments.size} 个附件，图片已OCR"
                else -> "已添加 ${newAttachments.size} 个附件"
            }
            isProcessingAttachments = false
        }
    }

    // 通用文件选择器（支持所有类型）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        addAttachments(uris)
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        addAttachments(uris)
    }

    val ocrImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        addAttachments(uris, forceOcr = true)
    }

    var prevMessagesCount by remember { mutableIntStateOf(messages.size) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val total = layoutInfo.totalItemsCount
            val atBottom = if (total == 0 || visibleItems.isEmpty()) {
                true
            } else {
                val lastItem = visibleItems.last()
                lastItem.index >= total - 1
            }
            Pair(listState.isScrollInProgress, atBottom)
        }.collect { (isScrolling, atBottom) ->
            if (isScrolling) {
                autoFollowOutput = atBottom
            } else if (atBottom) {
                autoFollowOutput = true
            }
        }
    }

    var hasInitialScrolledToBottom by remember(conversationId) { mutableStateOf(false) }
    LaunchedEffect(conversationId, displayMessages.size) {
        if (!hasInitialScrolledToBottom && displayMessages.isNotEmpty()) {
            hasInitialScrolledToBottom = true
            kotlinx.coroutines.yield()
            val lastIdx = displayMessages.size - 1
            try {
                listState.scrollToItem(lastIdx, scrollOffset = 100000)
            } catch (_: Exception) {}
            kotlinx.coroutines.delay(80)
            try {
                listState.scrollToItem(lastIdx, scrollOffset = 100000)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(messages.size) {
        val prev = prevMessagesCount
        prevMessagesCount = messages.size
        if (messages.size <= prev) {
            return@LaunchedEffect
        }
        if (autoFollowOutput && !preserveScrollForBranchGeneration && !listState.isScrollInProgress) {
            val totalCount = listState.layoutInfo.totalItemsCount
            if (totalCount > 0) {
                try {
                    listState.scrollToItem((totalCount - 1).coerceAtLeast(0), scrollOffset = 100000)
                } catch (_: Exception) {}
            }
        }
    }

    var lastStreamScrollTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(currentResponse.length, currentThinking.length, isGenerating) {
        if (preserveScrollForBranchGeneration || !autoFollowOutput || listState.isScrollInProgress) {
            return@LaunchedEffect
        }
        val isStreaming = isGenerating && (currentResponse.isNotEmpty() || currentThinking.isNotEmpty())
        if (isStreaming) {
            val now = System.currentTimeMillis()
            if (now - lastStreamScrollTime < 70L) {
                return@LaunchedEffect
            }
            lastStreamScrollTime = now
            val totalCount = listState.layoutInfo.totalItemsCount
            if (totalCount > 0) {
                val targetIndex = (totalCount - 1).coerceAtLeast(0)
                try {
                    listState.scrollToItem(targetIndex, scrollOffset = 100000)
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(isGenerating) {
        if (!isGenerating) {
            streamingBranchGroupId = null
            if (autoFollowOutput && !preserveScrollForBranchGeneration && !listState.isScrollInProgress) {
                kotlinx.coroutines.delay(40)
                val totalCount = listState.layoutInfo.totalItemsCount
                if (totalCount > 0) {
                    val targetIndex = (totalCount - 1).coerceAtLeast(0)
                    try {
                        listState.scrollToItem(targetIndex, scrollOffset = 100000)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    LaunchedEffect(conversationId) {
        streamingBranchGroupId = null
        pendingEditSource = null
    }

    val systemClipboard = LocalClipboardManager.current
    val inAppClipboard = remember(systemClipboard) {
        com.aiassistant.ui.components.InAppSelectionClipboardManager(systemClipboard)
    }
    val textToolbar = remember { EchoTextToolbar() }
    CompositionLocalProvider(
        LocalTextToolbar provides textToolbar,
        LocalClipboardManager provides inAppClipboard
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 全屏背景与全屏毛玻璃源 (涵盖从顶到底全部区域，包括 bottomBar 与 topBar 背后)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background)
                    .echoHazeSource(hazeState)
            ) {
                chatBackgroundBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                topBar = {},
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = pendingMemoryCandidate != null,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
                ) {
                    pendingMemoryCandidate?.let { candidate ->
                        EchoGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            shape = EchoTokens.Radius.shapeMd
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "智能识别记忆候选 (需主动确认)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissPendingMemory() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "忽略",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "「${candidate.distilledContent}」",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = { viewModel.dismissPendingMemory() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("忽略", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.acceptPendingMemory("session") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("仅本会话生效", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { viewModel.acceptPendingMemory("user") },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("存为跨会话长期记忆", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                // 时间线自动增量更新提醒胶囊
                AnimatedVisibility(
                    visible = timelineUpdateNotice != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    timelineUpdateNotice?.let { notice ->
                        EchoGlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                            shape = EchoTokens.Radius.shapeMd
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = notice,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            viewModel.dismissTimelineUpdateNotice()
                                            showSettingsDialog = true
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("查看", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissTimelineUpdateNotice() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "关闭",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 需求 6：模型回复时排队消息悬浮卡片 (UI 严格按照 media_1789390149204.png 设计落地)
                AnimatedVisibility(
                    visible = messageQueue.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    MessageQueueCard(
                        queue = messageQueue,
                        isPaused = isQueuePaused,
                        onTogglePause = { viewModel.toggleQueuePause() },
                        onRecall = { id ->
                            val recalled = viewModel.recallQueuedMessage(id)
                            if (recalled != null) {
                                inputText = if (inputText.isBlank()) recalled.content else "$inputText\n${recalled.content}"
                                if (recalled.attachments.isNotEmpty()) {
                                    selectedAttachments = selectedAttachments + recalled.attachments
                                }
                            }
                        },
                        onEdit = { msg -> editingQueueItem = msg },
                        onRemove = { id -> viewModel.removeQueuedMessage(id) },
                        onMove = { from, to -> viewModel.moveQueuedMessage(from, to) }
                    )
                }

                ChatInputBar(
                    hazeState = hazeState,
                    inputText = inputText,
                    onInputChange = {
                        inputText = it
                        ChatViewModel.saveDraft(conversationId, it)
                    },
                    quotedText = activeQuotedText,
                    onClearQuote = { activeQuotedText = null },
                    onSend = {
                        val trimmedInput = inputText.trim()
                        val hasContent = trimmedInput.isNotBlank() || selectedAttachments.isNotEmpty() || !activeQuotedText.isNullOrBlank()
                        if (hasContent && !isProcessingAttachments) {
                            val finalPrompt = if (!activeQuotedText.isNullOrBlank()) {
                                val quoteBlock = activeQuotedText!!.trim().lines().joinToString("\n") { "> $it" }
                                if (trimmedInput.isNotBlank()) {
                                    "$quoteBlock\n\n针对以上内容：\n$trimmedInput"
                                } else {
                                    "$quoteBlock\n\n针对以上内容："
                                }
                            } else {
                                inputText
                            }
                            val editSource = pendingEditSource
                            if (editSource != null) {
                                preserveScrollForBranchGeneration = true
                                val targetAssistantGroupId = editSource.variantGroupId
                                    ?.let { pairedVariantGroupId(it) }
                                    ?: "turn_${editSource.id}_assistant"
                                streamingBranchGroupId = targetAssistantGroupId
                                val userGroupId = editSource.variantGroupId ?: "turn_${editSource.id}_user"
                                val targetIndex = displayMessages.indexOfFirst {
                                    it.message.id == editSource.id || it.groupId == userGroupId
                                }
                                if (targetIndex >= 0) {
                                    scope.launch {
                                        try {
                                            listState.animateScrollToItem(targetIndex)
                                        } catch (_: Exception) {}
                                    }
                                }
                                viewModel.sendEditedMessage(editSource, finalPrompt, selectedAttachments)
                            } else {
                                preserveScrollForBranchGeneration = false
                                streamingBranchGroupId = null
                                autoFollowOutput = true
                                viewModel.sendMessage(finalPrompt, selectedAttachments)
                            }
                            inputText = ""
                            activeQuotedText = null
                            ChatViewModel.saveDraft(conversationId, "")
                            pendingEditSource = null
                            selectedAttachments = emptyList()
                            attachmentStatus = null
                        }
                    },
                    isGenerating = isGenerating,
                    onStopGeneration = { viewModel.stopGeneration() },
                    attachments = selectedAttachments,
                    onRemoveAttachment = { attachment ->
                        selectedAttachments = selectedAttachments.filter { it != attachment }
                    },
                    isProcessingAttachments = isProcessingAttachments,
                    attachmentStatus = attachmentStatus,
                    onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                    onPickImage = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    onOcrImages = {
                        attachmentStatus = "请选择需要OCR的图片"
                        ocrImagePickerLauncher.launch(arrayOf("image/*"))
                    },
                    enableWebSearch = tempSettings.enableWebSearch,
                    onWebSearchChange = { enabled ->
                        viewModel.updateTempSettings(tempSettings.copy(enableWebSearch = enabled))
                    },
                    enableThinking = tempSettings.enableThinking,
                    thinkingEffort = tempSettings.thinkingEffort,
                    onThinkingChange = { enabled, effort ->
                        viewModel.updateTempSettings(tempSettings.copy(enableThinking = enabled, thinkingEffort = effort))
                    },
                    showThinkingPopover = showThinkingPopover,
                    onThinkingPopoverChange = { showThinkingPopover = it },
                    isRoleplay = uiState.isRoleplay,
                    onPlotActionClick = { showPlotActionDialog = true },
                    readableBackdrop = readableBackdrops.bottom,
                    modelName = currentModelOption?.modelName ?: currentModel ?: uiState.modelName,
                    isBarsHidden = isBarsHidden,
                    onBarsHiddenChange = { isBarsHidden = it }
                )
            }
        }
) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val topFloatingBarHeight = 68.dp + (if (error != null) 60.dp else 0.dp)

            // 1. 底层：全屏贯通的消息列表，向上滚动时平滑穿透悬浮顶栏与错误提示
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 消息列表 (全屏延伸，向上滚动时平滑穿透悬浮工具栏和报错弹窗，被毛玻璃实时模糊)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = statusBarTopPadding + topFloatingBarHeight,
                        bottom = paddingValues.calculateBottomPadding() + 18.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 空状态
                    if (displayMessages.isEmpty() && currentResponse.isEmpty() && currentThinking.isEmpty()) {
                        item {
                            EmptyChatPlaceholder(
                                isRoleplay = uiState.isRoleplay,
                                characterName = uiState.roleplayCharacter?.name,
                                scenarioTitle = uiState.roleplayScenario?.name,
                                characterAvatarUri = uiState.roleplayCharacter?.avatarUri,
                                onTriggerOpening = { viewModel.triggerCharacterOpening() }
                            )
                        }
                    }

                    // 消息列表
                    val currentAssistantModelName = currentModelOption?.modelName ?: currentModel ?: uiState.modelName ?: "AI"
                    itemsIndexed(
                        items = displayMessages,
                        key = { _, item -> item.groupId ?: "${item.message.id}_${item.message.createdAt}_${item.message.role}" }
                    ) { index, displayItem ->
                        val message = displayItem.message
                        val isUser = message.role == "user"
                        val prevItem = if (index > 0) displayMessages.getOrNull(index - 1) else null
                        val isPrevAssistant = prevItem?.message?.role == "assistant"
                        // 加大用户输入气泡和模型上一次输出之间的距离
                        val extraTopSpacing = if (isUser && isPrevAssistant) 18.dp else 0.dp

                        val resolvedAssistantModelName = messageModelMap[message.id]
                            ?: messageModelMap[message.createdAt]
                            ?: currentAssistantModelName
                        val isBranchStreamingHere = streamingBranchGroupId != null &&
                            displayItem.groupId == streamingBranchGroupId &&
                            (isGenerating || currentResponse.isNotEmpty() || currentThinking.isNotEmpty())

                        val totalVariantsWithStreaming = if (isBranchStreamingHere) {
                            (displayItem.variantInfo?.total ?: 1) + 1
                        } else {
                            displayItem.variantInfo?.total ?: 1
                        }
                        val selectedVariantIndex = if (isBranchStreamingHere) {
                            variantSelections[streamingBranchGroupId!!] ?: totalVariantsWithStreaming
                        } else {
                            displayItem.variantInfo?.currentIndex ?: 1
                        }
                        val showStreamingBubbleHere = isBranchStreamingHere && selectedVariantIndex == totalVariantsWithStreaming

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = extraTopSpacing)
                        ) {
                            if (showStreamingBubbleHere) {
                                // 在原位以正在生成的最新版本渲染
                                MessageBubble(
                                    message = Message(
                                        conversationId = conversationId,
                                        role = "assistant",
                                        content = currentResponse,
                                        thinkingContent = currentThinking.ifEmpty { null },
                                        variantGroupId = streamingBranchGroupId,
                                        variantIndex = totalVariantsWithStreaming
                                    ),
                                    hazeState = hazeState,
                                    readableBackdrop = readableBackdrops.content,
                                    isGenerating = true,
                                    assistantAvatarRevision = modelAvatarRevision,
                                    assistantApiConfigId = currentModelOption?.apiConfigId,
                                    assistantModelName = currentAssistantModelName,
                                    reconnectStatus = reconnectStatus,
                                    variantInfo = VariantInfo(
                                        groupId = streamingBranchGroupId!!,
                                        currentIndex = totalVariantsWithStreaming,
                                        total = totalVariantsWithStreaming,
                                        availableIndices = (displayItem.variantInfo?.availableIndices ?: listOf(1)) + totalVariantsWithStreaming
                                    ),
                                    onVariantSelected = { groupId, index ->
                                        preserveScrollForBranchGeneration = true
                                        autoFollowOutput = false
                                        variantSelections[groupId] = index
                                        pairedVariantGroupId(groupId)?.let { pairedGroup ->
                                            variantSelections[pairedGroup] = index
                                        }
                                    },
                                    translatingThinking = false,
                                    onTranslateThinking = null,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(currentResponse))
                                    },
                                    onCopyThinking = {
                                        clipboardManager.setText(AnnotatedString(currentThinking))
                                    },
                                    onQuote = if (currentResponse.isNotBlank()) {
                                        {
                                            val quoteBlock = currentResponse.lines().joinToString("\n") { line -> "> $line" } + "\n针对以上内容：\n"
                                            inputText = if (inputText.isBlank()) quoteBlock else "$inputText\n\n$quoteBlock"
                                        }
                                    } else null,
                                    customAvatarUri = uiState.roleplayCharacter?.avatarUri ?: uiState.modelAvatarUri
                                )
                            } else {
                                val dynamicVariantInfo = if (isBranchStreamingHere) {
                                    VariantInfo(
                                        groupId = streamingBranchGroupId!!,
                                        currentIndex = selectedVariantIndex,
                                        total = totalVariantsWithStreaming,
                                        availableIndices = (displayItem.variantInfo?.availableIndices ?: listOf(1)) + totalVariantsWithStreaming
                                    )
                                } else {
                                    displayItem.variantInfo
                                }
                                MessageBubble(
                                    message = message,
                                    hazeState = hazeState,
                                    readableBackdrop = readableBackdrops.content,
                                    assistantAvatarRevision = modelAvatarRevision,
                                    assistantApiConfigId = currentModelOption?.apiConfigId,
                                    assistantModelName = resolvedAssistantModelName,
                                    variantInfo = dynamicVariantInfo,
                                    onVariantSelected = { groupId, index ->
                                        preserveScrollForBranchGeneration = true
                                        autoFollowOutput = false
                                        variantSelections[groupId] = index
                                        pairedVariantGroupId(groupId)?.let { pairedGroup ->
                                            if (messages.any { it.variantGroupId == pairedGroup && it.variantIndex == index }) {
                                                variantSelections[pairedGroup] = index
                                            }
                                        }
                                    },
                                    translatingThinking = translatingMessageIds.contains(message.id),
                                    onTranslateThinking = { msg -> viewModel.translateMessageThinking(msg) },
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(message.content))
                                    },
                                    onCopyThinking = {
                                        message.thinkingContent?.let {
                                            clipboardManager.setText(AnnotatedString(it))
                                        }
                                    },
                                    onQuote = if (message.role == "user" && message.content.isNotBlank()) {
                                        {
                                            activeQuotedText = message.content.trim()
                                        }
                                    } else null,
                                    onBranch = if (!isGenerating && message.role == "assistant" && message.id > 0) {
                                        {
                                            val targetIdx = displayMessages.indexOfFirst { it.message.id == message.id }
                                            val messagesToBranch = if (targetIdx >= 0) {
                                                displayMessages.take(targetIdx + 1).map { it.message }
                                            } else {
                                                null
                                            }
                                            viewModel.createBranch(message.id, messagesToBranch) { newId, branchTitle ->
                                                branchSuccessDialog = BranchSuccessDialogState(newId, branchTitle)
                                            }
                                        }
                                    } else null,
                                    onRegenerate = if (message.role == "assistant" && message == messages.lastOrNull { it.role == "assistant" }) {
                                        {
                                            preserveScrollForBranchGeneration = true
                                            autoFollowOutput = false
                                            streamingBranchGroupId = message.variantGroupId ?: "reply_${message.id}"
                                            viewModel.regenerateLastMessage()
                                        }
                                    } else null,
                                    onEdit = {
                                        if (message.role == "user") {
                                            val parsed = parseQuotedMessage(message.content)
                                            if (parsed != null) {
                                                activeQuotedText = parsed.quoteText
                                                inputText = parsed.replyText
                                            } else {
                                                inputText = message.content
                                                activeQuotedText = null
                                            }
                                            pendingEditSource = message
                                            autoFollowOutput = false
                                            selectedAttachments = emptyList()
                                            attachmentStatus = null
                                        } else {
                                            editingAssistantMessage = message
                                            editingAssistantContent = message.content
                                        }
                                    },
                                    onDelete = {
                                        messagePendingDelete = message
                                    },
                                    customAvatarUri = uiState.roleplayCharacter?.avatarUri ?: uiState.modelAvatarUri,
                                    onTogglePin = { msg -> viewModel.togglePinMessage(msg) },
                                    onToggleExclude = { msg -> viewModel.toggleExcludeMessage(msg) }
                                )
                            }

                            // 如果该轮还没有已入库的 assistant 消息，但在对应 user 消息后正在流式生成
                            val hasAssistantItemForThisTurn = displayMessages.any { it.groupId == streamingBranchGroupId }
                            if (
                                !hasAssistantItemForThisTurn &&
                                streamingBranchGroupId != null &&
                                (displayItem.groupId == pairedVariantGroupId(streamingBranchGroupId!!) ||
                                 streamingBranchGroupId!!.startsWith("turn_${displayItem.message.id}_")) &&
                                (isGenerating || currentResponse.isNotEmpty() || currentThinking.isNotEmpty())
                            ) {
                                Spacer(modifier = Modifier.height(14.dp))
                                MessageBubble(
                                    message = Message(
                                        conversationId = conversationId,
                                        role = "assistant",
                                        content = currentResponse,
                                        thinkingContent = currentThinking.ifEmpty { null },
                                        variantGroupId = streamingBranchGroupId
                                    ),
                                    hazeState = hazeState,
                                    readableBackdrop = readableBackdrops.content,
                                    isGenerating = true,
                                    assistantAvatarRevision = modelAvatarRevision,
                                    assistantApiConfigId = currentModelOption?.apiConfigId,
                                    assistantModelName = currentAssistantModelName,
                                    reconnectStatus = reconnectStatus,
                                    translatingThinking = false,
                                    onTranslateThinking = null,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(currentResponse))
                                    },
                                    onCopyThinking = {
                                        clipboardManager.setText(AnnotatedString(currentThinking))
                                    },
                                    customAvatarUri = uiState.roleplayCharacter?.avatarUri ?: uiState.modelAvatarUri
                                )
                            }
                        }
                    }

                    // 当前正在生成的内容
                    if (streamingBranchGroupId == null && (currentThinking.isNotEmpty() || currentResponse.isNotEmpty() || isGenerating)) {
                        item(key = "streaming_assistant_message") {
                            MessageBubble(
                                message = Message(
                                    conversationId = conversationId,
                                    role = "assistant",
                                    content = currentResponse,
                                    thinkingContent = currentThinking.ifEmpty { null }
                                ),
                                hazeState = hazeState,
                                readableBackdrop = readableBackdrops.content,
                                isGenerating = true,
                                assistantAvatarRevision = modelAvatarRevision,
                                assistantApiConfigId = currentModelOption?.apiConfigId,
                                assistantModelName = currentAssistantModelName,
                                reconnectStatus = reconnectStatus,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(currentResponse))
                                },
                                onCopyThinking = {
                                    clipboardManager.setText(AnnotatedString(currentThinking))
                                },
                                customAvatarUri = uiState.roleplayCharacter?.avatarUri ?: uiState.modelAvatarUri
                            )
                        }
                    }

                    item(key = "chat_bottom_anchor") {
                        Spacer(modifier = Modifier.height(1.dp))
                    }
                }
            }

            // 2. 顶部悬浮工具栏与错误提示：悬浮在最顶层，直接复用输入框相同 Surface + echoHazePanel 结构，无边缘包裹，半透明透字
            val toolbarShape = RoundedCornerShape(22.dp)
            val glass = echoGlassPalette()
            val toolbarTint = glass.input
            val toolbarContentColor = readableTextColorFor(
                background = toolbarTint,
                fallbackSurface = readableBackdrops.top
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                AnimatedContent(
                    targetState = isBarsHidden,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.8f, transformOrigin = TransformOrigin(0f, 0f), animationSpec = tween(280)))
                            .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 0f), animationSpec = tween(200)))
                    },
                    label = "topBarHiddenAnim"
                ) { hidden ->
                    if (hidden) {
                        val topPulseTransition = rememberInfiniteTransition(label = "topPulse")
                        val topPulseScale by topPulseTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "topPulseScale"
                        )
                        val topPulseAlpha by topPulseTransition.animateFloat(
                            initialValue = 0.55f,
                            targetValue = 0.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "topPulseAlpha"
                        )
                        val topPulseColor = MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .padding(start = 2.dp, top = 2.dp)
                                .size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val strokeWidth = 1.2.dp.toPx()
                                val baseRadius = (size.minDimension - strokeWidth) / 2f
                                val currentRadius = baseRadius * topPulseScale
                                drawCircle(
                                    color = topPulseColor.copy(alpha = topPulseAlpha),
                                    radius = currentRadius,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )
                            }
                            Surface(
                                onClick = { isBarsHidden = false },
                                shape = CircleShape,
                                color = glass.control,
                                contentColor = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(1.2.dp, glass.outlineSelected),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "取消隐藏并恢复顶部栏",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .echoHazePanel(
                                    hazeState = hazeState,
                                    shape = toolbarShape,
                                    tint = toolbarTint,
                                    blurRadius = 16.dp,
                                    highlightAlpha = 0.025f
                                ),
                            shape = toolbarShape,
                            color = Color.Transparent,
                            contentColor = toolbarContentColor,
                            border = BorderStroke(1.dp, glass.outline),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.leaveConversation(onNavigateBack) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                                }
                                ChatHeaderTitle(
                                    title = uiState.conversationTitle.ifBlank { "新对话" },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    onLongClick = {
                                        renameText = uiState.conversationTitle
                                        showRenameDialog = true
                                    }
                                )
                                ContextUsageButton(
                                    usage = contextUsage.usage,
                                    canCompress = contextUsage.usage?.canCompress == true,
                                    onClick = {
                                        viewModel.refreshContextUsage()
                                        showContextUsageDialog = true
                                    }
                                )
                                if (uiState.isRoleplay) {
                                    IconButton(
                                        onClick = { showStoryManagerDialog = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AutoStories,
                                            contentDescription = "故事创作与参数设置",
                                            tint = toolbarContentColor
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { showSettingsDialog = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = "对话设置",
                                            tint = toolbarContentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                error?.let { errorMsg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val errorShape = RoundedCornerShape(22.dp)
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val errorTint = if (isDark) Color(0xFF3F1D23).copy(alpha = 0.88f) else Color(0xFFFFF1F2).copy(alpha = 0.92f)
                        val errorBorder = if (isDark) Color(0xFFF43F5E).copy(alpha = 0.35f) else Color(0xFFFDA4AF).copy(alpha = 0.65f)
                        val errorContentColor = if (isDark) Color(0xFFFFE4E6) else Color(0xFF9F1239)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .echoHazePanel(
                                    hazeState = hazeState,
                                    shape = errorShape,
                                    tint = errorTint,
                                    blurRadius = 16.dp,
                                    highlightAlpha = 0.025f
                                ),
                            shape = errorShape,
                            color = Color.Transparent,
                            contentColor = errorContentColor,
                            border = BorderStroke(1.dp, errorBorder),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = errorContentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearError() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = errorContentColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                contextUsage.statusMessage?.let { statusMsg ->
                    Spacer(modifier = Modifier.height(6.dp))
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        val isWarning = statusMsg.startsWith("⚠️")
                        val isSuccess = statusMsg.startsWith("✅")
                        val infoShape = RoundedCornerShape(22.dp)
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val tintColor = when {
                            isWarning -> if (isDark) Color(0xFF422006).copy(alpha = 0.90f) else Color(0xFFFEFCE8).copy(alpha = 0.92f)
                            isSuccess -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.88f) else Color(0xFFECFDF5).copy(alpha = 0.92f)
                            else -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.88f) else Color(0xFFF1F5F9).copy(alpha = 0.92f)
                        }
                        val borderColor = when {
                            isWarning -> if (isDark) Color(0xFFF59E0B).copy(alpha = 0.40f) else Color(0xFFFCD34D).copy(alpha = 0.70f)
                            isSuccess -> if (isDark) Color(0xFF10B981).copy(alpha = 0.40f) else Color(0xFF6EE7B7).copy(alpha = 0.70f)
                            else -> if (isDark) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFBAE6FD).copy(alpha = 0.65f)
                        }
                        val bannerContentColor = when {
                            isWarning -> if (isDark) Color(0xFFFEF08A) else Color(0xFF854D0E)
                            isSuccess -> if (isDark) Color(0xFFA7F3D0) else Color(0xFF065F46)
                            else -> if (isDark) Color(0xFFE0F2FE) else Color(0xFF0369A1)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .echoHazePanel(
                                    hazeState = hazeState,
                                    shape = infoShape,
                                    tint = tintColor,
                                    blurRadius = 16.dp,
                                    highlightAlpha = 0.025f
                                ),
                            shape = infoShape,
                            color = Color.Transparent,
                            contentColor = bannerContentColor,
                            border = BorderStroke(1.dp, borderColor),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (contextUsage.isCompressing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = bannerContentColor
                                    )
                                } else {
                                    Icon(
                                        if (isWarning) Icons.Default.Warning else if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = bannerContentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = statusMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bannerContentColor,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearContextStatusMessage() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = bannerContentColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            TransientLazyListScrollbar(
                listState = listState,
                visible = showScrollControls,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )

            SideAnchorNavigator(
                items = chatNavItems,
                listState = listState,
                visible = showScrollControls,
                hazeState = hazeState,
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 4.dp)
            )

            ChatScrollJumpButtons(
                visible = showScrollControls && listState.layoutInfo.totalItemsCount > 1,
                onJumpToTop = {
                    autoFollowOutput = false
                    scrollControlsVisibilityState.extendVisibility(2800L)
                    scope.launch {
                        val firstVisible = listState.firstVisibleItemIndex
                        if (firstVisible > 8) {
                            listState.scrollToItem(6)
                        }
                        listState.animateScrollToItem(0)
                    }
                },
                onJumpToPrevInput = {
                    autoFollowOutput = false
                    scrollControlsVisibilityState.extendVisibility(2800L)
                    scope.launch {
                        val currentFirst = listState.firstVisibleItemIndex
                        val target = displayMessages.indices.reversed().firstOrNull { idx ->
                            idx < currentFirst && displayMessages[idx].message.role == "user"
                        } ?: 0
                        listState.animateScrollToItem(target)
                    }
                },
                onJumpToNextInput = {
                    autoFollowOutput = false
                    scrollControlsVisibilityState.extendVisibility(2800L)
                    scope.launch {
                        val currentFirst = listState.firstVisibleItemIndex
                        val target = displayMessages.indices.firstOrNull { idx ->
                            idx > currentFirst && displayMessages[idx].message.role == "user"
                        } ?: (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        listState.animateScrollToItem(target)
                    }
                },
                onJumpToBottom = {
                    autoFollowOutput = true
                    scrollControlsVisibilityState.extendVisibility(2800L)
                    scope.launch {
                        val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        val firstVisible = listState.firstVisibleItemIndex
                        if (lastIndex - firstVisible > 8) {
                            listState.scrollToItem((lastIndex - 6).coerceAtLeast(0))
                        }
                        listState.animateScrollToItem(lastIndex)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 18.dp,
                        bottom = paddingValues.calculateBottomPadding() + 18.dp
                    )
            )

            if (showThinkingPopover) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            showThinkingPopover = false
                        }
                )
            }

            EchoTextToolbarHost(toolbar = textToolbar) { quotedText ->
                val clean = quotedText.trim()
                if (clean.isNotBlank()) {
                    activeQuotedText = clean
                }
            }
        }
    }
}
}

    // 删除消息二次确认对话框
    messagePendingDelete?.let { targetMsg ->
        val isUserMsg = targetMsg.role == "user"
        AlertDialog(
            onDismissRequest = { messagePendingDelete = null },
            title = {
                Text(
                    text = if (isUserMsg) "删除提问消息" else "删除 AI 回复",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("确定要删除此条${if (isUserMsg) "提问消息" else "AI 回复"}吗？删除后不可恢复。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        autoFollowOutput = false
                        // 锁定当前可见视口锚点，防止删除回复导致列表滑动或跳跃
                        val targetIdx = displayMessages.indexOfFirst { it.message.id == targetMsg.id }
                        val firstVisible = listState.firstVisibleItemIndex
                        val currentOffset = listState.firstVisibleItemScrollOffset
                        if (targetIdx >= 0) {
                            if (firstVisible == targetIdx) {
                                // 如果被删除的消息恰好是视口顶部首项，其 key 即将销毁。
                                // 将锚点平移到上一项 (通常为对应的提问消息)；若无上一项则锚定下一项
                                if (targetIdx > 0) {
                                    val prevItemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIdx - 1 }
                                    val offset = if (prevItemInfo != null) (-prevItemInfo.offset).coerceAtLeast(0) else 0
                                    scope.launch {
                                        try {
                                            listState.scrollToItem(targetIdx - 1, scrollOffset = offset)
                                        } catch (_: Exception) {}
                                    }
                                } else if (displayMessages.size > 1) {
                                    scope.launch {
                                        try {
                                            listState.scrollToItem(0, scrollOffset = 0)
                                        } catch (_: Exception) {}
                                    }
                                }
                            } else if (firstVisible > targetIdx) {
                                // 被删除的消息在视口上方，删除后其后所有项索引减 1，保持当前首项视觉位置不动
                                scope.launch {
                                    try {
                                        listState.scrollToItem((firstVisible - 1).coerceAtLeast(0), scrollOffset = currentOffset)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                        viewModel.deleteMessage(targetMsg)
                        messagePendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { messagePendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 设置对话框
    if (showSettingsDialog) {
        ChatSettingsDialog(
            hazeState = hazeState,
            tempSettings = tempSettings,
            currentPrompt = uiState.systemPrompt,
            currentOption = currentModelOption,
            fallbackModel = currentModel ?: uiState.modelName,
            availableOptions = availableModelOptions,
            templates = promptTemplates,
            sessionMemories = sessionMemories,
            isReconcilingTimeline = isReconcilingTimeline,
            reconcileTimelineProgress = timelineReconcileProgress,
            onStartTimelineReconciliation = { viewModel.startTimelineReconciliation() },
            onCancelTimelineReconciliation = { viewModel.cancelTimelineReconciliation() },
            onAddSessionMemory = { viewModel.addSessionMemory(it) },
            onUpdateSessionMemory = { viewModel.updateSessionMemory(it) },
            onToggleSessionMemory = { id, enabled -> viewModel.toggleSessionMemory(id, enabled) },
            onDeleteSessionMemory = { viewModel.deleteSessionMemory(it) },
            onClearSessionMemories = { viewModel.clearSessionMemories() },
            onTempSettingsChange = { viewModel.updateTempSettings(it) },
            onDismiss = { showSettingsDialog = false },
            onSave = { settings, prompt ->
                viewModel.updateChatSettings(settings, prompt)
                showSettingsDialog = false
            },
            onModelSelected = { viewModel.switchModel(it) },
            onSavePromptTemplate = { name, content ->
                viewModel.savePromptTemplate(name, content)
            },
            onModelAvatarChanged = { modelAvatarRevision++ },
            onConvertToRoleplay = {
                showSettingsDialog = false
                showConvertToRoleplayDialog = true
            },
            conversationId = conversationId,
            currentConversationModelAvatarUri = uiState.modelAvatarUri,
            onUpdateConversationModelAvatar = { uri ->
                viewModel.updateConversationModelAvatar(uri)
                modelAvatarRevision++
            }
        )
    }

    // 全量历史时间轴梳理与校对审核弹窗
    if (showTimelineReconcileDialog && timelineReconcileResult != null) {
        TimelineReconcileDialog(
            hazeState = hazeState,
            initialResult = timelineReconcileResult!!,
            onDismiss = { viewModel.dismissTimelineReconcileDialog() },
            onApply = { currentTime, events, confirmedSettings ->
                viewModel.applyReconciledTimeline(currentTime, events, confirmedSettings)
            }
        )
    }

    if (showConvertToRoleplayDialog) {
        var charName by remember { mutableStateOf((uiState.conversationTitle.ifBlank { "故事主角" }).take(10)) }
        var charIdentity by remember { mutableStateOf("核心探索者") }
        var charPersonality by remember { mutableStateOf("沉着、机智、性格鲜明") }
        var scenarioName by remember { mutableStateOf("${uiState.conversationTitle.take(8)} · 世界观") }

        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showConvertToRoleplayDialog = false },
            title = { Text("转为角色扮演 / 故事创作") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "将当前对话平滑升级为故事创作会话。自动建立独立角色卡与世界观，完整保留全部聊天历史，解锁剧情推进动作与独立记忆体系。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = charName,
                        onValueChange = { charName = it },
                        label = { Text("主角名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = charIdentity,
                        onValueChange = { charIdentity = it },
                        label = { Text("主角身份/职业") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = charPersonality,
                        onValueChange = { charPersonality = it },
                        label = { Text("主角性格特征") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scenarioName,
                        onValueChange = { scenarioName = it },
                        label = { Text("舞台/世界观名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.convertToRoleplay(
                        charName = charName,
                        charIdentity = charIdentity,
                        charPersonality = charPersonality,
                        scenarioName = scenarioName
                    ) {
                        showConvertToRoleplayDialog = false
                        Toast.makeText(context, "已成功升级为角色扮演故事", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("立即转换")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertToRoleplayDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (editingAssistantMessage != null) {
        EchoGlassDialog(
            onDismissRequest = { editingAssistantMessage = null },
            shape = EchoTokens.Radius.shapeXl,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("编辑模型回复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "直接修润、增删或调整模型的历史回复内容，修改后将直接持久化保存至当前会话历史。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editingAssistantContent,
                        onValueChange = { editingAssistantContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 340.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = glassTextFieldColors(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        )
                    )
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { editingAssistantMessage = null }) {
                        Text("取消", maxLines = 1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val msg = editingAssistantMessage ?: return@Button
                            viewModel.editAssistantMessage(msg.id, editingAssistantContent)
                            editingAssistantMessage = null
                        },
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("保存修改", maxLines = 1)
                    }
                }
            }
        )
    }

    if (showContextUsageDialog) {
        ContextUsageDialog(
            hazeState = hazeState,
            state = contextUsage,
            onDismiss = { showContextUsageDialog = false },
            onRefresh = { viewModel.refreshContextUsage() },
            onCompress = { viewModel.compressContextNow() },
            onGenerateRollingSummary = { viewModel.generateRollingSummaryNow() },
            onEditRollingSummary = { showRollingSummaryDialog = true }
        )
    }

    if (showRollingSummaryDialog) {
        val currentSummary = viewModel.getCurrentRollingSummary()
        RollingSummaryEditDialog(
            hazeState = hazeState,
            initialSummary = currentSummary,
            onDismiss = { showRollingSummaryDialog = false },
            onSave = { updatedSummary ->
                viewModel.updateRollingSummary(updatedSummary)
            },
            onClear = {
                viewModel.clearRollingSummary()
            }
        )
    }

    if (showStoryManagerDialog && uiState.roleplaySession != null) {
        StoryUnifiedSettingsDialog(
            hazeState = hazeState,
            session = uiState.roleplaySession!!,
            characters = uiState.roleplayCharacters,
            allCharacters = allAvailableCharacters,
            scenario = uiState.roleplayScenario,
            allScenarios = allAvailableScenarios,
            narrativeMode = uiState.narrativeMode,
            currentOption = currentModelOption,
            fallbackModel = currentModel ?: uiState.modelName,
            availableOptions = availableModelOptions,
            tempSettings = tempSettings,
            currentPrompt = uiState.systemPrompt,
            templates = promptTemplates,
            onDismiss = { showStoryManagerDialog = false },
            onSaveAll = { charIds, scenarioId, mode, plotSummary, settings, prompt ->
                viewModel.updateStorySessionContext(charIds, scenarioId, mode, plotSummary)
                viewModel.updateChatSettings(settings, prompt)
                showStoryManagerDialog = false
            },
            onPlotAction = { action, custom ->
                viewModel.sendPlotAction(action, custom)
                showStoryManagerDialog = false
            },
            onSummarizeMemories = {
                Toast.makeText(context, "正在提炼剧情摘要与关键事实...", Toast.LENGTH_SHORT).show()
                viewModel.summarizeAndExtractMemories(
                    onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                )
            },
            onNavigateToMemory = {
                showStoryManagerDialog = false
                onNavigateToRoleplayMemory(uiState.roleplaySession!!.id)
            },
            onOpenSmartAppend = {
                showStorySmartAnalyzeDialog = true
            },
            onConvertToNormal = {
                viewModel.convertToNormal {
                    showStoryManagerDialog = false
                    Toast.makeText(context, "已转为普通对话（设定已合并为系统提示词）", Toast.LENGTH_SHORT).show()
                }
            },
            onSaveLocalCharacter = { updatedChar ->
                viewModel.addNewLocalCharacter(updatedChar)
                Toast.makeText(context, "已保存「${updatedChar.name}」故事设定", Toast.LENGTH_SHORT).show()
            },
            onDeleteLocalCharacter = { char ->
                viewModel.deleteLocalCharacter(char)
                Toast.makeText(context, "已从故事中移除角色「${char.name}」", Toast.LENGTH_SHORT).show()
            },
            onSaveLocalScenario = { updatedSc ->
                viewModel.saveLocalScenario(updatedSc)
                Toast.makeText(context, "已保存「${updatedSc.name}」故事世界观设定", Toast.LENGTH_SHORT).show()
            },
            onDeleteLocalScenario = {
                viewModel.deleteLocalScenario()
                Toast.makeText(context, "已从故事中移除世界观设定", Toast.LENGTH_SHORT).show()
            },
            onModelSelected = { viewModel.switchModel(it) },
            onSavePromptTemplate = { name, content ->
                viewModel.savePromptTemplate(name, content)
            },
            onModelAvatarChanged = { modelAvatarRevision++ }
        )
    }

    if (showStorySmartAnalyzeDialog) {
        SmartAppendStoryDialog(
            hazeState = hazeState,
            onDismiss = { showStorySmartAnalyzeDialog = false },
            onAppendAndMerge = { chars, scenario, resMap ->
                showStorySmartAnalyzeDialog = false
                viewModel.appendAndMergeStoryBundle(chars, scenario, resMap) { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (editingQueueItem != null) {
        var draftText by remember(editingQueueItem) { mutableStateOf(editingQueueItem!!.content) }
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { editingQueueItem = null },
            title = { Text("编辑排队消息") },
            text = {
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    label = { Text("消息内容") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 240.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingQueueItem?.let {
                            viewModel.editQueuedMessage(it.id, draftText)
                        }
                        editingQueueItem = null
                    },
                    enabled = draftText.isNotBlank()
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQueueItem = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (uiState.suggestedProposal != null) {
        EditableSettingProposalDialog(
            hazeState = hazeState,
            proposal = uiState.suggestedProposal!!,
            onDismiss = { viewModel.dismissProposedSetting() },
            onApply = { chars, sc ->
                viewModel.applyProposedSetting(chars, sc)
                Toast.makeText(context, "已成功添加并融合到当前故事专属设定！", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPlotActionDialog) {
        com.aiassistant.ui.screens.roleplay.PlotActionDialog(
            hazeState = hazeState,
            onAction = { action, custom ->
                viewModel.sendPlotAction(action, custom)
            },
            onProposeSetting = {
                val textToAnalyze = if (inputText.isNotBlank()) inputText else {
                    messages.takeLast(4).joinToString("\n") { "${it.role}: ${it.content}" }
                }
                if (textToAnalyze.isNotBlank()) {
                    Toast.makeText(context, "正在结合故事已有设定进行精准分析...", Toast.LENGTH_SHORT).show()
                    viewModel.analyzeAndProposeSettingFromInput(
                        text = textToAnalyze,
                        onProgress = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                        onNoProposal = { Toast.makeText(context, "未能从当前输入中提取到实质性设定变动", Toast.LENGTH_SHORT).show() },
                        onError = { err -> Toast.makeText(context, "识别失败: $err", Toast.LENGTH_SHORT).show() }
                    )
                } else {
                    Toast.makeText(context, "请先在输入框输入设定或剧情文本", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showPlotActionDialog = false }
        )
    }

    if (showRenameDialog) {
        EchoGlassDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(if (uiState.isRoleplay) "重命名故事" else "重命名对话") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameConversation(renameText)
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank()
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    branchSuccessDialog?.let { dialogState ->
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { branchSuccessDialog = null },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.AltRoute,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "分支创建成功",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "已为您生成包含当前回复及之前完整上下文的新分支：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = dialogState.branchTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "您可以留在当前对话继续探索，或立即跳转到新分支对话。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newId = dialogState.newConversationId
                        branchSuccessDialog = null
                        onNavigateToChat(newId)
                    }
                ) {
                    Text("跳转到新对话")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { branchSuccessDialog = null }
                ) {
                    Text("确定")
                }
            }
        )
    }
}
