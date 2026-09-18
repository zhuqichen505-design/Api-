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
internal fun StoryUnifiedSettingsDialog(
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
        val currentMax = tempSettings.maxTokens.takeIf { it >= 50000 } ?: 50000
        mutableStateOf(currentMax.toString())
    }
    var topP by remember { mutableFloatStateOf(tempSettings.topP) }
    var enableThinking by remember { mutableStateOf(tempSettings.enableThinking) }
    var thinkingEffort by remember { mutableStateOf(tempSettings.thinkingEffort) }
    var enableWebSearch by remember { mutableStateOf(tempSettings.enableWebSearch) }
    var enableExternalMemory by remember { mutableStateOf(session.enableExternalMemory) }
    var enableWorldBook by remember { mutableStateOf(session.enableWorldBook) }
    var promptTextFieldValue by rememberSaveable(currentPrompt, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = currentPrompt.orEmpty(),
                selection = TextRange(0)
            )
        )
    }
    var isPromptExpanded by remember { mutableStateOf(false) }
    var showTemplates by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var avatarRevision by remember { mutableIntStateOf(0) }
    val modelAvatarBitmap = remember(context, avatarRevision) {
        AvatarManager.getModelAvatarBitmap(context)
    }
    val modelAvatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingCropUri = it
        }
    }

    if (pendingCropUri != null) {
        ImageCropEditDialog(
            imageUri = pendingCropUri!!,
            shapeMode = CropShapeMode.CIRCLE,
            title = "裁剪与编辑故事角色头像",
            onDismiss = { pendingCropUri = null },
            onConfirm = { croppedBitmap ->
                pendingCropUri = null
                AvatarManager.saveModelAvatarBitmap(context, croppedBitmap)
                avatarRevision++
                onModelAvatarChanged()
            }
        )
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                        Text("外置记忆库", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text("结合角色长期事实库，意图匹配自动注入", style = MaterialTheme.typography.bodySmall, color = dialogSecondaryColor)
                                    }
                                    Switch(
                                        checked = enableExternalMemory,
                                        onCheckedChange = { enableExternalMemory = it }
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
                                        Text("世界书设定 (Lorebook)", style = MaterialTheme.typography.titleSmall, color = dialogContentColor)
                                        Text("根据关键词动态唤醒世界观词条或常驻条目", style = MaterialTheme.typography.bodySmall, color = dialogSecondaryColor)
                                    }
                                    Switch(
                                        checked = enableWorldBook,
                                        onCheckedChange = { enableWorldBook = it }
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
                            maxTokens = maxTokens.toIntOrNull() ?: 50000,
                            topP = topP,
                            enableThinking = enableThinking,
                            thinkingEffort = thinkingEffort,
                            enableWebSearch = enableWebSearch,
                            enableSessionMemory = tempSettings.enableSessionMemory,
                            enableExternalMemory = enableExternalMemory,
                            enableWorldBook = enableWorldBook,
                            activeWorldBookIds = tempSettings.activeWorldBookIds
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
internal fun EditableSettingProposalDialog(
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
internal fun ActionChip(text: String, onClick: () -> Unit) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelSmall) }
    )
}

@Composable
internal fun SmartAppendStoryDialog(
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


@Composable
fun TimelineReconcileDialog(
    hazeState: dev.chrisbanes.haze.HazeState,
    initialResult: TimelineReconcileResult,
    onDismiss: () -> Unit,
    onApply: (String, List<TimelineEventItem>, List<AtemporalSettingItem>) -> Unit
) {
    var storyTime by remember(initialResult) { mutableStateOf(initialResult.currentStoryTime) }
    val events = remember(initialResult) {
        mutableStateListOf<TimelineEventItem>().apply {
            addAll(initialResult.events.map { it.copy() })
        }
    }
    val atemporalSettings = remember(initialResult) {
        mutableStateListOf<AtemporalSettingItem>().apply {
            addAll(initialResult.atemporalSettings.map { it.copy() })
        }
    }

    // 筛选状态
    var selectedTimeFilter by remember { mutableStateOf("全部") }
    var selectedCategoryFilter by remember { mutableStateOf<TimelineCategory?>(null) }

    // 提取所有出现过的独立时间标签集合
    val distinctTimeTags = remember(events.size, events.map { it.timeTag }) {
        val tags = mutableListOf("全部")
        events.map { it.timeTag.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { tag ->
                if (!tags.contains(tag)) tags.add(tag)
            }
        tags
    }

    // 过滤后的事件列表
    val filteredEvents = remember(events.toList(), selectedTimeFilter, selectedCategoryFilter) {
        events.filter { event ->
            val matchTime = (selectedTimeFilter == "全部") ||
                    (event.timeTag.trim() == selectedTimeFilter) ||
                    (event.timeTag.contains(selectedTimeFilter))
            val matchCat = (selectedCategoryFilter == null) || (event.category == selectedCategoryFilter)
            matchTime && matchCat
        }
    }

    EchoGlassDialog(
        hazeState = hazeState,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.98f)
            .widthIn(max = 620.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "时间轴与多维设定工作台",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "已提炼 ${events.size} 条时间节点 · ${atemporalSettings.size} 条全局设定",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 0. 模型提炼来源与状态指示
                if (initialResult.extractionSource == "AI_MODEL") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (initialResult.modelUsed.isNotBlank()) {
                                    "✨ AI 大模型智慧深度提炼完成（模型：${initialResult.modelUsed}）"
                                } else {
                                    "✨ AI 大模型智慧深度提炼完成"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val modelHint = if (initialResult.modelUsed.isNotBlank()) "调用模型：${initialResult.modelUsed} | " else ""
                            val errorDetail = initialResult.extractionErrorMessage.orEmpty()
                            val timeoutHint = if (errorDetail.contains("timeout", ignoreCase = true) || errorDetail.contains("timed out", ignoreCase = true)) {
                                "全文较长且模型推理响应超时，可再次点击重新梳理或在「设置 -> 辅助模型」选用推理更快的模型"
                            } else {
                                errorDetail.ifBlank { "未检测到模型响应" }
                            }
                            Text(
                                text = "⚠️ 模型响应未成功（$modelHint$timeoutHint），当前显示本地精纯扫描。可在「设置 -> 辅助模型」指定独立模型或检查当前网络。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // 1. 顶部当前故事时间编辑卡片
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "当前故事停留在：",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        BasicTextField(
                            value = storyTime,
                            onValueChange = { storyTime = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (storyTime.isEmpty()) {
                                        Text(
                                            text = "例如：第5天·上午",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. 筛选控制栏 (时间过滤 Chips + 类别过滤 Chips)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 时间过滤 Chips (当筛选固有设定时隐藏时间过滤)
                    if (selectedCategoryFilter != TimelineCategory.ATEMPORAL_SETTING && distinctTimeTags.size > 2) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(distinctTimeTags) { tag ->
                                val isSelected = (selectedTimeFilter == tag)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTimeFilter = tag },
                                    label = {
                                        Text(
                                            if (tag == "全部") "🌟 全部时间线" else tag,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }

                    // 类别过滤 Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = (selectedCategoryFilter == null),
                                onClick = { selectedCategoryFilter = null },
                                label = { Text("全部类别", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        items(TimelineCategory.values()) { cat ->
                            val isSelected = (selectedCategoryFilter == cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = if (isSelected) null else cat
                                },
                                label = {
                                    Text("${cat.emoji} ${cat.displayName}", style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(android.graphics.Color.parseColor(cat.tagColorHex)).copy(alpha = 0.22f),
                                    selectedLabelColor = Color(android.graphics.Color.parseColor(cat.tagColorHex))
                                )
                            )
                        }
                    }
                }

                // 3. 核心滚动区 (包含时间无关设定确认卡片 + 垂直时间轴事件流)
                val showAtemporalSection = (selectedCategoryFilter == null && selectedTimeFilter == "全部") ||
                        (selectedCategoryFilter == TimelineCategory.ATEMPORAL_SETTING)
                val showTimelineEvents = selectedCategoryFilter != TimelineCategory.ATEMPORAL_SETTING

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // A. 与时间无关设定确认卡片 (Atemporal Settings Section)
                    if (showAtemporalSection && (atemporalSettings.isNotEmpty() || selectedCategoryFilter == TimelineCategory.ATEMPORAL_SETTING)) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "💡 世界观与角色固有设定（时间无关）",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = {
                                                    for (i in atemporalSettings.indices) {
                                                        atemporalSettings[i] = atemporalSettings[i].copy(isSelected = true)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("全选", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(
                                                onClick = {
                                                    for (i in atemporalSettings.indices) {
                                                        atemporalSettings[i] = atemporalSettings[i].copy(isSelected = false)
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("全不选", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "模型通读识别出以下不随具体剧情天数变化的常驻设定与规则。请确认勾选需要同步保存的条目：",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    atemporalSettings.forEachIndexed { sIdx, setting ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = setting.isSelected,
                                                    onCheckedChange = { checked ->
                                                        val idx = atemporalSettings.indexOfFirst { it.id == setting.id }
                                                        if (idx != -1) {
                                                            atemporalSettings[idx] = setting.copy(isSelected = checked)
                                                        }
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))

                                                // 类别切换胶囊
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                    modifier = Modifier.clickable {
                                                        val idx = atemporalSettings.indexOfFirst { it.id == setting.id }
                                                        if (idx != -1) {
                                                            atemporalSettings[idx] = setting.copy(category = setting.nextCategory())
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = setting.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                // 设定内容直接编辑
                                                BasicTextField(
                                                    value = setting.content,
                                                    onValueChange = { newContent ->
                                                        val idx = atemporalSettings.indexOfFirst { it.id == setting.id }
                                                        if (idx != -1) {
                                                            atemporalSettings[idx] = setting.copy(content = newContent)
                                                        }
                                                    },
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                                                    decorationBox = { innerTextField ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(
                                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .border(
                                                                    BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                                                    RoundedCornerShape(6.dp)
                                                                )
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            if (setting.content.isEmpty()) {
                                                                Text(
                                                                    text = "设定描述（如畏寒、不加糖黑咖啡）...",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                                )
                                                            }
                                                            innerTextField()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Spacer(modifier = Modifier.width(4.dp))

                                                // 目标范围切换按钮
                                                IconButton(
                                                    onClick = {
                                                        val idx = atemporalSettings.indexOfFirst { it.id == setting.id }
                                                        if (idx != -1) {
                                                            val nextScope = if (setting.targetScope == "global") "session" else "global"
                                                            atemporalSettings[idx] = setting.copy(targetScope = nextScope)
                                                        }
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Text(
                                                        text = if (setting.targetScope == "global") "全局" else "会话",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // 删除按钮
                                                IconButton(
                                                    onClick = {
                                                        val idx = atemporalSettings.indexOfFirst { it.id == setting.id }
                                                        if (idx != -1) {
                                                            atemporalSettings.removeAt(idx)
                                                        }
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.DeleteOutline,
                                                        contentDescription = "删除",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 手动补充全局设定
                                    OutlinedButton(
                                        onClick = {
                                            atemporalSettings.add(
                                                AtemporalSettingItem(
                                                    category = "角色设定",
                                                    content = "",
                                                    isSelected = true,
                                                    targetScope = "session"
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("＋ 手动补充全局设定", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    if (showTimelineEvents) {
                        // B. 编年史事件列表标题
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "时间轴发展脉络 (${filteredEvents.size}/${events.size}条)：",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (selectedTimeFilter != "全部" || selectedCategoryFilter != null) {
                                    TextButton(
                                        onClick = {
                                            selectedTimeFilter = "全部"
                                            selectedCategoryFilter = null
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("重置筛选", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // 垂直时间轴事件流 (Timeline Events Stream)
                        itemsIndexed(filteredEvents) { idx, item ->
                            val isLast = (idx == filteredEvents.size - 1)
                            val catColor = Color(android.graphics.Color.parseColor(item.category.tagColorHex))
                            val relativeTime = remember(item.timeTag, storyTime) {
                                TimelineMemoryHelper.calculateRelativeTime(item.timeTag, storyTime)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                // 垂直流线与发光节点列
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(26.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(catColor.copy(alpha = 0.22f), CircleShape)
                                            .border(2.dp, catColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .background(catColor, CircleShape)
                                        )
                                    }

                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(72.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            catColor.copy(alpha = 0.65f),
                                                            catColor.copy(alpha = 0.15f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // 事件编辑卡片
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, catColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(bottom = 6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // 头部操作行：类别切换胶囊 + 时间标签修改 + 相对时间距离徽章 + 删除
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 类别胶囊 (点击循环切换分类)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = catColor.copy(alpha = 0.16f),
                                                modifier = Modifier.clickable {
                                                    val all = TimelineCategory.values()
                                                    val nextIdx = (all.indexOf(item.category) + 1) % all.size
                                                    val idxInMaster = events.indexOfFirst { it.id == item.id }
                                                    if (idxInMaster != -1) {
                                                        events[idxInMaster] = item.copy(category = all[nextIdx])
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${item.category.emoji} ${item.category.displayName}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = catColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // 时间标签直接编辑输入框
                                            BasicTextField(
                                                value = item.timeTag,
                                                onValueChange = { newTag ->
                                                    val idxInMaster = events.indexOfFirst { it.id == item.id }
                                                    if (idxInMaster != -1) {
                                                        events[idxInMaster] = item.copy(timeTag = newTag)
                                                    }
                                                },
                                                singleLine = true,
                                                textStyle = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                decorationBox = { innerTextField ->
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .border(
                                                                BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                                RoundedCornerShape(6.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        contentAlignment = Alignment.CenterStart
                                                    ) {
                                                        if (item.timeTag.isEmpty()) {
                                                            Text(
                                                                text = "如：第1天·傍晚",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f)
                                            )

                                            // 相对时间距离胶囊
                                            if (!relativeTime.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = relativeTime,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            // 删除按钮
                                            IconButton(
                                                onClick = {
                                                    val idx = events.indexOfFirst { it.id == item.id }
                                                    if (idx != -1) {
                                                        events.removeAt(idx)
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "删除该条",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // 正文多行直接编辑输入框
                                        BasicTextField(
                                            value = item.content,
                                            onValueChange = { newContent ->
                                                val idx = events.indexOfFirst { it.id == item.id }
                                                if (idx != -1) {
                                                    events[idx] = item.copy(content = newContent)
                                                }
                                            },
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .border(
                                                            BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.TopStart
                                                ) {
                                                    if (item.content.isEmpty()) {
                                                        Text(
                                                            text = "输入在此时间节点发生的客观剧情事实或确立的设定规则...",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 补充遗漏事件按钮
                        item {
                            OutlinedButton(
                                onClick = {
                                    events.add(
                                        TimelineEventItem(
                                            timeTag = if (selectedTimeFilter != "全部") selectedTimeFilter else (storyTime.ifBlank { "第 1 天" }),
                                            content = "",
                                            category = if (selectedCategoryFilter != null && selectedCategoryFilter != TimelineCategory.ATEMPORAL_SETTING) selectedCategoryFilter!! else TimelineCategory.PLOT_EVENT
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("＋ 手动添加时间节点事件", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        buttons = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 顶部统计胶囊指示条
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "时间节点 ${events.size} 条",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "全局设定 ${atemporalSettings.count { it.isSelected }}/${atemporalSettings.size} 条",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Text(
                            text = storyTime.ifBlank { "未指定时间" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 底部操作胶囊行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 放弃按钮 (轻量液态玻璃胶囊)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { onDismiss() }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "放弃",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 保存并同步到记忆按钮 (高级 Echo 渐变流动光泽立体胶囊)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clickable {
                                val validEvents = events.filter { it.content.isNotBlank() }
                                val confirmedSettings = atemporalSettings.filter { it.isSelected && it.content.isNotBlank() }
                                onApply(storyTime, validEvents, confirmedSettings)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.BookmarkAdded,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "保存并同步到记忆",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
