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
    var speed by remember { mutableStateOf(120L) }

    fun randomFood(): Pair<Int,Int> {
        val all = (0 until COLS).flatMap { x -> (0 until ROWS).map { y -> x to y } } - body.toSet()
        return if (all.isEmpty()) 0 to 0 else all[Random.nextInt(all.size)]
    }
    fun resetGame() { body = initSnake; food = randomFood(); dir = 1 to 0; score = 0; over = false; speed = 120L }

    LaunchedEffect(over) {
        if (over) {
            if (score > best) { best = score; prefs.edit().putInt("best", best).apply() }
            return@LaunchedEffect
        }
        while (!over) {
            delay(speed)
            val head = body.first()
            val nh = (head.first + dir.first + COLS) % COLS to (head.second + dir.second + ROWS) % ROWS
            if (nh in body) { over = true; return@LaunchedEffect }
            val ate = nh == food
            body = if (ate) listOf(nh) + body else listOf(nh) + body.dropLast(1)
            if (ate) { score += 10; food = randomFood(); speed = (speed * 0.97).toLong().coerceAtLeast(50) }
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
                    var totalX = 0f; var totalY = 0f
                    detectDragGestures(onDrag = { _, off -> totalX += off.x; totalY += off.y },
                        onDragEnd = {
                            val nd = if (kotlin.math.abs(totalX) > kotlin.math.abs(totalY)) (if (totalX > 0) 1 else -1) to 0
                            else 0 to (if (totalY > 0) 1 else -1)
                            if (nd.first != -dir.first || nd.second != -dir.second) dir = nd
                            totalX = 0f; totalY = 0f
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
