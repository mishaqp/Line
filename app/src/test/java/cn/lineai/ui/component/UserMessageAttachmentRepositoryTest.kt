package cn.lineai.ui.component

import cn.lineai.model.InputAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserMessageAttachmentRepositoryTest {
    @Test
    fun repositoryMapsLegacyAttachmentsWithoutKeepingList() {
        val source = mutableListOf(
            InputAttachment("a.txt", "/a", "local"),
            InputAttachment("b.txt", "/b", "ssh")
        )
        val repository = UserMessageAttachmentRepository()

        repository.replaceAll(source)
        source.clear()

        assertEquals(
            listOf("a.txt", "b.txt"),
            repository.snapshot().items.map { it.name }
        )
    }

    @Test
    fun nullClearsRepository() {
        val repository = UserMessageAttachmentRepository()
        repository.replaceAll(
            listOf(InputAttachment("a.txt", "/a", "local"))
        )

        repository.replaceAll(null)

        assertTrue(repository.snapshot().items.isEmpty())
    }
}
