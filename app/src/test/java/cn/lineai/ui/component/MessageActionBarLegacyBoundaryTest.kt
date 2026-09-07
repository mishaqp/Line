package cn.lineai.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageActionBarLegacyBoundaryTest {
    @Test
    fun legacyConstructorArgumentsMapToFoundationSnapshot() {
        val user = MessageActionBarView.initialSnapshot(
            MessageActionBarView.ALIGN_RIGHT,
            true,
            false
        )
        assertTrue(user.alignRight)
        assertTrue(user.recallEnabled)
        assertTrue(user.actionsAllowed)

        val streaming = MessageActionBarView.initialSnapshot(
            MessageActionBarView.ALIGN_LEFT,
            false,
            true
        )
        assertFalse(streaming.alignRight)
        assertFalse(streaming.recallEnabled)
        assertFalse(streaming.actionsAllowed)
    }
}
