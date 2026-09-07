package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallPreviewViewModelTest {

    @Test
    fun initialSnapshotIsReadOnceAndKeepsOrderAndDuplicates() {
        val repository = RecordingRepository(
            ToolCallPreviewSnapshot(
                rows = listOf(
                    row("factory-0-main", "READ"),
                    row("factory-1-main", "SHELL"),
                    row("factory-1-running", "SHELL", running = true),
                    row("factory-2-main", "READ")
                ),
                registryAvailable = true
            )
        )
        val viewModel = ToolCallPreviewViewModel(repository)

        assertEquals(
            listOf("factory-0-main", "factory-1-main", "factory-1-running", "factory-2-main"),
            viewModel.state.value.rows.map { it.renderId }
        )
        assertEquals(
            listOf("READ", "SHELL", "SHELL", "READ"),
            viewModel.state.value.rows.map { it.categoryLabel }
        )
        assertEquals(4, viewModel.state.value.rows.map { it.renderId }.toSet().size)
        assertTrue(viewModel.state.value.registryAvailable)
        assertEquals(1, repository.snapshotCalls)
    }

    @Test
    fun emptyAndMissingRegistryDoNotCrash() {
        val viewModel = ToolCallPreviewViewModel(RecordingRepository())
        assertTrue(viewModel.state.value.rows.isEmpty())
        assertFalse(viewModel.state.value.registryAvailable)
    }

    @Test
    fun snapshotExceptionYieldsSafeState() {
        val viewModel = ToolCallPreviewViewModel(object : ToolCallPreviewRepository {
            override fun snapshot(): ToolCallPreviewSnapshot {
                throw IllegalStateException("boom")
            }
        })
        assertTrue(viewModel.state.value.rows.isEmpty())
        assertFalse(viewModel.state.value.registryAvailable)
        assertNull(viewModel.onAction(ToolCallPreviewUiAction.Reload))
        assertTrue(viewModel.state.value.rows.isEmpty())
    }

    @Test
    fun reloadReadsFreshSnapshotWithoutBack() {
        val repository = RecordingRepository(
            ToolCallPreviewSnapshot(listOf(row("a", "READ")), true)
        )
        val viewModel = ToolCallPreviewViewModel(repository)
        repository.snapshotValue = ToolCallPreviewSnapshot(
            listOf(row("a", "READ"), row("b", "WRITE")),
            true
        )

        assertNull(viewModel.onAction(ToolCallPreviewUiAction.Reload))
        assertEquals(listOf("a", "b"), viewModel.state.value.rows.map { it.renderId })
        assertEquals(2, repository.snapshotCalls)
    }

    @Test
    fun backIsOneShotAndReloadDoesNotReplayIt() {
        val viewModel = ToolCallPreviewViewModel(RecordingRepository())
        assertEquals(
            ToolCallPreviewUiEffect.Back,
            viewModel.onAction(ToolCallPreviewUiAction.Back)
        )
        assertNull(viewModel.onAction(ToolCallPreviewUiAction.Reload))
        assertEquals(
            ToolCallPreviewUiEffect.Back,
            viewModel.onAction(ToolCallPreviewUiAction.Back)
        )
    }

    @Test
    fun uiStateToStringOmitsViewsFactoriesAndSecrets() {
        val secret = "super-secret-token-xyz"
        val viewModel = ToolCallPreviewViewModel(
            RecordingRepository(
                ToolCallPreviewSnapshot(listOf(row("factory-0-main", "WRITE")), true)
            )
        )
        val text = viewModel.state.value.toString()
        assertFalse(text.contains(secret))
        assertFalse(text.contains("ToolCallViewFactory"))
        assertFalse(text.contains("ToolCallCardView"))
        assertFalse(text.contains("android.view.View"))
    }

    private class RecordingRepository(
        var snapshotValue: ToolCallPreviewSnapshot = ToolCallPreviewSnapshot()
    ) : ToolCallPreviewRepository {
        var snapshotCalls = 0

        override fun snapshot(): ToolCallPreviewSnapshot {
            snapshotCalls += 1
            return snapshotValue
        }
    }

    companion object {
        private fun row(
            renderId: String,
            categoryLabel: String,
            running: Boolean = false
        ): ToolCallPreviewRowUi = ToolCallPreviewRowUi(renderId, categoryLabel, running)
    }
}
