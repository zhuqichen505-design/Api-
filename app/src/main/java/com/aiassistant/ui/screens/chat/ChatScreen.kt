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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.aiassistant.AiAssistantApp
import com.aiassistant.R
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

    val hazeState = rememberEchoHazeState()
    val readableBackdrops = rememberReadableBackdropColors(chatBackgroundBitmap)
    val listState = rememberLazyListState()
    val showScrollControls by rememberLazyListControlsVisible(listState)
    val clipboardManager = LocalClipboardManager.current
    val promptTemplates by viewModel.promptTemplates.collectAsState()

    val roleplayRepo = remember { com.aiassistant.AiAssistantApp.instance.roleplayRepository }
    val allAvailableCharacters by roleplayRepo.getAllCharacters().collectAsState(initial = emptyList())
    val allAvailableScenarios by roleplayRepo.getAllScenarios().collectAsState(initial = emptyList())

    var inputText by remember(conversationId) { mutableStateOf(ChatViewModel.getDraft(conversationId)) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember(uiState.conversationTitle) { mutableStateOf(uiState.conversationTitle) }
    var showSettingsDialog by remember { mutableStateOf(false) }
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                                    streamingBranchGroupId = editSource.variantGroupId
                                        ?.let { pairedVariantGroupId(it) }
                                        ?: "turn_${editSource.id}_assistant"
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
                    onThinkingChange = { enabled ->
                        viewModel.updateTempSettings(tempSettings.copy(enableThinking = enabled))
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MessageBubble(
                                message = message,
                                hazeState = hazeState,
                                readableBackdrop = readableBackdrops.content,
                                assistantAvatarRevision = modelAvatarRevision,
                                assistantApiConfigId = currentModelOption?.apiConfigId,
                                assistantModelName = resolvedAssistantModelName,
                                variantInfo = displayItem.variantInfo,
                                onVariantSelected = { groupId, index ->
                                    variantSelections[groupId] = index
                                    pairedVariantGroupId(groupId)?.let { pairedGroup ->
                                        if (messages.any { it.variantGroupId == pairedGroup && it.variantIndex == index }) {
                                            variantSelections[pairedGroup] = index
                                        }
                                    }
                                },
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

                            if (
                                streamingBranchGroupId != null &&
                                displayItem.groupId == streamingBranchGroupId &&
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
            onModelAvatarChanged = { modelAvatarRevision++ }
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
        AlertDialog(
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
            progress = progress,
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

private fun formatThinkingCapsuleText(
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
    var showThinking by remember(message.id, isGenerating, hasThinkingContent) {
        mutableStateOf(isGenerating && hasThinkingContent)
    }

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
                        onCopy = onCopy,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        modifier = Modifier
                            .widthIn(max = bubbleMaxWidth)
                            .fillMaxWidth()
                    )

                    variantInfo?.let { info ->
                        VariantSwitcher(
                            info = info,
                            onSelect = { index -> onVariantSelected(info.groupId, index) },
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
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
                val thinkingContentColor = MaterialTheme.colorScheme.primary

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
                            else -> model
                        }
                    }

                    val isBlueCapsule = hasThinking || isConnecting
                    val capsuleShape = RoundedCornerShape(999.dp)
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 34.dp)
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
                                        tint = if (isBlueCapsule) thinkingBubbleColor else glass.control,
                                        blurRadius = 12.dp,
                                        highlightAlpha = 0.03f
                                    )
                                } else Modifier
                            ),
                        color = if (isBlueCapsule) thinkingBubbleColor else glass.control,
                        contentColor = if (isBlueCapsule) thinkingContentColor else MaterialTheme.colorScheme.primary,
                        shape = capsuleShape,
                        border = BorderStroke(
                            1.dp,
                            if (isBlueCapsule) glass.outlineSelected.copy(alpha = 0.72f) else glass.outline
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
                                    tint = thinkingContentColor
                                )
                            } else {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = capsuleText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.5.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (hasThinking) thinkingContentColor else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasThinking && hasThinkingContent) {
                                Icon(
                                    imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showThinking) "收起" else "展开",
                                    modifier = Modifier.size(16.dp),
                                    tint = thinkingContentColor.copy(alpha = 0.78f)
                                )
                            }
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
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, glass.outlineSelected.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "思考内容详情",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = onCopyThinking,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "复制思考",
                                            modifier = Modifier.size(14.dp),
                                            tint = thinkingContentColor.copy(alpha = 0.78f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.thinkingContent ?: "",
                                    style = MaterialTheme.typography.bodySmall,
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

                    MessageFooter(
                        isUser = false,
                        message = message,
                        onCopy = onCopy,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        modifier = Modifier.fillMaxWidth()
                    )

                    variantInfo?.let { info ->
                        VariantSwitcher(
                            info = info,
                            onSelect = { index -> onVariantSelected(info.groupId, index) },
                            modifier = Modifier.padding(top = 2.dp)
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
        FlowRow(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalArrangement = Arrangement.spacedBy(2.dp)
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

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    onThinkingChange: (Boolean) -> Unit = {},
    isRoleplay: Boolean = false,
    onPlotActionClick: () -> Unit = {},
    readableBackdrop: Color = Color.Unspecified
) {
    var showToolMenu by remember { mutableStateOf(false) }
    val inputShape = RoundedCornerShape(30.dp)
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
                        .heightIn(min = 42.dp, max = 112.dp)
                        .background(Color.Transparent),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = inputTextColor,
                        background = Color.Transparent
                    ),
                    cursorBrush = SolidColor(inputTextColor),
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                                .background(Color.Transparent)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isBlank()) {
                                Text(
                                    text = if (isRoleplay) "输入剧情提示、行动或指令..." else "给 Echo 发送消息",
                                    color = inputTextColor.copy(alpha = 0.62f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
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
                        // 1. 深度思考 按钮（排在第1位）
                        item {
                            InputPillButton(
                                text = "深度思考",
                                selected = enableThinking,
                                onClick = { onThinkingChange(!enableThinking) },
                                containerColor = if (enableThinking) {
                                    glass.controlSelected
                                } else {
                                    glass.control
                                },
                                contentColor = if (enableThinking) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    inputTextColor
                                },
                                borderColor = if (enableThinking) {
                                    glass.outlineSelected
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
                                    icon = Icons.Default.AltRoute,
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
    Column {
        Text(
            text = option.modelName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (option.configName.isNotBlank() || option.provider.isNotBlank()) {
            Text(
                text = listOf(option.configName, option.provider)
                    .filter { it.isNotBlank() }
                    .joinToString(" · "),
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
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
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
    val identity = listOfNotNull(
        currentOption?.provider,
        currentOption?.configName,
        currentOption?.modelName,
        fallbackModel
    ).joinToString(" ").lowercase()
    val isDeepSeek = "deepseek" in identity
    val isMiMo = "mimo" in identity || "xiaomi" in identity
    val label = when {
        isDeepSeek -> "DeepSeek"
        isMiMo -> "MiMo"
        else -> "当前模型"
    }

    return when {
        isDeepSeek -> ChatTuningProfile(
            modelLabel = label,
            temperatureMax = 2f,
            temperatureEnabled = !enableThinking,
            thinkingEfforts = if (enableThinking) {
                listOf(
                    ThinkingEffortOption("high", "高"),
                    ThinkingEffortOption("max", "最大")
                )
            } else {
                emptyList()
            }
        )
        isMiMo -> ChatTuningProfile(
            modelLabel = label,
            temperatureMax = 1f,
            temperatureEnabled = !enableThinking,
            thinkingEfforts = emptyList(),
            noThinkingEffortReason = if (enableThinking) "MiMo 的思考模式没有强度选项。" else null
        )
        else -> ChatTuningProfile(
            modelLabel = label,
            temperatureMax = 1f,
            temperatureEnabled = true,
            thinkingEfforts = if (enableThinking) {
                listOf(
                    ThinkingEffortOption("low", "低"),
                    ThinkingEffortOption("medium", "中"),
                    ThinkingEffortOption("high", "高")
                )
            } else {
                emptyList()
            }
        )
    }
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
    val currentLabel = currentOption?.modelName ?: fallbackModel

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("模型", style = MaterialTheme.typography.titleSmall, color = contentColor)
        Box {
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

            EchoGlassDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 280.dp)
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
    }
}

@Composable
private fun ChatSettingsSystemPromptSection(
    promptText: String,
    onPromptChange: (String) -> Unit,
    hasTemplates: Boolean,
    contentColor: Color,
    secondaryColor: Color,
    onChooseTemplate: () -> Unit,
    onSaveTemplate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("系统提示词", style = MaterialTheme.typography.titleSmall, color = contentColor)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onChooseTemplate,
                enabled = hasTemplates,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("选择模板")
            }
            OutlinedButton(
                onClick = onSaveTemplate,
                enabled = promptText.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存模板")
            }
        }
        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 118.dp),
            placeholder = { Text("例如：你是一个专业、简洁、可靠的助手。") },
            maxLines = 8,
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
    onModelAvatarChanged: () -> Unit
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
    var promptText by remember(currentPrompt) { mutableStateOf(currentPrompt.orEmpty()) }
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
            .fillMaxWidth()
            .widthIn(max = 560.dp),
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
                    Text(
                        text = "这些设置仅对当前对话有效，不会影响全局配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = dialogSecondaryColor
                    )
                }

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
                        promptText = promptText,
                        onPromptChange = { promptText = it },
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
                        onSave(settings, promptText.ifBlank { null })
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
                promptText = template.content
                showTemplates = false
            }
        )
    }

    if (showSaveDialog) {
        SaveTemplateDialog(
            hazeState = hazeState,
            content = promptText,
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
    var promptText by remember { mutableStateOf(currentPrompt ?: "") }
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
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("📖 故事与角色", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("⚙️ 模型与参数", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }

                Box(modifier = Modifier.heightIn(max = 440.dp)) {
                    if (activeTab == 0) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
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

                            // 登场角色勾选列表
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("登场角色 (${selectedCharIds.size}/${(allCharacters + characters).distinctBy { it.id }.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                    val charListToDisplay = (allCharacters + characters).distinctBy { it.id }
                                    if (charListToDisplay.isEmpty()) {
                                        Text("暂无角色卡，可点击上方「添加新角色」", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        charListToDisplay.forEach { char ->
                                            val isChecked = selectedCharIds.contains(char.id)
                                            EchoGlassCard(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .combinedClickable(
                                                        onClick = {
                                                            selectedCharIds = if (isChecked) {
                                                                selectedCharIds - char.id
                                                            } else {
                                                                selectedCharIds + char.id
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
                                                            selectedCharIds = if (checked) selectedCharIds + char.id else selectedCharIds - char.id
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
                            }

                            // 世界观与场景设定单选
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            Text("不指定世界观（自由背景）", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    val scenarioListToDisplay = (allScenarios + listOfNotNull(scenario)).distinctBy { it.id }
                                    scenarioListToDisplay.forEach { sc ->
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
                                                RadioButton(selected = isSelected, onClick = { selectedNarrativeMode = mode })
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(mode.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 剧情推进快捷指令
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("剧情导演指令", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("即时引导或改写故事", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ActionChip(text = "🎬 剧情走向选择") { onPlotAction(PlotAction.BRANCH_CHOICES, null) }
                                        ActionChip(text = "⚡ 继续推进") { onPlotAction(PlotAction.CONTINUE, null) }
                                        ActionChip(text = "🔄 重新生成") { onPlotAction(PlotAction.REGENERATE, null) }
                                        ActionChip(text = "✏️ 重写上一段") { onPlotAction(PlotAction.REWRITE, null) }
                                        ActionChip(text = "➕ 扩写细节") { onPlotAction(PlotAction.EXTEND, null) }
                                        ActionChip(text = "➖ 精简提炼") { onPlotAction(PlotAction.SHORTEN, null) }
                                        ActionChip(text = "👁️ 切换视角") { onPlotAction(PlotAction.CHANGE_PERSPECTIVE, null) }
                                        ActionChip(text = "📝 剧情摘要") { onPlotAction(PlotAction.SUMMARY, null) }
                                        ActionChip(text = "💬 自定义指令") { showCustomPlotDialog = true }
                                    }
                                }
                            }

                            // 一键提炼剧情摘要与记忆
                            item {
                                EchoGlassCard(
                                    onClick = onSummarizeMemories,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = EchoTokens.Radius.shapeSm,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("一键提炼剧情摘要与关键事实", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("通过AI分析上下文，自动更新故事记忆与事实库", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // 剧情备忘录/摘要编辑
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
                    } else {
                        // activeTab == 1: 模型与参数
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
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
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("系统提示词", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (templates.isNotEmpty()) {
                                                TextButton(onClick = { showTemplates = true }) {
                                                    Text("使用模板")
                                                }
                                            }
                                            if (promptText.isNotBlank()) {
                                                TextButton(onClick = { showSaveDialog = true }) {
                                                    Text("存为模板")
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = promptText,
                                        onValueChange = { promptText = it },
                                        placeholder = { Text("为故事或助手设定全局指导规则...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                }
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
                                        Text("适合复杂情节构思与严谨逻辑推演", style = MaterialTheme.typography.bodySmall, color = dialogSecondaryColor)
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
                        onSaveAll(
                            selectedCharIds.toList(),
                            selectedScenarioId,
                            selectedNarrativeMode,
                            plotSummaryText,
                            newSettings,
                            promptText.ifBlank { null }
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
                promptText = template.content
                showTemplates = false
            }
        )
    }

    if (showSaveDialog) {
        SaveTemplateDialog(
            hazeState = hazeState,
            content = promptText,
            onDismiss = { showSaveDialog = false },
            onSave = { name, content ->
                onSavePromptTemplate(name, content)
                showSaveDialog = false
            }
        )
    }

    if (showCustomPlotDialog) {
        AlertDialog(
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
                            Icons.Default.OpenInNew,
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
