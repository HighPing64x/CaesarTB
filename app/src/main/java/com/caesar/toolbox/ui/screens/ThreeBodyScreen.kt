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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

// 天体数据
data class Body(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f, val mass: Float = 1f, val color: Color = Color.Red, var trail: List<Offset> = emptyList())

enum class SimState { PLACING, RUNNING, ENDED }

private val bodyColors = listOf(Color(0xFFFF6B6B), Color(0xFF4ECDC4), Color(0xFFFFE66D))
private const val G = 800f
private const val SOFTEN = 15f
private const val CRASH_DIST = 30f
private const val MAX_TRAIL = 80

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeBodyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("threebody", Context.MODE_PRIVATE) }
    var state by remember { mutableStateOf(SimState.PLACING) }
    var bodies by remember { mutableStateOf(listOf<Body>()) }
    var elapsed by remember { mutableStateOf(0f) }
    var bestTime by remember { mutableStateOf<Float>(prefs.getFloat("best", 0f)) }

    fun reset() { state = SimState.PLACING; bodies = emptyList(); elapsed = 0f }
    fun start() { if (bodies.size == 3) { state = SimState.RUNNING; elapsed = 0f } }

    LaunchedEffect(state) {
        if (state != SimState.RUNNING) return@LaunchedEffect
        while (state == SimState.RUNNING) {
            val b = bodies.toMutableList()
            // 计算引力
            for (i in b.indices) for (j in i + 1 until b.size) {
                val dx = b[j].x - b[i].x; val dy = b[j].y - b[i].y
                val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(SOFTEN)
                val force = G * b[i].mass * b[j].mass / (dist * dist)
                val fx = force * dx / dist; val fy = force * dy / dist
                b[i] = b[i].copy(vx = b[i].vx + fx / b[i].mass, vy = b[i].vy + fy / b[i].mass)
                b[j] = b[j].copy(vx = b[j].vx - fx / b[j].mass, vy = b[j].vy - fy / b[j].mass)
            }
            // 更新位置
            for (i in b.indices) {
                val nx = b[i].x + b[i].vx; val ny = b[i].y + b[i].vy
                val trail = (listOf(Offset(b[i].x, b[i].y)) + b[i].trail).take(MAX_TRAIL)
                b[i] = b[i].copy(x = nx, y = ny, trail = trail)
            }
            bodies = b; elapsed += 0.016f
            // 碰撞检测
            for (i in b.indices) for (j in i + 1 until b.size) {
                val dx = b[i].x - b[j].x; val dy = b[i].y - b[j].y
                if (sqrt(dx * dx + dy * dy) < CRASH_DIST) {
                    state = SimState.ENDED
                    if (elapsed > bestTime) { bestTime = elapsed; prefs.edit().putFloat("best", elapsed).apply() }
                }
            }
            delay(16)
        }
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("三体运动", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // 状态提示
            Text(when (state) {
                SimState.PLACING -> "点击画面放置 ${bodies.size}/3 个天体"
                SimState.RUNNING -> "运行中… ${"%.1f".format(elapsed)}s"
                SimState.ENDED -> "碰撞！持续时间 ${"%.1f".format(elapsed)}s"
            }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

            // 最佳记录
            if (bestTime > 0) Text("🏆 最佳: ${"%.1f".format(bestTime)}s", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 画布
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(state) {
                    if (state != SimState.PLACING || bodies.size >= 3) return@pointerInput
                    awaitPointerEventScope {
                        val pos = awaitPointerEvent(); val p = pos.changes.first().position
                        bodies = bodies + Body(p.x, p.y, mass = 1f + bodies.size * 0.5f, color = bodyColors[bodies.size])
                        if (bodies.size == 3) start()
                    }
                }) {
                // 轨迹
                bodies.forEach { b ->
                    if (b.trail.size > 1) for (i in 1 until b.trail.size) {
                        drawLine(b.color.copy(alpha = 0.3f * i / b.trail.size), b.trail[i - 1], b.trail[i], strokeWidth = 1.5f)
                    }
                }
                // 天体
                bodies.forEach { b -> drawCircle(b.color, 10f + b.mass * 4f, Offset(b.x, b.y)) }
            }

            // 控制按钮
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { reset() }, modifier = Modifier.weight(1f)) { Text("重置") }
                Button(onClick = { start() }, modifier = Modifier.weight(1f), enabled = bodies.size == 3 && state != SimState.RUNNING) { Text("开始") }
            }
        }
    }
}
