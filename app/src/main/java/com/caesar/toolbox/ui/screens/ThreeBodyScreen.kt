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
// using PointerInputChange.consume() member
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    var saveReplay by remember { mutableStateOf(prefs.getBoolean("save_replay", false)) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var replayFiles by remember { mutableStateOf(listOf<String>()) }
    var showReplayDialog by remember { mutableStateOf(false) }
    var paramFiles by remember { mutableStateOf(listOf<String>()) }
    var showParamsDialog by remember { mutableStateOf(false) }
    var isPlayingReplay by remember { mutableStateOf(false) }
    var replayFrames by remember { mutableStateOf<List<List<Map<String, Float>>>?>(null) }
    var replayIndex by remember { mutableStateOf(0) }
    var replaySpeed by remember { mutableStateOf(1f) }
    // per-body vectors: list of Pair(angle, mag)
    var vectors by remember { mutableStateOf(listOf<Pair<Float,Float>>()) }
    var lastSavedReplay by remember { mutableStateOf<String?>(null) }
    var rotatingIndex by remember { mutableStateOf<Int?>(null) }

    // Physics engine handling (must be declared before start/reset functions)
    val engine = remember { com.caesar.toolbox.physics.PhysicsEngine(scope) }
    val record = remember { mutableStateListOf<List<Map<String, Float>>>() }

    fun reset() {
        state = SimState.PLACING; bodies = emptyList(); elapsed = 0f; vectors = emptyList(); lastSavedReplay = null
        engine.reset(); engine.stop(); record.clear()
    }
    fun start() {
        if (bodies.size == 3) {
            // 应用每体初速度并将数据传给引擎
            val pbs = bodies.mapIndexed { i, b ->
                val (ang, mag) = vectors.getOrNull(i) ?: (0f to 0f)
                val vx = (cos(Math.toRadians(ang.toDouble())) * mag).toFloat()
                val vy = (sin(Math.toRadians(ang.toDouble())) * mag).toFloat()
                com.caesar.toolbox.physics.PhysicsEngine.Body(b.x, b.y, vx, vy, b.mass)
            }
            engine.setBodies(pbs)
            elapsed = 0f; record.clear()
            engine.start()
            state = SimState.RUNNING
        }
    }

    val curState by rememberUpdatedState(state)
    val curBodies by rememberUpdatedState(bodies)

    DisposableEffect(engine) {
        engine.setOnTickListener { snaps ->
            // snaps come from physics thread; update UI on main
            scope.launch {
                val uiBodies = snaps.mapIndexed { i, pb -> Body(pb.x, pb.y, pb.vx, pb.vy, pb.mass, bodyColors.getOrElse(i) { Color.White }, emptyList()) }
                bodies = uiBodies
                record.add(snaps.map { mapOf("x" to it.x, "y" to it.y, "vx" to it.vx, "vy" to it.vy, "m" to it.mass) })
                elapsed += 0.016f
            }
        }
        // collisions now generate fragments inside PhysicsEngine; no automatic end-of-simulation here
        engine.setOnCollision {
            scope.launch {
                // auto-save if enabled and we have recorded frames
                if (saveReplay && record.isNotEmpty()) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                        val name = "threebody_replay_auto_${sdf.format(java.util.Date())}_${elapsed.toInt()}s.json"
                        val jarr = org.json.JSONArray()
                        for (frame in record) {
                            val farr = org.json.JSONArray()
                            for (obj in frame) {
                                val jo = org.json.JSONObject()
                                jo.put("x", obj["x"])
                                jo.put("y", obj["y"])
                                jo.put("vx", obj["vx"])
                                jo.put("vy", obj["vy"])
                                jo.put("m", obj["m"])
                                farr.put(jo)
                            }
                            jarr.put(farr)
                        }
                        ctx.openFileOutput(name, Context.MODE_PRIVATE).use { it.write(jarr.toString().toByteArray()) }
                        lastSavedReplay = name
                        // refresh list
                        val files = ctx.filesDir.listFiles()?.mapNotNull { it.name }?.filter { it.startsWith("threebody_replay_") } ?: emptyList()
                        replayFiles = files
                        snackbarHostState.showSnackbar("已自动保存回放: $name")
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("自动保存回放失败")
                    }
                } else {
                    snackbarHostState.showSnackbar("检测到碰撞（未保存回放，切换“保存回放”开启）")
                }
            }
        }
        onDispose { engine.stop() }
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { androidx.compose.material3.SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("三体运动（半成品）", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("保存回放", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Switch(checked = saveReplay, onCheckedChange = {
                    saveReplay = it; prefs.edit().putBoolean("save_replay", it).apply()
                })
                Spacer(Modifier.width(16.dp))
                Button(onClick = {
                    // 列出回放文件
                    val files = ctx.filesDir.listFiles()?.mapNotNull { it.name }?.filter { it.startsWith("threebody_replay_") } ?: emptyList()
                    replayFiles = files
                    showReplayDialog = true
                }) { Text("回放文件") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    // 列出参数文件以导入初始参数
                    val files = ctx.filesDir.listFiles()?.mapNotNull { it.name }?.filter { it.startsWith("threebody_params_") } ?: emptyList()
                    paramFiles = files
                    showParamsDialog = true
                }) { Text("导入参数") }
            }
            // 显示上次保存的回放文件名
            if (lastSavedReplay != null) Text(lastSavedReplay ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            // 为每个已放置的天体提供角度/幅度控制（放置三个天体后显示）
            Spacer(Modifier.height(8.dp))
            if (bodies.size == 3) {
                Column(Modifier.padding(8.dp)) {
                    for (i in 0 until 3) {
                        val a = vectors.getOrNull(i)?.first ?: 0f
                        val m = vectors.getOrNull(i)?.second ?: 0f
                        Text("天体 ${i+1}：角度 ${a.toInt()}°，幅度 ${"%.1f".format(m)}", fontSize = 12.sp)
                        Slider(value = a, onValueChange = { nv ->
                            val list = vectors.toMutableList()
                            while (list.size <= i) list.add(0f to 0f)
                            list[i] = nv to (list[i].second)
                            vectors = list
                        }, valueRange = 0f..360f)
                        Slider(value = m, onValueChange = { nv ->
                            val list = vectors.toMutableList()
                            while (list.size <= i) list.add(0f to 0f)
                            list[i] = (list[i].first) to nv
                            vectors = list
                        }, valueRange = 0f..30f)
                    }
                }
            }

            // 回放选择对话框
            if (showReplayDialog) {
                AlertDialog(onDismissRequest = { showReplayDialog = false },
                    title = { Text("选择回放") },
                    text = {
                        Column {
                            if (replayFiles.isEmpty()) Text("未找到回放文件")
                            else replayFiles.forEach { name ->
                                TextButton(onClick = {
                                    showReplayDialog = false
                                    scope.launch {
                                        try {
                                            // parse into frames and start replay player
                                            val txt = ctx.openFileInput(name).bufferedReader().use { it.readText() }
                                            val jarr = org.json.JSONArray(txt)
                                            val frames = mutableListOf<List<Map<String, Float>>>()
                                            for (i in 0 until jarr.length()) {
                                                val frame = jarr.getJSONArray(i)
                                                val f = mutableListOf<Map<String, Float>>()
                                                for (k in 0 until frame.length()) {
                                                    val obj = frame.getJSONObject(k)
                                                    val map = mapOf(
                                                        "x" to obj.optDouble("x", 0.0).toFloat(),
                                                        "y" to obj.optDouble("y", 0.0).toFloat(),
                                                        "vx" to obj.optDouble("vx", 0.0).toFloat(),
                                                        "vy" to obj.optDouble("vy", 0.0).toFloat(),
                                                        "m" to obj.optDouble("m", 1.0).toFloat()
                                                    )
                                                    f.add(map)
                                                }
                                                frames.add(f)
                                            }
                                            replayFrames = frames
                                            replayIndex = 0
                                            // pause engine to avoid interference
                                            engine.pause()
                                            isPlayingReplay = true
                                            state = SimState.RUNNING
                                        } catch (_: Exception) {
                                            // ignore
                                        }
                                    }
                                }) { Text(name) }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showReplayDialog = false }) { Text("关闭") } }
                )
            }

            if (showParamsDialog) {
                AlertDialog(onDismissRequest = { showParamsDialog = false },
                    title = { Text("选择参数文件") },
                    text = {
                        Column {
                            if (paramFiles.isEmpty()) Text("未找到参数文件")
                            else paramFiles.forEach { name ->
                                TextButton(onClick = {
                                    showParamsDialog = false
                                    scope.launch {
                                        try {
                                            val txt = ctx.openFileInput(name).bufferedReader().use { it.readText() }
                                            val jarr = org.json.JSONArray(txt)
                                            val newBodies = mutableListOf<Body>()
                                            for (i in 0 until jarr.length()) {
                                                val obj = jarr.getJSONObject(i)
                                                val x = obj.optDouble("x", 0.0).toFloat(); val y = obj.optDouble("y", 0.0).toFloat()
                                                val vx = obj.optDouble("vx", 0.0).toFloat(); val vy = obj.optDouble("vy", 0.0).toFloat()
                                                val m = obj.optDouble("m", 1.0).toFloat()
                                                newBodies.add(Body(x, y, vx, vy, m, bodyColors.getOrElse(i) { Color.White }, emptyList()))
                                            }
                                            bodies = newBodies
                                            // ensure placing mode so user can adjust vectors
                                            state = SimState.PLACING
                                        } catch (_: Exception) {
                                        }
                                    }
                                }) { Text(name) }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showParamsDialog = false }) { Text("关闭") } }
                )
            }

            // OpenGL 渲染视图（下层） + Compose 覆盖层（上层）
            var glViewRef: com.caesar.toolbox.gl.ThreeBodyGLSurfaceView? = null
            var cameraAzimuth by remember { mutableStateOf(0f) }
            var cameraElevation by remember { mutableStateOf(20f) }
            var cameraDistance by remember { mutableStateOf(800f) }
            var cameraFocusX by remember { mutableStateOf(0f) }
            var cameraFocusY by remember { mutableStateOf(0f) }
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        var prevDist: Float? = null
                        var prevCentroid: Offset? = null
                        while (true) {
                            val ev = awaitPointerEvent()
                            val changes = ev.changes
                            val p = changes.first()
                            // 放置阶段的触控逻辑（同 Canvas 逻辑）
                            if (curState == SimState.PLACING) {
                                if (curBodies.size < 3) {
                                    if (p.pressed && (p.previousPressed == false)) {
                                        bodies = curBodies + Body(p.position.x, p.position.y, mass = 1f + curBodies.size * 0.5f, color = bodyColors[curBodies.size])
                                        if (curBodies.size + 1 == 3) {
                                            val list = mutableListOf<Pair<Float,Float>>()
                                            repeat(3) { list.add(0f to 0f) }
                                            vectors = list
                                        }
                                        p.consume()
                                    }
                                } else {
                                    val pos = p.position
                                    if (p.pressed && (p.previousPressed == false)) {
                                        var picked: Int? = null
                                        for (i in curBodies.indices) {
                                            val b = curBodies[i]
                                            val ang = vectors.getOrNull(i)?.first ?: 0f
                                            val mag = vectors.getOrNull(i)?.second ?: 0f
                                            val len = mag * 6f
                                            val ex = b.x + (cos(Math.toRadians(ang.toDouble())) * len).toFloat()
                                            val ey = b.y + (sin(Math.toRadians(ang.toDouble())) * len).toFloat()
                                            val dToEnd = hypot(pos.x - ex, pos.y - ey)
                                            val dToCenter = hypot(pos.x - b.x, pos.y - b.y)
                                            if (dToEnd < 40f || dToCenter < 24f) { picked = i; break }
                                        }
                                        rotatingIndex = picked
                                        p.consume()
                                    }
                                    if (p.pressed && rotatingIndex != null) {
                                        val idx = rotatingIndex!!
                                        val b = curBodies.getOrNull(idx) ?: continue
                                        val dx = pos.x - b.x; val dy = pos.y - b.y
                                        val ang = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))).toFloat()
                                        val dist = hypot(dx, dy)
                                        val mag = (dist / 6f).coerceIn(0f, 30f)
                                        val list = vectors.toMutableList()
                                        while (list.size <= idx) list.add(0f to 0f)
                                        list[idx] = ang to mag
                                        vectors = list
                                        p.consume()
                                    }
                                    if (!p.pressed && rotatingIndex != null) {
                                        rotatingIndex = null
                                        p.consume()
                                    }
                                }
                            } else {
                                // camera gestures when not placing
                                if (changes.size == 1) {
                                    val c = changes[0]
                                    if (c.pressed) {
                                        val delta = c.position - c.previousPosition
                                        if (delta != Offset.Zero) {
                                            cameraAzimuth += delta.x * 0.2f
                                            cameraElevation = (cameraElevation - delta.y * 0.2f).coerceIn(-89f, 89f)
                                            glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, bodies.firstOrNull()?.x ?: 0f, bodies.firstOrNull()?.y ?: 0f)
                                            c.consume()
                                        }
                                        prevDist = null; prevCentroid = null
                                    }
                                } else if (changes.size >= 2) {
                                    val c0 = changes[0]; val c1 = changes[1]
                                    val curDist = (c0.position - c1.position).getDistance()
                                    val curCentroid = Offset((c0.position.x + c1.position.x) / 2f, (c0.position.y + c1.position.y) / 2f)
                                    val prevD = prevDist
                                    if (prevD != null) {
                                        val scale = curDist / prevD
                                        if (scale.isFinite()) {
                                            cameraDistance = (cameraDistance / scale).coerceIn(100f, 5000f)
                                            glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, bodies.firstOrNull()?.x ?: 0f, bodies.firstOrNull()?.y ?: 0f)
                                        }
                                        val pc = prevCentroid
                                        if (pc != null) {
                                            val dcent = curCentroid - pc
                                            cameraFocusX -= dcent.x
                                            cameraFocusY += dcent.y
                                            glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, cameraFocusX, cameraFocusY)
                                        }
                                    }
                                    prevDist = curDist
                                    prevCentroid = curCentroid
                                    c0.consume(); c1.consume()
                                }
                            }
                        }
                    }
                }) {
                // 下层 OpenGL 视图
                AndroidView(factory = { ctx ->
                    val v = com.caesar.toolbox.gl.ThreeBodyGLSurfaceView(ctx)
                    glViewRef = v
                    v
                }, modifier = Modifier.matchParentSize())

                // 上层 Compose 画布用于轨迹与箭头覆盖
                Canvas(modifier = Modifier.matchParentSize()) {
                    bodies.forEach { b ->
                        if (b.trail.size > 1) for (i in 1 until b.trail.size) {
                            drawLine(b.color.copy(alpha = 0.3f * i / b.trail.size), b.trail[i - 1], b.trail[i], strokeWidth = 1.5f)
                        }
                    }
                    bodies.forEachIndexed { idx, b ->
                        // 不再绘制主体圆，但绘制控制箭头端点与高亮
                        val vec = vectors.getOrNull(idx)
                        if (vec != null) {
                            val ang = vec.first
                            val mag = vec.second
                            val len = mag * 6f
                            val ex = b.x + (cos(Math.toRadians(ang.toDouble())) * len).toFloat()
                            val ey = b.y + (sin(Math.toRadians(ang.toDouble())) * len).toFloat()
                            val isActive = rotatingIndex == idx
                            drawLine(b.color.copy(alpha = if (isActive) 1f else 0.9f), Offset(b.x, b.y), Offset(ex, ey), strokeWidth = if (isActive) 5f else 3f)
                            drawCircle(b.color, if (isActive) 6f else 4f, Offset(ex, ey))
                            if (isActive) drawCircle(Color.White.copy(alpha = 0.6f), 10f, Offset(b.x, b.y))
                        }
                    }
                }
                // 每次 bodies 变更时把屏幕坐标传给 GL 视图
                LaunchedEffect(bodies) {
                    // prepare float array: x,y,r,g,b,size
                    val arr = FloatArray(bodies.size * 6)
                    bodies.forEachIndexed { i, b ->
                        arr[i * 6] = b.x
                        arr[i * 6 + 1] = b.y
                        val col = when (i) {
                            0 -> floatArrayOf(1f, 0.412f, 0.412f)
                            1 -> floatArrayOf(0.306f, 0.804f, 0.765f)
                            2 -> floatArrayOf(1f, 0.902f, 0.427f)
                            else -> floatArrayOf(1f, 1f, 1f)
                        }
                        arr[i * 6 + 2] = col[0]; arr[i * 6 + 3] = col[1]; arr[i * 6 + 4] = col[2]
                        arr[i * 6 + 5] = 10f + b.mass * 4f
                    }
                    glViewRef?.updateBodiesScreen(arr)
                    // update camera focus to center or first body
                    cameraFocusX = bodies.firstOrNull()?.x ?: 0f
                    cameraFocusY = bodies.firstOrNull()?.y ?: 0f
                    glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, cameraFocusX, cameraFocusY)
                }
            }

            // Camera controls
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("相机：轨道/缩放/焦点", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("方位: ${cameraAzimuth.toInt()}°", modifier = Modifier.width(100.dp))
                    Slider(value = cameraAzimuth, onValueChange = { v -> cameraAzimuth = v; glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, bodies.firstOrNull()?.x ?: 0f, bodies.firstOrNull()?.y ?: 0f) }, valueRange = 0f..360f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("仰俯: ${cameraElevation.toInt()}°", modifier = Modifier.width(100.dp))
                    Slider(value = cameraElevation, onValueChange = { v -> cameraElevation = v; glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, bodies.firstOrNull()?.x ?: 0f, bodies.firstOrNull()?.y ?: 0f) }, valueRange = -89f..89f, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("距离: ${cameraDistance.toInt()}", modifier = Modifier.width(100.dp))
                    Slider(value = cameraDistance, onValueChange = { v -> cameraDistance = v; glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, bodies.firstOrNull()?.x ?: 0f, bodies.firstOrNull()?.y ?: 0f) }, valueRange = 100f..3000f, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        // focus on first body if exists
                        val fb = bodies.firstOrNull()
                        if (fb != null) glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, fb.x, fb.y)
                    }) { Text("聚焦第1个天体") }
                    Button(onClick = {
                        glViewRef?.setCamera(cameraAzimuth, cameraElevation, cameraDistance, 0f, 0f)
                    }) { Text("重置焦点") }
                }
            }

            // 控制按钮 + 仿真控制
            var simPaused by remember { mutableStateOf(false) }
            var speedMul by remember { mutableStateOf(1f) }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { reset() }, modifier = Modifier.weight(1f)) { Text("重置") }
                    Button(onClick = { start() }, modifier = Modifier.weight(1f), enabled = bodies.size == 3 && state != SimState.RUNNING) { Text("开始") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        // save current recorded replay to file
                        if (record.isNotEmpty()) {
                            try {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                                val name = "threebody_replay_${sdf.format(java.util.Date())}_${elapsed.toInt()}s.json"
                                val jarr = org.json.JSONArray()
                                for (frame in record) {
                                    val farr = org.json.JSONArray()
                                    for (obj in frame) {
                                        val jo = org.json.JSONObject()
                                        jo.put("x", obj["x"])
                                        jo.put("y", obj["y"])
                                        jo.put("vx", obj["vx"])
                                        jo.put("vy", obj["vy"])
                                        jo.put("m", obj["m"])
                                        farr.put(jo)
                                    }
                                    jarr.put(farr)
                                }
                                ctx.openFileOutput(name, Context.MODE_PRIVATE).use { it.write(jarr.toString().toByteArray()) }
                                lastSavedReplay = name
                                // refresh list
                                val files = ctx.filesDir.listFiles()?.mapNotNull { it.name }?.filter { it.startsWith("threebody_replay_") } ?: emptyList()
                                replayFiles = files
                            } catch (_: Exception) { }
                        }
                    }) { Text("保存回放") }
                    Button(onClick = {
                        // export initial parameters: positions, velocities, masses
                        try {
                            val name = "threebody_params_${java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(java.util.Date())}.json"
                            val pa = org.json.JSONArray()
                            for (b in bodies) {
                                val jo = org.json.JSONObject()
                                jo.put("x", b.x); jo.put("y", b.y); jo.put("vx", b.vx); jo.put("vy", b.vy); jo.put("m", b.mass)
                                pa.put(jo)
                            }
                            ctx.openFileOutput(name, Context.MODE_PRIVATE).use { it.write(pa.toString().toByteArray()) }
                        } catch (_: Exception) { }
                    }) { Text("导出参数") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        if (!simPaused) {
                            engine.pause(); simPaused = true
                        } else {
                            engine.resume(); simPaused = false
                        }
                    }) { Text(if (!simPaused) "暂停" else "继续") }
                    Button(onClick = { engine.singleStep() }) { Text("单步") }
                    Spacer(Modifier.width(8.dp))
                    Text("速度: ${"%.1f".format(speedMul)}x", modifier = Modifier.width(120.dp))
                    Slider(value = speedMul, onValueChange = { v -> speedMul = v; engine.setSpeedMultiplier(v) }, valueRange = 0.1f..300f, modifier = Modifier.weight(1f))
                }
            }
            // 回放播放器控件（简洁）
            if (replayFrames != null) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (isPlayingReplay) {
                                isPlayingReplay = false
                            } else {
                                // start/resume playback
                                engine.pause()
                                isPlayingReplay = true
                            }
                        }) { Text(if (isPlayingReplay) "暂停回放" else "播放回放") }
                        Text("速度: ${"%.1f".format(replaySpeed)}x", modifier = Modifier.width(100.dp))
                        Slider(value = replaySpeed, onValueChange = { v -> replaySpeed = v }, valueRange = 0.1f..4f, modifier = Modifier.weight(1f))
                    }
                    val total = replayFrames?.size ?: 0
                    if (total > 0) {
                        Slider(value = replayIndex.toFloat(), onValueChange = { v ->
                            replayIndex = v.toInt().coerceIn(0, total - 1)
                            // jump to frame
                            val frame = replayFrames?.getOrNull(replayIndex)
                            if (frame != null) bodies = frame.mapIndexed { i, obj -> Body(obj["x"] ?: 0f, obj["y"] ?: 0f, obj["vx"] ?: 0f, obj["vy"] ?: 0f, obj["m"] ?: 1f, bodyColors.getOrElse(i) { Color.White }, emptyList()) }
                        }, valueRange = 0f..(max(0, total - 1).toFloat()))
                        Text("帧 ${replayIndex + 1} / $total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { replayFrames = null; isPlayingReplay = false }) { Text("停止回放") }
                        }
                    }
                }
            }
            // 回放状态提示
            if (isPlayingReplay) Text("回放中...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // playback effect
            LaunchedEffect(isPlayingReplay, replayFrames, replaySpeed, replayIndex) {
                if (replayFrames != null && isPlayingReplay) {
                    val frames = replayFrames ?: return@LaunchedEffect
                    while (isPlayingReplay && replayIndex < frames.size) {
                        val frame = frames[replayIndex]
                        bodies = frame.mapIndexed { i, obj -> Body(obj["x"] ?: 0f, obj["y"] ?: 0f, obj["vx"] ?: 0f, obj["vy"] ?: 0f, obj["m"] ?: 1f, bodyColors.getOrElse(i) { Color.White }, emptyList()) }
                        // advance
                        replayIndex = (replayIndex + 1).coerceAtMost(frames.size - 1)
                        // delay respects speed multiplier
                        val delayMs = (16.0 / replaySpeed).toLong().coerceAtLeast(1L)
                        delay(delayMs)
                        if (replayIndex >= frames.size - 1) {
                            isPlayingReplay = false
                        }
                    }
                }
            }
        }
    }
}
