package com.caesar.toolbox.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<MediaResult?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("auto") }
    val platforms = listOf("auto" to "自动", "bili" to "B站", "douyin" to "抖音", "kuaishou" to "快手", "direct" to "直链")
    val scope = rememberCoroutineScope()

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("视频下载", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                platforms.forEach { (k, v) -> FilterChip(selected = platform == k, onClick = { platform = k },
                    label = { Text(v, style = MaterialTheme.typography.labelSmall) }) } }

            OutlinedTextField(value = input, onValueChange = { input = it },
                label = { Text("链接或分享口令") }, modifier = Modifier.fillMaxWidth(), maxLines = 4, enabled = !isWorking)

            Button(onClick = {
                if (input.isNotBlank()) { isWorking = true; error = ""; result = null
                    scope.launch { try { result = resolveMedia(input, platform) } catch (e: Exception) { error = e.message ?: "解析失败" }; isWorking = false }
                }
            }, Modifier.fillMaxWidth(), enabled = !isWorking) { Text(if (isWorking) "解析中…" else "解析") }

            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)

            result?.let { r ->
                if (r.cover.isNotEmpty()) AsyncImage(model = ImageRequest.Builder(ctx).data(r.cover).crossfade(true).build(),
                    contentDescription = null, modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                Text(r.platform + " · " + r.title, fontWeight = FontWeight.Bold)
                if (r.author.isNotEmpty()) Text("作者: " + r.author, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text("下载链接 (${r.urls.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                r.urls.forEach { u ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(u.label, fontWeight = FontWeight.Medium, fontSize = 13.sp); Text(u.info, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { MediaDownloadService.start(ctx, u.url, (r.title + "_" + u.label).replace(" ","_").take(80), mapOf("Referer" to if(platform=="bili") "https://www.bilibili.com/" else "", "User-Agent" to "Mozilla/5.0")) }) { Icon(Icons.Outlined.Download, "下载", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("url", u.url)) }) { Icon(Icons.Outlined.ContentCopy, "复制", Modifier.size(18.dp)) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

data class MediaResult(val platform: String, val title: String, val cover: String, val author: String, val urls: List<MediaUrl>)
data class MediaUrl(val label: String, val url: String, val info: String)

private suspend fun resolveMedia(input: String, p: String): MediaResult = withContext(Dispatchers.IO) {
    val detected = if (p == "auto") when {
        input.contains("bilibili.com")||input.contains("BV")||input.contains("b23.tv") -> "bili"
        input.contains("douyin.com")||input.contains("v.douyin") -> "douyin"
        input.startsWith("http") -> "direct"
        else -> "unknown"
    } else p
    val resolved = when {
        input.contains("b23.tv") -> resolveB23(input)
        input.contains("v.douyin") -> resolveDouyinShort(input)
        else -> input
    }
    when (detected) { "bili" -> resolveBili(resolved); "douyin" -> resolveDouyin(resolved); "direct" -> MediaResult("直链","", "", "", listOf(MediaUrl("下载", input, ""))); else -> throw Exception("无法识别，请手动选择平台") }
}

// b23.tv 短链解析
private fun resolveB23(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.instanceFollowRedirects = false; conn.connectTimeout = 5000; conn.readTimeout = 5000
    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
    val loc = conn.getHeaderField("Location") ?: url
    conn.disconnect()
    return loc
}

// v.douyin.com 短链解析
private fun resolveDouyinShort(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.instanceFollowRedirects = false; conn.connectTimeout = 5000; conn.readTimeout = 5000
    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0) AppleWebKit/537.36")
    val loc = conn.getHeaderField("Location") ?: url
    conn.disconnect()
    return loc
}

// ===== B站 =====
private suspend fun resolveBili(input: String): MediaResult = withContext(Dispatchers.IO) {
    val bvid = Regex("BV[0-9A-Za-z]{10}").find(input)?.value ?: throw Exception("未找到BV号")
    val h = mapOf("Referer" to "https://www.bilibili.com/", "User-Agent" to "Mozilla/5.0")
    val d = JSONObject(httpGet("https://api.bilibili.com/x/web-interface/view?bvid=$bvid", h)).getJSONObject("data")
    val play = JSONObject(httpGet("https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=${d.getLong("cid")}&qn=127&fnval=4048&fourk=1", h))
    val pd = play.getJSONObject("data"); val urls = mutableListOf<MediaUrl>()
    pd.optJSONObject("dash")?.let { dash ->
        dash.optJSONArray("audio")?.let { for (i in 0 until it.length()) { val a = it.getJSONObject(i); val br = a.optInt("bandwidth")/1000
            urls.add(MediaUrl("🎵 音频 "+br+"kbps · "+fmt(a.optLong("size")), a.optString("baseUrl"), "m4a")) } }
        dash.optJSONArray("video")?.let { for (i in 0 until it.length()) { val v = it.getJSONObject(i)
            urls.add(MediaUrl("🎬 "+qn(v.optInt("id"))+" · "+fmt(v.optLong("size")), v.optString("baseUrl"), "m4s")) } }
    }
    if (urls.isEmpty()) pd.optJSONArray("durl")?.let { for (i in 0 until it.length()) { val du = it.getJSONObject(i)
        urls.add(MediaUrl("视频 "+(i+1), du.optString("url"), fmt(du.optLong("size")))) } }
    MediaResult("B站", d.optString("title"), d.optString("pic"), d.getJSONObject("owner").optString("name"), urls)
}

// ===== 抖音 =====
private suspend fun resolveDouyin(input: String): MediaResult = withContext(Dispatchers.IO) {
    val vid = Regex("([0-9]{15,20})").find(input)?.value ?: throw Exception("未识别抖音视频ID")
    val h = mapOf("User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0) AppleWebKit/537.36")
    val json = try { httpGet("https://api.allorigins.win/raw?url=https://www.iesdouyin.com/web/api/v2/aweme/iteminfo/?item_ids=$vid", h) }
    catch (_: Exception) { throw Exception("抖音API请求失败") }
    val data = JSONObject(json).optJSONArray("item_list")?.getJSONObject(0) ?: throw Exception("视频不存在")
    val cover = data.getJSONObject("video").getJSONObject("cover").optJSONArray("url_list")?.getString(0) ?: ""
    val urls = mutableListOf<MediaUrl>()
    data.getJSONObject("video").getJSONObject("play_addr").optJSONArray("url_list")?.let {
        urls.add(MediaUrl("🎬 无水印", it.getString(0).replace("playwm","play"), "")) }
    data.getJSONObject("music").getJSONObject("play_url").optJSONArray("url_list")?.let {
        urls.add(MediaUrl("🎵 原声", it.getString(0), "")) }
    if (urls.isEmpty()) urls.add(MediaUrl("🔗 链接", input, "浏览器打开"))
    MediaResult("抖音", data.optString("desc",""), cover, data.getJSONObject("author").optString("nickname",""), urls)
}

private fun qn(id: Int) = when(id) { 120->"4K";116->"1080P60";112->"1080P+";80->"1080P";74->"720P60";64->"720P";32->"480P";16->"360P";else->"${id}P" }
private fun fmt(b: Long) = when { b>1_000_000->"${"%.1f".format(b/1_000_000f)}MB"; b>1000->"${b/1000}KB"; b>0->"${b}B"; else->"" }
private fun httpGet(url: String, headers: Map<String, String>): String {
    val c = URL(url).openConnection() as HttpURLConnection; c.connectTimeout=10000; c.readTimeout=10000
    headers.forEach{(k,v)->c.setRequestProperty(k,v)}; return c.inputStream.bufferedReader().use{it.readText()}.also{c.disconnect()}
}
