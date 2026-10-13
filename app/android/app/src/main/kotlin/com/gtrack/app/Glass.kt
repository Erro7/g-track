package com.gtrack.app

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * 真实背景模糊（对齐原型 backdrop-filter: blur(20px) saturate(1.8)）。
 *
 * Compose 没有 backdrop-filter，此前只能用「半透明填充」近似：深色底下深色玻璃几乎不可见，
 * 且内容滚过栏底时是锐利的，与原型玻璃观感不符。这里改为离屏采样方案：
 *  1. 内容容器用 [recordGlassBackdrop] 把自身绘制录制进离屏层；
 *  2. 玻璃条用 [gtGlassSurface] 按自身位置对齐后把该层模糊绘制到自己的背景里。
 *
 * 采样层必须与玻璃条是兄弟节点（不能把玻璃自身录进去，否则回环）。
 */
class GlassBackdrop internal constructor() {
    internal var layer: GraphicsLayer? = null
    /** 采样内容在根布局中的位置：玻璃条绘制时用它把采样层对齐到自身坐标系 */
    internal var offset: Offset = Offset.Zero
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val context = LocalGraphicsContext.current
    val backdrop = remember(context) {
        GlassBackdrop().apply { layer = context.createGraphicsLayer() }
    }
    DisposableEffect(context, backdrop) {
        onDispose { backdrop.layer?.let { context.releaseGraphicsLayer(it) } }
    }
    return backdrop
}

/**
 * 内容侧：把自身绘制内容录制进采样层（作为玻璃的模糊源），再正常绘制自身。
 * 挂在「玻璃条之外」的容器上。
 *
 * @param enabled 无消费者时关掉：省掉整帧的录制开销
 * @param sampleHeight >0 时只录**顶部这一条**区域。全屏录制 + 全屏模糊是滚动掉帧的主因，
 *                     而顶栏只需要自己身后那一条（约 140dp 足够盖住栏高 + 模糊扩散）。
 */
fun Modifier.recordGlassBackdrop(
    backdrop: GlassBackdrop,
    enabled: Boolean = true,
    sampleHeight: Dp = 0.dp,
): Modifier = this
    .onGloballyPositioned { backdrop.offset = it.positionInRoot() }
    .drawWithContent {
        if (enabled) {
            val h = if (sampleHeight > 0.dp) size.height.coerceAtMost(sampleHeight.toPx()) else size.height
            backdrop.layer?.record(
                density = this,
                layoutDirection = layoutDirection,
                size = IntSize(size.width.toInt(), h.toInt()),
            ) {
                this@drawWithContent.drawContent()
            }
        }
        drawContent()
    }

/** 把 Shape 在当前尺寸下解析成裁剪路径（圆角/矩形/任意形状） */
private fun outlinePath(shape: Shape, size: Size, layoutDirection: LayoutDirection, density: Density): Path =
    when (val outline = shape.createOutline(size, layoutDirection, density)) {
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Generic -> outline.path
    }

/**
 * 玻璃侧：先画模糊后的背景，再叠玻璃填充与顶部内高光（原型 .glass inset 0 1px 0）。
 *
 * API 31 以下无 RenderEffect 模糊支持 → 加深填充不透明度兜底，
 * 避免内容直接透视影响标题可读性。
 */
@Composable
fun Modifier.gtGlassSurface(
    backdrop: GlassBackdrop?,
    shape: Shape = RectangleShape,
    blur: Dp = 20.dp,
    fill: Color,
    border: Color? = null,
    borderWidth: Dp = 1.dp,
    /** 顶缘 specular 高光：仅卡片/胶囊类浮动玻璃需要；顶栏只有底部 hairline，不能有这条 */
    specular: Boolean = true,
): Modifier {
    val radiusPx = with(LocalDensity.current) { blur.toPx() }
    val canBlur = backdrop?.layer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // 无模糊时提高不透明度（0.55 → ≈0.91），保证栏上文字可读
    val solidFill = if (canBlur) fill else fill.copy(alpha = (fill.alpha + (1f - fill.alpha) * 0.8f).coerceAtMost(0.97f))
    val highlight = if (LocalGTLight.current) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.16f)
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            // 关键：带 RenderEffect 的采样层会按自身矩形边界合成，
            // 仅靠 Modifier.clip 挡不住圆角外的溢出（底部胶囊因此露出方块）→ 这里再按形状做一次画布裁剪
            clipPath(outlinePath(shape, size, layoutDirection, this)) {
                if (canBlur) {
                    val layer = backdrop!!.layer!!
                    layer.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Decal)
                    layer.colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.6f) })
                    // 离屏合成：保证模糊结果受父级裁剪约束，不溢出形状
                    layer.compositingStrategy = CompositingStrategy.Offscreen
                    translate(-(origin.x - backdrop.offset.x), -(origin.y - backdrop.offset.y)) {
                        drawLayer(layer)
                    }
                }
                drawRect(solidFill)
                if (specular) {
                    drawLine(highlight, Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1f)
                }
            }
        }
        .then(if (border != null) Modifier.border(borderWidth, border, shape) else Modifier)
}
