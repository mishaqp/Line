package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextCompactViewModelTest {
    private class FakeRepository : ContextCompactRepository {
        override fun snapshot() = ContextCompactSnapshot("Compacting")
        override fun normalizeStatus(status: String?) = when (status) {
            "done" -> ContextCompactStatus.DONE
            "error" -> ContextCompactStatus.ERROR
            else -> ContextCompactStatus.RUNNING
        }
    }

    @Test
    fun initialStateComesFromRepository() {
        val state = ContextCompactViewModel(FakeRepository()).state.value

        assertEquals("Compacting", state.label)
        assertEquals(ContextCompactStatus.RUNNING, state.status)
    }

    @Test
    fun bindUpdatesOnlyTheStatus() {
        val viewModel = ContextCompactViewModel(FakeRepository())

        viewModel.onAction(ContextCompactUiAction.Bind("done"))
        assertEquals(ContextCompactStatus.DONE, viewModel.state.value.status)
        assertEquals("Compacting", viewModel.state.value.label)

        viewModel.onAction(ContextCompactUiAction.Bind("error"))
        assertEquals(ContextCompactStatus.ERROR, viewModel.state.value.status)
    }
}
