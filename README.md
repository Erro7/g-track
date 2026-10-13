# GTRACK

> 合法授权下的**公开信息核查**工具：IP 定位 · 号码解析 · 用户名跨平台核查。
> Android（Jetpack Compose）+ iOS（SwiftUI）双端，视觉与交互基准见 [`DESIGN.md`](DESIGN.md) 与可交互原型 [`prototype.html`](prototype.html)。

[![Android CI](https://github.com/<OWNER>/<REPO>/actions/workflows/android.yml/badge.svg)](https://github.com/<OWNER>/<REPO>/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Android-26%2B-blue.svg)](app/android)
[![Platform](https://img.shields.io/badge/iOS-26%2B-lightgrey.svg)](app/ios)

> **把上面徽标里的 `<OWNER>/<REPO>` 替换成你的 GitHub 仓库地址。**

---

## ⚠️ 合规与免责（使用 / 二次分发前必读）

GTRACK 只做**公开信息**的聚合与核对。使用本工具，即表示你同意并接受以下约束（同 App 内「使用声明」全文）：

| 条款 | 内容 |
|---|---|
| **用途边界** | 仅限用于**合法授权**的安全调查、失联亲友协寻、资产核实与公开信息研究。使用者需对自身查询行为承担**全部法律责任**。 |
| **数据来源** | 所有结果均来自公开数据接口与公开网页。本工具**不入侵、不监听、不获取任何非公开数据**。 |
| **数据留存** | 查询记录默认**仅保存在设备本地**，可随时在「设置」中一键清除；本项目不含任何服务端，不回传数据。 |
| **禁止行为** | 严禁用于**跟踪骚扰、人肉搜索、电信诈骗**或其他违法用途。发现滥用请立即停止使用。 |

- 本项目按「现状」（AS IS）提供，作者与贡献者**不对任何滥用行为及由此产生的后果负责**。
- 安全研究 / 合规问题请走 [`SECURITY.md`](SECURITY.md)，不要开公开 issue。

---

## 功能

- **侦查台**：本机 IP 查询（需手动点刷新，不自动联网）、最近侦查、三类侦查入口
- **IP 定位**：归属地 / ASN / 运营商网络 / 时区 / 坐标（公共接口 `ipwho.is`）
- **手机号解析**：国家码表 + 位数校验 + 国际/本地格式化；**不伪造**运营商与归属地
- **用户名跨平台核查**：23 个平台并发探测，仅以**公开个人主页 URL 的 HTTP 状态码**判定是否存在
- **记录**：同值去重保最新，仅存本机，可一键清除
- **外观**：自动（跟随系统）/ 深色 / 浅色
- **视觉**：iOS 26 Liquid Glass 语言；Android 端用离屏采样 + `RenderEffect` 实现**真实背景模糊**（API 31 以下自动加深填充兜底）
- **调试区**（彩蛋：连点版本号 5 次）：内含上游开源致谢；状态**仅本次会话有效**，重启即复位

---

## 目录结构

```
gtrack/
├── DESIGN.md            # 设计规范（色板 / 组件 / 状态 / 图标红线）—— 唯一视觉与交互基准
├── UPSTREAM.md          # 上游技术指标与差异决策（平台清单权威来源）
├── prototype.html       # 可交互原型（浏览器直接打开，无需构建）
├── flow.html            # 流程稿
├── assets/              # 图标源（svg/png）
├── app/
│   ├── android/         # Android 端（Jetpack Compose）    → app/android/README.md
│   └── ios/             # iOS 端（SwiftUI + XcodeGen）     → app/ios/README.md
├── CONTRIBUTING.md      # 贡献指南
├── SECURITY.md          # 安全与合规报告流程
├── CHANGELOG.md         # 版本记录
└── LICENSE              # MIT
```

---

## 快速开始

### Android

要求：**JDK 17** + **Android SDK（compileSdk 35）**。仓库已内置 Gradle Wrapper，无需预装 Gradle。

```bash
cd app/android
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

- 命令行构建无需 `local.properties`（CI 用 `android-actions/setup-android` 自动装 SDK）；Android Studio 打开时会自动生成该文件，已被 `.gitignore` 忽略。
- `minSdk 26` / `targetSdk 35` / Kotlin 2.0.21 / Compose BOM 2024.12.01。

### iOS

要求：**Xcode 26+（iOS 26 SDK）** + [XcodeGen](https://github.com/yonaskolb/XcodeGen)。仓库不提交 `.xcodeproj`（由 `project.yml` 生成）。

```bash
brew install xcodegen
cd app/ios
xcodegen                # 生成 GTRACK.xcodeproj
open GTRACK.xcodeproj   # ⌘R 运行
```

- Bundle ID：`com.gtrack.app`（改名请同步 `project.yml`）
- 签名：Automatic，真机需在 Xcode 选择自己的 Team

### 看设计稿（不用装任何工具）

直接在浏览器打开 `prototype.html` —— 它是可点击的高保真原型（Tab 切换、push 转场、Sheet、滚动后顶栏浮现与 Tab 栏收起等交互都在里面）。

---

## 数据来源与隐私

| 能力 | 数据来源 | 说明 |
|---|---|---|
| IP 定位 | `https://ipwho.is/` 公共接口 | 仅在你**主动点刷新 / 主动查询**时发起请求 |
| 手机号 | 本地国家码表解析 | 完全离线，不联网 |
| 用户名 | 各平台公开主页 URL | 仅取 HTTP 状态码；部分平台反爬（403/999）会记为未命中 |

- 无账号体系、无埋点、无自建服务端。
- 记录写入设备本地存储（Android `SharedPreferences` / iOS `UserDefaults`），设置页可一键清除。
- 崩溃日志：Android 端仅在本地落文件并在下次启动时提示可复制到剪贴板，不自动上传。

---

## 上游致谢

GTRACK 基于开源项目 [**GhostTrack**](https://github.com/HunxByts/GhostTrack)（by HunxByts）二次开发，平台清单与技术基线见 [`UPSTREAM.md`](UPSTREAM.md)。

- 上游署名按设计规范**只出现在调试区与页脚小注**，正式 UI 不出现「GhostTrack」字样（DESIGN.md §10）。
- 本仓库 LICENSE 为 **MIT**；若你二次分发，请一并保留上游的署名与许可声明。

---

## 参与贡献

请看 [`CONTRIBUTING.md`](CONTRIBUTING.md)。要点：

1. 视觉 / 交互改动必须对照 `DESIGN.md` 与 `prototype.html`，有偏差就在 PR 里写明理由（规范差异统一记录在 `DESIGN.md` §10）
2. 提交信息用 Conventional Commits（`feat:` / `fix:` / `docs:` …）
3. PR 需通过 Android CI（`assembleDebug` + `lintDebug` + 单元测试）

---

## 许可证

本项目以 **MIT** 许可开源 —— 见 [`LICENSE`](LICENSE)。

第三方依赖各自遵循其许可证；上游 GhostTrack 的署名与许可见 `UPSTREAM.md`。
