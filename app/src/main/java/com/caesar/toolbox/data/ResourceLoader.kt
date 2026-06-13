package com.caesar.toolbox.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*

/**
 * 资源加载器 — 优先从热更新目录加载
 * 支持：图片 / 文本 / 二进制 / 布局XML
 */
object ResourceLoader {

    /** 读取文本资源 */
    fun readText(ctx: Context, path: String, fallback: () -> String): String {
        val f = hotFile(ctx, path)
        return if (f.exists()) f.readText() else fallback()
    }

    /** 读取 Bitmap */
    fun loadBitmap(ctx: Context, path: String, fallback: () -> Bitmap?): Bitmap? {
        val f = hotFile(ctx, path)
        return if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else fallback()
    }

    /** 读取原始字节 */
    fun readBytes(ctx: Context, path: String): ByteArray? {
        val f = hotFile(ctx, path)
        return if (f.exists()) f.readBytes() else null
    }

    /** 获取热更新文件路径 */
    fun hotFile(ctx: Context, relPath: String): File {
        return File(ResourceUpdater.getResDir(ctx), relPath)
    }

    /** 检查热更新文件是否存在 */
    fun exists(ctx: Context, relPath: String): Boolean = hotFile(ctx, relPath).exists()

    /** 列出热更新目录下所有文件 */
    fun listAll(ctx: Context): List<String> {
        val dir = ResourceUpdater.getResDir(ctx)
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).path }.toList()
    }
}
