package com.aiassistant.ui.screens.stats

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.aiassistant.ui.components.EchoGlassPagePanelShape
import com.aiassistant.ui.components.EchoWallpaperBackground
import com.aiassistant.ui.components.echoHazePanel
import com.aiassistant.ui.components.echoShapeClick
import com.aiassistant.ui.components.readableTextColorFor
import com.aiassistant.ui.components.rememberEchoHazeState
import com.aiassistant.ui.components.rememberReadableBackdropColor
import com.aiassistant.utils.BackgroundImageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.pow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val StatsGlassPanelAlpha = 0.52f
private const val StatsGlassInnerAlpha = 0.44f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit
) {
    val localContext = LocalContext.current
    val context = localContext.applicationContext
    val statsBackgroundBitmap = remember(localContext) {
        BackgroundImageManager.getHomeBackgroundBitmap(localContext)
    }
    val hazeState = rememberEchoHazeState()
    val readableBackdrop = rememberReadableBackdropColor(statsBackgroundBitmap)
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

    EchoWallpaperBackground(
        backgroundBitmap = statsBackgroundBitmap,
        hazeState = hazeState
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatsHeaderIcon()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "使用统计",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        TextButton(onClick = { refreshKey = System.currentTimeMillis() }) {
                            Text("刷新")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PeriodTabs(
                        hazeState = hazeState,
                        selected = selectedPeriod,
                        readableBackdrop = readableBackdrop,
                        onSelected = {
                            selectedPeriod = it
                            refreshKey = System.currentTimeMillis()
                        }
                    )
                }

                item {
                    SummaryCard(
                        hazeState = hazeState,
                        summary = summary,
                        period = selectedPeriod,
                        statusText = statusText,
                        readableBackdrop = readableBackdrop
                    )
                }

                item {
                    ChartCard(hazeState = hazeState, title = "Token 消耗", readableBackdrop = readableBackdrop) {
                        TokenBars(
                            buckets = buckets,
                            maxToken = niceAxisMax(buckets.maxOfOrNull { it.totalTokens } ?: 0)
                        )
                    }
                }

                item {
                    ChartCard(hazeState = hazeState, title = "命中率趋势", readableBackdrop = readableBackdrop) {
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
                        EmptyCard(
                            hazeState = hazeState,
                            text = "暂无统计记录。之后的新请求会在这里显示。",
                            readableBackdrop = readableBackdrop
                        )
                    }
                } else {
                    items(modelRows.size) { index ->
                        ModelRowCard(
                            hazeState = hazeState,
                            row = modelRows[index],
                            readableBackdrop = readableBackdrop
                        )
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
    hazeState: dev.chrisbanes.haze.HazeState,
    selected: StatsPeriod,
    readableBackdrop: Color,
    onSelected: (StatsPeriod) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatsPeriod.entries.forEach { period ->
            val shape = RoundedCornerShape(999.dp)
            val tint = MaterialTheme.colorScheme.surface.copy(
                alpha = if (period == selected) StatsGlassPanelAlpha else StatsGlassInnerAlpha
            )
            val content = readableTextColorFor(
                background = tint,
                fallbackSurface = readableBackdrop
            )
            Surface(
                modifier = Modifier
                    .echoHazePanel(
                        hazeState = hazeState,
                        shape = shape,
                        tint = tint,
                        blurRadius = 16.dp
                    )
                    .echoShapeClick(shape) { onSelected(period) },
                shape = shape,
                color = if (period == selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                },
                contentColor = if (period == selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    content
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
    hazeState: dev.chrisbanes.haze.HazeState,
    summary: UsageSummary,
    period: StatsPeriod,
    statusText: String,
    readableBackdrop: Color
) {
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = StatsGlassPanelAlpha)
    val content = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = EchoGlassPagePanelShape,
                tint = tint,
                blurRadius = 20.dp
            ),
        shape = EchoGlassPagePanelShape,
        color = Color.Transparent,
        contentColor = content,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${period.label} · ${formatNumber(summary.totalTokens)} Token",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = content
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.72f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill("请求", summary.requestCount.toString(), content, Modifier.weight(1f))
                MetricPill("命中率", formatPercent(summary.cacheHitRate), content, Modifier.weight(1f))
                MetricPill("成功率", formatPercent(summary.successRate), content, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, contentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = StatsGlassInnerAlpha),
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = contentColor)
            Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.68f))
        }
    }
}

@Composable
private fun ChartCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    title: String,
    readableBackdrop: Color,
    content: @Composable () -> Unit
) {
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = StatsGlassPanelAlpha)
    val contentColor = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = EchoGlassPagePanelShape,
                tint = tint,
                blurRadius = 18.dp
            ),
        shape = EchoGlassPagePanelShape,
        color = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            content()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot("输入", MaterialTheme.colorScheme.primary)
                LegendDot("输出", MaterialTheme.colorScheme.secondary)
                LegendDot("思考", MaterialTheme.colorScheme.tertiary)
                LegendDot("其他", MaterialTheme.colorScheme.outline)
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
    val unclassified = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    val plot = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)

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
                    segment(bucket.otherTokens, unclassified)
                    segment(bucket.thinkingTokens, thinking)
                    segment(bucket.outputTokens, output)
                    segment(bucket.inputTokens, input)
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
    val plot = MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
    val pointHalo = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)

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
private fun ModelRowCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    row: ModelRow,
    readableBackdrop: Color
) {
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = StatsGlassPanelAlpha)
    val content = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = RoundedCornerShape(22.dp),
                tint = tint,
                blurRadius = 16.dp
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        contentColor = content,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.modelName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = content)
                Text(row.provider, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.70f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatNumber(row.totalTokens), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("${row.requestCount} 次 · 成功率 ${formatPercent(row.successRate)}", style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = 0.70f))
            }
        }
    }
}

@Composable
private fun EmptyCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    text: String,
    readableBackdrop: Color
) {
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = StatsGlassPanelAlpha)
    val content = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .echoHazePanel(
                hazeState = hazeState,
                shape = EchoGlassPagePanelShape,
                tint = tint,
                blurRadius = 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = content.copy(alpha = 0.70f))
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
            val inputTokens = cursor.getInt(2).coerceAtLeast(0)
            val outputTokens = cursor.getInt(3).coerceAtLeast(0)
            val thinkingTokens = cursor.getInt(4).coerceAtLeast(0)
            val recordedTotalTokens = cursor.getInt(5).coerceAtLeast(0)
            val knownTokens = inputTokens + outputTokens + thinkingTokens
            val totalTokens = maxOf(recordedTotalTokens, knownTokens)
            val otherTokens = (totalTokens - knownTokens).coerceAtLeast(0)
            rows += UsageRow(
                provider = cursor.getString(0) ?: "unknown",
                modelName = cursor.getString(1) ?: "unknown",
                inputTokens = inputTokens,
                outputTokens = outputTokens,
                thinkingTokens = thinkingTokens,
                otherTokens = otherTokens,
                totalTokens = totalTokens,
                cachedTokens = cursor.getInt(6).coerceIn(0, inputTokens),
                responseTime = cursor.getLong(7).coerceAtLeast(0L),
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
            otherTokens = bucketRows.sumOf { it.otherTokens },
            totalTokens = bucketRows.sumOf { it.totalTokens },
            cacheHitRate = if (input > 0) cached.toFloat() / input else 0f,
            successRate = if (bucketRows.isNotEmpty()) bucketRows.count { it.success }.toFloat() / bucketRows.size else 0f
        )
    }
}

private fun niceAxisMax(value: Int): Int {
    if (value <= 0) return 1
    val magnitude = 10.0.pow((value.toString().length - 1).toDouble()).toInt()
    val normalized = value.toFloat() / magnitude
    val nice = when {
        normalized <= 1f -> 1
        normalized <= 2f -> 2
        normalized <= 5f -> 5
        else -> 10
    }
    return nice * magnitude
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
    val otherTokens: Int,
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
    val otherTokens: Int,
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
