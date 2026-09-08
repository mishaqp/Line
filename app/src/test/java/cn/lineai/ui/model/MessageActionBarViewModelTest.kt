package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageActionBarViewModelTest {
    private class FakeRepository(
        private var snapshot: MessageActionBarSnapshot
    ) : MessageActionBarStateRepository {
        override fun snapshot() = snapshot

        override fun setExpanded(expanded: Boolean) {
            snapshot = snapshot.copy(expanded = expanded)
        }

        override fun setActionsAllowed(allowed: Boolean) {
            snapshot = snapshot.copy(
                actionsAllowed = allowed,
                expanded = if (allowed) snapshot.expanded else false
            )
        }
    }

    @Test
    fun expansionRevealsSecondaryActions() {
        val viewModel = viewModel(recall = true)

        assertTrue(viewModel.state.value.copyVisible)
        assertFalse(viewModel.state.value.secondaryVisible)

        viewModel.onAction(MessageActionBarUiAction.SetExpanded(true))

        assertTrue(viewModel.state.value.secondaryVisible)
        assertTrue(viewModel.state.value.recallEnabled)
        assertEquals(
            MessageActionBarUiEffect.Recall,
            viewModel.onAction(MessageActionBarUiAction.Recall)
        )
    }

    @Test
    fun disablingActionsCollapsesAndSuppressesEffects() {
        val viewModel = viewModel()
        viewModel.onAction(MessageActionBarUiAction.SetExpanded(true))

        viewModel.onAction(MessageActionBarUiAction.SetActionsAllowed(false))

        assertFalse(viewModel.state.value.actionsAllowed)
        assertFalse(viewModel.state.value.expanded)
        assertNull(viewModel.onAction(MessageActionBarUiAction.Copy))
        assertNull(viewModel.onAction(MessageActionBarUiAction.More))
    }

    @Test
    fun recallIsUnavailableForAssistantMessages() {
        val viewModel = viewModel(recall = false)
        viewModel.onAction(MessageActionBarUiAction.SetExpanded(true))

        assertNull(viewModel.onAction(MessageActionBarUiAction.Recall))
        assertEquals(
            MessageActionBarUiEffect.Copy,
            viewModel.onAction(MessageActionBarUiAction.Copy)
        )
    }

    private fun viewModel(recall: Boolean = false) = MessageActionBarViewModel(
        FakeRepository(
            MessageActionBarSnapshot(
                alignRight = false,
                recallEnabled = recall,
                actionsAllowed = true,
                expanded = false
            )
        )
    )
}
