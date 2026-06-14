package com.caesar.toolbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caesar.toolbox.BuildConfig
import com.caesar.toolbox.data.CrashHandler
import kotlinx.coroutines.launch

/**
 * 设置页 — 关于 + 后续扩展
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onCheckUpdate: suspend () -> Boolean = { false }
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var logContent by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // --- 关于区域 ---
            item {
                Text(
                    "关于",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "TB",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("CaesarTB", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "版本 ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                SettingsItem(label = "开发者", hint = "HighPing") { }
            }

            item {
                SettingsItem(
                    label = "检查更新",
                    hint = if (isChecking) "检查中…" else "通过 GitHub Pages"
                ) {
                    if (!isChecking) {
                        isChecking = true
                        scope.launch {
                            val hasUpdate = onCheckUpdate()
                            if (!hasUpdate) snackbarHostState.showSnackbar("已是最新版本")
                            isChecking = false
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // --- 日志 ---
            item {
                Text("调试", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
            item { SettingsItem(label = "查看日志", hint = "最近200行") {
                scope.launch {
                    val logs = CrashHandler.readLogs()
                    showLogDialog = true; logContent = logs
                }
            }}
            item { SettingsItem(label = "分享日志", hint = "发送给开发者") { CrashHandler.shareLogs(ctx) }}
            item { SettingsItem(label = "清除日志", hint = "重新开始记录") { CrashHandler.clearLogs() }}

            item { Spacer(Modifier.height(16.dp)) }

            // --- 偏好 ---
            item {
                Text(
                    "偏好设置",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsItem(label = "主题模式", hint = "跟随系统") {
                    // TODO: 主题切换
                }
            }

            item {
                SettingsItem(label = "数据管理", hint = "缓存与存储") {
                    // TODO: 数据管理
                }
            }
        }
    }

    if (showLogDialog) AlertDialog(onDismissRequest = { showLogDialog = false },
        title = { Text("运行日志") },
        text = { Text(logContent, style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) },
        confirmButton = { TextButton(onClick = { showLogDialog = false }) { Text("关闭") } })
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        content()
    }
}

@Composable
private fun SettingsItem(
    label: String,
    hint: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
