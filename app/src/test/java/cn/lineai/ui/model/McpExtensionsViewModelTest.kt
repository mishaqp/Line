package cn.lineai.ui.model

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpExtensionsViewModelTest {

    @Test
    fun loadsRepositorySnapshotWithoutExposingBackendConfig() {
        val repository = FakeRepository(
            snapshotValue = snapshot(item("one", "First", "https://mcp.example", 3, true))
        )

        val state = McpExtensionsViewModel(repository).state.value

        assertEquals(1, state.items.size)
        assertEquals("one", state.items.single().id)
        assertEquals(3, state.items.single().toolCount)
        assertTrue(state.items.single().enabled)
        assertFalse(state.operationFailed)
    }

    @Test
    fun backAddAndModifyProduceTypedOneShotEffects() {
        val viewModel = McpExtensionsViewModel(
            FakeRepository(snapshot(item("one", enabled = true)))
        )

        assertEquals(
            McpExtensionsUiEffect.Back,
            viewModel.onAction(McpExtensionsUiAction.Back)
        )
        assertEquals(
            McpExtensionsUiEffect.Navigate(LineDestination.McpEdit(null)),
            viewModel.onAction(McpExtensionsUiAction.Add)
        )

        viewModel.onAction(McpExtensionsUiAction.RequestActions("one"))
        assertEquals(
            McpExtensionsUiEffect.Navigate(LineDestination.McpEdit("one")),
            viewModel.onAction(McpExtensionsUiAction.Modify("one"))
        )
        assertNull(viewModel.state.value.sheet)
        assertNull(viewModel.onAction(McpExtensionsUiAction.Modify("missing")))
    }

    @Test
    fun deleteRequiresTwoStepsAndCannotRunTwice() {
        val repository = FakeRepository(snapshot(item("one", name = "First", enabled = true)))
        repository.onDelete = { id ->
            repository.snapshotValue = McpExtensionsSnapshot(
                repository.snapshotValue.items.filterNot { it.id == id }
            )
        }
        val viewModel = McpExtensionsViewModel(repository)

        viewModel.onAction(McpExtensionsUiAction.ConfirmDelete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(McpExtensionsUiAction.RequestActions("one"))
        assertTrue(viewModel.state.value.sheet is McpExtensionsSheet.Actions)
        viewModel.onAction(McpExtensionsUiAction.RequestDelete("one"))
        assertTrue(viewModel.state.value.sheet is McpExtensionsSheet.Delete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(McpExtensionsUiAction.ConfirmDelete)
        viewModel.onAction(McpExtensionsUiAction.ConfirmDelete)

        assertEquals(1, repository.deleteCalls)
        assertEquals("one", repository.lastDeletedId)
        assertTrue(viewModel.state.value.items.isEmpty())
        assertNull(viewModel.state.value.sheet)
    }

    @Test
    fun dismissingActionOrDeleteSheetNeverMutatesRepository() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        val viewModel = McpExtensionsViewModel(repository)

        viewModel.onAction(McpExtensionsUiAction.RequestActions("one"))
        viewModel.onAction(McpExtensionsUiAction.DismissSheet)
        viewModel.onAction(McpExtensionsUiAction.RequestDelete("one"))
        viewModel.onAction(McpExtensionsUiAction.DismissSheet)

        assertNull(viewModel.state.value.sheet)
        assertEquals(0, repository.deleteCalls)
        assertEquals(0, repository.setEnabledCalls)
    }

    @Test
    fun toggleCallsRepositoryOnceAndUsesPersistedSnapshot() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        repository.onSetEnabled = { id, enabled ->
            repository.snapshotValue = McpExtensionsSnapshot(
                repository.snapshotValue.items.map {
                    if (it.id == id) it.copy(enabled = enabled) else it
                }
            )
        }
        val viewModel = McpExtensionsViewModel(repository)

        viewModel.onAction(McpExtensionsUiAction.SetEnabled("one", false))

        assertEquals(1, repository.setEnabledCalls)
        assertEquals("one" to false, repository.lastEnabledCall)
        assertFalse(viewModel.state.value.items.single().enabled)
        assertFalse(viewModel.state.value.operationInProgress)
    }

    @Test
    fun reloadClearsStaleSheetAndPerformsNoMutation() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        val viewModel = McpExtensionsViewModel(repository)
        viewModel.onAction(McpExtensionsUiAction.RequestActions("one"))
        repository.snapshotValue = snapshot(item("two", enabled = false))

        viewModel.onAction(McpExtensionsUiAction.Reload)

        assertEquals(listOf("two"), viewModel.state.value.items.map { it.id })
        assertNull(viewModel.state.value.sheet)
        assertEquals(0, repository.deleteCalls)
        assertEquals(0, repository.setEnabledCalls)
    }

    @Test
    fun failedDeleteKeepsConfirmationAndReportsFailure() {
        val repository = FakeRepository(snapshot(item("one", enabled = true)))
        repository.failDelete = true
        val viewModel = McpExtensionsViewModel(repository)
        viewModel.onAction(McpExtensionsUiAction.RequestDelete("one"))

        viewModel.onAction(McpExtensionsUiAction.ConfirmDelete)

        assertEquals(1, repository.deleteCalls)
        assertTrue(viewModel.state.value.sheet is McpExtensionsSheet.Delete)
        assertTrue(viewModel.state.value.operationFailed)
        assertFalse(viewModel.state.value.operationInProgress)
    }

    private class FakeRepository(
        var snapshotValue: McpExtensionsSnapshot
    ) : McpExtensionsRepository {
        var snapshotCalls = 0
        var setEnabledCalls = 0
        var deleteCalls = 0
        var lastEnabledCall: Pair<String, Boolean>? = null
        var lastDeletedId: String? = null
        var failDelete = false
        var onSetEnabled: ((String, Boolean) -> Unit)? = null
        var onDelete: ((String) -> Unit)? = null

        override fun snapshot(): McpExtensionsSnapshot {
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
        private fun snapshot(vararg items: McpExtensionListItem) =
            McpExtensionsSnapshot(items.toList())

        private fun item(
            id: String,
            name: String = "MCP",
            url: String = "https://example.test",
            toolCount: Int = 1,
            enabled: Boolean
        ) = McpExtensionListItem(id, name, url, toolCount, enabled)
    }
}
