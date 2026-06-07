package com.caesar.toolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.caesar.toolbox.data.CrashHandler
import com.caesar.toolbox.data.UpdateChecker
import com.caesar.toolbox.ui.theme.CaesarTBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.init(this)
        enableEdgeToEdge()
        setContent {
            CaesarTBTheme {
                var updateInfo by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

                // 启动时自动检查
                LaunchedEffect(Unit) {
                    val info = UpdateChecker.check()
                    if (info.hasUpdate) updateInfo = info
                }

                CaesarTBApp(
                    updateInfo = updateInfo,
                    onDismissUpdate = { updateInfo = null },
                    onDownload = { url ->
                        UpdateChecker.openDownload(this@MainActivity, url)
                        updateInfo = null
                    },
                    onCheckUpdate = {
                        val info = UpdateChecker.check()
                        if (info.hasUpdate) updateInfo = info
                        info.hasUpdate
                    }
                )
            }
        }
    }
}
