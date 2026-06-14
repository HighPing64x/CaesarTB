plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.security.MessageDigest

android {
    namespace = "com.caesar.toolbox"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.caesar.toolbox"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.1.2"

        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // Compose BOM — 统一版本管理
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // 核心
    implementation("androidx.core:core-ktx:1.12.0")

    // ZXing 二维码
    implementation("com.google.zxing:core:3.5.3")

    // Coil 图片加载
    implementation("io.coil-kt:coil-compose:2.5.0")

    // 调试
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ===== 资源热更新 ZIP 生成任务 =====
tasks.register("packageResZip") {
    group = "caesar"
    description = "生成热更新资源包 resources.zip"
    doLast {
        val resDir = file("src/main/res")
        val assetsDir = file("src/main/assets")
        val outputDir = file("build/outputs/res_zip")
        outputDir.mkdirs()
        val zipFile = file("$outputDir/resources.zip")

        zipFile.outputStream().use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 打包 drawable 图片资源
                fileTree(resDir).matching { include("drawable*/**") }.forEach { f ->
                    if (f.isFile) {
                        val entry = ZipEntry("res/${f.relativeTo(resDir)}")
                        zos.putNextEntry(entry); f.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
                    }
                }
                // 打包 assets
                if (assetsDir.exists()) fileTree(assetsDir).forEach { f ->
                    if (f.isFile) {
                        val entry = ZipEntry("assets/${f.relativeTo(assetsDir)}")
                        zos.putNextEntry(entry); f.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
                    }
                }
            }
        }
        println("✅ 资源包: ${zipFile.absolutePath}")
        println("   大小: ${zipFile.length()} bytes")
    }
}

// assembleDebug 后自动打包资源
afterEvaluate { tasks.named("assembleDebug") { finalizedBy("packageResZip") } }

// Sync res_meta.json from version.json so resource package metadata stays up-to-date
tasks.register("syncResMeta") {
    group = "caesar"
    description = "从 version.json 生成 res_meta.json（包含资源包 versionCode 与下载 URL）"
    doLast {
        val root = project.rootDir
        val vfile = file("${root}/version.json")
        if (!vfile.exists()) {
            println("version.json not found, skipping syncResMeta")
            return@doLast
        }
        val text = vfile.readText()
        val nameMatch = Regex("\"versionName\"\\s*:\\s*\"([^\"]+)\"").find(text)
        val codeMatch = Regex("\"versionCode\"\\s*:\\s*(\\d+)").find(text)
        val vName = nameMatch?.groupValues?.get(1) ?: ""
        val vCode = codeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val downloadBase = "https://github.com/HighPing64x/CaesarTB/releases/download/v${vName}/resources.zip"
        val outText = "{" + System.lineSeparator() +
            "  \"resource\": {" + System.lineSeparator() +
            "    \"versionCode\": $vCode," + System.lineSeparator() +
            "    \"url\": \"$downloadBase\"" + System.lineSeparator() +
            "  }" + System.lineSeparator() +
            "}"
        val outFile = file("${root}/res_meta.json")
        outFile.writeText(outText)
        println("✅ res_meta.json updated: ${outFile.absolutePath}")
    }
}

afterEvaluate { tasks.named("packageResZip") { finalizedBy("syncResMeta") } }
