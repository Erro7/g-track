package com.gtrack.app

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ScanRoute(val type: String, val query: String)

@Composable
fun ScanScreen(route: ScanRoute, onBack: () -> Unit) {
    when (route.type) {
        "ip" -> IPTrackScreen(route.query, onBack)
        "phone" -> PhoneTrackScreen(route.query, onBack)
        else -> UsernameScanScreen(route.query, onBack)
    }
}

// ============ 通用扫描页骨架 ============

@Composable
fun ScanChrome(title: String, onBack: () -> Unit, toast: String? = null, content: @Composable ColumnScope.() -> Unit) {
    // 物理返回 = 页内返回（修复：物理返回直接退出 App）
    androidx.activity.compose.BackHandler { onBack() }
    // 玻璃采样源：内容区录制进离屏层，供顶栏做真实背景模糊
    val backdrop = rememberGlassBackdrop()
    Box(Modifier.fillMaxSize()) {
        // 内容区【独立滚动】：顶栏浮于其上，内容从栏底滚过（原型 .view.push .scroll：padding-top 70）
        // 外层容器【不可滚动】——嵌套 verticalScroll 会拿到无限高度约束直接崩溃
        Column(
            Modifier
                .fillMaxSize()
                .recordGlassBackdrop(backdrop, sampleHeight = 140.dp)
                .verticalScroll(rememberScrollState())
                // 顶部留白 = 顶栏高度 + 左右一致的 16 gap
                .padding(horizontal = 16.dp)
                .padding(top = 68.dp, bottom = 30.dp)
        ) {
            content()
        }

        // 顶栏浮层（原型 .pushbar：absolute + backdrop-filter blur(20) + separator hairline）
        Column(Modifier.fillMaxWidth().gtGlassTopBar(backdrop)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).noRippleClickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    GTLineIcon(GTIcon.Back, size = 22.dp, tint = GTRACKBlue, strokeWidth = 2.1f)
                }
                Text(
                    title, color = GTRACKLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(40.dp))
            }
            HorizontalDivider(color = GTRACKSeparator, thickness = 0.5.dp)
        }

        if (toast != null) GlassToast(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp))
    }
}

/** 环形进度（对齐原型 .ring）：轨道 + 绿色弧 + 百分比 */
@Composable
fun ProgressRing(progress: Float, sizeDp: Int = 88) {
    val trackColor = GTRACKLabel2.copy(alpha = 0.18f)
    Box(Modifier.size(sizeDp.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = GTRACKGreen,
                startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("${(progress * 100).toInt()}%", color = GTRACKLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 扫描面板：环 + 轮换状态语 + 线性进度条（对齐原型 .scanpanel） */
@Composable
fun ScanPanel(progress: Float, message: String) {
    Column(
        Modifier.fillMaxWidth().gtGlass().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(progress)
        Spacer(Modifier.height(10.dp))
        Text(message, color = GTRACKLabel2, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp).background(GTRACKLabel2.copy(alpha = 0.14f), RoundedCornerShape(2.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(GTRACKGreen, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun rememberToastHost(): Pair<MutableState<String?>, @Composable (Modifier) -> Unit> {
    val toast = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast.value) { if (toast.value != null) { delay(1500); toast.value = null } }
    return toast to { mod -> toast.value?.let { GlassToast(it, mod) } }
}

// ============ IP 定位 ============

@Composable
fun IPTrackScreen(initial: String, onBack: () -> Unit) {
    var input by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<ScanRepository.IPInfo?>(null) }
    var reserved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val cm = LocalClipboardManager.current
    val ctx = LocalContext.current
    val (toast, toastHost) = rememberToastHost()

    fun sanitize(s: String) = s.filter { c -> c.isDigit() || c == '.' }.take(15)

    ScanChrome(title = "IP 定位", onBack = onBack, toast = toast.value) {
        SectionLabel("目标 IP")
        GTRACKInput(input, { input = sanitize(it) }, "1.1.1.1")
        InlineError(error)
        Spacer(Modifier.height(14.dp))
        CTAButton(if (scanning) "追踪中…" else "开始追踪", ScanRepository.isValidIP(input), scanning) {
            error = null; reserved = false; result = null; scanning = true; progress = 0f
            scope.launch {
                // 真实请求与扫描动效并行：动效保底 2.4s（对齐原型节奏）
                val fetch = async {
                    runCatching { ScanRepository.trackIP(input.trim()) }
                        .fold(onSuccess = { it }, onFailure = { e ->
                            when {
                                e.message?.contains("reserved", true) == true -> "reserved"
                                else -> e.message ?: "查询失败"
                            }
                        })
                }
                val phases = listOf("正在建立连接…", "正在解析 ASN…", "正在查询归属地…", "正在匹配时区…")
                var p = 0f; var mi = 0
                while (scanning && p < 0.97f) {
                    p += 0.012f; progress = p
                    if ((p * 100).toInt() % 22 == 0) mi = (mi + 1).coerceAtMost(phases.lastIndex)
                    delay(28)
                }
                val r = fetch.await()
                scanning = false
                when {
                    r == "reserved" -> { reserved = true }
                    r is ScanRepository.IPInfo -> {
                        progress = 1f; result = r
                        HistoryStore.add(HistoryStore.Record("ip", r.ip, "${r.country} · ${r.city}"))
                    }
                    else -> error = r.toString()
                }
            }
        }

        if (scanning) {
            Spacer(Modifier.height(18.dp))
            val msg = when {
                progress < 0.25f -> "正在建立连接…"
                progress < 0.5f -> "正在解析 ASN…"
                progress < 0.75f -> "正在查询归属地…"
                else -> "正在匹配时区…"
            }
            ScanPanel(progress, msg)
        }

        if (reserved) {
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().gtGlass().padding(16.dp)) {
                Text("保留地址", color = GTRACKOrange, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("该地址不路由公网，无归属信息。请输入公网 IP 重试。", color = GTRACKLabel2, fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(12.dp))
                Text("重新输入", color = GTRACKGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.noRippleClickable { input = ""; reserved = false })
            }
        }

        result?.let { r ->
            Spacer(Modifier.height(18.dp))
            // 地图卡（上线版接入 MapKit；坐标已可直接反查）
            Column(
                Modifier.fillMaxWidth().gtGlass().padding(16.dp).noRippleClickable {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:${r.lat},${r.lon}?q=${r.lat},${r.lon}(${"${r.country} ${r.city}"})")))
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(GTRACKGreen.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { MonoText(r.countryCode, size = 13, bold = true, color = GTRACKGreen) }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("${r.country} · ${r.city}", color = GTRACKLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        MonoText("${r.lat}, ${r.lon}", size = 12, color = GTRACKLabel2)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("地图", color = GTRACKBlue, fontSize = 12.sp)
                        GTLineIcon(GTIcon.External, size = 12.dp, tint = GTRACKBlue, strokeWidth = 2.2f)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("位置信息")
            GroupCard {
                KRow("国家", r.country)
                KRow("城市", r.city)
                KRow("大洲", r.continent)
                KRow("邮编", r.postal, mono = true)
                KRow("时区", r.tzId)
                KRow("当地时间", r.tzLocal)
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("网络信息")
            GroupCard {
                KRow("ASN", r.asn, mono = true)
                KRow("ISP", r.isp)
                KRow("ORG", r.org)
                KRow("域名", r.domain, mono = true)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f).height(46.dp).background(GTRACKBlue.copy(alpha = 0.16f), RoundedCornerShape(14.dp)).noRippleClickable {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("geo:${r.lat},${r.lon}"))) }
                }, contentAlignment = Alignment.Center) {
                    // 原型 .actions2：图标 + 文案（外链 / 复制）
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GTLineIcon(GTIcon.External, size = 16.dp, tint = GTRACKBlue, strokeWidth = 1.8f)
                        Spacer(Modifier.width(6.dp))
                        Text("在地图中打开", color = GTRACKBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(Modifier.weight(1f).height(46.dp).background(GTRACKGreen.copy(alpha = 0.14f), RoundedCornerShape(14.dp)).noRippleClickable {
                    cm.setText(AnnotatedString("${r.lat}, ${r.lon}")); toast.value = "已拷贝"
                }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GTLineIcon(GTIcon.Copy, size = 16.dp, tint = GTRACKGreen, strokeWidth = 1.8f)
                        Spacer(Modifier.width(6.dp))
                        Text("复制坐标", color = GTRACKGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * E.164 →（区号, 本地号码）：按最长匹配从区号表里取区号（852 / 886 优先于 86 / 8）。
 * 不带 + 的普通号码按默认区号 86 原样回显。
 */
private fun splitE164(raw: String): Pair<Int, String> {
    val digits = raw.filter(Char::isDigit)
    if (!raw.trimStart().startsWith("+")) return 86 to raw.filter { it.isDigit() || it == ' ' }
    val cc = ScanRepository.regions
        .filter { digits.startsWith(it.cc.toString()) }
        .maxByOrNull { it.cc.toString().length }?.cc ?: 86
    return cc to digits.removePrefix(cc.toString())
}

// ============ 手机号追踪 ============

@Composable
fun PhoneTrackScreen(initial: String, onBack: () -> Unit) {
    // 历史记录存的是 E.164（+8613800138000）：回显必须拆回「区号 + 本地号码」，
    // 否则区号会跟着号码一起进输入框，出现"区号和号码没分开"
    val (initialCC, initialNumber) = remember(initial) { splitE164(initial) }
    var input by remember { mutableStateOf(initialNumber) }
    var cc by remember { mutableIntStateOf(initialCC) }
    var ccMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var result by remember { mutableStateOf<ScanRepository.PhoneInfo?>(null) }
    val scope = rememberCoroutineScope()
    val cm = LocalClipboardManager.current
    val (toast, toastHost) = rememberToastHost()
    val region = ScanRepository.regions.firstOrNull { it.cc == cc }!!

    fun sanitize(s: String) = s.filter { c -> c.isDigit() || c == ' ' }.take(region.max + 2)

    ScanChrome(title = "手机号追踪", onBack = onBack, toast = toast.value) {
        SectionLabel("目标号码")
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 区号选择器（新增：可切换区号）
            Box {
                Row(
                    Modifier
                        .height(56.dp)
                        .background(GTRACKSurface, RoundedCornerShape(16.dp))
                        .border(1.dp, GTRACKLabel2.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp)
                        .noRippleClickable { ccMenu = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonoText("+$cc", size = 15, bold = true)
                    Spacer(Modifier.width(4.dp))
                    GTLineIcon(GTIcon.ChevronDown, size = 13.dp, tint = GTRACKLabel2, strokeWidth = 2.2f)
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = ccMenu,
                    onDismissRequest = { ccMenu = false },
                    containerColor = GTRACKSurface,
                ) {
                    ScanRepository.regions.forEach { rgn ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("+${rgn.cc}  ${rgn.name}", color = GTRACKLabel, fontSize = 14.sp) },
                            onClick = { cc = rgn.cc; ccMenu = false },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            GTRACKInput(input, { input = sanitize(it) }, if (cc == 86) "138 0000 0000" else "415 555 0132", Modifier.weight(1f))
        }
        InlineError(error)
        Spacer(Modifier.height(14.dp))
        CTAButton(if (scanning) "追踪中…" else "开始追踪", input.filter { it.isDigit() }.length >= region.min - 2, scanning) {
            error = null; result = null; scanning = true; progress = 0f
            scope.launch {
                val fetch = async { runCatching { ScanRepository.parsePhone(input, cc) } }
                var p = 0f
                while (scanning && p < 0.95f) { p += 0.02f; progress = p; delay(24) }
                val r = fetch.await()
                scanning = false
                r.fold(
                    onSuccess = { info ->
                        progress = 1f; result = info
                        HistoryStore.add(HistoryStore.Record("phone", info.e164, "${info.region} · ${info.intl}"))
                    },
                    onFailure = { error = it.message ?: "号码解析失败" },
                )
            }
        }

        if (scanning) {
            Spacer(Modifier.height(18.dp))
            val msg = when {
                progress < 0.4f -> "正在解析号码结构…"
                else -> "正在匹配区号规则…"
            }
            ScanPanel(progress, msg)
        }

        result?.let { r ->
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth().gtGlass().padding(16.dp)) {
                // 原型概览卡：46px 文字码徽标（DESIGN.md §8 禁止旗帜 emoji）+ 国际格式 + 状态徽标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .background(GTRACKOrange.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        MonoText("+$cc", size = 14, bold = true, color = GTRACKOrange)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        MonoText(r.intl, size = 19, bold = true)
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Badge(if (r.valid) "号码有效" else "格式异常", if (r.valid) GTRACKGreen else GTRACKOrange)
                            Spacer(Modifier.width(8.dp))
                            Badge(r.region, GTRACKBlue)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("归属信息")
            GroupCard {
                KRow("国家 / 区号", "${r.region} +${r.countryCode}")
                KRow("位数校验", if (r.valid) "通过" else "未通过", mono = true)
                KRow("时区", "当前数据源暂未提供")
                KRow("运营商", "当前数据源暂未提供")
            }
            Spacer(Modifier.height(14.dp))
            SectionLabel("号码格式")
            GroupCard {
                KRow("E.164", r.e164, mono = true, onCopy = { cm.setText(AnnotatedString(r.e164)); toast.value = "已拷贝" })
                KRow("国际格式", r.intl, mono = true, onCopy = { cm.setText(AnnotatedString(r.intl)); toast.value = "已拷贝" })
                KRow("本地格式", r.national, mono = true, onCopy = { cm.setText(AnnotatedString(r.national)); toast.value = "已拷贝" })
            }
        }
    }
}

@Composable
fun Badge(text: String, color: Color) {
    Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

// ============ 用户名扫描 ============

private data class CellState(val found: Boolean? = null, val checking: Boolean = false)

@Composable
fun UsernameScanScreen(initial: String, onBack: () -> Unit) {
    var input by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf<String?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    val states = remember { mutableStateListOf<CellState>().apply { addAll(List(ScanRepository.platforms.size) { CellState() }) } }
    var visibleCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val (toast, toastHost) = rememberToastHost()

    val hits = states.count { it.found == true }

    ScanChrome(title = "用户名扫描", onBack = onBack, toast = toast.value) {
        SectionLabel("目标用户名")
        GTRACKInput(input, { input = it.take(30) }, "username")
        InlineError(error)
        Spacer(Modifier.height(14.dp))
        CTAButton(if (scanning) "扫描中…" else "开始扫描",
            input.matches(Regex("[\\w.\\-]{2,30}")) && !scanning, scanning) {
            val name = input.trim()
            if (!name.matches(Regex("[\\w.\\-]{2,30}"))) {
                error = "用户名仅支持字母、数字、点与下划线（2–30 位）。示例：username"
            } else {
                error = null; done = false; username = name; scanning = true
                states.replaceAll { CellState() }; visibleCount = 0
                scope.launch {
                    // 网格逐个点亮（对齐原型 60ms 交错入场）
                    ScanRepository.platforms.forEachIndexed { i, _ ->
                        delay(60); visibleCount = i + 1
                    }
                    var hitsCount = 0
                    ScanRepository.platforms.forEachIndexed { i, (name2, _) ->
                        states[i] = states[i].copy(checking = true)
                        val found = try { ScanRepository.checkPlatform(name2, name).found } catch (_: Exception) { false }
                        states[i] = CellState(found = found)
                        if (found) hitsCount++
                        delay(120)
                    }
                    scanning = false; done = true
                    HistoryStore.add(HistoryStore.Record("username", name, "$hitsCount 个平台命中"))
                }
            }
        }

        if (scanning || done) {
            Spacer(Modifier.height(16.dp))
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("已扫描 $visibleCount/${ScanRepository.platforms.size} 个平台", color = GTRACKLabel, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        if (scanning) DotSpinner()
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).background(GTRACKLabel2.copy(alpha = 0.14f), RoundedCornerShape(2.dp))) {
                        Box(Modifier.fillMaxWidth(if (states.size == 0) 0f else visibleCount / states.size.toFloat()).height(4.dp).background(GTRACKGreen, RoundedCornerShape(2.dp)))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            // 网格：3 列，逐个入场；命中绿玻璃
            val rows = (states.size + 2) / 3
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(rows) { r ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { c ->
                            val idx = r * 3 + c
                            if (idx < states.size) {
                                val (pname, _) = ScanRepository.platforms[idx]
                                val st = states[idx]
                                val appear = idx < visibleCount
                                PlatformCell(pname, st, appear, Modifier.weight(1f)) {
                                    if (st.found == true) {
                                        runCatching {
                                            ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(ScanRepository.platformUrl(pname, username))))
                                        }
                                    }
                                }
                            } else Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (done) {
            Spacer(Modifier.height(12.dp))
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 原型汇总卡：40px 命中色块 + 对勾图标
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(GTRACKGreen.copy(alpha = 0.16f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            GTLineIcon(GTIcon.Check, size = 20.dp, tint = GTRACKGreen, strokeWidth = 2f)
                        }
                        Spacer(Modifier.width(12.dp))
                        MonoText(username, size = 16, bold = true)
                        Spacer(Modifier.width(6.dp))
                        Text("·", color = GTRACKLabel2)
                        Spacer(Modifier.width(6.dp))
                        Text("$hits 个平台命中", color = GTRACKGreen, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text("点击命中卡片可查看对应平台链接", color = GTRACKLabel2, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PlatformCell(name: String, st: CellState, appear: Boolean, modifier: Modifier = Modifier, onTap: () -> Unit) {
    val bg by animateColorAsState(
        when {
            st.found == true -> GTRACKGreen.copy(alpha = 0.15f)
            else -> GTRACKLabel2.copy(alpha = 0.07f)
        }, label = "cellBg"
    )
    Column(
        modifier
            .background(bg, RoundedCornerShape(12.dp))
            .then(if (st.found == true) Modifier.border(1.dp, GTRACKGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)) else Modifier)
            .then(if (st.found == true) Modifier.noRippleClickable { onTap() } else Modifier)
            .padding(vertical = 12.dp)
            .alpha(if (appear) 1f else 0.25f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name.take(2).uppercase(), color = if (st.found == true) GTRACKGreen else GTRACKLabel2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(name, color = if (st.found == true) GTRACKLabel else GTRACKLabel2, fontSize = 9.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        when {
            st.checking -> DotSpinner(2)
            st.found == true -> GTLineIcon(GTIcon.Check, size = 14.dp, tint = GTRACKGreen, strokeWidth = 2.4f)
            st.found == false -> GTLineIcon(GTIcon.Minus, size = 14.dp, tint = GTRACKLabel3, strokeWidth = 2.4f)
            else -> Box(Modifier.size(4.dp).background(GTRACKLabel3, RoundedCornerShape(999.dp)))
        }
    }
}
