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
        Tool(
            id = "test_lab",
            name = "测试模块",
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
            id = "crypto",
            name = "加解密",
            description = "Base64 · 凯撒 · 摩斯 · AES · RSA · ECC",
            icon = Icons.Outlined.Lock,
            category = ToolCategory.DEV
        ),
        Tool(
            id = "qr",
            name = "二维码",
            description = "生成二维码并分享",
            icon = Icons.Outlined.QrCode,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "dns",
            name = "域名解析",
            description = "域名/IP 解析 · 多源归属地 · WHOIS · Ping",
            icon = Icons.Outlined.Dns,
            category = ToolCategory.NETWORK
        ),
        Tool(
            id = "media",
            name = "视频下载",
            description = "B站 · 抖音 · 快手 · 直链 · b23",
            icon = Icons.Outlined.Videocam,
            category = ToolCategory.MEDIA
        ),
        Tool(
            id = "name_gen",
            name = "姓名生成",
            description = "中文姓名 · 百家姓 · 复姓 · 随机",
            icon = Icons.Outlined.PersonAdd,
            category = ToolCategory.UTILITY
        ),
        Tool(
            id = "qb",
            name = "Q绑查询",
            description = "QQ绑定手机号查询 · HeikeBook",
            icon = Icons.Outlined.Search,
            category = ToolCategory.UTILITY
        )
    )

    fun getByCategory(category: ToolCategory): List<Tool> =
        if (category == ToolCategory.ALL) tools
        else tools.filter { it.category == category }
}
