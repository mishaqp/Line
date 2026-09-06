package cn.lineai.ui.model

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExtensionsViewModelTest {

    @Test
    fun loadsRepositorySnapshotWithoutExposingBackendConfig() {
        val repository = FakeRepository(
            snapshotValue = snapshot(item("one", "First", "agent-one", 3, true))
        )

        val state = AgentExtensionsViewModel(repository).state.value

        assertEquals(1, state.items.size)
        assertEquals("one", state.items.single().id)
        assertEquals("agent-one", state.items.single().slug)
        assertEquals(3, state.items.single().toolCount)
        assertTrue(state.items.single().enabled)
        assertFalse(state.operationFailed)
    }

    @Test
    fun backAddAndModifyProduceTypedOneShotEffects() {
        val viewModel = AgentExtensionsViewModel(
            FakeRepository(snapshot(item("one", enabled = true)))
        )

        assertEquals(
            AgentExtensionsUiEffect.Back,
            viewModel.onAction(AgentExtensionsUiAction.Back)
        )
        assertEquals(
            AgentExtensionsUiEffect.Navigate(LineDestination.AgentEdit(null)),
            viewModel.onAction(AgentExtensionsUiAction.Add)
        )

        viewModel.onAction(AgentExtensionsUiAction.RequestActions("one"))
        assertEquals(
            AgentExtensionsUiEffect.Navigate(LineDestination.AgentEdit("one")),
            viewModel.onAction(AgentExtensionsUiAction.Modify("one"))
        )
        assertNull(viewModel.state.value.sheet)
        assertNull(viewModel.onAction(AgentExtensionsUiAction.Modify("missing")))
    }

    @Test
    fun deleteRequiresTwoStepsAndCannotRunTwice() {
        val repository = FakeRepository(snapshot(item("one", name = "First", enabled = true)))
        repository.onDelete = { id ->
            repository.snapshotValue = AgentExtensionsSnapshot(
                repository.snapshotValue.items.filterNot { it.id == id }
            )
        }
        val viewModel = AgentExtensionsViewModel(repository)

        viewModel.onAction(AgentExtensionsUiAction.ConfirmDelete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(AgentExtensionsUiAction.RequestActions("one"))
        assertTrue(viewModel.state.value.sheet is AgentExtensionsSheet.Actions)
        viewModel.onAction(AgentExtensionsUiAction.RequestDelete("one"))
        assertTrue(viewModel.state.value.sheet is AgentExtensionsSheet.Delete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(AgentExtensionsUiAction.ConfirmDelete)
        viewModel.onAction(AgentExtensionsUiAction.ConfirmDelete)

        assertEquals(1, repository.deleteCalls)
        assertEquals("one", repository.lastDeletedId)
        assertTrue(viewModel.state.value.items.isEmpty())
        assertNull(viewModel.state.value.sheet)
    }

    @Test
    fun dismissingActionOrDeleteSheetNeverMutatesRepository() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        val viewModel = AgentExtensionsViewModel(repository)

        viewModel.onAction(AgentExtensionsUiAction.RequestActions("one"))
        viewModel.onAction(AgentExtensionsUiAction.DismissSheet)
        viewModel.onAction(AgentExtensionsUiAction.RequestDelete("one"))
        viewModel.onAction(AgentExtensionsUiAction.DismissSheet)

        assertNull(viewModel.state.value.sheet)
        assertEquals(0, repository.deleteCalls)
        assertEquals(0, repository.setEnabledCalls)
    }

    @Test
    fun toggleCallsRepositoryOnceAndUsesPersistedSnapshot() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        repository.onSetEnabled = { id, enabled ->
            repository.snapshotValue = AgentExtensionsSnapshot(
                repository.snapshotValue.items.map {
                    if (it.id == id) it.copy(enabled = enabled) else it
                }
            )
        }
        val viewModel = AgentExtensionsViewModel(repository)

        viewModel.onAction(AgentExtensionsUiAction.SetEnabled("one", false))

        assertEquals(1, repository.setEnabledCalls)
        assertEquals("one" to false, repository.lastEnabledCall)
        assertFalse(viewModel.state.value.items.single().enabled)
        assertFalse(viewModel.state.value.operationInProgress)
    }

    @Test
    fun reloadClearsStaleSheetAndPerformsNoMutation() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        val viewModel = AgentExtensionsViewModel(repository)
        viewModel.onAction(AgentExtensionsUiAction.RequestActions("one"))
        repository.snapshotValue = snapshot(item("two", enabled = false))

        viewModel.onAction(AgentExtensionsUiAction.Reload)

        assertEquals(listOf("two"), viewModel.state.value.items.map { it.id })
        assertNull(viewModel.state.value.sheet)
        assertEquals(0, repository.deleteCalls)
        assertEquals(0, repository.setEnabledCalls)
    }

    @Test
    fun failedDeleteKeepsConfirmationAndReportsFailure() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        repository.failDelete = true
        val viewModel = AgentExtensionsViewModel(repository)
        viewModel.onAction(AgentExtensionsUiAction.RequestDelete("one"))

        viewModel.onAction(AgentExtensionsUiAction.ConfirmDelete)

        assertEquals(1, repository.deleteCalls)
        assertTrue(viewModel.state.value.sheet is AgentExtensionsSheet.Delete)
        assertTrue(viewModel.state.value.operationFailed)
        assertFalse(viewModel.state.value.operationInProgress)
    }

    private class FakeRepository(
        var snapshotValue: AgentExtensionsSnapshot
    ) : AgentExtensionsRepository {
        var snapshotCalls = 0
        var setEnabledCalls = 0
        var deleteCalls = 0
        var lastEnabledCall: Pair<String, Boolean>? = null
        var lastDeletedId: String? = null
        var failDelete = false
        var onSetEnabled: ((String, Boolean) -> Unit)? = null
        var onDelete: ((String) -> Unit)? = null

        override fun snapshot(): AgentExtensionsSnapshot {
            snapshotCalls++
            return snapshotValue
        }

        override fun setEnabled(extensionId: String, enabled: Boolean) {
            setEnabledCalls++
            lastEnabledCall = extensionId to enabled
            onSetEnabled?.invoke(extensionId, enabled)
        }

        override fun delete(extensionId: String) {
            deleteCalls++
            lastDeletedId = extensionId
            if (failDelete) error("delete failed")
            onDelete?.invoke(extensionId)
        }
    }

    companion object {
        private fun snapshot(vararg items: AgentExtensionListItem) =
            AgentExtensionsSnapshot(items.toList())

        private fun item(
            id: String,
            name: String = "Agent",
            slug: String = "agent-slug",
            toolCount: Int = 1,
            enabled: Boolean
        ) = AgentExtensionListItem(id, name, slug, toolCount, enabled)
    }
}
