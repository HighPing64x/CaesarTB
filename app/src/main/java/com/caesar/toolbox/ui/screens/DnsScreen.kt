package com.caesar.toolbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("域名解析", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(value = input, onValueChange = { input = it },
                label = { Text("输入域名 / IPv4 / IPv6") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                trailingIcon = {
                    if (input.isNotEmpty()) IconButton(onClick = { clipboard.setText(AnnotatedString(input)) }) {
                        Icon(Icons.Outlined.ContentCopy, "复制", Modifier.size(18.dp))
                    }
                })

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (input.isNotBlank() && !isWorking) { isWorking = true
                        scope.launch { result = resolve(input); isWorking = false }
                    }
                }, modifier = Modifier.weight(1f), enabled = !isWorking) {
                    Text(if (isWorking) "解析中…" else "解析")
                }
                OutlinedButton(onClick = { result = ""; input = "" }) { Text("清空") }
            }

            if (result.isNotEmpty()) {
                SelectionContainer {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(text = result, modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(onClick = { clipboard.setText(AnnotatedString(result)) },
                    modifier = Modifier.fillMaxWidth()) { Text("复制结果") }
            }
        }
    }
}

// ========== 解析逻辑 ==========

private suspend fun resolve(input: String): String = withContext(Dispatchers.IO) {
    val target = input.trim()
    val sb = StringBuilder()
    sb.appendLine("══════ 查询目标：$target ══════\n")

    // 1. DNS 解析
    val ips = resolveDns(target)
    val primaryIp = ips.firstOrNull() ?: target
    sb.appendLine("【DNS 解析】")
    if (ips.isEmpty()) sb.appendLine("  无法解析域名")
    else ips.forEach { sb.appendLine("  $it") }

    // 2. 数值地址
    sb.appendLine("\n【数值地址】")
    try {
        val parts = primaryIp.split(".").map { it.toInt() }
        if (parts.size == 4) {
            val num = (parts[0].toLong() shl 24) + (parts[1] shl 16) + (parts[2] shl 8) + parts[3]
            sb.appendLine("  十进制: $num")
            sb.appendLine("  十六进制: 0x${num.toString(16).uppercase()}")
            sb.appendLine("  二进制: ${parts.joinToString(".") { Integer.toBinaryString(it + 256).takeLast(8) }}")
        }
    } catch (_: Exception) {}

    // 3. 多源归属地
    sb.appendLine("\n【归属地查询】")
    sb.append(geoFromIp9(primaryIp))
    sb.append(geoFromIpApiCom(primaryIp))
    sb.append(geoFromIpapiIs(primaryIp))
    sb.append(geoFromIpwhois(primaryIp))
    sb.appendLine("  [ipshudi.com] ${geoFromIpShudi(primaryIp)}")
    sb.appendLine("  [iplark.com] ${geoFromIplark(primaryIp)}")

    // 4. WHOIS（仅域名）
    if (!primaryIp.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
        sb.appendLine("\n【WHOIS 信息】")
        sb.appendLine(whoisLookup(target))
    }

    // 5. Ping
    sb.appendLine("\n【Ping 测试】")
    sb.appendLine(ping(primaryIp))

    sb.appendLine("\n【使用场景】")
    sb.appendLine("  • 网络诊断与排障")
    sb.appendLine("  • 服务器地理位置确认")
    sb.appendLine("  • 域名所有权验证")
    sb.appendLine("  • CDN/云服务节点识别")

    sb.toString()
}

private fun resolveDns(target: String): List<String> {
    val list = mutableListOf<String>()
    try {
        InetAddress.getAllByName(target).forEach { list.add(it.hostAddress ?: "") }
    } catch (_: Exception) {
        try {
            val ip = InetAddress.getByName(target)
            ip.hostAddress?.let { list.add(it) }
        } catch (_: Exception) {}
    }
    return list.filter { it.isNotEmpty() }
}

// ---------- ip9.com.cn JSON API ----------
private fun geoFromIp9(ip: String): String {
    try {
        val json = httpGet("https://ip9.com.cn/get?ip=$ip")
        val obj = JSONObject(json)
        if (obj.optInt("ret") != 200) return ""
        val d = obj.getJSONObject("data")
        return buildString {
            appendLine("  [ip9.com.cn]")
            appendLine("    国家: ${d.optString("country")} (${d.optString("country_code")})")
            appendLine("    省份: ${d.optString("prov")}")
            appendLine("    城市: ${d.optString("city")} ${d.optString("area")}")
            appendLine("    大区: ${d.optString("big_area")}")
            appendLine("    运营商: ${d.optString("isp")}")
            appendLine("    类型: ${d.optString("ip_type")}")
            appendLine("    邮编: ${d.optString("post_code")}  区号: ${d.optString("area_code")}")
            appendLine("    经纬度: ${d.optString("lng")}, ${d.optString("lat")}")
            appendLine("    LongIP: ${d.optLong("long_ip")}")
        }
    } catch (_: Exception) { return "" }
}

// ---------- ipshudi.com 解析 ----------
private fun geoFromIpShudi(ip: String): String {
    try {
        val html = httpGet("https://www.ipshudi.com/$ip.htm")
        val m = Regex("归属地[^<]*<td[^>]*>\\s*<span[^>]*>([^<]+)").find(html)
            ?: Regex("(<td[^>]*>\\s*[^<]*</td>\\s*<td[^>]*>\\s*<span[^>]*>)([^<]+)").find(html)
        return m?.groupValues?.get(2)?.trim() ?: "未查到"
    } catch (_: Exception) { return "查询失败" }
}

// ---------- WHOIS ----------
private fun whoisLookup(domain: String): String {
    try {
        val d = domain.removePrefix("www.")
        // 先查 IANA 获取 whois 服务器
        val iana = whoisRaw(d, "whois.iana.org")
        val ref = Regex("refer:\\s*(\\S+)", RegexOption.IGNORE_CASE).find(iana)
        val server = ref?.groupValues?.get(1) ?: "whois.verisign-grs.com"
        val raw = whoisRaw(d, server)
        val lines = raw.lines().filter { it.contains(":") && !it.startsWith("%") && !it.startsWith("#") }
            .take(15).joinToString("\n  ") { it.trim() }
        return if (lines.isNotBlank()) "  $lines" else "无 WHOIS 数据"
    } catch (_: Exception) { return "查询失败" }
}

private fun whoisRaw(domain: String, server: String): String {
    val s = Socket(server, 43)
    val w = OutputStreamWriter(s.getOutputStream()); w.write("$domain\r\n"); w.flush()
    val r = BufferedReader(InputStreamReader(s.getInputStream()))
    val sb = StringBuilder(); var line: String?
    while (r.readLine().also { line = it } != null) sb.appendLine(line)
    r.close(); w.close(); s.close()
    return sb.toString()
}

// ---------- Ping ----------
private fun ping(host: String): String {
    try {
        val start = System.currentTimeMillis()
        val addr = InetAddress.getByName(host)
        val reachable = addr.isReachable(2000)
        val latency = System.currentTimeMillis() - start
        return if (reachable) "  $host 可达，延迟约 ${latency}ms"
        else "  $host 不可达（超时）"
    } catch (_: Exception) { return "  Ping 失败" }
}

// ---------- iplark.com 解析 ----------
private fun geoFromIplark(ip: String): String {
    try {
        val html = httpGet("https://iplark.com/$ip", mobile = true)
        // 尝试多种正则匹配城市/地区
        val patterns = listOf(
            Regex("""地理位置[：:]\s*([^<\n]+)"""),
            Regex("""<td[^>]*>\s*位置\s*</td>\s*<td[^>]*>([^<]+)"""),
            Regex("""location[^<]*<[^>]+>([^<]+)""", RegexOption.IGNORE_CASE),
            Regex("""([^<]+)\s*</h\d>"""),
            Regex("""归属[^<]*<[^>]+>\s*<[^>]+>([^<]+)""")
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[1].trim().replace("&nbsp;", " ")
        }
        // 兜底：取 title
        val title = Regex("<title>([^<]+)</title>").find(html)
        return title?.groupValues?.get(1)?.trim()?.removeSuffix(" - IP地址信息查询")?.removePrefix("IP地址信息查询") ?: "未查到"
    } catch (_: Exception) { return "查询失败(反爬拦截)" }
}

// ---------- ip-api.com ----------
private fun geoFromIpApiCom(ip: String): String {
    try {
        val json = httpGet("http://ip-api.com/json/$ip?lang=zh-CN")
        val obj = JSONObject(json)
        if (obj.optString("status") != "success") return ""
        return "  [ip-api.com] ${obj.optString("country")} ${obj.optString("regionName")} ${obj.optString("city")} | ${obj.optString("isp")}\n"
    } catch (_: Exception) { return "" }
}

// ---------- api.ipapi.is ----------
private fun geoFromIpapiIs(ip: String): String {
    try {
        val json = httpGet("https://api.ipapi.is/?q=$ip")
        val obj = JSONObject(json)
        val loc = obj.optJSONObject("location") ?: return ""
        return buildString {
            appendLine("  [ipapi.is]")
            appendLine("    国家: ${loc.optString("country")} | 城市: ${loc.optString("city")}")
            appendLine("    运营商: ${obj.optJSONObject("asn")?.optString("org") ?: "N/A"}")
            appendLine("    类型: ${obj.optJSONObject("company")?.optString("type") ?: "N/A"}")
        }
    } catch (_: Exception) { return "" }
}

// ---------- ipwhois.app ----------
private fun geoFromIpwhois(ip: String): String {
    try {
        val json = httpGet("https://ipwhois.app/json/$ip")
        val obj = JSONObject(json)
        if (!obj.optBoolean("success", false)) return ""
        return "  [ipwhois.app] ${obj.optString("country")} ${obj.optString("region")} ${obj.optString("city")} | ${obj.optString("isp")} | ${obj.optString("type")}\n"
    } catch (_: Exception) { return "" }
}

// ---------- HTTP ----------
private fun httpGet(urlStr: String, mobile: Boolean = false): String {
    val url = URL(urlStr)
    val conn = url.openConnection() as HttpURLConnection
    conn.connectTimeout = 8000; conn.readTimeout = 8000
    conn.setRequestProperty("User-Agent",
        if (mobile) "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        else "Mozilla/5.0 (Linux; Android 14)")
    conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
    conn.setRequestProperty("Accept-Encoding", "gzip, deflate")
    conn.setRequestProperty("DNT", "1")
    conn.setRequestProperty("Connection", "keep-alive")
    conn.setRequestProperty("Upgrade-Insecure-Requests", "1")
    if (mobile) {
        conn.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"120\", \"Android WebView\";v=\"120\"")
        conn.setRequestProperty("sec-ch-ua-mobile", "?1")
        conn.setRequestProperty("sec-ch-ua-platform", "\"Android\"")
    }
    return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
}
