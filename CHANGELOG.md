# 更新日志（CHANGELOG）

本项目遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式，版本号遵循语义化版本。

## [Unreleased]

### 待办 / 进行中
- 深浅色切换与 iOS 设置页的对齐项
- 记录页单条删除（当前仅支持一键清空）

## [1.0.0] — 2026-10-13

首个开源准备版本。

### Added
- **Android**：Gradle Wrapper（8.11.1），CI 与其他开发者无需预装 Gradle
- **Android**：真实背景模糊 —— 内容录制进离屏 `GraphicsLayer`，顶栏 / 推页顶栏按形状裁剪后用 `RenderEffect.BlurEffect` + 饱和度矩阵绘制；API 31 以下自动加深填充兜底（`Glass.kt`）
- **Android**：外观设置新增「自动 / 深色 / 浅色」三态，默认跟随系统（`GTIcon.Auto`、半填充圆图标）
- **Android**：记录页空态补齐大标题；历史列表多条记录合并进单个玻璃分组卡（行间 hairline）
- **Android**：调试区「开源致谢」改为可点外链行（外链图标 + 打开上游仓库），调试状态改为**会话级、不落盘**
- **合规**：首页「本机 IP」改为**通用示例值 + 手动刷新**，不再进页面就自动请求外部接口
- **文档**：`README.md` / `CONTRIBUTING.md` / `SECURITY.md` / `CODE_OF_CONDUCT.md` / `LICENSE`(MIT)
- **CI**：`.github/workflows/android.yml`（构建 + lint + 单测）与 `release.yml`（tag 出包）

### Changed
- 底部悬浮胶囊全面对齐原型 `.tabbar` / `.tabbtn`：项宽封顶 92、内边距 5、图标 23 / stroke 1.8、标签 10/600 字距 .2、图标与文案间距回到 2dp
- 胶囊收起动画：标签的高度与透明度同步收（tween 220ms），消除「文字消失瞬间图标跳位」的卡顿
- 顶栏填充改为**继承背景色**且不透明度 .72，只保留底部 hairline，去掉顶缘高光
- 顶部玻璃背景只采样顶部 140dp 区域，未滚动时不采样（修复滚动掉帧）
- 图标按钮（本机 IP 刷新 / 使用声明）对齐原型 `.iconbtn`：34dp 圆形 fill 底 + label 色线稿
- 手机号回显：E.164 按最长前缀拆成「区号 + 本地号码」，区号不再重复出现在号码框
- 文案规范化：去掉口语化、内部黑话与未实现承诺；IP / 手机 / 用户名示例统一为 `1.1.1.1` / `138 0000 0000` / `username`
- 使用声明 Sheet 去掉「完成」按钮（grabber + 下拉 / 点遮罩关闭）；引导页声明入口补下划线
- 记录 / 首页最近侦查订阅 `HistoryStore.revision`，推页返回后立即刷新
- `gradle.properties` 移除机器相关的 `org.gradle.java.home`（Linux 绝对路径会让 CI 直接失败）

### Fixed
- 底部 Tab 栏滚动时宽度突变（宽度改为按屏宽恒定）
- 底部胶囊圆角外出现方块（RenderEffect 按矩形边界合成 → 增加画布级形状裁剪 + 离屏合成）
- 本机 IP 刷新图标：弧起点与箭头断开（按原型 `rotate-cw` 重画）
- 记录页返回后列表不刷新；历史列表最后一行滚不到胶囊之上（改为 `contentPadding`）
- 底部胶囊「装在容器里」：内容区去掉 96dp 底部留白，改为内容全高 + 滚动内容内边距 120

[Unreleased]: https://github.com/<OWNER>/<REPO>/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/<OWNER>/<REPO>/releases/tag/v1.0.0
