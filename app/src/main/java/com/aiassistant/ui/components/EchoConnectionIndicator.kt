package com.aiassistant.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import com.aiassistant.ui.theme.EchoMotion
import com.aiassistant.ui.theme.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.cos

/**
 * Echo 连接脉冲环（方案 P0-1）
 * 一圈圆环以呼吸式缩放 + 透明度脉冲表达"连接中/思考中"的活动感。
 *
 * 性能约束（R-3/R-7）：单个 InfiniteTransition 驱动，动画值仅在 Canvas（draw 阶段）读取，
 * 零重组、零每帧对象分配。reduced motion 时退化为静态圆环。
 */
@Composable
fun EchoPulseRing(
    color: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 16.dp,
    strokeWidth: Dp = 1.8.dp
) {
    val reduced = rememberReducedMotion()
    // 检查报告 P3-2：reduced motion 下条件创建 InfiniteTransition，避免帧回调空转（微功耗）
    val pulse: Float = if (reduced) {
        0.8f
    } else {
        val transition = rememberInfiniteTransition(label = "echoPulseRing")
        val v by transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = EchoMotion.reverseCycleSpec<Float>(EchoMotion.Cycle.pulse),
            label = "pulseScale"
        )
        v
    }
    Canvas(modifier) {
        val radius = ringSize.toPx() / 2f - strokeWidth.toPx() / 2f
        val scale = pulse
        val alpha = if (reduced) 0.85f else pulse
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius * scale,
            center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Echo 重连双弧环（方案 P0-1 ④）
 * 两段弧以 1.2s/圈 追逐旋转，表达"重连进行中"。
 * 与脉冲环同一性能约束：仅 draw 阶段动画，reduced motion 时退化为静态单弧。
 */
@Composable
fun EchoDoubleArcRing(
    color: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 16.dp,
    strokeWidth: Dp = 1.8.dp
) {
    val reduced = rememberReducedMotion()
    // 检查报告 P3-2：reduced motion 下条件创建（静态 0° 单弧）
    val rotation: Float = if (reduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "echoDoubleArc")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = EchoMotion.linearCycleSpec(EchoMotion.Cycle.reconnectArc),
            label = "arcRotation"
        )
        v
    }
    Canvas(modifier) {
        val radius = ringSize.toPx() / 2f - strokeWidth.toPx() / 2f
        val sweep = 100f
        val baseRotation = rotation
        val center = Offset(size.width / 2f, size.height / 2f)
        // 双弧追逐：主弧 100° + 次弧 60°，相位差 180°
        drawArc(
            color = color,
            startAngle = baseRotation,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color.copy(alpha = 0.45f),
            startAngle = baseRotation + 180f,
            sweepAngle = sweep * 0.6f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Echo 思考档位波浪点（供 TypingIndicator 复用的绘制核）
 * 三个圆点按相位差做"上浮-回落"波浪，透明度同步呼吸——iMessage 风格。
 * 单 InfiniteTransition + Canvas：一次组合、零重组、零字符串分配（R-3/R-7）。
 */
@Composable
fun EchoWaveDots(
    color: Color,
    modifier: Modifier = Modifier,
    dotRadius: Dp = 3.dp,
    lift: Dp = 3.dp
) {
    val reduced = rememberReducedMotion()
    // 检查报告 P3-2：reduced motion 下条件创建（t 固定 0.5f → 三点静止居中）
    val t: Float = if (reduced) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "echoWaveDots")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = EchoMotion.linearCycleSpec(EchoMotion.Cycle.typingDots),
            label = "dotPhase"
        )
        v
    }
    Canvas(modifier) {
        val r = dotRadius.toPx()
        val liftPx = lift.toPx()
        val gap = r * 2.8f
        val totalWidth = gap * 2
        val centerY = size.height / 2f
        repeat(3) { i ->
            val x = size.width / 2f - totalWidth / 2f + i * gap
            val phase = (t - i * 0.18f).mod(1f)
            // cos(2π·phase)：1→0→1，取反得 0→1→0 平滑上浮回落
            val wave = 1f - (0.5f - 0.5f * cos((phase * 2.0 * PI).toFloat()))
            val dotY = centerY - wave * liftPx
            val alpha = 0.4f + 0.6f * (1f - wave)
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = r,
                center = Offset(x, dotY)
            )
        }
    }
}
