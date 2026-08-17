@file:OptIn(ExperimentalMaterial3Api::class)

package com.aiassistant.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiassistant.AiAssistantApp
import com.aiassistant.BuildConfig
import com.aiassistant.R
import com.aiassistant.domain.model.ApiConfig
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.EnvironmentVariable
import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.EchoGlassDialog
import com.aiassistant.ui.components.EchoGlassDropdownMenu
import com.aiassistant.ui.components.echoFilterChipBorder
import com.aiassistant.ui.components.echoFilterChipColors
import com.aiassistant.ui.components.echoFilterChipElevation
import com.aiassistant.ui.components.echoGlassPalette
import com.aiassistant.ui.components.echoSegmentedButtonBorder
import com.aiassistant.ui.components.echoSegmentedButtonColors
import com.aiassistant.ui.components.echoHazePanel
import com.aiassistant.ui.components.echoHazeSource
import com.aiassistant.ui.components.echoShapeClick
import com.aiassistant.ui.components.rememberEchoHazeState
import com.aiassistant.utils.AvatarManager
import com.aiassistant.utils.BackgroundImageManager
import com.aiassistant.utils.BackupManager
import com.aiassistant.utils.HiddenConversationLock
import com.aiassistant.utils.TavilySearchSettings
import com.aiassistant.utils.AppThemeMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val CurrentFeatureHighlights = listOf(
    "沉浸式角色扮演工作室与剧情自由创作",
    "对话与故事长记忆严格隔离与全功能管理",
    "合并一体化个性化与全局设定管理面板",
    "多角色设定与世界观一站式管理及双向同步",
    "AI 智能识别、提炼追加与已有设定精准融入",
    "剧情动作指令库（推进、改写、分支、摘要等）",
    "多模型 API 与流式对话及深度思考推理",
    "Token 预算上下文、滚动摘要与长记忆库",
    "上下文使用情况查看与主动压缩",
    "全量数据备份与安全加密恢复",
    "全界面 Echo 液态玻璃设计与暗色主题适配"
)

private val CurrentVersionUserUpdates = listOf(
    "长记忆深度隔离：对话页与故事页长记忆彻底分立，防止小说创作被全局记忆干扰或污染新小说",
    "个性化与全局提示词合并：整合为一站式「个性化与全局设定」页面，操作直观统一",
    "模型长记忆管理中心：支持在个性化中查看、搜索、添加、编辑、删除与清空模型长记忆",
    "长记忆自动捕获开关：可一键启闭对话自动记忆，精准控制模型对偏好与背景的提取行为",
    "输入框操作栏重构：交换智能搜索与深度思考位置，故事页专注剧情并移除搜索按钮",
    "故事页输入框新增「剧情操作」按钮，集成继续、重生成、分支、改写与智能提取等完整指令",
    "故事页创作设置与工作室深度对齐：支持直接查看并编辑全字段角色卡与世界观场景卡",
    "故事专属设定管理：支持在故事创作设置中直接添加新角色/新世界观，或仅从本故事中移除",
    "角色与故事库双向同步：角色工坊长按故事支持按故事更新库或按主库更新故事",
    "消息气泡美化：深度思考输出胶囊与模型头像精准保持同一水平线并垂直居中对齐",
    "弹窗界面全面优化：修复对话页与故事页设置弹窗底部保存按钮被挤出可视区域的问题",
    "暗色液态玻璃修复：彻底消除添加 API 配置及设置子页面中偶现的白色背景异常",
    "AI 智能设定提取重构：精准区分已有角色设定更新、新角色发现与世界观扩充，避免粗暴重复创建"
)

private val SettingsPanelShape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeXl
private val SettingsInnerShape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeLg

@Composable
private fun SettingsGlassCard(
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = echoGlassPalette()
    val glassModifier = if (hazeState != null) {
        modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = SettingsPanelShape,
                tint = glass.panel,
                blurRadius = 18.dp,
                highlightAlpha = 0.025f
            )
    } else {
        modifier.fillMaxWidth()
    }
    Surface(
        modifier = glassModifier,
        shape = SettingsPanelShape,
        color = glass.panel,
        contentColor = glass.textPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, glass.outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = if (placeholder.isNotBlank()) {
                { Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) }
            } else null,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            shape = SettingsInnerShape,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
            )
        )
    }
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Long) -> Unit,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsBackgroundBitmap = remember(context) {
        BackgroundImageManager.getHomeBackgroundBitmap(context)
    }
    val hazeState = rememberEchoHazeState()
    val glass = echoGlassPalette()
    var selectedSection by remember { mutableStateOf<String?>(null) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var pendingBackAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var saveTrigger by remember { mutableStateOf(0) }

    fun executeBack() {
        if (selectedSection != null) {
            selectedSection = null
            hasUnsavedChanges = false
        } else {
            onNavigateBack()
        }
    }

    fun handleBack() {
        if (hasUnsavedChanges) {
            pendingBackAction = { executeBack() }
            showUnsavedDialog = true
        } else {
            executeBack()
        }
    }

    BackHandler(enabled = selectedSection != null || hasUnsavedChanges) {
        handleBack()
    }

    if (showUnsavedDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showUnsavedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "未保存的设置更改",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            content = {
                Text(
                    text = "检测到您修改了设置内容但尚未保存，是否保存后再退出？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        hasUnsavedChanges = false
                        pendingBackAction?.invoke()
                        pendingBackAction = null
                    }) {
                        Text("直接放弃", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = {
                        saveTrigger++
                        showUnsavedDialog = false
                        hasUnsavedChanges = false
                        pendingBackAction?.invoke()
                        pendingBackAction = null
                    }) {
                        Text("保存并返回")
                    }
                }
            }
        )
    }

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
            settingsBackgroundBitmap?.let { bitmap ->
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
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(glass.panelStrong)
                        .border(
                            BorderStroke(0.8.dp, glass.outline.copy(alpha = 0.4f)),
                            RectangleShape
                        )
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (selectedSection) {
                                    null -> "设置"
                                    "api_config" -> "API配置"
                                    "personalization" -> "个性化与全局设定"
                                    "web_search" -> "联网搜索"
                                    "hidden_conversations" -> "其他对话"
                                    "backup" -> "数据备份"
                                    "about" -> "关于"
                                    else -> "设置"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { handleBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                }
            }
        ) { paddingValues ->
            when (selectedSection) {
                null -> SettingsMenu(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues),
                    onSectionSelected = { selectedSection = it }
                )
                "api_config" -> ApiConfigTab(hazeState = hazeState, modifier = Modifier.padding(paddingValues))
                "personalization" -> PersonalizationTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues),
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onUnsavedStateChanged = { hasUnsavedChanges = it },
                    saveTrigger = saveTrigger
                )
                "web_search" -> WebSearchTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues),
                    onUnsavedStateChanged = { hasUnsavedChanges = it },
                    saveTrigger = saveTrigger
                )
                "hidden_conversations" -> HiddenConversationsTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues),
                    onNavigateToChat = onNavigateToChat
                )
                "backup" -> BackupTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues)
                )
                "about" -> AboutTab(hazeState = hazeState, modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
fun SettingsMenu(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    onSectionSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Key,
                title = "API配置",
                subtitle = "管理AI模型API密钥和配置",
                onClick = { onSectionSelected("api_config") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.AutoAwesome,
                title = "个性化与全局设定",
                subtitle = "应用主题、字体大小、思考胶囊自定义、全局提示词与长记忆",
                onClick = { onSectionSelected("personalization") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Search,
                title = "联网搜索",
                subtitle = "配置 Tavily，让对话中的智能搜索真正联网",
                onClick = { onSectionSelected("web_search") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.VisibilityOff,
                title = "其他对话",
                subtitle = "输入 6 位数字密码查看隐藏对话",
                onClick = { onSectionSelected("hidden_conversations") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Backup,
                title = "数据备份",
                subtitle = "备份和恢复应用数据",
                onClick = { onSectionSelected("backup") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "版本信息和功能介绍",
                onClick = { onSectionSelected("about") }
            )
        }
    }
}

@Composable
private fun ThemeModeCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = SettingsPanelShape,
                tint = echoGlassPalette().panel,
                blurRadius = 18.dp
            ),
        shape = SettingsPanelShape,
        color = echoGlassPalette().panel,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("应用主题", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "选择浅色、深色或跟随系统",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AppThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AppThemeMode.entries.size
                        ),
                        colors = echoSegmentedButtonColors(),
                        border = echoSegmentedButtonBorder(selected == mode)
                    ) {
                        Text(mode.label)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    hazeState: dev.chrisbanes.haze.HazeState,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val itemShape = SettingsPanelShape
    val glass = echoGlassPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 78.dp)
            .echoHazePanel(
                hazeState = hazeState,
                shape = itemShape,
                tint = glass.panel,
                blurRadius = 18.dp
            )
            .background(glass.panel, itemShape)
            .border(BorderStroke(1.dp, glass.outline), itemShape)
            .echoShapeClick(itemShape, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ApiConfigTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val repository = AiAssistantApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val configs by repository.getAllApiConfigs().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    BackHandler(enabled = showAddDialog || editingConfig != null) {
        showAddDialog = false
        editingConfig = null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "API配置管理",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(configs) { config ->
            ApiConfigCard(
                hazeState = hazeState,
                config = config,
                onEdit = { editingConfig = it },
                onDelete = {
                    scope.launch {
                        repository.deleteApiConfig(config)
                    }
                },
                onSetDefault = {
                    scope.launch {
                        repository.setDefaultConfig(config.id)
                    }
                }
            )
        }

        item {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加API配置")
            }
        }
    }

    if (showAddDialog || editingConfig != null) {
        ApiConfigDialog(
            hazeState = hazeState,
            config = editingConfig,
            isSaving = isSaving,
            onDismiss = {
                showAddDialog = false
                editingConfig = null
            },
            onSave = { config, modelNames, enabledModelNames, modelCapabilities, apiAvatarUri, clearApiAvatar ->
                if (!isSaving) {
                    isSaving = true
                    scope.launch {
                        try {
                            val configId = repository.saveApiConfig(config)
                            if (modelNames.isNotEmpty()) {
                                repository.replaceSelectedModels(
                                    apiConfigId = configId,
                                    modelNames = modelNames,
                                    enabledModelNames = enabledModelNames,
                                    modelCapabilities = modelCapabilities
                                )
                            }
                            if (clearApiAvatar) {
                                AvatarManager.deleteApiModelAvatar(context, configId)
                            }
                            apiAvatarUri?.let { uri ->
                                AvatarManager.saveApiModelAvatarFromUri(context, configId, uri)
                            }
                            showAddDialog = false
                            editingConfig = null
                        } finally {
                            isSaving = false
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ApiConfigCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    config: ApiConfig,
    onEdit: (ApiConfig) -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    SettingsGlassCard(hazeState = hazeState) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = config.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                    if (config.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            modifier = Modifier.height(24.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("新对话默认API", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    }
                    Text(
                        text = "${config.provider} · ${config.modelName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "API类型: ${config.apiType.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    IconButton(onClick = { onEdit(config) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    if (!config.isDefault) {
                        IconButton(onClick = onSetDefault) {
                            Icon(Icons.Default.StarBorder, contentDescription = "设为新对话默认API")
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
    }

    if (showDeleteDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除配置") },
            text = { Text("确定要删除这个API配置吗？") },
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
}

@Composable
fun WebSearchTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    onUnsavedStateChanged: (Boolean) -> Unit = {},
    saveTrigger: Int = 0
) {
    val manager = AiAssistantApp.instance.tavilySearchManager
    var settings by remember { mutableStateOf(manager.getSettings()) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var searchDepth by remember(settings) { mutableStateOf(settings.searchDepth) }
    var maxResults by remember(settings) { mutableStateOf(settings.maxResults.toString()) }
    var includeAnswer by remember(settings) { mutableStateOf(settings.includeAnswer) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val hasUnsaved = remember(settings, apiKey, enabled, searchDepth, maxResults, includeAnswer) {
        apiKey != settings.apiKey ||
        enabled != settings.enabled ||
        searchDepth != settings.searchDepth ||
        maxResults != settings.maxResults.toString() ||
        includeAnswer != settings.includeAnswer
    }

    LaunchedEffect(hasUnsaved) {
        onUnsavedStateChanged(hasUnsaved)
    }

    fun performSave() {
        val newSettings = TavilySearchSettings(
            enabled = enabled,
            apiKey = apiKey,
            searchDepth = searchDepth,
            maxResults = maxResults.toIntOrNull()?.coerceIn(1, 20) ?: 8,
            includeAnswer = includeAnswer
        )
        val ok = manager.saveSettings(newSettings)
        settings = manager.getSettings()
        savedMessage = if (ok) "联网搜索配置已保存" else "保存失败，请重试"
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            performSave()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tavily 联网搜索", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "开启后，对话里的智能搜索会先调用 Tavily，再把结果交给当前模型。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                SettingsInputField(
                    title = "Tavily API Key",
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = "tvly-..."
                )

                Text(
                    "Key 只保存在本机应用私有数据中，并使用 Android Keystore 加密；不会写入源码。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsGlassCard(hazeState = hazeState) {
                Text("搜索参数", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("basic", "advanced").forEach { depth ->
                        val selected = searchDepth == depth
                        FilterChip(
                            selected = selected,
                            onClick = { searchDepth = depth },
                            colors = echoFilterChipColors(),
                            border = echoFilterChipBorder(selected),
                            elevation = echoFilterChipElevation(),
                            label = { Text(if (depth == "basic") "基础" else "深入") }
                        )
                    }
                }

                SettingsInputField(
                    title = "最大结果数 (1-20)",
                    value = maxResults,
                    onValueChange = { value ->
                        maxResults = value.filter { it.isDigit() }.take(2)
                    },
                    placeholder = "10",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("包含 Tavily 自动摘要", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = includeAnswer,
                        onCheckedChange = { includeAnswer = it }
                    )
                }
            }
        }

        item {
            Button(
                onClick = { performSave() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存联网搜索配置")
            }
        }

        savedMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PersonalizationTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onUnsavedStateChanged: (Boolean) -> Unit = {},
    saveTrigger: Int = 0
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = AiAssistantApp.instance.personalizationManager
    val repository = AiAssistantApp.instance.repository
    val coroutineScope = rememberCoroutineScope()

    var avatarBase64 by remember { mutableStateOf(AvatarManager.getAvatar(context)) }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            if (AvatarManager.saveAvatarFromUri(context, it)) {
                avatarBase64 = AvatarManager.getAvatar(context)
            }
        }
    }

    var settings by remember { mutableStateOf(manager.getSettings()) }
    var globalPrompt by remember(settings) { mutableStateOf(settings.globalSystemPrompt) }
    var instruction by remember(settings) {
        mutableStateOf(
            listOf(
                settings.aboutUser,
                settings.responseStyle,
                settings.preferences,
                settings.avoid
            )
                .filter { it.isNotBlank() }
                .joinToString("\n\n")
        )
    }
    var autoMemoryEnabled by remember(settings) { mutableStateOf(settings.autoMemoryEnabled) }
    var thinkingTemplate by remember(settings) { mutableStateOf(settings.thinkingCapsuleTemplate) }
    var chatFontSize by remember(settings) { mutableIntStateOf(settings.chatFontSize) }
    var fontSizeScale by remember(settings) { mutableFloatStateOf(settings.fontSizeScale) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val hasUnsaved = remember(settings, globalPrompt, instruction, autoMemoryEnabled, thinkingTemplate, chatFontSize, fontSizeScale) {
        globalPrompt.trim() != settings.globalSystemPrompt.trim() ||
        instruction.trim() != settings.aboutUser.trim() ||
        autoMemoryEnabled != settings.autoMemoryEnabled ||
        thinkingTemplate.trim() != settings.thinkingCapsuleTemplate.trim() ||
        chatFontSize != settings.chatFontSize ||
        fontSizeScale != settings.fontSizeScale
    }

    LaunchedEffect(hasUnsaved) {
        onUnsavedStateChanged(hasUnsaved)
    }

    fun performSave() {
        val saved = manager.saveSettings(
            settings.copy(
                globalSystemPrompt = globalPrompt.trim(),
                aboutUser = instruction.trim(),
                responseStyle = "",
                preferences = "",
                avoid = "",
                autoMemoryEnabled = autoMemoryEnabled,
                thinkingCapsuleTemplate = thinkingTemplate.trim().ifBlank { "{model} {status} {time} {tokens}" },
                chatFontSize = chatFontSize,
                fontSizeScale = fontSizeScale
            )
        )
        settings = manager.getSettings()
        savedMessage = if (saved) "已保存个性化与全局设定" else "保存失败，请重试"
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            performSave()
        }
    }

    // 背景图片管理
    var backgroundRevision by remember { mutableIntStateOf(0) }
    val hasHomeBackground = remember(backgroundRevision) {
        BackgroundImageManager.hasHomeBackground(context)
    }
    val hasChatBackground = remember(backgroundRevision) {
        BackgroundImageManager.hasChatBackground(context)
    }
    val homeBackgroundPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val saved = BackgroundImageManager.saveHomeBackgroundFromUri(context, it)
            backgroundRevision++
            savedMessage = if (saved) "已设置首页背景" else "背景保存失败，请重试"
        }
    }
    val chatBackgroundPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val saved = BackgroundImageManager.saveChatBackgroundFromUri(context, it)
            backgroundRevision++
            savedMessage = if (saved) "已设置对话页背景" else "背景保存失败，请重试"
        }
    }

    // 记忆管理状态
    var memorySearchQuery by remember { mutableStateOf("") }
    val allMemories by remember(memorySearchQuery) {
        if (memorySearchQuery.isBlank()) repository.getAllMemories() else repository.searchMemories(memorySearchQuery.trim())
    }.collectAsState(initial = emptyList())

    var memoryToEdit by remember { mutableStateOf<MemoryItem?>(null) }
    var isAddingMemory by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val glass = echoGlassPalette()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. 用户头像设置
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("用户头像", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "自定义用户气泡头像，支持选择相册图片",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.5.dp, glass.outlineSelected), CircleShape)
                            .echoShapeClick(CircleShape) {
                                imagePickerLauncher.launch("image/*")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBase64 != null) {
                            val bitmap = remember(avatarBase64) {
                                try {
                                    val byteArray = android.util.Base64.decode(avatarBase64, android.util.Base64.NO_WRAP)
                                    android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                                } catch (e: Exception) { null }
                            }
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "用户头像",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("上传新头像")
                        }

                        if (avatarBase64 != null) {
                            OutlinedButton(
                                onClick = {
                                    AvatarManager.deleteAvatar(context)
                                    avatarBase64 = null
                                    savedMessage = "已恢复默认头像"
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("恢复默认头像")
                            }
                        }
                    }
                }
            }
        }

        // 1. 应用主题模式
        item {
            ThemeModeCard(
                hazeState = hazeState,
                selected = themeMode,
                onSelected = onThemeModeChange
            )
        }

        // 2. 界面与对话字体大小设置
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FormatSize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("字体大小调节", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "可分别调节对话正文字号与界面缩放比例，适应不同阅读习惯。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("对话正文字号", style = MaterialTheme.typography.bodyMedium)
                        Text("${chatFontSize} sp", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = chatFontSize.toFloat(),
                        onValueChange = {
                            chatFontSize = it.toInt()
                            savedMessage = null
                        },
                        valueRange = 13f..22f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("界面字体缩放", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val scales = listOf(0.85f to "紧凑", 1.0f to "标准", 1.15f to "大", 1.25f to "特大")
                        scales.forEachIndexed { index, (scale, label) ->
                            val selected = kotlin.math.abs(fontSizeScale - scale) < 0.05f
                            SegmentedButton(
                                selected = selected,
                                onClick = {
                                    fontSizeScale = scale
                                    savedMessage = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = scales.size),
                                colors = echoSegmentedButtonColors(),
                                border = echoSegmentedButtonBorder(selected)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                // 字体实时预览卡片
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsInnerShape,
                    color = glass.control,
                    border = BorderStroke(1.dp, glass.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "预览效果：",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "你好！我是 Echo 智能助手，这是一段用于预览对话与排版字体大小的示例文本。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = chatFontSize.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. 思考胶囊文案自定义
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("思考胶囊文案自定义", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "自定义模型输出时思考胶囊展示的文案，支持自由组合模型名称、耗时与 Token 消耗。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SettingsInputField(
                    title = "胶囊文案模板",
                    value = thinkingTemplate,
                    onValueChange = {
                        thinkingTemplate = it
                        savedMessage = null
                    },
                    placeholder = "{model} {status} {time} {tokens}"
                )

                // 常用预设快捷填入
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("快捷预设模板：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "默认" to "{model} {status} {time} {tokens}",
                            "叙述" to "{model} 思考了 {time} 消耗了 {tokens}",
                            "极简" to "{model} · {time} · {tokens}"
                        ).forEach { (name, tpl) ->
                            FilterChip(
                                selected = thinkingTemplate == tpl,
                                onClick = {
                                    thinkingTemplate = tpl
                                    savedMessage = null
                                },
                                label = { Text(name) },
                                colors = echoFilterChipColors(),
                                border = echoFilterChipBorder(thinkingTemplate == tpl)
                            )
                        }
                    }
                }

                // 变量标签
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("可点击插入占位变量：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "{model}" to "模型名",
                            "{time}" to "耗时",
                            "{tokens}" to "Token量",
                            "{status}" to "状态"
                        ).forEach { (varKey, _) ->
                            AssistChip(
                                onClick = {
                                    if (!thinkingTemplate.contains(varKey)) {
                                        thinkingTemplate = if (thinkingTemplate.isBlank()) varKey else "$thinkingTemplate $varKey"
                                        savedMessage = null
                                    }
                                },
                                label = { Text(varKey, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                // 胶囊实时渲染效果预览
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsInnerShape,
                    color = glass.controlSelected,
                    border = BorderStroke(1.dp, glass.outlineSelected)
                ) {
                    val previewText = remember(thinkingTemplate) {
                        var p = thinkingTemplate
                            .replace("{model}", "gpt-4o")
                            .replace("{status}", "思考过程")
                            .replace("{time}", "2.5s")
                            .replace("{tokens}", "150 token")
                            .replace("{token}", "150 token")
                        p = p.replace(Regex("\\s+"), " ").trim()
                        if (p.isBlank()) "gpt-4o 思考过程" else p
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 4. 全局系统提示词模块
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("全局系统提示词", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "设置的系统提示词将作为默认提示词应用于普通新对话（角色扮演/故事创作模式使用专属设定）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = globalPrompt,
                    onValueChange = {
                        globalPrompt = it
                        savedMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = { Text("输入默认全局系统提示词...") },
                    minLines = 4,
                    maxLines = 14,
                    shape = SettingsInnerShape
                )
            }
        }

        // 5. 自定义偏好与人设
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("个性化偏好", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "这些偏好会自动注入普通对话上下文；单个对话的提示词仍可覆盖它们。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { settings = settings.copy(enabled = it) }
                    )
                }

                OutlinedTextField(
                    value = instruction,
                    onValueChange = {
                        instruction = it
                        savedMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    placeholder = {
                        Text("例如：默认用中文回答；代码多写注释；少用表格；回答自然一点；复杂问题先给结论。")
                    },
                    minLines = 5,
                    maxLines = 14,
                    shape = SettingsInnerShape
                )
            }
        }

        // 6. 模型长期记忆库模块
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("模型长期记忆库", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "模型在对话中记住的偏好与事实。长记忆与故事页严格隔离，不会干扰新小说创作。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoMemoryEnabled,
                        onCheckedChange = {
                            autoMemoryEnabled = it
                            savedMessage = null
                        }
                    )
                }

                Text(
                    text = if (autoMemoryEnabled) "已开启自动记忆：模型将在对话中智能提炼并记录你的偏好与习惯" else "自动记忆已暂停：模型不再从新对话中自动记录记忆",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (autoMemoryEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 搜索栏与操作栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = memorySearchQuery,
                        onValueChange = { memorySearchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("搜索长期记忆...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (memorySearchQuery.isNotBlank()) {
                                IconButton(onClick = { memorySearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "清除搜索", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = SettingsInnerShape
                    )

                    FilledTonalButton(
                        onClick = { isAddingMemory = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加", style = MaterialTheme.typography.labelMedium)
                    }

                    if (allMemories.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirm = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空全部记忆", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // 记忆列表
                if (allMemories.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SettingsInnerShape,
                        color = glass.control,
                        border = androidx.compose.foundation.BorderStroke(1.dp, glass.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                if (memorySearchQuery.isBlank()) "暂无长期记忆条目\n开启自动记忆后模型将自动记录，也可以点击上方「添加」手动写入。" else "未找到与「$memorySearchQuery」匹配的记忆",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allMemories.forEach { memory ->
                            MemoryItemCard(
                                memory = memory,
                                onToggleEnabled = { enabled ->
                                    coroutineScope.launch {
                                        repository.setMemoryEnabled(memory.id, enabled)
                                    }
                                },
                                onEdit = { memoryToEdit = memory },
                                onDelete = {
                                    coroutineScope.launch {
                                        repository.deleteMemory(memory.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 7. 界面背景设置
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("界面背景", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "可分别为首页和对话页设置自定义图片背景，未设置时保持原有纯色背景。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                BackgroundPickerRow(
                    title = "首页背景",
                    hasImage = hasHomeBackground,
                    onPick = { homeBackgroundPicker.launch("image/*") },
                    onClear = {
                        BackgroundImageManager.deleteHomeBackground(context)
                        backgroundRevision++
                        savedMessage = "已恢复首页默认背景"
                    }
                )
                BackgroundPickerRow(
                    title = "对话页背景",
                    hasImage = hasChatBackground,
                    onPick = { chatBackgroundPicker.launch("image/*") },
                    onClear = {
                        BackgroundImageManager.deleteChatBackground(context)
                        backgroundRevision++
                        savedMessage = "已恢复对话页默认背景"
                    }
                )
            }
        }

        // 8. 保存反馈消息
        savedMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 9. 保存按钮
        item {
            Button(
                onClick = {
                    val saved = manager.saveSettings(
                        settings.copy(
                            globalSystemPrompt = globalPrompt.trim(),
                            aboutUser = instruction.trim(),
                            responseStyle = "",
                            preferences = "",
                            avoid = "",
                            autoMemoryEnabled = autoMemoryEnabled,
                            thinkingCapsuleTemplate = thinkingTemplate.trim().ifBlank { "{model} {status} {time} {tokens}" },
                            chatFontSize = chatFontSize,
                            fontSizeScale = fontSizeScale
                        )
                    )
                    settings = manager.getSettings()
                    savedMessage = if (saved) "已保存个性化与全局设定" else "保存失败，请重试"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存全部设定")
            }
        }
    }

    // 添加记忆弹窗
    if (isAddingMemory) {
        MemoryEditDialog(
            hazeState = hazeState,
            memory = null,
            onDismiss = { isAddingMemory = false },
            onConfirm = { newContent, newScope, newKeywords ->
                coroutineScope.launch {
                    val item = MemoryItem(
                        content = newContent.trim(),
                        scope = newScope,
                        keywords = newKeywords.trim().takeIf { it.isNotBlank() },
                        confidence = 1.0f,
                        isEnabled = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.insertMemory(item)
                    isAddingMemory = false
                    savedMessage = "已添加新长期记忆"
                }
            }
        )
    }

    // 编辑记忆弹窗
    memoryToEdit?.let { memory ->
        MemoryEditDialog(
            hazeState = hazeState,
            memory = memory,
            onDismiss = { memoryToEdit = null },
            onConfirm = { newContent, newScope, newKeywords ->
                coroutineScope.launch {
                    repository.updateMemory(
                        memory.copy(
                            content = newContent.trim(),
                            scope = newScope,
                            keywords = newKeywords.trim().takeIf { it.isNotBlank() },
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    memoryToEdit = null
                    savedMessage = "已更新长期记忆"
                }
            }
        )
    }

    // 清空全部记忆确认弹窗
    if (showClearAllConfirm) {
        EchoGlassDialog(
            hazeState = hazeState,
            title = { Text("清空长期记忆库") },
            text = {
                Text(
                    "确定要清空全部长期记忆条目吗？此操作无法撤销，模型将不再参考先前的偏好记忆。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteAllMemories()
                            showClearAllConfirm = false
                            savedMessage = "已清空全部长期记忆"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("取消")
                }
            },
            onDismissRequest = { showClearAllConfirm = false }
        )
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryItem,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = echoGlassPalette()
    val scopeLabel = when (memory.scope) {
        "user", "global" -> "全局偏好"
        "conversation" -> "会话专属"
        else -> memory.scope
    }
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val timeText = remember(memory.updatedAt) { dateFormat.format(Date(memory.updatedAt)) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsInnerShape,
        color = if (memory.isEnabled) glass.control else glass.control.copy(alpha = 0.4f),
        contentColor = if (memory.isEnabled) glass.textPrimary else glass.textPrimary.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, glass.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = scopeLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑记忆",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除记忆",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = memory.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.height(24.dp)
                )
            }

            Text(
                text = memory.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (memory.isEnabled) glass.textPrimary else glass.textPrimary.copy(alpha = 0.6f)
            )

            if (!memory.keywords.isNullOrBlank()) {
                Text(
                    text = "关键词: ${memory.keywords}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun MemoryEditDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    memory: MemoryItem?,
    onDismiss: () -> Unit,
    onConfirm: (content: String, scope: String, keywords: String) -> Unit
) {
    var content by remember { mutableStateOf(memory?.content.orEmpty()) }
    var scope by remember { mutableStateOf(memory?.scope ?: "user") }
    var keywords by remember { mutableStateOf(memory?.keywords.orEmpty()) }

    val isUserScope = scope == "user" || scope == "global"
    val isConvScope = scope == "conversation"

    EchoGlassDialog(
        hazeState = hazeState,
        title = { Text(if (memory == null) "添加长期记忆" else "编辑长期记忆") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("记忆内容", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("例如：用户习惯用 Kotlin 编写 Android 应用...") },
                    minLines = 3,
                    maxLines = 8,
                    shape = SettingsInnerShape
                )

                Text("作用域范围", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isUserScope,
                        onClick = { scope = "user" },
                        label = { Text("全局偏好 (user)") },
                        colors = echoFilterChipColors(),
                        border = echoFilterChipBorder(isUserScope)
                    )
                    FilterChip(
                        selected = isConvScope,
                        onClick = { scope = "conversation" },
                        label = { Text("会话专属 (conversation)") },
                        colors = echoFilterChipColors(),
                        border = echoFilterChipBorder(isConvScope)
                    )
                }

                Text("关联关键词 (可选)", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("以逗号分隔，例如：kotlin, android, 开发") },
                    singleLine = true,
                    shape = SettingsInnerShape
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content, scope, keywords) },
                enabled = content.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun BackgroundPickerRow(
    title: String,
    hasImage: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val glass = echoGlassPalette()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsInnerShape,
        color = glass.control,
        contentColor = glass.textPrimary,
        border = androidx.compose.foundation.BorderStroke(1.dp, glass.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                if (hasImage) Icons.Default.CheckCircle else Icons.Default.Wallpaper,
                contentDescription = null,
                tint = if (hasImage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (hasImage) "已使用自定义图片" else "使用默认纯色背景",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onPick) {
                Text(if (hasImage) "更换" else "选择")
            }
            if (hasImage) {
                TextButton(onClick = onClear) {
                    Text("恢复")
                }
            }
        }
    }
}

@Composable
private fun PersonalizationTextField(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp),
            placeholder = { Text(placeholder) },
            minLines = 3,
            maxLines = 8,
            shape = SettingsInnerShape
        )
    }
}

@Composable
fun HiddenConversationsTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    onNavigateToChat: (Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = AiAssistantApp.instance.repository
    val scope = rememberCoroutineScope()
    val lock = remember(context) { HiddenConversationLock(context) }
    var hasPassword by remember { mutableStateOf(lock.hasPassword()) }
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val hiddenConversations by repository.getHiddenConversations().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsGlassCard(hazeState = hazeState) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("其他对话", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "隐藏对话不会出现在首页。请使用 6 位数字密码查看或取消隐藏。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
        }

        if (!hasPassword) {
            item {
                PinSetupCard(
                    hazeState = hazeState,
                    pin = pin,
                    confirmPin = confirmPin,
                    message = message,
                    onPinChange = { pin = it.onlySixDigits() },
                    onConfirmPinChange = { confirmPin = it.onlySixDigits() },
                    onSave = {
                        when {
                            pin.length != 6 || confirmPin.length != 6 -> message = "请输入 6 位数字密码"
                            pin != confirmPin -> message = "两次输入的密码不一致"
                            lock.setPassword(pin) -> {
                                hasPassword = true
                                unlocked = true
                                pin = ""
                                confirmPin = ""
                                message = "密码已设置"
                            }
                            else -> message = "密码保存失败，请重试"
                        }
                    }
                )
            }
        } else if (!unlocked) {
            item {
                PinVerifyCard(
                    hazeState = hazeState,
                    pin = pin,
                    message = message,
                    onPinChange = { pin = it.onlySixDigits() },
                    onVerify = {
                        if (lock.verify(pin)) {
                            unlocked = true
                            pin = ""
                            message = null
                        } else {
                            message = "密码不正确"
                        }
                    }
                )
            }
        } else {
            item {
                Text(
                    text = "隐藏对话",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hiddenConversations.isEmpty()) {
                item {
                    SettingsGlassCard(hazeState = hazeState) {
                        Text(
                            text = "当前没有隐藏对话。",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(hiddenConversations, key = { it.id }) { conversation ->
                    HiddenConversationCard(
                        hazeState = hazeState,
                        conversation = conversation,
                        onOpen = { onNavigateToChat(conversation.id) },
                        onUnhide = {
                            scope.launch {
                                repository.setConversationHidden(conversation.id, false)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinSetupCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    pin: String,
    confirmPin: String,
    message: String?,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onSave: () -> Unit
) {
    SettingsGlassCard(hazeState = hazeState) {
            Text("首次使用请设置密码", style = MaterialTheme.typography.titleSmall)
            PinField(value = pin, onValueChange = onPinChange, label = "输入 6 位数字密码")
            PinField(value = confirmPin, onValueChange = onConfirmPinChange, label = "再次输入密码")
            HiddenLockMessage(message)
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length == 6 && confirmPin.length == 6
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("设置并进入")
            }
    }
}

@Composable
private fun PinVerifyCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    pin: String,
    message: String?,
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    SettingsGlassCard(hazeState = hazeState) {
            Text("输入密码", style = MaterialTheme.typography.titleSmall)
            PinField(value = pin, onValueChange = onPinChange, label = "6 位数字密码")
            HiddenLockMessage(message)
            Button(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                enabled = pin.length == 6
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("进入其他对话")
            }
    }
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    SettingsInputField(
        title = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = "请输入 6 位数字",
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
}

@Composable
private fun HiddenLockMessage(message: String?) {
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun HiddenConversationCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    conversation: Conversation,
    onOpen: () -> Unit,
    onUnhide: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    SettingsGlassCard(hazeState = hazeState) {
        Text(
            text = conversation.title.ifBlank { "未命名对话" },
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "${conversation.modelName} · ${conversation.messageCount} 条 · ${dateFormat.format(Date(conversation.updatedAt))}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUnhide,
                modifier = Modifier.weight(1f)
            ) {
                Text("取消隐藏")
            }
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f)
            ) {
                Text("进入")
            }
        }
    }
}

private fun String.onlySixDigits(): String {
    return filter { it.isDigit() }.take(6)
}



@Composable
fun BackupTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var backups by remember { mutableStateOf(BackupManager.getBackupList(context)) }
    var isBackingUp by remember { mutableStateOf(false) }
    var showMessage by remember { mutableStateOf<String?>(null) }
    val exportBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            scope.launch {
                isBackingUp = true
                val result = BackupManager.exportBackupToUri(context, it)
                isBackingUp = false
                showMessage = if (result) "备份已导出到所选位置" else "备份导出失败"
                backups = BackupManager.getBackupList(context)
            }
        }
    }
    val importBackupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isBackingUp = true
                val result = BackupManager.restoreBackupFromUri(context, it)
                isBackingUp = false
                showMessage = if (result) "导入成功，请重启应用后查看恢复的数据" else "导入失败，请确认文件是 Echo 备份 zip"
                backups = BackupManager.getBackupList(context)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .echoHazePanel(
                        hazeState = hazeState,
                        shape = SettingsPanelShape,
                        tint = echoGlassPalette().panel,
                        blurRadius = 18.dp
                    ),
                shape = SettingsPanelShape,
                color = echoGlassPalette().panel,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "数据备份与恢复",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "定期备份可以防止数据丢失。建议在更新应用前备份数据。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        isBackingUp = true
                        val result = BackupManager.createBackup(context)
                        isBackingUp = false
                        if (result != null) {
                            showMessage = "备份成功！"
                            backups = BackupManager.getBackupList(context)
                        } else {
                            showMessage = "备份失败"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBackingUp
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Backup, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("立即备份")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val fileName = "Echo_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.zip"
                        exportBackupLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isBackingUp
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("导出备份")
                }
                OutlinedButton(
                    onClick = {
                        importBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isBackingUp
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("导入备份")
                }
            }
        }

        showMessage?.let { message ->
            item {
                Card(
                    shape = SettingsPanelShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("成功") || message.contains("已导出") || message.contains("已保存"))
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.82f)
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (backups.isNotEmpty()) {
            item {
                Text(
                    text = "备份列表",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            items(backups) { backup ->
                BackupItemCard(
                    hazeState = hazeState,
                    backup = backup,
                    onRestore = {
                        scope.launch {
                            val result = BackupManager.restoreBackup(context, backup.filePath)
                            showMessage = if (result) "恢复成功！请重启应用" else "恢复失败"
                        }
                    },
                    onDelete = {
                        scope.launch {
                            BackupManager.deleteBackup(backup.filePath)
                            backups = BackupManager.getBackupList(context)
                        }
                    },
                    onShare = {
                        BackupManager.shareBackup(context, backup.filePath)
                    }
                )
            }
        }
    }
}

@Composable
fun BackupItemCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    backup: BackupManager.BackupItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = SettingsPanelShape,
                tint = echoGlassPalette().panel,
                blurRadius = 18.dp
            ),
        shape = SettingsPanelShape,
        color = echoGlassPalette().panel,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FolderZip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = backup.fileName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = dateFormat.format(Date(backup.lastModified)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "分享")
            }
            IconButton(onClick = { showRestoreDialog = true }) {
                Icon(Icons.Default.Restore, contentDescription = "恢复")
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showRestoreDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text("恢复备份", style = MaterialTheme.typography.titleLarge)
            },
            content = {
                Text(
                    text = "确定要从 ${backup.fileName} 恢复数据吗？恢复后建议重启应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            onRestore()
                            showRestoreDialog = false
                        }
                    ) {
                        Text("恢复")
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("删除备份", style = MaterialTheme.typography.titleLarge)
            },
            content = {
                Text(
                    text = "确定要删除 ${backup.fileName} 吗？此操作不可撤销。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("取消")
                    }
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
                }
            }
        )
    }
}

@Composable
fun AboutTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 应用信息
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Echo",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 本次更新
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Column {
                    Text(
                        text = "本次更新",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrentVersionUserUpdates.forEach { update ->
                        FeatureItem(update)
                    }
                }
            }
        }

        // 功能特性
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Column {
                    Text(
                        text = "功能特性",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrentFeatureHighlights.forEach { feature ->
                        FeatureItem(feature)
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

private fun parseModelList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val parsed = try {
        val type = object : TypeToken<List<String>>() {}.type
        Gson().fromJson<List<String>>(raw, type).orEmpty()
    } catch (e: Exception) {
        raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    return cleanModelNames(parsed)
}

private fun cleanModelNames(models: List<String>): List<String> {
    return models.mapNotNull { cleanModelName(it) }.distinct()
}

private fun cleanModelName(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    val blockedEdges = setOf('"', '“', '”', '\'', '`', ']', '[', '\\')
    if (value.first() in blockedEdges || value.last() in blockedEdges) return null
    if (value.any { it.isISOControl() }) return null
    return value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    config: ApiConfig?,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (ApiConfig, List<String>, Set<String>, Map<String, String>, android.net.Uri?, Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = AiAssistantApp.instance.repository
    val context = androidx.compose.ui.platform.LocalContext.current
    val gson = remember { Gson() }

    var name by remember { mutableStateOf(config?.name ?: "") }
    var provider by remember { mutableStateOf(config?.provider ?: "") }
    var baseUrl by remember { mutableStateOf(config?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(config?.apiKey ?: "") }
    var apiType by remember { mutableStateOf(config?.apiType ?: "openai") }
    var modelName by remember { mutableStateOf(cleanModelName(config?.modelName) ?: "") }
    var availableModels by remember {
        mutableStateOf(parseModelList(config?.availableModels).ifEmpty {
            cleanModelName(config?.modelName)?.let { listOf(it) } ?: emptyList()
        })
    }
    var enabledModelNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    var modelCapabilities by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var modelsExpanded by remember { mutableStateOf(false) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var selectedApiAvatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var clearApiAvatar by remember { mutableStateOf(false) }
    var avatarRevision by remember { mutableIntStateOf(0) }
    val currentApiAvatarBitmap = remember(context, config?.id, selectedApiAvatarUri, clearApiAvatar, avatarRevision) {
        when {
            selectedApiAvatarUri != null -> null
            clearApiAvatar -> null
            config?.id != null && config.id > 0L -> AvatarManager.getApiModelAvatarBitmap(context, config.id)
            else -> null
        }
    }
    val apiAvatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedApiAvatarUri = it
            clearApiAvatar = false
            avatarRevision++
        }
    }

    // 预设配置
    val presets = mapOf(
        "anthropic" to Triple("https://api.anthropic.com/v1", "claude-3-5-sonnet-20241022", "Anthropic"),
        "deepseek" to Triple("https://api.deepseek.com/v1", "deepseek-chat", "DeepSeek"),
        "openai" to Triple("https://api.openai.com/v1", "gpt-4o", "OpenAI")
    )

    LaunchedEffect(config?.id) {
        if (config != null) {
            repository.getDecryptedConfig(config.id)?.let { decrypted ->
                apiKey = decrypted.apiKey
            }
            val selectedModels = repository.getSelectedModels(config.id).first()
            if (selectedModels.isNotEmpty()) {
                val savedNames = selectedModels.map { it.modelName }
                availableModels = cleanModelNames(savedNames + availableModels + config.modelName)
                enabledModelNames = selectedModels
                    .filter { it.isEnabled }
                    .map { it.modelName }
                    .toSet()
                    .ifEmpty { savedNames.toSet() }
                modelCapabilities = selectedModels.associate { it.modelName to it.capability }
            } else {
                enabledModelNames = availableModels.toSet()
            }
        } else {
            enabledModelNames = availableModels.toSet()
        }
    }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text(if (config == null) "添加API配置" else "编辑API配置") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                // 预设选择
                item {
                    Text("快速预设", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        presets.forEach { (key, value) ->
                            AssistChip(
                                onClick = {
                                    provider = value.third
                                    baseUrl = value.first
                                    modelName = value.second
                                    if (name.isBlank()) name = value.third
                                    apiType = key
                                },
                                label = { Text(value.third, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // API类型
                item {
                    Text("API类型", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val openAiSelected = apiType == "openai"
                        FilterChip(
                            selected = openAiSelected,
                            onClick = { apiType = "openai" },
                            colors = echoFilterChipColors(),
                            border = echoFilterChipBorder(openAiSelected),
                            elevation = echoFilterChipElevation(),
                            label = { Text("OpenAI") }
                        )
                        val anthropicSelected = apiType == "anthropic"
                        FilterChip(
                            selected = anthropicSelected,
                            onClick = { apiType = "anthropic" },
                            colors = echoFilterChipColors(),
                            border = echoFilterChipBorder(anthropicSelected),
                            elevation = echoFilterChipElevation(),
                            label = { Text("Anthropic") }
                        )
                    }
                }

                // 配置名称
                item {
                    SettingsInputField(
                        title = "配置名称",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "例如: 官方 OpenAI / 自定义中转"
                    )
                }

                // 提供商
                item {
                    SettingsInputField(
                        title = "提供商",
                        value = provider,
                        onValueChange = { provider = it },
                        placeholder = "例如: OpenAI / Anthropic / DeepSeek"
                    )
                }

                // Base URL
                item {
                    SettingsInputField(
                        title = "Base URL",
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        placeholder = "https://api.example.com/v1"
                    )
                }

                // API Key
                item {
                    SettingsInputField(
                        title = "API Key",
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = "sk-..."
                    )
                }

                // 模型选择
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SettingsInputField(
                            title = "此 API 的默认模型",
                            value = modelName,
                            onValueChange = { modelName = it },
                            placeholder = "例如: deepseek-chat / gpt-4o"
                        )
                        Text(
                            text = "新对话会先使用标记为“新对话默认API”的配置，再使用这里设置的默认模型。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isLoadingModels = true
                                    val result = repository.fetchAvailableModelsDirect(baseUrl, apiKey, apiType)
                                    result.onSuccess { models ->
                                        availableModels = cleanModelNames(models + modelName)
                                        enabledModelNames = when {
                                            enabledModelNames.isNotEmpty() -> enabledModelNames.intersect(availableModels.toSet()).ifEmpty {
                                                setOf(modelName).filter { it.isNotBlank() }.toSet()
                                            }
                                            modelName.isNotBlank() -> setOf(modelName)
                                            else -> models.take(1).toSet()
                                        }
                                        modelCapabilities = modelCapabilities.filterKeys { it in availableModels }
                                        if (modelName.isBlank() && availableModels.isNotEmpty()) {
                                            modelName = availableModels.first()
                                        }
                                        modelsExpanded = true
                                    }
                                    isLoadingModels = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = baseUrl.isNotBlank() && apiKey.isNotBlank() && !isLoadingModels
                        ) {
                            if (isLoadingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("获取模型列表")
                        }

                        if (availableModels.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { enabledModelNames = availableModels.toSet() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("全选")
                                }
                                TextButton(
                                    onClick = { enabledModelNames = setOf(modelName).filter { it.isNotBlank() }.toSet() },
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .widthIn(min = 112.dp)
                                ) {
                                    Text("仅当前模型")
                                }
                                TextButton(
                                    onClick = { enabledModelNames = emptySet() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("清空")
                                }
                            }
                            Text(
                                text = "左侧勾选框：是否在对话中展示。右侧圆点：设为当前 API 的默认模型。已展示 ${enabledModelNames.size} 个",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { modelsExpanded = !modelsExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (modelsExpanded) "收起模型列表" else "展开模型列表")
                                Icon(
                                    if (modelsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }

                if (availableModels.isNotEmpty() && modelsExpanded) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 4.dp),
                            shape = SettingsInnerShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "可用模型列表 (共 ${availableModels.size} 个)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "已启用 ${enabledModelNames.size} 个",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(availableModels, key = { it }) { model ->
                        ModelDisplaySelectionRow(
                            model = model,
                            checked = enabledModelNames.contains(model),
                            selected = modelName == model,
                            capability = modelCapabilities[model] ?: "auto",
                            onCheckedChange = { checked ->
                                enabledModelNames = if (checked) {
                                    enabledModelNames + model
                                } else {
                                    enabledModelNames - model
                                }
                            },
                            onSelectAsDefault = {
                                modelName = model
                                enabledModelNames = enabledModelNames + model
                            },
                            onRowClick = {
                                enabledModelNames = if (enabledModelNames.contains(model)) {
                                    enabledModelNames - model
                                } else {
                                    enabledModelNames + model
                                }
                                if (modelName.isBlank()) modelName = model
                            },
                            onCapabilityChange = { capability ->
                                modelCapabilities = modelCapabilities + (model to capability)
                            }
                        )
                    }
                }

                item {
                    ApiModelAvatarSection(
                        currentBitmap = currentApiAvatarBitmap,
                        hasPendingAvatar = selectedApiAvatarUri != null,
                        clearAvatar = clearApiAvatar,
                        onPickAvatar = { apiAvatarPicker.launch("image/*") },
                        onClearAvatar = {
                            selectedApiAvatarUri = null
                            clearApiAvatar = true
                            avatarRevision++
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanedCurrentModel = cleanModelName(modelName).orEmpty()
                    val modelNames = cleanModelNames(availableModels + cleanedCurrentModel)
                    val enabledModels = (enabledModelNames + modelName)
                        .mapNotNull { cleanModelName(it) }
                        .toSet()
                    val newConfig = ApiConfig(
                        id = config?.id ?: 0,
                        name = name.ifBlank { provider },
                        provider = provider,
                        baseUrl = baseUrl,
                        apiKey = apiKey,
                        apiType = apiType,
                        modelName = cleanedCurrentModel,
                        availableModels = modelNames.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
                        temperature = 0.95f,
                        maxTokens = 8192,
                        topP = 1.0f,
                        enableThinking = false,
                        thinkingEffort = "medium",
                        enableWebSearch = false,
                        isDefault = config?.isDefault ?: false,
                        createdAt = config?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(newConfig, modelNames, enabledModels, modelCapabilities, selectedApiAvatarUri, clearApiAvatar)
                },
                enabled = baseUrl.isNotBlank() && apiKey.isNotBlank() && cleanModelName(modelName) != null && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("保存")
                }
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
private fun ModelDisplaySelectionRow(
    model: String,
    checked: Boolean,
    selected: Boolean,
    capability: String,
    onCheckedChange: (Boolean) -> Unit,
    onSelectAsDefault: () -> Unit,
    onRowClick: () -> Unit,
    onCapabilityChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rowShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .echoShapeClick(rowShape, onClick = onRowClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = model,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick = onSelectAsDefault
        )
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "模型能力")
            }
            EchoGlassDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf(
                    "auto" to "自动判断",
                    "text" to "纯文本",
                    "multimodal" to "多模态"
                ).forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onCapabilityChange(value)
                            expanded = false
                        },
                        leadingIcon = {
                            if (capability == value) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiModelAvatarSection(
    currentBitmap: android.graphics.Bitmap?,
    hasPendingAvatar: Boolean,
    clearAvatar: Boolean,
    onPickAvatar: () -> Unit,
    onClearAvatar: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("此 API 的模型对话头像", style = MaterialTheme.typography.titleSmall)
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
                when {
                    currentBitmap != null -> Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "API模型头像",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    hasPendingAvatar -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    else -> Image(
                        painter = painterResource(id = R.drawable.deepseek),
                        contentDescription = "默认模型头像",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = when {
                        hasPendingAvatar -> "已选择新头像，保存后生效"
                        clearAvatar -> "保存后恢复默认头像"
                        currentBitmap != null -> "当前使用此 API 的自定义头像"
                        else -> "当前使用默认 deepseek 头像"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickAvatar, shape = RoundedCornerShape(999.dp)) {
                        Text("更换")
                    }
                    TextButton(onClick = onClearAvatar) {
                        Text("恢复默认")
                    }
                }
            }
        }
    }
}
