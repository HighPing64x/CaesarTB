package com.caesar.toolbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.caesar.toolbox.data.model.ToolRegistry
import com.caesar.toolbox.ui.components.ToolCard
import com.caesar.toolbox.ui.navigation.Routes

/**
 * 工具页 — 全量工具列表（按分类分区）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "全部工具",
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ToolRegistry.tools, key = { it.id }) { tool ->
                ToolCard(
                    tool = tool,
                    onClick = { onToolClick(Routes.toolDetail(tool.id)) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
