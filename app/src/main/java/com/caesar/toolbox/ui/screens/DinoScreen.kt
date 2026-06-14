package com.caesar.toolbox.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// 小恐龙（朝右方向 + 底部按钮）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val density = LocalDensity.current.density
    val baseY = 220f * density * 0.78f
    val prefs = remember { ctx.getSharedPreferences("dino", Context.MODE_PRIVATE) }
    var best by remember { mutableStateOf(prefs.getInt("dino_best", 0)) }
    var score by remember { mutableStateOf(0f) }
    var rexY by remember { mutableStateOf(0f) }
    var rexVy by remember { mutableStateOf(0f) }
    var rexDuck by remember { mutableStateOf(false) }
    var obstacles by remember { mutableStateOf(listOf<Triple<Float, Float, Boolean>>()) }
    var speed by remember { mutableStateOf(6f) }
    var isRunning by remember { mutableStateOf(false) }
    var isOver by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    // 自定义模式设置
    var customMode by remember { mutableStateOf(prefs.getBoolean("dino_custom", false)) }
    var jumpPower by remember { mutableStateOf(prefs.getFloat("dino_jump", 16f)) }
    var gravity by remember { mutableStateOf(prefs.getFloat("dino_gravity", 0.8f)) }
    var cactusBase by remember { mutableStateOf(prefs.getFloat("dino_cactus", 30f)) }
    var scoreMul by remember { mutableStateOf(prefs.getFloat("dino_score_mul", 1f)) }

    fun jump() { if (rexY >= 0f) { rexVy = -jumpPower } }
    fun reset() { score=0f; rexY=0f; rexVy=0f; rexDuck=false; obstacles=emptyList(); speed=6f; isOver=false }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            // 平滑物理：较小的重力增量与位移缩放，减少跳跃卡顿
            // 使用可配置的重力和位移系数
            rexVy += gravity
            rexY += rexVy * 0.9f
            if (rexY >= 0f) { rexY = 0f; rexVy = 0f }
            if (obstacles.isEmpty() || obstacles.last().first < 500f + Random.nextFloat()*250f)
                obstacles = obstacles + Triple(900f, cactusBase + Random.nextFloat()*20f, score > 300f && Random.nextFloat() < 0.25f)
            obstacles = obstacles.map { it.copy(first = it.first - speed) }.filter { it.first > -60f }
            val dH = if (rexDuck) 20f else 40f
            val dBot = baseY + rexY
            for (o in obstacles) {
                val oLeft = o.first; val oRight = o.first + (if (o.third) 30f else 18f)
                val oTop = baseY - o.second; val oBot = baseY
                if (80f < oRight && 120f > oLeft && dBot - dH < oBot && dBot > oTop) {
                    isRunning=false; isOver=true; if (score.toInt()>best) { best=score.toInt(); prefs.edit().putInt("dino_best", best).apply() }
                }
            }
            score += 0.15f * scoreMul; speed = 6f + score/120f; delay(16)
        }
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("小恐龙", fontWeight = FontWeight.Bold) },
            navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("得分: ${score.toInt()}", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("🏆 $best", fontSize = 14.sp)
            }
            Box(Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    val gray = Color(0xFF555555)
                    drawLine(Color(0xFF8B7355), Offset(0f, baseY), Offset(size.width, baseY), 2f)
                    val dx = 80f; val dy = baseY + rexY - 40f
                    if (rexDuck) {
                        drawRoundRect(gray, Offset(dx, dy + 20f), Size(50f, 18f), CornerRadius(6f))
                        drawCircle(gray, 12f, Offset(dx + 38f, dy + 15f)) // 头朝右
                    } else {
                        drawRoundRect(gray, Offset(dx + 14f, dy + 8f), Size(28f, 30f), CornerRadius(6f))
                        drawCircle(gray, 14f, Offset(dx + 42f, dy)) // 头朝右
                        drawLine(gray, Offset(dx+18f, dy+38f), Offset(dx+10f, dy+48f), 5f); drawLine(gray, Offset(dx+28f, dy+38f), Offset(dx+36f, dy+48f), 5f)
                        drawCircle(Color.White, 4f, Offset(dx+44f, dy-4f)); drawCircle(Color.Black, 2f, Offset(dx+45f, dy-4f))
                        drawLine(gray, Offset(dx+3f, dy+14f), Offset(dx-5f, dy+6f), 4f) // 尾巴朝左
                    }
                    obstacles.forEach { (x, h, bird) ->
                        if (bird) drawCircle(Color(0xFFFF9800), 8f, Offset(x+15f, baseY-h-12f))
                        else drawRoundRect(Color(0xFF4CAF50), Offset(x, baseY-h), Size(18f, h), CornerRadius(3f))
                    }
                }
            }
            // 底部按钮
            if (started && !isOver) Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { jump() }) { Text("⬆ 跳跃") }
                Button(onClick = { rexDuck = !rexDuck }) { Text(if (rexDuck) "⬆ 站起" else "⬇ 趴下") }
            }
            // 自定义模式开关与滑块
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("自定义模式", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Switch(checked = customMode, onCheckedChange = {
                    customMode = it; prefs.edit().putBoolean("dino_custom", it).apply()
                })
            }
            if (customMode) {
                Column(Modifier.padding(8.dp)) {
                    Text("跳跃力度: ${jumpPower}" , fontSize = 12.sp)
                    Slider(value = jumpPower, onValueChange = { jumpPower = it }, valueRange = 8f..28f)
                    Text("重力加速度: ${gravity}", fontSize = 12.sp)
                    Slider(value = gravity, onValueChange = { gravity = it }, valueRange = 0.2f..2.0f)
                    Text("仙人掌基高: ${cactusBase}", fontSize = 12.sp)
                    Slider(value = cactusBase, onValueChange = { cactusBase = it }, valueRange = 10f..80f)
                    Text("得分倍率: ${"%.2f".format(scoreMul)}", fontSize = 12.sp)
                    Slider(value = scoreMul, onValueChange = { scoreMul = it }, valueRange = 0.5f..3.0f)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            prefs.edit().putFloat("dino_jump", jumpPower).putFloat("dino_gravity", gravity).putFloat("dino_cactus", cactusBase).putFloat("dino_score_mul", scoreMul).apply()
                        }) { Text("保存设置") }
                        Button(onClick = {
                            // 恢复默认
                            jumpPower = 16f; gravity = 0.8f; cactusBase = 30f; scoreMul = 1f
                        }) { Text("重置默认") }
                    }
                }
            }
            if (!started) Text("点击「开始」后使用下方按钮操控", fontSize=13.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            if (isOver) { Text("游戏结束！得分 ${score.toInt()}", fontWeight=FontWeight.Bold, color=MaterialTheme.colorScheme.error)
                Button(onClick={reset(); isRunning=true; started=true}){Text("再来一局")} }
            if (!started) Button(onClick={started=true; isRunning=true}){Text("开始游戏")}
        }
    }
}
