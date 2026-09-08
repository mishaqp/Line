package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkingStatusViewModelTest {
    private val repository = object : WorkingStatusRepository {
        override fun snapshot() = WorkingStatusSnapshot("Working", "Thinking")
    }

    @Test
    fun labelsComeFromRepositoryAndBindChoosesVisibleLabel() {
        val viewModel = WorkingStatusViewModel(repository)

        assertEquals("Working", viewModel.state.value.displayLabel)
        viewModel.onAction(WorkingStatusUiAction.Bind(true))
        assertEquals("Thinking", viewModel.state.value.displayLabel)
    }

    @Test
    fun startAndStopAreIdempotent() {
        val viewModel = WorkingStatusViewModel(repository)

        viewModel.onAction(WorkingStatusUiAction.Start)
        viewModel.onAction(WorkingStatusUiAction.Start)
        assertTrue(viewModel.state.value.working)

        viewModel.onAction(WorkingStatusUiAction.Stop)
        viewModel.onAction(WorkingStatusUiAction.Stop)
        assertFalse(viewModel.state.value.working)
    }
}
