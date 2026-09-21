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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.aiassistant.domain.model.ChatModelOption
import com.aiassistant.ui.components.ImageCropEditDialog
import com.aiassistant.ui.components.CropShapeMode
import com.aiassistant.ui.components.ExpandableText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
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
import com.aiassistant.domain.model.NamedApiKey
import com.aiassistant.domain.model.Conversation
import com.aiassistant.domain.model.EnvironmentVariable
import com.aiassistant.domain.model.MemoryItem
import com.aiassistant.domain.model.WorldBook
import com.aiassistant.domain.model.WorldBookEntry
import com.aiassistant.domain.model.ModelCapabilityEngine
import com.aiassistant.domain.model.ModelCustomSettings
import com.aiassistant.domain.model.PromptTemplate
import com.aiassistant.ui.components.EchoGlassCard
import com.aiassistant.ui.components.EchoGlassDialog
import com.aiassistant.ui.components.EchoGlassDropdownMenu
import com.aiassistant.ui.components.readableTextColorFor
import com.aiassistant.ui.components.rememberReadableBackdropColors
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
import com.aiassistant.ui.components.rememberSmoothReorderState
import com.aiassistant.ui.components.reorderItem
import com.aiassistant.ui.components.reorderDragHandle
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

@Composable
fun ModelFeaturesTab(
    hazeState: dev.chrisbanes.haze.HazeState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
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

    // 记忆提炼辅助模型状态
    var auxiliaryMemoryEnabled by remember(settings) { mutableStateOf(settings.auxiliaryMemoryEnabled) }
    var auxiliaryMemoryConfigId by remember(settings) { mutableLongStateOf(settings.auxiliaryMemoryApiConfigId) }
    var auxiliaryMemoryModel by remember(settings) { mutableStateOf(settings.auxiliaryMemoryModel) }
    var auxiliaryMemoryPrompt by remember(settings) { mutableStateOf(settings.auxiliaryMemoryPrompt) }
    var isTestingAuxiliaryMemory by remember { mutableStateOf(false) }
    var auxiliaryTestResult by remember { mutableStateOf<String?>(null) }
    var showAuxiliaryTestDialog by remember { mutableStateOf(false) }
    var testCustomInput by remember { mutableStateOf("我平时只喝无糖可乐，对花生重度过敏，正在用 Kotlin 开发 Android 应用") }

    fun persistAuxiliaryMemorySettings(
        enabled: Boolean = auxiliaryMemoryEnabled,
        configId: Long = auxiliaryMemoryConfigId,
        model: String = auxiliaryMemoryModel,
        prompt: String = auxiliaryMemoryPrompt
    ) {
        manager.saveSettings(
            settings.copy(
                auxiliaryMemoryEnabled = enabled,
                auxiliaryMemoryApiConfigId = configId,
                auxiliaryMemoryModel = model.trim(),
                auxiliaryMemoryPrompt = prompt.trim()
            )
        )
        settings = manager.getSettings()
    }

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

    LaunchedEffect(auxiliaryMemoryPrompt) {
        if (auxiliaryMemoryPrompt.trim() != settings.auxiliaryMemoryPrompt.trim()) {
            kotlinx.coroutines.delay(400)
            persistAuxiliaryMemorySettings(prompt = auxiliaryMemoryPrompt)
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
        contentPadding = contentPadding,
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

        // 3. 记忆提炼与时间线分析辅助模型 (自由选择所有模型)
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
                        FlowRow(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("记忆提炼与时间线辅助模型", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (auxiliaryMemoryEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (auxiliaryMemoryEnabled) "已启用" else "未启用",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (auxiliaryMemoryEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            "指定独立 API 与模型（如 deepseek-chat、gpt-4o-mini）专门提炼记忆与全量时间线梳理，彻底避免干扰主模型上下文与计费",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = auxiliaryMemoryEnabled,
                        onCheckedChange = {
                            auxiliaryMemoryEnabled = it
                            persistAuxiliaryMemorySettings(enabled = it)
                            savedMessage = if (it) "已开启记忆提炼辅助模型" else "已关闭记忆提炼辅助模型"
                        }
                    )
                }

                if (auxiliaryMemoryEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // 统一使用 UniversalModelPickerCard
                    UniversalModelPickerCard(
                        hazeState = hazeState,
                        title = "记忆提炼专用模型",
                        subtitle = "直接跨服务商自由选择所有模型，无需先切换服务商",
                        selectedConfigId = auxiliaryMemoryConfigId,
                        selectedModel = auxiliaryMemoryModel,
                        allConfigs = allApiConfigs,
                        onSelect = { cfgId, model ->
                            auxiliaryMemoryConfigId = cfgId
                            auxiliaryMemoryModel = model
                            persistAuxiliaryMemorySettings(configId = cfgId, model = model)
                            savedMessage = "已更新记忆提炼辅助模型"
                        }
                    )

                    // 自定义提炼提示词
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "自定义记忆提炼提示词（可选）：",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = auxiliaryMemoryPrompt,
                            onValueChange = {
                                auxiliaryMemoryPrompt = it
                                savedMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp),
                            placeholder = { Text("留空将使用默认记忆提炼与时间线解析提示词...") },
                            minLines = 2,
                            maxLines = 6,
                            shape = SettingsInnerShape
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "支持记忆提取与全量时间线分析",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = { showAuxiliaryTestDialog = true },
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("即时测试辅助连接", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 4. 思考胶囊文案自定义
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

    // 辅助模型测试与平滑降级弹窗
    if (showAuxiliaryTestDialog) {
        AuxiliaryMemoryTestDialog(
            hazeState = hazeState,
            testInput = testCustomInput,
            onInputChange = { testCustomInput = it },
            isTesting = isTestingAuxiliaryMemory,
            testResult = auxiliaryTestResult,
            onRunTest = {
                coroutineScope.launch {
                    isTestingAuxiliaryMemory = true
                    auxiliaryTestResult = null
                    val res = repository.testAuxiliaryMemoryExtraction(
                        apiConfigId = auxiliaryMemoryConfigId,
                        modelName = auxiliaryMemoryModel,
                        testText = testCustomInput,
                        customPrompt = auxiliaryMemoryPrompt
                    )
                    auxiliaryTestResult = res.getOrNull() ?: res.exceptionOrNull()?.message ?: "测试完成"
                    isTestingAuxiliaryMemory = false
                }
            },
            onDismiss = {
                showAuxiliaryTestDialog = false
                auxiliaryTestResult = null
            }
        )
    }
}
