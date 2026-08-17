package com.aiassistant.ui.screens.stats

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiassistant.ui.components.EchoGlassPagePanelShape
import com.aiassistant.ui.components.EchoWallpaperBackground
import com.aiassistant.ui.components.echoFilterChipBorder
import com.aiassistant.ui.components.echoFilterChipColors
import com.aiassistant.ui.components.echoFilterChipElevation
import com.aiassistant.ui.components.echoGlassPalette
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
    var selectedModelFilter by remember { mutableStateOf<String?>(null) }
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

    val availableModels = remember(stats) {
        stats.map { it.modelName }.distinct().sorted()
    }

    val filteredStats = remember(stats, selectedModelFilter) {
        if (selectedModelFilter == null) stats else stats.filter { it.modelName == selectedModelFilter }
    }

    val summary = remember(filteredStats) { filteredStats.toSummary() }
    val buckets = remember(filteredStats, selectedPeriod, refreshKey) {
        buildBuckets(filteredStats, selectedPeriod, System.currentTimeMillis())
    }
    val modelRows = remember(filteredStats) { filteredStats.toModelRows() }

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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                // 1. 时间跨度选择
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

                // 2. 模型维度筛选器（横向滑动芯片栏）
                if (availableModels.isNotEmpty()) {
                    item {
                        ModelFilterChips(
                            models = availableModels,
                            selectedModel = selectedModelFilter,
                            onModelSelected = { selectedModelFilter = it }
                        )
                    }
                }

                // 3. 统计核心概览卡片
                item {
                    SummaryCard(
                        hazeState = hazeState,
                        summary = summary,
                        period = selectedPeriod,
                        selectedModel = selectedModelFilter,
                        statusText = statusText,
                        readableBackdrop = readableBackdrop
                    )
                }

                // 4. Token 消耗可视化堆叠条形图
                item {
                    ChartCard(
                        hazeState = hazeState,
                        title = if (selectedModelFilter != null) "$selectedModelFilter · Token 消耗趋势" else "Token 消耗趋势",
                        subtitle = "按时间分段统计输入、输出与思考 Token 分布",
                        readableBackdrop = readableBackdrop
                    ) { chartContentColor ->
                        ModernTokenBars(
                            buckets = buckets,
                            maxToken = niceAxisMax(buckets.maxOfOrNull { it.totalTokens } ?: 0),
                            labelColor = chartContentColor.copy(alpha = 0.72f)
                        )
                    }
                }

                // 5. 成功率走势曲线图
                item {
                    ChartCard(
                        hazeState = hazeState,
                        title = "调用成功率走势",
                        subtitle = "按时间分段统计 API 调用的成功率变化曲线",
                        readableBackdrop = readableBackdrop
                    ) { chartContentColor ->
                        ModernTrendChart(
                            buckets = buckets,
                            labelColor = chartContentColor.copy(alpha = 0.72f)
                        )
                    }
                }

                // 6. 模型明细表格
                item {
                    Text(
                        text = "模型统计明细",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (modelRows.isEmpty()) {
                    item {
                        EmptyCard(
                            hazeState = hazeState,
                            text = "当前筛选条件下暂无统计记录",
                            readableBackdrop = readableBackdrop
                        )
                    }
                } else {
                    item {
                        ModernModelStatsTable(
                            hazeState = hazeState,
                            rows = modelRows,
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
    val glass = echoGlassPalette()
    val glassBlue = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = glass.control,
        contentColor = glassBlue,
        border = BorderStroke(0.8.dp, glass.outline),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(glassBlue.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .width(14.dp)
                        .height(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    listOf(4.5.dp, 8.5.dp, 12.5.dp).forEach { barHeight ->
                        Box(
                            modifier = Modifier
                                .width(2.8.dp)
                                .height(barHeight)
                                .background(glassBlue, RoundedCornerShape(1.dp))
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
    val glass = echoGlassPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsPeriod.entries.forEach { period ->
            val shape = RoundedCornerShape(999.dp)
            val isSelected = period == selected
            val tint = if (isSelected) glass.controlSelected else glass.control
            val content = readableTextColorFor(
                background = tint,
                fallbackSurface = readableBackdrop
            )
            Box(
                modifier = Modifier
                    .echoHazePanel(
                        hazeState = hazeState,
                        shape = shape,
                        tint = tint,
                        blurRadius = 16.dp
                    )
                    .background(tint, shape)
                    .border(
                        BorderStroke(
                            if (isSelected) 1.5.dp else 0.8.dp,
                            if (isSelected) glass.outlineSelected else glass.outline
                        ),
                        shape
                    )
                    .echoShapeClick(shape) { onSelected(period) }
            ) {
                Text(
                    text = period.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else content,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ModelFilterChips(
    models: List<String>,
    selectedModel: String?,
    onModelSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedModel == null,
            onClick = { onModelSelected(null) },
            colors = echoFilterChipColors(),
            border = echoFilterChipBorder(selectedModel == null),
            elevation = echoFilterChipElevation(),
            label = { Text("全部模型") }
        )
        models.forEach { model ->
            val isSelected = selectedModel == model
            FilterChip(
                selected = isSelected,
                onClick = { onModelSelected(if (isSelected) null else model) },
                colors = echoFilterChipColors(),
                border = echoFilterChipBorder(isSelected),
                elevation = echoFilterChipElevation(),
                label = { Text(model) }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    summary: UsageSummary,
    period: StatsPeriod,
    selectedModel: String?,
    statusText: String,
    readableBackdrop: Color
) {
    val glass = echoGlassPalette()
    val tint = glass.panelStrong
    val content = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = EchoGlassPagePanelShape,
                tint = tint,
                blurRadius = 20.dp
            )
            .background(tint, EchoGlassPagePanelShape)
            .border(BorderStroke(1.dp, glass.outline), EchoGlassPagePanelShape)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${period.label}${if (selectedModel != null) " · $selectedModel" else ""} · ${formatNumber(summary.totalTokens)} Tokens",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = content
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.72f)
                    )
                }
            }

            // 核心统计指标网格（扁平化渲染，杜绝滚动白影）
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        icon = Icons.Default.Send,
                        label = "总请求数",
                        value = "${summary.requestCount} 次",
                        contentColor = content,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        icon = Icons.Default.CheckCircle,
                        label = "调用成功率",
                        value = formatPercent(summary.successRate),
                        contentColor = content,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        icon = Icons.Default.Download,
                        label = "输入 Token",
                        value = formatNumber(summary.inputTokens),
                        contentColor = content,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        icon = Icons.Default.Upload,
                        label = "输出 Token",
                        value = formatNumber(summary.outputTokens),
                        contentColor = content,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        icon = Icons.Default.Psychology,
                        label = "思考 Token",
                        value = formatNumber(summary.thinkingTokens),
                        contentColor = content,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    contentColor: Color,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val glass = echoGlassPalette()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(glass.control.copy(alpha = 0.65f))
            .border(BorderStroke(0.8.dp, glass.outline.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    title: String,
    subtitle: String,
    readableBackdrop: Color,
    content: @Composable (Color) -> Unit
) {
    val glass = echoGlassPalette()
    val tint = glass.panelStrong
    val contentColor = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = EchoGlassPagePanelShape,
                tint = tint,
                blurRadius = 18.dp
            )
            .background(tint, EchoGlassPagePanelShape)
            .border(BorderStroke(1.dp, glass.outline), EchoGlassPagePanelShape)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.70f)
                )
            }

            content(contentColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot("输入 Token", Color(0xFF6BA4F8), contentColor.copy(alpha = 0.85f))
                LegendDot("输出 Token", Color(0xFF38BDF8), contentColor.copy(alpha = 0.85f))
                LegendDot("思考 Token", Color(0xFFFB7185), contentColor.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun LegendDot(text: String, color: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

@Composable
private fun ModernTokenBars(buckets: List<Bucket>, maxToken: Int, labelColor: Color) {
    val inputColor = Color(0xFF6BA4F8) // 淡天蓝
    val outputColor = Color(0xFF38BDF8) // 淡青蓝
    val thinkingColor = Color(0xFFFB7185) // 淡珊瑚粉
    val otherColor = Color(0xFFA5B4FC) // 淡紫
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val plotBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val emptyBarColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            AxisLabels(
                labels = listOf(formatNumber(maxToken), formatNumber(maxToken / 2), "0"),
                height = 180.dp,
                color = labelColor
            )
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            ) {
                // 背景区域
                drawRoundRect(
                    color = plotBackground,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                // 横向刻度网格线
                repeat(4) { line ->
                    val y = size.height * line / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.8.dp.toPx())
                }

                if (buckets.isEmpty()) return@Canvas
                val slot = size.width / buckets.size.coerceAtLeast(1)
                val barWidth = (slot * 0.60f).coerceIn(8.dp.toPx(), 28.dp.toPx())

                buckets.forEachIndexed { index, bucket ->
                    val left = slot * index + (slot - barWidth) / 2f
                    val total = bucket.totalTokens.coerceAtLeast(0)

                    // 空数据绘制微型基准胶囊
                    if (total <= 0) {
                        drawRoundRect(
                            color = emptyBarColor,
                            topLeft = Offset(left, size.height - 3.dp.toPx()),
                            size = Size(barWidth, 3.dp.toPx()),
                            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                        )
                        return@forEachIndexed
                    }

                    val fullHeight = (size.height * total / maxToken.coerceAtLeast(1).toFloat())
                        .coerceIn(4.dp.toPx(), size.height)
                    var bottom = size.height

                    // 依次堆叠绘制柱体段（带圆角和分层阴影）
                    fun drawBarSegment(value: Int, color: Color, isTopSegment: Boolean) {
                        if (value <= 0) return
                        val segmentHeight = (fullHeight * value / total.toFloat()).coerceAtLeast(2.dp.toPx())
                        val top = bottom - segmentHeight
                        val corner = if (isTopSegment) CornerRadius(4.dp.toPx(), 4.dp.toPx()) else CornerRadius.Zero

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, segmentHeight),
                            cornerRadius = corner
                        )
                        bottom -= segmentHeight
                    }

                    val hasInput = bucket.inputTokens > 0
                    val hasOutput = bucket.outputTokens > 0
                    val hasThinking = bucket.thinkingTokens > 0
                    val hasOther = bucket.otherTokens > 0

                    drawBarSegment(bucket.otherTokens, otherColor, isTopSegment = !hasInput && !hasOutput && !hasThinking)
                    drawBarSegment(bucket.thinkingTokens, thinkingColor, isTopSegment = !hasInput && !hasOutput)
                    drawBarSegment(bucket.outputTokens, outputColor, isTopSegment = !hasInput)
                    drawBarSegment(bucket.inputTokens, inputColor, isTopSegment = true)
                }
            }
        }
        XAxisLabels(buckets = buckets)
    }
}

@Composable
private fun ModernTrendChart(
    buckets: List<Bucket>,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val successColor = Color(0xFF38BDF8) // 淡青蓝
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    val plotBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val haloColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            AxisLabels(labels = listOf("100%", "50%", "0%"), height = 180.dp, color = labelColor)
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp)
            ) {
                drawRoundRect(
                    color = plotBackground,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                repeat(5) { line ->
                    val y = size.height * line / 4f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }

                fun drawSmoothCurve(values: List<Float>, color: Color) {
                    if (values.isEmpty()) return
                    val path = Path()
                    val areaPath = Path()
                    val points = values.mapIndexed { index, value ->
                        val x = if (values.size == 1) size.width / 2f else size.width * index / (values.size - 1)
                        val y = size.height * (1f - value.coerceIn(0f, 1f))
                        Offset(x, y)
                    }

                    points.forEachIndexed { index, point ->
                        if (index == 0) {
                            path.moveTo(point.x, point.y)
                            areaPath.moveTo(point.x, size.height)
                            areaPath.lineTo(point.x, point.y)
                        } else {
                            val prev = points[index - 1]
                            val midX = (prev.x + point.x) / 2f
                            path.cubicTo(midX, prev.y, midX, point.y, point.x, point.y)
                            areaPath.cubicTo(midX, prev.y, midX, point.y, point.x, point.y)
                        }
                    }

                    if (points.isNotEmpty()) {
                        areaPath.lineTo(points.last().x, size.height)
                        areaPath.close()
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.28f), Color.Transparent),
                                startY = 0f,
                                endY = size.height
                            )
                        )
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEach { point ->
                        drawCircle(color = haloColor, radius = 5.dp.toPx(), center = point)
                        drawCircle(color = color, radius = 3.2.dp.toPx(), center = point)
                    }
                }

                drawSmoothCurve(buckets.map { it.successRate }, successColor)
            }
        }
        XAxisLabels(buckets = buckets)
    }
}

@Composable
private fun AxisLabels(labels: List<String>, height: androidx.compose.ui.unit.Dp, color: Color) {
    Column(
        modifier = Modifier
            .width(44.dp)
            .height(height)
            .padding(end = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = color,
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
            .padding(start = 44.dp, top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun ModernModelStatsTable(
    hazeState: dev.chrisbanes.haze.HazeState,
    rows: List<ModelRow>,
    readableBackdrop: Color
) {
    val glass = echoGlassPalette()
    val tint = glass.panelStrong
    val content = readableTextColorFor(
        background = tint,
        fallbackSurface = readableBackdrop
    )
    var sortMode by remember { mutableIntStateOf(0) } // 0: Tokens, 1: 请求数, 2: 成功率, 3: 平均耗时

    val sortedRows = remember(rows, sortMode) {
        when (sortMode) {
            0 -> rows.sortedByDescending { it.totalTokens }
            1 -> rows.sortedByDescending { it.requestCount }
            2 -> rows.sortedByDescending { it.successRate }
            3 -> rows.sortedBy { if (it.avgResponseTime <= 0) Long.MAX_VALUE else it.avgResponseTime }
            else -> rows
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .echoHazePanel(
                hazeState = hazeState,
                shape = EchoGlassPagePanelShape,
                tint = tint,
                blurRadius = 18.dp
            )
            .background(tint, EchoGlassPagePanelShape)
            .border(BorderStroke(1.dp, glass.outline), EchoGlassPagePanelShape)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 表头与排序切换栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "模型统计明细",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = content
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${rows.size}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Tokens", "请求数", "成功率", "耗时").forEachIndexed { idx, title ->
                        val isSelected = sortMode == idx
                        val chipShape = RoundedCornerShape(8.dp)
                        Box(
                            modifier = Modifier
                                .clip(chipShape)
                                .background(if (isSelected) glass.controlSelected else glass.control.copy(alpha = 0.6f))
                                .border(
                                    BorderStroke(
                                        if (isSelected) 1.dp else 0.6.dp,
                                        if (isSelected) glass.outlineSelected else glass.outline.copy(alpha = 0.6f)
                                    ),
                                    chipShape
                                )
                                .echoShapeClick(chipShape) { sortMode = idx }
                                .padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else content.copy(alpha = 0.72f)
                            )
                        }
                    }
                }
            }

            // 极简精致模型列表卡片
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sortedRows.forEach { row ->
                    val rowShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(glass.control.copy(alpha = 0.52f))
                            .border(BorderStroke(0.8.dp, glass.outline.copy(alpha = 0.45f)), rowShape)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // 第一行：模型名称 + 供应商标签 + 总 Token
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = row.modelName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.SansSerif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp
                                        ),
                                        color = content,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (row.provider.isNotBlank() && row.provider != "unknown") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(glass.control.copy(alpha = 0.8f))
                                                .border(BorderStroke(0.5.dp, glass.outline.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = row.provider,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = content.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${formatNumber(row.totalTokens)} Tokens",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // 第二行：比例横条（输入/输出/思考可视化分布）
                            if (row.totalTokens > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(glass.control.copy(alpha = 0.8f))
                                ) {
                                    val inputRatio = (row.inputTokens.toFloat() / row.totalTokens).coerceIn(0f, 1f)
                                    val outputRatio = (row.outputTokens.toFloat() / row.totalTokens).coerceIn(0f, 1f)
                                    val thinkingRatio = (row.thinkingTokens.toFloat() / row.totalTokens).coerceIn(0f, 1f)

                                    if (inputRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(inputRatio)
                                                .fillMaxHeight()
                                                .background(Color(0xFF6BA4F8))
                                        )
                                    }
                                    if (outputRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(outputRatio)
                                                .fillMaxHeight()
                                                .background(Color(0xFF38BDF8))
                                        )
                                    }
                                    if (thinkingRatio > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(thinkingRatio)
                                                .fillMaxHeight()
                                                .background(Color(0xFFFB7185))
                                        )
                                    }
                                }
                            }

                            // 第三行：多维度紧凑数据标签
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniStatsChip(
                                    icon = Icons.Default.Send,
                                    label = "${row.requestCount}次请求",
                                    tint = content.copy(alpha = 0.78f)
                                )
                                MiniStatsChip(
                                    icon = Icons.Default.CheckCircle,
                                    label = "成功率 ${formatPercent(row.successRate)}",
                                    tint = Color(0xFF38BDF8)
                                )
                                if (row.avgResponseTime > 0) {
                                    MiniStatsChip(
                                        icon = Icons.Default.Timer,
                                        label = "${row.avgResponseTime}ms",
                                        tint = content.copy(alpha = 0.78f)
                                    )
                                }
                                if (row.thinkingTokens > 0) {
                                    MiniStatsChip(
                                        icon = Icons.Default.Psychology,
                                        label = "思考 ${formatNumber(row.thinkingTokens)}",
                                        tint = Color(0xFFFB7185)
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

@Composable
private fun MiniStatsChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 6.dp, vertical = 2.5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontFamily = FontFamily.SansSerif
            ),
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyCard(
    hazeState: dev.chrisbanes.haze.HazeState,
    text: String,
    readableBackdrop: Color
) {
    val tint = echoGlassPalette().panelStrong
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
            )
            .background(tint, EchoGlassPagePanelShape),
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
                return@withContext StatsReadResult(emptyList(), "统计表不可读，之后会重新记录")
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
    val output = sumOf { it.outputTokens }
    val thinking = sumOf { it.thinkingTokens }
    val cached = sumOf { it.cachedTokens }
    val successCount = count { it.success }
    return UsageSummary(
        totalTokens = sumOf { it.totalTokens },
        inputTokens = input,
        outputTokens = output,
        thinkingTokens = thinking,
        cachedTokens = cached,
        requestCount = size,
        cacheHitRate = if (input > 0) cached.toFloat() / input else 0f,
        successRate = if (isNotEmpty()) successCount.toFloat() / size else 0f
    )
}

private fun List<UsageRow>.toModelRows(): List<ModelRow> {
    return groupBy { it.provider to it.modelName }
        .map { (key, rows) ->
            val input = rows.sumOf { it.inputTokens }
            val cached = rows.sumOf { it.cachedTokens }
            ModelRow(
                provider = key.first,
                modelName = key.second,
                inputTokens = input,
                outputTokens = rows.sumOf { it.outputTokens },
                thinkingTokens = rows.sumOf { it.thinkingTokens },
                cachedTokens = cached,
                totalTokens = rows.sumOf { it.totalTokens },
                requestCount = rows.size,
                avgResponseTime = if (rows.isNotEmpty()) rows.map { it.responseTime }.average().toLong() else 0L,
                cacheHitRate = if (input > 0) cached.toFloat() / input else 0f,
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
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val thinkingTokens: Int = 0,
    val cachedTokens: Int = 0,
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
    val inputTokens: Int,
    val outputTokens: Int,
    val thinkingTokens: Int,
    val cachedTokens: Int,
    val totalTokens: Int,
    val requestCount: Int,
    val avgResponseTime: Long,
    val cacheHitRate: Float,
    val successRate: Float
)
