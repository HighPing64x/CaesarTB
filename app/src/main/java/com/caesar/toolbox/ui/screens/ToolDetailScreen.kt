package com.caesar.toolbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.caesar.toolbox.data.model.ToolRegistry

/**
 * 工具详情页 — 占位，后续各工具在此实现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    toolId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tool = ToolRegistry.tools.find { it.id == toolId }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tool?.name ?: "工具") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (tool != null) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "功能开发中…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
