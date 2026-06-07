package com.caesar.toolbox.data

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object CrashHandler : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private var ctx: Context? = null

    fun init(context: Context) {
        if (Thread.getDefaultUncaughtExceptionHandler() is CrashHandler) return
        ctx = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler(this)
        logI("CrashHandler initialized")
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        logE("崩溃: ${ex.message}", ex)
        defaultHandler?.uncaughtException(thread, ex)
    }

    // ---- 写日志 ----

    fun logI(msg: String) = log("INFO", msg, null)
    fun logE(msg: String, ex: Throwable? = null) = log("ERROR", msg, ex)

    private fun log(level: String, msg: String, ex: Throwable?) {
        try {
            val file = logFile() ?: return
            val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            file.appendText("$time [$level] $msg\n")
            ex?.let { val sw = StringWriter(); it.printStackTrace(PrintWriter(sw)); file.appendText(sw.toString() + "\n") }
        } catch (_: Exception) {}
    }

    // ---- 读取日志 ----

    fun readLogs(maxLines: Int = 200): String {
        try {
            val file = logFile() ?: return "无日志"
            return file.readLines().takeLast(maxLines).joinToString("\n").ifEmpty { "无日志记录" }
        } catch (_: Exception) { return "读取失败" }
    }

    fun clearLogs() { try { logFile()?.delete() } catch (_: Exception) {} }

    fun shareLogs(context: Context) {
        try {
            val file = logFile() ?: return
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "分享日志"))
        } catch (_: Exception) {}
    }

    private fun logFile(): File? {
        val dir = ctx?.getExternalFilesDir(null) ?: return null
        return File(dir, "caesar_log.txt")
    }
}
