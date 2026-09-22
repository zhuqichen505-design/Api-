package com.aiassistant.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * 时间线梳理草稿模型（支持断点续梳与实时持久化）
 */
data class TimelineReconcileDraft(
    val conversationId: Long,
    val lastProcessedMessageId: Long = 0L,
    val currentStoryTime: String = "未确定",
    val events: List<TimelineEventItem> = emptyList(),
    val atemporalSettings: List<AtemporalSettingItem> = emptyList(),
    val isCompleted: Boolean = false,
    val totalChunks: Int = 1,
    val processedChunks: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 时间线梳理草稿管理器（单例，负责磁盘文件的读写与缓存管理）
 */
object TimelineDraftManager {
    private const val TAG = "TimelineDraftManager"
    private const val DRAFT_DIR_NAME = "timeline_drafts"

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private fun getDraftDir(context: Context): File {
        val dir = File(context.filesDir, DRAFT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getDraftFile(context: Context, conversationId: Long): File {
        return File(getDraftDir(context), "draft_${conversationId}.json")
    }

    /**
     * 保存梳理草稿
     */
    fun saveDraft(context: Context, draft: TimelineReconcileDraft) {
        try {
            val file = getDraftFile(context, draft.conversationId)
            val json = gson.toJson(draft)
            file.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "保存时间线梳理草稿失败: ${e.message}", e)
        }
    }

    /**
     * 读取指定会话的梳理草稿
     */
    fun getDraft(context: Context, conversationId: Long): TimelineReconcileDraft? {
        try {
            val file = getDraftFile(context, conversationId)
            if (!file.exists()) return null
            val json = file.readText(Charsets.UTF_8)
            if (json.isBlank()) return null
            return gson.fromJson(json, TimelineReconcileDraft::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "读取时间线梳理草稿失败: ${e.message}")
            return null
        }
    }

    /**
     * 清理指定会话的草稿
     */
    fun clearDraft(context: Context, conversationId: Long) {
        try {
            val file = getDraftFile(context, conversationId)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "删除时间线梳理草稿失败: ${e.message}")
        }
    }

    /**
     * 是否存在可用草稿
     */
    fun hasDraft(context: Context, conversationId: Long): Boolean {
        val file = getDraftFile(context, conversationId)
        return file.exists() && file.length() > 0
    }
}
