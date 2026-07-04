package com.aiassistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    SelectionContainer {
        Column(modifier = modifier) {
            val lines = content.split("\n")
            var index = 0

            while (index < lines.size) {
                val line = lines[index]
                when {
                    // 代码块开始
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
                    // 标题
                    line.startsWith("# ") -> {
                        Text(
                            text = line.removePrefix("# "),
                            style = MaterialTheme.typography.titleLarge,
                            color = color,
                            modifier = Modifier.padding(top = 7.dp, bottom = 4.dp)
                        )
                        index++
                    }
                    line.startsWith("## ") -> {
                        Text(
                            text = line.removePrefix("## "),
                            style = MaterialTheme.typography.titleMedium,
                            color = color,
                            modifier = Modifier.padding(top = 6.dp, bottom = 3.dp)
                        )
                        index++
                    }
                    line.startsWith("### ") -> {
                        Text(
                            text = line.removePrefix("### "),
                            style = MaterialTheme.typography.titleSmall,
                            color = color,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        index++
                    }
                    line.startsWith("#### ") -> {
                        Text(
                            text = line.removePrefix("#### "),
                            style = MaterialTheme.typography.labelLarge,
                            color = color,
                            modifier = Modifier.padding(top = 3.dp, bottom = 2.dp)
                        )
                        index++
                    }
                    parseMarkdownTable(lines, index) != null -> {
                        val table = parseMarkdownTable(lines, index)!!
                        MarkdownTableBlock(table = table, color = color)
                        Spacer(modifier = Modifier.height(8.dp))
                        index += table.consumedLines
                    }
                    // 列表项
                    line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                        val indent = line.length - line.trimStart().length
                        Row(modifier = Modifier.padding(start = (8 + indent * 4).dp, top = 2.dp)) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = color
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InlineMarkdownText(
                                text = parseInlineMarkdown(line.trimStart().removePrefix("- ").removePrefix("* ")),
                                style = MaterialTheme.typography.bodyLarge,
                                color = color
                            )
                        }
                        index++
                    }
                    // 有序列表
                    line.trimStart().matches(Regex("^\\d+\\. .*")) -> {
                        val number = line.trimStart().substringBefore(".")
                        Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                            Text(
                                text = "$number.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = color
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            InlineMarkdownText(
                                text = parseInlineMarkdown(line.trimStart().substringAfter(". ")),
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
                        Divider(
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
                    // 普通文本（支持行内样式）
                    else -> {
                        InlineMarkdownText(
                            text = parseInlineMarkdown(line),
                            style = MaterialTheme.typography.bodyLarge,
                            color = color,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        index++
                    }
                }
            }
        }
    }
}

private fun parseHtmlTagColor(tag: String): Color? {
    val colorAttr = Regex("""color\s*=\s*["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)
        .find(tag)
        ?.groupValues
        ?.getOrNull(1)
    val styleColor = Regex("""color\s*:\s*([^;"'>]+)""", RegexOption.IGNORE_CASE)
        .find(tag)
        ?.groupValues
        ?.getOrNull(1)
    return parseInlineColor(colorAttr ?: styleColor)
}

private fun parseInlineColor(raw: String?): Color? {
    val value = raw?.trim()?.trim('"', '\'')?.lowercase().orEmpty()
    if (value.isBlank()) return null
    val normalized = when (value) {
        "red" -> "#DC2626"
        "orange" -> "#EA580C"
        "yellow" -> "#CA8A04"
        "green" -> "#16A34A"
        "blue" -> "#2563EB"
        "purple" -> "#7C3AED"
        "pink" -> "#DB2777"
        "gray", "grey" -> "#64748B"
        "black" -> "#111827"
        "white" -> "#FFFFFF"
        else -> value
    }
    val cssColor = when {
        normalized.matches(Regex("""#[0-9a-fA-F]{6}""")) -> normalized
        normalized.matches(Regex("""#[0-9a-fA-F]{8}""")) -> normalized
        normalized.matches(Regex("""0x[0-9a-fA-F]{8}""")) -> "#${normalized.drop(2)}"
        normalized.matches(Regex("""[0-9a-fA-F]{6}""")) -> "#$normalized"
        normalized.matches(Regex("""[0-9a-fA-F]{8}""")) -> "#$normalized"
        else -> return null
    }
    return runCatching { Color(android.graphics.Color.parseColor(cssColor)) }.getOrNull()
}

private data class MarkdownTable(
    val headers: List<String>,
    val rows: List<List<String>>,
    val consumedLines: Int
)

private fun parseMarkdownTable(lines: List<String>, startIndex: Int): MarkdownTable? {
    if (startIndex + 2 >= lines.size) return null
    val headerLine = lines[startIndex].trim()
    val dividerLine = lines[startIndex + 1].trim()
    if (!headerLine.startsWith("|") || !headerLine.endsWith("|")) return null
    if (!isMarkdownTableDivider(dividerLine)) return null

    val headers = splitTableRow(headerLine)
    if (headers.size < 2) return null

    val rows = mutableListOf<List<String>>()
    var index = startIndex + 2
    while (index < lines.size) {
        val line = lines[index].trim()
        if (!line.startsWith("|") || !line.endsWith("|")) break
        rows += normalizeTableCells(splitTableRow(line), headers.size)
        index++
    }

    if (rows.isEmpty()) return null
    return MarkdownTable(headers = headers, rows = rows, consumedLines = index - startIndex)
}

private fun isMarkdownTableDivider(line: String): Boolean {
    if (!line.startsWith("|") || !line.endsWith("|")) return false
    val cells = splitTableRow(line)
    return cells.size >= 2 && cells.all { cell ->
        cell.matches(Regex(":?-{3,}:?"))
    }
}

private fun splitTableRow(line: String): List<String> {
    val trimmed = line.trim()
    val content = trimmed
        .removePrefix("|")
        .removeSuffix("|")
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var inCodeSpan = false
    var index = 0

    while (index < content.length) {
        val char = content[index]
        val next = content.getOrNull(index + 1)
        when {
            char == '\\' && next == '|' -> {
                current.append('|')
                index += 2
            }
            char == '`' -> {
                inCodeSpan = !inCodeSpan
                current.append(char)
                index++
            }
            char == '|' && !inCodeSpan -> {
                cells += current.toString().trim()
                current.clear()
                index++
            }
            else -> {
                current.append(char)
                index++
            }
        }
    }
    cells += current.toString().trim()
    return cells
}

private fun normalizeTableCells(cells: List<String>, columnCount: Int): List<String> {
    if (columnCount <= 0) return cells
    return when {
        cells.size == columnCount -> cells
        cells.size < columnCount -> cells + List(columnCount - cells.size) { "" }
        else -> cells.take(columnCount - 1) + cells.drop(columnCount - 1).joinToString(" | ")
    }
}

@Composable
private fun MarkdownTableBlock(
    table: MarkdownTable,
    color: androidx.compose.ui.graphics.Color
) {
    if (table.headers.size == 2) {
        MobileTwoColumnTable(table = table, color = color)
    } else {
        ScrollableMarkdownTable(table = table, color = color)
    }
}

@Composable
private fun MobileTwoColumnTable(
    table: MarkdownTable,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        table.rows.forEach { row ->
            val title = row.getOrNull(0).orEmpty()
            val detail = row.getOrNull(1).orEmpty()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    InlineMarkdownText(
                        text = parseInlineMarkdown(title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    InlineMarkdownText(
                        text = parseInlineMarkdown(detail),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrollableMarkdownTable(
    table: MarkdownTable,
    color: androidx.compose.ui.graphics.Color
) {
    val scrollState = rememberScrollState()
    val rows = remember(table) {
        table.rows.map { normalizeTableCells(it, table.headers.size) }
    }
    val columnWidths = remember(table) {
        table.headers.indices.map { index ->
            val maxLength = (listOf(table.headers) + rows)
                .map { it.getOrNull(index).orEmpty().length }
                .maxOrNull()
                ?: 0
            (maxLength * 7 + 52).dp.coerceIn(112.dp, 260.dp)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .horizontalScroll(scrollState)
    ) {
        TableRow(cells = table.headers, columnWidths = columnWidths, color = color, isHeader = true)
        rows.forEach { row ->
            TableRow(cells = row, columnWidths = columnWidths, color = color)
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    columnWidths: List<Dp>,
    color: androidx.compose.ui.graphics.Color,
    isHeader: Boolean = false
) {
    Row(modifier = Modifier.width(columnWidths.fold(0.dp) { total, width -> total + width })) {
        cells.forEachIndexed { index, cell ->
            Surface(
                modifier = Modifier
                    .width(columnWidths.getOrElse(index) { 140.dp })
                    .heightIn(min = 42.dp),
                color = if (isHeader) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                },
                border = BorderStroke(
                    width = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)
                )
            ) {
                InlineMarkdownText(
                    text = parseInlineMarkdown(cell),
                    style = if (isHeader) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                    color = if (isHeader) MaterialTheme.colorScheme.primary else color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun InlineMarkdownText(
    text: AnnotatedString,
    style: TextStyle,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    ClickableText(
        text = text,
        style = style.copy(color = color),
        modifier = modifier,
        onClick = { offset ->
            text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()
                ?.let { annotation ->
                    runCatching { uriHandler.openUri(annotation.item) }
                }
        }
    )
}

@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // 语言标签和复制按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (language.isNotEmpty()) {
                    Text(
                        text = language,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(modifier = Modifier)
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制代码",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 代码内容
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

// 行内Markdown解析 - 支持粗体、斜体、行内代码
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("{#", i) -> {
                    val separator = text.indexOf('|', i + 2)
                    val end = if (separator != -1) text.indexOf('}', separator + 1) else -1
                    val colorValue = if (separator != -1) text.substring(i + 1, separator) else ""
                    val parsedColor = parseInlineColor(colorValue)
                    if (separator != -1 && end != -1 && parsedColor != null) {
                        withStyle(SpanStyle(color = parsedColor)) {
                            append(parseInlineMarkdown(text.substring(separator + 1, end)))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("<font", i, ignoreCase = true) -> {
                    val openEnd = text.indexOf('>', i)
                    val closeStart = text.indexOf("</font>", if (openEnd != -1) openEnd + 1 else i, ignoreCase = true)
                    val openTag = if (openEnd != -1) text.substring(i, openEnd + 1) else ""
                    val parsedColor = parseHtmlTagColor(openTag)
                    if (openEnd != -1 && closeStart != -1 && parsedColor != null) {
                        withStyle(SpanStyle(color = parsedColor)) {
                            append(parseInlineMarkdown(text.substring(openEnd + 1, closeStart)))
                        }
                        i = closeStart + "</font>".length
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text.startsWith("<span", i, ignoreCase = true) -> {
                    val openEnd = text.indexOf('>', i)
                    val closeStart = text.indexOf("</span>", if (openEnd != -1) openEnd + 1 else i, ignoreCase = true)
                    val openTag = if (openEnd != -1) text.substring(i, openEnd + 1) else ""
                    val parsedColor = parseHtmlTagColor(openTag)
                    if (openEnd != -1 && closeStart != -1 && parsedColor != null) {
                        withStyle(SpanStyle(color = parsedColor)) {
                            append(parseInlineMarkdown(text.substring(openEnd + 1, closeStart)))
                        }
                        i = closeStart + "</span>".length
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 粗体 **text** 或 __text__
                (text.startsWith("**", i) || text.startsWith("__", i)) -> {
                    val marker = if (text.startsWith("**", i)) "**" else "__"
                    val end = text.indexOf(marker, i + 2)
                    if (end != -1) {
                        val boldText = text.substring(i + 2, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(boldText)
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 斜体 *text* 或 _text_
                (text.startsWith("*", i) && !text.startsWith("**", i)) ||
                (text.startsWith("_", i) && !text.startsWith("__", i)) -> {
                    val marker = if (text.startsWith("*", i)) "*" else "_"
                    val end = text.indexOf(marker, i + 1)
                    if (end != -1 && end > i + 1) {
                        val italicText = text.substring(i + 1, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(italicText)
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 删除线 ~~text~~
                text.startsWith("~~", i) -> {
                    val end = text.indexOf("~~", i + 2)
                    if (end != -1) {
                        val strikethroughText = text.substring(i + 2, end)
                        withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
                            append(strikethroughText)
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 行内代码 `code`
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        val codeText = text.substring(i + 1, end)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                background = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.1f)
                            )
                        ) {
                            append(codeText)
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 链接 [text](url)
                text.startsWith("[", i) -> {
                    val closeBracket = text.indexOf("]", i)
                    if (closeBracket != -1 && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                        val closeParen = text.indexOf(")", closeBracket + 1)
                        if (closeParen != -1) {
                            val linkText = text.substring(i + 1, closeBracket)
                            val url = text.substring(closeBracket + 2, closeParen)
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(
                                SpanStyle(
                                    color = androidx.compose.ui.graphics.Color(0xFF2563EB),
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(linkText)
                            }
                            pop()
                            i = closeParen + 1
                        } else {
                            append(text[i])
                            i++
                        }
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // 普通字符
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
