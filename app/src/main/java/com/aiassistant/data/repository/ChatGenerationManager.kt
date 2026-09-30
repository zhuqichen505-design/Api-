package com.aiassistant.data.repository

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 全局会话生成状态管理器（解决返回首页/其他界面时思考气泡与回复中断或短暂消失的问题）
 * 1. 在 Application 生命周期内持有正在进行的生成流与思考流状态；
 * 2. 无论用户在聊天页、首页、设置页或角色扮演工作室之间如何切换导航，生成任务与流式数据都不会中断；
 * 3. 当用户重新进入聊天界面时，能够无缝挂载现存的生成状态与累计的思考/正文内容，绝不闪烁或空白；
 * 4. 内置原子锁机制，彻底杜绝多次保存或重复报错输出。
 */
object ChatGenerationManager {

    class ActiveSession(
        val conversationId: Long,
        initialModelName: String = "",
        val requestStartTime: Long = System.currentTimeMillis()
    ) {
        val _isGenerating = MutableStateFlow(true)
        val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

        val _currentResponse = MutableStateFlow("")
        val currentResponse: StateFlow<String> = _currentResponse.asStateFlow()

        val _currentThinking = MutableStateFlow("")
        val currentThinking: StateFlow<String> = _currentThinking.asStateFlow()

        val _reconnectStatus = MutableStateFlow<String?>(null)
        val reconnectStatus: StateFlow<String?> = _reconnectStatus.asStateFlow()

        val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error.asStateFlow()

        val _isConnecting = MutableStateFlow(true)
        val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

        val _callingModel = MutableStateFlow(initialModelName)
        val callingModel: StateFlow<String> = _callingModel.asStateFlow()

        val isMessageSaved = AtomicBoolean(false)
        var generationJob: Job? = null
        var assistantVariantGroupId: String? = null
        var assistantVariantIndex: Int = 1
        var userMessageId: Long? = null

        /**
         * 生成锚点（v2.6.5）：触发本轮生成的用户消息 id 及其 user 分组 id。
         * 流式回复气泡钉在该用户消息之后挂载，位置不随同一位置其他回复（如错误占位、
         * 旧 variant）的删除而移动；重进会话时可由此恢复挂载点。
         */
        var anchorUserMessageId: Long? = null
        var anchorUserGroupId: String? = null

        fun appendResponse(token: String) {
            _isConnecting.value = false
            _currentResponse.update { it + token }
        }

        fun appendThinking(token: String) {
            _isConnecting.value = false
            _currentThinking.update { it + token }
        }

        fun resetBuffer() {
            _currentResponse.value = ""
            _currentThinking.value = ""
        }

        fun setStatus(status: String?) {
            _reconnectStatus.value = status
        }

        fun setError(err: String?) {
            _error.value = err
            _isGenerating.value = false
            _isConnecting.value = false
        }

        fun markFinished() {
            _isGenerating.value = false
            _isConnecting.value = false
            _reconnectStatus.value = null
        }
    }

    private val sessions = ConcurrentHashMap<Long, ActiveSession>()

    fun getSession(conversationId: Long): ActiveSession? {
        return sessions[conversationId]
    }

    fun isGenerating(conversationId: Long): Boolean {
        return sessions[conversationId]?.isGenerating?.value == true
    }

    fun startSession(
        conversationId: Long,
        modelName: String,
        variantGroupId: String? = null,
        variantIndex: Int = 1
    ): ActiveSession {
        // 如果先前已有正在运行的旧 Session，先取消其 Job，防止两个生成任务并存冲突
        sessions[conversationId]?.generationJob?.cancel()

        val newSession = ActiveSession(
            conversationId = conversationId,
            initialModelName = modelName
        ).apply {
            assistantVariantGroupId = variantGroupId
            assistantVariantIndex = variantIndex
        }
        sessions[conversationId] = newSession
        return newSession
    }

    fun removeSession(conversationId: Long) {
        sessions.remove(conversationId)
    }

    /**
     * 原子标记消息已持久化保存，保证报错回复或正常回复在任何情况下仅保存一次
     */
    fun tryMarkMessageSaved(conversationId: Long): Boolean {
        val session = sessions[conversationId] ?: return false
        return session.isMessageSaved.compareAndSet(false, true)
    }
}
