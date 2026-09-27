package com.aiassistant.ui.components

import com.aiassistant.ui.components.markdown.LatexUnicodeConverter
import com.aiassistant.ui.components.markdown.MarkdownInlineParser
import com.aiassistant.ui.components.markdown.MarkdownTableBlock
import com.aiassistant.ui.components.markdown.parseMarkdownTable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onCitationClick: ((Int) -> Unit)? = null
) {
    Column(modifier = modifier) {
        val lines = remember(content) { content.split("\n") }
        var index = 0

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()

            when {
                // 代码块开始 ```lang
                line.trimStart().startsWith("```") -> {
                    val codeBlockLanguage = line.trimStart().removePrefix("```").trim()
                    val codeBlockContent = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trimStart().startsWith("```")) {
                        codeBlockContent.appendLine(lines[index])
                        index++
                    }
                    CodeBlock(
                        code = codeBlockContent.toString().trimEnd(),
                        language = codeBlockLanguage
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (index < lines.size) index++
                }

                // 独立 LaTeX 数学块 $$...$$ 或 \[...\] 或 \begin{...}
                trimmed.startsWith("$$") || trimmed.startsWith("\\[") || trimmed.startsWith("\\begin{") -> {
                    val mathContent = StringBuilder()
                    if (trimmed.startsWith("\\begin{")) {
                        val envName = trimmed.substringAfter("\\begin{").substringBefore("}")
                        val endTag = "\\end{$envName}"
                        if (trimmed.contains(endTag)) {
                            mathContent.append(trimmed)
                            index++
                        } else {
                            mathContent.appendLine(trimmed)
                            index++
                            while (index < lines.size && !lines[index].contains(endTag)) {
                                mathContent.appendLine(lines[index])
                                index++
                            }
                            if (index < lines.size) {
                                mathContent.append(lines[index])
                                index++
                            }
                        }
                    } else if (trimmed.startsWith("\\[")) {
                        if (trimmed.endsWith("\\]") && trimmed.length > 4) {
                            mathContent.append(trimmed.removePrefix("\\[").removeSuffix("\\]").trim())
                            index++
                        } else {
                            mathContent.appendLine(trimmed.removePrefix("\\[").trim())
                            index++
                            while (index < lines.size && !lines[index].trim().endsWith("\\]")) {
                                mathContent.appendLine(lines[index])
                                index++
                            }
                            if (index < lines.size) {
                                mathContent.append(lines[index].trim().removeSuffix("\\]").trim())
                                index++
                            }
                        }
                    } else {
                        if (trimmed.endsWith("$$") && trimmed.length > 4) {
                            mathContent.append(trimmed.removePrefix("$$").removeSuffix("$$").trim())
                            index++
                        } else {
                            mathContent.appendLine(trimmed.removePrefix("$$").trim())
                            index++
                            while (index < lines.size && !lines[index].trim().endsWith("$$")) {
                                mathContent.appendLine(lines[index])
                                index++
                            }
                            if (index < lines.size) {
                                mathContent.append(lines[index].trim().removeSuffix("$$").trim())
                                index++
                            }
                        }
                    }
                    MathBlock(formula = mathContent.toString().trim())
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 标题 1-6 级（含特定关键词加粗、加大字号、斜体强化）
                line.startsWith("# ") -> {
                    val text = line.removePrefix("# ")
                    renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), color = color, topPad = 8.dp, bottomPad = 4.dp)
                    index++
                }
                line.startsWith("## ") -> {
                    val text = line.removePrefix("## ")
                    renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = color, topPad = 6.dp, bottomPad = 3.dp)
                    index++
                }
                line.startsWith("### ") -> {
                    val text = line.removePrefix("### ")
                    renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 18.5.sp), color = color, topPad = 5.dp, bottomPad = 3.dp)
                    index++
                }
                line.startsWith("#### ") -> {
                    val text = line.removePrefix("#### ")
                    renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = color, topPad = 4.dp, bottomPad = 2.dp)
                    index++
                }
                line.startsWith("##### ") -> {
                    val text = line.removePrefix("##### ")
                    renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = color, topPad = 3.dp, bottomPad = 2.dp)
                    index++
                }
                line.startsWith("###### ") -> {
                    val text = line.removePrefix("###### ")
                    renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold), color = color, topPad = 2.dp, bottomPad = 2.dp)
                    index++
                }

                // 表格渲染
                parseMarkdownTable(lines, index) != null -> {
                    val table = parseMarkdownTable(lines, index)!!
                    MarkdownTableBlock(table = table, color = color)
                    Spacer(modifier = Modifier.height(8.dp))
                    index += table.consumedLines
                }

                // 末尾参考资料项（如 - [1] 标题 或 * [1] 标题 或 [1] 标题：前面不要有圆点，正常大小显示）
                isReferenceListItem(line) -> {
                    val cleanRefLine = cleanReferenceItemLine(line)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InlineMarkdownText(
                            text = parseInlineMarkdown(cleanRefLine, isReferenceItem = true),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            onCitationClick = onCitationClick
                        )
                    }
                    index++
                }

                // 普通无序列表项（无前缀圆点·，保持自然缩进与正文字号一致）
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val indent = line.length - line.trimStart().length
                    val itemContent = line.trimStart().removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(start = (8 + indent * 4).dp, top = 2.dp)) {
                        InlineMarkdownText(
                            text = parseInlineMarkdown(itemContent),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color
                        )
                    }
                    index++
                }

                    // 有序列表
                    line.trimStart().matches(Regex("^\\d+\\.\\s+.*")) -> {
                        val number = line.trimStart().substringBefore(".")
                        val itemContent = line.trimStart().substringAfter(". ")
                        Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                            Text(
                                text = "$number.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = color
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InlineMarkdownText(
                                text = parseInlineMarkdown(itemContent),
                                style = MaterialTheme.typography.bodyLarge,
                                color = color
                            )
                        }
                        index++
                    }

                    // 引用
                    line.startsWith("> ") -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InlineMarkdownText(
                                text = parseInlineMarkdown(line.removePrefix("> ")),
                                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                                color = color.copy(alpha = 0.8f)
                            )
                        }
                        index++
                    }

                    // 分割线
                    line.trim() == "---" || line.trim() == "***" -> {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        index++
                    }

                    // 空行
                    line.isBlank() -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        index++
                    }

                    // 普通文本
                    else -> {
                        // 检查普通单行中是否为核心关键词（参考资料/要点概括/详细解答）单独成行
                        if (isSpecialKeywordTitle(line.trim())) {
                            renderHeadingText(
                                text = line.trim(),
                                defaultStyle = MaterialTheme.typography.titleMedium,
                                color = color,
                                topPad = 6.dp,
                                bottomPad = 3.dp
                            )
                            index++
                        } else {
                            // 跨行格式保护：若当前行包含未闭合的加粗/斜体/删除线等标记，合并后续连续文本行
                            val mergedLines = StringBuilder(line)
                            index++
                            while (index < lines.size && hasUnclosedInlineFormatting(mergedLines.toString()) && !isBlockBoundaryLine(lines[index])) {
                                mergedLines.append("\n").append(lines[index])
                                index++
                            }
                            InlineMarkdownText(
                                text = parseInlineMarkdown(mergedLines.toString()),
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                                modifier = Modifier.padding(vertical = 2.dp),
                                onCitationClick = onCitationClick
                            )
                        }
                    }
                }
            }
        }
    }

/**
 * 校验文本行中是否存在未闭合的 Markdown 标记（如星号、下划线、删除线或 HTML 标签）
 */
private fun hasUnclosedInlineFormatting(text: String): Boolean {
    val norm = text.replace('＊', '*')
    fun countOccurrences(sub: String): Int {
        var count = 0
        var idx = 0
        while (idx < norm.length) {
            val found = norm.indexOf(sub, idx)
            if (found != -1) {
                count++
                idx = found + sub.length
            } else break
        }
        return count
    }
    val count3 = countOccurrences("***")
    val count2 = countOccurrences("**")
    val count1 = norm.count { it == '*' }
    val countTilde = countOccurrences("~~")
    val countFontOpen = norm.split(Regex("<font", RegexOption.IGNORE_CASE)).size - 1
    val countFontClose = norm.split(Regex("</font>", RegexOption.IGNORE_CASE)).size - 1
    val countSpanOpen = norm.split(Regex("<span", RegexOption.IGNORE_CASE)).size - 1
    val countSpanClose = norm.split(Regex("</span>", RegexOption.IGNORE_CASE)).size - 1

    return (count3 % 2 != 0) || (count2 % 2 != 0) || (count1 % 2 != 0) || (countTilde % 2 != 0) ||
        (countFontOpen > countFontClose) || (countSpanOpen > countSpanClose)
}

/**
 * 校验当前行是否为独立 Markdown 块级元素边界（标题、列表、代码块、引用等），跨行合并不得越界
 */
private fun isBlockBoundaryLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.isBlank() ||
        line.trimStart().startsWith("```") ||
        trimmed.startsWith("$$") || trimmed.startsWith("\\[") || trimmed.startsWith("\\begin{") ||
        line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ") ||
        line.startsWith("#### ") || line.startsWith("##### ") || line.startsWith("###### ") ||
        isReferenceListItem(line) ||
        line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") ||
        line.trimStart().matches(Regex("^\\d+\\.\\s+.*")) ||
        line.startsWith("> ") ||
        trimmed == "---" || trimmed == "***"
}

/**
 * 判断是否为特定关键词标题（加粗、加大字号、斜体）
 */
private fun isSpecialKeywordTitle(text: String): Boolean {
    val clean = text.replace(Regex("""[#*_\s：:]"""), "")
    return clean.contains("参考资料") ||
        clean.contains("要点概括") ||
        clean.contains("详细解答") ||
        clean.contains("资料来源") ||
        clean.contains("要点总结") ||
        clean.contains("核心解答")
}

@Composable
private fun renderHeadingText(
    text: String,
    defaultStyle: TextStyle,
    color: Color,
    topPad: Dp,
    bottomPad: Dp
) {
    val isSpecial = isSpecialKeywordTitle(text)
    val finalStyle = if (isSpecial) {
        MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic
        )
    } else {
        defaultStyle.copy(fontWeight = FontWeight.Bold)
    }
    val finalColor = if (isSpecial) MaterialTheme.colorScheme.primary else color

    InlineMarkdownText(
        text = parseInlineMarkdown(text),
        style = finalStyle,
        color = finalColor,
        modifier = Modifier.padding(top = topPad, bottom = bottomPad)
    )
}

/**
 * 判断是否为末尾参考资料项，例如：
 * - [1] 标题 (URL)
 * * [1] 标题
 * [1] 标题 http://...
 */
private fun isReferenceListItem(line: String): Boolean {
    val trimmed = line.trimStart().removePrefix("- ").removePrefix("* ").trim()
    return trimmed.matches(Regex("""^\[\^?\d+\].*"""))
}

private fun cleanReferenceItemLine(line: String): String {
    return line.trimStart().removePrefix("- ").removePrefix("* ").trim()
}

/**
 * LaTeX 数学公式块
 */
@Composable
private fun MathBlock(
    formula: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val parsedFormula = remember(formula) { parseLaTeXToUnicode(formula) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Functions,
                        contentDescription = "LaTeX公式",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "公式",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(formula)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制公式",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = parsedFormula,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Composable
internal fun InlineMarkdownText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    onCitationClick: ((Int) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val hasCitations = remember(text) { text.getStringAnnotations(tag = "CITATION", start = 0, end = text.length).isNotEmpty() }
    val hasUrls = remember(text) { text.getStringAnnotations(tag = "URL", start = 0, end = text.length).isNotEmpty() }

    if (!hasCitations && !hasUrls) {
        Text(
            text = text,
            style = style.copy(color = color),
            modifier = modifier
        )
    } else {
        ClickableText(
            text = text,
            style = style.copy(color = color),
            modifier = modifier,
            onClick = { offset ->
                text.getStringAnnotations(tag = "CITATION", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { annotation ->
                        val id = annotation.item.toIntOrNull() ?: 1
                        onCitationClick?.invoke(id)
                    }
                text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()
                    ?.let { annotation ->
                        runCatching { uriHandler.openUri(annotation.item) }
                    }
            }
        )
    }
}

// =========================================================================
// 向下兼容重导出函数（保障既有模块及 66 项单元测试无痛调用）
// =========================================================================

fun parseInlineMarkdown(text: String, isReferenceItem: Boolean = false): AnnotatedString =
    MarkdownInlineParser.parseInlineMarkdown(text, isReferenceItem)

fun cleanLeadingStarArtifacts(raw: String): String =
    MarkdownInlineParser.cleanLeadingStarArtifacts(raw)

fun parseLaTeXToUnicode(raw: String): String =
    LatexUnicodeConverter.parseLaTeXToUnicode(raw)

fun parseInlineColor(raw: String?): Color? =
    com.aiassistant.ui.components.markdown.MarkdownColorUtils.parseInlineColor(raw)

fun decodeHtmlEntities(input: String): String =
    com.aiassistant.ui.components.markdown.MarkdownColorUtils.decodeHtmlEntities(input)

fun highlightSyntax(code: String, language: String, isDark: Boolean): AnnotatedString =
    com.aiassistant.ui.components.markdown.highlightSyntax(code, language, isDark)

@Composable
fun CodeBlock(code: String, language: String = "", modifier: Modifier = Modifier) {
    com.aiassistant.ui.components.markdown.CodeBlock(code = code, language = language, modifier = modifier)
}