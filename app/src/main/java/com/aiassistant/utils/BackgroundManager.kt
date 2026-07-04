package com.aiassistant.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.roundToInt

object BackgroundManager {
    private const val HOME_BACKGROUND_FILE = "home_background.jpg"
    private const val CHAT_BACKGROUND_FILE = "chat_background.jpg"
    private const val MAX_BACKGROUND_EDGE = 1800

    fun saveHomeBackgroundFromUri(context: Context, uri: Uri): Boolean {
        return saveBackgroundFromUri(context, uri, HOME_BACKGROUND_FILE)
    }

    fun saveChatBackgroundFromUri(context: Context, uri: Uri): Boolean {
        return saveBackgroundFromUri(context, uri, CHAT_BACKGROUND_FILE)
    }

    fun getHomeBackgroundBitmap(context: Context): Bitmap? {
        return getBackgroundBitmap(context, HOME_BACKGROUND_FILE)
    }

    fun getChatBackgroundBitmap(context: Context): Bitmap? {
        return getBackgroundBitmap(context, CHAT_BACKGROUND_FILE)
    }

    fun hasHomeBackground(context: Context): Boolean {
        return hasBackground(context, HOME_BACKGROUND_FILE)
    }

    fun hasChatBackground(context: Context): Boolean {
        return hasBackground(context, CHAT_BACKGROUND_FILE)
    }

    fun deleteHomeBackground(context: Context) {
        deleteBackground(context, HOME_BACKGROUND_FILE)
    }

    fun deleteChatBackground(context: Context) {
        deleteBackground(context, CHAT_BACKGROUND_FILE)
    }

    private fun saveBackgroundFromUri(context: Context, uri: Uri, fileName: String): Boolean {
        return try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return false

            val resized = resizeToMaxEdge(bitmap, MAX_BACKGROUND_EDGE)
            val file = File(context.filesDir, fileName)
            file.outputStream().use { output ->
                resized.compress(Bitmap.CompressFormat.JPEG, 88, output)
            }

            if (resized != bitmap) resized.recycle()
            bitmap.recycle()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun resizeToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val edge = maxOf(width, height)
        if (edge <= maxEdge) return bitmap

        val scale = maxEdge.toFloat() / edge.toFloat()
        val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun getBackgroundBitmap(context: Context, fileName: String): Bitmap? {
        val file = File(context.filesDir, fileName)
        if (!file.exists() || file.length() <= 0L) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    private fun hasBackground(context: Context, fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists() && file.length() > 0
    }

    private fun deleteBackground(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) file.delete()
    }
}
