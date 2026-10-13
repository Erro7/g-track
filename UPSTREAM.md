# 上游技术指标与同步记录（UPSTREAM.md）

> 本文件是 GTRACK 二次开发的**技术指标基线**。凡与上游不一致处，以本文件记录的结论为准；争议项在「差异与决策」中留痕。

---

## 1. 上游基线

| 项 | 值 |
|---|---|
| 上游仓库 | https://github.com/HunxByts/GhostTrack.git |
| 基线分支 | `main` |
| 基线 commit | `a5cb8ad4c08acd803f166fb067b7dac724d6cb3d`（2024-01-11，*Update requirements.txt*） |
| 清单来源文件 | `GhostTR.py` → `TrackLu()` 内的 `social_media` 列表 |
| 记录日期 | 2026-10-10 |

上游署名仅出现在调试区与页脚小注，正式 UI 不出现「GhostTrack」字样（DESIGN.md §10）。

---

## 2. 用户名扫描平台清单（权威 = 上游）

上游 `social_media` 共 **24 条**，其中 **Snapchat 重复出现两次**（`https://www.snapchat.com/add/{}`），去重后为 **23 个平台**。

这解释了原型 / DESIGN.md §7 中的「**23 格网格**」：23 是去重后的有效平台数，不是笔误。

### 23 个平台（顺序同上游，去重后）

| # | 平台 | URL 模板 |
|---|---|---|
| 1 | Facebook | `https://www.facebook.com/{}` |
| 2 | Twitter | `https://www.twitter.com/{}` |
| 3 | Instagram | `https://www.instagram.com/{}` |
| 4 | LinkedIn | `https://www.linkedin.com/in/{}` |
| 5 | GitHub | `https://www.github.com/{}` |
| 6 | Pinterest | `https://www.pinterest.com/{}` |
| 7 | Tumblr | `https://www.tumblr.com/{}` |
| 8 | Youtube | `https://www.youtube.com/{}` |
| 9 | SoundCloud | `https://soundcloud.com/{}` |
| 10 | Snapchat | `https://www.snapchat.com/add/{}` |
| 11 | TikTok | `https://www.tiktok.com/@{}` |
| 12 | Behance | `https://www.behance.net/{}` |
| 13 | Medium | `https://www.medium.com/@{}` |
| 14 | Quora | `https://www.quora.com/profile/{}` |
| 15 | Flickr | `https://www.flickr.com/people/{}` |
| 16 | Periscope | `https://www.periscope.tv/{}` |
| 17 | Twitch | `https://www.twitch.tv/{}` |
| 18 | Dribbble | `https://www.dribbble.com/{}` |
| 19 | StumbleUpon | `https://www.stumbleupon.com/stumbler/{}` |
| 20 | Ello | `https://www.ello.co/{}` |
| 21 | Product Hunt | `https://www.producthunt.com/@{}` |
| 22 | Telegram | `https://www.telegram.me/{}` |
| 23 | We Heart It | `https://www.weheartit.com/{}` |

### 落地位置

| 平台 | 文件 | 符号 |
|---|---|---|
| Android | `app/android/app/src/main/kotlin/com/gtrack/app/ScanRepository.kt` | `ScanRepository.platforms`（`List<Pair<String, String>>`，`%s` 占位） |
| iOS | `app/ios/GTRACK/Core/Models.swift` | `Platforms.all`（`[Platform]`，`%@` 占位） |

两侧均已按上表逐条对齐，数量统一为 23。

### 同步步骤（上游更新时）

1. `git clone https://github.com/HunxByts/GhostTrack.git` → 取 `GhostTR.py::TrackLu()` 的 `social_media`。
2. 去重（按 URL 模板判重），得到 N 个平台。
3. 同步更新 `ScanRepository.platforms` 与 `Platforms.all`，保持顺序与命名一致。
4. 更新本文件：commit、日期、表格、数量 N。
5. UI 中的数量文案已改为动态取清单长度（Android `ScanRepository.platforms.size` / iOS `Platforms.all.count`），无需改文案；若 DESIGN.md §7 的网格数与实际不符，同步修订 DESIGN.md。

---

## 3. 差异与决策（相对上游的实现偏离）

| 项 | 上游 | GTRACK 实现 | 决策理由 |
|---|---|---|---|
| 命中判定 | `GET` + `status_code == 200` | `HEAD` + `2xx/3xx`，且排除 `999` | HEAD 省流量、更快；3xx 多为跳转后的真实主页；`999` 是 LinkedIn 反爬码，记为未命中，避免误报 |
| 重复项 | Snapchat 出现两次 | 去重为 1 条 | 重复项会让网格出现两个同名格，属上游缺陷 |
| URL 归一 | 部分平台写 `www.` | 保留上游原样 | 减少与上游 diff；跳转由服务端处理 |
| 并发 | 串行 `for` 循环 | 并发探测 + 60ms 交错入场 | DESIGN.md §7 动效要求（3.2s 内完成） |

---

## 4. 平台惯例差异（UI，已确认保留）

| 项 | 原型 / iOS | Android | 结论 |
|---|---|---|---|
| 推页返回 | 「‹ 返回」图标 + 文字 | **仅返回箭头图标** | 安卓平台惯例（Material 返回箭头），不跟原型改；已确认保留 |

---

## 5. 其他上游相关注记

- IP 数据源：上游用 `ipwho.is`（`https://ipwho.is/<ip>`），GTRACK 沿用同接口，本机 IP 用 `https://ipwho.is/`。
- 合规：上游为开源工具，无合规门控；GTRACK 增加首启合规页（`hasAgreedLegal` 门控）与使用声明 Sheet，属二次开发新增，非上游行为。
