package com.caesar.toolbox.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val CELL = 28f; private const val COLS = 20; private const val ROWS = 28
private const val GRID_W = COLS * CELL; private const val GRID_H = ROWS * CELL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("snake", Context.MODE_PRIVATE) }
    var best by remember { mutableStateOf<Int>(prefs.getInt("best", 0)) }

    val initSnake = listOf(COLS/2 to ROWS/2, COLS/2-1 to ROWS/2, COLS/2-2 to ROWS/2)
    var body by remember { mutableStateOf(initSnake) }
    var food by remember { mutableStateOf(5 to 5) }
    var dir by remember { mutableStateOf(1 to 0) }
    var score by remember { mutableStateOf(0) }
    var over by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(180L) }

    fun randomFood(): Pair<Int,Int> {
        val all = (0 until COLS).flatMap { x -> (0 until ROWS).map { y -> x to y } } - body.toSet()
        return if (all.isEmpty()) 0 to 0 else all[Random.nextInt(all.size)]
    }
    fun resetGame() { body = initSnake; food = randomFood(); dir = 1 to 0; score = 0; over = false; speed = 180L }

    var lastDirChange by remember { mutableStateOf(0L) }

    LaunchedEffect(over) {
        if (over) {
            if (score > best) { best = score; prefs.edit().putInt("best", best).apply() }
            return@LaunchedEffect
        }
        while (!over) {
            delay(speed)
            val head = body.first()
            val nh = (head.first + dir.first + COLS) % COLS to (head.second + dir.second + ROWS) % ROWS
            if (nh in body) { over = true; break }
            val ate = nh == food
            body = if (ate) listOf(nh) + body else listOf(nh) + body.dropLast(1)
            if (ate) { score += 10; food = randomFood(); speed = (speed * 0.94).toLong().coerceAtLeast(60) }
        }
        // 循环结束后处理结算并保存成绩记录
        if (over) {
            if (score > best) { best = score; prefs.edit().putInt("best", best).apply() }
            try {
                val entry = "${System.currentTimeMillis()}:\t$score\n"
                ctx.openFileOutput("snake_scores.txt", Context.MODE_APPEND).use { it.write(entry.toByteArray()) }
            } catch (_: Exception) {}
        }
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("贪吃蛇", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("得分: $score", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("🏆 $best", fontSize = 14.sp)
            }
            Box(Modifier.size(GRID_W.dp, GRID_H.dp).padding(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectDragGestures(onDrag = { _, offset ->
                        val now = System.currentTimeMillis()
                        if (now - lastDirChange < 60) return@detectDragGestures
                                val nd = if (kotlin.math.abs(offset.x) > kotlin.math.abs(offset.y))
                                    (if (offset.x > 0) 1 else -1) to 0
                                else 0 to (if (offset.y > 0) 1 else -1)
                                // 忽略与当前方向完全相反的输入
                                if (nd.first == -dir.first && nd.second == -dir.second) return@detectDragGestures
                                // 预判下一步位置，若会撞到自身或在墙对面造成撞击则判定失败并保存成绩
                                val head = body.first()
                                val rawX = head.first + nd.first
                                val rawY = head.second + nd.second
                                val nx = (rawX + COLS) % COLS
                                val ny = (rawY + ROWS) % ROWS
                                val willGrow = (nx to ny) == food
                                val tail = body.last()
                                val collision = (nx to ny) in body && !((nx to ny) == tail && !willGrow)
                                if (collision) {
                                    over = true
                                    // 保存成绩记录
                                    try {
                                        val entry = "${System.currentTimeMillis()}:\t$score\n"
                                        ctx.openFileOutput("snake_scores.txt", Context.MODE_APPEND).use { it.write(entry.toByteArray()) }
                                    } catch (_: Exception) {}
                                    return@detectDragGestures
                                }
                                dir = nd; lastDirChange = now
                    })
                }) {
                Canvas(Modifier.fillMaxSize()) {
                    val ox = (size.width - GRID_W) / 2; val oy = (size.height - GRID_H) / 2
                    for (x in 0..COLS) drawLine(Color.Gray.copy(alpha=0.1f), Offset(ox+x*CELL, oy), Offset(ox+x*CELL, oy+GRID_H))
                    for (y in 0..ROWS) drawLine(Color.Gray.copy(alpha=0.1f), Offset(ox, oy+y*CELL), Offset(ox+GRID_W, oy+y*CELL))
                    drawCircle(Color.Red, CELL/2-2, Offset(ox+food.first*CELL+CELL/2, oy+food.second*CELL+CELL/2))
                    body.forEachIndexed { i, (x, y) ->
                        drawRoundRect(if(i==0) Color(0xFF4CAF50) else Color(0xFF388E3C),
                            Offset(ox+x*CELL+1, oy+y*CELL+1), Size(CELL-2, CELL-2), CornerRadius(4f))
                    }
                }
            }
            if (over) {
                Text("游戏结束！得分 $score", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { resetGame() }) { Text("再来一局") } }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
