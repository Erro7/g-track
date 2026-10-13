# GTRACK iOS（SwiftUI · iOS 26）

GTRACK Android/iOS 双端中的 iOS 端。设计规范见 `../../DESIGN.md`，交互基准见 `../../prototype.html`。

## 构建

```bash
brew install xcodegen          # 一次即可
cd app/ios
xcodegen                       # 生成 GTRACK.xcodeproj
open GTRACK.xcodeproj          # Xcode 26+, iOS 26 SDK, 真机或模拟器 ⌘R
```

- Bundle ID：`com.gtrack.app`（改名后同步 `project.yml`）
- 签名：Automatic，跑真机需在 Xcode 里选自己的 Team

## 结构

```
GTRACK/
├── GTRACKApp.swift        # @main + AppState（合规门控 / 外观）
├── Core/
│   ├── Theme.swift        # 色板 token、gtGlass 材质、字体、触感
│   ├── Models.swift       # ScanRecord / IPInfo / PhoneInfo / Platform
│   ├── ScanService.swift  # IP(ipwho.is) · 号码本地解析 · 用户名并发探测
│   └── HistoryStore.swift # 历史记录（本地 JSON，仅存设备）
├── Views/
│   ├── RootView.swift     # 首启合规页（半屏声明 Sheet）+ 主 Tab
│   ├── HomeView.swift     # 侦查台：本机 IP / 三入口 / 最近记录
│   ├── ScanViews.swift    # IP / 手机号 / 用户名 三屏
│   └── HistorySettings.swift # 记录 + 设置（调试彩蛋：5 连点版本号）
├── Components.swift       # ScanScaffold 扫描页骨架（进度环/错误/CTA）
└── Assets.xcassets        # AppIcon(1024) + AccentColor(#30D158)
```

## 实现注记（对应 DESIGN.md §8）

- 导航：`NavigationStack` + `navigationDestination(for: ScanRecord.self)`（记录回查与侦查台共用一个 path 模型 `NavHub`）
- 玻璃：`glassEffect(.regular.interactive)`（iOS 26），旧系统降级 `ultraThinMaterial + 描边`
- Tab：`tabBarMinimizeBehavior(.onScrollDown)`——滚动收缩为原生能力
- 声明：`presentationDetents([.medium, .large])` 半屏优先
- CTA 合法才启用；错误行内显示并保留输入；历史同值去重保最新
- 上游署名只在「设置 → 调试」（5 连点版本号解锁），正式 UI 不出现 GhostTrack 字样

## 数据边界

- IP 定位走 `ipwho.is` 公共接口（无密钥）；结果字段与原型一一对应
- 手机号为本地解析（国家码表 + 格式化），运营商/时区字段当前数据源暂未提供——不伪造数据
- 用户名探测对 23 个平台并发 HTTP 探测（清单以上游 GhostTrack 为准，见 UPSTREAM.md），仅以公开个人主页 URL 的状态码判定
