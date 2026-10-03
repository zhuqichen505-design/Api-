package com.aiassistant

import com.aiassistant.data.repository.ChatGenerationManager
import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Test

class GenerationLifecycleTest {
    @Test fun navigationReattachesSameBuffersAndCancellationPreventsLateSave() {
        val id = 908070L
        val session = ChatGenerationManager.startSession(id, "model")
        session.generationJob = Job()
        session.appendThinking("reasoning")
        session.appendResponse("partial")
        session.anchorUserMessageId = 123
        val reattached = ChatGenerationManager.getSession(id)!!
        assertSame(session, reattached)
        session.appendResponse(" completed")
        assertEquals("partial completed", reattached.currentResponse.value)
        assertEquals("reasoning", reattached.currentThinking.value)
        assertEquals(123L, reattached.anchorUserMessageId)
        ChatGenerationManager.cancelSession(id)
        assertTrue(session.generationJob!!.isCancelled)
        assertFalse(session.isGenerating.value)
        assertFalse(session.isMessageSaved.compareAndSet(false, true))
        assertNull(ChatGenerationManager.getSession(id))
    }

    @Test fun oldCompletionCannotRemoveNewGeneration() {
        val id = 908071L
        val old = ChatGenerationManager.startSession(id, "old")
        val current = ChatGenerationManager.startSession(id, "new")
        ChatGenerationManager.removeSession(id, old)
        assertSame(current, ChatGenerationManager.getSession(id))
        assertTrue(ChatGenerationManager.tryMarkMessageSaved(id))
        assertFalse(ChatGenerationManager.tryMarkMessageSaved(id))
        ChatGenerationManager.removeSession(id, current)
    }
}
