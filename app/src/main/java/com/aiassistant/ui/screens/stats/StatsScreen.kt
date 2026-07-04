package com.aiassistant.ui.screens.stats

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aiassistant.ui.components.EchoGlassBackground
import com.aiassistant.ui.components.GlassSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    var selectedPeriod by remember { mutableStateOf(StatsPeriod.Day) }
    var refreshKey by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var stats by remember { mutableStateOf<List<UsageRow>>(emptyList()) }
    var statusText by remember { mutableStateOf("正在读取统计") }

    LaunchedEffect(selectedPeriod, refreshKey) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - selectedPeriod.durationMillis
        val result = readUsageRows(context, startTime, endTime)
        stats = result.rows
        statusText = result.message
    }

    val summary = remember(stats) { stats.toSummary() }
    val buckets = remember(stats, selectedPeriod, refreshKey) {
        buildBuckets(stats, selectedPeriod, System.currentTimeMillis())
    }
    val modelRows = remember(stats) { stats.toModelRows() }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.66f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text("返回")
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatsHeaderIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "使用统计",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "（不准，仅供参考）",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { refreshKey = System.currentTimeMillis() }) {
                    Text("刷新")
                }
            }
        }
    ) { padding ->
        EchoGlassBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PeriodTabs(
                        selected = selectedPeriod,
                        onSelected = {
                            selectedPeriod = it
                            refreshKey = System.currentTimeMillis()
                        }
                    )
                }

                item {
                    SummaryCard(summary = summary, period = selectedPeriod, statusText = statusText)
                }

                item {
                    ChartCard(title = "Token 消耗") {
                        TokenBars(buckets = buckets, maxToken = buckets.maxOfOrNull { it.totalTokens }?.coerceAtLeast(1) ?: 1)
                    }
                }

                item {
                    ChartCard(title = "命中率趋势") {
                        RateLines(buckets = buckets)
                    }
                }

                item {
                    Text(
                        text = "模型明细",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (modelRows.isEmpty()) {
                    item {
                        EmptyCard("暂无统计记录。之后的新请求会在这里显示。")
                    }
                } else {
                    items(modelRows.size) { index ->
                        ModelRowCard(row = modelRows[index])
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeaderIcon() {
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = Color(0xFFE7EDF5),
        contentColor = Color(0xFF1F2937)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(21.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF1F2937)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .width(13.dp)
                        .height(13.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(4.dp, 8.dp, 12.dp).forEach { barHeight ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .background(Color.White, RoundedCornerShape(1.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodTabs(
    selected: StatsPeriod,
    onSelected: (StatsPeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatsPeriod.entries.forEach { period ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onSelected(period) },
                shape = RoundedCornerShape(999.dp),
                color = if (period == selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (period == selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ) {
                Text(
                    text = period.label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: UsageSummary,
    period: StatsPeriod,
    statusText: String
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${period.label} · ${formatNumber(summary.totalTokens)} Token",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("请求", summary.requestCount.toString(), Modifier.weight(1f))
                MetricPill("命中率", formatPercent(summary.cacheHitRate), Modifier.weight(1f))
                MetricPill("成功率", formatPercent(summary.successRate), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot("输入", MaterialTheme.colorScheme.primary)
                LegendDot("输出", MaterialTheme.colorScheme.secondary)
                LegendDot("思考/命中", MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun LegendDot(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TokenBars(buckets: List<Bucket>) {
    TokenBars(buckets = buckets, maxToken = buckets.maxOfOrNull { it.totalTokens }?.coerceAtLeast(1) ?: 1)
}

@Composable
private fun TokenBars(buckets: List<Bucket>, maxToken: Int) {
    val input = MaterialTheme.colorScheme.primary
    val output = MaterialTheme.colorScheme.secondary
    val thinking = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    val plot = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)

    Column {
        Row {
            AxisLabels(
                labels = listOf(formatNumber(maxToken), formatNumber(maxToken / 2), "0"),
                height = 170.dp
            )
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            ) {
                drawRoundRect(
                    color = plot,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                repeat(4) { line ->
                    val y = size.height * line / 3f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val slot = size.width / buckets.size.coerceAtLeast(1)
                val barWidth = (slot * 0.62f).coerceIn(5.dp.toPx(), 22.dp.toPx())
                buckets.forEachIndexed { index, bucket ->
                    val left = slot * index + (slot - barWidth) / 2f
                    val total = bucket.totalTokens.coerceAtLeast(0)
                    if (total <= 0) return@forEachIndexed
                    val fullHeight = size.height * total / maxToken
                    var bottom = size.height
                    fun segment(value: Int, color: Color) {
                        if (value <= 0) return
                        val height = (fullHeight * value / total.toFloat()).coerceAtLeast(1.dp.toPx())
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, bottom - height),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                        bottom -= height
                    }
                    segment(bucket.inputTokens, input)
                    segment(bucket.outputTokens, output)
                    segment(bucket.thinkingTokens, thinking)
                }
            }
        }
        XAxisLabels(buckets = buckets)
    }
}

@Composable
private fun RateLines(buckets: List<Bucket>) {
    val cache = MaterialTheme.colorScheme.tertiary
    val success = MaterialTheme.colorScheme.secondary
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    val plot = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
    val pointHalo = MaterialTheme.colorScheme.surface

    Column {
        Row {
            AxisLabels(labels = listOf("100%", "50%", "0%"), height = 170.dp)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
            ) {
                drawRoundRect(
                    color = plot,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                repeat(5) { line ->
                    val y = size.height * line / 4f
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }

                fun drawLineFor(values: List<Float>, color: Color) {
                    if (values.isEmpty()) return
                    val path = Path()
                    values.forEachIndexed { index, value ->
                        val x = if (values.size == 1) size.width / 2f else size.width * index / (values.size - 1)
                        val y = size.height * (1f - value.coerceIn(0f, 1f))
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    values.forEachIndexed { index, value ->
                        val x = if (values.size == 1) size.width / 2f else size.width * index / (values.size - 1)
                        val y = size.height * (1f - value.coerceIn(0f, 1f))
                        drawCircle(color = pointHalo, radius = 4.5.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(x, y))
                    }
                }

                drawLineFor(buckets.map { it.cacheHitRate }, cache)
                drawLineFor(buckets.map { it.successRate }, success)
            }
        }
        XAxisLabels(buckets = buckets)
    }
}

@Composable
private fun AxisLabels(labels: List<String>, height: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .width(42.dp)
            .height(height)
            .padding(end = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun XAxisLabels(buckets: List<Bucket>) {
    val labels = remember(buckets) { bucketAxisLabels(buckets) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 42.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModelRowCard(row: ModelRow) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.modelName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(row.provider, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatNumber(row.totalTokens), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("${row.requestCount} 次 · 成功率 ${formatPercent(row.successRate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private suspend fun readUsageRows(
    context: Context,
    startTime: Long,
    endTime: Long
): StatsReadResult = withContext(Dispatchers.IO) {
    val dbFile = context.getDatabasePath("ai_assistant_database")
    if (!dbFile.exists()) {
        return@withContext StatsReadResult(emptyList(), "暂无统计数据库")
    }

    try {
        SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            if (!hasUsableStatsTable(db)) {
                return@withContext StatsReadResult(emptyList(), "统计表不存在或不可用，之后会重新记录")
            }
            StatsReadResult(queryUsageRows(db, startTime, endTime), "读取完成")
        }
    } catch (_: Throwable) {
        StatsReadResult(emptyList(), "统计数据不可读，已自动忽略旧统计")
    }
}

private fun hasUsableStatsTable(db: SQLiteDatabase): Boolean {
    val required = setOf(
        "id", "apiConfigId", "provider", "modelName", "inputTokens", "outputTokens",
        "thinkingTokens", "totalTokens", "cachedTokens", "responseTime", "success",
        "errorMessage", "timestamp"
    )
    val columns = mutableSetOf<String>()
    return try {
        db.rawQuery("PRAGMA table_info(`api_usage_stats`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        columns.containsAll(required)
    } catch (_: Throwable) {
        false
    }
}

private fun queryUsageRows(db: SQLiteDatabase, startTime: Long, endTime: Long): List<UsageRow> {
    val rows = mutableListOf<UsageRow>()
    db.rawQuery(
        """
        SELECT provider, modelName, inputTokens, outputTokens, thinkingTokens,
               totalTokens, cachedTokens, responseTime, success, timestamp
        FROM api_usage_stats
        WHERE timestamp >= ? AND timestamp <= ?
        ORDER BY timestamp ASC
        """.trimIndent(),
        arrayOf(startTime.toString(), endTime.toString())
    ).use { cursor ->
        while (cursor.moveToNext()) {
            val inputTokens = cursor.getInt(2)
            val outputTokens = cursor.getInt(3)
            val thinkingTokens = cursor.getInt(4)
            val totalTokens = cursor.getInt(5)
            val normalizedOutputTokens = if (
                totalTokens > 0 && inputTokens + outputTokens + thinkingTokens == 0
            ) {
                totalTokens
            } else {
                outputTokens
            }
            rows += UsageRow(
                provider = cursor.getString(0) ?: "unknown",
                modelName = cursor.getString(1) ?: "unknown",
                inputTokens = inputTokens,
                outputTokens = normalizedOutputTokens,
                thinkingTokens = thinkingTokens,
                totalTokens = totalTokens.takeIf { it > 0 } ?: (inputTokens + normalizedOutputTokens + thinkingTokens),
                cachedTokens = cursor.getInt(6),
                responseTime = cursor.getLong(7),
                success = cursor.getInt(8) == 1,
                timestamp = cursor.getLong(9)
            )
        }
    }
    return rows
}

private fun List<UsageRow>.toSummary(): UsageSummary {
    val input = sumOf { it.inputTokens }
    val cached = sumOf { it.cachedTokens }
    val successCount = count { it.success }
    return UsageSummary(
        totalTokens = sumOf { it.totalTokens },
        requestCount = size,
        cacheHitRate = if (input > 0) cached.toFloat() / input else 0f,
        successRate = if (isNotEmpty()) successCount.toFloat() / size else 0f
    )
}

private fun List<UsageRow>.toModelRows(): List<ModelRow> {
    return groupBy { it.provider to it.modelName }
        .map { (key, rows) ->
            ModelRow(
                provider = key.first,
                modelName = key.second,
                totalTokens = rows.sumOf { it.totalTokens },
                requestCount = rows.size,
                successRate = if (rows.isNotEmpty()) rows.count { it.success }.toFloat() / rows.size else 0f
            )
        }
        .sortedByDescending { it.totalTokens }
}

private fun buildBuckets(rows: List<UsageRow>, period: StatsPeriod, endTime: Long): List<Bucket> {
    val bucketSize = period.durationMillis / period.bucketCount
    val startTime = endTime - period.durationMillis
    val formatter = SimpleDateFormat(period.labelPattern, Locale.getDefault())
    return List(period.bucketCount) { index ->
        val bucketStart = startTime + bucketSize * index
        val bucketEnd = if (index == period.bucketCount - 1) endTime else bucketStart + bucketSize
        val bucketRows = rows.filter { it.timestamp >= bucketStart && it.timestamp < bucketEnd }
        val input = bucketRows.sumOf { it.inputTokens }
        val cached = bucketRows.sumOf { it.cachedTokens }
        Bucket(
            label = formatter.format(Date(bucketStart)),
            inputTokens = input,
            outputTokens = bucketRows.sumOf { it.outputTokens },
            thinkingTokens = bucketRows.sumOf { it.thinkingTokens },
            totalTokens = bucketRows.sumOf { it.totalTokens },
            cacheHitRate = if (input > 0) cached.toFloat() / input else 0f,
            successRate = if (bucketRows.isNotEmpty()) bucketRows.count { it.success }.toFloat() / bucketRows.size else 0f
        )
    }
}

private fun formatNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format(Locale.getDefault(), "%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}

private fun formatPercent(value: Float): String {
    return "${(value.coerceIn(0f, 1f) * 100).toInt()}%"
}

private fun bucketAxisLabels(buckets: List<Bucket>): List<String> {
    if (buckets.isEmpty()) return emptyList()
    val indices = listOf(0, buckets.lastIndex / 2, buckets.lastIndex).distinct()
    return indices.map { buckets[it].label }
}

private enum class StatsPeriod(
    val label: String,
    val durationMillis: Long,
    val bucketCount: Int,
    val labelPattern: String
) {
    Hour("1小时", 60L * 60L * 1000L, 12, "HH:mm"),
    Day("1天", 24L * 60L * 60L * 1000L, 24, "HH:mm"),
    Week("7天", 7L * 24L * 60L * 60L * 1000L, 7, "MM-dd"),
    Month("30天", 30L * 24L * 60L * 60L * 1000L, 30, "MM-dd"),
    Quarter("90天", 90L * 24L * 60L * 60L * 1000L, 30, "MM-dd")
}

private data class StatsReadResult(
    val rows: List<UsageRow>,
    val message: String
)

private data class UsageRow(
    val provider: String,
    val modelName: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val thinkingTokens: Int,
    val totalTokens: Int,
    val cachedTokens: Int,
    val responseTime: Long,
    val success: Boolean,
    val timestamp: Long
)

private data class UsageSummary(
    val totalTokens: Int,
    val requestCount: Int,
    val cacheHitRate: Float,
    val successRate: Float
)

private data class Bucket(
    val label: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val thinkingTokens: Int,
    val totalTokens: Int,
    val cacheHitRate: Float,
    val successRate: Float
)

private data class ModelRow(
    val provider: String,
    val modelName: String,
    val totalTokens: Int,
    val requestCount: Int,
    val successRate: Float
)
