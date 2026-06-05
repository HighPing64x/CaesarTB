package com.caesar.toolbox.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航项
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Home("home", "首页", Icons.Outlined.Home),
    Tools("tools", "工具", Icons.Outlined.Widgets),
    Settings("settings", "设置", Icons.Outlined.Settings)
}

/**
 * 工具详情页路由
 */
object Routes {
    const val TOOL_DETAIL = "tool/{toolId}"

    fun toolDetail(toolId: String) = "tool/$toolId"
}
