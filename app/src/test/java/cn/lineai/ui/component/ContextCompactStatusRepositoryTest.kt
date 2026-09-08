package cn.lineai.ui.component

import cn.lineai.model.ChatMessage
import cn.lineai.ui.model.ContextCompactStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ContextCompactStatusRepositoryTest {
    @Test
    fun snapshotSanitizesLabelAndRemainsStable() {
        val repository = ContextCompactStatusRepository(null)

        assertEquals("", repository.snapshot().label)
        assertSame(repository.snapshot(), repository.snapshot())
    }

    @Test
    fun legacyStatusesMapExactlyToFoundationState() {
        val repository = ContextCompactStatusRepository("Compacting")

        assertEquals(
            ContextCompactStatus.RUNNING,
            repository.normalizeStatus(null)
        )
        assertEquals(
            ContextCompactStatus.RUNNING,
            repository.normalizeStatus("")
        )
        assertEquals(
            ContextCompactStatus.DONE,
            repository.normalizeStatus(ChatMessage.COMPACT_STATUS_DONE)
        )
        assertEquals(
            ContextCompactStatus.ERROR,
            repository.normalizeStatus(ChatMessage.COMPACT_STATUS_ERROR)
        )
    }
}
