package com.gtrack.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 调试模式开关：**仅本次会话有效，不落盘**。
 *
 * 之前写在 HistoryStore 的 SharedPreferences 里 —— 那是记录存储，而且
 * 一旦解锁就永久保留、App 里没有任何关闭入口。现在改成进程级状态：
 * 重启 App 自动回到未解锁，需重新连点版本号 5 次。
 */
object DebugMode {
    var enabled by mutableStateOf(false)
}
