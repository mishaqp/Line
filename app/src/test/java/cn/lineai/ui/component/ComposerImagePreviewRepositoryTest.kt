package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerImagePreviewRepositoryTest {
    @Test
    fun repeatedShowAdvancesRevisionEvenForTheSameUri() {
        val repository = ComposerImagePreviewRepository()

        repository.show("content://image/1", "encoded", "image/png", "preview.png")
        val first = repository.snapshot()
        repository.show("content://image/1", "encoded", "image/png", "preview.png")
        val second = repository.snapshot()

        assertTrue(second.revision > first.revision)
        assertTrue(second.visible)
        assertEquals("content://image/1", second.uri)
    }

    @Test
    fun clearRemovesPayloadAndKeepsMonotonicRevision() {
        val repository = ComposerImagePreviewRepository()
        repository.show("content://image/1", "encoded", "image/png", "preview.png")
        val shownRevision = repository.snapshot().revision

        repository.clear()
        val cleared = repository.snapshot()

        assertFalse(cleared.visible)
        assertFalse(cleared.base64.isNotEmpty())
        assertEquals(null, cleared.uri)
        assertTrue(cleared.revision > shownRevision)
    }
}
