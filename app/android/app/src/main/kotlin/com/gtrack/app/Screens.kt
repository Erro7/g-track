package com.gtrack.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 上游开源项目地址（设置 · 调试 · 开源致谢） */
private const val GTUpstreamURL = "https://github.com/HunxByts/GhostTrack"

/** 本机 IP 的示例值（通用占位）：真实出口 IP 只在用户手动点刷新时才去获取 */
private const val HomeIPExample = "1.1.1.1"

// ================= 首启合规页 =================

@Composable
fun OnboardingScreen(onAgree: () -> Unit) {
    var showLegal by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AmbientBackground {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1.1f))
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ghost_mark),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text("GTRACK", color = GTRACKLabel, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "合法授权下的公开信息核查工具\nIP 定位 · 号码解析 · 用户名跨平台核查",
                    color = GTRACKLabel2, fontSize = 15.sp, lineHeight = 24.sp,
                    modifier = Modifier.padding(bottom = 26.dp),
                )
                GroupCard {
                    legalPoint(GTIcon.ShieldCheck, "仅限合法授权调查", "查询行为由使用者承担全部法律责任")
                    legalPoint(GTIcon.Globe, "只查公开数据源", "公开接口与公开网页，不涉非公开数据")
                    legalPoint(GTIcon.Device, "记录仅存本机", "不上传云端，可随时一键清除")
                }
                Spacer(Modifier.height(26.dp))
                CTAButton("同意并进入", true, false) { onAgree() }
                Spacer(Modifier.height(14.dp))
                // 原型 .fulllegal：次要文字 + 下划线，明示可点（不能是朴素正文）
                Text(
                    "查看完整使用声明", color = GTRACKLabel2, fontSize = 13.sp,
                    style = androidx.compose.ui.text.TextStyle(textDecoration = TextDecoration.Underline),
                    modifier = Modifier.noRippleClickable { showLegal = true },
                )
                Spacer(Modifier.height(10.dp))
                Text("GTRACK · V$GTAppVersion", color = GTRACKLabel3, fontSize = 11.sp)
                Spacer(Modifier.weight(1.4f))
            }
        }
        if (showLegal) LegalSheet { showLegal = false }
    }
}

/** 首启要点行：统一 Lucide 线稿（对齐原型 .onboard .pt .pi，禁止 Material 异形图标混用） */
@Composable
private fun legalPoint(icon: GTIcon, title: String, sub: String) = Column {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        GTLineIcon(icon, size = 20.dp, tint = GTRACKGreen, strokeWidth = 1.9f)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = GTRACKLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = GTRACKLabel2, fontSize = 12.sp)
        }
    }
    // 列表行 hairline 用 separator（DESIGN.md §4），不用 glass/border
    HorizontalDivider(color = GTRACKSeparator, thickness = 0.5.dp)
}

// ================= 使用声明 · 半屏 Sheet =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalSheet(onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = GTRACKSurface,
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), Alignment.Center) {
                Box(Modifier.width(36.dp).height(5.dp).background(GTRACKLabel2.copy(alpha = 0.4f), RoundedCornerShape(3.dp)))
            }
        },
    ) {
        // 无「完成」按钮：grabber + 下拉 / 点遮罩即可关闭（半屏 Sheet 的标准关闭方式）
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("使用声明", color = GTRACKLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = GTRACKSeparator, modifier = Modifier.padding(top = 10.dp))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
            legalBlock("用途边界", "本工具仅限用于合法授权的安全调查、失联亲友协寻、资产核实与公开信息研究。使用者需对自身查询行为承担全部法律责任。")
            legalBlock("数据来源", "所有查询结果均来自公开数据接口与公开网页，本工具不入侵、不监听、不获取任何非公开数据。")
            legalBlock("数据留存", "查询记录默认仅保存在设备本地，可随时在「设置」中一键清除。")
            legalBlock("禁止行为", "严禁用于跟踪骚扰、人肉搜索、电信诈骗或其他违法用途。发现滥用请立即停止使用。")
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun legalBlock(title: String, body: String) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(title, color = GTRACKLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(body, color = GTRACKLabel2, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

// ================= 侦查台 =================

@Composable
fun HomeScreen(onOpen: (ScanRoute) -> Unit, onGoHistory: () -> Unit, onScrollFlag: (Boolean) -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // 本机 IP：默认显示通用示例，**不自动联网**；真实出口 IP 需用户手动点刷新
    // （原型 .myip 就是静态示例 + 「示例」徽标；未请求前不该自发访问外部接口）
    var myIP by rememberSaveable { mutableStateOf(HomeIPExample) }
    var ipIsExample by rememberSaveable { mutableStateOf(true) }
    var showLegal by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    val cm = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    // 订阅 HistoryStore.revision：推页里完成侦查后返回首页，最近列表立即重取
    val recent = remember(HistoryStore.revision) { HistoryStore.recent(3) }
    // 滚动信号上抛：顶栏玻璃浮现 + Tab 收图标
    LaunchedEffect(scrollState) {
        androidx.compose.runtime.snapshotFlow { scrollState.value > 60 }.collect { onScrollFlag(it) }
    }

    LaunchedEffect(toast) { if (toast != null) { delay(1500); toast = null } }

    Box(Modifier.fillMaxSize()) {
        AmbientBackground {
            Column(
                // 底部留白 104 + 页脚 16 = 120（原型 .scroll padding-bottom 120）：
                // 最后一条内容可滚到悬浮胶囊之上，而不是被它压住
                Modifier.fillMaxSize().verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, bottom = 104.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LargeTitle("侦查台")
                    Spacer(Modifier.weight(1f))
                    // 合规声明快捷入口（原型 .iconbtn：圆形 fill 底 + label 色线稿）
                    GIconButton(GTIcon.ShieldCheck, { showLegal = true }, iconSize = 19.dp)
                }
                // 原型 .homesub：大标题下的一句话定位
                Text(
                    "公开信息核查与定位，查询行为仅存于本机。",
                    color = GTRACKLabel2, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.height(6.dp))
                // 原型 .sec-head 的「示例」徽标：未刷新前显示的是示例值，不是真实出口 IP
                SectionLabel("本机 IP", if (ipIsExample) ({ Badge("示例", GTRACKBlue) }) else null)
                GroupCard {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        // 原型 .row .ricon：30px 实心色块 + 白色线稿（DESIGN.md §6 图标色块）
                        Box(
                            Modifier
                                .size(30.dp)
                                .background(GTRACKBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            GTLineIcon(GTIcon.Wifi, size = 17.dp, tint = Color.White, strokeWidth = 1.8f)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            MonoText(myIP, size = 16, bold = true)
                        }
                        var spin by remember { mutableStateOf(false) }
                        val angle by animateFloatAsState(
                            targetValue = if (spin) 360f else 0f,
                            animationSpec = tween(600), label = "refresh",
                        )
                        // 原型 #ipRefresh = .iconbtn：圆形 fill 底 + label 色线稿（此前误用绿色）
                        GIconButton(
                            GTIcon.Refresh,
                            onClick = {
                                spin = true
                                myIP = "查询中…"
                                ipIsExample = false
                                scope.launch {
                                    myIP = runCatching { ScanRepository.myIP() }.getOrDefault("获取失败")
                                    spin = false
                                }
                            },
                            iconModifier = Modifier.graphicsLayer { rotationZ = angle },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel(
                    "最近侦查",
                    if (recent.isEmpty()) null else ({
                        Text("查看全部", color = GTRACKBlue, fontSize = 13.sp,
                            modifier = Modifier.noRippleClickable { onGoHistory() })
                    }),
                )
                if (recent.isEmpty()) {
                    Text(
                        "暂无记录，完成一次侦查后将显示在此", color = GTRACKLabel2,
                        fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }
                // 原型 #recentList：多条记录共用一个 .glass group，行间 hairline
                if (recent.isNotEmpty()) {
                    GroupCard {
                        recent.forEachIndexed { i, rec ->
                            if (i > 0) HorizontalDivider(color = GTRACKSeparator, thickness = 0.5.dp)
                            HistoryRow(rec) { onOpen(ScanRoute(rec.type, rec.value)) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionLabel("侦查入口")
                EntryCard("IP 定位", "查询归属地、ASN 与运营商网络", GTIcon.ScanFrame, GTRACKBlue, "ip", onOpen)
                EntryCard("手机号追踪", "解析号码归属地与运营商", GTIcon.Phone, GTRACKOrange, "phone", onOpen)
                EntryCard("用户名扫描", "一次核查 ${ScanRepository.platforms.size} 个社交平台的同名账号", GTIcon.UserSearch, Color(0xFFBF5AF2), "username", onOpen)
                Spacer(Modifier.height(24.dp))
                Text(
                    "仅供合法授权的安全调查与信息核实", color = GTRACKLabel3,
                    fontSize = 11.sp, modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "GTRACK · v$GTAppVersion · 记录仅存本机", color = GTRACKLabel3,
                    fontSize = 11.sp, modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 3.dp, bottom = 16.dp),
                )
            }
        }
        // 原型 .toast bottom 96：抬到悬浮胶囊之上（内容全高后 28dp 会被胶囊压住）
        toast?.let { GlassToast(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)) }
    }

    if (showLegal) LegalSheet { showLegal = false }
}

@Composable
private fun EntryCard(title: String, sub: String, icon: GTIcon, tint: Color, route: String, onOpen: (ScanRoute) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .gtGlass(20)
            .noRippleClickable { onOpen(ScanRoute(route, "")) }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(tint, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GTLineIcon(icon, size = 22.dp, tint = Color.White, strokeWidth = 1.8f)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = GTRACKLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(sub, color = GTRACKLabel2, fontSize = 13.sp)
            }
            GTLineIcon(GTIcon.ChevronRight, size = 16.dp, tint = GTRACKLabel3, strokeWidth = 2.2f)
        }
    }
}

/** 历史行类型图标：色块 + 线性图标（对齐原型 TYPE_META 配色） */
@Composable
private fun TypeIcon(type: String) {
    val pair: Pair<Color, GTIcon> = when (type) {
        "ip" -> Pair(GTRACKBlue, GTIcon.ScanFrame)
        "phone" -> Pair(GTRACKOrange, GTIcon.Phone)
        else -> Pair(Color(0xFFBF5AF2), GTIcon.UserSearch)
    }
    val tint = pair.first
    val icon = pair.second
    Box(
        Modifier.size(30.dp).background(tint, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        GTLineIcon(icon, size = 17.dp, tint = Color.White, strokeWidth = 1.8f)
    }
}

/**
 * 历史行：对应原型 .row —— **自身不带胶囊**。
 * 多条记录由调用方放进同一个 .glass group（GroupCard），行间用 hairline 分隔；
 * 之前每行一个独立胶囊 + 8dp 间隙，多条时就是一串"各自为政"的小胶囊，与原型不符。
 */
@Composable
fun HistoryRow(rec: HistoryStore.Record, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .noRippleClickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeIcon(rec.type)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            MonoText(rec.value, size = 14, color = GTRACKLabel)
            Text("${typeLabel(rec.type)} · ${rec.timeText}", color = GTRACKLabel2, fontSize = 11.sp)
        }
        GTLineIcon(GTIcon.ChevronRight, size = 14.dp, tint = GTRACKLabel3, strokeWidth = 2.2f)
    }
}

internal fun typeLabel(t: String) = when (t) {
    "ip" -> "IP 定位"; "phone" -> "手机号"; else -> "用户名"
}

// ================= 记录 =================

@Composable
fun HistoryScreen(onOpen: (ScanRoute) -> Unit, onScrollFlag: (Boolean) -> Unit) {
    // 订阅 HistoryStore.revision：推页里新增记录 / 清空后返回，列表立即重取
    val records = remember(HistoryStore.revision) { HistoryStore.all() }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 60
        }.collect { onScrollFlag(it) }
    }

    if (records.isEmpty()) {
        // 空态也要保留大标题（与列表态一致）；空态内容在标题下方的剩余空间居中
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            LargeTitle("记录")
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GTLineIcon(GTIcon.Clock, size = 44.dp, tint = GTRACKLabel3, strokeWidth = 1.6f)
                Spacer(Modifier.height(10.dp))
                Text("暂无侦查记录", color = GTRACKLabel, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "在侦查台完成一次侦查后，记录将保存在此处（仅存储于本机）",
                    color = GTRACKLabel2, fontSize = 13.sp, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text("前往「侦查台」开始", color = GTRACKGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        return
    }

    // 用 contentPadding 而不是 modifier.padding：后者会裁掉列表视口，
    // 导致最后一行滚不到悬浮胶囊之上（列表视口需保持全高）
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
    ) {
        item { LargeTitle("记录"); Spacer(Modifier.height(4.dp)) }
        HistoryStore.typeOrder().forEach { type ->
            val items = records.filter { it.type == type }
            if (items.isNotEmpty()) {
                // 原型 .glass group：同一类型的多条记录合成一个胶囊，行间 hairline（不再每行一个胶囊）
                item {
                    SectionLabel(typeLabel(type))
                    GroupCard {
                        items.forEachIndexed { i, rec ->
                            if (i > 0) HorizontalDivider(color = GTRACKSeparator, thickness = 0.5.dp)
                            HistoryRow(rec) { onOpen(ScanRoute(rec.type, rec.value)) }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ================= 设置 =================

@Composable
fun SettingsScreen(themeMode: Int, onThemeModeChange: (Int) -> Unit, onScrollFlag: (Boolean) -> Unit) {
    val view = LocalView.current
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState) {
        androidx.compose.runtime.snapshotFlow { scrollState.value > 60 }.collect { onScrollFlag(it) }
    }
    var dbgTaps by remember { mutableIntStateOf(0) }
    // 调试模式：会话级（DebugMode），不落盘 —— 重启 App 即复位
    val debugUnlocked = DebugMode.enabled
    var toast by remember { mutableStateOf<String?>(null) }
    var showLegal by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val cm = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(toast) { if (toast != null) { delay(1500); toast = null } }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 104.dp)
        ) {
            LargeTitle("设置")

            SectionLabel("外观")
            GroupCard {
                Column(Modifier.fillMaxWidth().padding(10.dp)) {
                    GTSegmented(
                        options = listOf("自动", "深色", "浅色"),
                        icons = listOf(GTIcon.Auto, GTIcon.Moon, GTIcon.Sun),
                        selected = themeMode,
                        onChange = onThemeModeChange,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("数据源")
            GroupCard {
                KRow("IPWHO.IS 接口", "公共接口", sub = "IP 归属与 ASN 数据")
                KRow("号码解析引擎", "本地规则库", sub = "归属地 / 运营商 / 线路类型")
                KRow("平台探测名单", "覆盖 ${ScanRepository.platforms.size} 个平台", onCopy = {
                    cm.setText(AnnotatedString(ScanRepository.platforms.joinToString("\n") { it.first }))
                    toast = "已拷贝"
                })
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("数据")
            GroupCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .noRippleClickable { confirmClear = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("清除全部记录", color = GTRACKRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("删除本机保存的侦查历史", color = GTRACKLabel2, fontSize = 12.sp)
                    }
                    GTLineIcon(GTIcon.ChevronRight, size = 14.dp, tint = GTRACKRed, strokeWidth = 2.2f)
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel("合规")
            GroupCard {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .noRippleClickable { showLegal = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("使用声明", color = GTRACKLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("合法授权与数据边界", color = GTRACKLabel2, fontSize = 12.sp)
                    }
                    GTLineIcon(GTIcon.ChevronRight, size = 14.dp, tint = GTRACKLabel3, strokeWidth = 2.2f)
                }
            }

            if (debugUnlocked) {
                Spacer(Modifier.height(16.dp))
                SectionLabel("调试")
                GroupCard {
                    // 原型 #debugGroup .row：标题 + 副标题 + 外链图标 + 可点
                    // 之前误用 KRow（键值行：右侧数值 + 行尾分隔线，且不可点）
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .noRippleClickable {
                                runCatching {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(GTUpstreamURL),
                                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }.onFailure {
                                    cm.setText(AnnotatedString(GTUpstreamURL))
                                    toast = "上游项目地址已复制"
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("开源致谢", color = GTRACKLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("GhostTrack · 上游开源项目", color = GTRACKLabel2, fontSize = 12.sp)
                        }
                        GTLineIcon(GTIcon.External, size = 14.dp, tint = GTRACKLabel3, strokeWidth = 2f)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(
                if (debugUnlocked) "GTRACK · 版本 $GTAppVersion · 调试模式" else "GTRACK · 版本 $GTAppVersion",
                color = GTRACKLabel3, fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .noRippleClickable {
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                        dbgTaps++
                        if (!debugUnlocked && dbgTaps >= 5) {
                            DebugMode.enabled = true
                            toast = "调试模式已开启"
                        }
                    },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(90.dp))
        }
        // 原型 .toast bottom 96：抬到悬浮胶囊之上（内容全高后 28dp 会被胶囊压住）
        toast?.let { GlassToast(it, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)) }
    }

    if (showLegal) LegalSheet { showLegal = false }
    if (confirmClear) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = GTRACKSurface,
            title = { Text("清除全部记录？", color = GTRACKLabel, fontSize = 16.sp) },
            text = { Text("本机保存的侦查历史将被删除，不可恢复。", color = GTRACKLabel2, fontSize = 13.sp) },
            confirmButton = {
                Text("清除", color = GTRACKRed, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp).noRippleClickable {
                        HistoryStore.clear(); confirmClear = false; toast = "已清除全部记录"
                    })
            },
            dismissButton = {
                Text("取消", color = GTRACKBlue, fontSize = 15.sp,
                    modifier = Modifier.padding(8.dp).noRippleClickable { confirmClear = false })
            },
        )
    }
}

@Composable
private fun SegButton(selected: Boolean, label: String, onClick: () -> Unit) {
    Text(
        label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = if (selected) GTRACKLabel else GTRACKLabel2,
        modifier = Modifier
            .background(
                if (selected) GTRACKLabel.copy(alpha = 0.14f) else GTRACKLabel2.copy(alpha = 0.08f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .noRippleClickable { onClick() },
    )
}

@Composable
fun UIHaptic() {
    LocalView.current.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
}
