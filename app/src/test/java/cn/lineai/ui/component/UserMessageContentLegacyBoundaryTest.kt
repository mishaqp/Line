package cn.lineai.ui.component

import cn.lineai.model.ChatMessage
import cn.lineai.model.InputAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserMessageContentLegacyBoundaryTest {
    @Test
    fun javaBoundaryPreservesAttachmentOnlyRule() {
        val attachment =
            InputAttachment("file.txt", "/file", "local")
        val message = ChatMessage(
            "1",
            ChatMessage.Role.USER,
            "Attached files",
            false,
            listOf(attachment)
        )

        val snapshot = UserMessageContentView.snapshotOf(
            message,
            "Attached files",
            210f
        )

        assertTrue(snapshot.content.isEmpty())
        assertEquals(210f, snapshot.maxWidthDp)
    }
}
