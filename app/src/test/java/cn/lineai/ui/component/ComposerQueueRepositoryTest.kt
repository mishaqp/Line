package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerQueueRepositoryTest {
    @Test
    fun snapshotPreservesLegacyPreviewAndOverflowRules() {
        val queue = ComposerQueue()
        repeat(ComposerQueue.MAX_VISIBLE_ROWS + 2) { index ->
            queue.add("message $index", null)
        }
        val repository = ComposerQueueRepository(queue)

        val snapshot = repository.snapshot()

        assertEquals(ComposerQueue.MAX_VISIBLE_ROWS, snapshot.items.size)
        assertEquals("1. message 0", snapshot.items.first().preview)
        assertEquals(2, snapshot.overflowCount)
    }

    @Test
    fun removalCrossesBoundaryExactlyOnce() {
        val queue = ComposerQueue()
        queue.add("first", null)
        queue.add("second", null)
        val repository = ComposerQueueRepository(queue)

        assertTrue(repository.removeAt(0))
        assertEquals("1. second", repository.snapshot().items.single().preview)
        assertFalse(repository.removeAt(3))
    }
}
