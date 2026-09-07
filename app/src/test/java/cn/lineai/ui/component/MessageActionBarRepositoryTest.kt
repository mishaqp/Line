package cn.lineai.ui.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageActionBarRepositoryTest {
    @Test
    fun directionAndRecallCapabilityStayImmutable() {
        val repository = MessageActionBarRepository(
            alignRight = true,
            recallEnabled = true,
            actionsAllowed = true
        )

        repository.setExpanded(true)
        val snapshot = repository.snapshot()

        assertTrue(snapshot.alignRight)
        assertTrue(snapshot.recallEnabled)
        assertTrue(snapshot.expanded)
    }

    @Test
    fun hidingActionsAlsoClosesExpandedMenu() {
        val repository = MessageActionBarRepository(false, false, true)
        repository.setExpanded(true)

        repository.setActionsAllowed(false)
        val snapshot = repository.snapshot()

        assertFalse(snapshot.actionsAllowed)
        assertFalse(snapshot.expanded)
    }
}
