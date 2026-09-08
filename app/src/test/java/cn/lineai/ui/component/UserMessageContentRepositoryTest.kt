package cn.lineai.ui.component

import cn.lineai.model.ChatMessage
import cn.lineai.model.InputAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserMessageContentRepositoryTest {
    private val attachment =
        InputAttachment("file.txt", "/file", "local")

    @Test
    fun normalTextStaysVisible() {
        val repository =
            UserMessageContentRepository(
                "Attached files",
                240f
            )

        repository.bind(message("Hello"))
        val snapshot = repository.snapshot()

        assertEquals("Hello", snapshot.content)
        assertEquals(240f, snapshot.maxWidthDp)
    }

    @Test
    fun attachmentOnlyPlaceholderIsHidden() {
        val repository =
            UserMessageContentRepository(
                "Attached files",
                240f
            )

        repository.bind(
            message(
                "  Attached files  ",
                listOf(attachment)
            )
        )

        assertTrue(repository.snapshot().content.isEmpty())
    }

    @Test
    fun sameTextWithoutAttachmentRemainsVisible() {
        val repository =
            UserMessageContentRepository(
                "Attached files",
                240f
            )

        repository.bind(message("Attached files"))

        assertFalse(repository.snapshot().content.isEmpty())
    }

    private fun message(
        content: String,
        attachments: List<InputAttachment> =
            emptyList()
    ) = ChatMessage(
        "1",
        ChatMessage.Role.USER,
        content,
        false,
        attachments
    )
}
