@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.aiassistant.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import com.aiassistant.domain.model.ChatModelOption
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
import androidx.compose.ui.draw.clipToBounds
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
    "输入框半透明液态毛玻璃效果完美还原，根除全黑/纯色覆盖，恢复通透背景模糊",
    "顶部悬浮工具栏与报错弹窗实现真实毛玻璃半透明透字效果，消除双层底色覆盖",
    "彻底修复模型思考胶囊文字无法显示问题，移除异常滚动裁剪，增加多级文案安全兜底",
    "根除对话页流式生成后幽灵气泡残留，完善分支生成生命周期自动回收与异常空消息过滤",
    "输入框放大同心圆弧手柄尺寸永久统一，划选复制工具栏解耦防抖彻底保持稳定",
    "辅助滑动 4 个按键保持柔和浅天蓝半透明体系与微光质感"
)

internal val V1927UserUpdates = listOf(
    "顶部悬浮工具栏与错误提示无边缘包裹且全屏穿透，文字半透明透出并实时液态毛玻璃模糊",
    "输入框放大弧线手柄大小彻底统一，拖拽前后永久固定为精致 22dp/17dp 贴角同心弧",
    "对话页辅助滑动 4 个按键重构为柔和浅天蓝半透明体系，搭配微光白边与透光质感",
    "文本划选弹窗与快照状态深度解耦，彻底修复高频闪烁死循环，复制与引用 100% 稳定响应",
    "大模型非标字体颜色与尾随星号容错解析，输入气泡呼吸感间距保留",
    "延续思考快速档纯正天蓝配色与新建会话记忆隔离规范"
)

internal val V1926UserUpdates = listOf(
    "顶部悬浮栏与错误弹窗直接复用输入框玻璃背景规范，消除悬浮栏与弹窗状态栏阴影异常穿透",
    "输入框右上角弧线控制手柄与边框精确同心贴合，拖拽微调更优雅",
    "全面兼容大模型非标颜色标签（如 <font color=\"2B7DEP\"> 与尾随星号）的精准渲染",
    "加宽用户输入气泡与上一条模型回复的纵向间距，提升长对话视觉呼吸感",
    "思考强度快速档重调为柔和纯正天蓝色，告别偏灰暗沉感",
    "对话页悬浮滚动快捷键升级为4键独立体系（到顶/上一条/下一条/到底），阶梯色彩与双线箭头",
    "文本长按选中弹窗防抖优化",
    "延续新建对话专属记忆默认关闭与长列表滚动条防断触优化"
)

internal val V1925UserUpdates = listOf(
    "对话流式响应时增加跟手跟随自动滚动",
    "修复对话顶部悬浮栏在部分机型上背景异常与玻璃穿透问题",
    "修复弹窗状态栏阴影未完整覆盖状态栏顶部边缘",
    "新建对话默认思考开启、联网关闭，专属记忆默认关闭",
    "新对话默认 API 选择入口移至「设置 - API 配置」顶部",
    "思考中与连接中状态文案支持在设置中自定义并一键重置",
    "长按文本工具栏增加剪切与粘贴，修复高频闪烁与空框问题",
    "修复点击中断后错误生成两条回复的问题",
    "对话中错误提示气泡支持折叠与双击展开/收起",
    "优化长列表滚动条滑动稳定性，避免快速拖动断触",
    "输入框展开按钮重构为优雅弧形控制手柄，支持拖拽随手调整高度"
)

internal val V1924UserUpdates = listOf(
    "修复弹窗状态栏阴影未完整覆盖状态栏顶部边缘问题",
    "修复顶部悬浮栏在部分机型上背景异常与玻璃穿透问题",
    "优化长列表滚动条滑动稳定性，避免快速拖动断触",
    "新建对话默认开启深度思考、关闭联网搜索、关闭会话专属记忆",
    "模型选择器支持按供应商分组和名称搜索"
)

internal val V1923UserUpdates = listOf(
    "修复弹窗状态栏阴影未完整覆盖状态栏顶部边缘",
    "会话专属记忆支持独立开关与范围隔离",
    "思考强度快速档位颜色调整",
    "液态玻璃组件优化"
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
    val glass = echoGlassPalette()
    val settingsBackgroundBitmap = remember(context) {
        BackgroundImageManager.getHomeBackgroundBitmap(context)
    }
    val hazeState = rememberEchoHazeState()
    var selectedSection by remember { mutableStateOf<String?>(null) }

    fun executeBack() {
        if (selectedSection != null) {
            selectedSection = null
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = selectedSection != null) {
        executeBack()
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
                                    "appearance" -> "界面与外观"
                                    "model_features" -> "模型辅助与思考"
                                    "prompts_memory" -> "提示词与记忆"
                                    "personalization" -> "界面与外观"
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
                            IconButton(onClick = { executeBack() }) {
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
                "appearance" -> AppearanceTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues),
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
                "model_features" -> ModelFeaturesTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues)
                )
                "prompts_memory" -> PromptsMemoryTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues)
                )
                "personalization" -> AppearanceTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues),
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
                "web_search" -> WebSearchTab(
                    hazeState = hazeState,
                    modifier = Modifier.padding(paddingValues)
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
                subtitle = "管理AI模型API密钥和服务商配置",
                onClick = { onSectionSelected("api_config") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Palette,
                title = "界面与外观",
                subtitle = "主题模式、用户头像、字体大小与壁纸设置",
                onClick = { onSectionSelected("appearance") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.Psychology,
                title = "模型辅助与思考",
                subtitle = "全模型自由直选自动命名、思考翻译与思考胶囊",
                onClick = { onSectionSelected("model_features") }
            )
        }
        item {
            SettingsMenuItem(
                hazeState = hazeState,
                icon = Icons.Default.AutoAwesome,
                title = "提示词与记忆",
                subtitle = "全局提示词、创作规范、关于我画像与长记忆",
                onClick = { onSectionSelected("prompts_memory") }
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
                subtitle = "备份和恢复应用数据与角色设定",
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
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
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
            val defaultConfig = configs.firstOrNull { it.isDefault } ?: configs.firstOrNull()
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("新对话默认 API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (defaultConfig != null) "当前默认：${defaultConfig.name} (${defaultConfig.provider})" else "尚未设置默认 API 配置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (configs.isNotEmpty()) {
                    Text(
                        "点击直接切换新对话默认生效的服务商：",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        configs.forEach { cfg ->
                            FilterChip(
                                selected = cfg.isDefault,
                                onClick = {
                                    scope.launch {
                                        repository.setDefaultConfig(cfg.id)
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (cfg.isDefault) "${cfg.name} (默认)" else cfg.name,
                                        fontWeight = if (cfg.isDefault) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (cfg.isDefault) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = echoFilterChipColors(),
                                border = echoFilterChipBorder(cfg.isDefault)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "API配置管理",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
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
    modifier: Modifier = Modifier
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

    fun syncAllSettings() {
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
        tavilyManager.saveSettings(newTavily)
        tavilySettings = tavilyManager.getSettings()
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
                            onClick = {
                                searchEngine = engine
                                toolHub.setSearchEngine(engine)
                            },
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
                        onValueChange = {
                            exaApiKey = it
                            toolHub.setExaApiKey(it)
                        },
                        placeholder = "留空则使用官方免Key通道"
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("启用 Tavily 搜索", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = tavilyEnabled,
                            onCheckedChange = {
                                tavilyEnabled = it
                                val newTavily = tavilySettings.copy(enabled = it)
                                tavilyManager.saveSettings(newTavily)
                                tavilySettings = tavilyManager.getSettings()
                            }
                        )
                    }

                    SettingsInputField(
                        title = "Tavily API Key",
                        value = tavilyApiKey,
                        onValueChange = {
                            tavilyApiKey = it
                            val newTavily = tavilySettings.copy(apiKey = it)
                            tavilyManager.saveSettings(newTavily)
                            tavilySettings = tavilyManager.getSettings()
                        },
                        placeholder = "tvly-..."
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("basic", "advanced").forEach { depth ->
                            val selected = searchDepth == depth
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    searchDepth = depth
                                    val newTavily = tavilySettings.copy(searchDepth = depth)
                                    tavilyManager.saveSettings(newTavily)
                                    tavilySettings = tavilyManager.getSettings()
                                },
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
                        onValueChange = { v ->
                            val cleaned = v.filter { it.isDigit() }.take(2)
                            maxResults = cleaned
                            val newTavily = tavilySettings.copy(maxResults = cleaned.toIntOrNull()?.coerceIn(1, 20) ?: 8)
                            tavilyManager.saveSettings(newTavily)
                            tavilySettings = tavilyManager.getSettings()
                        },
                        placeholder = "8",
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
                            onCheckedChange = {
                                includeAnswer = it
                                val newTavily = tavilySettings.copy(includeAnswer = it)
                                tavilyManager.saveSettings(newTavily)
                                tavilySettings = tavilyManager.getSettings()
                            }
                        )
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
                                onClick = {
                                    searchResultCount = count
                                    toolHub.setSearchResultCount(count)
                                },
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
                                    toolHub.setSearchResultCount(count)
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
                    Switch(
                        checked = deviceToolsEnabled,
                        onCheckedChange = {
                            deviceToolsEnabled = it
                            toolHub.setDeviceToolsEnabled(it)
                        }
                    )
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
                            onValueChange = {
                                manualCity = it
                                toolHub.locationAddressManager.setManualCity(it)
                            },
                            placeholder = "例如：深圳 / 北京 / 上海"
                        )
                    }

                    // 健康与运动数据 (华为运动健康与硬件计步)
                    val isHealthInstalled = remember { toolHub.healthDataManager.isHuaweiHealthInstalled() }
                    val healthAppName = remember { toolHub.healthDataManager.getHealthAppName() }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 标题栏与安装状态徽标
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DirectionsWalk,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "运动健康与设备体征",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isHealthInstalled) Color(0xFF2ECC71).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isHealthInstalled) Color(0xFF2ECC71).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = if (isHealthInstalled) "已安装 $healthAppName" else "未检测到健康App",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = if (isHealthInstalled) Color(0xFF27AE60) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 3列体征高对比指标卡
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 步数
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("今日步数", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "${currentHealthSummary.todaySteps}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text("步", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                    }
                                }

                                // 心率
                                val hrVal = currentHealthSummary.heartRate
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE74C3C).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFFE74C3C).copy(alpha = 0.22f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("静息心率", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            if (hrVal > 0) "$hrVal" else "--",
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (hrVal > 0) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(if (hrVal > 0) "bpm" else "未录入", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // 睡眠
                                val sleepMins = currentHealthSummary.sleepMinutes
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF9B59B6).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFF9B59B6).copy(alpha = 0.22f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("昨晚睡眠", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            if (sleepMins > 0) "${sleepMins / 60}h${sleepMins % 60}m" else "--",
                                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (sleepMins > 0) Color(0xFF9B59B6) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(if (sleepMins > 0 && currentHealthSummary.sleepScore > 0) "评分 ${currentHealthSummary.sleepScore}" else if (sleepMins > 0) "已记录" else "未录入", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // 状态详情与更新时间 (使用优雅格式化时间)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "传感器: ${toolHub.healthDataManager.getSensorStatusText()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "同步: ${toolHub.healthDataManager.getFormattedLastUpdateTime()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // 操作按钮栏 (清晰分行，响应式三等分)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
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
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("刷新步数", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = { showHuaweiHealthSyncDialog = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("数据校准", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!toolHub.healthDataManager.openHuaweiHealthApp(context)) {
                                            savedMessage = "未检测到已安装的华为运动健康应用"
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("打开健康App", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
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
                        onValueChange = {
                            jinaApiKey = it
                            toolHub.setJinaApiKey(it)
                        },
                        placeholder = "留空使用免费通道，填入可避免数据中心限制"
                    )
                }
            }
        }

        // 自动存盘提示
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = com.aiassistant.ui.theme.EchoTokens.Radius.shapeMd,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "所有联网搜索与工具配置修改后均已实时自动存盘生效，无需手动保存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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
            val healthDataManager = remember { AiAssistantApp.instance.echoToolHub.healthDataManager }
            val isInstalled = remember { healthDataManager.isHuaweiHealthInstalled() }
            val appName = remember { healthDataManager.getHealthAppName() }
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (screenHeight * 0.72f).dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "机制说明：受 Android 系统安全沙箱保护，三方应用无法直接跨应用暗中读取华为运动健康私有数据。当前步数由本机硬件计步传感器自动累加；若需将手环/手表记录的心率与睡眠同步给 AI，可点击下方打开健康 APP 对照填入。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedButton(
                    onClick = {
                        healthDataManager.openHuaweiHealthApp(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isInstalled) "打开 $appName 查看数据" else "打开华为运动健康 APP 查看数据")
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

data class FullModelChoice(
    val configId: Long,
    val configName: String,
    val provider: String,
    val modelName: String,
    val isDefault: Boolean = false
)

@Composable
fun UniversalModelPickerCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    title: String,
    subtitle: String,
    selectedConfigId: Long,
    selectedModel: String,
    allConfigs: List<ApiConfig>,
    onSelect: (configId: Long, model: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var manualInputMode by remember { mutableStateOf(false) }
    var manualModelName by remember { mutableStateOf(selectedModel) }
    var manualConfigId by remember { mutableLongStateOf(if (selectedConfigId > 0L) selectedConfigId else allConfigs.firstOrNull()?.id ?: 0L) }
    val repository = AiAssistantApp.instance.repository
    val glass = echoGlassPalette()

    // 读取与聊天对话页完全一致的全部已启用模型列表 (跨服务商)
    val visibleChatOptions by produceState<List<ChatModelOption>>(initialValue = emptyList(), allConfigs) {
        value = repository.getAllVisibleChatModelOptions()
    }

    val currentConfig = remember(selectedConfigId, allConfigs) {
        if (selectedConfigId > 0L) allConfigs.find { it.id == selectedConfigId }
        else allConfigs.find { it.isDefault } ?: allConfigs.firstOrNull()
    }

    val displayTitle = remember(selectedConfigId, selectedModel, currentConfig) {
        if (selectedConfigId == 0L && selectedModel.isBlank()) {
            "跟随当前会话模型（自动继承）"
        } else if (selectedModel.isNotBlank()) {
            selectedModel
        } else {
            currentConfig?.modelName?.ifBlank { "默认模型" } ?: "未指定模型"
        }
    }

    val displayBadge = remember(selectedConfigId, selectedModel, currentConfig) {
        if (selectedConfigId == 0L && selectedModel.isBlank()) {
            "默认推荐"
        } else {
            currentConfig?.let { it.name.ifBlank { it.provider } } ?: "未绑定"
        }
    }

    val allChoices = remember(allConfigs, visibleChatOptions) {
        val list = mutableListOf<FullModelChoice>()
        // 1. 优先加入所有在 API 配置中启用的模型 (和对话页完全一致)
        visibleChatOptions.forEach { opt ->
            list.add(
                FullModelChoice(
                    configId = opt.apiConfigId,
                    configName = opt.configName.ifBlank { opt.provider },
                    provider = opt.provider,
                    modelName = opt.modelName,
                    isDefault = false
                )
            )
        }
        // 2. 补充服务商默认模型和 availableModels 中尚未添加的模型
        allConfigs.forEach { cfg ->
            val cfgName = cfg.name.ifBlank { cfg.provider }
            val defaultModel = cleanModelName(cfg.modelName)
            if (!defaultModel.isNullOrBlank() && list.none { it.configId == cfg.id && it.modelName == defaultModel }) {
                list.add(FullModelChoice(cfg.id, cfgName, cfg.provider, defaultModel, isDefault = true))
            }
            val parsed = parseModelList(cfg.availableModels)
            parsed.forEach { m ->
                if (m.isNotBlank() && list.none { it.configId == cfg.id && it.modelName == m }) {
                    list.add(FullModelChoice(cfg.id, cfgName, cfg.provider, m, isDefault = false))
                }
            }
        }
        list.distinctBy { "${it.configId}:${it.modelName}" }
    }

    val filteredChoices = remember(allChoices, searchQuery) {
        if (searchQuery.isBlank()) allChoices
        else allChoices.filter {
            it.modelName.contains(searchQuery.trim(), ignoreCase = true) ||
            it.configName.contains(searchQuery.trim(), ignoreCase = true) ||
            it.provider.contains(searchQuery.trim(), ignoreCase = true)
        }
    }

    val groupedChoices = remember(filteredChoices) {
        filteredChoices.groupBy { "${it.configName} (${it.provider})" }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsInnerShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, glass.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = displayBadge,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 主下拉按键：向下展开
            OutlinedButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsInnerShape,
                border = BorderStroke(
                    1.dp,
                    if (isExpanded) MaterialTheme.colorScheme.primary else glass.outline
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起模型列表" else "展开模型列表",
                        tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 向下直接展开列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = glass.control,
                    border = BorderStroke(1.dp, glass.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (manualInputMode) {
                            // 手动输入模式
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("自定义手动输入模型名称：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = manualModelName,
                                    onValueChange = { manualModelName = it },
                                    placeholder = { Text("例如：gpt-4o-mini 或 deepseek-chat", fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Text("绑定服务商：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                allConfigs.forEach { cfg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { manualConfigId = cfg.id }
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = manualConfigId == cfg.id,
                                            onClick = { manualConfigId = cfg.id }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${cfg.name.ifBlank { cfg.provider }} (${cfg.provider})",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { manualInputMode = false }) {
                                        Text("返回列表", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = {
                                            if (manualModelName.isNotBlank()) {
                                                onSelect(manualConfigId, manualModelName.trim())
                                                isExpanded = false
                                                manualInputMode = false
                                            }
                                        },
                                        enabled = manualModelName.isNotBlank(),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                    ) {
                                        Text("确认选择", fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            // 搜索栏 (高度 40dp，匀称美观)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, glass.outline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    "搜索模型名称或服务商...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { searchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "清除",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 滚动模型列表
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // 1. 跟随当前会话模型
                                item {
                                    val isFollowSelected = selectedConfigId == 0L && selectedModel.isBlank()
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onSelect(0L, "")
                                                isExpanded = false
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isFollowSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        border = if (isFollowSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, glass.outline.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "跟随当前会话模型（自动继承）",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isFollowSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isFollowSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "按每个会话各自绑定的模型与配置自动调用",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isFollowSelected) {
                                                Icon(Icons.Default.Check, contentDescription = "已选择", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                // 2. 分组显示所有服务商与模型
                                groupedChoices.forEach { (groupTitle, choices) ->
                                    item {
                                        Text(
                                            text = groupTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                                        )
                                    }
                                    items(choices) { choice ->
                                        val isSelected = selectedConfigId == choice.configId &&
                                            (selectedModel == choice.modelName || (selectedModel.isBlank() && choice.isDefault))
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onSelect(choice.configId, choice.modelName)
                                                    isExpanded = false
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = choice.modelName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (choice.isDefault) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                            ) {
                                                                Text(
                                                                    "默认",
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                    color = MaterialTheme.colorScheme.secondary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "已选择",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (allChoices.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                            Text("未检测到已配置的服务商模型，请先在「API配置」中添加", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // 底部操作区
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { manualInputMode = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("自定义输入模型", style = MaterialTheme.typography.labelSmall)
                                }

                                TextButton(
                                    onClick = { isExpanded = false },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("收起", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = AiAssistantApp.instance.personalizationManager

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
    var chatFontSize by remember(settings) { mutableIntStateOf(settings.chatFontSize) }
    var fontSizeScale by remember(settings) { mutableFloatStateOf(settings.fontSizeScale) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    fun performSave() {
        manager.saveSettings(
            settings.copy(
                chatFontSize = chatFontSize,
                fontSizeScale = fontSizeScale
            )
        )
        settings = manager.getSettings()
        savedMessage = "已保存界面与外观设定"
    }

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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBase64 != null) {
                            val bitmap = remember(avatarBase64) { AvatarManager.base64ToBitmap(avatarBase64) }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "用户头像",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
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
                        },
                        onValueChangeFinished = {
                            performSave()
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
                                    manager.saveSettings(settings.copy(chatFontSize = chatFontSize, fontSizeScale = scale))
                                    settings = manager.getSettings()
                                    savedMessage = "已保存界面字体缩放"
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

        // 3. 界面背景设置
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
                            "可为首页和对话页选择内置低饱和护眼纯色或自定义相册图片背景。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 内置低饱和度纯色背景选择区
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "内置极低饱和纯色背景（护眼高可读性）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "精选极低饱和度（<5%）淡雅纯色，完全不影响文字清晰度与对比度（符合 WCAG AAA）。轻触选择并一键应用：",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var selectedColorPreset by remember { mutableStateOf<BackgroundImageManager.SolidColorPreset?>(null) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BackgroundImageManager.PRESET_SOLID_COLORS.forEach { preset ->
                            val isDark = themeMode == AppThemeMode.Dark || (themeMode == AppThemeMode.System && androidx.compose.foundation.isSystemInDarkTheme())
                            val displayColorInt = if (isDark) preset.darkColorInt else preset.lightColorInt
                            val isSelected = selectedColorPreset?.id == preset.id

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedColorPreset = if (isSelected) null else preset
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(displayColorInt))
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 2.5.dp else 1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    preset.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    selectedColorPreset?.let { preset ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsInnerShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "应用「${preset.name}」到：",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isDark = themeMode == AppThemeMode.Dark || (themeMode == AppThemeMode.System && androidx.compose.foundation.isSystemInDarkTheme())
                                    val targetColorInt = if (isDark) preset.darkColorInt else preset.lightColorInt

                                    FilledTonalButton(
                                        onClick = {
                                            BackgroundImageManager.saveHomeBackgroundSolidColor(context, targetColorInt)
                                            backgroundRevision++
                                            savedMessage = "已将「${preset.name}」应用为首页背景"
                                            selectedColorPreset = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("设为首页", style = MaterialTheme.typography.labelSmall)
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            BackgroundImageManager.saveChatBackgroundSolidColor(context, targetColorInt)
                                            backgroundRevision++
                                            savedMessage = "已将「${preset.name}」应用为对话页背景"
                                            selectedColorPreset = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("设为对话页", style = MaterialTheme.typography.labelSmall)
                                    }

                                    Button(
                                        onClick = {
                                            BackgroundImageManager.saveHomeBackgroundSolidColor(context, targetColorInt)
                                            BackgroundImageManager.saveChatBackgroundSolidColor(context, targetColorInt)
                                            backgroundRevision++
                                            savedMessage = "已将「${preset.name}」应用为全局背景"
                                            selectedColorPreset = null
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                    ) {
                                        Text("全部应用", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // 自定义相册背景图片
                Text("自定义相册图片背景", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

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

        // 保存反馈消息
        savedMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModelFeaturesTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val manager = AiAssistantApp.instance.personalizationManager
    val repository = AiAssistantApp.instance.repository
    val coroutineScope = rememberCoroutineScope()

    var settings by remember { mutableStateOf(manager.getSettings()) }
    var autoNameEnabled by remember(settings) { mutableStateOf(settings.autoNameEnabled) }
    var autoNameApiConfigId by remember(settings) { mutableLongStateOf(settings.autoNameApiConfigId) }
    var autoNameModel by remember(settings) { mutableStateOf(settings.autoNameModel) }
    var autoNamePrompt by remember(settings) { mutableStateOf(settings.autoNamePrompt) }

    var enableThinkingTranslation by remember(settings) { mutableStateOf(settings.enableThinkingTranslation) }
    var thinkingTranslationApiConfigId by remember(settings) { mutableLongStateOf(settings.thinkingTranslationApiConfigId) }
    var thinkingTranslationModel by remember(settings) { mutableStateOf(settings.thinkingTranslationModel) }

    var thinkingTemplate by remember(settings) { mutableStateOf(settings.thinkingCapsuleTemplate) }

    val allApiConfigs by repository.getAllApiConfigs().collectAsState(initial = emptyList())

    var connectingTemplate by remember(settings) { mutableStateOf(settings.connectingTextTemplate) }
    var thinkingTextTemplate by remember(settings) { mutableStateOf(settings.thinkingTextTemplate) }

    var savedMessage by remember { mutableStateOf<String?>(null) }

    fun persistSettings(
        newAutoNameEnabled: Boolean = autoNameEnabled,
        newAutoNameConfigId: Long = autoNameApiConfigId,
        newAutoNameModel: String = autoNameModel,
        newAutoNamePrompt: String = autoNamePrompt,
        newEnableThinkingTranslation: Boolean = enableThinkingTranslation,
        newThinkingConfigId: Long = thinkingTranslationApiConfigId,
        newThinkingModel: String = thinkingTranslationModel,
        newThinkingTemplate: String = thinkingTemplate,
        newConnectingTemplate: String = connectingTemplate,
        newThinkingTextTemplate: String = thinkingTextTemplate
    ) {
        manager.saveSettings(
            settings.copy(
                autoNameEnabled = newAutoNameEnabled,
                autoNameApiConfigId = newAutoNameConfigId,
                autoNameModel = newAutoNameModel.trim(),
                autoNamePrompt = newAutoNamePrompt.trim(),
                enableThinkingTranslation = newEnableThinkingTranslation,
                thinkingTranslationApiConfigId = newThinkingConfigId,
                thinkingTranslationModel = newThinkingModel.trim(),
                thinkingCapsuleTemplate = newThinkingTemplate.trim().ifBlank { "{model} {status} {time} {tokens}" },
                connectingTextTemplate = newConnectingTemplate.trim().ifBlank { "{model} 正在连接中..." },
                thinkingTextTemplate = newThinkingTextTemplate.trim().ifBlank { "{model} 正在思考中..." }
            )
        )
        settings = manager.getSettings()
    }

    LaunchedEffect(autoNamePrompt) {
        if (autoNamePrompt.trim() != settings.autoNamePrompt.trim()) {
            kotlinx.coroutines.delay(400)
            persistSettings(newAutoNamePrompt = autoNamePrompt)
        }
    }

    LaunchedEffect(thinkingTemplate) {
        if (thinkingTemplate.trim() != settings.thinkingCapsuleTemplate.trim()) {
            kotlinx.coroutines.delay(400)
            persistSettings(newThinkingTemplate = thinkingTemplate)
        }
    }

    LaunchedEffect(connectingTemplate) {
        if (connectingTemplate.trim() != settings.connectingTextTemplate.trim()) {
            kotlinx.coroutines.delay(400)
            persistSettings(newConnectingTemplate = connectingTemplate)
        }
    }

    LaunchedEffect(thinkingTextTemplate) {
        if (thinkingTextTemplate.trim() != settings.thinkingTextTemplate.trim()) {
            kotlinx.coroutines.delay(400)
            persistSettings(newThinkingTextTemplate = thinkingTextTemplate)
        }
    }

    val glass = echoGlassPalette()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 对话智能自动命名模型 (自由选择所有模型)
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
                            "新对话首轮交互后自动生成简短精炼标题，可直接自由选择所有模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoNameEnabled,
                        onCheckedChange = {
                            autoNameEnabled = it
                            persistSettings(newAutoNameEnabled = it)
                            savedMessage = if (it) "已开启对话自动命名" else "已关闭对话自动命名"
                        }
                    )
                }

                if (autoNameEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 自由直选所有模型
                    UniversalModelPickerCard(
                        hazeState = hazeState,
                        title = "指定命名专用模型",
                        subtitle = "直接跨服务商自由选择所有模型，无需先切换服务商",
                        selectedConfigId = autoNameApiConfigId,
                        selectedModel = autoNameModel,
                        allConfigs = allApiConfigs,
                        onSelect = { cfgId, model ->
                            autoNameApiConfigId = cfgId
                            autoNameModel = model
                            persistSettings(newAutoNameConfigId = cfgId, newAutoNameModel = model)
                            savedMessage = "已更新自动命名模型"
                        }
                    )

                    // 自定义提示词
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "自定义命名提示词（可选）：",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = autoNamePrompt,
                            onValueChange = {
                                autoNamePrompt = it
                                savedMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp),
                            placeholder = { Text("留空将使用默认精炼命名提示词...") },
                            minLines = 2,
                            maxLines = 6,
                            shape = SettingsInnerShape
                        )
                    }
                }
            }
        }

        // 2. 深度思考链语言翻译 (自由选择所有模型)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("思考链语言翻译", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "模型输出英文/多语言思考过程时，在消息上方提供「翻译」按钮进行快速中文译制",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = enableThinkingTranslation,
                        onCheckedChange = {
                            enableThinkingTranslation = it
                            persistSettings(newEnableThinkingTranslation = it)
                            savedMessage = if (it) "已开启思考链翻译" else "已关闭思考链翻译"
                        }
                    )
                }

                if (enableThinkingTranslation) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 自由直选所有模型
                    UniversalModelPickerCard(
                        hazeState = hazeState,
                        title = "翻译专用模型",
                        subtitle = "直接跨服务商自由选择所有模型，无需先切换服务商",
                        selectedConfigId = thinkingTranslationApiConfigId,
                        selectedModel = thinkingTranslationModel,
                        allConfigs = allApiConfigs,
                        onSelect = { cfgId, model ->
                            thinkingTranslationApiConfigId = cfgId
                            thinkingTranslationModel = model
                            persistSettings(newThinkingConfigId = cfgId, newThinkingModel = model)
                            savedMessage = "已更新思考链翻译模型"
                        }
                    )
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
                                    persistSettings(newThinkingTemplate = tpl)
                                    savedMessage = "已应用胶囊模板"
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
                                        val newTpl = if (thinkingTemplate.isBlank()) varKey else "$thinkingTemplate $varKey"
                                        thinkingTemplate = newTpl
                                        persistSettings(newThinkingTemplate = newTpl)
                                        savedMessage = "已插入 $varKey"
                                    }
                                },
                                label = { Text(varKey, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        // 4. 生成状态文案自定义 (连接中 & 思考中)
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Pending,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("生成状态文案自定义", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "自定义模型正在连接与深度思考时显示的提示文案，支持 {model} 变量占位",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            connectingTemplate = "{model} 正在连接中..."
                            thinkingTextTemplate = "{model} 正在思考中..."
                            persistSettings(
                                newConnectingTemplate = "{model} 正在连接中...",
                                newThinkingTextTemplate = "{model} 正在思考中..."
                            )
                            savedMessage = "已重置生成文案为默认值"
                        }
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "重置为默认值",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                SettingsInputField(
                    title = "连接中文案（支持 {model}）",
                    value = connectingTemplate,
                    onValueChange = {
                        connectingTemplate = it
                        savedMessage = null
                    },
                    placeholder = "{model} 正在连接中..."
                )

                SettingsInputField(
                    title = "思考中文案（支持 {model}）",
                    value = thinkingTextTemplate,
                    onValueChange = {
                        thinkingTextTemplate = it
                        savedMessage = null
                    },
                    placeholder = "{model} 正在思考中..."
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            connectingTemplate = "{model} 正在连接中..."
                            thinkingTextTemplate = "{model} 正在思考中..."
                            persistSettings(
                                newConnectingTemplate = "{model} 正在连接中...",
                                newThinkingTextTemplate = "{model} 正在思考中..."
                            )
                            savedMessage = "已恢复默认文案"
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重置为默认值")
                    }
                }
            }
        }

        // 保存反馈消息
        savedMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PromptsMemoryTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier
) {
    val manager = AiAssistantApp.instance.personalizationManager
    val repository = AiAssistantApp.instance.repository
    val coroutineScope = rememberCoroutineScope()

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

    var savedMessage by remember { mutableStateOf<String?>(null) }

    fun persistPromptSettings(
        newGlobalPrompt: String = globalPrompt,
        newGlobalRoleplayPrompt: String = globalRoleplayPrompt,
        newInstruction: String = instruction,
        newAutoMemoryEnabled: Boolean = autoMemoryEnabled
    ) {
        manager.saveSettings(
            settings.copy(
                globalSystemPrompt = newGlobalPrompt.trim(),
                globalRoleplayPrompt = newGlobalRoleplayPrompt.trim(),
                aboutUser = newInstruction.trim(),
                responseStyle = "",
                preferences = "",
                avoid = "",
                autoMemoryEnabled = newAutoMemoryEnabled
            )
        )
        settings = manager.getSettings()
    }

    LaunchedEffect(globalPrompt) {
        if (globalPrompt.trim() != settings.globalSystemPrompt.trim()) {
            kotlinx.coroutines.delay(400)
            persistPromptSettings(newGlobalPrompt = globalPrompt)
        }
    }

    LaunchedEffect(globalRoleplayPrompt) {
        if (globalRoleplayPrompt.trim() != settings.globalRoleplayPrompt.trim()) {
            kotlinx.coroutines.delay(400)
            persistPromptSettings(newGlobalRoleplayPrompt = globalRoleplayPrompt)
        }
    }

    LaunchedEffect(instruction) {
        if (instruction.trim() != settings.aboutUser.trim()) {
            kotlinx.coroutines.delay(400)
            persistPromptSettings(newInstruction = instruction)
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

    // 提示词模板状态
    var isTemplatesExpanded by remember { mutableStateOf(false) }
    var templateSearchQuery by remember { mutableStateOf("") }
    val allTemplates by repository.getAllPromptTemplates().collectAsState(initial = emptyList())
    val filteredTemplates = remember(allTemplates, templateSearchQuery) {
        if (templateSearchQuery.isBlank()) allTemplates
        else allTemplates.filter { it.name.contains(templateSearchQuery.trim(), ignoreCase = true) || it.content.contains(templateSearchQuery.trim(), ignoreCase = true) }
    }
    var templateToEdit by remember { mutableStateOf<PromptTemplate?>(null) }
    var isAddingTemplate by remember { mutableStateOf(false) }

    // 环境变量状态
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
        // 1. 提示词与记忆生效机制与优先级说明
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

                AnimatedVisibility(
                    visible = showPriorityDetails,
                    enter = expandVertically(
                        animationSpec = androidx.compose.animation.core.tween(220),
                        expandFrom = Alignment.Top
                    ) + fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(180)
                    ),
                    exit = shrinkVertically(
                        animationSpec = androidx.compose.animation.core.tween(200),
                        shrinkTowards = Alignment.Top
                    ) + fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(150)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
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

        // 2. 全局系统提示词模块
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
                    TextButton(
                        onClick = {
                            val defaultPrompt = "你是一个专业、严谨、有深度思考能力的 AI 助手。回答问题时逻辑清晰、论证充分，遇到专业问题主动给出高质量的代码或技术解释，语言自然流畅。"
                            globalPrompt = defaultPrompt
                            persistPromptSettings(newGlobalPrompt = defaultPrompt)
                            savedMessage = "已填入并保存默认提示词"
                        }
                    ) {
                        Text("填入默认预设", style = MaterialTheme.typography.labelSmall)
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
                    placeholder = { Text("例如：你是一个专业、富有同理心的全能AI助手...") },
                    minLines = 4,
                    maxLines = 14,
                    shape = SettingsInnerShape
                )
            }
        }

        // 3. 故事创作与角色扮演全局教学指引
        item {
            SettingsGlassCard(hazeState = hazeState) {
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
                            val defaultGuidelines = com.aiassistant.data.repository.RoleplayRepository.DEFAULT_FICTION_TEACHING_GUIDELINES
                            globalRoleplayPrompt = defaultGuidelines
                            persistPromptSettings(newGlobalRoleplayPrompt = defaultGuidelines)
                            savedMessage = "已填入并保存创作规范"
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

        // 4. 自定义偏好与关于我画像
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
                        Text("“关于我”与自定义偏好", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "模型在所有普通对话中都会参考这些背景信息，让回答更贴合您的喜好与需求。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PersonalizationTextField(
                    title = "用户画像与偏好指令",
                    value = instruction,
                    placeholder = "例如：\n- 我是一名全栈工程师，主要使用 Kotlin 和 Python\n- 回答请直接切入重点，少说客套话\n- 代码请附带关键行注释",
                    onValueChange = {
                        instruction = it
                        savedMessage = null
                    }
                )
            }
        }

        // 5. 跨会话长期记忆
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
                        Text("跨会话长期记忆", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "AI 会在日常对话中识别重要信息，弹出确认条由您决定是否存入记忆库，跨对话持续生效。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = autoMemoryEnabled,
                        onCheckedChange = {
                            autoMemoryEnabled = it
                            persistPromptSettings(newAutoMemoryEnabled = it)
                            savedMessage = if (it) "已开启跨会话长期记忆" else "已关闭跨会话长期记忆"
                        }
                    )
                }

                if (autoMemoryEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .echoShapeClick(SettingsInnerShape) { isMemoriesExpanded = !isMemoriesExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "记忆库管理",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${allMemories.size} 条",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isAddingMemory = true }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "添加记忆", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { isMemoriesExpanded = !isMemoriesExpanded }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (isMemoriesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isMemoriesExpanded) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = memorySearchQuery,
                                onValueChange = { memorySearchQuery = it },
                                placeholder = { Text("搜索记忆内容或关键词...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (memorySearchQuery.isNotBlank()) {
                                        IconButton(onClick = { memorySearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(999.dp)
                            )

                            if (allMemories.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = SettingsInnerShape,
                                    color = glass.control
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "暂无记忆条目\n当与 AI 对话提及个人习惯或点击右上角「+」时将在此处列出。",
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
                                                    repository.saveMemory(memory.copy(isEnabled = enabled, updatedAt = System.currentTimeMillis()))
                                                }
                                            },
                                            onEdit = { memoryToEdit = memory },
                                            onDelete = {
                                                coroutineScope.launch {
                                                    repository.deleteMemory(memory)
                                                }
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { showClearAllConfirm = true }) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("清空所有记忆", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. 提示词模板系统
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("提示词模板工作流", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "预设结构化指令与 {{变量}} 占位符，支持聊天输入框快速调用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(SettingsInnerShape) { isTemplatesExpanded = !isTemplatesExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模板库列表", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Text("${allTemplates.size} 个", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isAddingTemplate = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "新建模板", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isTemplatesExpanded = !isTemplatesExpanded }, modifier = Modifier.size(32.dp)) {
                            Icon(if (isTemplatesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                AnimatedVisibility(visible = isTemplatesExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = templateSearchQuery,
                            onValueChange = { templateSearchQuery = it },
                            placeholder = { Text("搜索模板名称或内容...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (templateSearchQuery.isNotBlank()) {
                                    IconButton(onClick = { templateSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "清除", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp)
                        )

                        if (filteredTemplates.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = SettingsInnerShape,
                                color = glass.control
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "暂无匹配的提示词模板\n点击右上角「+」新建专属的高效生产力模板。",
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
                                filteredTemplates.forEach { tpl ->
                                    PromptTemplateItemCard(
                                        template = tpl,
                                        onEdit = { templateToEdit = tpl },
                                        onDelete = {
                                            coroutineScope.launch {
                                                repository.deletePromptTemplate(tpl)
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

        // 7. 环境变量与安全密钥注入
        item {
            SettingsGlassCard(hazeState = hazeState) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DataObject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("提示词环境变量注入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "配置自定义环境变量（如 {{PROJECT_NAME}}），发送时自动解密并动态替换",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .echoShapeClick(SettingsInnerShape) { isEnvVarsExpanded = !isEnvVarsExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("环境变量列表", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)) {
                            Text("${allEnvVars.size} 个", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isAddingEnvVar = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "添加变量", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { isEnvVarsExpanded = !isEnvVarsExpanded }, modifier = Modifier.size(32.dp)) {
                            Icon(if (isEnvVarsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                AnimatedVisibility(visible = isEnvVarsExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (allEnvVars.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = SettingsInnerShape,
                                color = glass.control
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
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

        // 保存反馈消息
        savedMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    // 记忆编辑弹窗
    if (isAddingMemory || memoryToEdit != null) {
        MemoryEditDialog(
            hazeState = hazeState,
            memory = memoryToEdit,
            onDismiss = {
                isAddingMemory = false
                memoryToEdit = null
            },
            onConfirm = { content, scope, keywords ->
                coroutineScope.launch {
                    val target = memoryToEdit?.copy(
                        content = content,
                        scope = scope,
                        keywords = keywords,
                        updatedAt = System.currentTimeMillis()
                    ) ?: MemoryItem(
                        content = content,
                        scope = scope,
                        keywords = keywords
                    )
                    repository.saveMemory(target)
                    isAddingMemory = false
                    memoryToEdit = null
                    savedMessage = "记忆已保存"
                }
            }
        )
    }

    // 清空记忆确认弹窗
    if (showClearAllConfirm) {
        EchoGlassDialog(
            hazeState = hazeState,
            title = { Text("清空所有记忆") },
            text = { Text("确定要清空全部长期记忆吗？此操作无法撤销。") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.clearAllMemories()
                            showClearAllConfirm = false
                            savedMessage = "已清空全部记忆"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("清空")
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

    // 提示词模板新建/编辑弹窗
    if (isAddingTemplate || templateToEdit != null) {
        PromptTemplateEditDialog(
            hazeState = hazeState,
            template = templateToEdit,
            onDismiss = {
                isAddingTemplate = false
                templateToEdit = null
            },
            onConfirm = { name, content, description, category ->
                coroutineScope.launch {
                    val target = templateToEdit?.copy(
                        name = name,
                        content = content,
                        description = description,
                        category = category,
                        updatedAt = System.currentTimeMillis()
                    ) ?: PromptTemplate(
                        name = name,
                        content = content,
                        description = description,
                        category = category
                    )
                    repository.savePromptTemplate(target)
                    isAddingTemplate = false
                    templateToEdit = null
                    savedMessage = "模板已保存"
                }
            }
        )
    }

    // 环境变量新建/编辑弹窗
    if (isAddingEnvVar || envVarToEdit != null) {
        EnvironmentVariableEditDialog(
            hazeState = hazeState,
            variable = envVarToEdit,
            onDismiss = {
                isAddingEnvVar = false
                envVarToEdit = null
            },
            onConfirm = { name, value, description ->
                coroutineScope.launch {
                    val target = envVarToEdit?.copy(
                        name = name,
                        value = value,
                        description = description,
                        updatedAt = System.currentTimeMillis()
                    ) ?: EnvironmentVariable(
                        name = name,
                        value = value,
                        description = description
                    )
                    repository.saveEnvironmentVariable(target)
                    isAddingEnvVar = false
                    envVarToEdit = null
                    savedMessage = "环境变量已保存"
                }
            }
        )
    }
}

@Composable
fun PersonalizationTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit
) {
    AppearanceTab(
        hazeState = hazeState,
        modifier = modifier,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange
    )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsInnerShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
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
    var keyList by remember {
        val initialList = if (!config?.apiKey.isNullOrBlank()) {
            AiRepository.parseApiKeys(config!!.apiKey)
        } else emptyList()
        mutableStateOf(initialList.ifEmpty { listOf("") })
    }
    var keyVisibilityList by remember {
        mutableStateOf(List(16) { false })
    }
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
                val parsed = AiRepository.parseApiKeys(decrypted.apiKey)
                keyList = parsed.ifEmpty { listOf("") }
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
            val screenHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = (screenHeight * 0.78f).dp)
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

                // API Key 独立输入框列表 (需求 1)
                item {
                    val validKeysCount = keyList.count { it.isNotBlank() }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "API Key 密钥列表",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (validKeysCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ) {
                                    Text(
                                        text = if (validKeysCount > 1) "已录入 $validKeysCount 个密钥 · 自动轮询故障转移" else "已录入 1 个密钥",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        keyList.forEachIndexed { index, currentKey ->
                            val isVisible = keyVisibilityList.getOrElse(index) { false }
                            val keyLabel = if (index == 0) "Key 1 (主密钥)" else "Key ${index + 1} (备用密钥 $index)"

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = keyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium
                                )
                                OutlinedTextField(
                                    value = currentKey,
                                    onValueChange = { newVal ->
                                        val splitKeys = AiRepository.parseApiKeys(newVal)
                                        if (splitKeys.size > 1) {
                                            val updated = keyList.toMutableList()
                                            updated.removeAt(index)
                                            updated.addAll(index, splitKeys)
                                            keyList = updated
                                            apiKey = updated.filter { it.isNotBlank() }.joinToString("\n")
                                        } else {
                                            val updated = keyList.toMutableList()
                                            updated[index] = newVal.trim()
                                            keyList = updated
                                            apiKey = updated.filter { it.isNotBlank() }.joinToString("\n")
                                        }
                                    },
                                    placeholder = { Text(if (index == 0) "填写主密钥 (sk-...)" else "填写备用密钥 (sk-...)", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)) },
                                    visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = SettingsInnerShape,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)
                                    ),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    val nextVis = keyVisibilityList.toMutableList()
                                                    while (nextVis.size <= index) nextVis.add(false)
                                                    nextVis[index] = !isVisible
                                                    keyVisibilityList = nextVis
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (isVisible) "隐藏密钥" else "显示密钥",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                            if (keyList.size > 1) {
                                                IconButton(
                                                    onClick = {
                                                        val updated = keyList.toMutableList()
                                                        updated.removeAt(index)
                                                        keyList = updated.ifEmpty { listOf("") }
                                                        apiKey = keyList.filter { it.isNotBlank() }.joinToString("\n")
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "删除此密钥",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // "+ 添加 Key" 按钮
                        OutlinedButton(
                            onClick = {
                                keyList = keyList + ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("添加独立 Key 输入框 (Key ${keyList.size + 1})", style = MaterialTheme.typography.labelMedium)
                        }

                        Text(
                            text = "💡 每个输入框填写一个独立 Key。亦可将多行或逗号分隔的密钥批量粘贴进任意输入框自动拆分。当请求超时或遇到连接/认证报错时，系统将自动切换至备用 Key 并透明重试。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                        )
                    }
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
                        maxTokens = 50000,
                        topP = 1.0f,
                        enableThinking = true,
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
