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
import kotlin.math.abs
import kotlin.math.cos

/**
 * Echo 连接脉冲环（方案 P0-1）
 * 一圈圆环以呼吸式缩放 + 透明度脉冲表达"连接中/思考中"的活动感。
 *
 * v2.7.2 需求 4：新增 animated 参数——回复完毕后的落定态以「静态圆环」呈现（animated=false，
 * 不创建 InfiniteTransition），生成全程（连接→思考→流式）保持 animated=true 的圆环动效。
 *
 * 性能约束（R-3/R-7）：单个 InfiniteTransition 驱动，动画值仅在 Canvas（draw 阶段）读取，
 * 零重组、零每帧对象分配。reduced motion / animated=false 时退化为静态圆环。
 */
@Composable
fun EchoPulseRing(
    color: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 16.dp,
    strokeWidth: Dp = 1.8.dp,
    animated: Boolean = true
) {
    val reduced = rememberReducedMotion()
    // 检查报告 P3-2：reduced motion / 静态圆环下条件创建 InfiniteTransition，避免帧回调空转（微功耗）
    val pulse: Float = if (reduced) {
        0.8f
    } else if (!animated) {
        1f
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
        val alpha = if (reduced || !animated) 0.85f else pulse
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
 * Echo 思考等待指示器「呼吸光环点」（替代原波浪点，供 TypingIndicator 使用）
 * 三个圆点按相位差做呼吸脉冲：点亮时柔和放大、提亮，身后同步扩散出一圈渐隐光环，
 * 脉冲只占周期前 55%、余下时间静息，节奏从容，表达"正在思考/等待首 token"。
 *
 * v2.7.7 需求 5 精致化（全部为绘制层细节，性能约束不变）：
 * ① 双层光晕——近层辉光与脉冲同步呼吸、远层光环随脉冲进程扩散渐隐，层次更立体；
 * ② 峰值轻微上浮——点在脉冲顶点向上漂移小半半径，有"呼吸悬浮"的生命感；
 * ③ 中点略大、两侧略小的尺寸阶梯 + 左→右亮度递进，构图与动效更有方向感；
 * ④ 脉冲峰值在点内叠加一枚偏移高光，呈现珠光质感。
 *
 * 配色约定：消费 generationAccentColor（思考模型取思考档位色，否则 primary），
 * 与流式光标、脉冲环同源（P0-1②/P0-2②）。
 *
 * 性能约束（R-3/R-7）：单 InfiniteTransition + Canvas，动画值仅在 draw 阶段读取，
 * 零重组、零每帧堆分配（Color/Offset 均为值类型）。reduced motion 时退化为静态三点。
 */
@Composable
fun EchoThinkingDots(
    color: Color,
    modifier: Modifier = Modifier,
    dotRadius: Dp = 2.8.dp
) {
    val reduced = rememberReducedMotion()
    // 检查报告 P3-2：reduced motion 下条件创建 InfiniteTransition，避免帧回调空转
    val t: Float = if (reduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "echoThinkingDots")
        val v by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = EchoMotion.linearCycleSpec(EchoMotion.Cycle.thinkingDots),
            label = "dotPhase"
        )
        v
    }
    Canvas(modifier) {
        val r = dotRadius.toPx()
        val gap = r * 3.4f
        val totalWidth = gap * 2
        val baseY = size.height / 2f
        repeat(3) { i ->
            val x = size.width / 2f - totalWidth / 2f + i * gap
            // 相位错开：三点依次点亮；reduced 时 t=0 全部处于静息相位
            val phase = (t - i * 0.24f).mod(1f)
            // 呼吸窗：脉冲压缩在周期前 55%（0→1），后 45% 静息（固定 1，alpha 归零）
            val pulsePhase = (phase / 0.55f).coerceIn(0f, 1f)
            val wave = if (reduced) 0f else 0.5f - 0.5f * cos((pulsePhase * 2.0 * PI).toFloat())
            // 尺寸阶梯：中点 +8%，构图更稳；亮度阶梯：左→右递进，动效有方向感
            val sizeStagger = 1f + 0.08f * (1f - abs(i - 1))
            val brightStagger = 0.78f + 0.22f * (i / 2f)
            // 峰值轻微上浮（小半半径），静息时落回基线
            val dotY = baseY - r * 0.6f * wave
            // 远层光环：随脉冲进程线性扩散（2.2r→3.7r）、线性渐隐，静息相位 alpha 为 0 不绘制
            val haloAlpha = 0.14f * (1f - pulsePhase)
            if (!reduced && haloAlpha > 0.004f) {
                drawCircle(
                    color = color.copy(alpha = haloAlpha),
                    radius = r * (2.2f + 1.5f * pulsePhase) * sizeStagger,
                    center = Offset(x, dotY)
                )
            }
            // 近层辉光：与脉冲同步呼吸（1.9r→2.4r），叠加出层次
            if (!reduced && wave > 0.02f) {
                drawCircle(
                    color = color.copy(alpha = 0.16f * wave),
                    radius = r * (1.9f + 0.5f * wave) * sizeStagger,
                    center = Offset(x, dotY)
                )
            }
            // 主体点：呼吸式放大 + 提亮
            drawCircle(
                color = color.copy(alpha = (0.30f + 0.70f * wave) * brightStagger),
                radius = r * (1f + 0.38f * wave) * sizeStagger,
                center = Offset(x, dotY)
            )
            // 峰值高光：点内左上偏移一枚小白点，珠光质感
            if (!reduced && wave > 0.35f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.30f * (wave - 0.35f) / 0.65f),
                    radius = r * 0.28f * sizeStagger,
                    center = Offset(x - r * 0.32f, dotY - r * 0.32f)
                )
            }
        }
    }
}
