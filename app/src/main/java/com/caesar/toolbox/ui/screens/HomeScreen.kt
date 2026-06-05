package com.caesar.toolbox.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caesar.toolbox.data.model.ToolCategory
import com.caesar.toolbox.data.model.ToolRegistry
import com.caesar.toolbox.ui.components.ToolCard
import com.caesar.toolbox.ui.navigation.Routes

/**
 * 首页 — 工具概览
 * 顶部问候 + 分类快捷筛选 + 工具列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(ToolCategory.ALL) }
    val tools = remember(selectedCategory) { ToolRegistry.getByCategory(selectedCategory) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CaesarTB",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "个人数字中枢",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 分类筛选栏
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(ToolCategory.entries.toList()) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) },
                            leadingIcon = {
                                Icon(category.icon, contentDescription = null, Modifier.size(16.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // 工具卡片
            items(tools, key = { it.id }) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { onToolClick(Routes.toolDetail(tool.id)) }
                )
            }

            // 底部留白
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
