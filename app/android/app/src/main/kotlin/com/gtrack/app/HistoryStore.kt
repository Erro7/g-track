package com.gtrack.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 侦查历史：同值去重保最新，仅存本机（对齐 iOS HistoryStore）。 */
object HistoryStore {
    private lateinit var app: Context
    private const val KEY = "history"
    private const val MAX = 200

    fun init(context: Context) { app = context.applicationContext }

    /**
     * 数据版本号：每次写入自增。
     * 订阅处（首页「最近侦查」/ 记录页）读取它即建立依赖，推页返回后无需手动刷新。
     */
    var revision by mutableIntStateOf(0)
        private set

    data class Record(
        val type: String, val value: String, val summary: String,
        val time: Long = System.currentTimeMillis(),
    ) {
        val timeText: String
            get() = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(time))
    }

    private val prefs get() = app.getSharedPreferences("gtrack", Context.MODE_PRIVATE)

    fun hasAgreedLegal(): Boolean = prefs.getBoolean("hasAgreedLegal", false)
    fun setAgreedLegal() = prefs.edit().putBoolean("hasAgreedLegal", true).apply()

    fun all(): List<Record> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Record(o.getString("type"), o.getString("value"), o.getString("summary"), o.getLong("time"))
        }
    }.getOrDefault(emptyList())

    fun recent(n: Int) = all().take(n)

    fun typeOrder() = listOf("ip", "phone", "username")

    fun add(record: Record) {
        val list = all().filterNot { it.type == record.type && it.value == record.value }.toMutableList()
        list.add(0, record)
        val arr = JSONArray()
        list.take(MAX).forEach {
            arr.put(JSONObject().put("type", it.type).put("value", it.value)
                .put("summary", it.summary).put("time", it.time))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
        revision++
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
        revision++
    }
}
