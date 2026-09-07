package cn.lineai.ui.component

import cn.lineai.model.InputAttachment
import cn.lineai.ui.model.ComposerAttachmentId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerAttachmentRepositoryTest {
    @Test
    fun removalTargetsPathAndSourceInsteadOfStalePosition() {
        val repository = ComposerAttachmentRepository()
        repository.toggle(attachment("same", "/first", InputAttachment.SOURCE_LOCAL))
        repository.toggle(attachment("target", "/target", InputAttachment.SOURCE_SSH))

        assertTrue(
            repository.remove(
                ComposerAttachmentId("/target", InputAttachment.SOURCE_SSH)
            )
        )

        assertEquals(listOf("/first"), repository.pathsForSource(InputAttachment.SOURCE_LOCAL))
        assertTrue(repository.pathsForSource(InputAttachment.SOURCE_SSH).isEmpty())
    }

    @Test
    fun invalidAttachmentAndMissingIdentityDoNotMutateState() {
        val repository = ComposerAttachmentRepository()

        assertFalse(repository.toggle(null))
        assertFalse(repository.remove(ComposerAttachmentId("/missing", "local")))
        assertTrue(repository.isEmpty())
    }

    private fun attachment(name: String, path: String, source: String) =
        InputAttachment(name, path, source)
}
