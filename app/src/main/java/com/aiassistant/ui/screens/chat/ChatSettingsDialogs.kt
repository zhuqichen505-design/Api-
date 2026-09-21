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
import androidx.compose.runtime.saveable.rememberSaveable
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
internal fun getCategoryDisplayName(category: String): String {
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

data class ThinkingEffortOption(
    val value: String,
    val label: String
)

data class ChatTuningProfile(
    val modelLabel: String,
    val temperatureMax: Float,
    val temperatureEnabled: Boolean,
    val thinkingEfforts: List<ThinkingEffortOption>,
    val noThinkingEffortReason: String? = null
)

internal fun chatTuningProfile(
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

    val efforts = if (enableThinking) {
        val gears = if (cap.supportedThinkingGears.isNotEmpty()) {
            cap.supportedThinkingGears
        } else {
            listOf("low", "medium", "high", "ultra")
        }
        gears.map { gear ->
            val mappedGear = if (gear == "max") "ultra" else gear
            val gearLabel = when (mappedGear) {
                "low" -> "快速"
                "medium" -> "平衡"
                "high" -> "深入"
                "ultra", "max" -> "极高"
                else -> gear
            }
            ThinkingEffortOption(mappedGear, gearLabel)
        }
    } else {
        emptyList()
    }

    val reason: String? = null

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
internal fun ChatSettingsModelSelector(
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
        val cap = remember(currentLabel) {
            com.aiassistant.domain.model.ModelCapabilityEngine.evaluateModel(currentLabel)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("模型", style = MaterialTheme.typography.titleSmall, color = contentColor)
            Row(
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
                    text = currentLabel.displayModelShortName().ifBlank { "未选择模型" },
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
    }
}

@Composable
internal fun ChatSettingsSystemPromptSection(
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
fun ChatSettingsSessionMemorySection(
    sessionMemories: List<MemoryItem>,
    contentColor: Color,
    secondaryColor: Color,
    enableSessionMemory: Boolean = true,
    onEnableSessionMemoryChange: (Boolean) -> Unit = {},
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    isReconcilingTimeline: Boolean = false,
    reconcileTimelineProgress: String? = null,
    onStartTimelineReconciliation: () -> Unit = {},
    onCancelTimelineReconciliation: () -> Unit = {},
    onAddMemory: (String) -> Unit,
    onUpdateMemory: (MemoryItem) -> Unit,
    onToggleMemory: (Long, Boolean) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearMemories: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var memoryToEdit by remember { mutableStateOf<MemoryItem?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var addMemoryText by remember { mutableStateOf("") }
    var editMemoryText by remember { mutableStateOf("") }
    var isMemoriesExpanded by remember { mutableStateOf(true) }

    val glass = echoGlassPalette()
    val primaryColor = MaterialTheme.colorScheme.primary

    val currentStoryTime = remember(sessionMemories) {
        sessionMemories.firstOrNull {
            it.content.startsWith("【当前故事时间】：") || it.content.startsWith("当前故事时间：")
        }?.content?.substringAfter("：")?.trim()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = glass.control,
        border = BorderStroke(1.dp, glass.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Memory,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "会话专属记忆",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Switch(
                    checked = enableSessionMemory,
                    onCheckedChange = onEnableSessionMemoryChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryColor,
                        checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                    )
                )
            }

            if (!currentStoryTime.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = primaryColor.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "当前故事驻留时间：$currentStoryTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isReconcilingTimeline) {
                    OutlinedButton(
                        onClick = onCancelTimelineReconciliation,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            reconcileTimelineProgress ?: "梳理中 (点击取消)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onStartTimelineReconciliation,
                        enabled = enableSessionMemory,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🕒 梳理全量时间线", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            addMemoryText = ""
                            showAddDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        enabled = enableSessionMemory
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("添加", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    if (sessionMemories.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirmDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            enabled = enableSessionMemory,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("清空", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }

            Text(
                text = if (enableSessionMemory) {
                    "已开启：自动识别事件天数并组装时间差参照系，彻底防止相对时间混淆；亦可点击「梳理全量时间线」通读校对。"
                } else {
                    "已关闭：本会话发送消息时将暂时不附带记忆与时间线设定。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryColor
            )

            // 故事时间推进锚点展示
            if (!currentStoryTime.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "当前故事推进节点：$currentStoryTime",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val displayMemories = sessionMemories.filter {
                !it.content.startsWith("【当前故事时间】：") && !it.content.startsWith("当前故事时间：")
            }

            if (displayMemories.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                    border = BorderStroke(1.dp, glass.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "当前会话暂无专属记忆与时间线设定",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor
                        )
                        OutlinedButton(
                            onClick = {
                                addMemoryText = ""
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            enabled = enableSessionMemory
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ 添加第一条事件记忆", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            } else {
                // 记忆列表展开与收起切换条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isMemoriesExpanded = !isMemoriesExpanded }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "专属记忆清单 (${displayMemories.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (isMemoriesExpanded) "收起" else "展开",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = if (isMemoriesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isMemoriesExpanded) "收起" else "展开",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isMemoriesExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        displayMemories.forEach { memory ->
                            val event = remember(memory.content) {
                                TimelineMemoryHelper.parseContentToEvent(memory.content)
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enableSessionMemory) 0.45f else 0.22f),
                                border = BorderStroke(1.dp, glass.outline.copy(alpha = if (enableSessionMemory) 0.5f else 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 左侧：时间标签与记忆文本（占据全量横向空间）
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (event.timeTag.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    text = event.timeTag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = event.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (memory.isEnabled && enableSessionMemory) contentColor else secondaryColor
                                        )
                                    }

                                    // 右侧操作区：开关在上，编辑与删除按键在下
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Switch(
                                            checked = memory.isEnabled && enableSessionMemory,
                                            onCheckedChange = { onToggleMemory(memory.id, it) },
                                            enabled = enableSessionMemory,
                                            modifier = Modifier.scale(0.78f)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    memoryToEdit = memory
                                                    editMemoryText = memory.content
                                                },
                                                enabled = enableSessionMemory,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "编辑",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = secondaryColor
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteMemory(memory.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "删除",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
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

    // 添加记忆弹窗 (EchoGlassDialog 保证全屏阴影遮罩覆盖状态栏)
    if (showAddDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showAddDialog = false },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 400.dp),
            title = {
                Text("添加会话专属记忆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "例如：本项目为 Kotlin+Compose 移动端项目，所有返回请提供带详细中文注释的代码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = addMemoryText,
                        onValueChange = { addMemoryText = it },
                        placeholder = { Text("输入此会话的专属设定或约束...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 68.dp, max = 140.dp),
                        maxLines = 4,
                        shape = RoundedCornerShape(10.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                    )
                }
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (addMemoryText.isNotBlank()) {
                                onAddMemory(addMemoryText.trim())
                                showAddDialog = false
                            }
                        },
                        enabled = addMemoryText.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        )
    }

    // 编辑记忆弹窗
    memoryToEdit?.let { memory ->
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { memoryToEdit = null },
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 400.dp),
            title = {
                Text("编辑会话专属记忆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            content = {
                OutlinedTextField(
                    value = editMemoryText,
                    onValueChange = { editMemoryText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp, max = 140.dp),
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { memoryToEdit = null }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (editMemoryText.isNotBlank()) {
                                onUpdateMemory(memory.copy(content = editMemoryText.trim()))
                                memoryToEdit = null
                            }
                        },
                        enabled = editMemoryText.isNotBlank()
                    ) {
                        Text("更新")
                    }
                }
            }
        )
    }

    // 清空确认弹窗
    if (showClearConfirmDialog) {
        EchoGlassDialog(
            hazeState = hazeState,
            onDismissRequest = { showClearConfirmDialog = false },
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .widthIn(max = 380.dp),
            title = {
                Text("清空本会话记忆", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            content = {
                Text("确定要清空当前会话的所有专属记忆吗？此操作无法撤销。", style = MaterialTheme.typography.bodyMedium)
            },
            buttons = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onClearMemories()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("确认清空")
                    }
                }
            }
        )
    }
}

@Composable
internal fun ChatSettingsWorldBookAndExternalMemorySection(
    enableExternalMemory: Boolean,
    onEnableExternalMemoryChange: (Boolean) -> Unit,
    enableWorldBook: Boolean,
    onEnableWorldBookChange: (Boolean) -> Unit,
    contentColor: Color,
    secondaryColor: Color,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "跨会话记忆与世界书 (Lorebook)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            // 跨会话长期记忆
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "跨会话长期记忆",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = "跨会话的全局偏好与用户画像，按输入意图和关键词动态检索注入，角色扮演默认严格隔离",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )
                }
                Switch(
                    checked = enableExternalMemory,
                    onCheckedChange = onEnableExternalMemoryChange,
                    modifier = Modifier.scale(0.85f)
                )
            }

            // 世界书设定
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "世界书设定 (Lorebook)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = contentColor
                    )
                    Text(
                        text = "根据关键词动态唤醒世界观设定与专有名词知识，可在系统设置中管理词条",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor
                    )
                }
                Switch(
                    checked = enableWorldBook,
                    onCheckedChange = onEnableWorldBookChange,
                    modifier = Modifier.scale(0.85f)
                )
            }
        }
    }
}

@Composable
internal fun glassTextFieldColors(
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
    sessionMemories: List<MemoryItem> = emptyList(),
    isReconcilingTimeline: Boolean = false,
    reconcileTimelineProgress: String? = null,
    onStartTimelineReconciliation: () -> Unit = {},
    onCancelTimelineReconciliation: () -> Unit = {},
    onAddSessionMemory: (String) -> Unit = {},
    onUpdateSessionMemory: (MemoryItem) -> Unit = {},
    onToggleSessionMemory: (Long, Boolean) -> Unit = { _, _ -> },
    onDeleteSessionMemory: (Long) -> Unit = {},
    onClearSessionMemories: () -> Unit = {},
    onTempSettingsChange: ((TempChatSettings) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (TempChatSettings, String?) -> Unit,
    onModelSelected: (ChatModelOption) -> Unit,
    onSavePromptTemplate: (String, String) -> Unit,
    onModelAvatarChanged: () -> Unit,
    onConvertToRoleplay: () -> Unit = {},
    conversationId: Long = 0L,
    currentConversationModelAvatarUri: String? = null,
    onUpdateConversationModelAvatar: ((String?) -> Unit)? = null
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
    var enableSessionMemory by remember { mutableStateOf(tempSettings.enableSessionMemory) }
    var enableExternalMemory by remember { mutableStateOf(tempSettings.enableExternalMemory) }
    var enableWorldBook by remember { mutableStateOf(tempSettings.enableWorldBook) }
    var promptTextFieldValue by rememberSaveable(currentPrompt, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = currentPrompt.orEmpty(),
                selection = TextRange(0)
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

    LaunchedEffect(tempSettings) {
        maxTokens = tempSettings.maxTokens.toString()
        topP = tempSettings.topP
        enableThinking = tempSettings.enableThinking
        thinkingEffort = tempSettings.thinkingEffort
        enableWebSearch = tempSettings.enableWebSearch
        enableSessionMemory = tempSettings.enableSessionMemory
        enableExternalMemory = tempSettings.enableExternalMemory
        enableWorldBook = tempSettings.enableWorldBook
    }

    fun notifyTempSettingsChange() {
        val updated = TempChatSettings(
            temperature = temperature.coerceIn(0f, tuningProfile.temperatureMax),
            maxTokens = maxTokens.toIntOrNull() ?: 8192,
            topP = topP,
            enableThinking = enableThinking,
            thinkingEffort = thinkingEffort,
            enableWebSearch = enableWebSearch,
            enableSessionMemory = enableSessionMemory,
            enableExternalMemory = enableExternalMemory,
            enableWorldBook = enableWorldBook,
            activeWorldBookIds = tempSettings.activeWorldBookIds
        )
        onTempSettingsChange?.invoke(updated)
    }

    LaunchedEffect(tuningProfile.temperatureMax) {
        temperature = temperature.coerceIn(0f, tuningProfile.temperatureMax)
    }
    LaunchedEffect(enableThinking, tuningProfile.thinkingEfforts) {
        if (enableThinking && tuningProfile.thinkingEfforts.isNotEmpty() && thinkingEffort !in tuningProfile.thinkingEfforts.map { it.value }) {
            thinkingEffort = tuningProfile.thinkingEfforts.first().value
        }
    }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    val modelAvatarBitmap = remember(context, avatarRevision, currentConversationModelAvatarUri) {
        if (!currentConversationModelAvatarUri.isNullOrBlank()) {
            runCatching {
                val uri = Uri.parse(currentConversationModelAvatarUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        } else {
            AvatarManager.getModelAvatarBitmap(context)
        }
    }
    val modelAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            pendingCropUri = it
        }
    }

    if (pendingCropUri != null) {
        ImageCropEditDialog(
            imageUri = pendingCropUri!!,
            shapeMode = CropShapeMode.CIRCLE,
            title = "裁剪与编辑会话模型头像",
            onDismiss = { pendingCropUri = null },
            onConfirm = { croppedBitmap ->
                pendingCropUri = null
                if (conversationId > 0L && onUpdateConversationModelAvatar != null) {
                    val savedUri = AvatarManager.saveConversationModelAvatarBitmap(context, conversationId, croppedBitmap)
                    onUpdateConversationModelAvatar(savedUri)
                } else {
                    AvatarManager.saveModelAvatarBitmap(context, croppedBitmap)
                    onModelAvatarChanged()
                }
                avatarRevision++
            }
        )
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
                                placeholder = { Text("例如 4096 或 8192") },
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
                            onCheckedChange = {
                                enableThinking = it
                                notifyTempSettingsChange()
                            }
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
                            onCheckedChange = {
                                enableWebSearch = it
                                notifyTempSettingsChange()
                            }
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
                                        onClick = {
                                            thinkingEffort = level.value
                                            notifyTempSettingsChange()
                                        },
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
                    ChatSettingsSessionMemorySection(
                        sessionMemories = sessionMemories,
                        contentColor = dialogContentColor,
                        secondaryColor = dialogSecondaryColor,
                        enableSessionMemory = enableSessionMemory,
                        onEnableSessionMemoryChange = {
                            enableSessionMemory = it
                            notifyTempSettingsChange()
                        },
                        hazeState = hazeState,
                        isReconcilingTimeline = isReconcilingTimeline,
                        reconcileTimelineProgress = reconcileTimelineProgress,
                        onStartTimelineReconciliation = onStartTimelineReconciliation,
                        onCancelTimelineReconciliation = onCancelTimelineReconciliation,
                        onAddMemory = onAddSessionMemory,
                        onUpdateMemory = onUpdateSessionMemory,
                        onToggleMemory = onToggleSessionMemory,
                        onDeleteMemory = onDeleteSessionMemory,
                        onClearMemories = onClearSessionMemories
                    )
                }

                item {
                    ChatSettingsWorldBookAndExternalMemorySection(
                        enableExternalMemory = enableExternalMemory,
                        onEnableExternalMemoryChange = {
                            enableExternalMemory = it
                            notifyTempSettingsChange()
                        },
                        enableWorldBook = enableWorldBook,
                        onEnableWorldBookChange = {
                            enableWorldBook = it
                            notifyTempSettingsChange()
                        },
                        contentColor = dialogContentColor,
                        secondaryColor = dialogSecondaryColor,
                        hazeState = hazeState
                    )
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
                                    text = when {
                                        !currentConversationModelAvatarUri.isNullOrBlank() -> "当前使用本会话专属自定义头像（仅限本会话）"
                                        modelAvatarBitmap != null -> "当前使用全局自定义模型头像"
                                        else -> "当前使用 deepseek 默认头像"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dialogSecondaryColor
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { modelAvatarPicker.launch("image/*") },
                                        shape = RoundedCornerShape(999.dp)
                                    ) {
                                        Text("更换", maxLines = 1)
                                    }
                                    if (modelAvatarBitmap != null || !currentConversationModelAvatarUri.isNullOrBlank()) {
                                        TextButton(
                                            onClick = {
                                                if (conversationId > 0L && onUpdateConversationModelAvatar != null) {
                                                    AvatarManager.deleteConversationModelAvatar(context, currentConversationModelAvatarUri)
                                                    onUpdateConversationModelAvatar(null)
                                                } else {
                                                    AvatarManager.deleteModelAvatar(context)
                                                    onModelAvatarChanged()
                                                }
                                                avatarRevision++
                                            }
                                        ) {
                                            Text("恢复默认", maxLines = 1)
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
                            enableWebSearch = enableWebSearch,
                            enableSessionMemory = enableSessionMemory,
                            enableExternalMemory = enableExternalMemory,
                            enableWorldBook = enableWorldBook,
                            activeWorldBookIds = tempSettings.activeWorldBookIds
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
