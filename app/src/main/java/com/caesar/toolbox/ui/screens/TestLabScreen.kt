package com.caesar.toolbox.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 测试实验室 — 全面验证 UI 与逻辑
 *
 * 覆盖：
 * - ViewModel 状态管理
 * - 计数器 + 动画
 * - 文本输入输出
 * - Snackbar 反馈
 * - 主题颜色渲染
 * - 按压交互
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TestLabViewModel = viewModel()
) {
    val counter by viewModel.counter.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val reversedText by viewModel.reversedText.collectAsStateWithLifecycle()
    val useDarkCard by viewModel.useDarkCard.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar 消费
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.consumeToast()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("测试实验室", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← 返回") }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ========== 1. 计数器测试 ==========
            SectionHeader("计数器测试")

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (useDarkCard)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 数字动画
                    AnimatedContent(
                        targetState = counter,
                        transitionSpec = {
                            if (targetState > initialState)
                                slideInVertically { -it } + fadeIn() togetherWith
                                        slideOutVertically { it } + fadeOut()
                            else
                                slideInVertically { it } + fadeIn() togetherWith
                                        slideOutVertically { -it } + fadeOut()
                        },
                        label = "counterAnim"
                    ) { value ->
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CounterButton("−") { viewModel.decrement() }
                        CounterButton("+") { viewModel.increment() }
                    }

                    Spacer(Modifier.height(10.dp))

                    TextButton(onClick = { viewModel.resetCounter() }) {
                        Text("归零")
                    }
                }
            }

            // ========== 2. 卡片样式切换 ==========
            SectionHeader("主题切换测试")

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                onClick = { viewModel.toggleCardStyle() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("卡片样式", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (useDarkCard) "深色" else "浅色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ========== 3. 文本处理测试 ==========
            SectionHeader("文本处理测试")

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (useDarkCard)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.updateInputText(it) },
                        label = { Text("输入文本") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.reverseText() },
                            enabled = inputText.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("反转")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearText() },
                            enabled = inputText.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("清空")
                        }
                    }

                    // 输出结果
                    AnimatedVisibility(
                        visible = reversedText.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Text(
                                text = "反转结果：$reversedText",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ========== 4. 颜色色板测试 ==========
            SectionHeader("主题色彩测试")

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ColorSwatchRow(
                        label = "Primary",
                        color = MaterialTheme.colorScheme.primary
                    )
                    ColorSwatchRow(
                        label = "Surface",
                        color = MaterialTheme.colorScheme.surface
                    )
                    ColorSwatchRow(
                        label = "SurfaceVariant",
                        color = MaterialTheme.colorScheme.surfaceVariant
                    )
                    ColorSwatchRow(
                        label = "Background",
                        color = MaterialTheme.colorScheme.background
                    )
                    ColorSwatchRow(
                        label = "Error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ========== 5. 状态指标 ==========
            SectionHeader("状态摘要")

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusRow("✅ 计数器", "当前值 = $counter")
                    StatusRow("✅ 文本输入", if (inputText.isEmpty()) "空" else "${inputText.length} 字符")
                    StatusRow("✅ 主题切换", if (useDarkCard) "深色模式" else "浅色模式")
                    StatusRow("✅ ViewModel", "运行正常")
                    StatusRow("✅ Snackbar", "反馈通道就绪")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ========== 子组件 ==========

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun CounterButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btnScale"
    )

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(label: String, color: androidx.compose.ui.graphics.Color) {
    val hex = String.format("#%06X", 0xFFFFFF and color.toArgb())

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = hex,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
