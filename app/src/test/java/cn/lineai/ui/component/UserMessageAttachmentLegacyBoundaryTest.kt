package cn.lineai.ui.component

import cn.lineai.model.InputAttachment
import org.junit.Assert.assertEquals
import org.junit.Test

class UserMessageAttachmentLegacyBoundaryTest {
    @Test
    fun javaBoundaryPreservesNamesAndOrder() {
        val snapshot = UserMessageAttachmentList.snapshotOf(
            listOf(
                InputAttachment("first.zip", "/first", "local"),
                InputAttachment("second.md", "/second", "ssh")
            )
        )

        assertEquals(
            listOf("first.zip", "second.md"),
            snapshot.items.map { it.name }
        )
    }
}
