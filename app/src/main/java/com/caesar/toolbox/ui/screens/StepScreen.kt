package com.caesar.toolbox.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    var realSteps by remember { mutableStateOf(0L) }
    var isSensorAvailable by remember { mutableStateOf(stepSensor != null) }
    var manualSteps by remember { mutableStateOf("") }
    var displayedSteps by remember { mutableStateOf(0L) }
    var targetSteps by remember { mutableStateOf(8000L) }
    var targetInput by remember { mutableStateOf("8000") }
    var history by remember { mutableStateOf<List<Pair<String, Long>>>(listOf("今天 00:00" to 0L)) }

    // 传感器监听
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    realSteps = event.values[0].toLong()
                    displayedSteps = realSteps
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        stepSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // 若传感器不可用则降级为手动模式
    LaunchedEffect(Unit) {
        if (!isSensorAvailable) { displayedSteps = manualSteps.toLongOrNull() ?: 0 }
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(title = { Text("微信步数", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // 步数展示圆环
            val progress = if (targetSteps > 0) (displayedSteps.toFloat() / targetSteps).coerceIn(0f, 1f) else 0f
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                CircularProgressIndicator(progress = progress, modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(displayedSteps.toString(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("步", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("目标 ${targetSteps} 步 · ${(progress * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 目标设置
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = targetInput, onValueChange = { targetInput = it.filter { c -> c.isDigit() } },
                    label = { Text("目标步数") }, modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = { targetInput.toLongOrNull()?.let { targetSteps = it } }) { Text("设定") }
            }

            // 手动输入（传感器不可用时作为主模式）
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = manualSteps, onValueChange = { manualSteps = it.filter { c -> c.isDigit() } },
                    label = { Text(if (isSensorAvailable) "手动覆盖步数" else "手动输入步数") },
                    modifier = Modifier.weight(1f), singleLine = true)
                Button(onClick = {
                    manualSteps.toLongOrNull()?.let { displayedSteps = it }
                }) { Text("更新") }
            }

            if (isSensorAvailable) Text("📱 传感器: 真实步数 $realSteps 步", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            // 快捷操作
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3000L to "3千", 6000L to "6千", 10000L to "1万", 20000L to "2万", 30000L to "3万").forEach { (v, label) ->
                    OutlinedButton(onClick = { displayedSteps = v }, modifier = Modifier.weight(1f)) { Text(label) }
                }
            }

            // 历史记录区
            if (history.size > 1) {
                Text("步数记录", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(history) { (time, steps) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$steps 步", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
