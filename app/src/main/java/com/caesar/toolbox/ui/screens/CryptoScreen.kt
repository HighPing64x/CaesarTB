package com.caesar.toolbox.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.*
import java.security.spec.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.*
import java.util.Base64 as JBase64

// ============================================================
// 加密解密工具
// ============================================================

private enum class CipherTab(val label: String) {
    BASE64("Base64"), CAESAR("凯撒"), MORSE("摩斯"),
    AES("AES"), RSA("RSA"), ECC("ECC")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var tab by remember { mutableStateOf(CipherTab.BASE64) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("加解密", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标签选择
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                CipherTab.entries.forEach { t ->
                    FilterChip(
                        selected = tab == t,
                        onClick = { tab = t },
                        label = { Text(t.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            when (tab) {
                CipherTab.BASE64 -> Base64Panel()
                CipherTab.CAESAR -> CaesarPanel()
                CipherTab.MORSE -> MorsePanel()
                CipherTab.AES -> AesPanel()
                CipherTab.RSA -> RsaPanel()
                CipherTab.ECC -> EccPanel()
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ========== 通用输入输出组件 ==========

@Composable
private fun CryptoIO(
    input: String, onInputChange: (String) -> Unit,
    output: String, labelIn: String = "输入", labelOut: String = "输出",
    buttons: @Composable RowScope.() -> Unit
) {
    val clipboard = LocalClipboardManager.current
    OutlinedTextField(value = input, onValueChange = onInputChange,
        label = { Text(labelIn) }, modifier = Modifier.fillMaxWidth(), maxLines = 4,
        trailingIcon = {
            if (input.isNotEmpty()) IconButton(onClick = { clipboard.setText(AnnotatedString(input)) }) {
                Icon(Icons.Outlined.ContentCopy, "复制", modifier = Modifier.size(20.dp))
            }
        })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { buttons() }
    OutlinedTextField(value = output, onValueChange = {},
        label = { Text(labelOut) }, modifier = Modifier.fillMaxWidth(),
        readOnly = true, maxLines = 6,
        trailingIcon = {
            if (output.isNotEmpty()) IconButton(onClick = { clipboard.setText(AnnotatedString(output)) }) {
                Icon(Icons.Outlined.ContentCopy, "复制", modifier = Modifier.size(20.dp))
            }
        })
}

// ========== Base64 ==========

@Composable
private fun Base64Panel() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    CryptoIO(input, { input = it }, output, buttons = {
        Button(onClick = { output = JBase64.getEncoder().encodeToString(input.toByteArray()) }) { Text("编码") }
        Button(onClick = { try { output = JBase64.getDecoder().decode(input).toString(Charsets.UTF_8) } catch (_: Exception) { output = "解码失败" } }) { Text("解码") }
    })
}

// ========== 凯撒密码 ==========

@Composable
private fun CaesarPanel() {
    var input by remember { mutableStateOf("") }
    var shift by remember { mutableStateOf("3") }
    var output by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = shift, onValueChange = { shift = it.filter { c -> c.isDigit() || c == '-' } },
            label = { Text("偏移量") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        CryptoIO(input, { input = it }, output, buttons = {
            Button(onClick = {
                val s = shift.toIntOrNull() ?: 3
                output = input.map { (it.code + s).toChar() }.joinToString("")
            }) { Text("加密") }
            Button(onClick = {
                val s = shift.toIntOrNull() ?: 3
                output = input.map { (it.code - s).toChar() }.joinToString("")
            }) { Text("解密") }
        })
    }
}

// ========== 摩斯密码 ==========

private val MORSE_MAP = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---",
    '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...",
    '8' to "---..", '9' to "----.", ' ' to "/"
)
private val MORSE_REV = MORSE_MAP.entries.associate { (k, v) -> v to k }

@Composable
private fun MorsePanel() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    CryptoIO(input, { input = it }, output, buttons = {
        Button(onClick = {
            output = input.uppercase().map { MORSE_MAP[it] ?: it.toString() }.joinToString(" ")
        }) { Text("编码") }
        Button(onClick = {
            output = input.split(" ").map { MORSE_REV[it]?.toString() ?: it }.joinToString("")
        }) { Text("解码") }
    })
}

// ========== AES ==========

@Composable
private fun AesPanel() {
    var input by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = password, onValueChange = { password = it },
            label = { Text("密钥") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        CryptoIO(input, { input = it }, output, buttons = {
            Button(onClick = { output = aesEncrypt(input, password) }) { Text("加密") }
            Button(onClick = { output = aesDecrypt(input, password) }) { Text("解密") }
        })
    }
}

private fun aesEncrypt(plain: String, pwd: String): String {
    try {
        val key = deriveKey(pwd, 256)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return JBase64.getEncoder().encodeToString(combined)
    } catch (_: Exception) { return "加密失败" }
}

private fun aesDecrypt(data: String, pwd: String): String {
    try {
        val combined = JBase64.getDecoder().decode(data)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val key = deriveKey(pwd, 256)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    } catch (_: Exception) { return "解密失败" }
}

private fun deriveKey(password: String, keySize: Int): SecretKeySpec {
    val salt = "CaesarTBSalt!!".toByteArray()
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt, 65536, keySize)
    return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
}

// ========== RSA ==========

@Composable
private fun RsaPanel() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var pubKey by remember { mutableStateOf("") }
    var priKey by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val kp = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.genKeyPair()
            pubKey = JBase64.getEncoder().encodeToString(kp.public.encoded)
            priKey = JBase64.getEncoder().encodeToString(kp.private.encoded)
        }, modifier = Modifier.fillMaxWidth()) { Text("生成 RSA 密钥对 (2048bit)") }
        OutlinedTextField(value = pubKey, onValueChange = { pubKey = it },
            label = { Text("公钥 (Base64)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        OutlinedTextField(value = priKey, onValueChange = { priKey = it },
            label = { Text("私钥 (Base64)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        CryptoIO(input, { input = it }, output, buttons = {
            Button(onClick = { output = rsaEncrypt(input, pubKey) }) { Text("公钥加密") }
            Button(onClick = { output = rsaDecrypt(input, priKey) }) { Text("私钥解密") }
        })
    }
}

private fun rsaEncrypt(plain: String, pubKeyB64: String): String {
    try {
        val bytes = JBase64.getDecoder().decode(pubKeyB64)
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(bytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return JBase64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    } catch (_: Exception) { return "加密失败" }
}

private fun rsaDecrypt(data: String, priKeyB64: String): String {
    try {
        val bytes = JBase64.getDecoder().decode(priKeyB64)
        val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes))
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(JBase64.getDecoder().decode(data)).toString(Charsets.UTF_8)
    } catch (_: Exception) { return "解密失败" }
}

// ========== ECC ==========

@Composable
private fun EccPanel() {
    var input by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var pubKey by remember { mutableStateOf("") }
    var priKey by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            val kp = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.genKeyPair()
            pubKey = JBase64.getEncoder().encodeToString(kp.public.encoded)
            priKey = JBase64.getEncoder().encodeToString(kp.private.encoded)
        }, modifier = Modifier.fillMaxWidth()) { Text("生成 ECC 密钥对 (secp256r1)") }
        OutlinedTextField(value = pubKey, onValueChange = { pubKey = it },
            label = { Text("公钥 (Base64)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        OutlinedTextField(value = priKey, onValueChange = { priKey = it },
            label = { Text("私钥 (Base64)") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
        CryptoIO(input, { input = it }, output, buttons = {
            Button(onClick = { output = eccSign(input, priKey) }) { Text("签名") }
            Button(onClick = { output = eccVerify(input, output, pubKey) }) { Text("验签") }
        })
    }
}

private fun eccSign(data: String, priKeyB64: String): String {
    try {
        val bytes = JBase64.getDecoder().decode(priKeyB64)
        val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(bytes))
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(key); sig.update(data.toByteArray())
        return JBase64.getEncoder().encodeToString(sig.sign())
    } catch (_: Exception) { return "签名失败" }
}

private fun eccVerify(data: String, sigB64: String, pubKeyB64: String): String {
    try {
        val keyBytes = JBase64.getDecoder().decode(pubKeyB64)
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(key); sig.update(data.toByteArray())
        return if (sig.verify(JBase64.getDecoder().decode(sigB64))) "✅ 签名有效" else "❌ 签名无效"
    } catch (_: Exception) { return "验签失败" }
}
