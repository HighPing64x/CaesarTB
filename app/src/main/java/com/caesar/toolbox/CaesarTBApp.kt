package com.caesar.toolbox

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.caesar.toolbox.data.UpdateChecker
import com.caesar.toolbox.ui.navigation.BottomNavItem
import com.caesar.toolbox.ui.navigation.Routes
import com.caesar.toolbox.ui.screens.*

/**
 * 应用根 Composable — 底部导航 + 路由 + 更新弹窗 + 水印
 */
@Composable
fun CaesarTBApp(
    updateInfo: UpdateChecker.UpdateInfo? = null,
    onDismissUpdate: () -> Unit = {},
    onDownload: (String) -> Unit = {},
    onCheckUpdate: suspend () -> Boolean = { false }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = BottomNavItem.entries.map { it.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200))
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        BottomNavItem.entries.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(BottomNavItem.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(item.icon, contentDescription = item.label)
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(250)) },
                exitTransition = { fadeOut(tween(200)) }
            ) {
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        onToolClick = { route -> navController.navigate(route) }
                    )
                }
                composable(BottomNavItem.Tools.route) {
                    ToolsScreen(
                        onToolClick = { route -> navController.navigate(route) }
                    )
                }
                composable(BottomNavItem.Settings.route) {
                    SettingsScreen(onCheckUpdate = onCheckUpdate)
                }
                composable(
                    route = Routes.TOOL_DETAIL,
                    arguments = listOf(navArgument("toolId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val toolId = backStackEntry.arguments?.getString("toolId") ?: ""
                    when (toolId) {
                        "test_lab" -> TestLabScreen(
                            onBack = { navController.popBackStack() }
                        )
                        "game_2048" -> Game2048Screen(
                            onBack = { navController.popBackStack() }
                        )
                        "crypto" -> CryptoScreen(
                            onBack = { navController.popBackStack() }
                        )
                        "qr" -> QrScreen(
                            onBack = { navController.popBackStack() }
                        )
                        "dns" -> DnsScreen(
                            onBack = { navController.popBackStack() }
                        )
                        "media" -> MediaScreen(
                            onBack = { navController.popBackStack() }
                        )
                        "name_gen" -> NameScreen(
                            onBack = { navController.popBackStack() }
                        )
                        else -> ToolDetailScreen(
                            toolId = toolId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        // --- HighPing 水印（右下角） ---
        Text(
            text = "Powered by HighPing",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 100.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
        )
    }

    // --- 更新弹窗 ---
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = onDismissUpdate,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("发现新版本", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 版本对比
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("当前", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(BuildConfig.VERSION_NAME, fontWeight = FontWeight.Medium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("最新", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(updateInfo.latestVersion,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold)
                    }

                    // 更新时间
                    if (updateInfo.updateTime.isNotEmpty()) {
                        Text("更新时间：${updateInfo.updateTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }

                    // 更新日志
                    val log = updateInfo.changelog
                    if (log.newFeatures.isNotEmpty() || log.changed.isNotEmpty() ||
                        log.removed.isNotEmpty() || log.fixed.isNotEmpty()
                    ) {
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        ChangelogSection("\u2728 新增", log.newFeatures)
                        ChangelogSection("\uD83D\uDD27 修改", log.changed)
                        ChangelogSection("\uD83D\uDDD1 删除", log.removed)
                        ChangelogSection("\uD83D\uDC1B 修复", log.fixed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    updateInfo.downloadUrl?.let { onDownload(it) }
                }) {
                    Text("立即更新", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpdate) {
                    Text("暂不更新")
                }
            }
        )
    }
}

// ========== 更新日志条目 ==========

@Composable
private fun ChangelogSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Column(modifier = Modifier.padding(top = 2.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface)
        items.forEach { item ->
            Text(
                text = "  • $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
