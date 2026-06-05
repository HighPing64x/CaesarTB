package com.caesar.toolbox.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.caesar.toolbox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Pages 更新检查器
 *
 * 预期在 GitHub Pages 部署一个 version.json：
 * {
 *   "versionName": "1.0.1",
 *   "versionCode": 2,
 *   "downloadUrl": "https://github.com/xxx/CaesarTB/releases/latest"
 * }
 */
object UpdateChecker {

    // TODO: 替换为你的 GitHub Pages 地址
    private const val VERSION_URL =
        "https://YOUR_USERNAME.github.io/CaesarTB/version.json"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String?
    )

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL(VERSION_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.requestMethod = "GET"

            if (conn.responseCode != 200) return@withContext UpdateInfo(false, "", null)

            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = JSONObject(json)

            val remoteVersion = obj.optString("versionName", "")
            val downloadUrl = obj.optString("downloadUrl", "")

            val hasUpdate = remoteVersion.isNotEmpty() &&
                    remoteVersion != BuildConfig.VERSION_NAME

            UpdateInfo(hasUpdate, remoteVersion, downloadUrl.ifEmpty { null })
        } catch (_: Exception) {
            // 网络不通/地址错误 — 静默失败
            UpdateInfo(false, "", null)
        }
    }

    fun openDownload(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) { }
    }
}
