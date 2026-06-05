package com.caesar.toolbox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.caesar.toolbox.data.model.Tool

/**
 * 工具卡片 — 极简风格
 * 图标 + 名称 + 描述，悬停微动效
 */
@Composable
fun ToolCard(
    tool: Tool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 按压缩放动画
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pressScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (tool.enabled) 1f else 0.4f,
        label = "enabledAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isPressed) 0.dp else 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = tool.enabled,
                    onClick = onClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标容器
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!tool.enabled) {
                Text(
                    text = "即将推出",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
