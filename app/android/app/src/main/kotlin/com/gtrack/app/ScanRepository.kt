package com.gtrack.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 扫描引擎（重写版）：**所有网络操作一律在 Dispatchers.IO**，
 * JSON 全部用 opt* 安全取值 —— 主线程网络异常是上一版崩溃的根因。
 */
object ScanRepository {

    fun init(@Suppress("UNUSED_PARAMETER") context: Any) = Unit // 预留：接入密钥后使用

    // ---------- 数据模型 ----------

    data class IPInfo(
        val ip: String, val type: String, val continent: String, val country: String,
        val flag: String, val countryCode: String, val city: String, val postal: String,
        val lat: Double, val lon: Double,
        val asn: String, val isp: String, val org: String, val domain: String,
        val tzId: String, val tzUtc: String, val tzLocal: String,
    )

    data class PhoneInfo(
        val e164: String, val intl: String, val national: String,
        val countryCode: Int, val region: String, val digits: Int,
        val valid: Boolean,
    )

    data class PlatformHit(val name: String, val found: Boolean, val url: String)

    class ScanException(message: String) : Exception(message)

    // ---------- IP 定位 ----------

    private val reserved = listOf("127.", "10.", "192.168.", "172.16.", "169.254.", "0.0")

    fun isValidIP(v: String): Boolean {
        val parts = v.trim().split(".")
        if (parts.size != 4) return false
        return parts.all { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) && (it.toIntOrNull() ?: 256) in 0..255 }
    }

    suspend fun trackIP(raw: String): IPInfo = withContext(Dispatchers.IO) {
        val q = raw.trim()
        if (!isValidIP(q)) throw ScanException("IP 格式不正确，示例：1.1.1.1")
        if (reserved.any { q.startsWith(it) }) throw ScanException("保留地址不路由公网，请输入公网 IP 重试")
        val obj = runCatching { getJson("https://ipwho.is/$q") }
            .getOrElse { throw ScanException("网络不可用，请检查连接后重试") }
        if (!obj.optBoolean("success", false)) throw ScanException(obj.optString("message", "查询失败，请稍后重试"))
        val conn = obj.optJSONObject("connection") ?: JSONObject()
        val tz = obj.optJSONObject("timezone") ?: JSONObject()
        IPInfo(
            ip = obj.optString("ip", q),
            type = obj.optString("type", ""),
            continent = obj.optString("continent", "—"),
            country = obj.optString("country", "—"),
            flag = obj.optString("country_code", "—"),
            countryCode = obj.optString("country_code", "—"),
            city = obj.optString("city", "—"),
            postal = obj.optString("postal", "").ifEmpty { "—" },
            lat = obj.optDouble("latitude", 0.0),
            lon = obj.optDouble("longitude", 0.0),
            asn = "AS" + conn.optInt("asn", 0).let { if (it == 0) "—" else it.toString() },
            isp = conn.optString("isp", "—"),
            org = conn.optString("org", "—"),
            domain = conn.optString("domain", "").ifEmpty { "—" },
            tzId = tz.optString("id", "—"),
            tzUtc = tz.optString("utc", "—"),
            tzLocal = tz.optString("current_time", "—").take(16).replace('T', ' '),
        )
    }

    suspend fun myIP(): String = withContext(Dispatchers.IO) {
        runCatching { getJson("https://ipwho.is/").optString("ip", "获取失败") }.getOrDefault("获取失败")
    }

    // ---------- 手机号（本地解析，不伪造运营商；区号由用户选择） ----------

    data class PhoneRegion(val cc: Int, val name: String, val min: Int, val max: Int)

    val regions = listOf(
        PhoneRegion(86, "中国", 11, 11), PhoneRegion(1, "美国/加拿大", 10, 10),
        PhoneRegion(44, "英国", 10, 10), PhoneRegion(81, "日本", 10, 10),
        PhoneRegion(49, "德国", 10, 11), PhoneRegion(33, "法国", 9, 9),
        PhoneRegion(91, "印度", 10, 10), PhoneRegion(61, "澳大利亚", 9, 9),
        PhoneRegion(55, "巴西", 10, 11), PhoneRegion(7, "俄罗斯", 10, 10),
        PhoneRegion(82, "韩国", 9, 10), PhoneRegion(39, "意大利", 9, 10),
        PhoneRegion(34, "西班牙", 9, 9), PhoneRegion(66, "泰国", 9, 9),
        PhoneRegion(62, "印度尼西亚", 9, 12), PhoneRegion(852, "香港", 8, 8),
        PhoneRegion(886, "中国台湾", 9, 9), PhoneRegion(65, "新加坡", 8, 8),
    )

    fun parsePhone(raw: String, cc: Int): PhoneInfo {
        val reg = regions.firstOrNull { it.cc == cc } ?: throw ScanException("暂不支持该区号")
        val all = raw.filter(Char::isDigit)
        if (all.length < reg.min - 2) throw ScanException("号码不完整，请输入 +$cc 区号下的完整号码")
        // 用户在号码里重复带区号时自动剥离
        val national = if (all.startsWith(cc.toString()) && all.length > reg.min) all.removePrefix(cc.toString()) else all
        val valid = national.length in reg.min..reg.max
        val grouped = national.chunked(3).joinToString(" ")
        return PhoneInfo(
            e164 = "+$cc$national",
            intl = "+$cc $grouped",
            national = grouped,
            countryCode = cc,
            region = reg.name,
            digits = national.length,
            valid = valid,
        )
    }

    // ---------- 用户名（逐平台探测，供 UI 逐格点亮） ----------

    /**
     * 平台清单 —— 以上游技术指标为准：
     * 上游 https://github.com/HunxByts/GhostTrack → GhostTR.py::TrackLu() 的 social_media 列表。
     * 上游 24 条中 Snapchat 重复出现两次，去重后为 23 个平台（与 DESIGN.md §7「23 格网格」一致）。
     * 详见仓库根目录 UPSTREAM.md。
     */
    val platforms = listOf(
        "Facebook" to "https://www.facebook.com/%s",
        "Twitter" to "https://www.twitter.com/%s",
        "Instagram" to "https://www.instagram.com/%s",
        "LinkedIn" to "https://www.linkedin.com/in/%s",
        "GitHub" to "https://www.github.com/%s",
        "Pinterest" to "https://www.pinterest.com/%s",
        "Tumblr" to "https://www.tumblr.com/%s",
        "Youtube" to "https://www.youtube.com/%s",
        "SoundCloud" to "https://soundcloud.com/%s",
        "Snapchat" to "https://www.snapchat.com/add/%s",
        "TikTok" to "https://www.tiktok.com/@%s",
        "Behance" to "https://www.behance.net/%s",
        "Medium" to "https://www.medium.com/@%s",
        "Quora" to "https://www.quora.com/profile/%s",
        "Flickr" to "https://www.flickr.com/people/%s",
        "Periscope" to "https://www.periscope.tv/%s",
        "Twitch" to "https://www.twitch.tv/%s",
        "Dribbble" to "https://www.dribbble.com/%s",
        "StumbleUpon" to "https://www.stumbleupon.com/stumbler/%s",
        "Ello" to "https://www.ello.co/%s",
        "Product Hunt" to "https://www.producthunt.com/@%s",
        "Telegram" to "https://www.telegram.me/%s",
        "We Heart It" to "https://www.weheartit.com/%s",
    )

    fun usernameValid(v: String) = v.length in 2..30 && v.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }

    suspend fun checkPlatform(name: String, username: String): PlatformHit = withContext(Dispatchers.IO) {
        val tpl = platforms.first { it.first == name }.second
        val url = String.format(tpl, URLEncoder.encode(username, "UTF-8"))
        PlatformHit(name, headCode(url) in 200..399, url)
    }

    suspend fun scanUsername(username: String): List<PlatformHit> = coroutineScope {
        platforms.map { (name, tpl) ->
            async {
                val url = String.format(tpl, URLEncoder.encode(username, "UTF-8"))
                PlatformHit(name, headCode(url) in 200..399 && headCode(url) != 999, url)
            }
        }.map { it.await() }
    }

    /** 组装平台主页 URL（命中卡片跳转用） */
    fun platformUrl(name: String, username: String): String =
        platforms.firstOrNull { it.first == name }?.let {
            String.format(it.second, URLEncoder.encode(username, "UTF-8"))
        } ?: ""

    // ---------- 底层 ----------

    private fun getJson(url: String): JSONObject =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000
            setRequestProperty("User-Agent", "GTRACK/$GTAppVersion")
        }.let { c ->
            try { c.inputStream.bufferedReader().use { JSONObject(it.readText()) } }
            finally { c.disconnect() }
        }

    private fun headCode(url: String): Int =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"; connectTimeout = 6000; readTimeout = 6000
            setRequestProperty("User-Agent", "Mozilla/5.0 (GTRACK/$GTAppVersion)")
            instanceFollowRedirects = false
        }.let { c ->
            try { c.responseCode } finally { c.disconnect() }
        }
}
