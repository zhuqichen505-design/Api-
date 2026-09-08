@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)

package com.aiassistant.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import com.aiassistant.domain.model.ToolCallRecord
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
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
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.MarkdownText
import com.aiassistant.ui.components.SideAnchorItem
import com.aiassistant.ui.components.SideAnchorNavigator
import com.aiassistant.ui.components.TransientLazyListScrollbar
import com.aiassistant.ui.components.EchoGlassDialog
import com.aiassistant.ui.components.EchoGlassDropdownMenu
import com.aiassistant.ui.components.echoFilterChipBorder
import com.aiassistant.ui.components.echoFilterChipColors
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

private const val ChatGlassTintAlpha = 0.86f
private val ChatUserGlassTint = Color(0xFFD9ECFF)
private val ThinkingContentBlue = Color(0xFF6BA4F8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToRoleplayMemory: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val chatBackgroundBitmap = remember(context) {
        BackgroundImageManager.getChatBackgroundBitmap(context)
    }
    val scope = rememberCoroutineScope()
    val viewModel: ChatViewModel = viewModel(
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

    val hazeState = rememberEchoHazeState()
    val readableBackdrops = rememberReadableBackdropColors(chatBackgroundBitmap)
    val listState = rememberLazyListState()
    val showScrollControls by rememberLazyListControlsVisible(listState)
    val clipboardManager = LocalClipboardManager.current
    val promptTemplates by viewModel.promptTemplates.collectAsState()
    val translatingMessageIds by viewModel.translatingMessageIds.collectAsState()

    val roleplayRepo = remember { com.aiassistant.AiAssistantApp.instance.roleplayRepository }
    val allAvailableCharacters by roleplayRepo.getAllCharacters().collectAsState(initial = emptyList())
    val allAvailableScenarios by roleplayRepo.getAllScenarios().collectAsState(initial = emptyList())

    var inputText by remember(conversationId) { mutableStateOf(ChatViewModel.getDraft(conversationId)) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(uiState.conversationTitle) { mutableStateOf(uiState.conversationTitle) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showConvertToRoleplayDialog by remember { mutableStateOf(false) }
    var showContextUsageDialog by remember { mutableStateOf(false) }
    var showStoryManagerDialog by remember { mutableStateOf(false) }
    var showStorySmartAnalyzeDialog by remember { mutableStateOf(false) }
    var showPlotActionDialog by remember { mutableStateOf(false) }
    var selectedAttachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var isProcessingAttachments by remember { mutableStateOf(false) }
    var attachmentStatus by remember { mutableStateOf<String?>(null) }
    var modelAvatarRevision by remember { mutableIntStateOf(0) }
    var pendingEditSource by remember { mutableStateOf<Message?>(null) }
    var preserveScrollForBranchGeneration by remember { mutableStateOf(false) }
    var streamingBranchGroupId by remember { mutableStateOf<String?>(null) }
    var autoFollowOutput by remember { mutableStateOf(true) }
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
        viewModel.leaveConversation(onNavigateBack)
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
            if (layoutInfo.totalItemsCount == 0 || visibleItems.isEmpty()) {
                true
            } else {
                val lastItem = visibleItems.last()
                val isLastItem = lastItem.index == layoutInfo.totalItemsCount - 1
                val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
                isLastItem && (lastItem.offset + lastItem.size <= viewportBottom + 80)
            }
        }.collect { atBottom ->
            if (listState.isScrollInProgress) {
                autoFollowOutput = atBottom
            }
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
                    listState.animateScrollToItem((totalCount - 1).coerceAtLeast(0))
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(currentResponse.length, currentThinking.length, isGenerating) {
        if (preserveScrollForBranchGeneration || !autoFollowOutput || listState.isScrollInProgress) {
            return@LaunchedEffect
        }
        val isStreaming = isGenerating && (currentResponse.isNotEmpty() || currentThinking.isNotEmpty())
        if (isStreaming) {
            val totalCount = listState.layoutInfo.totalItemsCount
            if (totalCount > 0) {
                val targetIndex = (totalCount - 1).coerceAtLeast(0)
                try {
                    listState.scrollToItem(targetIndex)
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        topBar = {
            val toolbarShape = RoundedCornerShape(24.dp)
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val topBarBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.92f else 0.96f)
            val toolbarContentColor = readableTextColorFor(
                background = topBarBg,
                fallbackSurface = readableBackdrops.top
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = toolbarShape,
                color = topBarBg,
                contentColor = toolbarContentColor,
                border = null,
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
        },
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

                ChatInputBar(
                    hazeState = hazeState,
                    inputText = inputText,
                    onInputChange = {
                        inputText = it
                        ChatViewModel.saveDraft(conversationId, it)
                    },
                    onSend = {
                        if (inputText.isNotBlank() || selectedAttachments.isNotEmpty()) {
                            if (!isProcessingAttachments) {
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
                                    viewModel.sendEditedMessage(editSource, inputText, selectedAttachments)
                                } else {
                                    preserveScrollForBranchGeneration = false
                                    streamingBranchGroupId = null
                                    autoFollowOutput = true
                                    viewModel.sendMessage(inputText, selectedAttachments)
                                }
                                inputText = ""
                                ChatViewModel.saveDraft(conversationId, "")
                                pendingEditSource = null
                                selectedAttachments = emptyList()
                                attachmentStatus = null
                            }
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
                    isRoleplay = uiState.isRoleplay,
                    onPlotActionClick = { showPlotActionDialog = true },
                    readableBackdrop = readableBackdrops.bottom
                )
            }
        }
) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding())
            ) {
                // 错误提示 (在顶部导航栏下方显示，避免重合)
                error?.let { errorMsg ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            border = null,
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
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMsg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearError() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // 消息列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 6.dp,
                        bottom = paddingValues.calculateBottomPadding() + 18.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 空状态
                    if (displayMessages.isEmpty() && currentResponse.isEmpty() && currentThinking.isEmpty()) {
                        item {
                            EmptyChatPlaceholder()
                        }
                    }

                    // 消息列表
                    val currentAssistantModelName = currentModelOption?.modelName ?: currentModel ?: uiState.modelName ?: "AI"
                    items(
                        items = displayMessages,
                        key = { item -> item.groupId ?: "${item.message.id}_${item.message.createdAt}_${item.message.role}" }
                    ) { displayItem ->
                        val message = displayItem.message
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

                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                    variantInfo = VariantInfo(
                                        groupId = streamingBranchGroupId!!,
                                        currentIndex = totalVariantsWithStreaming,
                                        total = totalVariantsWithStreaming,
                                        availableIndices = (displayItem.variantInfo?.availableIndices ?: listOf(1)) + totalVariantsWithStreaming
                                    ),
                                    onVariantSelected = { groupId, index ->
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
                                    }
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
                                    onRegenerate = if (message.role == "assistant" && message == messages.lastOrNull { it.role == "assistant" }) {
                                        {
                                            preserveScrollForBranchGeneration = true
                                            autoFollowOutput = false
                                            streamingBranchGroupId = message.variantGroupId ?: "reply_${message.id}"
                                            viewModel.regenerateLastMessage()
                                        }
                                    } else null,
                                    onEdit = if (message.role == "user") {
                                        {
                                            inputText = message.content
                                            pendingEditSource = message
                                            autoFollowOutput = false
                                            selectedAttachments = emptyList()
                                            attachmentStatus = null
                                        }
                                    } else null,
                                    onDelete = {
                                        viewModel.deleteMessage(message)
                                    }
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
                                    translatingThinking = false,
                                    onTranslateThinking = null,
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(currentResponse))
                                    },
                                    onCopyThinking = {
                                        clipboardManager.setText(AnnotatedString(currentThinking))
                                    }
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
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(currentResponse))
                                },
                                onCopyThinking = {
                                    clipboardManager.setText(AnnotatedString(currentThinking))
                                }
                            )
                        }
                    }

                    item(key = "chat_bottom_anchor") {
                        Spacer(modifier = Modifier.height(1.dp))
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
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                onJumpToBottom = {
                    autoFollowOutput = true
                    scope.launch {
                        val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
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
        }
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

    if (showContextUsageDialog) {
        ContextUsageDialog(
            hazeState = hazeState,
            state = contextUsage,
            onDismiss = { showContextUsageDialog = false },
            onRefresh = { viewModel.refreshContextUsage() },
            onCompress = { viewModel.compressContextNow() }
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
}

private fun buildChatAnchorItems(displayMessages: List<DisplayMessageItem>): List<SideAnchorItem> {
    return displayMessages.mapIndexedNotNull { index, item ->
        val message = item.message
        if (message.role != "user") return@mapIndexedNotNull null
        SideAnchorItem(
            title = anchorTitle(message.content),
            itemIndex = index
        )
    }
}

private fun anchorTitle(value: String): String {
    return value
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.take(28)
        ?: "我的提问"
}

@Composable
private fun ChatScrollJumpButtons(
    visible: Boolean,
    onJumpToTop: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallFloatingActionButton(
                onClick = onJumpToTop,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "一键到顶")
            }
            SmallFloatingActionButton(
                onClick = onJumpToBottom,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "一键到底")
            }
        }
    }
}

@Composable
private fun ContextUsageButton(
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
            if (canCompress) {
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
private fun ContextUsageRing(
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
private fun ContextUsageDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    state: ContextUsageUiState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onCompress: () -> Unit
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
                        text = "当前模型窗口、上下文预算与压缩状态",
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
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        ContextUsageOverview(usage = usage)
                    }
                    item {
                        ContextUsageDetails(usage = usage)
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
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isCompressing
                ) {
                    Text("刷新")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
                Button(
                    onClick = onCompress,
                    enabled = !state.isCompressing
                ) {
                    if (state.isCompressing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isCompressing) "压缩中" else "主动压缩")
                }
            }
        }
    )

}

@Composable
private fun ContextUsageOverview(usage: ConversationContextUsage) {
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
private fun ContextUsageDetails(usage: ConversationContextUsage) {
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
                label = "较早消息",
                value = "${usage.olderMessageCount} 条"
            )
            ContextUsageRow(
                label = "滚动摘要",
                value = if (usage.hasRollingSummary) {
                    "${formatTokenCount(usage.summaryTokens)} tokens"
                } else {
                    "尚未生成"
                }
            )
            ContextUsageRow(
                label = "长期记忆",
                value = "${usage.memoryItemCount} 条 · ${formatTokenCount(usage.memoryTokens)} tokens"
            )
            ContextUsageRow(
                label = "已压缩至",
                value = usage.compressedThroughMessageId?.let { "#$it" } ?: "尚未压缩"
            )
            ContextUsageRow(
                label = "摘要时间",
                value = usage.summaryUpdatedAt?.let(::formatContextTimestamp) ?: "暂无"
            )
        }
    }
}

@Composable
private fun ContextUsageStatus(
    usage: ConversationContextUsage,
    statusMessage: String?
) {
    val message = statusMessage ?: if (usage.canCompress) {
        "有较早消息尚未进入滚动摘要，可主动压缩。"
    } else {
        "当前上下文摘要已覆盖可压缩范围。"
    }
    val icon = if (usage.canCompress) Icons.Default.Warning else Icons.Default.CheckCircle
    val color = if (usage.canCompress) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

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
private fun ContextUsageRow(
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
private fun contextUsageColor(usagePercent: Float): Color {
    return when {
        usagePercent >= 0.85f -> MaterialTheme.colorScheme.error
        usagePercent >= 0.65f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
}

private fun formatTokenCount(value: Int): String {
    return if (value >= 1000) {
        String.format(Locale.getDefault(), "%.1fk", value / 1000f)
    } else {
        value.toString()
    }
}

private fun formatContextTimestamp(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatHeaderTitle(
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

private data class DisplayMessageItem(
    val message: Message,
    val groupId: String?,
    val variantInfo: VariantInfo? = null
)

private data class ScrollFollowSnapshot(
    val isScrolling: Boolean,
    val lastVisibleIndex: Int,
    val totalItems: Int
) {
    val isAtBottom: Boolean
        get() = totalItems <= 0 || lastVisibleIndex >= totalItems - 2
}

fun formatThinkingCapsuleText(
    template: String,
    modelName: String,
    isThinkingActive: Boolean,
    responseTimeMs: Long,
    thinkingTokens: Int,
    totalTokens: Int
): String {
    val model = modelName.ifBlank { "AI" }
    val status = if (isThinkingActive) "思考中..." else "思考过程"
    val time = if (responseTimeMs > 0) formatTime(responseTimeMs) else ""
    val tokens = when {
        thinkingTokens > 0 -> "${thinkingTokens} token"
        totalTokens > 0 -> "${totalTokens} token"
        else -> ""
    }

    var result = template
        .replace("{model}", model)
        .replace("{status}", status)
        .replace("{time}", time)
        .replace("{tokens}", tokens)
        .replace("{token}", tokens)

    result = result.replace(Regex("\\s+"), " ").trim()
    return if (result.isBlank()) "$model $status" else result
}

fun formatNonThinkingCapsuleText(
    modelName: String,
    responseTimeMs: Long,
    tokenCount: Int,
    content: String = ""
): String {
    val model = modelName.ifBlank { "AI" }
    val effectiveTokens = if (tokenCount > 0) tokenCount else AiRepository.estimateTokenCount(content)
    val seconds = (responseTimeMs / 1000).toInt().coerceAtLeast(1)
    return when {
        responseTimeMs > 0 && effectiveTokens > 0 ->
            "${model}用${seconds}秒吃掉了你${effectiveTokens}token"
        responseTimeMs > 0 ->
            "${model}用${seconds}秒回复了你"
        effectiveTokens > 0 ->
            "${model}吃掉了你${effectiveTokens}token"
        else -> model
    }
}

private data class VariantInfo(
    val groupId: String,
    val currentIndex: Int,
    val total: Int,
    val availableIndices: List<Int>
)

private fun buildDisplayMessages(
    messages: List<Message>,
    selections: Map<String, Int>
): List<DisplayMessageItem> {
    val groups = messages
        .filter { !it.variantGroupId.isNullOrBlank() }
        .groupBy { it.variantGroupId!! }
    val consumedGroups = mutableSetOf<String>()
    val result = mutableListOf<DisplayMessageItem>()

    messages.forEach { message ->
        val groupId = message.variantGroupId
        if (groupId.isNullOrBlank()) {
            result += DisplayMessageItem(message = message, groupId = null)
            return@forEach
        }
        if (!consumedGroups.add(groupId)) return@forEach

        val variants = groups[groupId].orEmpty().sortedBy { it.variantIndex }
        val indices = variants.map { it.variantIndex }.distinct().sorted()
        val selectedIndex = selections[groupId]
            ?.takeIf { it in indices }
            ?: indices.lastOrNull()
            ?: 1
        val selectedMessage = variants.lastOrNull { it.variantIndex == selectedIndex }
            ?: variants.last()
        result += DisplayMessageItem(
            message = selectedMessage,
            groupId = groupId,
            variantInfo = if (indices.size > 1) {
                VariantInfo(
                    groupId = groupId,
                    currentIndex = selectedIndex,
                    total = indices.size,
                    availableIndices = indices
                )
            } else null
        )
    }

    return result
}

private fun pairedVariantGroupId(groupId: String): String? {
    return when {
        groupId.endsWith("_user") -> groupId.removeSuffix("_user") + "_assistant"
        groupId.endsWith("_assistant") -> groupId.removeSuffix("_assistant") + "_user"
        else -> null
    }
}

private fun isErrorMessage(content: String): Boolean {
    val trimmed = content.trim()
    return trimmed.startsWith("请求失败") ||
           trimmed.startsWith("[请求失败]") ||
           trimmed.startsWith("Error:") ||
           trimmed.startsWith("error:") ||
           trimmed.contains("[输出已被中断:")
}

@Composable
private fun MessageBubble(
    message: Message,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    readableBackdrop: Color = Color.Unspecified,
    isGenerating: Boolean = false,
    assistantAvatarRevision: Int = 0,
    assistantApiConfigId: Long? = null,
    assistantModelName: String = "AI",
    variantInfo: VariantInfo? = null,
    onVariantSelected: (String, Int) -> Unit = { _, _ -> },
    translatingThinking: Boolean = false,
    onTranslateThinking: ((Message) -> Unit)? = null,
    onCopy: () -> Unit,
    onCopyThinking: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isUser = message.role == "user"
    val resolvedReadableBackdrop = readableBackdrop.takeOrElse {
        MaterialTheme.colorScheme.background
    }
    val glass = echoGlassPalette()
    val userBubbleTint = glass.userBubble
    val bubbleColor = if (isUser) glass.userBubble else glass.assistantBubble
    val textBackground = if (isUser) bubbleColor else resolvedReadableBackdrop
    val textColor = readableTextColorFor(
        background = if (isUser) textBackground else glass.panelStrong,
        fallbackSurface = resolvedReadableBackdrop
    )
    val bubbleShape = if (isUser) {
        RoundedCornerShape(18.dp, 6.dp, 18.dp, 18.dp)
    } else {
        RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp)
    }
    val hasThinkingContent = !message.thinkingContent.isNullOrBlank()
    val hasThinking = hasThinkingContent || message.thinkingTokens > 0
    var showThinking by remember(message.id, hasThinkingContent) {
        mutableStateOf(hasThinkingContent)
    }
    val isThinkingEnglish = remember(message.thinkingContent) {
        AiRepository.isMainlyEnglish(message.thinkingContent)
    }
    val hasTranslation = !message.translatedThinking.isNullOrBlank()
    var showTranslated by remember(message.id, message.translatedThinking) {
        mutableStateOf(hasTranslation)
    }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // 解析附件
    val attachments = remember(message.attachments) {
        if (message.attachments.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val gson = com.google.gson.Gson()
                val type = com.google.gson.reflect.TypeToken.getParameterized(
                    List::class.java, Attachment::class.java
                ).type
                gson.fromJson<List<Attachment>>(message.attachments, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    var activeCitation by remember { mutableStateOf<CitationInfo?>(null) }
    val citations = remember(message.content) {
        if (!isUser) extractCitationsFromContent(message.content) else emptyList()
    }

    if (activeCitation != null) {
        CitationDetailDialog(
            citation = activeCitation!!,
            onDismiss = { activeCitation = null }
        )
    }

    @Composable
    fun MessageContent(contentColor: Color) {
        Column(
            modifier = if (isUser) {
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            }
        ) {
            if (message.content.isNotBlank()) {
                if (isUser) {
                    Text(
                        text = message.content,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    MarkdownText(
                        content = message.content,
                        color = contentColor,
                        onCitationClick = { id ->
                            activeCitation = citations.find { it.index == id }
                                ?: CitationInfo(id, "参考资料 $id", "https://www.google.com/search?q=$id")
                        }
                    )
                    if (citations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CitationsCardsRow(
                            citations = citations,
                            onCitationClick = { activeCitation = it }
                        )
                    }
                }
            } else if (!isGenerating && !hasThinking && attachments.isEmpty()) {
                Text(
                    text = "空消息",
                    color = contentColor.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isGenerating) {
                if (message.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                TypingIndicator(textColor = contentColor)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = (maxWidth - 52.dp).coerceAtLeast(160.dp).coerceAtMost(360.dp)

        if (isUser) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    horizontalAlignment = Alignment.End
                ) {
                    if (attachments.isNotEmpty()) {
                        AttachmentGroupBubble(
                            attachments = attachments,
                            modifier = Modifier
                                .widthIn(max = bubbleMaxWidth)
                                .padding(bottom = if (message.content.isNotBlank() || isGenerating) 8.dp else 0.dp)
                        )
                    }

                    if (message.content.isNotBlank() || isGenerating) {
                        Surface(
                            modifier = Modifier,
                            color = bubbleColor,
                            contentColor = textColor,
                            shape = bubbleShape,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                width = 0.8.dp,
                                color = glass.outlineSelected.copy(alpha = 0.35f)
                            )
                        ) {
                            MessageContent(textColor)
                        }
                    }

                    MessageFooter(
                        isUser = true,
                        message = message,
                        variantInfo = variantInfo,
                        onVariantSelected = onVariantSelected,
                        onCopy = onCopy,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        modifier = Modifier
                            .widthIn(max = bubbleMaxWidth)
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                ChatAvatar(isUser = true)
            }
        } else {
            // 模型回复：头像与身份置顶对齐，正文与思考全宽居中展开，左右对称无空白浪费
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                val thinkingBubbleColor = glass.controlSelected
                val thinkingHeaderColor = MaterialTheme.colorScheme.onPrimaryContainer
                val thinkingContentColor = glass.textPrimary

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatAvatar(
                        isUser = false,
                        avatarRevision = assistantAvatarRevision,
                        apiConfigId = assistantApiConfigId
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    val personalizationSettings = remember {
                        AiAssistantApp.instance.personalizationManager.getSettings()
                    }
                    val isConnecting = isGenerating && message.content.isBlank() && !hasThinking
                    val isThinkingActive = isGenerating && hasThinking && message.content.isBlank()
                    val capsuleText = remember(
                        assistantModelName,
                        hasThinking,
                        isGenerating,
                        isConnecting,
                        isThinkingActive,
                        message.content,
                        message.responseTime,
                        message.thinkingTokens,
                        message.tokenCount,
                        personalizationSettings.thinkingCapsuleTemplate
                    ) {
                        val model = assistantModelName.ifBlank { "AI" }
                        when {
                            isConnecting -> "正在连接 $model..."
                            isThinkingActive -> "模型正在思考中"
                            hasThinking -> formatThinkingCapsuleText(
                                template = personalizationSettings.thinkingCapsuleTemplate,
                                modelName = model,
                                isThinkingActive = false,
                                responseTimeMs = message.responseTime,
                                thinkingTokens = message.thinkingTokens,
                                totalTokens = message.tokenCount
                            )
                            isGenerating -> "$model 正在思考回复中..."
                            else -> formatNonThinkingCapsuleText(
                                modelName = model,
                                responseTimeMs = message.responseTime,
                                tokenCount = message.tokenCount,
                                content = message.content
                            )
                        }
                    }

                    val capsuleShape = RoundedCornerShape(999.dp)
                    val capsuleScrollState = rememberScrollState()
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 34.dp)
                            .widthIn(max = 300.dp)
                            .then(
                                if (hasThinking && hasThinkingContent) {
                                    Modifier.echoShapeClick(capsuleShape) { showThinking = !showThinking }
                                } else Modifier
                            )
                            .then(
                                if (hazeState != null) {
                                    Modifier.echoHazePanel(
                                        hazeState = hazeState,
                                        shape = capsuleShape,
                                        tint = thinkingBubbleColor,
                                        blurRadius = 12.dp,
                                        highlightAlpha = 0.03f
                                    )
                                } else Modifier
                            ),
                        color = thinkingBubbleColor,
                        contentColor = thinkingHeaderColor,
                        shape = capsuleShape,
                        border = BorderStroke(
                            1.dp,
                            glass.outlineSelected.copy(alpha = 0.72f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isConnecting || isThinkingActive) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(13.dp),
                                    strokeWidth = 1.8.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (hasThinking) {
                                Icon(
                                    Icons.Default.Psychology,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = thinkingHeaderColor
                                )
                            } else {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = thinkingHeaderColor
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .horizontalScroll(capsuleScrollState)
                            ) {
                                Text(
                                    text = capsuleText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = thinkingHeaderColor,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (hasThinking && hasThinkingContent) {
                                Icon(
                                    imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showThinking) "收起" else "展开",
                                    modifier = Modifier.size(16.dp),
                                    tint = thinkingHeaderColor.copy(alpha = 0.78f)
                                )
                            }
                        }
                    }
                }

                if (!isGenerating && isThinkingEnglish && !hasTranslation) {
                    Surface(
                        modifier = Modifier
                            .padding(top = 4.dp, start = 40.dp)
                            .clickable {
                                showThinking = true
                                onTranslateThinking?.invoke(message)
                            },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (translatingThinking) "正在翻译思考链..." else "检测到英文思考 · 点击汉化",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (hasThinking) {
                    AnimatedVisibility(visible = showThinking && hasThinkingContent) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 4.dp),
                            color = glass.controlSelected.copy(alpha = 0.6f),
                            contentColor = thinkingContentColor,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, glass.outlineSelected.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "思考内容详情",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = thinkingHeaderColor
                                        )

                                        if (hasTranslation) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(glass.control.copy(alpha = 0.7f))
                                                    .padding(2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (showTranslated) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    modifier = Modifier.clickable { showTranslated = true }
                                                ) {
                                                    Text(
                                                        text = "译文",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = if (showTranslated) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (showTranslated) MaterialTheme.colorScheme.onPrimary else thinkingHeaderColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (!showTranslated) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    modifier = Modifier.clickable { showTranslated = false }
                                                ) {
                                                    Text(
                                                        text = "原文",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = if (!showTranslated) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (!showTranslated) MaterialTheme.colorScheme.onPrimary else thinkingHeaderColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (!hasTranslation && isThinkingEnglish && !isGenerating) {
                                            if (translatingThinking) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(13.dp),
                                                        strokeWidth = 1.6.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "翻译中...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    shape = RoundedCornerShape(999.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                                    modifier = Modifier.clickable { onTranslateThinking?.invoke(message) }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Text("翻译为中文", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        } else if (hasTranslation && !isGenerating) {
                                            IconButton(
                                                onClick = { onTranslateThinking?.invoke(message) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh,
                                                    contentDescription = "重新翻译",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = thinkingHeaderColor.copy(alpha = 0.78f)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val textToCopy = if (hasTranslation && showTranslated) message.translatedThinking ?: "" else message.thinkingContent ?: ""
                                                clipboardManager.setText(AnnotatedString(textToCopy))
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "复制思考",
                                                modifier = Modifier.size(14.dp),
                                                tint = thinkingHeaderColor.copy(alpha = 0.78f)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val displayThinking = if (hasTranslation && showTranslated) message.translatedThinking ?: "" else (message.thinkingContent ?: "")
                                MarkdownText(
                                    content = displayThinking,
                                    color = thinkingContentColor
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val isErrorOutput = !isUser && isErrorMessage(message.content)

                    if (isErrorOutput) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = "错误",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    MarkdownText(
                                        content = message.content,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    } else {
                        MessageContent(textColor)
                    }

                    // 工具调用留痕展示 (支持展开查看执行摘要与注入的上下文详情)
                    val toolCallsList = remember(message.toolCalls) {
                        if (message.toolCalls.isNullOrBlank()) {
                            emptyList<ToolCallRecord>()
                        } else {
                            try {
                                val gson = com.google.gson.Gson()
                                val type = com.google.gson.reflect.TypeToken.getParameterized(
                                    List::class.java, ToolCallRecord::class.java
                                ).type
                                gson.fromJson<List<ToolCallRecord>>(message.toolCalls, type) ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    if (toolCallsList.isNotEmpty()) {
                        ToolCallsFooter(toolCalls = toolCallsList)
                    }

                    MessageFooter(
                        isUser = false,
                        message = message,
                        variantInfo = variantInfo,
                        onVariantSelected = onVariantSelected,
                        onCopy = onCopy,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isGenerating) {
                        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDarkTheme) 0.35f else 0.50f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageFooter(
    isUser: Boolean,
    message: Message,
    variantInfo: VariantInfo? = null,
    onVariantSelected: ((String, Int) -> Unit)? = null,
    onCopy: () -> Unit,
    onRegenerate: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val thinkingTokensInThinkingBubble = message.role == "assistant" &&
        (!message.thinkingContent.isNullOrBlank() || message.thinkingTokens > 0)
    val responseTimeInThinkingBubble = thinkingTokensInThinkingBubble

    Row(
        modifier = modifier.padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧元数据：时间戳、Token 等支持水平横向滚动
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            MessageMetaText(
                text = formatMessageClock(message.createdAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )

            if (message.responseTime > 0 && !responseTimeInThinkingBubble) {
                MessageMetaText(
                    text = formatTime(message.responseTime),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                )
            }

            if (message.tokenCount > 0) {
                MessageMetaText(
                    text = "${message.tokenCount} tokens",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                )

                if (message.responseTime > 0) {
                    val seconds = message.responseTime / 1000.0
                    if (seconds > 0.05) {
                        val speed = message.tokenCount / seconds
                        MessageMetaText(
                            text = String.format(Locale.US, "%.1f tokens/s", speed),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                        )
                    }
                }
            }

            if (message.thinkingTokens > 0 && !thinkingTokensInThinkingBubble) {
                MessageMetaText(
                    text = "思考: ${message.thinkingTokens} tokens",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 右侧操作栏：版本切换器与操作按钮同一行排布
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (variantInfo != null && variantInfo.total > 1 && onVariantSelected != null) {
                VariantSwitcher(
                    info = variantInfo,
                    onSelect = { index -> onVariantSelected(variantInfo.groupId, index) }
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            FooterIconButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "复制",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onCopy
            )

            if (!isUser && onRegenerate != null) {
                FooterIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "重新生成",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onRegenerate
                )
            }

            if (isUser && onEdit != null) {
                FooterIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "重新编辑",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onEdit
                )
            }

            if (onDelete != null) {
                FooterIconButton(
                    icon = Icons.Default.DeleteOutline,
                    contentDescription = "删除本条",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.78f),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun MessageMetaText(
    text: String,
    color: Color
) {
    Text(
        text = text,
        modifier = Modifier.padding(end = 8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}

@Composable
private fun FooterIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(15.dp),
            tint = tint
        )
    }
}

@Composable
private fun VariantSwitcher(
    info: VariantInfo,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPosition = info.availableIndices.indexOf(info.currentIndex).coerceAtLeast(0)
    val canGoPrevious = currentPosition > 0
    val canGoNext = currentPosition < info.availableIndices.lastIndex

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                if (canGoPrevious) onSelect(info.availableIndices[currentPosition - 1])
            },
            enabled = canGoPrevious,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                contentDescription = "上一版",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${currentPosition + 1}/${info.total}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(
            onClick = {
                if (canGoNext) onSelect(info.availableIndices[currentPosition + 1])
            },
            enabled = canGoNext,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "下一版",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatAvatar(
    isUser: Boolean,
    avatarRevision: Int = 0,
    apiConfigId: Long? = null
) {
    val context = LocalContext.current
    val userAvatarBitmap = if (isUser) remember(context) { AvatarManager.getAvatarBitmap(context) } else null
    val modelAvatarBitmap = if (!isUser) {
        remember(context, avatarRevision, apiConfigId) {
            AvatarManager.getPreferredModelAvatarBitmap(context, apiConfigId)
        }
    } else null
    val background = if (isUser) Color.White else MaterialTheme.colorScheme.surface
    val foreground = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = Modifier
            .requiredSize(36.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (isUser && userAvatarBitmap != null) {
            Image(
                bitmap = userAvatarBitmap.asImageBitmap(),
                contentDescription = "用户头像",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (!isUser) {
            if (modelAvatarBitmap != null) {
                Image(
                    bitmap = modelAvatarBitmap.asImageBitmap(),
                    contentDescription = "模型头像",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.deepseek),
                    contentDescription = "模型头像",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = foreground,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun TypingIndicator(
    textColor: androidx.compose.ui.graphics.Color
) {
    var dotCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(400)
            dotCount = (dotCount + 1) % 4
        }
    }

    Text(
        text = "●".repeat(dotCount) + "○".repeat(3 - dotCount),
        color = Color(0xFF93C5FD),
        style = MaterialTheme.typography.bodyLarge,
        letterSpacing = 2.sp
    )
}

// 格式化时间
private fun formatTime(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000}s"
        else -> "${ms / 60000}m${(ms % 60000) / 1000}s"
    }
}

private fun formatMessageClock(timestamp: Long): String {
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun AttachmentChip(attachment: Attachment) {
    val isImage = FileUtils.isImage(attachment.mimeType, attachment.name)
    val hasOcr = !attachment.ocrText.isNullOrBlank() || attachment.processingNote?.contains("OCR") == true

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.primary
        ),
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
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = attachment.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AttachmentGroupBubble(
    attachments: List<Attachment>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(12.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(10.dp)
        ) {
            items(attachments) { attachment ->
                AttachmentChip(attachment = attachment)
            }
        }
    }
}

@Composable
fun ChatInputBar(
    hazeState: dev.chrisbanes.haze.HazeState,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
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
    enableThinking: Boolean = false,
    thinkingEffort: String = "medium",
    onThinkingChange: (Boolean, String) -> Unit = { _, _ -> },
    isRoleplay: Boolean = false,
    onPlotActionClick: () -> Unit = {},
    readableBackdrop: Color = Color.Unspecified
) {
    var showToolMenu by remember { mutableStateOf(false) }
    var isInputExpanded by remember { mutableStateOf(false) }
    var showThinkingPopover by remember { mutableStateOf(false) }
    val inputShape = if (isInputExpanded) RoundedCornerShape(22.dp) else RoundedCornerShape(30.dp)
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
        // 深度思考向上展开渐变滑块气泡弹窗 (Requirement 7)
        AnimatedVisibility(
            visible = showThinkingPopover,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            ReasoningEffortPopupCard(
                enableThinking = enableThinking,
                thinkingEffort = thinkingEffort,
                onEffortSelected = { enabled, effort ->
                    onThinkingChange(enabled, effort)
                },
                onClose = { showThinkingPopover = false }
            )
        }

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
            color = inputTint,
            contentColor = inputTextColor,
            border = BorderStroke(1.dp, glass.outline),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
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

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            min = if (isInputExpanded) 160.dp else 42.dp,
                            max = if (isInputExpanded) 320.dp else 112.dp
                        )
                        .background(Color.Transparent),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = inputTextColor,
                        background = Color.Transparent
                    ),
                    cursorBrush = SolidColor(inputTextColor),
                    maxLines = if (isInputExpanded) 15 else 5,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = if (isInputExpanded) 160.dp else 42.dp)
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
                            IconButton(
                                onClick = { isInputExpanded = !isInputExpanded },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isInputExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                    contentDescription = if (isInputExpanded) "收起输入框" else "放大输入框",
                                    tint = inputTextColor.copy(alpha = 0.65f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 深度思考 按钮（排在第1位，点击向上展开档位弹窗）
                        item {
                            val effortText = when {
                                !enableThinking -> "深度思考 · 关 ⌃"
                                thinkingEffort.equals("low", true) || thinkingEffort.equals("fast", true) -> "深度思考 · 快速 ⌃"
                                thinkingEffort.equals("medium", true) || thinkingEffort.equals("balanced", true) -> "深度思考 · 平衡 ⌃"
                                thinkingEffort.equals("high", true) || thinkingEffort.equals("deep", true) -> "深度思考 · 深入 ⌃"
                                thinkingEffort.equals("ultra", true) || thinkingEffort.equals("max", true) -> "深度思考 · Ultra ⌃"
                                else -> "深度思考 · 平衡 ⌃"
                            }
                            val effortAccentColor = when {
                                !enableThinking -> glass.outline
                                thinkingEffort.equals("low", true) -> Color(0xFF2ECC71)
                                thinkingEffort.equals("medium", true) -> Color(0xFF3498DB)
                                thinkingEffort.equals("high", true) -> Color(0xFF9B59B6)
                                thinkingEffort.equals("ultra", true) || thinkingEffort.equals("max", true) -> Color(0xFFB950FD)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            InputPillButton(
                                text = effortText,
                                icon = Icons.Default.Psychology,
                                selected = enableThinking,
                                onClick = { showThinkingPopover = !showThinkingPopover },
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

                    val softButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = glass.control,
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = glass.control.copy(alpha = 0.52f),
                        disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    )

                    Box {
                        FilledTonalIconButton(
                            onClick = { showToolMenu = true },
                            enabled = !isProcessingAttachments,
                            modifier = Modifier.size(40.dp),
                            colors = softButtonColors
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "添加内容", modifier = Modifier.size(20.dp))
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

                    if (isGenerating) {
                        FilledIconButton(
                            onClick = onStopGeneration,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "停止")
                        }
                    } else {
                        FilledIconButton(
                            onClick = onSend,
                            enabled = !isProcessingAttachments && (inputText.isNotBlank() || attachments.isNotEmpty()),
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                disabledContainerColor = glass.control.copy(alpha = 0.52f),
                                disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "发送")
                        }
                    }
                }
            }
        }
    }
}

private data class ThinkingEffortLevel(
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
private fun ReasoningEffortPopupCard(
    enableThinking: Boolean,
    thinkingEffort: String,
    onEffortSelected: (Boolean, String) -> Unit,
    onClose: () -> Unit
) {
    val levels = remember {
        listOf(
            ThinkingEffortLevel(
                step = 0,
                key = "none",
                enabled = false,
                name = "关闭",
                subtitle = "极速直答 · 无思考",
                detail = "跳过深度思维链推演，以模型原生最高速度直接生成最终回复内容。",
                primaryColor = Color(0xFF7F8C8D),
                gradientColors = listOf(Color(0xFF7F8C8D), Color(0xFF95A5A6))
            ),
            ThinkingEffortLevel(
                step = 1,
                key = "low",
                enabled = true,
                name = "快速",
                subtitle = "轻度思考 · 快速响应",
                detail = "分配少量思考预算进行简要推理，适合常规闲聊、翻译与基础问答。",
                primaryColor = Color(0xFF2ECC71),
                gradientColors = listOf(Color(0xFF2ECC71), Color(0xFF27AE60))
            ),
            ThinkingEffortLevel(
                step = 2,
                key = "medium",
                enabled = true,
                name = "平衡",
                subtitle = "适中思考 · 兼顾速度与深度",
                detail = "兼顾逻辑严谨性与响应耗时，应对大多数日常工作、分析与创作场景（推荐）。",
                primaryColor = Color(0xFF3498DB),
                gradientColors = listOf(Color(0xFF3498DB), Color(0xFF2980B9))
            ),
            ThinkingEffortLevel(
                step = 3,
                key = "high",
                enabled = true,
                name = "深入",
                subtitle = "深度思考 · 严密推演",
                detail = "投入大量思考预算进行多步论证、边界检查与代码架构推演，适合复杂技术任务。",
                primaryColor = Color(0xFF9B59B6),
                gradientColors = listOf(Color(0xFF9B59B6), Color(0xFF8E44AD))
            ),
            ThinkingEffortLevel(
                step = 4,
                key = "ultra",
                enabled = true,
                name = "Ultra",
                subtitle = "极限思考 · 极致推理",
                detail = "释放最大思考预算上限，全力攻坚数学证明、高难度算法与复杂多维哲学推理。",
                primaryColor = Color(0xFFB950FD),
                gradientColors = listOf(Color(0xFF8E44AD), Color(0xFFE056FD))
            )
        )
    }

    val currentStep = remember(enableThinking, thinkingEffort) {
        if (!enableThinking) 0
        else when (thinkingEffort.lowercase()) {
            "low", "fast" -> 1
            "medium", "balanced" -> 2
            "high", "deep" -> 3
            "ultra", "max" -> 4
            else -> 2
        }
    }

    var sliderIndex by remember(currentStep) { mutableFloatStateOf(currentStep.toFloat()) }
    val currentLevel = levels[sliderIndex.roundToInt().coerceIn(0, 4)]
    val glass = echoGlassPalette()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, currentLevel.primaryColor.copy(alpha = 0.5f)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部 Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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
                        Text(
                            text = "深度思考强度",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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

            // 档位详细说明卡片
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = currentLevel.primaryColor.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, currentLevel.primaryColor.copy(alpha = 0.25f))
            ) {
                Text(
                    text = currentLevel.detail,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            // 渐变滑块区域
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = sliderIndex,
                    onValueChange = { newVal ->
                        sliderIndex = newVal
                        val stepInt = newVal.roundToInt().coerceIn(0, 4)
                        val target = levels[stepInt]
                        onEffortSelected(target.enabled, target.key)
                    },
                    valueRange = 0f..4f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = currentLevel.primaryColor,
                        activeTrackColor = currentLevel.primaryColor,
                        inactiveTrackColor = currentLevel.primaryColor.copy(alpha = 0.2f),
                        activeTickColor = Color.White.copy(alpha = 0.8f),
                        inactiveTickColor = currentLevel.primaryColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
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
}

@Composable
private fun InputModelSelector(
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

    Box {
        InputPillButton(
            text = currentLabel.shortModelLabel(),
            selected = true,
            onClick = { expanded = true }
        )

        EchoGlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
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
private fun ModelOptionText(option: ChatModelOption) {
    val cap = remember(option.modelName) {
        com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(option.modelName)
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = option.modelName,
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
private fun InputPillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
        }
    }
}

private fun String.shortModelLabel(): String {
    if (isBlank()) return "选择模型"
    return when {
        length <= 18 -> this
        else -> take(8) + "..." + takeLast(7)
    }
}

private fun String.imageSupportOverride(): Boolean? = when (this) {
    "text" -> false
    "multimodal" -> true
    else -> null
}

private fun ChatModelOption.sameModelOption(
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
fun EmptyChatPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

@Composable
fun SystemPromptDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    currentPrompt: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    onSaveAsTemplate: ((String, String) -> Unit)? = null,
    templates: List<PromptTemplate> = emptyList()
) {
    var promptText by remember { mutableStateOf(currentPrompt ?: "") }
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text("系统提示词") },
        text = {
            Column {
                Text(
                    text = "设置系统提示词可以定义AI助手的行为和角色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 模板选择和保存按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 选择模板按钮
                    OutlinedButton(
                        onClick = { showTemplates = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择模板")
                    }

                    // 保存为模板按钮
                    if (onSaveAsTemplate != null && promptText.isNotBlank()) {
                        OutlinedButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存模板")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 提示词输入框
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = { Text("例如：你是一个专业的编程助手...") },
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(promptText.ifBlank { null }) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )

    // 模板选择对话框
    if (showTemplates) {
        TemplateListDialog(
            hazeState = hazeState,
            templates = templates,
            onDismiss = { showTemplates = false },
            onSelect = { template ->
                promptText = template.content
                showTemplates = false
            }
        )
    }

    // 保存模板对话框
    if (showSaveDialog) {
        SaveTemplateDialog(
            hazeState = hazeState,
            content = promptText,
            onDismiss = { showSaveDialog = false },
            onSave = { name, content ->
                onSaveAsTemplate?.invoke(name, content)
                showSaveDialog = false
            }
        )
    }
}

@Composable
fun TemplateListDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    templates: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onSelect: (PromptTemplate) -> Unit
) {
    val categories = remember(templates) { templates.map { it.category }.distinct() }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text("选择提示词模板") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    item(key = "header_$category") {
                        Text(
                            text = getCategoryDisplayName(category),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    val categoryTemplates = templates.filter { it.category == category }
                    items(
                        items = categoryTemplates,
                        key = { it.id }
                    ) { template ->
                        Card(
                            onClick = { onSelect(template) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = template.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                template.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = template.content.take(100) + if (template.content.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 获取分类显示名称
private fun getCategoryDisplayName(category: String): String {
    return when (category) {
        "general" -> "通用"
        "coding" -> "编程"
        "writing" -> "写作"
        "analysis" -> "分析"
        "education" -> "教育"
        else -> category.replaceFirstChar { it.uppercaseChar() }
    }
}

@Composable
fun SaveTemplateDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    content: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text("保存为模板") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("模板名称") },
                    placeholder = { Text("例如：代码助手") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("描述（可选）") },
                    placeholder = { Text("简短描述模板用途") },
                    singleLine = true
                )

                // 分类选择
                Column {
                    Text("分类", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("general", "coding", "writing", "analysis", "education").forEach { cat ->
                            val selected = category == cat
                            FilterChip(
                                selected = selected,
                                onClick = { category = cat },
                                colors = echoFilterChipColors(),
                                border = echoFilterChipBorder(selected),
                                elevation = echoFilterChipElevation(),
                                label = {
                                    Text(when(cat) {
                                        "general" -> "通用"
                                        "coding" -> "编程"
                                        "writing" -> "写作"
                                        "analysis" -> "分析"
                                        "education" -> "教育"
                                        else -> cat
                                    })
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, content) },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun RenameDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onAutoGenerate: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text("重命名对话") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("对话标题") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 自动生成标题按钮
                TextButton(
                    onClick = onAutoGenerate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("根据内容自动生成标题")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(title) },
                enabled = title.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ModelSelector(
    currentModel: String,
    availableModels: List<String>,
    onModelSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        val selectorShape = RoundedCornerShape(999.dp)
        Surface(
            modifier = Modifier
                .padding(top = 2.dp)
                .echoShapeClick(selectorShape) { expanded = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = selectorShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentModel.ifBlank { "未选择模型" },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        EchoGlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            if (availableModels.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(currentModel) },
                    onClick = { expanded = false },
                    leadingIcon = {
                        Icon(Icons.Default.Check, contentDescription = null)
                    }
                )
            } else {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = model,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        },
                        leadingIcon = {
                            if (model == currentModel) {
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
fun ModelSelector(
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    onModelSelected: (ChatModelOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(currentOption, fallbackModel, availableOptions) {
        val fallback = currentOption ?: ChatModelOption(
            apiConfigId = 0,
            configName = "",
            provider = "",
            apiType = "",
            modelName = fallbackModel
        )
        (availableOptions + fallback)
            .filter { it.modelName.isNotBlank() }
            .distinctBy { "${it.apiConfigId}:${it.modelName}" }
    }
    val currentLabel = currentOption?.modelName ?: fallbackModel

    Box {
        val selectorShape = RoundedCornerShape(999.dp)
        Surface(
            modifier = Modifier
                .padding(top = 2.dp)
                .echoShapeClick(selectorShape) { expanded = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = selectorShape
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = currentLabel.shortModelLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        EchoGlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("暂无可切换模型") },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { ModelOptionText(option = option) },
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

private data class ThinkingEffortOption(
    val value: String,
    val label: String
)

private data class ChatTuningProfile(
    val modelLabel: String,
    val temperatureMax: Float,
    val temperatureEnabled: Boolean,
    val thinkingEfforts: List<ThinkingEffortOption>,
    val noThinkingEffortReason: String? = null
)

private fun chatTuningProfile(
    currentOption: ChatModelOption?,
    fallbackModel: String,
    enableThinking: Boolean
): ChatTuningProfile {
    val modelName = currentOption?.modelName?.ifBlank { null } ?: fallbackModel
    val provider = currentOption?.provider.orEmpty()
    val cap = com.aiassistant.domain.model.ModelCapabilityEngine.resolveCapabilities(modelName, provider)

    val label = when (cap.reasoningProviderType) {
        "deepseek_fixed" -> "DeepSeek 推理"
        "openai" -> "OpenAI 推理"
        "anthropic" -> "Claude 思考"
        else -> if (provider.isNotBlank()) provider else "当前模型"
    }

    val efforts = if (enableThinking && cap.supportedThinkingGears.isNotEmpty()) {
        cap.supportedThinkingGears.map { gear ->
            val gearLabel = when (gear) {
                "low" -> "低"
                "medium" -> "中"
                "high" -> "高"
                "max" -> "最大"
                else -> gear
            }
            ThinkingEffortOption(gear, gearLabel)
        }
    } else {
        emptyList()
    }

    val reason = when {
        !enableThinking -> null
        cap.reasoningProviderType == "deepseek_fixed" -> "原生全量推理模型，默认全强度输出，无需设置档位。"
        cap.reasoningProviderType == "none" && cap.supportsThinking -> "该模型仅支持思考开关，无档位调节。"
        cap.reasoningProviderType == "none" -> "当前模型无官方思考档位参数，保持默认输出。"
        else -> null
    }

    val isDeepSeek = cap.reasoningProviderType == "deepseek_fixed" || modelName.lowercase().contains("deepseek")
    val tempMax = if (isDeepSeek) 2f else 1f
    val tempEnabled = !(enableThinking && (cap.reasoningProviderType == "openai" || cap.reasoningProviderType == "deepseek_fixed"))

    return ChatTuningProfile(
        modelLabel = label,
        temperatureMax = tempMax,
        temperatureEnabled = tempEnabled,
        thinkingEfforts = efforts,
        noThinkingEffortReason = reason
    )
}

@Composable
private fun ChatSettingsModelSelector(
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    contentColor: Color,
    secondaryColor: Color,
    onModelSelected: (ChatModelOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectorWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val currentLabel = currentOption?.modelName ?: fallbackModel

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("模型", style = MaterialTheme.typography.titleSmall, color = contentColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size ->
                    selectorWidth = with(density) { size.width.toDp() }
                }
        ) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentLabel.ifBlank { "未选择模型" },
                    modifier = Modifier.weight(1f),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = secondaryColor)
            }

            val menuModifier = if (selectorWidth > 0.dp) {
                Modifier
                    .width(selectorWidth)
                    .heightIn(max = 280.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            }

            EchoGlassDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = menuModifier
            ) {
                if (availableOptions.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("暂无可切换模型") },
                        onClick = { expanded = false }
                    )
                } else {
                    availableOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { ModelOptionText(option = option) },
                            enabled = option.apiConfigId > 0,
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

        val cap = remember(currentLabel) {
            com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(currentLabel)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cap.contextWindowDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "窗口: ${cap.contextWindowDisplay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (cap.supportsVision) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "🖼️ 视觉",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (cap.supportsTools) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "🛠️ 工具",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (cap.supportsReasoning) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "🧠 思考",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSettingsSystemPromptSection(
    promptTextFieldValue: TextFieldValue,
    onPromptChange: (TextFieldValue) -> Unit,
    hasTemplates: Boolean,
    contentColor: Color,
    secondaryColor: Color,
    onChooseTemplate: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showPriorityTip by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showPriorityTip = !showPriorityTip },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "系统提示词优先级说明",
                        tint = if (showPriorityTip) MaterialTheme.colorScheme.primary else secondaryColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("系统提示词", style = MaterialTheme.typography.titleSmall, color = contentColor)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                        contentDescription = if (isExpanded) "缩小输入框" else "放大输入框",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasTemplates) {
                    TextButton(
                        onClick = onChooseTemplate,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("模板", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (promptTextFieldValue.text.isNotBlank()) {
                    TextButton(
                        onClick = onSaveTemplate,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("存为模板", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showPriorityTip,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "【优先级说明】若设置了当前对话的系统提示词，将 100% 覆盖全局系统提示词；若留空则自动继承全局系统提示词。",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = secondaryColor
                    )
                }
            }
        }
        OutlinedTextField(
            value = promptTextFieldValue,
            onValueChange = onPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = if (isExpanded) 220.dp else 118.dp,
                    max = if (isExpanded) 360.dp else 160.dp
                ),
            placeholder = { Text("例如：你是一个专业、简洁、可靠的助手。") },
            maxLines = if (isExpanded) 16 else 8,
            shape = RoundedCornerShape(14.dp),
            colors = glassTextFieldColors(
                contentColor = contentColor,
                secondaryColor = secondaryColor,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
            )
        )
    }
}

@Composable
private fun glassTextFieldColors(
    contentColor: Color,
    secondaryColor: Color,
    containerColor: Color
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = contentColor,
    unfocusedTextColor = contentColor,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = containerColor,
    cursorColor = contentColor,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = secondaryColor,
    focusedPlaceholderColor = secondaryColor,
    unfocusedPlaceholderColor = secondaryColor,
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.62f),
    unfocusedBorderColor = secondaryColor.copy(alpha = 0.28f)
)

@Composable
fun ChatSettingsDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    tempSettings: TempChatSettings,
    currentPrompt: String?,
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    templates: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onSave: (TempChatSettings, String?) -> Unit,
    onModelSelected: (ChatModelOption) -> Unit,
    onSavePromptTemplate: (String, String) -> Unit,
    onModelAvatarChanged: () -> Unit,
    onConvertToRoleplay: () -> Unit = {}
) {
    val context = LocalContext.current
    val dialogContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
    val dialogContentColor = readableTextColorFor(
        background = dialogContainerColor,
        fallbackSurface = MaterialTheme.colorScheme.background
    )
    val dialogSecondaryColor = dialogContentColor.copy(alpha = 0.72f)
    var maxTokens by remember { mutableStateOf(tempSettings.maxTokens.toString()) }
    var topP by remember { mutableStateOf(tempSettings.topP) }
    var enableThinking by remember { mutableStateOf(tempSettings.enableThinking) }
    var thinkingEffort by remember { mutableStateOf(tempSettings.thinkingEffort) }
    var enableWebSearch by remember { mutableStateOf(tempSettings.enableWebSearch) }
    var promptTextFieldValue by remember(currentPrompt) {
        mutableStateOf(
            TextFieldValue(
                text = currentPrompt.orEmpty(),
                selection = TextRange(currentPrompt?.length ?: 0)
            )
        )
    }
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var avatarRevision by remember { mutableIntStateOf(0) }
    val dialogListState = rememberLazyListState()
    val modelOptions = remember(currentOption, fallbackModel, availableOptions) {
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
    val tuningProfile = remember(currentOption, fallbackModel, enableThinking) {
        chatTuningProfile(currentOption, fallbackModel, enableThinking)
    }
    var temperature by remember(tuningProfile.temperatureMax) {
        mutableStateOf(tempSettings.temperature.coerceIn(0f, tuningProfile.temperatureMax))
    }
    LaunchedEffect(tuningProfile.temperatureMax) {
        temperature = temperature.coerceIn(0f, tuningProfile.temperatureMax)
    }
    LaunchedEffect(enableThinking, tuningProfile.thinkingEfforts) {
        if (enableThinking && tuningProfile.thinkingEfforts.isNotEmpty() && thinkingEffort !in tuningProfile.thinkingEfforts.map { it.value }) {
            thinkingEffort = tuningProfile.thinkingEfforts.first().value
        }
    }
    val modelAvatarBitmap = remember(context, avatarRevision) {
        AvatarManager.getModelAvatarBitmap(context)
    }
    val modelAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (AvatarManager.saveModelAvatarFromUri(context, it)) {
                avatarRevision++
                onModelAvatarChanged()
            }
        }
    }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.90f)
            .widthIn(max = 430.dp),
        tint = dialogContainerColor,
        containerColor = dialogContainerColor,
        contentColor = dialogContentColor,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("对话设置", style = MaterialTheme.typography.titleLarge, color = dialogContentColor)
                    Text(
                        text = "当前对话配置会直接生效",
                        style = MaterialTheme.typography.bodySmall,
                        color = dialogSecondaryColor
                    )
                }
            }
        },
        content = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                state = dialogListState,
                contentPadding = PaddingValues(end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ChatSettingsModelSelector(
                        currentOption = currentOption,
                        fallbackModel = fallbackModel,
                        availableOptions = modelOptions,
                        contentColor = dialogContentColor,
                        secondaryColor = dialogSecondaryColor,
                        onModelSelected = onModelSelected
                    )
                }

                item {
                    ChatSettingsSystemPromptSection(
                        promptTextFieldValue = promptTextFieldValue,
                        onPromptChange = { promptTextFieldValue = it },
                        hasTemplates = templates.isNotEmpty(),
                        contentColor = dialogContentColor,
                        secondaryColor = dialogSecondaryColor,
                        onChooseTemplate = { showTemplates = true },
                        onSaveTemplate = { showSaveDialog = true }
                    )
                }

                // 温度
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (tuningProfile.temperatureEnabled) {
                                    "温度: ${String.format("%.2f", temperature)}"
                                } else {
                                    "温度: 思考模式下不可调"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = dialogContentColor
                            )
                            Text(
                                text = "越低严谨，越高富有想象力",
                                style = MaterialTheme.typography.labelSmall,
                                color = dialogSecondaryColor
                            )
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { newValue ->
                                // 精度为0.05
                                temperature = (newValue * 20).toInt() / 20f
                            },
                            valueRange = 0f..tuningProfile.temperatureMax,
                            steps = (tuningProfile.temperatureMax * 20).toInt().coerceAtLeast(1) - 1,
                            enabled = tuningProfile.temperatureEnabled
                        )
                        if (!tuningProfile.temperatureEnabled) {
                            Text(
                                text = "${tuningProfile.modelLabel} 的思考模式不支持调整温度，发送请求时会自动省略 temperature。",
                                style = MaterialTheme.typography.labelSmall,
                                color = dialogSecondaryColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("精确", style = MaterialTheme.typography.labelSmall, color = dialogSecondaryColor)
                            Text("平衡", style = MaterialTheme.typography.labelSmall, color = dialogSecondaryColor)
                            Text("发散", style = MaterialTheme.typography.labelSmall, color = dialogSecondaryColor)
                        }
                    }
                }

                // 最大Token
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("最大 Token 数", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                            Text("限制单次回复的最大生成长度", style = MaterialTheme.typography.labelSmall, color = dialogSecondaryColor)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = maxTokens,
                                onValueChange = { value -> maxTokens = value.filter { it.isDigit() }.take(6) },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("例如 8192") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = glassTextFieldColors(dialogContentColor, dialogSecondaryColor, dialogContainerColor)
                            )
                            Surface(
                                modifier = Modifier.height(54.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = echoGlassPalette().control,
                                contentColor = dialogSecondaryColor,
                                border = BorderStroke(1.dp, echoGlassPalette().outline),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("tokens", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        Text(
                            "留空会使用模型或全局配置的默认值。",
                            style = MaterialTheme.typography.labelSmall,
                            color = dialogSecondaryColor
                        )
                    }
                }

                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Top P: ${String.format("%.2f", topP)}",
                                style = MaterialTheme.typography.titleSmall,
                                color = dialogContentColor
                            )
                            Text("核采样概率阈值，控制用词发散程度", style = MaterialTheme.typography.labelSmall, color = dialogSecondaryColor)
                        }
                        Slider(
                            value = topP,
                            onValueChange = { newValue ->
                                topP = (newValue * 20).toInt() / 20f
                            },
                            valueRange = 0f..1f,
                            steps = 19
                        )
                    }
                }

                // 思考模式
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text("思考模式", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                            Text(
                                "支持时会传入真实思考参数；DeepSeek 官方 chat 会改用 reasoner",
                                style = MaterialTheme.typography.bodySmall,
                                color = dialogSecondaryColor
                            )
                        }
                        Switch(
                            checked = enableThinking,
                            onCheckedChange = { enableThinking = it }
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text("联网", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                            Text(
                                "仅对支持联网的API或模型生效",
                                style = MaterialTheme.typography.bodySmall,
                                color = dialogSecondaryColor
                            )
                        }
                        Switch(
                            checked = enableWebSearch,
                            onCheckedChange = { enableWebSearch = it }
                        )
                    }
                }

                // 思考强度
                if (enableThinking && tuningProfile.thinkingEfforts.isNotEmpty()) {
                    item {
                        Column {
                            Text("思考强度", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(tuningProfile.thinkingEfforts) { level ->
                                    val selected = thinkingEffort == level.value
                                    FilterChip(
                                        selected = selected,
                                        onClick = { thinkingEffort = level.value },
                                        colors = echoFilterChipColors(),
                                        border = echoFilterChipBorder(selected),
                                        elevation = echoFilterChipElevation(),
                                        label = {
                                            Text(level.label)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (enableThinking && tuningProfile.noThinkingEffortReason != null) {
                    item {
                        Text(
                            text = tuningProfile.noThinkingEffortReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = dialogSecondaryColor
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("模型头像", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (modelAvatarBitmap != null) {
                                    Image(
                                        bitmap = modelAvatarBitmap.asImageBitmap(),
                                        contentDescription = "模型头像",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.deepseek),
                                        contentDescription = "默认模型头像",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (modelAvatarBitmap == null) "当前使用 deepseek 默认头像" else "当前使用自定义头像",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dialogSecondaryColor
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { modelAvatarPicker.launch("image/*") },
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text("更换")
                                    }
                                    if (modelAvatarBitmap != null) {
                                        TextButton(
                                            onClick = {
                                                AvatarManager.deleteModelAvatar(context)
                                                avatarRevision++
                                                onModelAvatarChanged()
                                            }
                                        ) {
                                            Text("恢复默认")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    EchoGlassCard(
                        onClick = onConvertToRoleplay,
                        modifier = Modifier.fillMaxWidth(),
                        shape = EchoTokens.Radius.shapeMd,
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("转为角色扮演 / 故事创作", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                Text("平滑升级为故事会话，解锁角色卡、世界观与剧情推进指令", style = MaterialTheme.typography.bodySmall, color = dialogSecondaryColor)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = dialogSecondaryColor)
                        }
                    }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        val settings = TempChatSettings(
                            temperature = temperature.coerceIn(0f, tuningProfile.temperatureMax),
                            maxTokens = maxTokens.toIntOrNull() ?: 8192,
                            topP = topP,
                            enableThinking = enableThinking,
                            thinkingEffort = thinkingEffort,
                            enableWebSearch = enableWebSearch
                        )
                        onSave(settings, promptTextFieldValue.text.ifBlank { null })
                    }
                ) {
                    Text("保存")
                }
            }
        }
    )

    if (showTemplates) {
        TemplateListDialog(
            hazeState = hazeState,
            templates = templates,
            onDismiss = { showTemplates = false },
            onSelect = { template ->
                promptTextFieldValue = TextFieldValue(
                    text = template.content,
                    selection = TextRange(template.content.length)
                )
                showTemplates = false
            }
        )
    }

    if (showSaveDialog) {
        SaveTemplateDialog(
            hazeState = hazeState,
            content = promptTextFieldValue.text,
            onDismiss = { showSaveDialog = false },
            onSave = { name, content ->
                onSavePromptTemplate(name, content)
                showSaveDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryUnifiedSettingsDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    session: RoleplaySession,
    characters: List<CharacterProfile>,
    allCharacters: List<CharacterProfile>,
    scenario: RoleplayScenario?,
    allScenarios: List<RoleplayScenario>,
    narrativeMode: NarrativeMode,
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    tempSettings: TempChatSettings,
    currentPrompt: String?,
    templates: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onSaveAll: (
        selectedCharIds: List<Long>,
        selectedScenarioId: Long?,
        mode: NarrativeMode,
        plotSummary: String,
        newSettings: TempChatSettings,
        newPrompt: String?
    ) -> Unit,
    onPlotAction: (PlotAction, String?) -> Unit,
    onSummarizeMemories: () -> Unit,
    onNavigateToMemory: () -> Unit,
    onOpenSmartAppend: () -> Unit,
    onConvertToNormal: () -> Unit = {},
    onSaveLocalCharacter: (CharacterProfile) -> Unit = {},
    onDeleteLocalCharacter: (CharacterProfile) -> Unit = {},
    onSaveLocalScenario: (RoleplayScenario) -> Unit = {},
    onDeleteLocalScenario: () -> Unit = {},
    onModelSelected: (ChatModelOption) -> Unit,
    onSavePromptTemplate: (String, String) -> Unit,
    onModelAvatarChanged: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val dialogContentColor = if (isDark) Color.White.copy(alpha = 0.95f) else Color.Black.copy(alpha = 0.9f)
    val dialogSecondaryColor = if (isDark) Color.White.copy(alpha = 0.65f) else Color.Black.copy(alpha = 0.6f)

    var activeTab by remember { mutableIntStateOf(0) }
    var editingLocalCharacter by remember { mutableStateOf<CharacterProfile?>(null) }
    var editingLocalScenario by remember { mutableStateOf<RoleplayScenario?>(null) }

    // 故事与角色 Tab 状态
    val initialCharIds = remember(session, characters) {
        val ids = session.getEffectiveCharacterIds()
        if (ids.isNotEmpty()) ids.toSet() else characters.map { it.id }.toSet()
    }
    var selectedCharIds by remember { mutableStateOf(initialCharIds) }
    var selectedScenarioId by remember { mutableStateOf(session.scenarioId) }
    var selectedNarrativeMode by remember { mutableStateOf(narrativeMode) }
    var plotSummaryText by remember { mutableStateOf(session.currentPlotSummary) }
    var showCustomPlotDialog by remember { mutableStateOf(false) }
    var customInstructionText by remember { mutableStateOf("") }

    // 模型与参数 Tab 状态
    var temperature by remember { mutableFloatStateOf(tempSettings.temperature) }
    var maxTokens by remember {
        val currentMax = tempSettings.maxTokens.takeIf { it > 4096 } ?: 8192
        mutableStateOf(currentMax.toString())
    }
    var topP by remember { mutableFloatStateOf(tempSettings.topP) }
    var enableThinking by remember { mutableStateOf(tempSettings.enableThinking) }
    var thinkingEffort by remember { mutableStateOf(tempSettings.thinkingEffort) }
    var enableWebSearch by remember { mutableStateOf(tempSettings.enableWebSearch) }
    var promptTextFieldValue by remember(currentPrompt) {
        mutableStateOf(
            TextFieldValue(
                text = currentPrompt.orEmpty(),
                selection = TextRange(currentPrompt?.length ?: 0)
            )
        )
    }
    var isPromptExpanded by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var avatarRevision by remember { mutableIntStateOf(0) }
    val modelAvatarBitmap = remember(context, avatarRevision) {
        AvatarManager.getModelAvatarBitmap(context)
    }
    val modelAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (AvatarManager.saveModelAvatarFromUri(context, it)) {
                avatarRevision++
                onModelAvatarChanged()
            }
        }
    }

    val tuningProfile = remember(currentOption, fallbackModel, enableThinking) {
        chatTuningProfile(currentOption, fallbackModel, enableThinking)
    }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.90f)
            .widthIn(max = 430.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("故事创作与参数设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("世界观、登场角色与模型生成参数一站式管理", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScrollableTabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("🎭 剧情导向", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("👥 登场角色", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("🌍 世界观", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("📜 创作规范", fontWeight = if (activeTab == 3) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        text = { Text("⚙️ 模型参数", fontWeight = if (activeTab == 4) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 480.dp)) {
                    if (activeTab == 0) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 快捷 AI 追加
                            item {
                                EchoGlassCard(
                                    onClick = {
                                        onDismiss()
                                        onOpenSmartAppend()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = EchoTokens.Radius.shapeMd,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("AI 智能识别、追加与融合", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("粘贴小说章节或人设，实时并入当前故事", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }

                            // 转为普通对话操作
                            item {
                                EchoGlassCard(
                                    onClick = onConvertToNormal,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = EchoTokens.Radius.shapeMd,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("转为普通对话", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("将角色与世界观设定转译融合为普通系统提示词，降级为日常聊天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }

                            // 叙事模式选择
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("当前叙事模式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("点击即时切换导演/对话风格", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    NarrativeMode.values().forEach { mode ->
                                        val isSelected = selectedNarrativeMode == mode
                                        EchoGlassCard(
                                            onClick = { selectedNarrativeMode = mode },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = EchoTokens.Radius.shapeSm,
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Unspecified
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedNarrativeMode = mode }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(mode.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 剧情提示快捷动作
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("快捷剧情提示指令", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf(
                                            PlotAction.CONTINUE to "继续剧情",
                                            PlotAction.BRANCH_CHOICES to "决策分支",
                                            PlotAction.SUMMARY to "剧情摘要",
                                            PlotAction.REWRITE to "改写上一段",
                                            PlotAction.EXTEND to "延长描写",
                                            PlotAction.SHORTEN to "精简对白",
                                            PlotAction.CHANGE_PERSPECTIVE to "切换视角",
                                            PlotAction.CUSTOM to "自定义指令..."
                                        ).forEach { (action, label) ->
                                            OutlinedButton(
                                                onClick = {
                                                    if (action == PlotAction.CUSTOM) {
                                                        showCustomPlotDialog = true
                                                    } else {
                                                        onDismiss()
                                                        onPlotAction(action, null)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }

                            // 剧情摘要输入
                            item {
                                OutlinedTextField(
                                    value = plotSummaryText,
                                    onValueChange = { plotSummaryText = it },
                                    label = { Text("当前剧情摘要 / 备忘录") },
                                    placeholder = { Text("记录当前故事线推进到的关键阶段或核心暗线...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 4
                                )
                            }

                            // 记忆管理
                            item {
                                OutlinedButton(
                                    onClick = onNavigateToMemory,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("管理长期记忆与关键事实库")
                                }
                            }
                        }
                    } else if (activeTab == 1) {
                        // activeTab == 1: 登场角色独立列表与管理
                        val charListToDisplay = (allCharacters + characters).distinctBy { it.id }
                        val validCharIds = charListToDisplay.map { it.id }.toSet()
                        val effectiveSelectedCharIds = selectedCharIds.filter { validCharIds.contains(it) }.toSet()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("登场角色 (${effectiveSelectedCharIds.size}/${charListToDisplay.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    OutlinedButton(
                                        onClick = {
                                            editingLocalCharacter = CharacterProfile(id = 0, name = "")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("添加新角色", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            if (charListToDisplay.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("暂无角色卡，点击上方「添加新角色」立即为故事编排登场人物。", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            } else {
                                items(charListToDisplay.size) { idx ->
                                    val char = charListToDisplay[idx]
                                    val isChecked = effectiveSelectedCharIds.contains(char.id)
                                    EchoGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    selectedCharIds = if (isChecked) {
                                                        effectiveSelectedCharIds - char.id
                                                    } else {
                                                        effectiveSelectedCharIds + char.id
                                                    }
                                                },
                                                onLongClick = {
                                                    editingLocalCharacter = char
                                                }
                                            ),
                                        shape = EchoTokens.Radius.shapeSm,
                                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Unspecified
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedCharIds = if (checked) effectiveSelectedCharIds + char.id else effectiveSelectedCharIds - char.id
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(char.name + if (char.identity.isNotBlank()) " · ${char.identity}" else "", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                if (char.personality.isNotBlank()) {
                                                    Text("性格: ${char.personality}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                }
                                            }
                                            IconButton(
                                                onClick = { editingLocalCharacter = char },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑角色", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { onDeleteLocalCharacter(char) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "从故事移除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (activeTab == 2) {
                        // activeTab == 2: 世界观与场景独立列表
                        val scenarioListToDisplay = (allScenarios + listOfNotNull(scenario)).distinctBy { it.id }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("世界观与场景设定", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    OutlinedButton(
                                        onClick = {
                                            editingLocalScenario = RoleplayScenario(id = 0, name = "")
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("添加新世界观", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            item {
                                EchoGlassCard(
                                    onClick = { selectedScenarioId = null },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = EchoTokens.Radius.shapeSm,
                                    containerColor = if (selectedScenarioId == null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Unspecified
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selectedScenarioId == null, onClick = { selectedScenarioId = null })
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("不指定世界观（自由开放背景）", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            items(scenarioListToDisplay.size) { idx ->
                                val sc = scenarioListToDisplay[idx]
                                val isSelected = selectedScenarioId == sc.id
                                EchoGlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { selectedScenarioId = sc.id },
                                            onLongClick = { editingLocalScenario = sc }
                                        ),
                                    shape = EchoTokens.Radius.shapeSm,
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Unspecified
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = isSelected, onClick = { selectedScenarioId = sc.id })
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sc.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            if (sc.worldview.isNotBlank()) {
                                                Text(sc.worldview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                            }
                                        }
                                        IconButton(
                                            onClick = { editingLocalScenario = sc },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "编辑世界观", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteLocalScenario() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "从故事移除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else if (activeTab == 3) {
                        // activeTab == 3: 创作规范与提示词 (教学模型如何创作)
                        val personalizationMgr = remember { AiAssistantApp.instance.personalizationManager }
                        val initialGlobalRpPrompt = remember { personalizationMgr.getSettings().globalRoleplayPrompt }
                        var globalRoleplayPromptText by remember { mutableStateOf(initialGlobalRpPrompt) }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 本故事系统提示词
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("本故事专属系统提示词", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = dialogContentColor)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { isPromptExpanded = !isPromptExpanded },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPromptExpanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                                                    contentDescription = if (isPromptExpanded) "缩小" else "放大",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (templates.isNotEmpty()) {
                                                TextButton(
                                                    onClick = { showTemplates = true },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("模板", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                            if (promptTextFieldValue.text.isNotBlank()) {
                                                TextButton(
                                                    onClick = { showSaveDialog = true },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("存模板", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                    Text("仅作用于当前故事，指导本故事特定的叙事基调、伏笔暗线或风格限制", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedTextField(
                                        value = promptTextFieldValue,
                                        onValueChange = { promptTextFieldValue = it },
                                        placeholder = { Text("例如：采用冷硬派侦探小说笔触，多用客观白描，强化悬疑氛围...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(
                                                min = if (isPromptExpanded) 160.dp else 80.dp,
                                                max = if (isPromptExpanded) 260.dp else 120.dp
                                            ),
                                        minLines = if (isPromptExpanded) 5 else 2,
                                        maxLines = if (isPromptExpanded) 10 else 4,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // 全局创作教学规范
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("全局角色创作规范与教学指引", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = dialogContentColor)
                                        TextButton(
                                            onClick = {
                                                globalRoleplayPromptText = com.aiassistant.data.repository.RoleplayRepository.DEFAULT_FICTION_TEACHING_GUIDELINES
                                                personalizationMgr.saveSettings(personalizationMgr.getSettings().copy(globalRoleplayPrompt = globalRoleplayPromptText))
                                            },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("恢复默认文学规范", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text("作用于所有角色扮演故事，用于教学模型如何创作故事、行文规范与沉浸感", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    OutlinedTextField(
                                        value = globalRoleplayPromptText,
                                        onValueChange = {
                                            globalRoleplayPromptText = it
                                            personalizationMgr.saveSettings(personalizationMgr.getSettings().copy(globalRoleplayPrompt = it))
                                        },
                                        placeholder = { Text("留空将使用内置文学创作铁律（以演代述、神态微表情描写、禁止出戏性格副词）...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp, max = 180.dp),
                                        minLines = 3,
                                        maxLines = 8,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // 机制说明卡片
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("💡 创作提示词分层作用机制说明", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("1. 全局创作规范：向模型传授文学创作方法论（以演代述、避免性格副词、台词动作节奏），全局共用。", style = MaterialTheme.typography.bodySmall)
                                        Text("2. 本故事专属系统提示词：指导本故事特定的情节、题材与世界限制，优先级高于全局提示词。", style = MaterialTheme.typography.bodySmall)
                                        Text("3. 登场角色卡与场景卡：作为独立结构化卡片拼入上下文，与剧情提示词解耦，确保人设永不走样。", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    } else {
                        // activeTab == 4: 模型与参数
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                ChatSettingsModelSelector(
                                    currentOption = currentOption,
                                    fallbackModel = fallbackModel,
                                    availableOptions = availableOptions,
                                    contentColor = dialogContentColor,
                                    secondaryColor = dialogSecondaryColor,
                                    onModelSelected = onModelSelected
                                )
                            }

                            item {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("温度 (Temperature)", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text(String.format(Locale.getDefault(), "%.2f", temperature), style = MaterialTheme.typography.bodyMedium, color = dialogSecondaryColor)
                                    }
                                    Slider(
                                        value = temperature,
                                        onValueChange = { temperature = it },
                                        valueRange = 0f..tuningProfile.temperatureMax,
                                        steps = 20
                                    )
                                }
                            }

                            item {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("最大输出 (Max Tokens)", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text("默认 8192+", style = MaterialTheme.typography.bodySmall, color = dialogSecondaryColor)
                                    }
                                    OutlinedTextField(
                                        value = maxTokens,
                                        onValueChange = { maxTokens = it.filter { char -> char.isDigit() } },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }

                            item {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Top P (核采样)", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text(String.format(Locale.getDefault(), "%.2f", topP), style = MaterialTheme.typography.bodyMedium, color = dialogSecondaryColor)
                                    }
                                    Slider(
                                        value = topP,
                                        onValueChange = { topP = it },
                                        valueRange = 0f..1f,
                                        steps = 20
                                    )
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("深度思考模式", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text(
                                            if (tuningProfile.noThinkingEffortReason != null) {
                                                tuningProfile.noThinkingEffortReason
                                            } else {
                                                "适合复杂情节构思与严谨逻辑推演"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = dialogSecondaryColor
                                        )
                                    }
                                    Switch(
                                        checked = enableThinking,
                                        onCheckedChange = { enableThinking = it }
                                    )
                                }
                            }

                            if (enableThinking && tuningProfile.thinkingEfforts.isNotEmpty()) {
                                item {
                                    Column {
                                        Text("思考强度档位", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(tuningProfile.thinkingEfforts) { level ->
                                                val selected = thinkingEffort == level.value
                                                FilterChip(
                                                    selected = selected,
                                                    onClick = { thinkingEffort = level.value },
                                                    colors = echoFilterChipColors(),
                                                    border = echoFilterChipBorder(selected),
                                                    elevation = echoFilterChipElevation(),
                                                    label = { Text(level.label) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("联网搜索", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text("仅对支持联网的 API 生效", style = MaterialTheme.typography.bodySmall, color = dialogSecondaryColor)
                                    }
                                    Switch(
                                        checked = enableWebSearch,
                                        onCheckedChange = { enableWebSearch = it }
                                    )
                                }
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("模型头像", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (modelAvatarBitmap != null) {
                                                Image(
                                                    bitmap = modelAvatarBitmap.asImageBitmap(),
                                                    contentDescription = "模型头像",
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Image(
                                                    painter = painterResource(id = R.drawable.deepseek),
                                                    contentDescription = "默认模型头像",
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { modelAvatarPicker.launch("image/*") },
                                                    shape = RoundedCornerShape(999.dp)
                                                ) {
                                                    Text("更换")
                                                }
                                                if (modelAvatarBitmap != null) {
                                                    TextButton(
                                                        onClick = {
                                                            AvatarManager.deleteModelAvatar(context)
                                                            avatarRevision++
                                                            onModelAvatarChanged()
                                                        }
                                                    ) {
                                                        Text("恢复默认")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val newSettings = TempChatSettings(
                            temperature = temperature.coerceIn(0f, tuningProfile.temperatureMax),
                            maxTokens = maxTokens.toIntOrNull() ?: 8192,
                            topP = topP,
                            enableThinking = enableThinking,
                            thinkingEffort = thinkingEffort,
                            enableWebSearch = enableWebSearch
                        )
                        val charListToDisplay = (allCharacters + characters).distinctBy { it.id }
                        val validCharIds = charListToDisplay.map { it.id }.toSet()
                        val finalCharIds = selectedCharIds.filter { validCharIds.contains(it) }.toList()

                        onSaveAll(
                            finalCharIds,
                            selectedScenarioId,
                            selectedNarrativeMode,
                            plotSummaryText,
                            newSettings,
                            promptTextFieldValue.text.ifBlank { null }
                        )
                    }
                ) {
                    Text("保存")
                }
            }
        }
    )

    if (showTemplates) {
        TemplateListDialog(
            hazeState = hazeState,
            templates = templates,
            onDismiss = { showTemplates = false },
            onSelect = { template ->
                promptTextFieldValue = TextFieldValue(
                    text = template.content,
                    selection = TextRange(template.content.length)
                )
                showTemplates = false
            }
        )
    }

    if (showSaveDialog) {
        SaveTemplateDialog(
            hazeState = hazeState,
            content = promptTextFieldValue.text,
            onDismiss = { showSaveDialog = false },
            onSave = { name, content ->
                onSavePromptTemplate(name, content)
                showSaveDialog = false
            }
        )
    }

    if (showCustomPlotDialog) {
        EchoGlassDialog(
            onDismissRequest = { showCustomPlotDialog = false },
            title = { Text("输入自定义剧情指令") },
            text = {
                OutlinedTextField(
                    value = customInstructionText,
                    onValueChange = { customInstructionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("剧情提示 / 导演要求") },
                    placeholder = { Text("例如：接下来让他们在雨夜车站再次相遇...") },
                    maxLines = 4
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customInstructionText.isNotBlank()) {
                            onPlotAction(PlotAction.CUSTOM, customInstructionText)
                            showCustomPlotDialog = false
                        }
                    },
                    enabled = customInstructionText.isNotBlank()
                ) {
                    Text("发送指令")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPlotDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (editingLocalCharacter != null) {
        Dialog(
            onDismissRequest = { editingLocalCharacter = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                com.aiassistant.ui.screens.roleplay.CharacterEditorScreen(
                    character = if (editingLocalCharacter?.name?.isNotBlank() == true) editingLocalCharacter else null,
                    onSave = { updatedChar ->
                        onSaveLocalCharacter(updatedChar)
                        editingLocalCharacter = null
                    },
                    onDelete = {
                        if (editingLocalCharacter != null && editingLocalCharacter!!.name.isNotBlank()) {
                            onDeleteLocalCharacter(editingLocalCharacter!!)
                        }
                        editingLocalCharacter = null
                    },
                    onBack = { editingLocalCharacter = null }
                )
            }
        }
    }

    if (editingLocalScenario != null) {
        Dialog(
            onDismissRequest = { editingLocalScenario = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                com.aiassistant.ui.screens.roleplay.ScenarioEditorScreen(
                    scenario = if (editingLocalScenario?.name?.isNotBlank() == true) editingLocalScenario else null,
                    onSave = { updatedSc ->
                        onSaveLocalScenario(updatedSc)
                        editingLocalScenario = null
                    },
                    onDelete = {
                        onDeleteLocalScenario()
                        editingLocalScenario = null
                    },
                    onBack = { editingLocalScenario = null }
                )
            }
        }
    }
}

@Composable
private fun EditableSettingProposalDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    proposal: ProposedSettingBundle,
    onDismiss: () -> Unit,
    onApply: (List<CharacterProfile>, RoleplayScenario?) -> Unit
) {
    var updatedChars by remember(proposal) {
        mutableStateOf(proposal.updatedCharacters.map { it.character })
    }
    var newChars by remember(proposal) {
        mutableStateOf(proposal.newCharacters.map { it.character })
    }
    var scenario by remember(proposal) {
        mutableStateOf(proposal.scenario)
    }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("AI 精准识别到设定融入与更新", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(proposal.summary.ifBlank { "已结合已知设定智能分类更新与新角色" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        content = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 更新已有角色
                if (updatedChars.isNotEmpty()) {
                    item {
                        Text("🔄 更新已有角色设定 (${updatedChars.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    itemsIndexed(updatedChars) { index, char ->
                        val summaryText = proposal.updatedCharacters.getOrNull(index)?.summaryOfChanges ?: "设定变动"
                        EchoGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = EchoTokens.Radius.shapeMd
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("【${char.name}】 $summaryText", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                    IconButton(
                                        onClick = { updatedChars = updatedChars.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "忽略此更新", modifier = Modifier.size(16.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = char.identity,
                                    onValueChange = { newId -> updatedChars = updatedChars.toMutableList().also { it[index] = char.copy(identity = newId) } },
                                    label = { Text("身份 / 职业") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = char.personality,
                                    onValueChange = { newP -> updatedChars = updatedChars.toMutableList().also { it[index] = char.copy(personality = newP) } },
                                    label = { Text("性格特质") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                                OutlinedTextField(
                                    value = char.background,
                                    onValueChange = { newBg -> updatedChars = updatedChars.toMutableList().also { it[index] = char.copy(background = newBg) } },
                                    label = { Text("背景与补充经历") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }

                // 2. 发现新登场角色
                if (newChars.isNotEmpty()) {
                    item {
                        Text("➕ 发现新登场角色 (${newChars.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    itemsIndexed(newChars) { index, char ->
                        val summaryText = proposal.newCharacters.getOrNull(index)?.summaryOfChanges ?: "新角色"
                        EchoGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = EchoTokens.Radius.shapeMd
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("新角色：【${char.name}】", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyMedium)
                                    IconButton(
                                        onClick = { newChars = newChars.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "不添加此角色", modifier = Modifier.size(16.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = char.name,
                                    onValueChange = { newName -> newChars = newChars.toMutableList().also { it[index] = char.copy(name = newName) } },
                                    label = { Text("姓名") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = char.identity,
                                    onValueChange = { newId -> newChars = newChars.toMutableList().also { it[index] = char.copy(identity = newId) } },
                                    label = { Text("身份 / 职业") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = char.personality,
                                    onValueChange = { newP -> newChars = newChars.toMutableList().also { it[index] = char.copy(personality = newP) } },
                                    label = { Text("性格特质") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                                OutlinedTextField(
                                    value = char.background,
                                    onValueChange = { newBg -> newChars = newChars.toMutableList().also { it[index] = char.copy(background = newBg) } },
                                    label = { Text("背景经历") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }

                // 3. 世界观更新
                scenario?.let { sc ->
                    item {
                        Text("🌍 世界观与规则设定更新", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    item {
                        val summaryText = proposal.scenarioUpdate?.summaryOfChanges ?: "世界观完善"
                        EchoGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = EchoTokens.Radius.shapeMd
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(summaryText, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                    IconButton(
                                        onClick = { scenario = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "忽略世界观更新", modifier = Modifier.size(16.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = sc.name,
                                    onValueChange = { newName -> scenario = sc.copy(name = newName) },
                                    label = { Text("世界观名称") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = sc.worldview,
                                    onValueChange = { newWv -> scenario = sc.copy(worldview = newWv) },
                                    label = { Text("世界观法则与背景") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                OutlinedTextField(
                                    value = sc.rules,
                                    onValueChange = { newRules -> scenario = sc.copy(rules = newRules) },
                                    label = { Text("不可违背的法则") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                                OutlinedTextField(
                                    value = sc.premise,
                                    onValueChange = { newPremise -> scenario = sc.copy(premise = newPremise) },
                                    label = { Text("当前剧情前提") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("放弃")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val finalChars = updatedChars + newChars
                        onApply(finalChars, scenario)
                    }
                ) {
                    Text("决定添加并融合")
                }
            }
        }
    )
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) }
    )
}

@Composable
private fun SmartAppendStoryDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    onDismiss: () -> Unit,
    onAppendAndMerge: (List<CharacterProfile>, RoleplayScenario?, Map<String, ConflictAction>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { com.aiassistant.AiAssistantApp.instance.repository }
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var analyzeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var extractedBundle by remember { mutableStateOf<com.aiassistant.utils.AnalyzedRoleplayBundle?>(null) }

    val apiConfigs by repository.getAllApiConfigs().collectAsState(initial = emptyList())
    var selectedConfig by remember { mutableStateOf<com.aiassistant.domain.model.ApiConfig?>(null) }

    LaunchedEffect(apiConfigs) {
        if (selectedConfig == null && apiConfigs.isNotEmpty()) {
            selectedConfig = apiConfigs.firstOrNull { it.isDefault } ?: apiConfigs.firstOrNull()
        }
    }

    if (extractedBundle == null) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = {
                if (isAnalyzing) {
                    analyzeJob?.cancel()
                    isAnalyzing = false
                }
                onDismiss()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("智能识别与实时追加")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "在此粘贴小说新章节、新角色档案或世界观设定，AI 将自动识别提炼并智能融合到当前故事会话中。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        label = { Text("粘贴设定/正文文本") },
                        placeholder = { Text("粘贴内容...") },
                        enabled = !isAnalyzing
                    )
                    if (isAnalyzing) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = progressStatus.ifBlank { "正在分析中..." },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = {
                                    analyzeJob?.cancel()
                                    isAnalyzing = false
                                    Toast.makeText(context, "已终止分析", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("停止", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                EchoPrimaryButton(
                    onClick = {
                        analyzeJob = scope.launch {
                            isAnalyzing = true
                            progressStatus = "正在提取人设与世界观..."
                            try {
                                val bundle = RoleplaySmartAnalyzer.analyzeTextOrNovel(
                                    context = context,
                                    rawText = inputText,
                                    repository = repository,
                                    preferredConfig = selectedConfig,
                                    selectedModel = selectedConfig?.modelName.orEmpty(),
                                    onProgress = { progressStatus = it }
                                )
                                extractedBundle = bundle
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                Toast.makeText(context, "解析已取消", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "分析失败: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isAnalyzing = false
                            }
                        }
                    },
                    enabled = inputText.isNotBlank() && !isAnalyzing
                ) {
                    Text(if (isAnalyzing) "解析中..." else "开始解析")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (isAnalyzing) {
                            analyzeJob?.cancel()
                            isAnalyzing = false
                        }
                        onDismiss()
                    }
                ) {
                    Text(if (isAnalyzing) "终止" else "取消")
                }
            }
        )
    } else {
        val bundle = extractedBundle!!
        val resolutionMap = remember { mutableStateMapOf<String, ConflictAction>() }
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { extractedBundle = null },
            title = { Text("确认追加/融合至本故事") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("识别到 ${bundle.characters.size} 位角色与 ${if (bundle.scenario != null) 1 else 0} 个世界观设定：", style = MaterialTheme.typography.labelSmall)
                    bundle.characters.forEach { char ->
                        EchoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(char.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                if (char.identity.isNotBlank()) Text("身份: ${char.identity}", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ConflictAction.values().forEach { action ->
                                        val isSelected = (resolutionMap[char.name.trim()] ?: ConflictAction.MERGE) == action
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { resolutionMap[char.name.trim()] = action },
                                            label = { Text(action.displayName, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    bundle.scenario?.let { sc ->
                        EchoGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("世界观：${sc.name}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                if (sc.worldview.isNotBlank()) Text(sc.worldview, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                EchoPrimaryButton(
                    onClick = {
                        onAppendAndMerge(bundle.characters, bundle.scenario, resolutionMap.toMap())
                    }
                ) {
                    Text("确认融合并入")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text("放弃")
                    }
                    TextButton(onClick = { extractedBundle = null }) {
                        Text("上一步")
                    }
                }
            }
        )
    }
}

data class CitationInfo(
    val index: Int,
    val title: String,
    val url: String,
    val snippet: String = ""
)

fun extractCitationsFromContent(content: String): List<CitationInfo> {
    val list = mutableListOf<CitationInfo>()
    val regex = Regex("""\[(\d+)\]\s*\[(.*?)\]\((https?://[^\s)]+)\)""")
    regex.findAll(content).forEach { match ->
        val id = match.groupValues[1].toIntOrNull() ?: (list.size + 1)
        val title = match.groupValues[2].ifBlank { "参考网页 $id" }
        val url = match.groupValues[3].trim()
        if (list.none { it.url == url || it.index == id }) {
            list.add(CitationInfo(id, title, url))
        }
    }
    if (list.isEmpty()) {
        val generalRegex = Regex("""\[(.*?)\]\((https?://[^\s)]+)\)""")
        generalRegex.findAll(content).forEachIndexed { idx, match ->
            val title = match.groupValues[1].ifBlank { "参考网页 ${idx + 1}" }
            val url = match.groupValues[2].trim()
            if (list.none { it.url == url }) {
                list.add(CitationInfo(idx + 1, title, url))
            }
        }
    }
    return list
}

@Composable
fun CitationsCardsRow(
    citations: List<CitationInfo>,
    onCitationClick: (CitationInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = echoGlassPalette()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "参考资料来源 (${citations.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            citations.forEach { citation ->
                val cardShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .clip(cardShape)
                        .background(glass.control.copy(alpha = 0.65f))
                        .border(BorderStroke(0.8.dp, glass.outline.copy(alpha = 0.5f)), cardShape)
                        .echoShapeClick(cardShape) { onCitationClick(citation) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "[${citation.index}]",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = citation.title,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CitationDetailDialog(
    citation: CitationInfo,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val glass = echoGlassPalette()
    var copied by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(22.dp),
            color = glass.panelStrong,
            border = BorderStroke(1.dp, glass.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[${citation.index}]",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = citation.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "参考网址来源 (可长按文本选中)：",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.08f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = citation.url,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(citation.url))
                            copied = true
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "已复制网址" else "复制网址")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            runCatching<Unit> { uriHandler.openUri(citation.url) }
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("打开网页")
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCallsFooter(
    toolCalls: List<ToolCallRecord>,
    modifier: Modifier = Modifier
) {
    if (toolCalls.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    var selectedRecordForDialog by remember { mutableStateOf<ToolCallRecord?>(null) }
    val glass = echoGlassPalette()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = glass.control.copy(alpha = if (isDarkTheme) 0.5f else 0.7f),
        border = BorderStroke(0.8.dp, glass.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 头部摘要栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "成功调用 ${toolCalls.size} 项系统与联网工具",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "收起留痕" else "查看留痕",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 展开的工具调用记录列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    toolCalls.forEach { record ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRecordForDialog = record },
                            shape = RoundedCornerShape(10.dp),
                            color = glass.controlSelected.copy(alpha = 0.4f),
                            border = BorderStroke(0.6.dp, glass.outlineSelected.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val icon = when (record.toolType) {
                                    "WEATHER" -> Icons.Default.Cloud
                                    "TIME_CALENDAR" -> Icons.Default.Schedule
                                    "HEALTH" -> Icons.Default.DirectionsWalk
                                    "DEVICE_HARDWARE" -> Icons.Default.Smartphone
                                    "LOCATION" -> Icons.Default.LocationOn
                                    "JINA_READER" -> Icons.Default.MenuBook
                                    else -> Icons.Default.Search
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = record.toolName,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = record.toolName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "执行成功",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (record.summary.isNotBlank()) {
                                        Text(
                                            text = record.summary,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { selectedRecordForDialog = record },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("详情", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedRecordForDialog != null) {
        ToolCallDetailDialog(
            record = selectedRecordForDialog!!,
            onDismiss = { selectedRecordForDialog = null }
        )
    }
}

@Composable
fun ToolCallDetailDialog(
    record: ToolCallRecord,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(record.toolName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("执行摘要：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(record.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("工具获取并注入模型的完整上下文数据：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = record.detailContent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(record.detailContent))
                    android.widget.Toast.makeText(context, "已复制工具数据到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("复制数据")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
