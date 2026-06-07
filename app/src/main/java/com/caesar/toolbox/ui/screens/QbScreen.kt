package com.caesar.toolbox.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QbScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("Q绑查询", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {

            OutlinedTextField(value = input, onValueChange = { input = it.filter { c -> c.isDigit() }.take(11) },
                label = { Text("输入QQ号") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !isWorking)

            Button(onClick = {
                if (input.length in 5..11) { isWorking = true; result = ""
                    scope.launch { result = doQuery(input); isWorking = false }
                }
            }, Modifier.fillMaxWidth(), enabled = input.length in 5..11 && !isWorking) { Text(if (isWorking) "查询中…" else "查询") }

            if (result.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(result, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private suspend fun doQuery(qq: String) = withContext(Dispatchers.IO) {
    val h = mapOf(
        "accept" to "application/json", "origin" to "https://qb.heikebook.com",
        "referer" to "https://qb.heikebook.com/",
        "User-Agent" to "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/149")
    try {
        // 1. 获取 qb_key
        val keyJson = httpGet("https://api.heikebook.com/api/v1/qbapp/get", h)
        val keyData = JSONObject(keyJson)
        if (keyData.optInt("code") != 200) return@withContext "获取认证密钥失败"
        val qbKey = keyData.getJSONObject("data").getString("key")

        // 2. 查询
        val queryJson = httpGet("https://api.heikebook.com/api/v1/sgk/qq/qq?qq=$qq&qb_key=$qbKey", h)
        val data = JSONObject(queryJson)
        if (data.optInt("code") == 200) {
            val info = data.getJSONObject("data")
            "✅ 查询成功\n\nQQ号：${info.optString("qq", qq)}\n绑定手机：${info.optString("phone", "未知")}\n归属地：${info.optString("belonging", "未知")}"
        } else if (data.optInt("code") == 404) {
            "❌ 未查到绑定记录\n\nQQ号：$qq\n${data.optString("msg", "该QQ无绑定数据")}"
        } else if (data.optInt("code") == 401) {
            "⚠️ 请求被拒\n\n${data.optString("msg", "今日查询次数可能已用完")}"
        } else {
            "查询失败：${data.optString("msg", "未知错误")}"
        }
    } catch (e: Exception) { "查询异常：${e.message}" }
}

private fun httpGet(url: String, headers: Map<String, String>): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 10000; conn.readTimeout = 10000
    headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
    return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
}
