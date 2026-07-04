package com.aiassistant.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object AvatarManager {
    private const val AVATAR_FILE = "user_avatar.dat"
    private const val MODEL_AVATAR_FILE = "model_avatar.dat"

    private fun apiModelAvatarFileName(apiConfigId: Long): String =
        "model_avatar_api_$apiConfigId.dat"

    // 保存头像（Base64编码）
    fun saveAvatar(context: Context, base64Data: String) {
        saveAvatar(context, AVATAR_FILE, base64Data)
    }

    // 从URI保存头像
    fun saveAvatarFromUri(context: Context, uri: Uri): Boolean {
        return saveAvatarFromUri(context, uri, AVATAR_FILE)
    }

    fun saveModelAvatarFromUri(context: Context, uri: Uri): Boolean {
        return saveAvatarFromUri(context, uri, MODEL_AVATAR_FILE)
    }

    fun saveApiModelAvatarFromUri(context: Context, apiConfigId: Long, uri: Uri): Boolean {
        if (apiConfigId <= 0L) return false
        return saveAvatarFromUri(context, uri, apiModelAvatarFileName(apiConfigId))
    }

    private fun saveAvatar(context: Context, fileName: String, base64Data: String) {
        val file = File(context.filesDir, fileName)
        file.writeText(base64Data)
    }

    private fun saveAvatarFromUri(context: Context, uri: Uri, fileName: String): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) return false

            // 压缩并转换为Base64
            val resized = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            saveAvatar(context, fileName, base64)

            if (resized != bitmap) resized.recycle()
            bitmap.recycle()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 获取头像（Base64编码）
    fun getAvatar(context: Context): String? {
        return getAvatar(context, AVATAR_FILE)
    }

    fun getModelAvatar(context: Context): String? {
        return getAvatar(context, MODEL_AVATAR_FILE)
    }

    fun getApiModelAvatar(context: Context, apiConfigId: Long): String? {
        if (apiConfigId <= 0L) return null
        return getAvatar(context, apiModelAvatarFileName(apiConfigId))
    }

    private fun getAvatar(context: Context, fileName: String): String? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            file.readText().ifBlank { null }
        } else null
    }

    // 获取头像Bitmap
    fun getAvatarBitmap(context: Context): Bitmap? {
        return getAvatarBitmap(context, AVATAR_FILE)
    }

    fun getModelAvatarBitmap(context: Context): Bitmap? {
        return getAvatarBitmap(context, MODEL_AVATAR_FILE)
    }

    fun getApiModelAvatarBitmap(context: Context, apiConfigId: Long?): Bitmap? {
        val id = apiConfigId ?: return null
        if (id <= 0L) return null
        return getAvatarBitmap(context, apiModelAvatarFileName(id))
    }

    fun getPreferredModelAvatarBitmap(context: Context, apiConfigId: Long?): Bitmap? {
        return getApiModelAvatarBitmap(context, apiConfigId) ?: getModelAvatarBitmap(context)
    }

    private fun getAvatarBitmap(context: Context, fileName: String): Bitmap? {
        val base64 = getAvatar(context, fileName) ?: return null
        return try {
            val byteArray = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            null
        }
    }

    // 删除头像
    fun deleteAvatar(context: Context) {
        deleteAvatar(context, AVATAR_FILE)
    }

    fun deleteModelAvatar(context: Context) {
        deleteAvatar(context, MODEL_AVATAR_FILE)
    }

    fun deleteApiModelAvatar(context: Context, apiConfigId: Long) {
        if (apiConfigId > 0L) deleteAvatar(context, apiModelAvatarFileName(apiConfigId))
    }

    private fun deleteAvatar(context: Context, fileName: String) {
        val file = File(context.filesDir, fileName)
        if (file.exists()) file.delete()
    }

    // 检查是否有自定义头像
    fun hasCustomAvatar(context: Context): Boolean {
        val file = File(context.filesDir, AVATAR_FILE)
        return file.exists() && file.length() > 0
    }

    fun hasCustomModelAvatar(context: Context): Boolean {
        val file = File(context.filesDir, MODEL_AVATAR_FILE)
        return file.exists() && file.length() > 0
    }

    fun hasCustomApiModelAvatar(context: Context, apiConfigId: Long): Boolean {
        if (apiConfigId <= 0L) return false
        val file = File(context.filesDir, apiModelAvatarFileName(apiConfigId))
        return file.exists() && file.length() > 0
    }
}
