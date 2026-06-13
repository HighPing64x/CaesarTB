package com.caesar.toolbox.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("dino", Context.MODE_PRIVATE) }
    var best by remember { mutableStateOf<Int>(prefs.getInt("dino_best", 0)) }

    var score by remember { mutableStateOf(0f) }
    var rexY by remember { mutableStateOf(0f) }; var rexVy by remember { mutableStateOf(0f) }
    var rexDuck by remember { mutableStateOf(false) }
    var obstacles by remember { mutableStateOf(listOf<Triple<Float, Float, Boolean>>()) } // x, h, isBird
    var speed by remember { mutableStateOf(6f) }
    var isRunning by remember { mutableStateOf(false) }
    var isOver by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    fun reset() { score=0f; rexY=0f; rexVy=0f; rexDuck=false; obstacles=emptyList(); speed=6f; isOver=false; started=false }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            rexVy += 0.8f; rexY += rexVy
            if (rexY >= 0f) { rexY = 0f; rexVy = 0f }
            if (obstacles.isEmpty() || obstacles.last().first < 500f + Random.nextFloat()*200f) {
                val h = 30f + Random.nextFloat()*20f
                obstacles = obstacles + Triple(900f, h, score > 400f && Random.nextFloat() < 0.25f)
            }
            obstacles = obstacles.map { it.copy(first = it.first - speed) }.filter { it.first > -60f }

            val dr = Rect(80f, 380f + rexY - (if (rexDuck) 20f else 40f), 40f, 40f)
            for (o in obstacles) {
                val or = Rect(o.first, 400f - o.second, if (o.third) 30f else 18f, o.second)
                if (dr.overlaps(or)) { isRunning=false; isOver=true; if (score.toInt()>best) { best=score.toInt(); prefs.edit().putInt("dino_best", best).apply() } }
            }
            score += 0.15f; speed = 6f + score/100f; delay(16)
        }
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("小恐龙", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("得分: ${score.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("🏆 $best", fontSize = 14.sp)
            }
            Box(Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    awaitPointerEventScope { while (true) {
                        val e = awaitPointerEvent()
                        if (e.changes.first().pressed) {
                            if (!started) { started=true; isRunning=true; score=0f; obstacles=emptyList(); rexY=0f }
                            else if (e.changes.first().position.y < 170f) rexVy = -12f
                            else rexDuck = true
                        } else rexDuck = false
                    } }
                }) {
                Canvas(Modifier.fillMaxSize()) {
                    val base = size.height * 0.8f; val gray = Color(0xFF555555)
                    drawLine(Color(0xFF8B7355), Offset(0f, base), Offset(size.width, base), 2f)
                    // 恐龙
                    val dx = 80f; val dy = base + rexY - 40f
                    if (rexDuck) {
                        drawRoundRect(gray, Offset(dx, dy + 20f), Size(50f, 18f), CornerRadius(6f))
                        drawCircle(gray, 12f, Offset(dx + 12f, dy + 15f))
                    } else {
                        drawRoundRect(gray, Offset(dx + 8f, dy + 8f), Size(28f, 30f), CornerRadius(6f))
                        drawCircle(gray, 14f, Offset(dx + 8f, dy))
                        drawLine(gray, Offset(dx+22f, dy+38f), Offset(dx+30f, dy+48f), 5f)
                        drawLine(gray, Offset(dx+14f, dy+38f), Offset(dx+6f, dy+48f), 5f)
                        drawCircle(Color.White, 4f, Offset(dx+2f, dy-4f))
                        drawCircle(Color.Black, 2f, Offset(dx+3f, dy-4f))
                        drawLine(gray, Offset(dx+36f, dy+14f), Offset(dx+44f, dy+6f), 4f)
                    }
                    // 障碍物
                    obstacles.forEach { (x, h, bird) ->
                        if (bird) {
                            drawCircle(Color(0xFFFF9800), 8f, Offset(x+15f, base-h-12f))
                        } else {
                            drawRoundRect(Color(0xFF4CAF50), Offset(x, base-h), Size(18f, h), CornerRadius(3f))
                        }
                    }
                }
            }
            if (!started) Text("点击上方跳跃，下方趴下", fontSize=13.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            if (isOver) { Text("游戏结束！得分 ${score.toInt()}", fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.error)
                Button(onClick={reset(); isRunning=true; started=true}){Text("再来一局")} }
            if (!started) Button(onClick={started=true; isRunning=true}){Text("开始游戏")}
        }
    }
}
