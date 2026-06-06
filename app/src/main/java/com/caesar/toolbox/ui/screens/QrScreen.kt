package com.caesar.toolbox.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        modifier = modifier, containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("二维码生成", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("← 返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = input, onValueChange = { input = it },
                label = { Text("输入内容（文字/链接）") }, modifier = Modifier.fillMaxWidth(), maxLines = 4)

            Button(onClick = {
                if (input.isNotBlank()) scope.launch {
                    bitmap = withContext(Dispatchers.Default) {
                        val writer = QRCodeWriter()
                        val matrix = writer.encode(input, BarcodeFormat.QR_CODE, 512, 512)
                        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
                        for (x in 0 until 512) for (y in 0 until 512)
                            bmp.setPixel(x, y, if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                        bmp
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("生成二维码") }

            bitmap?.let { bmp ->
                Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR",
                        modifier = Modifier.size(280.dp).padding(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        clipboard.setText(AnnotatedString(input))
                    }) { Text("复制内容") }
                }
            }
        }
    }
}
