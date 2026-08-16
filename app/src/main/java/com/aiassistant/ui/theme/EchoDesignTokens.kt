package com.aiassistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Echo 设计令牌体系 (Design Tokens)
 * 集中管理全应用的间距、圆角、模糊规格、透明度与排版尺寸，确保全应用视觉语言的高度一致性。
 */
object EchoTokens {

    /**
     * 间距规范 (Spacing)
     */
    object Spacing {
        val xxs: Dp = 2.dp
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 20.dp
        val xxl: Dp = 28.dp
        val screenHorizontal: Dp = 14.dp
        val screenVertical: Dp = 8.dp
        val itemSpacing: Dp = 10.dp
    }

    /**
     * 圆角规范 (Radius)
     */
    object Radius {
        val xs: Dp = 6.dp
        val sm: Dp = 8.dp       // 标签、小徽章、紧凑组件
        val md: Dp = 12.dp      // 普通按钮、输入框、辅助卡片
        val lg: Dp = 18.dp      // 核心业务卡片、对话气泡、分块容器
        val xl: Dp = 24.dp      // 模态弹窗、主悬浮栏、底部面板
        val pill: Dp = 999.dp   // 全胶囊组件 (如状态胶囊、浮动导航条)

        val shapeXs: Shape = RoundedCornerShape(xs)
        val shapeSm: Shape = RoundedCornerShape(sm)
        val shapeMd: Shape = RoundedCornerShape(md)
        val shapeLg: Shape = RoundedCornerShape(lg)
        val shapeXl: Shape = RoundedCornerShape(xl)
        val shapePill: Shape = RoundedCornerShape(pill)
    }

    /**
     * 毛玻璃与液态流体渲染规范 (Glass Specification)
     */
    object Glass {
        // 卡片透明度 (清透晶莹，告别沉重死板)
        const val cardAlphaLight: Float = 0.28f
        const val cardAlphaDark: Float = 0.38f

        // 面板与对话框透明度
        const val panelAlphaLight: Float = 0.65f
        const val panelAlphaDark: Float = 0.75f
        const val dialogAlphaLight: Float = 0.85f
        const val dialogAlphaDark: Float = 0.90f

        // 控件透明度
        const val controlAlphaLight: Float = 0.22f
        const val controlAlphaDark: Float = 0.30f
        const val inputAlphaLight: Float = 0.38f
        const val inputAlphaDark: Float = 0.50f

        // 边框与高光规范
        val borderWidth: Dp = 1.dp
        val activeBorderWidth: Dp = 1.3.dp
        const val borderAlphaLight: Float = 0.22f
        const val borderAlphaDark: Float = 0.14f
        const val highlightAlphaLight: Float = 0.08f
        const val highlightAlphaDark: Float = 0.05f

        // 模糊半径
        val blurRadiusStandard: Dp = 20.dp
        val blurRadiusHeavy: Dp = 30.dp
        val blurRadiusSubtle: Dp = 12.dp
    }

    /**
     * 阴影标尺 (Elevation)
     */
    object Elevation {
        val none: Dp = 0.dp
        val subtle: Dp = 2.dp
        val standard: Dp = 4.dp
        val floating: Dp = 8.dp
    }
}
