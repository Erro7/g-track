# 贡献指南（CONTRIBUTING）

感谢你想让 GTRACK 更好。请先花一分钟看完这份指南 —— 它能省掉来回沟通的几轮 review。

## 0. 前提：合规红线

GTRACK 只做**公开信息**核查。以下改动**不会被接受**：

- 任何绕过认证 / 越权访问 / 爬取非公开数据的实现
- 入侵、监听、中间人、社工库对接
- 暗网 / 泄露数据 / 人肉搜索相关的能力或清单
- 伪造数据（例如编造运营商、归属地、精确坐标）

详见 [`README.md`](README.md) 的「合规与免责」与 [`SECURITY.md`](SECURITY.md)。

## 1. 开发环境

### Android
- JDK 17（Temurin 推荐）、Android SDK Platform 35 + Build Tools
- Gradle 用仓库自带的 Wrapper，**不要**用系统 Gradle：
  ```bash
  cd app/android && ./gradlew :app:assembleDebug
  ```
- 不要提交 `local.properties`（已 gitignore）

### iOS
- Xcode 26+ / iOS 26 SDK / XcodeGen（`brew install xcodegen`）
- `xcodegen` 生成的 `.xcodeproj` 不入库，不要提交

## 2. 提交前必做

```bash
cd app/android
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

三者都必须通过（CI 里跑的是同一组命令）。

## 3. 代码规范

**Kotlin / Compose**
- 官方 Kotlin 代码风格（`kotlin.code.style=official`），4 空格缩进，不用通配符 import
- 颜色、圆角、间距一律走 `Theme.kt` 的 token，**不写死色值**
- 图标只用 `GTIcons.kt` 的线稿图标，**禁止文字符号当图标**（DESIGN.md 图标红线）
- 玻璃材质统一用 `gtGlass*` / `Glass.kt` 系列，不要自己叠半透明背景
- 状态提升：跨屏共享的状态放 store / ViewModel，不要靠 `remember` 跨屏传

**Swift / SwiftUI**
- 遵循 iOS 端现有结构（Core / Views 分层），颜色走 `Theme.swift` token
- 新平台请看 `UPSTREAM.md`：平台清单以**上游**为准

**文案**
- 全端统一；不要出现口语化、内部黑话、未实现功能的承诺（如「完整版支持…」）
- 新增/修改中文文案时，Android 与 iOS **同步改**

## 4. 视觉与交互改动

- 基准是 `DESIGN.md` + `prototype.html`。实现与原型不一致时：
  - 属于**平台惯例差异** → 记在 `DESIGN.md` §10
  - 属于**设计决策变更** → 在 PR 描述里写明原因，并同步更新 `DESIGN.md`
- PR 请附**前后截图 / 录屏**（Android 可用 `assets/` 里的图标做对照），涉及动效的请给视频

## 5. 提交信息与分支

- 分支：`feat/xxx`、`fix/xxx`、`docs/xxx`、`refactor/xxx`
- 提交信息用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/)：
  ```
  fix(android): 修复记录页返回后列表不刷新
  feat(ios): 外观设置新增自动模式
  docs: 补充构建说明
  ```
- 一个 PR 只做一件事；大改动先开 issue 讨论

## 6. PR 检查清单

- [ ] 本地 `assembleDebug` / `lintDebug` / 单测通过
- [ ] 文案、图标、色值都走了 token / 图标库
- [ ] Android 与 iOS 需要同步的改动已同步
- [ ] 视觉偏差已在 `DESIGN.md` 留痕或 PR 说明
- [ ] 附截图 / 录屏
- [ ] 没有引入新的网络请求 / 权限（如引入，说明用途与隐私影响）

## 7. 行为准则

参与本项目即视为同意 [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)。
