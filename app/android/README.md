# GTRACK Android（Jetpack Compose）

GTRACK 双端中的 Android 端，交互与视觉对齐 `../../DESIGN.md` 与 iOS 端实现。

## 构建

```bash
cd app/android
# Android Studio Ladybug+ 打开即可；或命令行：
./gradlew :app:assembleDebug   # 产物 app/build/outputs/apk/debug/app-debug.apk
```

- minSdk 26 / targetSdk 35 / Kotlin 2.0 + Compose BOM
- 首次构建会自动拉依赖，需要网络

## 结构

```
app/src/main/
├── AndroidManifest.xml            # 单 Activity；v31 启动屏主题
├── kotlin/com/gtrack/app/
│   ├── MainActivity.kt            # 深色默认 · 三 Tab · NavHost（与 iOS 端对齐）
│   ├── Theme.kt                   # 色板 token（磷光绿/深底/墨白）
│   ├── ScanRepository.kt          # IP(ipwho.is) · 号码本地解析 · 用户名并发探测
│   ├── HistoryStore.kt            # 历史记录（SharedPreferences，仅存本机）
│   ├── Components.kt              # GlassCard / KVRow / CTA / Onboarding
│   └── Screens.kt                 # 侦查台 + 三侦查屏 + 记录 + 设置
└── res/
    ├── drawable/ic_launcher_foreground.xml   # 雷达幽灵（自适应前景，66dp 安全区）
    ├── drawable/ic_launcher_background.xml   # 径向渐变底
    ├── drawable/ic_launcher_monochrome.xml   # 主题图标（偶奇镂空眼睛）
    └── values-v31/themes.xml                 # Android 12+ SplashScreen
```

## 与 iOS 端的行为对齐

- 首启合规：iOS 端为独立 Onboarding 页 + 半屏声明 Sheet；Android 端用同文案的 OnboardingScreen
- 调试彩蛋：连点版本号 5 次（1.6s 重置）解锁「调试」组，内含致谢上游；状态**仅本次会话有效**（不落盘，重启复位）
- CTA「合法才可点」、错误行内显示保输入、历史同值去重保最新
- 自适应图标不支持虚线：雷达环改为实线弱透明（DESIGN.md 图标红线允许）

## 已知边界

- 手机号为本地解析，运营商/归属地字段当前数据源暂未提供，不伪造数据
- 用户名探测用 HEAD 状态码判定，部分平台反爬（999/403）会记为未命中；平台清单以上游 GhostTrack 为准（23 个，见 UPSTREAM.md）
- Compose 无 `backdrop-filter`：**已用离屏采样 + RenderEffect 做真实背景模糊**（`Glass.kt`）——
  内容录制进离屏 `GraphicsLayer`，玻璃条按形状裁剪绘制；API 31 以下自动加深填充兜底。
  顶栏填充 `GTRACKBarFill`（继承背景色 .72），底部胶囊为不透明实色（不继承底色）+ 1px 描边 + 投影
- 深浅色：设置页三段「自动 / 深色 / 浅色」，默认自动（跟随系统）
