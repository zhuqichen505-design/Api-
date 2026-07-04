@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.aiassistant.ui.screens.home

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiassistant.R
import com.aiassistant.domain.model.ApiConfig
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.Folder
import com.aiassistant.ui.components.SideAnchorItem
import com.aiassistant.ui.components.SideAnchorNavigator
import com.aiassistant.ui.components.TransientLazyListScrollbar
import com.aiassistant.ui.components.echoShapeClick
import com.aiassistant.ui.components.echoHazePanel
import com.aiassistant.ui.components.echoHazeSource
import com.aiassistant.ui.components.readableTextColorFor
import com.aiassistant.ui.components.rememberEchoHazeState
import com.aiassistant.ui.components.rememberLazyListControlsVisible
import com.aiassistant.utils.AvatarManager
import com.aiassistant.utils.BackgroundImageManager
import java.text.SimpleDateFormat
import java.util.*

private val HomeSelectedChipColor = Color(0xFFF0F8FF)
private val DeleteDialogPink = Color(0xFFFFE4E6)
private val DeleteDialogContent = Color(0xFFBE123C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToFolders: () -> Unit
) {
    val context = LocalContext.current
    val homeBackgroundBitmap = remember(context) {
        BackgroundImageManager.getHomeBackgroundBitmap(context)
    }
    val viewModel: HomeViewModel = viewModel()
    val conversations by viewModel.conversations.collectAsState()
    val apiConfigs by viewModel.apiConfigs.collectAsState()
    val selectedConfig by viewModel.selectedConfig.collectAsState()
    val showNewChatDialog by viewModel.showNewChatDialog.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val hazeState = rememberEchoHazeState()
    val conversationListState = rememberLazyListState()
    val showScrollControls by rememberLazyListControlsVisible(conversationListState)
    var searchQuery by remember { mutableStateOf("") }
    var pinnedExpanded by rememberSaveable { mutableStateOf(true) }
    var selectedConversationIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBatchMoveDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    val filteredConversations = remember(conversations, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            conversations
        } else {
            conversations.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.modelName.contains(query, ignoreCase = true) ||
                    it.systemPrompt?.contains(query, ignoreCase = true) == true
            }
        }
    }
    val visibleConversationIds = remember(filteredConversations) {
        filteredConversations.map { it.id }.toSet()
    }
    val selectedConversations = remember(filteredConversations, selectedConversationIds) {
        filteredConversations.filter { it.id in selectedConversationIds }
    }
    val homeNavItems = remember(filteredConversations, pinnedExpanded) {
        buildHomeAnchorItems(filteredConversations, pinnedExpanded)
    }
    val isSelectionMode = selectedConversationIds.isNotEmpty()
    val selectedAreAllInFolders = selectedConversations.isNotEmpty() &&
        selectedConversations.all { it.folderId != null }

    fun toggleConversationSelection(conversationId: Long) {
        selectedConversationIds = if (conversationId in selectedConversationIds) {
            selectedConversationIds - conversationId
        } else {
            selectedConversationIds + conversationId
        }
    }

    LaunchedEffect(visibleConversationIds) {
        val visibleSelection = selectedConversationIds.intersect(visibleConversationIds)
        if (visibleSelection.size != selectedConversationIds.size) {
            selectedConversationIds = visibleSelection
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (conversations.isNotEmpty() && !isSelectionMode) {
                NewConversationFab(
                    hazeState = hazeState,
                    onClick = {
                        viewModel.createDefaultConversation { conversationId ->
                            onNavigateToChat(conversationId)
                        }
                    },
                    onLongClick = { viewModel.showNewChatDialog() }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            homeBackgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.fillMaxSize()) {
                HomeDashboardHeader(
                    hazeState = hazeState,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToStats = onNavigateToStats
                )

                HomeSearchRow(
                    hazeState = hazeState,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onNavigateToHistory = onNavigateToHistory
                )

                // 文件夹选择栏
                FolderSelector(
                    hazeState = hazeState,
                    folders = folders,
                    selectedFolderId = selectedFolderId,
                    onFolderSelected = { viewModel.selectFolder(it) },
                    onManageFolders = onNavigateToFolders
                )

            AnimatedVisibility(visible = isSelectionMode) {
                BatchSelectionBar(
                    selectedCount = selectedConversationIds.size,
                    moveActionText = if (selectedAreAllInFolders) "\u79fb\u51fa\u6587\u4ef6\u5939" else "\u79fb\u5165\u6587\u4ef6\u5939",
                    onClearSelection = { selectedConversationIds = emptySet() },
                    onPinSelected = {
                        viewModel.setPinned(selectedConversationIds, true)
                        selectedConversationIds = emptySet()
                    },
                    onUnpinSelected = {
                        viewModel.setPinned(selectedConversationIds, false)
                        selectedConversationIds = emptySet()
                    },
                    onMoveSelected = {
                        if (selectedAreAllInFolders) {
                            viewModel.moveConversationsToFolder(selectedConversationIds, null)
                            selectedConversationIds = emptySet()
                        } else {
                            showBatchMoveDialog = true
                        }
                    },
                    onHideSelected = {
                        viewModel.hideConversations(selectedConversationIds)
                        selectedConversationIds = emptySet()
                    },
                    onDeleteSelected = {
                        showBatchDeleteDialog = true
                    }
                )
            }

            if (filteredConversations.isEmpty()) {
                EmptyHomeContent(
                    modifier = Modifier.weight(1f),
                    onStartChat = {
                        viewModel.createDefaultConversation { conversationId ->
                            onNavigateToChat(conversationId)
                        }
                    },
                    isSearching = searchQuery.isNotBlank()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .echoHazeSource(hazeState),
                        state = conversationListState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 置顶对话
                        val pinnedConversations = filteredConversations.filter { it.isPinned }
                        if (pinnedConversations.isNotEmpty()) {
                            item {
                                PinnedSectionHeader(
                                    count = pinnedConversations.size,
                                    expanded = pinnedExpanded,
                                    onToggle = { pinnedExpanded = !pinnedExpanded }
                                )
                            }
                            if (pinnedExpanded) {
                                items(
                                    items = pinnedConversations,
                                    key = { "pinned_${it.id}" }
                                ) { conversation ->
                                    ConversationCard(
                                        conversation = conversation,
                                        selected = conversation.id in selectedConversationIds,
                                        selectionMode = isSelectionMode,
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleConversationSelection(conversation.id)
                                            } else {
                                                onNavigateToChat(conversation.id)
                                            }
                                        },
                                        onLongClick = { toggleConversationSelection(conversation.id) },
                                        onDelete = { viewModel.deleteConversation(conversation) },
                                        onPin = { viewModel.togglePin(conversation) },
                                        onMoveToFolder = { folderId ->
                                            viewModel.moveToFolder(conversation.id, folderId)
                                        },
                                        onRename = { newTitle ->
                                            viewModel.renameConversation(conversation.id, newTitle)
                                        },
                                        onHide = {
                                            viewModel.hideConversations(listOf(conversation.id))
                                        },
                                        onCreateFolder = { name, icon, color ->
                                            viewModel.createFolder(name, icon, color)
                                        },
                                        onUpdateFolder = { folder, name, icon, color ->
                                            viewModel.updateFolder(folder, name, icon, color)
                                        },
                                        folders = folders
                                    )
                                }
                            }
                        }

                        // 普通对话
                        val normalConversations = filteredConversations.filter { !it.isPinned }
                        if (normalConversations.isNotEmpty() && pinnedConversations.isNotEmpty()) {
                            item {
                                Text(
                                    text = "其他",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                        items(
                            items = normalConversations,
                            key = { it.id }
                        ) { conversation ->
                            ConversationCard(
                                conversation = conversation,
                                selected = conversation.id in selectedConversationIds,
                                selectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        toggleConversationSelection(conversation.id)
                                    } else {
                                        onNavigateToChat(conversation.id)
                                    }
                                },
                                onLongClick = { toggleConversationSelection(conversation.id) },
                                onDelete = { viewModel.deleteConversation(conversation) },
                                onPin = { viewModel.togglePin(conversation) },
                                onMoveToFolder = { folderId ->
                                    viewModel.moveToFolder(conversation.id, folderId)
                                },
                                onRename = { newTitle ->
                                    viewModel.renameConversation(conversation.id, newTitle)
                                },
                                onHide = {
                                    viewModel.hideConversations(listOf(conversation.id))
                                },
                                onCreateFolder = { name, icon, color ->
                                    viewModel.createFolder(name, icon, color)
                                },
                                onUpdateFolder = { folder, name, icon, color ->
                                    viewModel.updateFolder(folder, name, icon, color)
                                },
                                folders = folders
                            )
                        }

                        // 底部间距
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                    TransientLazyListScrollbar(
                        listState = conversationListState,
                        visible = showScrollControls,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                    )
                }
            }
        }
            SideAnchorNavigator(
                items = homeNavItems,
                listState = conversationListState,
                visible = showScrollControls,
                modifier = Modifier
                    .matchParentSize()
                    .padding(end = 12.dp)
            )
        }
    }

    if (showBatchMoveDialog) {
        val commonFolderId = selectedConversations
            .map { it.folderId }
            .distinct()
            .singleOrNull()

        MoveToFolderDialog(
            folders = folders,
            currentFolderId = commonFolderId,
            onDismiss = { showBatchMoveDialog = false },
            onCreateFolder = { name, icon, color ->
                viewModel.createFolder(name, icon, color)
            },
            onUpdateFolder = { folder, name, icon, color ->
                viewModel.updateFolder(folder, name, icon, color)
            },
            onMove = { folderId ->
                viewModel.moveConversationsToFolder(selectedConversationIds, folderId)
                selectedConversationIds = emptySet()
                showBatchMoveDialog = false
            }
        )
    }

    if (showBatchDeleteDialog) {
        BatchDeleteConfirmDialog(
            selectedCount = selectedConversationIds.size,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                viewModel.deleteConversations(selectedConversationIds)
                selectedConversationIds = emptySet()
                showBatchDeleteDialog = false
            }
        )
    }

    // 新建对话对话框
    if (showNewChatDialog) {
        NewChatDialog(
            selectedConfig = selectedConfig,
            configs = apiConfigs,
            folders = folders,
            currentFolderId = selectedFolderId?.takeUnless { it == -2L || it == -1L },
            onDismiss = { viewModel.hideNewChatDialog() },
            onCreate = { title, prompt, folderId, newFolderName, config, isPrivate ->
                if (newFolderName.isNullOrBlank()) {
                    viewModel.createNewConversation(title, prompt, folderId, config, isPrivate) { conversationId ->
                        onNavigateToChat(conversationId)
                    }
                } else {
                    viewModel.createConversationInNewFolder(title, prompt, newFolderName, config, isPrivate) { conversationId ->
                        onNavigateToChat(conversationId)
                    }
                }
            }
        )
    }
}

private fun buildHomeAnchorItems(
    conversations: List<Conversation>,
    pinnedExpanded: Boolean
): List<SideAnchorItem> {
    val pinned = conversations.filter { it.isPinned }
    val normal = conversations.filter { !it.isPinned }
    val anchors = mutableListOf<SideAnchorItem>()
    var index = 0

    if (pinned.isNotEmpty()) {
        index += 1 // pinned section header
        if (pinnedExpanded) {
            pinned.forEach { conversation ->
                anchors += SideAnchorItem(
                    title = anchorTitle(conversation.title),
                    itemIndex = index
                )
                index += 1
            }
        }
    }

    if (normal.isNotEmpty() && pinned.isNotEmpty()) {
        index += 1 // "other" section header
    }

    normal.forEach { conversation ->
        anchors += SideAnchorItem(
            title = anchorTitle(conversation.title),
            itemIndex = index
        )
        index += 1
    }

    return anchors
}

private fun anchorTitle(value: String): String {
    return value
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.take(26)
        ?: "未命名对话"
}

@Composable
fun HomeDashboardHeader(
    hazeState: dev.chrisbanes.haze.HazeState,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EchoWordmark(modifier = Modifier.weight(1f))
            StatsIconButton(
                hazeState = hazeState,
                onClick = onNavigateToStats,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassHomeIconButton(
                hazeState = hazeState,
                icon = Icons.Default.Settings,
                contentDescription = "设置",
                onClick = onNavigateToSettings,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun NewConversationFab(
    hazeState: dev.chrisbanes.haze.HazeState,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val buttonShape = RoundedCornerShape(22.dp)
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .height(56.dp)
            .echoHazePanel(
                hazeState = hazeState,
                shape = buttonShape,
                tint = primary.copy(alpha = 0.32f),
                blurRadius = 34.dp
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "配置新对话"
            ),
        shape = buttonShape,
        color = primary.copy(alpha = 0.22f),
        contentColor = primary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text(
                text = "创建对话",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun EchoWordmark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .height(56.dp)
            .widthIn(min = 132.dp)
    ) {
        val textSize = 39.sp.toPx()
        val baseline = size.height * 0.68f
        val startX = 1.dp.toPx()

        val underline = Path().apply {
            moveTo(startX + 8.dp.toPx(), size.height * 0.78f)
            cubicTo(
                size.width * 0.24f,
                size.height * 0.98f,
                size.width * 0.64f,
                size.height * 0.86f,
                size.width * 0.78f,
                size.height * 0.72f
            )
        }
        drawPath(
            path = underline,
            color = primary.copy(alpha = 0.38f),
            style = Stroke(width = 3.2.dp.toPx())
        )

        drawIntoCanvas { canvas ->
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primary.copy(alpha = 0.22f).toArgb()
                this.textSize = textSize
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                letterSpacing = 0.01f
            }
            canvas.nativeCanvas.drawText("Echo", startX + 2.2.dp.toPx(), baseline + 2.4.dp.toPx(), shadowPaint)

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.textSize = textSize
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                letterSpacing = 0.01f
                shader = LinearGradient(
                    0f,
                    0f,
                    size.width * 0.78f,
                    size.height,
                    intArrayOf(ink.toArgb(), primary.toArgb()),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.nativeCanvas.drawText("Echo", startX, baseline, fillPaint)

            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = primary.copy(alpha = 0.9f).toArgb()
                style = Paint.Style.STROKE
                strokeWidth = 1.8.dp.toPx()
                strokeCap = Paint.Cap.ROUND
            }
            canvas.nativeCanvas.drawLine(
                startX + 78.dp.toPx(),
                size.height * 0.22f,
                startX + 93.dp.toPx(),
                size.height * 0.2f,
                accentPaint
            )
        }
    }
}

@Composable
private fun StatsIconButton(
    hazeState: dev.chrisbanes.haze.HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    val glassBlue = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.echoHazePanel(
            hazeState = hazeState,
            shape = CircleShape,
            tint = buttonTint,
            blurRadius = 18.dp
        ),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = glassBlue,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .echoShapeClick(CircleShape, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(23.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(glassBlue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .width(15.dp)
                        .height(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(5.dp, 9.dp, 13.dp).forEach { barHeight ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .background(glassBlue, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassHomeIconButton(
    hazeState: dev.chrisbanes.haze.HazeState,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
    val glassBlue = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.echoHazePanel(
            hazeState = hazeState,
            shape = CircleShape,
            tint = buttonTint,
            blurRadius = 18.dp
        ),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = glassBlue,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .echoShapeClick(CircleShape, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = glassBlue
            )
        }
    }
}

@Composable
private fun HomeSearchRow(
    hazeState: dev.chrisbanes.haze.HazeState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .echoHazePanel(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(22.dp)
                ),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isBlank()) {
                                Text(
                                    text = "搜索对话、模型或提示词",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = { onSearchQueryChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清空搜索",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        GlassHomeIconButton(
            hazeState = hazeState,
            icon = Icons.Default.History,
            contentDescription = "历史记录",
            onClick = onNavigateToHistory,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun BatchSelectionBar(
    selectedCount: Int,
    moveActionText: String,
    onClearSelection: () -> Unit,
    onPinSelected: () -> Unit,
    onUnpinSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    onHideSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val barShape = RoundedCornerShape(24.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = barShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已选择 $selectedCount 个对话",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "退出多选",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onPinSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("置顶", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onUnpinSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("取消置顶", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onMoveSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(moveActionText, maxLines = 1)
                }
            }

            FilledTonalButton(
                onClick = onHideSelected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(
                    Icons.Default.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("隐藏对话")
            }

            FilledTonalButton(
                onClick = onDeleteSelected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = DeleteDialogPink,
                    contentColor = DeleteDialogContent
                )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("批量删除")
            }
        }
    }
}

@Composable
private fun BatchDeleteConfirmDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeleteDialogPink,
        icon = {
            Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                tint = DeleteDialogContent
            )
        },
        title = {
            Text(
                text = "确认删除 $selectedCount 个对话？",
                color = DeleteDialogContent
            )
        },
        text = {
            Text(
                text = "删除后这些对话和消息记录将无法恢复。请再次确认是否继续删除。",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeleteDialogContent,
                    contentColor = Color.White
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = DeleteDialogContent)
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun PinnedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val headerShape = RoundedCornerShape(18.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .echoShapeClick(headerShape, onClick = onToggle),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "置顶",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起置顶对话" else "展开置顶对话",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HomeMetric(
    modifier: Modifier = Modifier,
    value: String? = null,
    label: String,
    onClick: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null
) {
    val metricShape = RoundedCornerShape(20.dp)
    val clickableModifier = if (onClick != null) {
        modifier.echoShapeClick(metricShape, onClick = onClick)
    } else {
        modifier
    }

    Surface(
        modifier = clickableModifier.height(64.dp),
        shape = metricShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .height(26.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    icon()
                } else {
                    Text(
                        text = value.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MiniStatsIcon() {
    Row(
        modifier = Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(10.dp, 17.dp, 13.dp, 22.dp).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index == 3) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f)
                    )
            )
        }
    }
}

@Composable
fun FolderSelector(
    hazeState: dev.chrisbanes.haze.HazeState,
    folders: List<Folder>,
    selectedFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onManageFolders: () -> Unit
) {
    val folderColors = listOf(
        Color(0xFFE57373),
        Color(0xFFFFB74D),
        Color(0xFFFFF176),
        Color(0xFF60A5FA),
        Color(0xFF64B5F6),
        Color(0xFF9575CD),
        Color(0xFF818CF8),
        Color(0xFFA1887F),
    )

    val folderIcons = mapOf(
        "folder" to Icons.Default.Folder,
        "star" to Icons.Default.Star,
        "work" to Icons.Default.Work,
        "school" to Icons.Default.School,
        "code" to Icons.Default.Code,
        "chat" to Icons.Default.Chat,
        "favorite" to Icons.Default.Favorite,
        "bookmark" to Icons.Default.Bookmark
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            HomeGlassChip(
                hazeState = hazeState,
                selected = selectedFolderId == null,
                onClick = { onFolderSelected(null) },
                label = "对话",
                leadingIcon = {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        item {
            HomeGlassChip(
                hazeState = hazeState,
                selected = selectedFolderId == -2L,
                onClick = { onFolderSelected(-2L) },
                label = "置顶",
                leadingIcon = {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        items(folders) { folder ->
            val color = folderColors.getOrElse(folder.color - 1) { MaterialTheme.colorScheme.primary }
            val icon = folderIcons[folder.icon] ?: Icons.Default.Folder

            HomeGlassChip(
                hazeState = hazeState,
                selected = selectedFolderId == folder.id,
                onClick = { onFolderSelected(folder.id) },
                label = folder.name,
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (selectedFolderId == folder.id) color else color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (selectedFolderId == folder.id) Color.White else color
                        )
                    }
                }
            )
        }

        item {
            HomeGlassChip(
                hazeState = hazeState,
                selected = false,
                onClick = onManageFolders,
                label = "添加",
                leadingIcon = {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun HomeGlassChip(
    hazeState: dev.chrisbanes.haze.HazeState,
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val chipShape = RoundedCornerShape(999.dp)
    val tint = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    }
    val chipContentColor = readableTextColorFor(
        background = tint,
        darkColor = MaterialTheme.colorScheme.primary,
        lightColor = Color(0xFFEAF4FF),
        fallbackSurface = MaterialTheme.colorScheme.surface
    )
    Surface(
        modifier = Modifier
            .height(38.dp)
            .echoHazePanel(
                hazeState = hazeState,
                shape = chipShape,
                tint = tint,
                blurRadius = 18.dp
            )
            .echoShapeClick(chipShape, onClick = onClick),
        shape = chipShape,
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        contentColor = chipContentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ConfigSelector(
    configs: List<ApiConfig>,
    selectedConfig: ApiConfig?,
    onConfigSelected: (ApiConfig) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = selectedConfig?.name ?: "选择模型",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            configs.forEach { config ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(config.name)
                            Row {
                                Text(
                                    text = config.modelName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onConfigSelected(config)
                        expanded = false
                    },
                    trailingIcon = {
                        if (config.isDefault) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "默认",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    leadingIcon = {
                        if (config.id == selectedConfig?.id) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }

            if (configs.isNotEmpty()) {
                Divider()
            }

            DropdownMenuItem(
                text = { Text("管理API配置") },
                onClick = {
                    expanded = false
                    onNavigateToSettings()
                },
                leadingIcon = {
                    Icon(Icons.Default.Settings, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun HomeModelAvatar(
    apiConfigId: Long?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val modelAvatarBitmap = remember(context, apiConfigId) {
        AvatarManager.getPreferredModelAvatarBitmap(context, apiConfigId)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
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
                contentDescription = "模型头像",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationCard(
    conversation: Conversation,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onMoveToFolder: (Long?) -> Unit,
    onRename: (String) -> Unit,
    onHide: () -> Unit,
    onCreateFolder: (String, String, Int) -> Unit,
    onUpdateFolder: (Folder, String, String, Int) -> Unit,
    folders: List<Folder>
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showMoveToFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val cardShape = RoundedCornerShape(30.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "多选对话"
            ),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                HomeSelectedChipColor
            } else {
                Color(0xFFFBFEFD)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Icon(
                    if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (selected) "已选择" else "未选择",
                    modifier = Modifier.size(24.dp),
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            HomeModelAvatar(
                apiConfigId = conversation.apiConfigId,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 对话信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = conversation.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (conversation.isPinned) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "置顶",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 模型标签
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                conversation.modelName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.height(24.dp)
                    )
                    Text(
                        text = "${conversation.messageCount}条",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateFormat.format(Date(conversation.updatedAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!selectionMode) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.clip(RoundedCornerShape(18.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("重命名") },
                            onClick = {
                                showRenameDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (conversation.isPinned) "取消置顶" else "置顶") },
                            onClick = {
                                onPin()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("移动到文件夹") },
                            onClick = {
                                showMoveToFolderDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DriveFileMove, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("隐藏") },
                            onClick = {
                                onHide()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                showDeleteDialog = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // 删除确认对话框
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除对话") },
            text = { Text("确定要删除这个对话吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 移动到文件夹对话框
    if (showMoveToFolderDialog) {
        MoveToFolderDialog(
            folders = folders,
            currentFolderId = conversation.folderId,
            onDismiss = { showMoveToFolderDialog = false },
            onCreateFolder = onCreateFolder,
            onUpdateFolder = onUpdateFolder,
            onMove = { folderId ->
                onMoveToFolder(folderId)
                showMoveToFolderDialog = false
            }
        )
    }

    // 重命名对话框
    if (showRenameDialog) {
        RenameConversationDialog(
            currentTitle = conversation.title,
            onDismiss = { showRenameDialog = false },
            onRename = { newTitle ->
                onRename(newTitle)
                showRenameDialog = false
            }
        )
    }
}

@Composable
fun MoveToFolderDialog(
    folders: List<Folder>,
    currentFolderId: Long?,
    onDismiss: () -> Unit,
    onCreateFolder: (String, String, Int) -> Unit,
    onUpdateFolder: (Folder, String, String, Int) -> Unit,
    onMove: (Long?) -> Unit
) {
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var editingFolder by remember { mutableStateOf<Folder?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移动到文件夹") },
        text = {
            Column {
                OutlinedButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("新建文件夹")
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    item {
                        val unfiledShape = RoundedCornerShape(16.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .echoShapeClick(unfiledShape) { onMove(null) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("未分类", modifier = Modifier.weight(1f))
                            if (currentFolderId == null) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    items(folders) { folder ->
                        val folderRowShape = RoundedCornerShape(16.dp)
                        val folderColors = listOf(
                            Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFFFFF176),
                            Color(0xFF60A5FA), Color(0xFF64B5F6), Color(0xFF9575CD),
                            Color(0xFF818CF8), Color(0xFFA1887F)
                        )
                        val color = folderColors.getOrElse(folder.color - 1) {
                            MaterialTheme.colorScheme.primary
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .echoShapeClick(folderRowShape) { onMove(folder.id) }
                                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentFolderId == folder.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { editingFolder = folder },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "重命名文件夹",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (folders.isEmpty()) {
                        item {
                            Text(
                                text = "还没有文件夹，可以先新建一个。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
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

    if (showCreateFolderDialog) {
        FolderEditDialog(
            folder = null,
            onDismiss = { showCreateFolderDialog = false },
            onSave = { name, icon, color ->
                onCreateFolder(name, icon, color)
                showCreateFolderDialog = false
            }
        )
    }

    editingFolder?.let { folder ->
        FolderEditDialog(
            folder = folder,
            onDismiss = { editingFolder = null },
            onSave = { name, icon, color ->
                onUpdateFolder(folder, name, icon, color)
                editingFolder = null
            }
        )
    }
}

@Composable
fun RenameConversationDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名对话") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("对话标题") },
                singleLine = true
            )
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
fun EmptyHomeContent(
    modifier: Modifier = Modifier,
    onStartChat: () -> Unit,
    isSearching: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标 - 使用更现代的设计
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isSearching) "没有找到匹配的对话" else "开始新的对话",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isSearching) "换个关键词试试，或直接开始一段新对话" else "与AI助手交流，获取帮助和灵感",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isSearching) {
            Button(
                onClick = onStartChat,
                modifier = Modifier.height(48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("新建对话")
            }
        }
    }
}

@Composable
fun NewChatDialog(
    selectedConfig: ApiConfig?,
    configs: List<ApiConfig>,
    folders: List<Folder>,
    currentFolderId: Long?,
    onDismiss: () -> Unit,
    onCreate: (String, String?, Long?, String?, ApiConfig?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var systemPrompt by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf(currentFolderId) }
    var createNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedApiConfig by remember(selectedConfig, configs) {
        mutableStateOf(selectedConfig ?: configs.firstOrNull())
    }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "新建对话",
                    modifier = Modifier.weight(1f)
                )
                Box {
                    AssistChip(
                        onClick = { modelMenuExpanded = true },
                        enabled = configs.isNotEmpty(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Column {
                                Text(
                                    text = selectedApiConfig?.name ?: "选择模型",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedApiConfig?.modelName.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = modelMenuExpanded,
                        onDismissRequest = { modelMenuExpanded = false },
                        modifier = Modifier.clip(RoundedCornerShape(18.dp))
                    ) {
                        configs.forEach { config ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(config.name)
                                        Text(
                                            text = config.modelName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedApiConfig = config
                                    modelMenuExpanded = false
                                },
                                leadingIcon = {
                                    if (config.id == selectedApiConfig?.id) {
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
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "对话标题",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例如：代码助手") },
                    singleLine = true
                )

                Text(
                    text = "选择文件夹",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = !createNewFolder && selectedFolderId == null,
                            onClick = {
                                createNewFolder = false
                                selectedFolderId = null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HomeSelectedChipColor
                            ),
                            label = { Text("无") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = createNewFolder,
                            onClick = {
                                createNewFolder = true
                                selectedFolderId = null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HomeSelectedChipColor
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CreateNewFolder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = { Text("新建文件夹") }
                        )
                    }
                    items(folders) { folder ->
                        FilterChip(
                            selected = !createNewFolder && selectedFolderId == folder.id,
                            onClick = {
                                createNewFolder = false
                                selectedFolderId = folder.id
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HomeSelectedChipColor
                            ),
                            label = { Text(folder.name) }
                        )
                    }
                }

                AnimatedVisibility(visible = createNewFolder) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("文件夹名称") },
                        singleLine = true
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("隐私对话", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "退出后立即销毁本次消息记录",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it }
                        )
                    }
                }

                Text(
                    text = "系统提示词（可选）",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("定义AI助手的行为和角色...") },
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        title.trim(),
                        systemPrompt.ifBlank { null },
                        if (createNewFolder) null else selectedFolderId,
                        newFolderName.trim().takeIf { createNewFolder && it.isNotBlank() },
                        selectedApiConfig,
                        isPrivate
                    )
                },
                enabled = selectedApiConfig != null && (!createNewFolder || newFolderName.isNotBlank())
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
