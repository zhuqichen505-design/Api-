package com.aiassistant.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

object BackgroundImageManager {
    private const val HOME_BACKGROUND_FILE = "home_background.jpg"
    private const val CHAT_BACKGROUND_FILE = "chat_background.jpg"
    private const val MAX_BACKGROUND_SIDE = 2160

    data class SolidColorPreset(
        val id: String,
        val name: String,
        val lightColorInt: Int,
        val darkColorInt: Int,
        val displayHex: String
    )

    val PRESET_SOLID_COLORS = listOf(
        SolidColorPreset("pale_green", "浅艾绿", 0xFFF1F8F4.toInt(), 0xFF0C140F.toInt(), "#F1F8F4"),
        SolidColorPreset("pale_blue", "浅湖蓝", 0xFFF0F5FA.toInt(), 0xFF0C121A.toInt(), "#F0F5FA"),
        SolidColorPreset("pale_purple", "浅薰紫", 0xFFF6F3F9.toInt(), 0xFF130E18.toInt(), "#F6F3F9"),
        SolidColorPreset("pale_pink", "浅樱粉", 0xFFFAF2F4.toInt(), 0xFF160E11.toInt(), "#FAF2F4"),
        SolidColorPreset("pale_yellow", "浅暖杏", 0xFFFAF8F0.toInt(), 0xFF15140D.toInt(), "#FAF8F0"),
        SolidColorPreset("pale_cyan", "浅山岚", 0xFFF0F7F7.toInt(), 0xFF0C1414.toInt(), "#F0F7F7")
    )

    fun saveHomeBackgroundFromUri(context: Context, uri: Uri): Boolean {
        return saveBackgroundFromUri(context, uri, HOME_BACKGROUND_FILE)
    }

    fun saveChatBackgroundFromUri(context: Context, uri: Uri): Boolean {
        return saveBackgroundFromUri(context, uri, CHAT_BACKGROUND_FILE)
    }

    fun saveHomeBackgroundSolidColor(context: Context, colorInt: Int): Boolean {
        return saveSolidColor(context, HOME_BACKGROUND_FILE, colorInt)
    }

    fun saveChatBackgroundSolidColor(context: Context, colorInt: Int): Boolean {
        return saveSolidColor(context, CHAT_BACKGROUND_FILE, colorInt)
    }

    fun getHomeBackgroundBitmap(context: Context): Bitmap? {
        return getBackgroundBitmap(context, HOME_BACKGROUND_FILE)
    }

    fun getChatBackgroundBitmap(context: Context): Bitmap? {
        return getBackgroundBitmap(context, CHAT_BACKGROUND_FILE)
    }

    fun hasHomeBackground(context: Context): Boolean {
        return backgroundFile(context, HOME_BACKGROUND_FILE).length() > 0
    }

    fun hasChatBackground(context: Context): Boolean {
        return backgroundFile(context, CHAT_BACKGROUND_FILE).length() > 0
    }

    fun deleteHomeBackground(context: Context) {
        backgroundFile(context, HOME_BACKGROUND_FILE).delete()
    }

    fun deleteChatBackground(context: Context) {
        backgroundFile(context, CHAT_BACKGROUND_FILE).delete()
    }

    private fun saveSolidColor(context: Context, fileName: String, colorInt: Int): Boolean {
        return try {
            val bitmap = Bitmap.createBitmap(120, 240, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            canvas.drawColor(colorInt)
            backgroundFile(context, fileName).outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveBackgroundFromUri(context: Context, uri: Uri, fileName: String): Boolean {
        return try {
            val bitmap = decodeScaledBitmap(context, uri) ?: return false
            backgroundFile(context, fileName).outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
            }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun getBackgroundBitmap(context: Context, fileName: String): Bitmap? {
        val file = backgroundFile(context, fileName)
        if (!file.exists() || file.length() <= 0) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        var scaledWidth = width
        var scaledHeight = height
        while (scaledWidth / 2 >= MAX_BACKGROUND_SIDE || scaledHeight / 2 >= MAX_BACKGROUND_SIDE) {
            sampleSize *= 2
            scaledWidth /= 2
            scaledHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun backgroundFile(context: Context, fileName: String): File {
        return File(context.filesDir, fileName)
    }
}
