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
