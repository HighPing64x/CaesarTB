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
 * version.json 格式见项目根目录 version.sample.json
 */
object UpdateChecker {

    // TODO: 替换为你的 GitHub Pages 地址
    private const val VERSION_URL =
        "https://HighPing64x.github.io/CaesarTB/version.json"

    data class Changelog(
        val newFeatures: List<String> = emptyList(),
        val changed: List<String> = emptyList(),
        val removed: List<String> = emptyList(),
        val fixed: List<String> = emptyList()
    )

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersion: String,
        val downloadUrl: String?,
        val updateTime: String = "",
        val changelog: Changelog = Changelog()
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
            val updateTime = obj.optString("updateTime", "")

            // 解析 changelog
            val changelogJson = obj.optJSONObject("changelog")
            val changelog = if (changelogJson != null) {
                Changelog(
                    newFeatures = changelogJson.optJSONArray("new")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    changed = changelogJson.optJSONArray("changed")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    removed = changelogJson.optJSONArray("removed")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList(),
                    fixed = changelogJson.optJSONArray("fixed")?.let { arr ->
                        (0 until arr.length()).map { arr.getString(it) }
                    } ?: emptyList()
                )
            } else Changelog()

            val hasUpdate = remoteVersion.isNotEmpty() &&
                    remoteVersion != BuildConfig.VERSION_NAME

            UpdateInfo(hasUpdate, remoteVersion, downloadUrl.ifEmpty { null }, updateTime, changelog)
        } catch (_: Exception) {
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
