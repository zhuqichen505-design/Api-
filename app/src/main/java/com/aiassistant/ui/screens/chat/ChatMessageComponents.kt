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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import com.aiassistant.domain.model.ToolCallRecord
import com.aiassistant.domain.model.QueuedMessage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import com.aiassistant.ui.components.echoShapeClick
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.aiassistant.ui.components.parseInlineMarkdown
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


fun formatThinkingCapsuleText(
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

fun formatNonThinkingCapsuleText(
    modelName: String,
    responseTimeMs: Long,
    tokenCount: Int,
    content: String = ""
): String {
    val model = modelName.ifBlank { "AI" }
    val effectiveTokens = if (tokenCount > 0) tokenCount else AiRepository.estimateTokenCount(content)
    val seconds = (responseTimeMs / 1000).toInt().coerceAtLeast(1)
    return when {
        responseTimeMs > 0 && effectiveTokens > 0 ->
            "${model}用${seconds}秒吃掉了你${effectiveTokens}token"
        responseTimeMs > 0 ->
            "${model}用${seconds}秒回复了你"
        effectiveTokens > 0 ->
            "${model}吃掉了你${effectiveTokens}token"
        else -> model
    }
}

fun parseQuotedMessage(content: String): ParsedQuotedMessage? {
    val trimmed = content.trimStart()
    if (!trimmed.startsWith(">")) return null
    val lines = content.lines()
    val quoteLines = mutableListOf<String>()
    var splitIndex = -1

    for (i in lines.indices) {
        val line = lines[i]
        if (line.startsWith(">")) {
            quoteLines.add(line.removePrefix(">").trimStart())
        } else if (line.isBlank() && quoteLines.isNotEmpty() && splitIndex == -1) {
            splitIndex = i + 1
            break
        } else {
            splitIndex = i
            break
        }
    }

    if (quoteLines.isEmpty()) return null
    val quoteText = quoteLines.joinToString("\n").trim()
    if (quoteText.isBlank()) return null

    val rawRemaining = if (splitIndex in lines.indices) {
        lines.subList(splitIndex, lines.size).joinToString("\n").trim()
    } else ""

    val replyText = rawRemaining
        .removePrefix("针对以上内容：\n")
        .removePrefix("针对以上内容：")
        .trim()

    return ParsedQuotedMessage(quoteText = quoteText, replyText = replyText)
}


internal fun buildDisplayMessages(
    messages: List<Message>,
    selections: Map<String, Int>
): List<DisplayMessageItem> {
    // 过滤掉无内容、无思考、无附件、无工具调用的无效空白异常消息，杜绝幽灵气泡残留
    val validMessages = messages.filter { message ->
        message.content.isNotBlank() ||
        !message.thinkingContent.isNullOrBlank() ||
        !message.attachments.isNullOrBlank() ||
        !message.toolCalls.isNullOrBlank()
    }
    val groups = validMessages
        .filter { !it.variantGroupId.isNullOrBlank() }
        .groupBy { it.variantGroupId!! }
    val consumedGroups = mutableSetOf<String>()
    val result = mutableListOf<DisplayMessageItem>()

    validMessages.forEach { message ->
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

internal fun pairedVariantGroupId(groupId: String): String? {
    return when {
        groupId.endsWith("_user") -> groupId.removeSuffix("_user") + "_assistant"
        groupId.endsWith("_assistant") -> groupId.removeSuffix("_assistant") + "_user"
        else -> null
    }
}

internal fun isErrorMessage(content: String): Boolean {
    val trimmed = content.trim()
    return trimmed.startsWith("请求失败") ||
           trimmed.startsWith("[请求失败]") ||
           trimmed.startsWith("Error:") ||
           trimmed.startsWith("error:") ||
           trimmed.contains("[输出已被中断:")
}

@Composable
internal fun MessageBubble(
    message: Message,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    readableBackdrop: Color = Color.Unspecified,
    isGenerating: Boolean = false,
    assistantAvatarRevision: Int = 0,
    assistantApiConfigId: Long? = null,
    assistantModelName: String = "AI",
    reconnectStatus: String? = null,
    variantInfo: VariantInfo? = null,
    onVariantSelected: (String, Int) -> Unit = { _, _ -> },
    translatingThinking: Boolean = false,
    onTranslateThinking: ((Message) -> Unit)? = null,
    onCopy: () -> Unit,
    onCopyThinking: () -> Unit,
    onQuote: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    customAvatarUri: String? = null,
    onTogglePin: ((Message) -> Unit)? = null,
    onToggleExclude: ((Message) -> Unit)? = null
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
    var showThinking by remember(message.id) {
        mutableStateOf(isGenerating && hasThinkingContent && message.content.isBlank())
    }
    val isThinkingEnglish = remember(message.thinkingContent) {
        AiRepository.isMainlyEnglish(message.thinkingContent)
    }
    val hasTranslation = !message.translatedThinking.isNullOrBlank()
    var showTranslated by remember(message.id, message.translatedThinking) {
        mutableStateOf(hasTranslation)
    }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

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

    activeCitation?.let { citation ->
        CitationDetailDialog(
            citation = citation,
            onDismiss = { activeCitation = null }
        )
    }

    @Composable
    fun MessageContent(contentColor: Color) {
        SelectionContainer {
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
                        val parsedQuote = remember(message.content) { parseQuotedMessage(message.content) }
                        if (parsedQuote != null) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                var isQuoteExpanded by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = contentColor.copy(alpha = 0.09f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .clickable { isQuoteExpanded = !isQuoteExpanded }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight()
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                                    ),
                                                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                                                )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FormatQuote,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "引用内容",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = parseInlineMarkdown(parsedQuote.quoteText),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = contentColor.copy(alpha = 0.82f),
                                                maxLines = if (isQuoteExpanded) Int.MAX_VALUE else 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (parsedQuote.replyText.isNotBlank()) {
                                    Text(
                                        text = parseInlineMarkdown(parsedQuote.replyText),
                                        color = contentColor,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = parseInlineMarkdown(message.content),
                                color = contentColor,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
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
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (message.isExcluded) 0.52f else 1f
            }
    ) {
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
                        variantInfo = variantInfo,
                        onVariantSelected = onVariantSelected,
                        onCopy = onCopy,
                        onQuote = onQuote,
                        onBranch = null,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onTogglePin = onTogglePin?.let { cb -> { cb(message) } },
                        onToggleExclude = onToggleExclude?.let { cb -> { cb(message) } },
                        modifier = Modifier
                            .widthIn(max = bubbleMaxWidth)
                            .fillMaxWidth()
                    )
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
                val thinkingHeaderColor = MaterialTheme.colorScheme.onPrimaryContainer
                val thinkingContentColor = glass.textPrimary

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChatAvatar(
                        isUser = false,
                        avatarRevision = assistantAvatarRevision,
                        apiConfigId = assistantApiConfigId,
                        customAvatarUri = customAvatarUri
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
                        personalizationSettings.thinkingCapsuleTemplate,
                        reconnectStatus
                    ) {
                        val rawModel = assistantModelName.ifBlank { "AI" }
                        val model = rawModel.displayModelShortName()
                        when {
                            isConnecting -> {
                                if (!reconnectStatus.isNullOrBlank()) {
                                    reconnectStatus
                                } else {
                                    personalizationSettings.connectingTextTemplate.replace("{model}", model).ifBlank { "正在连接 $model..." }
                                }
                            }
                            isThinkingActive -> personalizationSettings.thinkingTextTemplate.replace("{model}", model).ifBlank { "$model 正在思考中..." }
                            hasThinking -> formatThinkingCapsuleText(
                                template = personalizationSettings.thinkingCapsuleTemplate.ifBlank { "{model} {status} {time} {tokens}" },
                                modelName = model,
                                isThinkingActive = isGenerating && message.content.isBlank(),
                                responseTimeMs = message.responseTime,
                                thinkingTokens = message.thinkingTokens,
                                totalTokens = message.tokenCount
                            )
                            isGenerating -> personalizationSettings.thinkingTextTemplate.replace("{model}", model).ifBlank { "$model 正在思考回复中..." }
                            else -> formatNonThinkingCapsuleText(
                                modelName = model,
                                responseTimeMs = message.responseTime,
                                tokenCount = message.tokenCount,
                                content = message.content
                            )
                        }
                    }

                    var isStatusExpanded by remember { mutableStateOf(false) }
                    val isStatusError = !reconnectStatus.isNullOrBlank() && (
                        capsuleText.contains("异常") ||
                        capsuleText.contains("报错") ||
                        capsuleText.contains("失败") ||
                        capsuleText.contains("错误") ||
                        capsuleText.contains("Error", ignoreCase = true) ||
                        capsuleText.contains("HTTP", ignoreCase = true)
                    )
                    // 状态文本包含多行、超长详细报错/URL信息或报错状态时提供展开功能，无多余内容不给展开键
                    val hasDetailedExpandableContent = !hasThinkingContent && (capsuleText.contains("\n") || capsuleText.length > 36 || isStatusError)
                    val canExpandStatus = hasDetailedExpandableContent || isStatusExpanded
                    val capsuleShape = RoundedCornerShape(16.dp)
                    val maxBubbleWidth = if (isStatusExpanded || isStatusError) 380.dp else 320.dp
                    val maxLinesCount = when {
                        isStatusExpanded -> 16
                        isStatusError -> 4
                        else -> 1
                    }
                    val enableSoftWrap = isStatusExpanded || isStatusError
                    Surface(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 34.dp)
                            .widthIn(max = maxBubbleWidth)
                            .animateContentSize()
                            .clip(capsuleShape)
                            .then(
                                if (hasThinking && hasThinkingContent) {
                                    Modifier.echoShapeClick(shape = capsuleShape) {
                                        showThinking = !showThinking
                                    }
                                } else if (canExpandStatus) {
                                    Modifier.echoShapeClick(shape = capsuleShape) {
                                        isStatusExpanded = !isStatusExpanded
                                    }
                                } else Modifier
                            ),
                        color = if (isStatusError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f) else thinkingBubbleColor,
                        contentColor = if (isStatusError) MaterialTheme.colorScheme.error else thinkingHeaderColor,
                        shape = capsuleShape,
                        border = BorderStroke(
                            1.dp,
                            if (isStatusError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else glass.outlineSelected.copy(alpha = 0.72f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = if (isStatusExpanded || isStatusError) Alignment.Top else Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .then(if (isStatusExpanded || isStatusError) Modifier.padding(top = 1.dp) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isStatusError) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = "连接报错",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                } else if (isConnecting || isThinkingActive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        strokeWidth = 1.8.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    // 统一图标样式，删除机器人头像样式 (SmartToy)，全状态保持一致的 Psychology 图标
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = thinkingHeaderColor
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .then(
                                        if (!enableSoftWrap) Modifier.horizontalScroll(rememberScrollState())
                                        else Modifier
                                    )
                            ) {
                                Text(
                                    text = capsuleText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 16.sp
                                    ),
                                    color = if (isStatusError) MaterialTheme.colorScheme.error else thinkingHeaderColor,
                                    maxLines = maxLinesCount,
                                    softWrap = enableSoftWrap,
                                    overflow = TextOverflow.Clip
                                )
                            }
                            if (hasThinking && hasThinkingContent) {
                                Icon(
                                    imageVector = if (showThinking) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (showThinking) "收起" else "展开",
                                    modifier = Modifier.size(16.dp),
                                    tint = thinkingHeaderColor.copy(alpha = 0.78f)
                                )
                            } else if (canExpandStatus && hasDetailedExpandableContent) {
                                Icon(
                                    imageVector = if (isStatusExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isStatusExpanded) "收起完整信息" else "展开完整信息",
                                    modifier = Modifier.size(16.dp),
                                    tint = (if (isStatusError) MaterialTheme.colorScheme.error else thinkingHeaderColor).copy(alpha = 0.78f)
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
                                .padding(horizontal = 2.dp, vertical = 4.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { showThinking = !showThinking }
                                    )
                                },
                            color = glass.controlSelected.copy(alpha = 0.6f),
                            contentColor = thinkingContentColor,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, glass.outlineSelected.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "思考内容详情",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = thinkingHeaderColor
                                        )

                                        if (hasTranslation) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(glass.control.copy(alpha = 0.7f))
                                                    .padding(2.dp),
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (showTranslated) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    modifier = Modifier.clickable { showTranslated = true }
                                                ) {
                                                    Text(
                                                        text = "译文",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = if (showTranslated) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (showTranslated) MaterialTheme.colorScheme.onPrimary else thinkingHeaderColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (!showTranslated) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                    modifier = Modifier.clickable { showTranslated = false }
                                                ) {
                                                    Text(
                                                        text = "原文",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = if (!showTranslated) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (!showTranslated) MaterialTheme.colorScheme.onPrimary else thinkingHeaderColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (!isGenerating) {
                                            if (translatingThinking) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(end = 4.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(13.dp),
                                                        strokeWidth = 1.6.dp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "翻译中...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                IconButton(
                                                    onClick = { onTranslateThinking?.invoke(message) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Translate,
                                                        contentDescription = if (hasTranslation) "重新翻译思考" else "翻译思考为中文",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = if (hasTranslation) MaterialTheme.colorScheme.primary else thinkingHeaderColor.copy(alpha = 0.78f)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                val textToCopy = if (hasTranslation && showTranslated) message.translatedThinking ?: "" else message.thinkingContent ?: ""
                                                clipboardManager.setText(AnnotatedString(textToCopy))
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "复制思考",
                                                modifier = Modifier.size(14.dp),
                                                tint = thinkingHeaderColor.copy(alpha = 0.78f)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val displayThinking = if (hasTranslation && showTranslated) message.translatedThinking ?: "" else (message.thinkingContent ?: "")
                                MarkdownText(
                                    content = displayThinking,
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
                        var showErrorDetails by remember(message.id) { mutableStateOf(false) }
                        val errorSummary = remember(message.content) {
                            val lines = message.content.lines().map { it.trim() }.filter { it.isNotBlank() }
                            val detailLine = lines.firstOrNull { it != "请求失败" && !it.startsWith("[输出已被中断") && !it.startsWith("可以检查") }
                            when {
                                !detailLine.isNullOrBlank() -> detailLine
                                lines.isNotEmpty() -> lines.first()
                                else -> "请求发生异常"
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { showErrorDetails = !showErrorDetails }
                                    )
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.40f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showErrorDetails = !showErrorDetails },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = "错误",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = if (showErrorDetails) "错误信息详情" else errorSummary,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        maxLines = if (showErrorDetails) 1 else 4,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showErrorDetails = !showErrorDetails },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showErrorDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (showErrorDetails) "收起错误" else "展开错误",
                                            tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showErrorDetails,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(modifier = Modifier.padding(top = 8.dp)) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        MarkdownText(
                                            content = message.content,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        MessageContent(textColor)
                    }

                    // 工具调用留痕展示 (支持展开查看执行摘要与注入的上下文详情)
                    val toolCallsList = remember(message.toolCalls) {
                        if (message.toolCalls.isNullOrBlank()) {
                            emptyList<ToolCallRecord>()
                        } else {
                            try {
                                val gson = com.google.gson.Gson()
                                val type = com.google.gson.reflect.TypeToken.getParameterized(
                                    List::class.java, ToolCallRecord::class.java
                                ).type
                                gson.fromJson<List<ToolCallRecord>>(message.toolCalls, type) ?: emptyList()
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    if (toolCallsList.isNotEmpty()) {
                        ToolCallsFooter(toolCalls = toolCallsList)
                    }

                    MessageFooter(
                        isUser = false,
                        message = message,
                        variantInfo = variantInfo,
                        onVariantSelected = onVariantSelected,
                        onCopy = onCopy,
                        onQuote = onQuote,
                        onBranch = onBranch,
                        onRegenerate = onRegenerate,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onTogglePin = onTogglePin?.let { cb -> { cb(message) } },
                        onToggleExclude = onToggleExclude?.let { cb -> { cb(message) } },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!isGenerating && (message.content.isNotBlank() || hasThinking)) {
                        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val dividerColor = if (isDarkTheme) {
                            Color.White.copy(alpha = 0.16f)
                        } else {
                            Color.Black.copy(alpha = 0.12f)
                        }
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 4.dp),
                            thickness = 1.dp,
                            color = dividerColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MessageFooter(
    isUser: Boolean,
    message: Message,
    variantInfo: VariantInfo? = null,
    onVariantSelected: ((String, Int) -> Unit)? = null,
    onCopy: () -> Unit,
    onQuote: (() -> Unit)? = null,
    onBranch: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onTogglePin: (() -> Unit)? = null,
    onToggleExclude: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val thinkingTokensInThinkingBubble = message.role == "assistant" &&
        (!message.thinkingContent.isNullOrBlank() || message.thinkingTokens > 0)
    val responseTimeInThinkingBubble = thinkingTokensInThinkingBubble

    Row(
        modifier = modifier.padding(top = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧元数据：时间戳、Token 等支持水平横向滚动
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (message.isPinned) {
                MessageMetaText(
                    text = "📌已固定",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (message.isExcluded) {
                MessageMetaText(
                    text = "🚫已排除",
                    color = MaterialTheme.colorScheme.error
                )
            }

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

        // 右侧操作栏：版本切换器与操作按钮同一行排布
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (variantInfo != null && variantInfo.total > 1 && onVariantSelected != null) {
                VariantSwitcher(
                    info = variantInfo,
                    onSelect = { index -> onVariantSelected(variantInfo.groupId, index) }
                )
                Spacer(modifier = Modifier.width(2.dp))
            }

            FooterIconButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = "复制",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onCopy
            )

            if (!isUser && onBranch != null) {
                FooterIconButton(
                    icon = Icons.AutoMirrored.Filled.AltRoute,
                    contentDescription = "分支对话",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onBranch
                )
            } else if (onQuote != null) {
                FooterIconButton(
                    icon = Icons.Default.FormatQuote,
                    contentDescription = "引用",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onQuote
                )
            }

            if (!isUser && onRegenerate != null) {
                FooterIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = "重新生成",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onRegenerate
                )
            }

            // 需求 3：将编辑、固定、排除功能收纳进二级菜单（同时整合删除操作，使工具栏紧凑优雅）
            var showMoreMenu by remember { mutableStateOf(false) }
            val hasSecondaryActions = onEdit != null || onTogglePin != null || onToggleExclude != null || onDelete != null

            if (hasSecondaryActions) {
                Box {
                    FooterIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "更多操作",
                        tint = if (message.isPinned || message.isExcluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { showMoreMenu = true }
                    )

                    EchoGlassDropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text(if (isUser) "重新编辑" else "编辑回复", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onEdit()
                                }
                            )
                        }

                        if (onTogglePin != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (message.isPinned) "取消固定" else "固定到上下文",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (message.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = null,
                                        tint = if (message.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onTogglePin()
                                }
                            )
                        }

                        if (onToggleExclude != null) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (message.isExcluded) "恢复参与上下文" else "从上下文中排除",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (message.isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (message.isExcluded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = if (message.isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onToggleExclude()
                                }
                            )
                        }

                        if (onDelete != null) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            DropdownMenuItem(
                                text = { Text("删除本条", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MessageMetaText(
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
internal fun FooterIconButton(
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
internal fun VariantSwitcher(
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
internal fun ChatAvatar(
    isUser: Boolean,
    avatarRevision: Int = 0,
    apiConfigId: Long? = null,
    customAvatarUri: String? = null
) {
    val context = LocalContext.current
    val userAvatarBitmap = if (isUser) remember(context) { AvatarManager.getAvatarBitmap(context) } else null
    val customAvatarBitmap = if (!isUser && !customAvatarUri.isNullOrBlank()) {
        remember(customAvatarUri) {
            runCatching {
                val uri = Uri.parse(customAvatarUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    } else null
    val modelAvatarBitmap = if (!isUser && customAvatarBitmap == null) {
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
            if (customAvatarBitmap != null) {
                Image(
                    bitmap = customAvatarBitmap.asImageBitmap(),
                    contentDescription = "角色头像",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else if (modelAvatarBitmap != null) {
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
internal fun formatTime(ms: Long): String {
    return when {
        ms < 1000 -> "${ms}ms"
        ms < 60000 -> "${ms / 1000}s"
        else -> "${ms / 60000}m${(ms % 60000) / 1000}s"
    }
}

internal fun formatMessageClock(timestamp: Long): String {
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
internal fun AttachmentGroupBubble(
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
                            Icons.AutoMirrored.Filled.OpenInNew,
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
    var copied by remember { mutableStateOf(false) }

    EchoGlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 440.dp),
        title = {
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
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
        },
        buttons = {
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
    )
}

@Composable
fun ToolCallsFooter(
    toolCalls: List<ToolCallRecord>,
    modifier: Modifier = Modifier
) {
    if (toolCalls.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    var selectedRecordForDialog by remember { mutableStateOf<ToolCallRecord?>(null) }
    val glass = echoGlassPalette()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = glass.control.copy(alpha = if (isDarkTheme) 0.5f else 0.7f),
        border = BorderStroke(0.8.dp, glass.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 头部摘要栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = "成功调用 ${toolCalls.size} 项系统与联网工具",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "收起留痕" else "查看留痕",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 展开的工具调用记录列表
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    toolCalls.forEach { record ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedRecordForDialog = record },
                            shape = RoundedCornerShape(10.dp),
                            color = glass.controlSelected.copy(alpha = 0.4f),
                            border = BorderStroke(0.6.dp, glass.outlineSelected.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val icon = when (record.toolType) {
                                    "WEATHER" -> Icons.Default.Cloud
                                    "TIME_CALENDAR" -> Icons.Default.Schedule
                                    "HEALTH" -> Icons.Default.DirectionsWalk
                                    "DEVICE_HARDWARE" -> Icons.Default.Smartphone
                                    "LOCATION" -> Icons.Default.LocationOn
                                    "JINA_READER" -> Icons.Default.MenuBook
                                    else -> Icons.Default.Search
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = record.toolName,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = record.toolName,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        ) {
                                            Text(
                                                text = "执行成功",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (record.summary.isNotBlank()) {
                                        Text(
                                            text = record.summary,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { selectedRecordForDialog = record },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 28.dp)
                                ) {
                                    Text("详情", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedRecordForDialog != null) {
        ToolCallDetailDialog(
            record = selectedRecordForDialog!!,
            onDismiss = { selectedRecordForDialog = null }
        )
    }
}

@Composable
fun ToolCallDetailDialog(
    record: ToolCallRecord,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    EchoGlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 440.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(record.toolName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("执行摘要：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(record.summary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("工具获取并注入模型的完整上下文数据：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = record.detailContent,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(10.dp)
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
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(record.detailContent))
                        android.widget.Toast.makeText(context, "已复制工具数据到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("复制数据")
                }
            }
        }
    )
}

// 需求 6：模型回复时排队消息悬浮卡片 (UI 严格按照 media_1789390149204.png 设计落地)
@Composable
internal fun MessageQueueCard(
    queue: List<QueuedMessage>,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onRecall: (String) -> Unit,
    onEdit: (QueuedMessage) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit
) {
    val glass = echoGlassPalette()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, glass.outline.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶部标题行: 排队中 (N) + 暂停/开启图标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "排队中 (${queue.size})${if (isPaused) " · 已暂停" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "开启自动发送" else "暂停自动发送（只排队）",
                        tint = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val queueReorderState = rememberSmoothReorderState()

            // 队列列表项
            queue.forEachIndexed { index, msg ->
                val isActive = queueReorderState.isItemActive(index)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isActive) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    border = BorderStroke(
                        if (isActive) 1.5.dp else 0.8.dp,
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.75f) else glass.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .reorderItem(queueReorderState, index, msg.id)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 左侧拖动手柄 (按住六个点上下拖拽调序，带平滑动画)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else Color.Transparent)
                                .reorderDragHandle(
                                    state = queueReorderState,
                                    index = { index },
                                    key = { msg.id },
                                    keys = { queue.map { it.id } },
                                    listSize = { queue.size },
                                    onMove = onMove
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "按住上下拖动调整顺序",
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // 中间文本内容预览
                        Text(
                            text = msg.content.ifBlank { "[附件消息]" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // 右侧操作区：编辑(铅笔)、撤回回填输入框(✕)（需求 4：删除无用发送键，仅保留编辑与撤回）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { onEdit(msg) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "编辑排队消息",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRecall(msg.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "撤回排队消息至输入框",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
