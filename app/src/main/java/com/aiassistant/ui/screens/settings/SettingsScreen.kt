@file:OptIn(ExperimentalMaterial3Api::class)

package com.aiassistant.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.aiassistant.data.repository.AiRepository
import com.aiassistant.domain.model.ApiConfig
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.EnvironmentVariable
import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.EchoGlassCard
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
import com.aiassistant.tools.search.SearchEngineType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aiassistant.tools.cloud.OpenMeteoWeatherEngine
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

internal val CurrentVersionUserUpdates = listOf(
    "【v1.9.17 本次更新】自定义联网搜索结果数：自由输入并指定 1~20 条搜索结果，精准控制会话上下文体积与搜索丰富度",
    "对话自动命名读取模型列表：自动读取 API 服务商模型列表供一键选择，支持显示上下文窗口与思考能力徽标，免除手动输入",
    "华为运动健康步数主动刷新：增加活动识别权限申请与硬件计步传感器主动探测刷新，彻底摆脱手动输入",
    "提示词与记忆优先级与机制明确：系统提示词 100% 独占覆盖全局提示词，个性化偏好全局引导，长记忆弹窗确认入库，角色创作物理严格隔离并在设置中清晰说明",
    "角色与创作设置 3 栏清爽重构：重构为剧情导向、角色世界观、模型参数三栏架构，剧情导演指令一触即达",
    "普通对话与角色扮演双向无损互转：支持从普通对话一键升级为故事创作并自动提炼主角人设，亦可将故事会话无损转回普通对话",
    "设置页冗长内容手风琴折叠：对长期记忆库、环境变量库、提示词模板库默认采用手风琴卡片折叠，大幅降低滚动认知负担",
    "模型全维度能力即时解析：上下文窗口 (4K~2M)、多模态 (Vision)、工具调用 (Tool Calling)、深度思考 (Reasoning) 及档位徽标即时显示",
    "UI 极客深色模式全面重构：WCAG AAA 超高对比度，深蓝黑极客美学，彻底根除灰紫杂色与白色气泡背景白斑",
    "对话输入框展开放大：对话页主输入框支持一键放大展开为宽敞编辑面板，长提示词、长代码与复杂剧情构思输入更从容",
    "系统提示词输入框可放大：对话设置与故事创作中系统提示词输入框支持一键放大，大幅改善长设定规则阅读和编辑体验",
    "系统提示词光标定位修复：精准解决点击修改系统提示词时光标被强制跳至文本开头的异常，精确响应点击落点与定位准确性",
    "对话设置窗口宽度与模型下拉对齐：优化对话设置弹窗宽度比例，展开模型列表与上方选择按钮严格等宽对齐",
    "对话页消息分割线：模型回复完毕后在日期时间行下方新增优雅微光分割线，对话轮次更分明、视觉流更舒适",
    "Exa 免Key 联网搜索引擎：官方托管免费搜索通道开箱即用，无需 API Key 即可实时联网",
    "手机设备与健康生态深度联动：无缝读取设备时间、GPS定位与逆地理编码、手机计步传感器及硬件状态"
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
    EchoGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = SettingsPanelShape,
        containerColor = glass.panel,
        borderColor = glass.outline,
        showBorder = true
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
                                    "web_search" -> "联网搜索与智能工具箱"
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
                title = "联网搜索与智能工具箱",
                subtitle = "Exa 免Key搜索、Open-Meteo天气、健康步数与设备硬件",
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
    val glass = echoGlassPalette()
    EchoGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsPanelShape,
        containerColor = glass.panel,
        borderColor = glass.outline,
        showBorder = true
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
    EchoGlassCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 78.dp),
        shape = itemShape,
        containerColor = glass.panel,
        borderColor = glass.outline,
        showBorder = true
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
    val toolHub = AiAssistantApp.instance.echoToolHub
    val tavilyManager = AiAssistantApp.instance.tavilySearchManager
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var searchEngine by remember { mutableStateOf(toolHub.getSearchEngine()) }
    var searchResultCount by remember { mutableIntStateOf(toolHub.getSearchResultCount()) }
    var exaApiKey by remember { mutableStateOf(toolHub.getExaApiKey()) }
    var jinaApiKey by remember { mutableStateOf(toolHub.getJinaApiKey()) }
    var deviceToolsEnabled by remember { mutableStateOf(toolHub.isDeviceToolsEnabled()) }
    var manualCity by remember { mutableStateOf(toolHub.locationAddressManager.getManualCity()) }

    var tavilySettings by remember { mutableStateOf(tavilyManager.getSettings()) }
    var tavilyApiKey by remember(tavilySettings) { mutableStateOf(tavilySettings.apiKey) }
    var tavilyEnabled by remember(tavilySettings) { mutableStateOf(tavilySettings.enabled) }
    var searchDepth by remember(tavilySettings) { mutableStateOf(tavilySettings.searchDepth) }
    var maxResults by remember(tavilySettings) { mutableStateOf(tavilySettings.maxResults.toString()) }
    var includeAnswer by remember(tavilySettings) { mutableStateOf(tavilySettings.includeAnswer) }

    var weatherTestResult by remember { mutableStateOf<String?>(null) }
    var isTestingWeather by remember { mutableStateOf(false) }

    var showHuaweiHealthSyncDialog by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    val hasUnsaved = remember(
        searchEngine, searchResultCount, exaApiKey, jinaApiKey, deviceToolsEnabled, manualCity,
        tavilySettings, tavilyApiKey, tavilyEnabled, searchDepth, maxResults, includeAnswer
    ) {
        searchEngine != toolHub.getSearchEngine() ||
        searchResultCount != toolHub.getSearchResultCount() ||
        exaApiKey != toolHub.getExaApiKey() ||
        jinaApiKey != toolHub.getJinaApiKey() ||
        deviceToolsEnabled != toolHub.isDeviceToolsEnabled() ||
        manualCity != toolHub.locationAddressManager.getManualCity() ||
        tavilyApiKey != tavilySettings.apiKey ||
        tavilyEnabled != tavilySettings.enabled ||
        searchDepth != tavilySettings.searchDepth ||
        maxResults != tavilySettings.maxResults.toString() ||
        includeAnswer != tavilySettings.includeAnswer
    }

    LaunchedEffect(hasUnsaved) {
        onUnsavedStateChanged(hasUnsaved)
    }

    fun performSave() {
        toolHub.setSearchEngine(searchEngine)
        toolHub.setSearchResultCount(searchResultCount)
        toolHub.setExaApiKey(exaApiKey)
        toolHub.setJinaApiKey(jinaApiKey)
        toolHub.setDeviceToolsEnabled(deviceToolsEnabled)
        toolHub.locationAddressManager.setManualCity(manualCity)

        val newTavily = TavilySearchSettings(
            enabled = tavilyEnabled,
            apiKey = tavilyApiKey,
            searchDepth = searchDepth,
            maxResults = maxResults.toIntOrNull()?.coerceIn(1, 20) ?: 8,
            includeAnswer = includeAnswer
        )
        val ok = tavilyManager.saveSettings(newTavily)
        tavilySettings = tavilyManager.getSettings()
        savedMessage = if (ok) "联网搜索与智能工具箱配置已保存" else "保存失败，请重试"
    }

    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            performSave()
        }
    }

    val currentTime = remember { toolHub.timeCalendarManager.getCurrentTimeFormatted() }
    var currentLocation by remember { mutableStateOf(toolHub.locationAddressManager.getCurrentLocation()) }
    var currentHealthSummary by remember { mutableStateOf(toolHub.healthDataManager.getHealthDataSummary()) }
    val deviceStatus = remember { toolHub.deviceHardwareManager.getDeviceStatus() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 卡片 1：搜索引擎选择
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("联网搜索引擎", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "为对话中的智能联网选择搜索底层提供商。Exa 采用官方云端托管，支持免 Key 直接调用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchEngineType.entries.forEach { engine ->
                        val selected = searchEngine == engine
                        FilterChip(
                            selected = selected,
                            onClick = { searchEngine = engine },
                            colors = echoFilterChipColors(),
                            border = echoFilterChipBorder(selected),
                            elevation = echoFilterChipElevation(),
                            label = { Text(engine.displayName) }
                        )
                    }
                }

                if (searchEngine == SearchEngineType.EXA) {
                    Surface(
                        shape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeMd,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "免 Key 体验模式生效中：日常对话联网开箱即用，无需申请与配置 API 密钥。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    SettingsInputField(
                        title = "Exa API Key (选填)",
                        value = exaApiKey,
                        onValueChange = { exaApiKey = it },
                        placeholder = "留空则使用官方免Key通道"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("启用 Tavily 搜索", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = tavilyEnabled, onCheckedChange = { tavilyEnabled = it })
                    }

                    SettingsInputField(
                        title = "Tavily API Key",
                        value = tavilyApiKey,
                        onValueChange = { tavilyApiKey = it },
                        placeholder = "tvly-..."
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("basic", "advanced").forEach { depth ->
                            val selected = searchDepth == depth
                            FilterChip(
                                selected = selected,
                                onClick = { searchDepth = depth },
                                colors = echoFilterChipColors(),
                                border = echoFilterChipBorder(selected),
                                elevation = echoFilterChipElevation(),
                                label = { Text(if (depth == "basic") "基础搜索" else "深入搜索") }
                            )
                        }
                    }

                    SettingsInputField(
                        title = "最大结果数 (1-20)",
                        value = maxResults,
                        onValueChange = { v -> maxResults = v.filter { it.isDigit() }.take(2) },
                        placeholder = "8",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("包含 Tavily 自动摘要", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = includeAnswer, onCheckedChange = { includeAnswer = it })
                    }
                }

                // 搜索结果返回条数选择 (支持 3 / 5 / 8 / 10 条及自定义 1~20)
                var showCustomCountDialog by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "搜索返回条数：当前 $searchResultCount 条",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 8, 10).forEach { count ->
                            val selected = searchResultCount == count
                            FilterChip(
                                selected = selected,
                                onClick = { searchResultCount = count },
                                colors = echoFilterChipColors(),
                                border = echoFilterChipBorder(selected),
                                elevation = echoFilterChipElevation(),
                                label = { Text(if (count == 5) "5条 (推荐)" else "${count}条") }
                            )
                        }
                        val isCustom = searchResultCount !in listOf(3, 5, 8, 10)
                        FilterChip(
                            selected = isCustom,
                            onClick = { showCustomCountDialog = true },
                            colors = echoFilterChipColors(),
                            border = echoFilterChipBorder(isCustom),
                            elevation = echoFilterChipElevation(),
                            label = { Text(if (isCustom) "自定义: ${searchResultCount}条" else "自定义...") }
                        )
                    }

                    if (showCustomCountDialog) {
                        var customInput by remember { mutableStateOf(searchResultCount.toString()) }
                        AlertDialog(
                            onDismissRequest = { showCustomCountDialog = false },
                            title = { Text("自定义搜索结果数 (1-20)") },
                            text = {
                                OutlinedTextField(
                                    value = customInput,
                                    onValueChange = { customInput = it.filter { char -> char.isDigit() }.take(2) },
                                    label = { Text("条数 (1~20)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val count = customInput.toIntOrNull()?.coerceIn(1, 20) ?: 5
                                    searchResultCount = count
                                    showCustomCountDialog = false
                                }) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomCountDialog = false }) {
                                    Text("取消")
                                }
                            }
                        )
                    }
                }
            }
        }

        // 卡片 2：手机本地设备与健康工具
        item {
            val activityRecognitionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                toolHub.healthDataManager.forceRefreshHardwareSteps()
                currentHealthSummary = toolHub.healthDataManager.getHealthDataSummary()
            }

            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("手机设备与健康数据联动", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("允许模型根据问题读取时间、定位、步数、心率、睡眠及硬件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = deviceToolsEnabled, onCheckedChange = { deviceToolsEnabled = it })
                }

                if (deviceToolsEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 时间与日历
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("系统时间：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(currentTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // 定位与逆地理编码
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("当前定位：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text(currentLocation.city.ifBlank { "检测中" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { currentLocation = toolHub.locationAddressManager.getCurrentLocation() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("刷新定位", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (currentLocation.fullAddress.isNotBlank()) {
                            Text("详细位置: ${currentLocation.fullAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SettingsInputField(
                            title = "手动指定常驻城市（选填，留空自动定位）",
                            value = manualCity,
                            onValueChange = { manualCity = it },
                            placeholder = "例如：深圳 / 北京 / 上海"
                        )
                    }

                    // 健康与运动数据 (华为运动健康与硬件计步)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("华为运动健康 / 硬件计步：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Text("${currentHealthSummary.todaySteps} 步", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q &&
                                            !toolHub.healthDataManager.hasActivityRecognitionPermission()
                                        ) {
                                            activityRecognitionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                                        } else {
                                            toolHub.healthDataManager.forceRefreshHardwareSteps()
                                            currentHealthSummary = toolHub.healthDataManager.getHealthDataSummary()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("刷新传感器", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(
                                    onClick = { showHuaweiHealthSyncDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("数据校准", style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(
                                    onClick = {
                                        if (!toolHub.healthDataManager.openHuaweiHealthApp(context)) {
                                            savedMessage = "未检测到已安装的华为运动健康应用"
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("打开华为健康", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Text(
                            "传感器: ${toolHub.healthDataManager.getSensorStatusText()} · 上次更新: ${toolHub.healthDataManager.getLastUpdateTime()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val hrDisplay = if (currentHealthSummary.heartRate > 0) "${currentHealthSummary.heartRate} bpm" else "暂未录入"
                        val sleepDisplay = if (currentHealthSummary.sleepMinutes > 0) {
                            "${currentHealthSummary.sleepMinutes / 60}小时${currentHealthSummary.sleepMinutes % 60}分" +
                                if (currentHealthSummary.deepSleepMinutes > 0) " (深睡 ${currentHealthSummary.deepSleepMinutes / 60}小时${currentHealthSummary.deepSleepMinutes % 60}分)" else "" +
                                if (currentHealthSummary.sleepScore > 0) " · 评分: ${currentHealthSummary.sleepScore}" else ""
                        } else {
                            "暂未录入"
                        }
                        Text(
                            "心率: $hrDisplay · 昨晚睡眠: $sleepDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 硬件状态
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "电池: ${if (deviceStatus.batteryLevel >= 0) "${deviceStatus.batteryLevel}%" else "未知"}${if (deviceStatus.isCharging) " (充电中 ⚡)" else ""} · ${deviceStatus.deviceModel} · ${deviceStatus.networkType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 卡片 3：公开免 Key 云端工具 (Open-Meteo 与 Jina Reader)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Text("开放免Key云端工具", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                // Open-Meteo
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open-Meteo 全球气象 (免Key)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("全球高精度天气与预报，无需任何 API Key", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                isTestingWeather = true
                                coroutineScope.launch {
                                    val loc = toolHub.locationAddressManager.getCurrentLocation()
                                    val res = withContext(Dispatchers.IO) {
                                        toolHub.openMeteoWeatherEngine.getWeather(
                                            cityName = manualCity.ifBlank { null },
                                            defaultLat = loc.latitude,
                                            defaultLon = loc.longitude
                                        )
                                    }
                                    isTestingWeather = false
                                    weatherTestResult = res.fold(
                                        onSuccess = { "${it.cityName}: ${it.condition} · 气温 ${String.format(Locale.US, "%.1f", it.temperature)}℃ · 湿度 ${it.humidity}% · 风速 ${String.format(Locale.US, "%.1f", it.windSpeed)}km/h" },
                                        onFailure = { "查询失败: ${it.message}" }
                                    )
                                }
                            },
                            enabled = !isTestingWeather
                        ) {
                            Text(if (isTestingWeather) "测试中..." else "测试天气")
                        }
                    }
                    weatherTestResult?.let { result ->
                        Surface(
                            shape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeSm,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(result, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Jina Reader
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Jina Reader 网页长文提取", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text("输入网页网址时，自动抓取并转换为纯净 Markdown 正文，免去广告干扰。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsInputField(
                        title = "Jina API Key (选填)",
                        value = jinaApiKey,
                        onValueChange = { jinaApiKey = it },
                        placeholder = "留空使用免费通道，填入可避免数据中心限制"
                    )
                }
            }
        }

        // 保存按键
        item {
            Button(
                onClick = { performSave() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存联网搜索与工具配置")
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

    if (showHuaweiHealthSyncDialog) {
        HuaweiHealthSyncDialog(
            initialSteps = currentHealthSummary.todaySteps,
            initialHeartRate = currentHealthSummary.heartRate,
            initialSleepMinutes = currentHealthSummary.sleepMinutes,
            onDismiss = { showHuaweiHealthSyncDialog = false },
            onCalibrateSteps = { steps ->
                toolHub.healthDataManager.calibrateTodaySteps(steps)
                currentHealthSummary = toolHub.healthDataManager.getHealthDataSummary()
            },
            onSyncAll = { steps, hr, sleep, deepSleep, score ->
                toolHub.healthDataManager.syncHuaweiHealthData(steps, hr, sleep, deepSleep, score)
                currentHealthSummary = toolHub.healthDataManager.getHealthDataSummary()
            }
        )
    }
}

@Composable
fun HuaweiHealthSyncDialog(
    initialSteps: Int,
    initialHeartRate: Int,
    initialSleepMinutes: Int,
    onDismiss: () -> Unit,
    onCalibrateSteps: (Int) -> Unit,
    onSyncAll: (steps: Int, heartRate: Int, sleepMinutes: Int, deepSleepMinutes: Int, score: Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var stepsText by remember { mutableStateOf(if (initialSteps > 0) initialSteps.toString() else "") }
    var heartRateText by remember { mutableStateOf(if (initialHeartRate > 0) initialHeartRate.toString() else "") }
    var sleepHoursText by remember { mutableStateOf(if (initialSleepMinutes > 0) (initialSleepMinutes / 60).toString() else "") }
    var sleepMinsText by remember { mutableStateOf(if (initialSleepMinutes > 0) (initialSleepMinutes % 60).toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("华为运动健康数据校准与同步", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "机制说明：受 Android 系统安全沙箱保护，三方应用无法直接跨应用暗中读取华为运动健康私有数据。当前步数由本机硬件计步传感器自动累加；若需将手环/手表记录的心率与睡眠同步给 AI，可点击下方打开华为运动健康 APP 对照填入。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        val pm = context.packageManager
                        val launchIntent = pm.getLaunchIntentForPackage("com.huawei.health")
                        if (launchIntent != null) {
                            launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("打开华为运动健康 APP 查看数据")
                }

                SettingsInputField(
                    title = "今日实时步数 (步)",
                    value = stepsText,
                    onValueChange = { stepsText = it.filter { c -> c.isDigit() }.take(6) },
                    placeholder = if (initialSteps > 0) initialSteps.toString() else "输入今日步数",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                SettingsInputField(
                    title = "静态/静息心率 (bpm，可选)",
                    value = heartRateText,
                    onValueChange = { heartRateText = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = "留空表示未录入",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SettingsInputField(
                            title = "昨晚睡眠 (小时)",
                            value = sleepHoursText,
                            onValueChange = { sleepHoursText = it.filter { c -> c.isDigit() }.take(2) },
                            placeholder = "可选，如 7",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        SettingsInputField(
                            title = "睡眠零头 (分钟)",
                            value = sleepMinsText,
                            onValueChange = { sleepMinsText = it.filter { c -> c.isDigit() }.take(2) },
                            placeholder = "可选，如 30",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val steps = stepsText.toIntOrNull() ?: initialSteps
                    val hr = heartRateText.toIntOrNull() ?: -1
                    val totalSleep = if (sleepHoursText.isNotBlank() || sleepMinsText.isNotBlank()) {
                        (sleepHoursText.toIntOrNull() ?: 0) * 60 + (sleepMinsText.toIntOrNull() ?: 0)
                    } else {
                        -1
                    }
                    val deepSleep = if (totalSleep > 0) (totalSleep * 0.28f).toInt() else -1
                    val score = if (totalSleep in 420..540) 88 else if (totalSleep > 0) 80 else -1
                    onSyncAll(steps, hr, totalSleep, deepSleep, score)
                    onDismiss()
                }
            ) {
                Text("保存并完成校准")
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
    var globalRoleplayPrompt by remember(settings) { mutableStateOf(settings.globalRoleplayPrompt) }
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

    var autoNameEnabled by remember(settings) { mutableStateOf(settings.autoNameEnabled) }
    var autoNameApiConfigId by remember(settings) { mutableLongStateOf(settings.autoNameApiConfigId) }
    var autoNameModel by remember(settings) { mutableStateOf(settings.autoNameModel) }
    var autoNamePrompt by remember(settings) { mutableStateOf(settings.autoNamePrompt) }
    val allApiConfigs by repository.getAllApiConfigs().collectAsState(initial = emptyList())

    var testAutoNameInput by remember { mutableStateOf("帮我写一个Python快速排序算法") }
    var testAutoNameResult by remember { mutableStateOf<String?>(null) }
    var isTestingAutoName by remember { mutableStateOf(false) }

    var savedMessage by remember { mutableStateOf<String?>(null) }

    val hasUnsaved = remember(
        settings, globalPrompt, globalRoleplayPrompt, instruction, autoMemoryEnabled, thinkingTemplate, chatFontSize, fontSizeScale,
        autoNameEnabled, autoNameApiConfigId, autoNameModel, autoNamePrompt
    ) {
        globalPrompt.trim() != settings.globalSystemPrompt.trim() ||
        globalRoleplayPrompt.trim() != settings.globalRoleplayPrompt.trim() ||
        instruction.trim() != settings.aboutUser.trim() ||
        autoMemoryEnabled != settings.autoMemoryEnabled ||
        thinkingTemplate.trim() != settings.thinkingCapsuleTemplate.trim() ||
        chatFontSize != settings.chatFontSize ||
        fontSizeScale != settings.fontSizeScale ||
        autoNameEnabled != settings.autoNameEnabled ||
        autoNameApiConfigId != settings.autoNameApiConfigId ||
        autoNameModel.trim() != settings.autoNameModel.trim() ||
        autoNamePrompt.trim() != settings.autoNamePrompt.trim()
    }

    LaunchedEffect(hasUnsaved) {
        onUnsavedStateChanged(hasUnsaved)
    }

    fun performSave() {
        val saved = manager.saveSettings(
            settings.copy(
                globalSystemPrompt = globalPrompt.trim(),
                globalRoleplayPrompt = globalRoleplayPrompt.trim(),
                aboutUser = instruction.trim(),
                responseStyle = "",
                preferences = "",
                avoid = "",
                autoMemoryEnabled = autoMemoryEnabled,
                thinkingCapsuleTemplate = thinkingTemplate.trim().ifBlank { "{model} {status} {time} {tokens}" },
                chatFontSize = chatFontSize,
                fontSizeScale = fontSizeScale,
                autoNameEnabled = autoNameEnabled,
                autoNameApiConfigId = autoNameApiConfigId,
                autoNameModel = autoNameModel.trim(),
                autoNamePrompt = autoNamePrompt.trim()
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
    var isMemoriesExpanded by remember { mutableStateOf(false) }
    var memorySearchQuery by remember { mutableStateOf("") }
    val allMemories by remember(memorySearchQuery) {
        if (memorySearchQuery.isBlank()) repository.getAllMemories() else repository.searchMemories(memorySearchQuery.trim())
    }.collectAsState(initial = emptyList())

    var memoryToEdit by remember { mutableStateOf<MemoryItem?>(null) }
    var isAddingMemory by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    // 提示词模板状态 (Requirement 7)
    var isTemplatesExpanded by remember { mutableStateOf(false) }
    var templateSearchQuery by remember { mutableStateOf("") }
    val allTemplates by repository.getAllPromptTemplates().collectAsState(initial = emptyList())
    val filteredTemplates = remember(allTemplates, templateSearchQuery) {
        if (templateSearchQuery.isBlank()) allTemplates
        else allTemplates.filter { it.name.contains(templateSearchQuery.trim(), ignoreCase = true) || it.content.contains(templateSearchQuery.trim(), ignoreCase = true) }
    }
    var templateToEdit by remember { mutableStateOf<PromptTemplate?>(null) }
    var isAddingTemplate by remember { mutableStateOf(false) }

    // 环境变量状态 (Requirement 7)
    var isEnvVarsExpanded by remember { mutableStateOf(false) }
    val allEnvVars by repository.getAllEnvironmentVariables().collectAsState(initial = emptyList())
    var envVarToEdit by remember { mutableStateOf<EnvironmentVariable?>(null) }
    var isAddingEnvVar by remember { mutableStateOf(false) }

    // 机制与优先级说明折叠状态
    var showPriorityDetails by remember { mutableStateOf(false) }

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

        // 3.1 提示词与记忆生效机制与优先级说明 (Requirement 4)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(SettingsInnerShape) { showPriorityDetails = !showPriorityDetails },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("提示词与记忆生效机制与优先级说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "系统提示词 / 全局提示词 / 个性化偏好 / 长期记忆规则",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showPriorityDetails = !showPriorityDetails }) {
                        Icon(
                            if (showPriorityDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = showPriorityDetails) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PriorityRuleRow(
                            badge = "最高优先级",
                            title = "会话专属系统提示词 (100% 独占)",
                            description = "在单个对话设置中填写的系统提示词拥有最高优先级。当其存在时，全局系统提示词将被 100% 覆盖，0 作用生效。",
                            badgeColor = MaterialTheme.colorScheme.primary
                        )
                        PriorityRuleRow(
                            badge = "默认兜底",
                            title = "全局系统提示词 (全局兜底)",
                            description = "仅在对话未设置任何专属系统提示词时自动继承；一旦对话设置了专属提示词即刻失效。",
                            badgeColor = MaterialTheme.colorScheme.secondary
                        )
                        PriorityRuleRow(
                            badge = "全局引导",
                            title = "个性化偏好 (全局引导)",
                            description = "对所有普通对话起全局引导与输出润色效果；若偏好内容与提示词规则发生冲突，严格以提示词为准。",
                            badgeColor = MaterialTheme.colorScheme.tertiary
                        )
                        PriorityRuleRow(
                            badge = "弹窗确认",
                            title = "长期记忆 vs 会话记忆 (严禁静默入库)",
                            description = "模型识别到重要偏好或事实后，必须在聊天输入框上方弹出确认浮条，由您主动点击【存为跨会话长期记忆】或【仅本会话生效】或【忽略】，杜绝静默污染记忆库。",
                            badgeColor = MaterialTheme.colorScheme.primary
                        )
                        PriorityRuleRow(
                            badge = "物理隔离",
                            title = "角色扮演与故事创作 (物理严格隔离)",
                            description = "角色与故事创作拥有独立角色卡、世界观与剧情备忘录，绝对不读取也不污染普通对话的提示词、偏好与日常记忆。",
                            badgeColor = MaterialTheme.colorScheme.error
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("故事创作与角色扮演全局教学指引", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "用于教学模型如何创作故事、行文规范与沉浸感（如以演代述、避免性格副词、维持角色独立性）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = {
                            globalRoleplayPrompt = com.aiassistant.data.repository.RoleplayRepository.DEFAULT_FICTION_TEACHING_GUIDELINES
                            savedMessage = null
                        }
                    ) {
                        Text("填入默认规范", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = globalRoleplayPrompt,
                    onValueChange = {
                        globalRoleplayPrompt = it
                        savedMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    placeholder = { Text("留空将使用内置文学创作铁律（Show Don't Tell、真实对白与微动作交融）...") },
                    minLines = 4,
                    maxLines = 14,
                    shape = SettingsInnerShape
                )
            }
        }

        // 4.1 对话智能自动命名模型 (Requirement 2 & 7)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DriveFileRenameOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("对话智能自动命名", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "新对话首轮交互后自动生成简短精炼标题，可指定高速低成本专属模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoNameEnabled,
                        onCheckedChange = {
                            autoNameEnabled = it
                            savedMessage = null
                        }
                    )
                }

                if (autoNameEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // API 配置选择
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "命名专用 API 服务商：",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // 选项列表
                        val configOptions = listOf(0L to "跟随当前会话 API 配置（默认）") +
                            allApiConfigs.map { it.id to "${it.name.ifBlank { it.provider }} (${it.provider})" }

                        var expandedApiDropdown by remember { mutableStateOf(false) }
                        val currentConfigLabel = configOptions.find { it.first == autoNameApiConfigId }?.second
                            ?: "跟随当前会话 API 配置（默认）"

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedApiDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = SettingsInnerShape
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentConfigLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedApiDropdown,
                                onDismissRequest = { expandedApiDropdown = false }
                            ) {
                                configOptions.forEach { (cfgId, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                fontWeight = if (cfgId == autoNameApiConfigId) FontWeight.Bold else FontWeight.Normal,
                                                color = if (cfgId == autoNameApiConfigId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            autoNameApiConfigId = cfgId
                                            expandedApiDropdown = false
                                            savedMessage = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 指定模型选择 (Requirement 2: 读取模型列表选择，而不是手动输入模型)
                    val currentTargetConfig = remember(autoNameApiConfigId, allApiConfigs) {
                        if (autoNameApiConfigId > 0L) {
                            allApiConfigs.find { it.id == autoNameApiConfigId }
                        } else {
                            allApiConfigs.find { it.isDefault } ?: allApiConfigs.firstOrNull()
                        }
                    }
                    var isFetchingAutoNameModels by remember { mutableStateOf(false) }
                    var autoNameFetchedModels by remember(currentTargetConfig?.id) { mutableStateOf<List<String>>(emptyList()) }
                    val autoNameModelOptions = remember(currentTargetConfig, autoNameFetchedModels) {
                        val fromConfig = parseModelList(currentTargetConfig?.availableModels)
                        val defaultModel = cleanModelName(currentTargetConfig?.modelName)
                        (autoNameFetchedModels + fromConfig + listOfNotNull(defaultModel)).distinct().filter { it.isNotBlank() }
                    }
                    var expandedModelDropdown by remember { mutableStateOf(false) }
                    var isManualInputMode by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "指定命名专用模型：",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentTargetConfig != null && !isManualInputMode) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            isFetchingAutoNameModels = true
                                            val res = repository.fetchAvailableModels(currentTargetConfig.id)
                                            res.onSuccess { models ->
                                                autoNameFetchedModels = models
                                            }
                                            isFetchingAutoNameModels = false
                                        }
                                    },
                                    enabled = !isFetchingAutoNameModels
                                ) {
                                    if (isFetchingAutoNameModels) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text("从服务商获取", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (!isManualInputMode) {
                            val selectedModelLabel = if (autoNameModel.isBlank()) {
                                "跟随配置默认 (${currentTargetConfig?.modelName?.ifBlank { "未指定" } ?: "未指定"})"
                            } else {
                                autoNameModel
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedModelDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = SettingsInnerShape
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedModelLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = expandedModelDropdown,
                                    onDismissRequest = { expandedModelDropdown = false },
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "跟随配置默认 (${currentTargetConfig?.modelName?.ifBlank { "未指定" } ?: "未指定"})",
                                                fontWeight = if (autoNameModel.isBlank()) FontWeight.Bold else FontWeight.Normal,
                                                color = if (autoNameModel.isBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            autoNameModel = ""
                                            expandedModelDropdown = false
                                            savedMessage = null
                                        }
                                    )
                                    autoNameModelOptions.forEach { m ->
                                        val cap = com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(m)
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        m,
                                                        fontWeight = if (autoNameModel == m) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (autoNameModel == m) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (cap.contextWindowDisplay.isNotBlank()) {
                                                            Text(cap.contextWindowDisplay, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                        }
                                                        if (cap.supportsVision) Text("视觉", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                                        if (cap.supportsReasoning) Text("思考", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                autoNameModel = m
                                                expandedModelDropdown = false
                                                savedMessage = null
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("手动输入自定义模型名称...", color = MaterialTheme.colorScheme.secondary) },
                                        onClick = {
                                            isManualInputMode = true
                                            expandedModelDropdown = false
                                        }
                                    )
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = autoNameModel,
                                    onValueChange = {
                                        autoNameModel = it
                                        savedMessage = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("例如 gpt-4o-mini / deepseek-chat") },
                                    singleLine = true,
                                    shape = SettingsInnerShape
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(onClick = { isManualInputMode = false }) {
                                    Text("切换列表")
                                }
                            }
                        }
                    }

                    // 命名提示词模板
                    SettingsInputField(
                        title = "命名提示词指令 (选填)",
                        value = autoNamePrompt,
                        onValueChange = {
                            autoNamePrompt = it
                            savedMessage = null
                        },
                        placeholder = "请根据下面这段对话，生成一个简短精炼的中文标题。严格在12个字以内，不要标点符号与引号。"
                    )

                    // 测试命名效果
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = SettingsInnerShape,
                        color = glass.control.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, glass.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("实时测试自动命名效果：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = testAutoNameInput,
                                onValueChange = { testAutoNameInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("输入示例文本...") },
                                singleLine = true
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        isTestingAutoName = true
                                        testAutoNameResult = null
                                        coroutineScope.launch {
                                            val targetId = if (autoNameApiConfigId > 0L) autoNameApiConfigId else allApiConfigs.firstOrNull()?.id ?: 0L
                                            if (targetId == 0L) {
                                                testAutoNameResult = "未找到可用的 API 配置，请先在模型设置中添加 API"
                                                isTestingAutoName = false
                                                return@launch
                                            }
                                            val res = repository.testAutoNaming(
                                                apiConfigId = targetId,
                                                modelName = autoNameModel,
                                                testText = testAutoNameInput,
                                                customPrompt = autoNamePrompt
                                            )
                                            isTestingAutoName = false
                                            testAutoNameResult = res.fold(
                                                onSuccess = { "生成标题成功: 「$it」" },
                                                onFailure = { "生成失败: ${it.message}" }
                                            )
                                        }
                                    },
                                    enabled = !isTestingAutoName && testAutoNameInput.isNotBlank()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isTestingAutoName) "生成中..." else "测试生成标题")
                                }
                            }
                            testAutoNameResult?.let { resText ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = resText,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (resText.startsWith("生成标题成功")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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

        // 6. 模型长期记忆库模块 (Requirement 7: 手风琴折叠卡片)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(SettingsInnerShape) { isMemoriesExpanded = !isMemoriesExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("模型长期记忆库 (共 ${allMemories.size} 条)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (isMemoriesExpanded) "点击收起记忆库管理" else "点击展开查看、搜索与管理已记住的偏好与事实",
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
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { isMemoriesExpanded = !isMemoriesExpanded }) {
                        Icon(
                            if (isMemoriesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = isMemoriesExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (autoMemoryEnabled) "已开启自动记忆：模型将在对话中智能提炼并弹出确认条" else "自动记忆已暂停：模型不再从新对话中检测新记忆",
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
                                        if (memorySearchQuery.isBlank()) "暂无长期记忆条目\n点击上方「添加」手动写入，或在聊天中确认自动提炼的记忆。" else "未找到与「$memorySearchQuery」匹配的记忆",
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
            }
        }

        // 6.1 提示词模板库模块 (Requirement 7: 手风琴折叠卡片)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(SettingsInnerShape) { isTemplatesExpanded = !isTemplatesExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("提示词模板库 (共 ${allTemplates.size} 个)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (isTemplatesExpanded) "点击收起模板库管理" else "点击展开管理常用提示词模板与插值变量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isTemplatesExpanded = !isTemplatesExpanded }) {
                        Icon(
                            if (isTemplatesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = isTemplatesExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "提示词模板支持使用 {{变量}} 占位符，支持在聊天输入框与快捷工作流中秒级调用。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 搜索与添加
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = templateSearchQuery,
                                onValueChange = { templateSearchQuery = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("搜索模板名称或内容...") },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (templateSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { templateSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "清除搜索", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                shape = SettingsInnerShape
                            )

                            FilledTonalButton(
                                onClick = { isAddingTemplate = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("新建", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (filteredTemplates.isEmpty()) {
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
                                        Icons.Default.Bookmarks,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        if (templateSearchQuery.isBlank()) "暂无自定义提示词模板\n点击上方「新建」即可创建通用或创作提示词模板。" else "未找到匹配的模板",
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
                                filteredTemplates.forEach { template ->
                                    PromptTemplateItemCard(
                                        template = template,
                                        onEdit = { templateToEdit = template },
                                        onDelete = {
                                            coroutineScope.launch {
                                                repository.deletePromptTemplate(template)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6.2 环境变量库模块 (Requirement 7: 手风琴折叠卡片)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(SettingsInnerShape) { isEnvVarsExpanded = !isEnvVarsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("环境变量库 (共 ${allEnvVars.size} 个)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (isEnvVarsExpanded) "点击收起环境变量管理" else "点击展开管理提示词动态占位符与安全变量",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isEnvVarsExpanded = !isEnvVarsExpanded }) {
                        Icon(
                            if (isEnvVarsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = isEnvVarsExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "提示词中输入 {{变量名}} 可自动替换为变量值；敏感值支持加密安全存储与脱敏隐藏。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { isAddingEnvVar = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加变量", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (allEnvVars.isEmpty()) {
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
                                        Icons.Default.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "暂无自定义环境变量\n点击右上角「添加变量」创建可动态替换的变量。",
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
                                allEnvVars.forEach { variable ->
                                    EnvironmentVariableItemCard(
                                        variable = variable,
                                        onEdit = { envVarToEdit = variable },
                                        onDelete = {
                                            coroutineScope.launch {
                                                repository.deleteEnvironmentVariable(variable)
                                            }
                                        }
                                    )
                                }
                            }
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
                    performSave()
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

    // 提示词模板创建与编辑弹窗
    if (isAddingTemplate) {
        PromptTemplateEditDialog(
            hazeState = hazeState,
            template = null,
            onDismiss = { isAddingTemplate = false },
            onConfirm = { name, content, description, category ->
                coroutineScope.launch {
                    val template = PromptTemplate(
                        name = name,
                        content = content,
                        description = description,
                        category = category,
                        isBuiltIn = false,
                        useCount = 0,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.savePromptTemplate(template)
                    isAddingTemplate = false
                    savedMessage = "已创建提示词模板「$name」"
                }
            }
        )
    }

    templateToEdit?.let { template ->
        PromptTemplateEditDialog(
            hazeState = hazeState,
            template = template,
            onDismiss = { templateToEdit = null },
            onConfirm = { name, content, description, category ->
                coroutineScope.launch {
                    repository.savePromptTemplate(
                        template.copy(
                            name = name,
                            content = content,
                            description = description,
                            category = category,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    templateToEdit = null
                    savedMessage = "已更新提示词模板「$name」"
                }
            }
        )
    }

    // 环境变量创建与编辑弹窗
    if (isAddingEnvVar) {
        EnvironmentVariableEditDialog(
            hazeState = hazeState,
            variable = null,
            onDismiss = { isAddingEnvVar = false },
            onConfirm = { name, value, description ->
                coroutineScope.launch {
                    val variable = EnvironmentVariable(
                        name = name,
                        value = value,
                        description = description,
                        environment = "default",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.saveEnvironmentVariable(variable)
                    isAddingEnvVar = false
                    savedMessage = "已添加环境变量「$name」"
                }
            }
        )
    }

    envVarToEdit?.let { variable ->
        EnvironmentVariableEditDialog(
            hazeState = hazeState,
            variable = variable,
            onDismiss = { envVarToEdit = null },
            onConfirm = { name, value, description ->
                coroutineScope.launch {
                    repository.saveEnvironmentVariable(
                        variable.copy(
                            name = name,
                            value = value,
                            description = description,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    envVarToEdit = null
                    savedMessage = "已更新环境变量「$name」"
                }
            }
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
private fun PriorityRuleRow(
    badge: String,
    title: String,
    description: String,
    badgeColor: Color
) {
    Surface(
        shape = SettingsInnerShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PromptTemplateItemCard(
    template: PromptTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = echoGlassPalette()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsInnerShape,
        color = glass.control,
        border = BorderStroke(1.dp, glass.outline)
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
                        text = template.category.ifBlank { "general" },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
            if (!template.description.isNullOrBlank()) {
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = template.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PromptTemplateEditDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    template: PromptTemplate?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, content: String, description: String?, category: String) -> Unit
) {
    var name by remember { mutableStateOf(template?.name.orEmpty()) }
    var category by remember { mutableStateOf(template?.category.orEmpty().ifBlank { "general" }) }
    var description by remember { mutableStateOf(template?.description.orEmpty()) }
    var content by remember { mutableStateOf(template?.content.orEmpty()) }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "新建提示词模板" else "编辑提示词模板") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("模板名称") },
                    placeholder = { Text("例如: 代码重构专家 / 故事剧情续写") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("分类 (可选)") },
                    placeholder = { Text("例如: 创作 / 编程 / 翻译 / 效率") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("简介说明 (可选)") },
                    placeholder = { Text("简述模板用途与触发时机") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("模板正文 (支持 {{变量}} 占位符)") },
                    placeholder = { Text("输入提示词内容，使用 {{变量}} 作为插值占位符...") },
                    minLines = 4,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), content.trim(), description.trim().takeIf { it.isNotBlank() }, category.trim().ifBlank { "general" }) },
                enabled = name.isNotBlank() && content.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EnvironmentVariableItemCard(
    variable: EnvironmentVariable,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val glass = echoGlassPalette()
    var isRevealed by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsInnerShape,
        color = glass.control,
        border = BorderStroke(1.dp, glass.outline)
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
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "{{${variable.name}}}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { isRevealed = !isRevealed }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isRevealed) "隐藏" else "查看",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "删除", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
            if (!variable.description.isNullOrBlank()) {
                Text(
                    text = variable.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (isRevealed) variable.value else "••••••••••••",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EnvironmentVariableEditDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    variable: EnvironmentVariable?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, value: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf(variable?.name.orEmpty()) }
    var value by remember { mutableStateOf(variable?.value.orEmpty()) }
    var description by remember { mutableStateOf(variable?.description.orEmpty()) }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        title = { Text(if (variable == null) "添加环境变量" else "编辑环境变量") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("变量名 (KEY)") },
                    placeholder = { Text("例如: USER_NAME / PROJECT_NAME") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("变量值 (VALUE)") },
                    placeholder = { Text("实际注入的变量文本或密钥") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述说明 (可选)") },
                    placeholder = { Text("说明该变量在何处使用") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), value.trim(), description.trim().takeIf { it.isNotBlank() }) },
                enabled = name.isNotBlank() && value.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
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

        item {
            Spacer(modifier = Modifier.height(32.dp))
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
    var modelSearchQuery by remember { mutableStateOf("") }
    val filteredModels = remember(availableModels, modelSearchQuery) {
        if (modelSearchQuery.isBlank()) availableModels
        else availableModels.filter { it.contains(modelSearchQuery.trim(), ignoreCase = true) }
    }
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
                modifier = Modifier.heightIn(max = 500.dp)
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
                    val detectedKeys = remember(apiKey) {
                        AiRepository.parseApiKeys(apiKey)
                    }
                    SettingsInputField(
                        title = if (detectedKeys.size > 1) "API Key (已录入 ${detectedKeys.size} 个密钥 · 自动故障转移)" else "API Key",
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = "sk-...\n支持输入多个 Key（换行、分号或逗号分隔），第一个失败后自动使用下一个",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4
                    )
                    Text(
                        text = "💡 支持输入多个 Key（换行、分号或逗号隔开）。请求超时重连 3 次失败或连接报错时，将自动切换至下一个可用 Key 并透明重试。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            text = if (modelSearchQuery.isNotBlank()) "可用模型 (${filteredModels.size}/${availableModels.size})" else "可用模型列表 (共 ${availableModels.size} 个)",
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

                                OutlinedTextField(
                                    value = modelSearchQuery,
                                    onValueChange = { modelSearchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("搜索模型名称 (如: deepseek, gpt, claude)...", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    trailingIcon = if (modelSearchQuery.isNotBlank()) {
                                        {
                                            IconButton(onClick = { modelSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Clear, contentDescription = "清除搜索", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    } else null,
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                    )
                                )

                                if (modelSearchQuery.isNotBlank() && filteredModels.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextButton(
                                            onClick = { enabledModelNames = enabledModelNames + filteredModels.toSet() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("勾选搜出的 ${filteredModels.size} 个", style = MaterialTheme.typography.labelSmall)
                                        }
                                        TextButton(
                                            onClick = { enabledModelNames = enabledModelNames - filteredModels.toSet() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("取消搜出的 ${filteredModels.size} 个", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (filteredModels.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "未找到包含 “$modelSearchQuery” 的模型",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(filteredModels, key = { it }) { model ->
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
        val cap = remember(model) {
            com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(model)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cap.contextWindowDisplay.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = cap.contextWindowDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (cap.supportsVision || capability == "multimodal") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "视觉",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (cap.supportsTools) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "工具",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (cap.supportsReasoning) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "思考",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
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
