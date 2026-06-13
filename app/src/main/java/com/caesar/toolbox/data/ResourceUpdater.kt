package com.caesar.toolbox.data

import android.content.Context
import android.os.Environment
import com.caesar.toolbox.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * 资源热更新管理器
 * - 检查 GitHub Release 上的资源版本
 * - 下载 zip 资源包
 * - 解压到私有目录
 */
object ResourceUpdater {

    private const val META_URL = "https://highping64x.github.io/CaesarTB/res_meta.json"
    private const val RES_DIR = "hot_res"

    data class ResInfo(val versionCode: Int, val zipUrl: String)

    fun getResDir(ctx: Context) = File(ctx.filesDir, RES_DIR)
    fun getVersion(ctx: Context) = ctx.getSharedPreferences("res_update", Context.MODE_PRIVATE).getInt("res_ver", 0)

    /** 检查并下载更新，返回是否需要重启加载新资源 */
    suspend fun checkAndUpdate(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val meta = fetchMeta() ?: return@withContext false
            val localVer = getVersion(ctx)
            if (meta.versionCode <= localVer) return@withContext false

            val zipFile = File(ctx.cacheDir, "res_${meta.versionCode}.zip")
            if (!downloadZip(meta.zipUrl, zipFile)) return@withContext false

            val resDir = getResDir(ctx)
            resDir.deleteRecursively()
            if (!extractZip(zipFile, resDir)) { zipFile.delete(); return@withContext false }
            zipFile.delete()

            ctx.getSharedPreferences("res_update", Context.MODE_PRIVATE).edit().putInt("res_ver", meta.versionCode).apply()
            true
        } catch (_: Exception) { false }
    }

    private fun fetchMeta(): ResInfo? {
        val conn = URL(META_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000; conn.readTimeout = 8000
        if (conn.responseCode != 200) return null
        val json = conn.inputStream.bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val res = obj.optJSONObject("resource") ?: return null
        return ResInfo(res.getInt("versionCode"), res.getString("url"))
    }

    private fun downloadZip(url: String, dest: File): Boolean {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000; conn.readTimeout = 60000
        if (conn.responseCode != 200) return false
        conn.inputStream.use { input -> FileOutputStream(dest).use { output -> input.copyTo(output) } }
        return true
    }

    private fun extractZip(zipFile: File, destDir: File): Boolean {
        destDir.mkdirs()
        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(destDir, entry.name)
                    if (entry.isDirectory) file.mkdirs()
                    else { file.parentFile?.mkdirs(); FileOutputStream(file).use { zis.copyTo(it) } }
                    entry = zis.nextEntry
                }
            }
            true
        } catch (_: Exception) { false }
    }
}
