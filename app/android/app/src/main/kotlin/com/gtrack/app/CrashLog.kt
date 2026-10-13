package com.gtrack.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃捕获：兜住未捕获异常，把堆栈落到本机文件；
 * 下次启动自动复制到剪贴板并提示用户反馈——远程排错闭环。
 */
object CrashLog {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                context.openFileOutput(FILE, Context.MODE_PRIVATE).use {
                    it.write(
                        buildString {
                            append(SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                            append("  thread=").append(t.name).append('\n')
                            append(android.util.Log.getStackTraceString(e))
                        }.toByteArray()
                    )
                }
            }
            prev?.uncaughtException(t, e)
        }
    }

    fun peek(context: Context): String? =
        runCatching {
            context.getFileStreamPath(FILE)?.takeIf { it.exists() && it.length() > 0 }?.readText()
        }.getOrNull()

    fun clear(context: Context) {
        runCatching { context.getFileStreamPath(FILE)?.delete() }
    }
}
