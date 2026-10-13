# GTRACK · iOS 26 设计规范（DESIGN.md）

> 依据：prototype.html V3（已通过交互回归）。SwiftUI 实现以本文件为准，原型为视觉与交互基准。
> 适用：GTRACK iOS / Android App（基于开源 GhostTrack 二次开发，**MIT 开源**）。

---

## 1. 品牌与定位

| 项 | 值 |
|---|---|
| 产品名 | **GTRACK**（App 内所有界面；「GhostTrack」仅用于致谢上游的调试入口，不进正式 UI） |
| 定位 | 合法授权下的公开信息核查工具（IP 定位 / 号码解析 / 用户名跨平台核查） |
| 设计语言 | iOS 26 Liquid Glass，深色默认，浅色对照 |
| 气质 | 终端血统 × 苹果原生：磷光绿强调色 + 系统色板，克制、专业、冷峻 |
| 版本基线 | App 版本 **1.0.0**；上游基线 v2.2（对齐上游，见 UPSTREAM.md） |

---

## 2. 色板 Token

### 2.1 深色（默认）

| Token | 值 | 语义 |
|---|---|---|
| `bg` | `#000000` | 纯黑画布（环境光层叠其上） |
| `ambient/green` | `rgba(16,120,88,.34)` | 环境光 blob A（玻璃折射背景，blur 60px） |
| `ambient/cyan` | `rgba(20,90,110,.30)` | 环境光 blob B |
| `glass/fill` | `rgba(28,28,30,.55)` | 玻璃卡底色 |
| `glass/border` | `rgba(255,255,255,.12)` | 玻璃描边 |
| `glass/specular` | `rgba(255,255,255,.18)` | 顶缘内高光（inset 0 1px 0） |
| `label` | `#F2F2F7` | 主文本 |
| `label-2` | `rgba(235,235,245,.62)` | 次文本 |
| `label-3` | `rgba(235,235,245,.32)` | 弱文本 / 占位 / chevron |
| `separator` | `rgba(255,255,255,.08)` | 分隔线 / 行间 hairline |
| `accent/green` | `#30D158` | 磷光绿：CTA、命中态、验证态、激活 Tab |
| `accent/blue` | `#0A84FF` | 链接、导航动作、地图直达 |
| `accent/red` | `#FF453A` | 破坏性操作、错误 |
| `accent/orange` | `#FF9F0A` | 号码域图标、警示点缀 |
| `fill` | `rgba(120,120,128,.24)` | 输入框底、grabber、分段控件轨 |

### 2.2 浅色

| Token | 值 |
|---|---|
| `bg` | `#F2F2F7` |
| `ambient/green` | `rgba(52,199,89,.16)` |
| `ambient/cyan` | `rgba(90,170,200,.16)` |
| `glass/fill` | `rgba(255,255,255,.60)`（导航栏 55%） |
| `glass/border` | `rgba(0,0,0,.06)` |
| `label` | `#1C1C1E` |
| `label-2` | `rgba(60,60,67,.62)` |
| `label-3` | `rgba(60,60,67,.30)` |
| `separator` | `rgba(0,0,0,.07)` |
| `accent/green` | `#1FA84B` |
| `accent/blue` | `#007AFF` |
| `accent/red` | `#E0362C` |
| `accent/orange` | `#C27500` |
| `fill` | `rgba(120,120,128,.14)` |

**对比度约束**：正文对底 ≥ 4.5:1；绿/蓝强调色仅用于 ≥14px 加粗或大字号文本时允许 3:1。行内错误一律红字 + 红描边（不只靠颜色，配文案）。

---

## 3. 字体

- 栈：`-apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "PingFang SC", "HarmonyOS Sans SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif`
- 中文回退必须含 PingFang SC（或系统 CJK）；SwiftUI 直接用系统字体 + `.rounded` 仅限品牌数字场景
- 数据值（IP、坐标、E.164、ASN）一律 **等宽**：SwiftUI `.monospaced()`，CSS `ui-monospace, "SF Mono"`

| 级别 | 字号/行高 | 字重 | 用途 |
|---|---|---|---|
| Large Title | 34 / 40 | 700 | Tab 首屏大标题 |
| Title | 22 / 28 | 700 | 推页内嵌标题（导航栏中轴 17/600） |
| Headline | 16 / 22 | 600 | 列表行主标题、结果行 |
| Body | 14 / 20 | 400 | 正文、行副标题 13/18 可用 |
| Subhead | 13 / 18 | 400 | 副标题、组说明 |
| Caption | 11.5 / 16 | 400 | 版本页脚、页内小注 |
| Data Mono | 15–19 | 600–700 | 输入框、结果值、格式行 |

字距：中文不加字距；Latin/数字大标题 `-0.4px`；等宽数据 `tabular-nums`。

---

## 4. 间距 / 圆角 / 栅格

- **间距（4pt 栅格）**：`4 / 8 / 12 / 16 / 20 / 26 / 32`；屏幕左右安全边 16；组间 26，组头到组体 10
- **圆角（同心原则：内外层圆心一致）**：

| Token | 值 | 用途 |
|---|---|---|
| `r-screen` | 55（壳）/ 47（屏） | 仅演示壳；真机由系统管理 |
| `r-card` | 26 | 玻璃卡、分组卡 |
| `r-inner` | 20 | 紧凑组卡（设置页） |
| `r-btn` | 14 | 主按钮 |
| `r-chip` | 999 | 徽标、胶囊、分段控件 |
| `r-icon` | 13–14 | 40–46px 图标容器（含号码域 46px） |

- **列表行 hairline**：左侧缩进 50px（图标右缘），高 1px，`separator` 色；SwiftUI `Divider().padding(.leading, 50)`
- 滚动容器：顶部预留导航高度（70px），底部预留 Tab 高度（120px）

---

## 5. 玻璃材质（Liquid Glass 参数）

| 层 | 参数 |
|---|---|
| 卡片 | `background glass/fill` + `backdrop-filter: blur(20px) saturate(180%)` + 1px `glass/border` + inset 顶缘 `glass/specular` |
| 导航/Tab 栏 | 顶栏：blur 24px，填充**继承背景色** .72，只有底部 hairline、无顶缘高光。**底部胶囊：不透明实色（`surface`，不继承父容器底色）**，靠 1px 边框 + 投影 `0 8px 28px` + 顶缘 specular 建立悬浮层次。滚动后中轴标题淡入（>34px 触发） |
| Sheet | blur 24px，填充 `.72`，仅顶边框，投影 `0 -12px 40px rgba(0,0,0,.5)` |
| Toast | blur 16px，胶囊，`rgba(24,24,26,.78)` |

**SwiftUI 映射（iOS 26）**：`.glassEffect()` / `GlassEffectContainer`；圆角用 `rect(r-card)`；顶缘高光用 `.overlay(alignment: .top)` 的渐变 hairline 模拟 specular。环境光层用两个 `RadialGradient` + `.blur(60)` 固定于 ZStack 底层，**所有玻璃组件共享同一折射背景**。

**Android 映射（Compose，无 backdrop-filter）**：内容容器录制进离屏 `GraphicsLayer`（`recordGlassBackdrop`），玻璃条按自身位置对齐后以 `RenderEffect.BlurEffect` + 饱和度矩阵绘制为背景，再叠填充与顶缘高光 —— 即真实背景模糊，不是半透明近似。API 31 以下不支持 RenderEffect → 填充不透明度自动加深（≈.91）兜底，保证栏上文字可读。玻璃条必须与内容容器是兄弟节点（自录会回环）。

---

## 6. 组件状态规则

| 组件 | 状态矩阵 |
|---|---|
| 主 CTA（开始追踪/扫描） | 默认：绿底 16% 透明度 + 绿字 17/600；**禁用：仅当输入不合法**（占位灰、不可点）；加载中：文案「追踪中…」+ 禁点防重复提交 |
| Ghost CTA | 透明底 + 1px 玻璃描边 + 主文本色 |
| 输入框 | `fill` 底、圆角 14、focus 描边绿 1.5px；错误：红描边 + 下方红字（`role=alert`），**保留输入**，blur 时校验，输入合法即清错 |
| 列表行 | 默认/按压（`fill` 高亮）/图标色块 30px、圆角 8 |
| Tab 栏 | 悬浮玻璃胶囊 bottom 26；激活项白 14% 底胶囊 + 绿色图标文字；滚动收缩（bottom 26→20，标签 opacity 0）；推页时整栏下沉隐藏 |
| 半屏 Sheet | 抓取条 36×5、头部栏、右上「完成」绿胶囊；上滑 340ms；点遮罩关闭 |
| Alert（破坏性确认） | 居中玻璃卡 r-32；「清除」红 600 / 「取消」蓝 |
| 徽标 | 「示例数据」：`fill` 底 label-2 字；「已验证」：绿 16% 底绿字 |
| Toast | 底部 96px 居中，22ms 淡入，2.4s 自动消失，`role=status` |

**无障碍**：所有图标按钮带 `aria-label`（SwiftUI `.accessibilityLabel`）；`:focus-visible` 绿色 2px 外描边；`prefers-reduced-motion` 时全部动画降到 0.01ms（SwiftUI `@Environment(\.accessibilityReduceMotion)` 分支关闭扫描动效与位移转场）。

---

## 7. 动效

| 场景 | 时长 / 缓动 |
|---|---|
| 推页转场 | 320ms `cubic-bezier(.32,.72,0,1)`（SwiftUI 默认 navigation 转场即可） |
| 半屏 Sheet | 340ms 同上曲线 + scrim 淡入 280ms |
| 推页时底层 root | 340ms 后 `visibility:hidden`（返回时先恢复再关闭，防叠透） |
| 扫描动效（IP） | 环形进度 + 三段状态文案轮播，总时长 3.2s，完成即出结果，**无无限转圈** |
| 扫描动效（用户名） | 23 格网格：入场 stagger 60ms/格，逐格 300ms 判定（hit 绿/miss 降 52% 透明度），进度条与计数同步 |
| 通用过渡 | 150–300ms ease；按压高亮 200ms |

---

## 8. 图标（Lucide 线稿 → SF Symbols 映射）

统一线稿：24px 栅格、1.8px 描边、圆头圆角（Lucide family）。SwiftUI 直接用 SF Symbols 等价替换：

| 语义 | 原型（Lucide） | SF Symbols |
|---|---|---|
| IP 定位 | globe + scan brackets | `globe` / `dot.radiowaves.left.and.right` |
| 手机号 | smartphone | `iphone.gen3` / `phone` |
| 用户名扫描 | user + magnifier | `magnifyingglass` + `person.crop.circle` |
| 本机 IP | wifi | `wifi` |
| 记录 | clock | `clock.arrow.circlepath` |
| 设置 | gear | `gearshape` |
| 命中 | check | `checkmark.circle.fill` |
| 未命中 | minus | `minus.circle` |
| 外链 | arrow-up-right box | `arrow.up.right.square` |
| 清除 | trash | `trash` |
| 复制 | copy | `doc.on.doc` |

禁止 emoji 作图标/状态；国家/地区用**文字码徽标**（US/DE），不用旗帜 emoji。

---

## 9. 信息架构与实现注记（SwiftUI / iOS 26）

```
TabView（3 tab，glass 底部栏）
├─ 侦查台 Home（root）        → NavigationLink/push 三个入口
│   ├─ IP 定位（push）        → 输入校验 → 扫描态 → 结果（地图卡/位置组/网络组/动作行）
│   ├─ 手机号追踪（push）     → 区号前缀 + 号码 → 扫描 → 概览卡/归属组/格式组（每行复制）
│   └─ 用户名扫描（push）     → 网格扫描动效 → 汇总卡 + 命中网格（命中可点开链接）
├─ 记录（root）               → 按类型分组历史；空态（说明 + 去侦查台 CTA）；点行回填重查
└─ 设置（root）               → 外观分段切换（实时）/ 数据源 / 数据（清除→Alert 确认）/ 合规
                              → 隐藏调试区：连点版本号 5 次（1.6s 超时重计）解锁，内含致谢上游；**仅本次会话有效，不落盘，重启即复位**
首启 Onboarding（全屏 z100，一次性）→ 半屏 Sheet 查看完整声明（z105）→ 同意并进入
```

- 导航：`NavigationStack` + `.navigationDestination(for:)`（禁用已废弃 NavigationView / NavigationLink(destination:)）
- 状态：`@Observable` 视图模型（禁 ObservableObject 新写法）；返回 `@Environment(\.dismiss)`
- Tab 收缩：`toolbar(.minimize)` 系 API（iOS 26）；Sheet：`.sheet(detents: [.medium])` 对应本稿半屏
- 首启合规：UserDefaults `hasAgreedLegal` 门控，未同意前 Tab 内容不可达

## 10. 数据与合规注记

- 演示数据：IP 用 RFC 5737 段（203.0.113.0/24、198.51.100.0/24），号码用 555-01XX 保留段；所有结果页带「示例」徽标
- 上游署名仅出现在调试区 + 关于页脚小注；正式 UI 不出现「GhostTrack」字样
- **平台清单以上游技术指标为准**：用户名扫描的 23 个平台取自上游 <https://github.com/HunxByts/GhostTrack>（`GhostTR.py::TrackLu()`），上游 24 条中 Snapchat 重复，去重后 23 —— 与 §7「23 格网格」一致；同步记录见 **UPSTREAM.md**
- 平台差异：Android 推页返回仅箭头图标（安卓平台惯例），iOS/原型为「‹ 返回」带文字 —— 已确认保留差异，见 UPSTREAM.md §4
- 使用声明 Sheet：**不设「完成」按钮**（原型 `.sheet .sdone` 有，本 UX 决策移除）。关闭方式 = grabber / 下拉手势 / 点遮罩；标题行只留「使用声明」
- 引导页声明入口：原型 `.fulllegal` = 次要文字 **+ 下划线**，不是朴素正文、也不是主按钮 —— 两端须带下划线
- 上线前：接入真实接口后复核各结果字段与原型字段一一对应（见 §9 各 push 屏字段表）
