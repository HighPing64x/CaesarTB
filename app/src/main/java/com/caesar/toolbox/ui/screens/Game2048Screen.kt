package com.caesar.toolbox.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Game2048Screen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val vm: Game2048ViewModel = viewModel(factory = Game2048ViewModel.Factory(ctx))
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("2048", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // 分数栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScoreBox("分数", state.score, Modifier.weight(1f))
                ScoreBox("最佳", state.bestScore, Modifier.weight(1f))
            }

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { vm.reset() }) {
                    Text("重新开始", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            // 游戏面板（滑动区域）
            BoardView(
                board = state.board,
                justMerged = state.justMerged,
                onSwipe = { vm.swipe(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // 历史最高分
            if (state.history.isNotEmpty()) {
                Text(
                    "历史记录",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.history.sortedDescending().take(10)) { score ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = score.toString(),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 游戏结束覆盖层
        if (state.isGameOver) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("游戏结束", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("得分：${state.score}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.reset() }) { Text("再来一局") }
                    }
                }
            }
        }
    }
}

// ========== 棋盘 ==========

@Composable
private fun BoardView(
    board: List<List<Int>>,
    justMerged: Set<Pair<Int, Int>>,
    onSwipe: (Game2048ViewModel.Direction) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                var totalX = 0f; var totalY = 0f
                detectDragGestures(
                    onDrag = { _, offset ->
                        totalX += offset.x; totalY += offset.y
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(totalX) > kotlin.math.abs(totalY))
                            onSwipe(if (totalX < 0) Game2048ViewModel.Direction.LEFT
                            else Game2048ViewModel.Direction.RIGHT)
                        else
                            onSwipe(if (totalY < 0) Game2048ViewModel.Direction.UP
                            else Game2048ViewModel.Direction.DOWN)
                        totalX = 0f; totalY = 0f
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        BoxWithConstraints(modifier = Modifier.padding(8.dp)) {
            val tileSize = (maxWidth - 8.dp * 3) / 4
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (c in 0 until 4) {
                            TileCell(
                                value = board[r][c],
                                isMerged = (r to c) in justMerged,
                                modifier = Modifier.size(tileSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== 单个格子 ==========

@Composable
private fun TileCell(value: Int, isMerged: Boolean, modifier: Modifier) {
    val bgColor by animateColorAsState(tileColor(value), label = "tileBg")
    val scale by animateFloatAsState(
        targetValue = if (isMerged) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "merge"
    )

    Surface(
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        tonalElevation = if (value > 0) 0.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (value > 0) {
                Text(
                    text = value.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        value >= 1024 -> 18.sp; value >= 128 -> 20.sp; else -> 24.sp
                    },
                    color = if (value <= 4) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ========== 分数框 ==========

@Composable
private fun ScoreBox(label: String, value: Int, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
        }
    }
}

// ========== 格子颜色 ==========

private fun tileColor(value: Int): Color = when (value) {
    0 -> Color.Transparent
    2 -> Color(0xFFE8ECF0)
    4 -> Color(0xFFD5DDE4)
    8 -> Color(0xFFF0B27A)
    16 -> Color(0xFFF39C5C)
    32 -> Color(0xFFF07B4B)
    64 -> Color(0xFFE74C3C)
    128 -> Color(0xFFF1C40F)
    256 -> Color(0xFFF9E74A)
    512 -> Color(0xFFF9D423)
    1024 -> Color(0xFF00C9A7)
    2048 -> Color(0xFF0099FF)
    else -> Color(0xFF5B2C6F)
}
