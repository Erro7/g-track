package com.gtrack.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * 线性图标库（Lucide 风格 24 网格，圆帽描边）——
 * 替换所有文字符号图标（‹ ⧉ ✓ — · 之类），DESIGN.md 图标红线要求。
 */
@Composable
fun GTLineIcon(
    icon: GTIcon,
    size: Dp = 24.dp,
    tint: Color,
    strokeWidth: Float = 1.9f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        // 必须绕原点缩放：默认 pivot 是画布中心，会让 24 网格整体缩进中间（图标"飘"的根因）
        scale(s, s, pivot = Offset.Zero) {
            val w = strokeWidth
            icon.draw(this, tint, w)
        }
    }
}

enum class GTIcon { Back, ChevronDown, ChevronRight, Scan, Crosshair, Copy, Wifi, Refresh, Radar, Clock, Gear, Pin, External, Check, Minus, User, Phone, Globe, Trash, Shield,
    ShieldCheck, Device, ScanFrame, UserSearch, Moon, Sun, Auto;

    fun draw(scope: DrawScope, c: Color, w: Float) {
        val stroke = Stroke(w, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        fun p(builder: Path.() -> Unit) = scope.drawPath(Path().apply(builder), c, style = stroke)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = p { moveTo(x1, y1); lineTo(x2, y2) }
        fun circle(cx: Float, cy: Float, r: Float, fill: Boolean = false) {
            if (fill) scope.drawCircle(c, r, Offset(cx, cy))
            else scope.drawCircle(c, r, Offset(cx, cy), style = stroke)
        }
        when (this) {
            Back -> p { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
            ChevronRight -> p { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
            Copy -> {
                p {
                    moveTo(15.5f, 6.5f); lineTo(15.5f, 5.5f)
                    // 后侧 L 形圆角
                    quadraticBezierTo(15.5f, 3.5f, 13.5f, 3.5f)
                    lineTo(5.5f, 3.5f)
                    quadraticBezierTo(3.5f, 3.5f, 3.5f, 5.5f)
                    lineTo(3.5f, 13.5f)
                    quadraticBezierTo(3.5f, 15.5f, 5.5f, 15.5f)
                    lineTo(6.5f, 15.5f)
                }
                scope.drawRoundRect(c, Offset(8f, 8f), androidx.compose.ui.geometry.Size(12f, 12f), androidx.compose.ui.geometry.CornerRadius(2.5f), style = stroke)
            }
            Wifi -> {
                circle(12f, 18.6f, 1.5f, fill = true)
                listOf(4.2f, 8f, 11.8f).forEach { r ->
                    scope.drawArc(c, startAngle = -135f, sweepAngle = 90f, useCenter = false,
                        topLeft = Offset(12f - r, 18.6f - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        style = stroke)
                }
            }
            Refresh -> {
                // rotate-cw（Lucide / 原型 #ipRefresh）：近整圈圆弧 + 缺口处直角箭头
                // 箭头顶点必须落在弧的起点上（旧实现弧起点在顶部、箭头却在右上，两段是断开的）
                scope.drawArc(c, startAngle = -7f, sweepAngle = -309f, useCenter = false,
                    topLeft = Offset(4f, 4f), size = androidx.compose.ui.geometry.Size(16f, 16f), style = stroke)
                p { moveTo(20f, 5f); lineTo(20f, 11f); lineTo(14f, 11f) }
            }
            Radar -> {
                circle(12f, 13f, 1.7f, fill = true)
                scope.drawArc(c, startAngle = -110f, sweepAngle = 130f, useCenter = false,
                    topLeft = Offset(7f, 8f), size = androidx.compose.ui.geometry.Size(10f, 10f), style = stroke)
                scope.drawArc(c, startAngle = -110f, sweepAngle = 130f, useCenter = false,
                    topLeft = Offset(3.5f, 4.5f), size = androidx.compose.ui.geometry.Size(17f, 17f), style = stroke)
                line(12f, 13f, 15.2f, 9.8f)
            }
            Clock -> {
                circle(12f, 12f, 8.4f)
                p { moveTo(12f, 7.2f); lineTo(12f, 12f); lineTo(15.2f, 14f) }
            }
            Gear -> {
                circle(12f, 12f, 3.1f)
                for (i in 0 until 8) {
                    val a = Math.toRadians((i * 45).toDouble())
                    line(12f + (5.4f * cos(a)).toFloat(), 12f + (5.4f * sin(a)).toFloat(),
                        12f + (8.1f * cos(a)).toFloat(), 12f + (8.1f * sin(a)).toFloat())
                }
            }
            Pin -> {
                p {
                    moveTo(12f, 21f)
                    // 泪滴轮廓
                    cubicTo(9f, 18f, 5.5f, 14.4f, 5.5f, 10.4f)
                    arcTo(androidx.compose.ui.geometry.Rect(5.5f, 3.9f, 18.5f, 16.9f), 180f, -180f, false)
                    cubicTo(18.5f, 14.4f, 15f, 18f, 12f, 21f)
                    close()
                }
                circle(12f, 10.4f, 2.4f)
            }
            External -> {
                line(14f, 4.5f, 19.5f, 4.5f); line(19.5f, 4.5f, 19.5f, 10f); line(19.5f, 4.5f, 11f, 13f)
                p {
                    moveTo(19.5f, 13.5f); lineTo(19.5f, 17.5f)
                    quadraticBezierTo(19.5f, 19.5f, 17.5f, 19.5f)
                    lineTo(6.5f, 19.5f)
                    quadraticBezierTo(4.5f, 19.5f, 4.5f, 17.5f)
                    lineTo(4.5f, 6.5f)
                    quadraticBezierTo(4.5f, 4.5f, 6.5f, 4.5f)
                    lineTo(10.5f, 4.5f)
                }
            }
            Check -> p { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
            Minus -> line(6.5f, 12f, 17.5f, 12f)
            User -> {
                circle(12f, 7.8f, 3.6f)
                p {
                    moveTo(4.8f, 20.6f)
                    quadraticBezierTo(4.8f, 14.8f, 12f, 14.8f)
                    quadraticBezierTo(19.2f, 14.8f, 19.2f, 20.6f)
                }
            }
            Phone -> {
                scope.drawRoundRect(c, Offset(7f, 2.5f), androidx.compose.ui.geometry.Size(10f, 19f), androidx.compose.ui.geometry.CornerRadius(2.6f), style = stroke)
                line(10.8f, 18.4f, 13.2f, 18.4f)
            }
            Globe -> {
                circle(12f, 12f, 8.4f)
                scope.drawOval(c, Offset(8.2f, 3.6f), androidx.compose.ui.geometry.Size(7.6f, 16.8f), style = stroke)
                line(3.6f, 12f, 20.4f, 12f)
            }
            Scan -> {
                // 取景框四角 + 中心点（对齐原型 onboarding mark）
                p {
                    moveTo(9f, 3.5f); lineTo(5.6f, 3.5f)
                    quadraticBezierTo(3.5f, 3.5f, 3.5f, 5.6f); lineTo(3.5f, 9f)
                    moveTo(15f, 3.5f); lineTo(18.4f, 3.5f)
                    quadraticBezierTo(20.5f, 3.5f, 20.5f, 5.6f); lineTo(20.5f, 9f)
                    moveTo(15f, 20.5f); lineTo(18.4f, 20.5f)
                    quadraticBezierTo(20.5f, 20.5f, 20.5f, 18.4f); lineTo(20.5f, 15f)
                    moveTo(9f, 20.5f); lineTo(5.6f, 20.5f)
                    quadraticBezierTo(3.5f, 20.5f, 3.5f, 18.4f); lineTo(3.5f, 15f)
                }
                circle(12f, 12f, 1.8f, fill = true)
            }
            Crosshair -> {
                circle(12f, 12f, 7f)
                line(12f, 2.4f, 12f, 5.4f)
                line(12f, 18.6f, 12f, 21.6f)
                line(2.4f, 12f, 5.4f, 12f)
                line(18.6f, 12f, 21.6f, 12f)
                circle(12f, 12f, 1.3f, fill = true)
            }
            ChevronDown -> {
                p { moveTo(6f, 9.5f); lineTo(12f, 15.5f); lineTo(18f, 9.5f) }
            }
            Trash -> {
                line(4.5f, 6.5f, 19.5f, 6.5f)
                p {
                    moveTo(8f, 6.5f); lineTo(8f, 4.8f)
                    quadraticBezierTo(8f, 3.5f, 9.3f, 3.5f)
                    lineTo(14.7f, 3.5f)
                    quadraticBezierTo(16f, 3.5f, 16f, 4.8f)
                    lineTo(16f, 6.5f)
                    moveTo(6.5f, 6.5f); lineTo(7.5f, 19f)
                    quadraticBezierTo(7.6f, 20.5f, 9.1f, 20.5f)
                    lineTo(14.9f, 20.5f)
                    quadraticBezierTo(16.4f, 20.5f, 16.5f, 19f)
                    lineTo(17.5f, 6.5f)
                }
            }
            Shield -> {
                p {
                    moveTo(12f, 3f); lineTo(19.5f, 6.2f); lineTo(19.5f, 11.2f)
                    quadraticBezierTo(19.5f, 17.5f, 12f, 20.8f)
                    quadraticBezierTo(4.5f, 17.5f, 4.5f, 11.2f)
                    lineTo(4.5f, 6.2f); close()
                }
            }
            // 合规/授权语义：盾 + 勾（原型 onboarding 点与首页声明入口）
            ShieldCheck -> {
                p {
                    moveTo(12f, 3f); lineTo(19.5f, 6.2f); lineTo(19.5f, 11.2f)
                    quadraticBezierTo(19.5f, 17.5f, 12f, 20.8f)
                    quadraticBezierTo(4.5f, 17.5f, 4.5f, 11.2f)
                    lineTo(4.5f, 6.2f); close()
                }
                p { moveTo(9f, 12f); lineTo(11f, 14f); lineTo(15f, 9.5f) }
            }
            // 本机存储语义：外框 + 内芯（原型 onboarding 第三点）
            Device -> {
                scope.drawRoundRect(c, Offset(4f, 4f), androidx.compose.ui.geometry.Size(16f, 16f), CornerRadius(3f), style = stroke)
                scope.drawRoundRect(c, Offset(9f, 9f), androidx.compose.ui.geometry.Size(6f, 6f), CornerRadius(1.5f), style = stroke)
            }
            // IP 定位语义：取景框 + 中心圆（原型 IC.ip / 首页 Tab 同族）
            ScanFrame -> {
                p {
                    moveTo(8f, 4f); lineTo(5.5f, 4f)
                    quadraticBezierTo(4f, 4f, 4f, 5.5f); lineTo(4f, 8f)
                    moveTo(16f, 4f); lineTo(18.5f, 4f)
                    quadraticBezierTo(20f, 4f, 20f, 5.5f); lineTo(20f, 8f)
                    moveTo(20f, 16f); lineTo(20f, 18.5f)
                    quadraticBezierTo(20f, 20f, 18.5f, 20f); lineTo(16f, 20f)
                    moveTo(8f, 20f); lineTo(5.5f, 20f)
                    quadraticBezierTo(4f, 20f, 4f, 18.5f); lineTo(4f, 16f)
                }
                circle(12f, 12f, 2.6f)
                line(6.5f, 12f, 9.4f, 12f)
                line(14.6f, 12f, 17.5f, 12f)
            }
            // 用户名扫描语义：人像 + 放大镜（DESIGN.md §8 user + magnifier）
            UserSearch -> {
                circle(10f, 8f, 3.4f)
                p {
                    moveTo(4.5f, 19.5f)
                    cubicTo(5.2f, 16.5f, 7.4f, 14.7f, 10f, 14.7f)
                    cubicTo(11f, 14.7f, 12f, 14.95f, 12.8f, 15.42f)
                }
                circle(16.4f, 15.4f, 3f)
                line(18.7f, 17.7f, 21f, 20f)
            }
            Sun -> {
                circle(12f, 12f, 4f)
                line(12f, 2.5f, 12f, 4.9f)
                line(12f, 19.1f, 12f, 21.5f)
                line(2.5f, 12f, 4.9f, 12f)
                line(19.1f, 12f, 21.5f, 12f)
                line(4.9f, 4.9f, 6.6f, 6.6f)
                line(17.4f, 17.4f, 19.1f, 19.1f)
                line(19.1f, 4.9f, 17.4f, 6.6f)
                line(6.6f, 17.4f, 4.9f, 19.1f)
            }
            // 月牙：外凸弧 + 内凹弧同径偏移闭合（原型 .seg 深色项）
            Moon -> p {
                arcTo(androidx.compose.ui.geometry.Rect(3.1f, 2.9f, 20.1f, 19.9f), 13.8f, 248f, false)
                arcTo(androidx.compose.ui.geometry.Rect(10.4f, -3.4f, 27.4f, 13.6f), 187.2f, -104.4f, false)
                close()
            }
            // circle-half（Lucide 风格）：整圈描边 + 右半实心 = 自动 / 跟随系统
            Auto -> {
                scope.drawArc(
                    c, startAngle = -90f, sweepAngle = 180f, useCenter = true,
                    topLeft = Offset(4f, 4f), size = androidx.compose.ui.geometry.Size(16f, 16f),
                )
                circle(12f, 12f, 8f)
            }
        }
    }
}
