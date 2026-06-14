package com.caesar.toolbox.ui.screens

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

class MediaDownloadService : Service() {

    override fun onBind(i: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val fileName = intent.getStringExtra("name") ?: "download"
        val headers = intent.getStringExtra("headers") ?: ""
        startForeground(startId, NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(fileName)
            .setProgress(100, 0, true).build())
        scope.launch { download(url, fileName, parseHeaders(headers), startId) }
        return START_NOT_STICKY
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private suspend fun download(url: String, name: String, headers: Map<String, String>, id: Int) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 15000; conn.readTimeout = 30000
            conn.instanceFollowRedirects = true
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (!headers.containsKey("Referer")) conn.setRequestProperty("Referer", "https://www.bilibili.com/")
            conn.setRequestProperty("User-Agent", headers["User-Agent"] ?: "Mozilla/5.0 (Linux; Android 14)")

            val total = conn.contentLengthLong
            val input = conn.inputStream
            val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!! else Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val safeName = name.replace("[^a-zA-Z0-9_.\\-]".toRegex(), "_").take(80)
            val file = File(dir, safeName)
            val output = FileOutputStream(file)
            val buffer = ByteArray(8192); var downloaded = 0L; var read: Int
            val mNotify = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            while (input.read(buffer).also { read = it } > 0) {
                output.write(buffer, 0, read); downloaded += read
                if (total > 0) mNotify.notify(id, NotificationCompat.Builder(this, CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(name)
                    .setContentText("${formatBytes(downloaded)} / ${formatBytes(total)}")
                    .setProgress(100, (downloaded * 100 / total).toInt(), false).build())
            }
            output.close(); input.close(); conn.disconnect()
            // 完成通知
            mNotify.notify(id, NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done).setContentTitle("下载完成")
                .setContentText(name).setProgress(0, 0, false).setAutoCancel(true).build())
            withContext(Dispatchers.Main) { Toast.makeText(this@MediaDownloadService, "下载完成: $name", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { Toast.makeText(this@MediaDownloadService, "下载失败: ${e.message}", Toast.LENGTH_LONG).show() }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(id)
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    private fun parseHeaders(raw: String) = raw.split("|").filter { it.contains(":") }.associate {
        val i = it.indexOf(':'); it.substring(0, i).trim() to it.substring(i + 1).trim()
    }

    companion object {
        const val CHANNEL = "media_dl"
        private fun formatBytes(b: Long) = when { b > 1_000_000 -> "${"%.1f".format(b/1_000_000f)}MB"; else -> "${b/1000}KB" }

        fun start(ctx: Context, url: String, name: String, headers: Map<String, String> = emptyMap()) {
            val intent = Intent(ctx, MediaDownloadService::class.java).apply {
                putExtra("url", url); putExtra("name", name)
                putExtra("headers", headers.entries.joinToString("|") { "${it.key}:${it.value}" })
            }
            ctx.startForegroundService(intent)
            Toast.makeText(ctx, "开始下载: $name", Toast.LENGTH_SHORT).show()
        }

        fun createChannel(ctx: Context) {
            (ctx.getSystemService(NotificationManager::class.java)).createNotificationChannel(
                NotificationChannel(CHANNEL, "媒体下载", NotificationManager.IMPORTANCE_LOW))
        }
    }
}
