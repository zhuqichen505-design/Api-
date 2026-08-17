package com.aiassistant.ui.components

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
    SelectionContainer {
        Column(modifier = modifier) {
            val lines = content.split("\n")
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

                    // 独立 LaTeX 数学块 $$...$$ 或 \[...\]
                    trimmed.startsWith("$$") || trimmed.startsWith("\\[") -> {
                        val isBracketStyle = trimmed.startsWith("\\[")
                        val mathContent = StringBuilder()
                        if (isBracketStyle) {
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
                        renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleLarge, color = color, topPad = 8.dp, bottomPad = 4.dp)
                        index++
                    }
                    line.startsWith("## ") -> {
                        val text = line.removePrefix("## ")
                        renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleMedium, color = color, topPad = 6.dp, bottomPad = 3.dp)
                        index++
                    }
                    line.startsWith("### ") -> {
                        val text = line.removePrefix("### ")
                        renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.titleSmall, color = color, topPad = 5.dp, bottomPad = 3.dp)
                        index++
                    }
                    line.startsWith("#### ") -> {
                        val text = line.removePrefix("#### ")
                        renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.labelLarge, color = color, topPad = 4.dp, bottomPad = 2.dp)
                        index++
                    }
                    line.startsWith("##### ") -> {
                        val text = line.removePrefix("##### ")
                        renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp), color = color, topPad = 3.dp, bottomPad = 2.dp)
                        index++
                    }
                    line.startsWith("###### ") -> {
                        val text = line.removePrefix("###### ")
                        renderHeadingText(text = text, defaultStyle = MaterialTheme.typography.labelMedium, color = color, topPad = 2.dp, bottomPad = 2.dp)
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

                    // 普通无序列表项
                    line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                        val indent = line.length - line.trimStart().length
                        val itemContent = line.trimStart().removePrefix("- ").removePrefix("* ")
                        Row(modifier = Modifier.padding(start = (8 + indent * 4).dp, top = 2.dp)) {
                            Text(
                                text = "•",
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
                        } else {
                            InlineMarkdownText(
                                text = parseInlineMarkdown(line),
                                style = MaterialTheme.typography.bodyLarge,
                                color = color,
                                modifier = Modifier.padding(vertical = 2.dp),
                                onCitationClick = onCitationClick
                            )
                        }
                        index++
                    }
                }
            }
        }
    }
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

/**
 * 将常见 LaTeX 数学符号与表达式转换为易读的数学 Unicode 排版
 */
fun parseLaTeXToUnicode(raw: String): String {
    var text = raw.trim()

    // 替换分数 \frac{a}{b} -> (a)/(b)
    val fracRegex = Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}""")
    while (fracRegex.containsMatchIn(text)) {
        text = text.replace(fracRegex) { match ->
            val num = match.groupValues[1].trim()
            val den = match.groupValues[2].trim()
            if (num.length <= 3 && den.length <= 3) "$num/$den" else "($num)/($den)"
        }
    }

    // 替换根号 \sqrt{x} -> √(x)
    text = text.replace(Regex("""\\sqrt\{([^{}]+)\}"""), "√($1)")
    text = text.replace(Regex("""\\sqrt\[([^{}]+)\]\{([^{}]+)\}"""), "$1√($2)")

    // 希腊字母与常用数学常数
    val symbolMap = listOf(
        "\\alpha" to "α", "\\beta" to "β", "\\gamma" to "γ", "\\delta" to "δ",
        "\\epsilon" to "ε", "\\zeta" to "ζ", "\\eta" to "η", "\\theta" to "θ",
        "\\iota" to "ι", "\\kappa" to "κ", "\\lambda" to "λ", "\\mu" to "μ",
        "\\nu" to "ν", "\\xi" to "ξ", "\\pi" to "π", "\\rho" to "ρ",
        "\\sigma" to "σ", "\\tau" to "τ", "\\upsilon" to "υ", "\\phi" to "φ",
        "\\chi" to "χ", "\\psi" to "ψ", "\\omega" to "ω",
        "\\Gamma" to "Γ", "\\Delta" to "Δ", "\\Theta" to "Θ", "\\Lambda" to "Λ",
        "\\Xi" to "Ξ", "\\Pi" to "Π", "\\Sigma" to "Σ", "\\Upsilon" to "Υ",
        "\\Phi" to "Φ", "\\Psi" to "Ψ", "\\Omega" to "Ω",
        "\\times" to " × ", "\\cdot" to " · ", "\\div" to " ÷ ", "\\pm" to " ± ",
        "\\mp" to " ∓ ", "\\leq" to " ≤ ", "\\le" to " ≤ ", "\\geq" to " ≥ ",
        "\\ge" to " ≥ ", "\\neq" to " ≠ ", "\\ne" to " ≠ ", "\\approx" to " ≈ ",
        "\\equiv" to " ≡ ", "\\infty" to "∞", "\\propto" to " ∝ ",
        "\\sum" to "∑", "\\prod" to "∏", "\\int" to "∫", "\\iint" to "∬",
        "\\iiint" to "∭", "\\oint" to "∮", "\\nabla" to "∇", "\\partial" to "∂",
        "\\forall" to "∀", "\\exists" to "∃", "\\in" to " ∈ ", "\\notin" to " ∉ ",
        "\\subset" to " ⊂ ", "\\supset" to " ⊃ ", "\\cup" to " ∪ ", "\\cap" to " ∩ ",
        "\\rightarrow" to " → ", "\\to" to " → ", "\\leftarrow" to " ← ",
        "\\Rightarrow" to " ⇒ ", "\\Leftarrow" to " ⇐ ", "\\Leftrightarrow" to " ⇔ ",
        "\\quad" to "   ", "\\qquad" to "      ", "\\," to " ", "\\;" to " ",
        "\\left" to "", "\\right" to "", "\\mathbf" to "", "\\mathit" to "",
        "\\mathrm" to "", "\\text" to ""
    )

    for ((latex, unicode) in symbolMap) {
        text = text.replace(latex, unicode)
    }

    // 上标转换 ^2 -> ², ^{12} -> ¹²
    text = convertSuperSubScripts(text)

    return text.replace(Regex("""\s+"""), " ").trim()
}

private fun convertSuperSubScripts(input: String): String {
    val supers = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ'
    )
    val subs = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ',
        'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ', 'p' to 'ₚ', 'r' to 'ᵣ',
        's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ', 'v' to 'ᵥ', 'x' to 'ₓ'
    )

    var res = input
    // ^{...}
    res = Regex("""\^\{([0-9a-zA-Z+\-=()]+)\}""").replace(res) { m ->
        m.groupValues[1].map { supers[it] ?: it }.joinToString("")
    }
    // ^x
    res = Regex("""\^([0-9a-zA-Z+\-=])""").replace(res) { m ->
        val c = m.groupValues[1][0]
        supers[c]?.toString() ?: "^$c"
    }
    // _{...}
    res = Regex("""_\{([0-9a-zA-Z+\-=()]+)\}""").replace(res) { m ->
        m.groupValues[1].map { subs[it] ?: it }.joinToString("")
    }
    // _x
    res = Regex("""_([0-9a-zA-Z+\-=])""").replace(res) { m ->
        val c = m.groupValues[1][0]
        subs[c]?.toString() ?: "_$c"
    }

    return res
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

/**
 * 解码 HTML 实体
 */
private fun decodeHtmlEntities(input: String): String {
    return input
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&mdash;", "—")
        .replace("&ndash;", "–")
        .replace("&times;", "×")
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
    color: Color
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
    color: Color
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
    color: Color
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
    color: Color,
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
    color: Color,
    modifier: Modifier = Modifier,
    onCitationClick: ((Int) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
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

/**
 * 代码块组件（支持语法高亮与复制）
 */
@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val highlightedCode = remember(code, language, isDark) {
        highlightSyntax(code, language, isDark)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.95f) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column {
            // 顶部栏：语言标签与复制按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.7f) else Color(0xFFE2E8F0).copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(code)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "复制代码",
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 代码内容高亮展示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = highlightedCode,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    softWrap = false,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Visible
                )
            }
        }
    }
}

/**
 * 现代轻量语法高亮引擎
 */
fun highlightSyntax(code: String, language: String, isDark: Boolean): AnnotatedString {
    val keywordColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED) // 紫色
    val stringColor = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A) // 绿色
    val numberColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C) // 橙色
    val commentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B) // 灰色
    val typeColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7) // 浅蓝
    val defaultColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)

    val keywords = setOf(
        "fun", "val", "var", "class", "interface", "object", "import", "package",
        "return", "if", "else", "while", "for", "in", "is", "as", "try", "catch",
        "finally", "throw", "when", "def", "from", "const", "let", "function",
        "async", "await", "struct", "impl", "enum", "public", "private", "protected",
        "static", "void", "select", "update", "delete", "insert", "where", "case",
        "switch", "break", "continue", "default", "true", "false", "null", "nil"
    )

    return buildAnnotatedString {
        var i = 0
        while (i < code.length) {
            when {
                // 单行注释 // 或 #
                code.startsWith("//", i) || (language.lowercase() in listOf("python", "py", "bash", "sh", "yaml", "yml", "dockerfile") && code[i] == '#') -> {
                    val end = code.indexOf('\n', i)
                    val commentText = if (end != -1) code.substring(i, end) else code.substring(i)
                    withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                        append(commentText)
                    }
                    i += commentText.length
                }

                // 块注释 /* ... */
                code.startsWith("/*", i) -> {
                    val end = code.indexOf("*/", i + 2)
                    val commentText = if (end != -1) code.substring(i, end + 2) else code.substring(i)
                    withStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)) {
                        append(commentText)
                    }
                    i += commentText.length
                }

                // 字符串 "..." 或 '...' 或 `...`
                code[i] == '"' || code[i] == '\'' || code[i] == '`' -> {
                    val quote = code[i]
                    var end = i + 1
                    while (end < code.length) {
                        if (code[end] == '\\') {
                            end += 2
                        } else if (code[end] == quote) {
                            end++
                            break
                        } else {
                            end++
                        }
                    }
                    val strText = code.substring(i, end.coerceAtMost(code.length))
                    withStyle(SpanStyle(color = stringColor)) {
                        append(strText)
                    }
                    i = end
                }

                // 标识符或关键词
                code[i].isLetter() || code[i] == '_' || code[i] == '@' -> {
                    var end = i
                    while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_')) {
                        end++
                    }
                    val word = code.substring(i, end)
                    val style = when {
                        word in keywords -> SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)
                        word.startsWith("@") -> SpanStyle(color = numberColor)
                        word.isNotEmpty() && word[0].isUpperCase() -> SpanStyle(color = typeColor, fontWeight = FontWeight.SemiBold)
                        else -> SpanStyle(color = defaultColor)
                    }
                    withStyle(style) {
                        append(word)
                    }
                    i = end
                }

                // 数字
                code[i].isDigit() -> {
                    var end = i
                    while (end < code.length && (code[end].isDigit() || code[end] == '.' || code[end] in "xXaAbBcCdDeEfF")) {
                        end++
                    }
                    val num = code.substring(i, end)
                    withStyle(SpanStyle(color = numberColor)) {
                        append(num)
                    }
                    i = end
                }

                else -> {
                    withStyle(SpanStyle(color = defaultColor)) {
                        append(code[i])
                    }
                    i++
                }
            }
        }
    }
}

/**
 * 行内 Markdown 与多格式富文本解析
 */
fun parseInlineMarkdown(
    text: String,
    isReferenceItem: Boolean = false
): AnnotatedString {
    val decoded = decodeHtmlEntities(text)

    return buildAnnotatedString {
        var i = 0
        while (i < decoded.length) {
            when {
                // 自定义颜色语法 {#color|text}
                decoded.startsWith("{#", i) -> {
                    val separator = decoded.indexOf('|', i + 2)
                    val end = if (separator != -1) decoded.indexOf('}', separator + 1) else -1
                    val colorValue = if (separator != -1) decoded.substring(i + 1, separator) else ""
                    val parsedColor = parseInlineColor(colorValue)
                    if (separator != -1 && end != -1 && parsedColor != null) {
                        withStyle(SpanStyle(color = parsedColor)) {
                            append(parseInlineMarkdown(decoded.substring(separator + 1, end)))
                        }
                        i = end + 1
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <font color="...">text</font>
                decoded.startsWith("<font", i, ignoreCase = true) -> {
                    val openEnd = decoded.indexOf('>', i)
                    val closeStart = decoded.indexOf("</font>", if (openEnd != -1) openEnd + 1 else i, ignoreCase = true)
                    val openTag = if (openEnd != -1) decoded.substring(i, openEnd + 1) else ""
                    val parsedColor = parseHtmlTagColor(openTag)
                    if (openEnd != -1 && closeStart != -1 && parsedColor != null) {
                        withStyle(SpanStyle(color = parsedColor)) {
                            append(parseInlineMarkdown(decoded.substring(openEnd + 1, closeStart)))
                        }
                        i = closeStart + "</font>".length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <span style="...">text</span>
                decoded.startsWith("<span", i, ignoreCase = true) -> {
                    val openEnd = decoded.indexOf('>', i)
                    val closeStart = decoded.indexOf("</span>", if (openEnd != -1) openEnd + 1 else i, ignoreCase = true)
                    val openTag = if (openEnd != -1) decoded.substring(i, openEnd + 1) else ""
                    val parsedColor = parseHtmlTagColor(openTag)
                    if (openEnd != -1 && closeStart != -1 && parsedColor != null) {
                        withStyle(SpanStyle(color = parsedColor)) {
                            append(parseInlineMarkdown(decoded.substring(openEnd + 1, closeStart)))
                        }
                        i = closeStart + "</span>".length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <b> 或 <strong>
                decoded.startsWith("<b>", i, ignoreCase = true) || decoded.startsWith("<strong>", i, ignoreCase = true) -> {
                    val isStrong = decoded.startsWith("<strong>", i, ignoreCase = true)
                    val tagLen = if (isStrong) 8 else 3
                    val closeTag = if (isStrong) "</strong>" else "</b>"
                    val closeIndex = decoded.indexOf(closeTag, i + tagLen, ignoreCase = true)
                    if (closeIndex != -1) {
                        val inner = decoded.substring(i + tagLen, closeIndex)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(parseInlineMarkdown(inner))
                        }
                        i = closeIndex + closeTag.length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <i> 或 <em>
                decoded.startsWith("<i>", i, ignoreCase = true) || decoded.startsWith("<em>", i, ignoreCase = true) -> {
                    val isEm = decoded.startsWith("<em>", i, ignoreCase = true)
                    val tagLen = if (isEm) 4 else 3
                    val closeTag = if (isEm) "</em>" else "</i>"
                    val closeIndex = decoded.indexOf(closeTag, i + tagLen, ignoreCase = true)
                    if (closeIndex != -1) {
                        val inner = decoded.substring(i + tagLen, closeIndex)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(inner))
                        }
                        i = closeIndex + closeTag.length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <u> 下划线
                decoded.startsWith("<u>", i, ignoreCase = true) -> {
                    val closeIndex = decoded.indexOf("</u>", i + 3, ignoreCase = true)
                    if (closeIndex != -1) {
                        val inner = decoded.substring(i + 3, closeIndex)
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append(parseInlineMarkdown(inner))
                        }
                        i = closeIndex + "</u>".length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <s>, <del>, <strike> 删除线
                decoded.startsWith("<s>", i, ignoreCase = true) || decoded.startsWith("<del>", i, ignoreCase = true) || decoded.startsWith("<strike>", i, ignoreCase = true) -> {
                    val closeTag = when {
                        decoded.startsWith("<del>", i, ignoreCase = true) -> "</del>"
                        decoded.startsWith("<strike>", i, ignoreCase = true) -> "</strike>"
                        else -> "</s>"
                    }
                    val openLen = if (closeTag == "</s>") 3 else if (closeTag == "</del>") 5 else 8
                    val closeIndex = decoded.indexOf(closeTag, i + openLen, ignoreCase = true)
                    if (closeIndex != -1) {
                        val inner = decoded.substring(i + openLen, closeIndex)
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(parseInlineMarkdown(inner))
                        }
                        i = closeIndex + closeTag.length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <code>
                decoded.startsWith("<code>", i, ignoreCase = true) -> {
                    val closeIndex = decoded.indexOf("</code>", i + 6, ignoreCase = true)
                    if (closeIndex != -1) {
                        val inner = decoded.substring(i + 6, closeIndex)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                background = Color.Gray.copy(alpha = 0.12f)
                            )
                        ) {
                            append(inner)
                        }
                        i = closeIndex + "</code>".length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <mark> 高亮
                decoded.startsWith("<mark>", i, ignoreCase = true) -> {
                    val closeIndex = decoded.indexOf("</mark>", i + 6, ignoreCase = true)
                    if (closeIndex != -1) {
                        val inner = decoded.substring(i + 6, closeIndex)
                        withStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF713F12))) {
                            append(parseInlineMarkdown(inner))
                        }
                        i = closeIndex + "</mark>".length
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // HTML <br> 或 <br/>
                decoded.startsWith("<br/>", i, ignoreCase = true) || decoded.startsWith("<br>", i, ignoreCase = true) -> {
                    append("\n")
                    i += if (decoded.startsWith("<br/>", i, ignoreCase = true)) 5 else 4
                }

                // LaTeX 行内数学公式 \(...\)
                decoded.startsWith("\\(", i) -> {
                    val end = decoded.indexOf("\\)", i + 2)
                    if (end != -1) {
                        val math = decoded.substring(i + 2, end)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(parseLaTeXToUnicode(math))
                        }
                        i = end + 2
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // LaTeX 行内数学公式 $...$
                decoded.startsWith("$", i) && !decoded.startsWith("$$", i) && isInlineMathStart(decoded, i) -> {
                    val end = decoded.indexOf('$', i + 1)
                    if (end != -1 && end > i + 1 && !decoded.substring(i + 1, end).contains('\n')) {
                        val math = decoded.substring(i + 1, end)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append(parseLaTeXToUnicode(math))
                        }
                        i = end + 1
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 粗斜体 ***text*** 或 ___text___ 或 **_text_** 或 *__text__*
                (decoded.startsWith("***", i) || decoded.startsWith("___", i)) -> {
                    val marker = if (decoded.startsWith("***", i)) "***" else "___"
                    val end = decoded.indexOf(marker, i + 3)
                    if (end != -1) {
                        val boldItalicText = decoded.substring(i + 3, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(boldItalicText))
                        }
                        i = end + 3
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                decoded.startsWith("**_", i) -> {
                    val end = decoded.indexOf("_**", i + 3)
                    if (end != -1) {
                        val boldItalicText = decoded.substring(i + 3, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(boldItalicText))
                        }
                        i = end + 3
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                decoded.startsWith("*__", i) -> {
                    val end = decoded.indexOf("__*", i + 3)
                    if (end != -1) {
                        val boldItalicText = decoded.substring(i + 3, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(boldItalicText))
                        }
                        i = end + 3
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 粗体 **text** 或 __text__
                (decoded.startsWith("**", i) || decoded.startsWith("__", i)) -> {
                    val marker = if (decoded.startsWith("**", i)) "**" else "__"
                    val end = decoded.indexOf(marker, i + 2)
                    if (end != -1) {
                        val boldText = decoded.substring(i + 2, end)
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(parseInlineMarkdown(boldText))
                        }
                        i = end + 2
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 斜体 *text* 或 _text_
                (decoded.startsWith("*", i) && !decoded.startsWith("**", i)) ||
                (decoded.startsWith("_", i) && !decoded.startsWith("__", i)) -> {
                    val marker = if (decoded.startsWith("*", i)) "*" else "_"
                    val end = decoded.indexOf(marker, i + 1)
                    if (end != -1 && end > i + 1) {
                        val italicText = decoded.substring(i + 1, end)
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(italicText))
                        }
                        i = end + 1
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 删除线 ~~text~~
                decoded.startsWith("~~", i) -> {
                    val end = decoded.indexOf("~~", i + 2)
                    if (end != -1) {
                        val strikethroughText = decoded.substring(i + 2, end)
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(parseInlineMarkdown(strikethroughText))
                        }
                        i = end + 2
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 行内代码 `code`
                decoded.startsWith("`", i) -> {
                    val end = decoded.indexOf("`", i + 1)
                    if (end != -1) {
                        val codeText = decoded.substring(i + 1, end)
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                background = Color.Gray.copy(alpha = 0.12f)
                            )
                        ) {
                            append(codeText)
                        }
                        i = end + 1
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 引用角标 [1] 或 [^1]
                decoded.startsWith("[", i) && decoded.substring(i).matches(Regex("""^\[\^?\d+\].*""")) -> {
                    val closeBracket = decoded.indexOf("]", i)
                    if (closeBracket != -1) {
                        val numStr = decoded.substring(i + 1, closeBracket).removePrefix("^")
                        pushStringAnnotation(tag = "CITATION", annotation = numStr)

                        if (isReferenceItem || i == 0) {
                            // 末尾参考资料列表展示为正常大小，不使用上标
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            ) {
                                append("[$numStr] ")
                            }
                        } else {
                            // 正文内引用角标使用微型上标
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    baselineShift = BaselineShift.Superscript
                                )
                            ) {
                                append("[$numStr]")
                            }
                        }
                        pop()
                        i = closeBracket + 1
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // Markdown 链接 [text](url)
                decoded.startsWith("[", i) -> {
                    val closeBracket = decoded.indexOf("]", i)
                    if (closeBracket != -1 && closeBracket + 1 < decoded.length && decoded[closeBracket + 1] == '(') {
                        val closeParen = decoded.indexOf(")", closeBracket + 1)
                        if (closeParen != -1) {
                            val linkText = decoded.substring(i + 1, closeBracket)
                            val url = decoded.substring(closeBracket + 2, closeParen)
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(linkText)
                            }
                            pop()
                            i = closeParen + 1
                        } else {
                            append(decoded[i])
                            i++
                        }
                    } else {
                        append(decoded[i])
                        i++
                    }
                }

                // 纯文本链接 http:// 或 https:// 自动识别为可点击
                decoded.startsWith("http://", i) || decoded.startsWith("https://", i) -> {
                    var end = i
                    while (end < decoded.length && !decoded[end].isWhitespace() && decoded[end] !in listOf(')', ']', '}', '>', '"', '\'')) {
                        end++
                    }
                    val url = decoded.substring(i, end)
                    pushStringAnnotation(tag = "URL", annotation = url)
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF2563EB),
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(url)
                    }
                    pop()
                    i = end
                }

                // 普通字符
                else -> {
                    append(decoded[i])
                    i++
                }
            }
        }
    }
}

/**
 * 校验是否为行内数学公式起始（避免把普通美元价格如 $100 当作公式）
 */
private fun isInlineMathStart(text: String, index: Int): Boolean {
    if (index + 1 >= text.length) return false
    val nextChar = text[index + 1]
    if (nextChar.isWhitespace() || nextChar.isDigit()) return false
    val nextDollar = text.indexOf('$', index + 1)
    if (nextDollar == -1 || nextDollar == index + 1) return false
    val content = text.substring(index + 1, nextDollar)
    return content.any { it.isLetter() || it in "+-*/=^_{}()\\<>" }
}
