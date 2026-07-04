@file:OptIn(ExperimentalMaterial3Api::class)

package com.aiassistant.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aiassistant.AiAssistantApp
import com.aiassistant.BuildConfig
import com.aiassistant.R
import com.aiassistant.domain.model.ApiConfig
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.EnvironmentVariable
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.utils.AvatarManager
import com.aiassistant.utils.BackgroundImageManager
import com.aiassistant.utils.BackupManager
import com.aiassistant.utils.HiddenConversationLock
import com.aiassistant.utils.TavilySearchSettings
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val CurrentFeatureHighlights = listOf(
    "多模型 API 与流式对话",
    "Token 预算上下文、滚动摘要与长期记忆",
    "上下文使用情况查看与主动压缩",
    "对话导航随滚动出现",
    "对话历史、文件夹与置顶管理",
    "思考模式、联网搜索与临时对话设置",
    "文件/图片上传与 OCR 辅助",
    "环境变量管理、数据备份与恢复",
    "自定义用户头像、模型头像与背景图片"
)

private val CurrentVersionUserUpdates = listOf(
    "修复用户消息气泡文字不可见和滚动时模糊层错位",
    "上下文圆环入口恢复为正圆，主动压缩按钮任何时候都可点击",
    "上下文预算不再被 64k 截断，长上下文模型显示更准确",
    "消息底栏信息和复制、重生成、编辑、删除按钮重新排版对齐",
    "应用内容支持延伸到手机状态栏区域",
    "首页创建对话按钮移除异常白色矩形，首页导航栏仅在滑动时出现"
)

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Long) -> Unit
) {
    var selectedSection by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedSection != null) {
                            selectedSection = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        when (selectedSection) {
            null -> SettingsMenu(
                modifier = Modifier.padding(paddingValues),
                onSectionSelected = { selectedSection = it }
            )
            "api_config" -> ApiConfigTab(modifier = Modifier.padding(paddingValues))
            "web_search" -> WebSearchTab(modifier = Modifier.padding(paddingValues))
            "personalization" -> PersonalizationTab(modifier = Modifier.padding(paddingValues))
            "global_prompt" -> GlobalPromptTab(modifier = Modifier.padding(paddingValues))
            "env_variables" -> EnvironmentVariablesTab(modifier = Modifier.padding(paddingValues))
            "hidden_conversations" -> HiddenConversationsTab(
                modifier = Modifier.padding(paddingValues),
                onNavigateToChat = onNavigateToChat
            )
            "backup" -> BackupTab(modifier = Modifier.padding(paddingValues))
            "about" -> AboutTab(modifier = Modifier.padding(paddingValues))
        }
    }
}

@Composable
fun SettingsMenu(
    modifier: Modifier = Modifier,
    onSectionSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SettingsMenuItem(
                icon = Icons.Default.Key,
                title = "API配置",
                subtitle = "管理AI模型API密钥和配置",
                onClick = { onSectionSelected("api_config") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.Search,
                title = "联网搜索",
                subtitle = "配置 Tavily，让对话中的智能搜索真正联网",
                onClick = { onSectionSelected("web_search") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.AutoAwesome,
                title = "个性化",
                subtitle = "设置所有对话都会参考的自定义偏好",
                onClick = { onSectionSelected("personalization") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.Psychology,
                title = "全局提示词",
                subtitle = "设置适用于所有对话的系统提示词",
                onClick = { onSectionSelected("global_prompt") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.Code,
                title = "环境变量",
                subtitle = "管理可在对话中引用的变量",
                onClick = { onSectionSelected("env_variables") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.VisibilityOff,
                title = "其他对话",
                subtitle = "输入 6 位数字密码查看隐藏对话",
                onClick = { onSectionSelected("hidden_conversations") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.Backup,
                title = "数据备份",
                subtitle = "备份和恢复应用数据",
                onClick = { onSectionSelected("backup") }
            )
        }
        item {
            SettingsMenuItem(
                icon = Icons.Default.Info,
                title = "关于",
                subtitle = "版本信息和功能介绍",
                onClick = { onSectionSelected("about") }
            )
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ApiConfigTab(modifier: Modifier = Modifier) {
    val repository = AiAssistantApp.instance.repository
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val configs by repository.getAllApiConfigs().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var editingConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var isSaving by remember { mutableStateOf(false) }

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
    config: ApiConfig,
    onEdit: (ApiConfig) -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        SuggestionChip(
                            onClick = {},
                            label = { Text("新对话默认API") },
                            modifier = Modifier.height(24.dp)
                        )
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
    }

    if (showDeleteDialog) {
        AlertDialog(
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
fun WebSearchTab(modifier: Modifier = Modifier) {
    val manager = AiAssistantApp.instance.tavilySearchManager
    var settings by remember { mutableStateOf(manager.getSettings()) }
    var apiKey by remember(settings) { mutableStateOf(settings.apiKey) }
    var enabled by remember(settings) { mutableStateOf(settings.enabled) }
    var searchDepth by remember(settings) { mutableStateOf(settings.searchDepth) }
    var maxResults by remember(settings) { mutableStateOf(settings.maxResults.toString()) }
    var includeAnswer by remember(settings) { mutableStateOf(settings.includeAnswer) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tavily 联网搜索", style = MaterialTheme.typography.titleMedium)
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

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tavily API Key") },
                        placeholder = { Text("tvly-...") },
                        singleLine = true
                    )

                    Text(
                        "Key 只保存在本机应用私有数据中，并使用 Android Keystore 加密；不会写入源码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("搜索参数", style = MaterialTheme.typography.titleMedium)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("basic", "advanced").forEach { depth ->
                            FilterChip(
                                selected = searchDepth == depth,
                                onClick = { searchDepth = depth },
                                label = { Text(if (depth == "basic") "基础" else "深入") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = maxResults,
                        onValueChange = { value ->
                            maxResults = value.filter { it.isDigit() }.take(2)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("最大结果数 1-20") },
                        singleLine = true
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
        }

        item {
            Button(
                onClick = {
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
                },
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
fun GlobalPromptTab(modifier: Modifier = Modifier) {
    val repository = AiAssistantApp.instance.repository
    val scope = rememberCoroutineScope()
    val templates by repository.getAllPromptTemplates().collectAsState(initial = emptyList())

    var globalPrompt by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "全局系统提示词",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "设置的提示词将应用于所有新对话",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = globalPrompt,
                onValueChange = { globalPrompt = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                placeholder = { Text("输入全局系统提示词...") },
                maxLines = 20
            )
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        // 保存全局提示词
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存")
            }
        }
    }
}

@Composable
fun PersonalizationTab(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = AiAssistantApp.instance.personalizationManager
    var settings by remember { mutableStateOf(manager.getSettings()) }
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
    var savedMessage by remember { mutableStateOf<String?>(null) }
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            Text("个性化", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "这些偏好会自动应用到所有对话，单个对话的系统提示词仍可覆盖它们。",
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
                        onValueChange = { instruction = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        placeholder = {
                            Text("例如：默认用中文回答；少用表格；回答自然一点；复杂问题先给结论。")
                        },
                        minLines = 8,
                        maxLines = 18,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
        }

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

        item {
            Button(
                onClick = {
                    val saved = manager.saveSettings(
                        settings.copy(
                            aboutUser = instruction.trim(),
                            responseStyle = "",
                            preferences = "",
                            avoid = ""
                        )
                    )
                    settings = manager.getSettings()
                    savedMessage = if (saved) "已保存个性化设置" else "保存失败，请重试"
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存")
            }
        }
    }
}

@Composable
private fun BackgroundPickerRow(
    title: String,
    hasImage: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun HiddenConversationsTab(
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
        }

        if (!hasPassword) {
            item {
                PinSetupCard(
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
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "当前没有隐藏对话。",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(hiddenConversations, key = { it.id }) { conversation ->
                    HiddenConversationCard(
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
    pin: String,
    confirmPin: String,
    message: String?,
    onPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
}

@Composable
private fun PinVerifyCard(
    pin: String,
    message: String?,
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
}

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
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
    conversation: Conversation,
    onOpen: () -> Unit,
    onUnhide: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
}

private fun String.onlySixDigits(): String {
    return filter { it.isDigit() }.take(6)
}

@Composable
fun EnvironmentVariablesTab(modifier: Modifier = Modifier) {
    val repository = AiAssistantApp.instance.repository
    val scope = rememberCoroutineScope()
    val variables by repository.getAllEnvironmentVariables().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "环境变量",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "使用 {{变量名}} 在对话中引用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(variables) { variable ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "{{${variable.name}}}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        variable.description?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            repository.deleteEnvironmentVariable(variable)
                        }
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加环境变量")
            }
        }
    }
}

@Composable
fun BackupTab(modifier: Modifier = Modifier) {
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
            Card(modifier = Modifier.fillMaxWidth()) {
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
                    colors = CardDefaults.cardColors(
                        containerColor = if (message.contains("成功") || message.contains("已导出") || message.contains("已保存"))
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
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
    backup: BackupManager.BackupItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
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
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = "恢复")
            }
        }
    }
}

@Composable
fun AboutTab(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 用户头像设置
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "用户头像",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
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
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击更换头像",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 应用信息
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
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

    AlertDialog(
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
                        FilterChip(
                            selected = apiType == "openai",
                            onClick = { apiType = "openai" },
                            label = { Text("OpenAI") }
                        )
                        FilterChip(
                            selected = apiType == "anthropic",
                            onClick = { apiType = "anthropic" },
                            label = { Text("Anthropic") }
                        )
                    }
                }

                // 配置名称
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("配置名称") },
                        singleLine = true
                    )
                }

                // 提供商
                item {
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("提供商") },
                        singleLine = true
                    )
                }

                // Base URL
                item {
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.example.com/v1") },
                        singleLine = true
                    )
                }

                // API Key
                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Key") },
                        singleLine = true
                    )
                }

                // 模型选择
                item {
                    Column {
                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("此 API 的默认模型") },
                            singleLine = true
                        )
                        Text(
                            text = "新建对话会先使用标记为“新对话默认API”的配置，再使用这里设置的默认模型。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
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
                        maxTokens = 4096,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onRowClick)
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
            DropdownMenu(
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
