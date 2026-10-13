package com.gtrack.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

// ================= 色板 token（DESIGN.md §2，深色默认） =================
// token 为 @Composable 读取：随设置页的深/浅切换实时生效

/** App 版本：唯一来源 = Gradle versionName，UI/UA 一律读它，避免硬编码漂移 */
val GTAppVersion: String = BuildConfig.VERSION_NAME

val LocalGTLight = staticCompositionLocalOf { false }

val GTRACKGreen = Color(0xFF30D158)
val GTRACKGreenDeep = Color(0xFF1FA84B)
val GTRACKBgDark = Color(0xFF050B08)
val GTRACKSurfaceDark = Color(0xFF1C1C1E)
val GTRACKBorderDark = Color(0x1FEBEBF5)
val GTRACKRed = Color(0xFFFF453A)
val GTRACKBlue = Color(0xFF0A84FF)
val GTRACKOrange = Color(0xFFFF9F0A)
val GTRACKOnGreenDark = Color(0xFF04110A)

// 可切换 token（组合内读取）
val GTRACKBg: Color @Composable get() = if (LocalGTLight.current) Color(0xFFF2F2F7) else GTRACKBgDark
val GTRACKSurface: Color @Composable get() = if (LocalGTLight.current) Color.White else GTRACKSurfaceDark
val GTRACKLabel: Color @Composable get() = if (LocalGTLight.current) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
val GTRACKLabel2: Color @Composable get() = if (LocalGTLight.current) Color(0xFF6C6C72) else Color(0xFF9E9EA9)
val GTRACKLabel3: Color @Composable get() = if (LocalGTLight.current) Color(0xFFB7B7BC) else Color(0xFF4A4A52)
val GTRACKBorder: Color @Composable get() = if (LocalGTLight.current) Color(0x14000000) else GTRACKBorderDark
val GTRACKOnGreen: Color @Composable get() = if (LocalGTLight.current) Color(0xFF062B15) else GTRACKOnGreenDark

/** 玻璃填充透明度（DESIGN.md §2 glass/fill：深色 .55 / 浅色 .60） */
val GTRACKGlassAlpha: Float @Composable get() = if (LocalGTLight.current) 0.60f else 0.55f

/**
 * 导航 / 推页顶栏填充：直接取背景色本身（继承底色）+ 更高不透明度（.72）。
 * 比原型 rgba(10,10,12,.42) 更实：模糊后的内容只作为质感层，标题不会透视发虚。
 */
val GTRACKBarFill: Color @Composable get() = GTRACKBg.copy(alpha = 0.72f)

/**
 * 原型 --fill：图标按钮 / 输入框底（深色 rgba(120,120,128,.24) / 浅色 rgba(120,120,128,.14)）
 */
val GTRACKFill: Color @Composable get() = if (LocalGTLight.current) Color(0x24787880) else Color(0x3D787880)

/** 分隔线 hairline（DESIGN.md §2 separator：深色 rgba(255,255,255,.08) / 浅色 rgba(0,0,0,.07)） */
val GTRACKSeparator: Color @Composable get() = if (LocalGTLight.current) Color(0x12000000) else Color(0x14FFFFFF)

/** 玻璃卡：半透明面 + 细描边（对齐原型 .glass） */
fun Modifier.gtGlass(corner: Int = 20): Modifier = composed {
    clip(RoundedCornerShape(corner.dp))
        .background(GTRACKSurface.copy(alpha = GTRACKGlassAlpha))
        .border(1.dp, GTRACKBorder, RoundedCornerShape(corner.dp))
}

/**
 * 浮动胶囊（Tab 栏）：**不透明实色**，不继承父容器底色 ——
 * 悬浮层次靠 1px 描边 + 投影（原型 .glass 的 0 8px 28px）建立，不靠透视背景。
 * 因此不需要背景采样（采样只服务于顶栏的半透明玻璃）。
 */
fun Modifier.gtGlassBar(corner: Int = 30): Modifier = composed {
    val shape = RoundedCornerShape(corner.dp)
    val specular = if (LocalGTLight.current) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.16f)
    this
        .shadow(12.dp, shape, clip = false)
        .clip(shape)
        .background(GTRACKSurface)
        // 原型 .glass inset 0 1px 0：顶缘 specular
        .drawBehind { drawLine(specular, Offset(0f, 0.5f), Offset(size.width, 0.5f), strokeWidth = 1f) }
        .border(1.dp, GTRACKBorder, shape)
}

/**
 * 顶栏玻璃（navglass / pushbar）：整宽矩形 + 更实、且继承底色的填充。
 * 填充直接取背景色本身（而非固定灰），模糊后的内容只作为质感层，标题始终可读。
 * 边框只有底部 hairline（调用方用 separator 补），无顶缘高光 —— 原型 .navglass / .pushbar 只有 border-bottom。
 */
fun Modifier.gtGlassTopBar(backdrop: GlassBackdrop?): Modifier = composed {
    gtGlassSurface(
        backdrop = backdrop,
        shape = RectangleShape,
        blur = 24.dp,
        fill = GTRACKBarFill,
        specular = false,
    )
}

/** 环境光底（首页/首启页的磷光绿氛围，替代原型 .ambient） */
@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    Box(
        Modifier
            .background(GTRACKBg)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (LocalGTLight.current) Color(0x2934C759) else Color(0xFF0C2118),
                        Color.Transparent
                    ),
                    radius = 900f,
                )
            )
    ) { content() }
}

/**
 * @param forceLight 强制外观：true=浅色 / false=深色 / null=跟随系统（AUTO）
 */
@Composable
fun GTRACKTheme(forceLight: Boolean? = null, content: @Composable () -> Unit) {
    val light = forceLight ?: !isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalGTLight provides light) {
        MaterialTheme(
            colorScheme = if (!light) darkColorScheme(
                primary = GTRACKGreen,
                background = GTRACKBgDark,
                surface = GTRACKSurfaceDark,
                onPrimary = GTRACKOnGreenDark,
                onBackground = GTRACKLabel,
                onSurface = GTRACKLabel,
                onSurfaceVariant = GTRACKLabel2,
                outline = GTRACKBorder,
                error = GTRACKRed,
            ) else lightColorScheme(
                primary = GTRACKGreenDeep,
                background = Color(0xFFF2F2F7),
                surface = Color.White,
                onBackground = Color(0xFF1C1C1E),
                onSurface = Color(0xFF1C1C1E),
                onSurfaceVariant = Color(0xFF6C6C72),
            ),
            content = content,
        )
    }
}
