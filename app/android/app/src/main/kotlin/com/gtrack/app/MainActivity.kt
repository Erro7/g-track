package com.gtrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * GTRACK 主入口（重写版）：
 * - enableEdgeToEdge + 透明系统栏 → 沉浸式（内容绘制到系统栏下）
 * - 系统栏样式随 App 外观切换（深色默认恒白图标；浅色模式切深图标）——修复日间状态栏发亮
 * - 底部玻璃 Tab 栏：圆角悬浮胶囊、避让手势条
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScanRepository.init(this)
        HistoryStore.init(this)
        CrashLog.install(this)
        // 上次如有崩溃：自动把堆栈复制到剪贴板，方便用户粘贴反馈
        CrashLog.peek(this)?.let { stack ->
            val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("GTRACK crash", stack))
            android.widget.Toast.makeText(
                this, "检测到上次运行异常，日志已复制到剪贴板", android.widget.Toast.LENGTH_LONG
            ).show()
            CrashLog.clear(this)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent { GTRACKRoot() }
    }
}

private data class TabSpec(val route: String, val label: String, val icon: GTIcon)

@Composable
fun GTRACKRoot() {
    val activity = androidx.compose.ui.platform.LocalView.current.context as? ComponentActivity
    var agreedLegal by rememberSaveable { mutableStateOf(HistoryStore.hasAgreedLegal()) }
    // 外观模式：0=AUTO 跟随系统 / 1=深色 / 2=浅色（与设置页分段控件顺序一致）
    var themeMode by rememberSaveable { mutableIntStateOf(0) }
    val isLight = when (themeMode) {
        1 -> false
        2 -> true
        else -> !androidx.compose.foundation.isSystemInDarkTheme()
    }

    // 系统栏样式跟随 App 外观：浅色主题下状态栏切深色图标，不再被亮色系统栏影响
    androidx.compose.runtime.SideEffect {
        val (sb, nb) = if (isLight) {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) to
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) to
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        }
        activity?.enableEdgeToEdge(statusBarStyle = sb, navigationBarStyle = nb)
    }

    GTRACKTheme(forceLight = isLight) {
        if (!agreedLegal) {
            OnboardingScreen(onAgree = { HistoryStore.setAgreedLegal(); agreedLegal = true })
        } else {
            MainTabs(themeMode = themeMode, onThemeModeChange = { themeMode = it })
        }
    }
}

@Composable
private fun MainTabs(themeMode: Int, onThemeModeChange: (Int) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("home") }
    var push by remember { mutableStateOf<ScanRoute?>(null) }
    val tabs = listOf(
        TabSpec("home", "侦查台", GTIcon.ScanFrame),
        TabSpec("history", "记录", GTIcon.Clock),
        TabSpec("settings", "设置", GTIcon.Gear),
    )

    // 滚动信号：子页滚动超过阈值 → 顶栏玻璃浮现 + Tab 收成纯图标
    var scrolled by remember { mutableStateOf(false) }
    val scrollFlag: (Boolean) -> Unit = { scrolled = it }
    androidx.compose.runtime.LaunchedEffect(tab) { scrolled = false }
    val inlineTitle = tabs.firstOrNull { it.route == tab }?.label.orEmpty()

    // 玻璃采样源：内容区录制进离屏层，供顶栏 / Tab 栏做真实背景模糊
    val backdrop = rememberGlassBackdrop()

    Box(Modifier.fillMaxSize()) {
        // 内容区（采样源）：只录内容，玻璃条作为兄弟节点浮在其上，避免自录回环
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                // 顶栏只在 scrolled 时出现 → 未滚动时不采样；且只录顶部 140dp
                .recordGlassBackdrop(backdrop, enabled = scrolled, sampleHeight = 140.dp)
        ) {
            // 内容区占满全高：内容从悬浮胶囊栏底滚过（原型 .scroll padding-bottom 120 + .tabbar absolute）
            // 早期这里给容器留了 96dp 底距，结果底部是一条没有内容/环境光的空白带，
            // 胶囊看起来像"装在底部容器里"而不是浮在内容之上
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
            ) {
                when (tab) {
                    "home" -> HomeScreen(onOpen = { push = it }, onGoHistory = { tab = "history" }, onScrollFlag = scrollFlag)
                    "history" -> HistoryScreen(onOpen = { push = it }, onScrollFlag = scrollFlag)
                    "settings" -> SettingsScreen(themeMode, onThemeModeChange, scrollFlag)
                }
            }
        }

        // 顶栏：滚动后玻璃浮现，居中内联标题（对齐原型 .navglass.show）
        androidx.compose.animation.Crossfade(targetState = scrolled, label = "navglass") { shown ->
            if (shown) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .gtGlassTopBar(backdrop)
                        .padding(WindowInsets.statusBars.asPaddingValues())
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text(inlineTitle, color = GTRACKLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
                    // 原型 .navglass：border-bottom 用 separator（rgba(255,255,255,.08)），不是 glass/border
                    HorizontalDivider(color = GTRACKSeparator, thickness = 0.5.dp)
                }
            }
        }

        // 圆角悬浮玻璃胶囊 Tab 栏：滚动时只收标签 + 上移，胶囊宽度恒定（对齐原型 .scrolled）
        val cfg = LocalConfiguration.current
        // 原型 .tabbtn 宽 92 / .tabbar padding 5 + gap 4：
        // 胶囊宽 = 3×tabWidth + 2×4 + 2×5，需 ≤ 屏宽 − 外层 40×2
        val tabWidth = remember(cfg.screenWidthDp) {
            ((cfg.screenWidthDp.dp - 40.dp * 2 - 18.dp) / 3).coerceIn(64.dp, 92.dp)
        }
        // 收起动画共用一个 spec：tween 而不是 spring —— spring 会回弹，
        // 和"文字消失"叠在一起就是观感上的卡顿
        val tabAnim = tween<Dp>(durationMillis = 220, easing = FastOutSlowInEasing)
        // 原型 .scrolled：bottom 26 → 20（这里表现为整体上移 6dp）
        val barLift by animateDpAsState(if (scrolled) 4.dp else 10.dp, tabAnim, label = "tabbar-lift")
        // Tab 项高度（按原型实测：图标 23 + gap 2 + 标签行 12 + padding 7/5 = 49）
        // 收起后标签 height:0 → 7 + 23 + 5 = 35；胶囊外高 = 内容 + padding 5×2 = 59 / 45
        val itemHeight by animateDpAsState(if (scrolled) 35.dp else 49.dp, tabAnim, label = "tab-item-h")
        // 标签的高度与透明度一起收：只做淡出的话标签仍占 12dp，
        // 淡出结束瞬间移除会让图标从"偏上"跳到居中 —— 就是那一下卡顿
        val labelHeight by animateDpAsState(if (scrolled) 0.dp else 12.dp, tabAnim, label = "tab-label-h")
        val labelAlpha by animateFloatAsState(
            if (scrolled) 0f else 1f,
            tween(220, easing = FastOutSlowInEasing),
            label = "tab-label-a",
        )
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 40.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + barLift)
                .gtGlassBar(30)
                // 原型 .tabbar padding 5（之前写成 10，胶囊内边距偏大、整体偏宽）
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEach { t ->
                val selected = tab == t.route
                Column(
                    Modifier
                        .width(tabWidth)
                        .height(itemHeight)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selected) GTRACKLabel.copy(alpha = 0.14f) else Color.Transparent)
                        .noRippleClickable { tab = t.route },
                    // 内容居中：收起后图标仍在胶囊正中（旧写法 top 8 / bottom 6，收起时偏上）
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 原型 .tabbtn svg：23×23 / stroke 1.8
                    GTLineIcon(t.icon, size = 23.dp, tint = if (selected) GTRACKGreen else GTRACKLabel2, strokeWidth = 1.8f)
                    // 原型 .tl：10/600、字距 .2；gap 由行盒 leading 提供（不再额外加 padding）
                    Text(
                        t.label, fontSize = 10.sp, lineHeight = 12.sp,
                        letterSpacing = 0.2.sp, fontWeight = FontWeight.SemiBold,
                        color = if (selected) GTRACKGreen else GTRACKLabel2,
                        modifier = Modifier.height(labelHeight).clipToBounds().alpha(labelAlpha),
                    )
                }
            }
        }

        // 推页：全屏覆盖、隐藏 Tab 栏（对齐原型 .view.push 行为）
        push?.let { route ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(GTRACKBg)
                    .padding(WindowInsets.statusBars.asPaddingValues())
            ) {
                ScanScreen(route) { push = null }
            }
        }
    }
}
