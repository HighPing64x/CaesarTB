package com.caesar.toolbox.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 工具分组
 */
enum class ToolCategory(val label: String, val icon: ImageVector) {
    ALL("全部", Icons.Outlined.Apps),
    UTILITY("实用", Icons.Outlined.Build),
    DEV("开发", Icons.Outlined.Code),
    MEDIA("媒体", Icons.Outlined.PlayCircle),
    NETWORK("网络", Icons.Outlined.Language),
    SYSTEM("系统", Icons.Outlined.Settings)
}

/**
 * 单个工具定义 — 后续添加功能只需在此注册
 */
data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val category: ToolCategory,
    val enabled: Boolean = true
)

/**
 * 工具注册表 — 所有工具在此集中管理
 * 添加新工具：在此列表追加 Tool 条目 + 在 Navigation 中注册路由
 */
object ToolRegistry {
    val tools = listOf(
        // --- 示例工具（后续替换为实际功能） ---
        Tool(
            id = "test_lab",
            name = "测试实验室",
            description = "验证 UI 组件、状态管理与交互逻辑",
            icon = Icons.Outlined.Science,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "game_2048",
            name = "2048",
            description = "经典数字合成游戏，滑动合并",
            icon = Icons.Outlined.GridOn,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "placeholder_text",
            name = "文本工具",
            description = "编码转换、格式化等文本处理",
            icon = Icons.Outlined.TextFields,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "placeholder_calc",
            name = "计算器",
            description = "进制转换、哈希计算等",
            icon = Icons.Outlined.Calculate,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "placeholder_json",
            name = "JSON 格式化",
            description = "JSON 校验与美化",
            icon = Icons.Outlined.DataObject,
            category = ToolCategory.DEV
        ),
        Tool(
            id = "placeholder_qr",
            name = "二维码",
            description = "生成与扫描二维码",
            icon = Icons.Outlined.QrCode,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "placeholder_todo",
            name = "待办",
            description = "轻量待办事项",
            icon = Icons.Outlined.Checklist,
            category = ToolCategory.UTILITY,
            enabled = false
        ),
        Tool(
            id = "placeholder_ping",
            name = "Ping 工具",
            description = "网络连通性测试",
            icon = Icons.Outlined.Wifi,
            category = ToolCategory.NETWORK,
            enabled = false
        )
    )

    fun getByCategory(category: ToolCategory): List<Tool> =
        if (category == ToolCategory.ALL) tools
        else tools.filter { it.category == category }
}
