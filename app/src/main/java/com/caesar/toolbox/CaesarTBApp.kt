package com.caesar.toolbox

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onDownload: (String) -> Unit = {}
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
                    SettingsScreen()
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
                Text(
                    "发现新版本",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("当前版本：${BuildConfig.VERSION_NAME}")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "最新版本：${updateInfo.latestVersion}",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "是否前往下载？",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
