@file:OptIn(ExperimentalMaterial3Api::class)

package com.aiassistant.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.asImageBitmap
import com.aiassistant.R
import com.aiassistant.domain.model.Attachment
import com.aiassistant.domain.model.ChatModelOption
import com.aiassistant.domain.model.ConversationContextUsage
import com.aiassistant.domain.model.Message
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.EchoGlassBackground
import com.aiassistant.ui.components.GlassSurface
import com.aiassistant.ui.components.MarkdownText
import com.aiassistant.ui.components.SideAnchorItem
import com.aiassistant.ui.components.SideAnchorNavigator
import com.aiassistant.ui.components.TransientLazyListScrollbar
import com.aiassistant.ui.components.rememberLazyListControlsVisible
import com.aiassistant.utils.AvatarManager
import com.aiassistant.utils.BackgroundManager
import com.aiassistant.utils.FileUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
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
    val useTempSettings by viewModel.useTempSettings.collectAsState()
    val contextUsage by viewModel.contextUsage.collectAsState()

    val listState = rememberLazyListState()
    val showScrollControls by rememberLazyListControlsVisible(listState)
    val clipboardManager = LocalClipboardManager.current
    val promptTemplates by viewModel.promptTemplates.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showContextUsageDialog by remember { mutableStateOf(false) }
    var selectedAttachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var isProcessingAttachments by remember { mutableStateOf(false) }
    var attachmentStatus by remember { mutableStateOf<String?>(null) }
    var modelAvatarRevision by remember { mutableIntStateOf(0) }
    var pendingEditSource by remember { mutableStateOf<Message?>(null) }
    var preserveScrollForBranchGeneration by remember { mutableStateOf(false) }
    var streamingBranchGroupId by remember { mutableStateOf<String?>(null) }
    var autoFollowOutput by remember { mutableStateOf(true) }
    val variantSelections = remember { mutableStateMapOf<String, Int>() }
    val variantSelectionSnapshot = variantSelections.toMap()
    val displayMessages = remember(messages, variantSelectionSnapshot) {
        buildDisplayMessages(messages, variantSelectionSnapshot)
    }
    val chatNavItems = remember(displayMessages) {
        buildChatAnchorItems(displayMessages)
    }
    val chatBackground = remember(context) {
        BackgroundManager.getChatBackgroundBitmap(context)
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

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisibleIndex = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = info.totalItemsCount
            ScrollFollowSnapshot(
                isScrolling = listState.isScrollInProgress,
                lastVisibleIndex = lastVisibleIndex,
                totalItems = totalItems
            )
        }.collect { snapshot ->
            if (snapshot.isScrolling && snapshot.totalItems > 0) {
                autoFollowOutput = snapshot.lastVisibleIndex >= snapshot.totalItems - 3
            }
        }
    }

    // 自动滚动到底部
    LaunchedEffect(messages.size, currentResponse.length, currentThinking.length, isGenerating) {
        if (preserveScrollForBranchGeneration) {
            return@LaunchedEffect
        }
        if (!autoFollowOutput) {
            return@LaunchedEffect
        }
        if (messages.isNotEmpty() || currentResponse.isNotEmpty() || currentThinking.isNotEmpty()) {
            kotlinx.coroutines.delay(16)
            val bottomIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            listState.scrollToItem(bottomIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ChatHeaderTitle(
                        title = uiState.conversationTitle.ifBlank { "新对话" },
                        currentOption = currentModelOption,
                        fallbackModel = currentModel ?: uiState.modelName,
                        availableOptions = availableModelOptions,
                        useTempSettings = useTempSettings,
                        isGenerating = isGenerating,
                        onModelSelected = { viewModel.switchModel(it) }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.leaveConversation(onNavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    ContextUsageButton(
                        usage = contextUsage.usage,
                        canCompress = contextUsage.usage?.canCompress == true,
                        onClick = {
                            viewModel.refreshContextUsage()
                            showContextUsageDialog = true
                        }
                    )
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "对话设置",
                            tint = if (useTempSettings) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showSystemPromptDialog = true }) {
                        Icon(Icons.Default.Psychology, contentDescription = "系统提示词")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChange = { inputText = it },
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
                onClearAttachments = {
                    selectedAttachments = emptyList()
                    attachmentStatus = null
                },
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
                currentOption = currentModelOption,
                fallbackModel = currentModel ?: uiState.modelName,
                availableOptions = availableModelOptions,
                onModelSelected = { viewModel.switchModel(it) }
            )
        }
    ) { paddingValues ->
        EchoGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            backgroundBitmap = chatBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 错误提示
                error?.let { errorMsg ->
                    AnimatedVisibility(visible = true) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearError() }) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭")
                                }
                            }
                        }
                    }
                }

                // 消息列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 空状态
                    if (displayMessages.isEmpty() && currentResponse.isEmpty() && currentThinking.isEmpty()) {
                        item {
                            EmptyChatPlaceholder()
                        }
                    }

                    // 消息列表
                    items(
                        items = displayMessages,
                        key = { item -> item.groupId ?: item.message.id }
                    ) { displayItem ->
                        val message = displayItem.message
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MessageBubble(
                                message = message,
                                assistantAvatarRevision = modelAvatarRevision,
                                assistantApiConfigId = currentModelOption?.apiConfigId,
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
                                    isGenerating = true,
                                    assistantAvatarRevision = modelAvatarRevision,
                                    assistantApiConfigId = currentModelOption?.apiConfigId,
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
                    if (streamingBranchGroupId == null && (currentThinking.isNotEmpty() || currentResponse.isNotEmpty())) {
                        item {
                            MessageBubble(
                                message = Message(
                                    conversationId = conversationId,
                                    role = "assistant",
                                    content = currentResponse,
                                    thinkingContent = currentThinking.ifEmpty { null }
                                ),
                                isGenerating = true,
                                assistantAvatarRevision = modelAvatarRevision,
                                assistantApiConfigId = currentModelOption?.apiConfigId,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(currentResponse))
                                },
                                onCopyThinking = {
                                    clipboardManager.setText(AnnotatedString(currentThinking))
                                }
                            )
                        }
                    }

                    // 生成中的加载指示器
                    if (streamingBranchGroupId == null && isGenerating && currentResponse.isEmpty() && currentThinking.isEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                ChatAvatar(
                                    isUser = false,
                                    avatarRevision = modelAvatarRevision,
                                    apiConfigId = currentModelOption?.apiConfigId
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "正在努力思考...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
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
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 12.dp)
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
                    .padding(end = 18.dp, bottom = 18.dp)
            )
        }
    }

    // 系统提示词对话框
    if (showSystemPromptDialog) {
        SystemPromptDialog(
            currentPrompt = uiState.systemPrompt,
            onDismiss = { showSystemPromptDialog = false },
            onSave = { prompt ->
                viewModel.updateSystemPrompt(prompt)
                showSystemPromptDialog = false
            },
            onSaveAsTemplate = { name, content ->
                viewModel.savePromptTemplate(name, content)
            },
            templates = promptTemplates
        )
    }

    // 设置对话框
    if (showSettingsDialog) {
        ChatSettingsDialog(
            tempSettings = tempSettings,
            useTempSettings = useTempSettings,
            currentOption = currentModelOption,
            fallbackModel = currentModel ?: uiState.modelName,
            onDismiss = { showSettingsDialog = false },
            onSave = { settings, enabled ->
                if (enabled) {
                    viewModel.updateTempSettings(settings)
                } else {
                    viewModel.toggleTempSettings(false)
                }
                showSettingsDialog = false
            },
            onModelAvatarChanged = { modelAvatarRevision++ }
        )
    }

    if (showContextUsageDialog) {
        ContextUsageDialog(
            state = contextUsage,
            onDismiss = { showContextUsageDialog = false },
            onRefresh = { viewModel.refreshContextUsage() },
            onCompress = { viewModel.compressContextNow() }
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
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "一键到顶")
            }
            SmallFloatingActionButton(
                onClick = onJumpToBottom,
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
    val percentText = usage?.let { "${(usagePercent * 100).toInt().coerceIn(0, 999)}%" } ?: "--%"

    Surface(
        modifier = Modifier
            .height(36.dp)
            .padding(end = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.12f),
        contentColor = accent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Default.Memory,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = percentText,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
            if (canCompress) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ContextUsageDialog(
    state: ContextUsageUiState,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onCompress: () -> Unit
) {
    val usage = state.usage
    val canCompress = usage?.canCompress == true && !state.isCompressing

    AlertDialog(
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
                        text = "当前对话的预算估算",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
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
        confirmButton = {
            Button(
                onClick = onCompress,
                enabled = canCompress
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
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onRefresh,
                    enabled = !state.isCompressing
                ) {
                    Text("刷新")
                }
                TextButton(onClick = onDismiss) {
                    Text("关闭")
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "输入预算",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${formatTokenCount(usage.estimatedInputTokens)} / ${formatTokenCount(usage.promptBudgetTokens)} tokens",
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

@Composable
private fun ChatHeaderTitle(
    title: String,
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    useTempSettings: Boolean,
    isGenerating: Boolean,
    onModelSelected: (ChatModelOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (useTempSettings) {
                HeaderStatusChip(
                    icon = Icons.Default.Tune,
                    checked = true,
                    text = "本对话配置"
                )
            }
            if (isGenerating) {
                HeaderStatusChip(
                    icon = Icons.Default.Bolt,
                    text = "生成中"
                )
            }
        }
    }
}

@Composable
private fun HeaderStatusChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean = false,
    text: String
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
            if (checked) {
                Spacer(modifier = Modifier.width(3.dp))
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
            } else {
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = text, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
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
)

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

@Composable
private fun MessageBubble(
    message: Message,
    isGenerating: Boolean = false,
    assistantAvatarRevision: Int = 0,
    assistantApiConfigId: Long? = null,
    variantInfo: VariantInfo? = null,
    onVariantSelected: (String, Int) -> Unit = { _, _ -> },
    onCopy: () -> Unit,
    onCopyThinking: () -> Unit,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isUser = message.role == "user"
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val bubbleShape = if (isUser) {
        RoundedCornerShape(18.dp, 6.dp, 18.dp, 18.dp)
    } else {
        RoundedCornerShape(6.dp, 18.dp, 18.dp, 18.dp)
    }
    var showThinking by remember(message.id, isGenerating) { mutableStateOf(isGenerating) }
    val hasThinking = !message.thinkingContent.isNullOrBlank()

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

    @Composable
    fun MessageContent(contentColor: Color) {
        Column(
            modifier = if (isUser) {
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp)
            }
        ) {
            if (hasThinking) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showThinking = !showThinking },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isGenerating) "正在努力思考" else "思考过程",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onCopyThinking,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制思考",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Icon(
                                if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = showThinking) {
                            Text(
                                text = message.thinkingContent ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

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
                        color = contentColor
                    )
                }
            } else if (!isGenerating && !hasThinking && attachments.isEmpty()) {
                Text(
                    text = "空消息",
                    color = contentColor.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (isGenerating) {
                if (message.content.isNotBlank() || hasThinking) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                TypingIndicator(textColor = contentColor)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val bubbleMaxWidth = if (isUser) {
            (maxWidth - 52.dp).coerceAtLeast(160.dp).coerceAtMost(360.dp)
        } else {
            (maxWidth - 46.dp).coerceAtLeast(180.dp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!isUser) {
                ChatAvatar(
                    isUser = false,
                    avatarRevision = assistantAvatarRevision,
                    apiConfigId = assistantApiConfigId
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = bubbleMaxWidth),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                if (attachments.isNotEmpty()) {
                    AttachmentGroupBubble(
                        attachments = attachments,
                        modifier = Modifier
                            .widthIn(max = bubbleMaxWidth)
                            .padding(bottom = if (message.content.isNotBlank() || isGenerating) 8.dp else 0.dp)
                    )
                }

                if (isUser && (message.content.isNotBlank() || isGenerating)) {
                    Surface(
                        color = bubbleColor,
                        contentColor = textColor,
                        shape = bubbleShape,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        MessageContent(textColor)
                    }
                } else {
                    if (!isUser || message.content.isNotBlank() || isGenerating) {
                        MessageContent(textColor)
                    }
                }

                Row(
                    modifier = Modifier
                        .widthIn(max = bubbleMaxWidth)
                        .padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = formatMessageClock(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (message.responseTime > 0) {
                        Text(
                            text = formatTime(message.responseTime),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (message.tokenCount > 0) {
                        Text(
                            text = "${message.tokenCount} tokens",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (message.thinkingTokens > 0) {
                        Text(
                            text = "思考: ${message.thinkingTokens}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "复制",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isUser && onRegenerate != null) {
                        IconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "重新生成",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isUser && onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "重新编辑",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "删除本条",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                variantInfo?.let { info ->
                    VariantSwitcher(
                        info = info,
                        onSelect = { index -> onVariantSelected(info.groupId, index) },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                ChatAvatar(isUser = true)
            }
        }
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
        color = textColor.copy(alpha = 0.6f),
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
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
fun AttachmentChip(attachment: Attachment) {
    val isImage = FileUtils.isImage(attachment.mimeType, attachment.name)
    val hasOcr = !attachment.ocrText.isNullOrBlank() || attachment.processingNote?.contains("OCR") == true

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    onStopGeneration: () -> Unit,
    attachments: List<Attachment>,
    onRemoveAttachment: (Attachment) -> Unit,
    isProcessingAttachments: Boolean,
    attachmentStatus: String?,
    onClearAttachments: () -> Unit,
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onOcrImages: () -> Unit,
    enableWebSearch: Boolean,
    onWebSearchChange: (Boolean) -> Unit,
    currentOption: ChatModelOption?,
    fallbackModel: String,
    availableOptions: List<ChatModelOption>,
    onModelSelected: (ChatModelOption) -> Unit
) {
    var showToolMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
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
                                onRemove = { onRemoveAttachment(attachment) }
                            )
                        }
                    }
                }

                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp, max = 112.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 5,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 42.dp)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isBlank()) {
                                Text(
                                    text = "给 Echo 发送消息",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        item {
                            InputPillButton(
                                text = "智能搜索",
                                selected = enableWebSearch,
                                onClick = { onWebSearchChange(!enableWebSearch) }
                            )
                        }
                        item {
                            InputModelSelector(
                                currentOption = currentOption,
                                fallbackModel = fallbackModel,
                                availableOptions = availableOptions,
                                onModelSelected = onModelSelected
                            )
                        }

                        attachmentStatus?.let { status ->
                            item {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(status, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    leadingIcon = {
                                        if (isProcessingAttachments) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    val softButtonColors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
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
                        DropdownMenu(
                            expanded = showToolMenu,
                            onDismissRequest = { showToolMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("图片") },
                                onClick = {
                                    showToolMenu = false
                                    onPickImage()
                                },
                                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("文件") },
                                onClick = {
                                    showToolMenu = false
                                    onPickFile()
                                },
                                leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("图片 OCR") },
                                onClick = {
                                    showToolMenu = false
                                    onOcrImages()
                                },
                                leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) }
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
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    onRemove: () -> Unit
) {
    val isImage = FileUtils.isImage(attachment.mimeType, attachment.name)
    val hasOcr = !attachment.ocrText.isNullOrBlank() || attachment.processingNote?.contains("OCR") == true

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = attachment.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
                Text(
                    text = listOfNotNull(
                        FileUtils.formatFileSize(attachment.size),
                        attachment.processingNote
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.size(14.dp)
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
    currentPrompt: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
    onSaveAsTemplate: ((String, String) -> Unit)? = null,
    templates: List<PromptTemplate> = emptyList()
) {
    var promptText by remember { mutableStateOf(currentPrompt ?: "") }
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    AlertDialog(
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
    templates: List<PromptTemplate>,
    onDismiss: () -> Unit,
    onSelect: (PromptTemplate) -> Unit
) {
    val categories = remember(templates) { templates.map { it.category }.distinct() }

    AlertDialog(
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
    content: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("general") }

    AlertDialog(
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
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
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
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onAutoGenerate: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
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
        Surface(
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(999.dp)
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
        Surface(
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(999.dp)
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
fun ChatSettingsDialog(
    tempSettings: TempChatSettings,
    useTempSettings: Boolean,
    currentOption: ChatModelOption?,
    fallbackModel: String,
    onDismiss: () -> Unit,
    onSave: (TempChatSettings, Boolean) -> Unit,
    onModelAvatarChanged: () -> Unit
) {
    val context = LocalContext.current
    var maxTokens by remember { mutableStateOf(tempSettings.maxTokens.toString()) }
    var topP by remember { mutableStateOf(tempSettings.topP) }
    var enableThinking by remember { mutableStateOf(tempSettings.enableThinking) }
    var thinkingEffort by remember { mutableStateOf(tempSettings.thinkingEffort) }
    var enableWebSearch by remember { mutableStateOf(tempSettings.enableWebSearch) }
    var isEnabled by remember { mutableStateOf(useTempSettings) }
    var avatarRevision by remember { mutableIntStateOf(0) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("对话设置", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (isEnabled) "当前对话配置生效" else "使用模型默认配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                contentPadding = PaddingValues(end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "这些设置仅对当前对话有效，不会影响全局配置",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 温度
                item {
                    Column {
                        Text(
                            text = if (tuningProfile.temperatureEnabled) {
                                "温度: ${String.format("%.2f", temperature)}"
                            } else {
                                "温度: 思考模式下不可调"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = temperature,
                            onValueChange = { newValue ->
                                // 精度为0.05
                                temperature = (newValue * 20).toInt() / 20f
                            },
                            valueRange = 0f..tuningProfile.temperatureMax,
                            steps = (tuningProfile.temperatureMax * 20).toInt().coerceAtLeast(1) - 1,
                            enabled = isEnabled && tuningProfile.temperatureEnabled
                        )
                        if (!tuningProfile.temperatureEnabled) {
                            Text(
                                text = "${tuningProfile.modelLabel} 的思考模式不支持调整温度，发送请求时会自动省略 temperature。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("精确", style = MaterialTheme.typography.labelSmall)
                            Text("平衡", style = MaterialTheme.typography.labelSmall)
                            Text("发散", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // 最大Token
                item {
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("最大Token数") },
                        singleLine = true,
                        enabled = isEnabled
                    )
                }

                item {
                    Column {
                        Text(
                            text = "Top P: ${String.format("%.2f", topP)}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Slider(
                            value = topP,
                            onValueChange = { newValue ->
                                topP = (newValue * 20).toInt() / 20f
                            },
                            valueRange = 0f..1f,
                            steps = 19,
                            enabled = isEnabled
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
                            Text("思考模式", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "支持时会传入真实思考参数；DeepSeek 官方 chat 会改用 reasoner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableThinking,
                            onCheckedChange = { enableThinking = it },
                            enabled = isEnabled
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
                            Text("联网", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "仅对支持联网的API或模型生效",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableWebSearch,
                            onCheckedChange = { enableWebSearch = it },
                            enabled = isEnabled
                        )
                    }
                }

                // 思考强度
                if (enableThinking && tuningProfile.thinkingEfforts.isNotEmpty()) {
                    item {
                        Column {
                            Text("思考强度", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(tuningProfile.thinkingEfforts) { level ->
                                    FilterChip(
                                        selected = thinkingEffort == level.value,
                                        onClick = { thinkingEffort = level.value },
                                        label = {
                                            Text(level.label)
                                        },
                                        enabled = isEnabled
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("模型头像", style = MaterialTheme.typography.titleSmall)
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        confirmButton = {
            TextButton(
                onClick = {
                    val settings = TempChatSettings(
                        temperature = temperature.coerceIn(0f, tuningProfile.temperatureMax),
                        maxTokens = maxTokens.toIntOrNull() ?: 4096,
                        topP = topP,
                        enableThinking = enableThinking,
                        thinkingEffort = thinkingEffort,
                        enableWebSearch = enableWebSearch
                    )
                    onSave(settings, isEnabled)
                }
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
