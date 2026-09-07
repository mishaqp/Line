package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerPendingQueueViewModelTest {
    private class FakeRepository(
        private val rows: MutableList<ComposerPendingQueueItem>
    ) : ComposerPendingQueueRepository {
        override fun snapshot() = ComposerPendingQueueSnapshot(rows.toList(), 0)

        override fun removeAt(index: Int): Boolean {
            val position = rows.indexOfFirst { it.index == index }
            if (position < 0) return false
            rows.removeAt(position)
            return true
        }
    }

    @Test
    fun initialAndRefreshStatesAreImmutableSnapshots() {
        val rows = mutableListOf(ComposerPendingQueueItem(0, "1. first"))
        val repository = FakeRepository(rows)
        val viewModel = ComposerPendingQueueViewModel(repository)

        assertTrue(viewModel.state.value.visible)
        assertEquals("1. first", viewModel.state.value.items.single().preview)

        rows += ComposerPendingQueueItem(1, "2. second")
        assertEquals(1, viewModel.state.value.items.size)

        viewModel.onAction(ComposerPendingQueueUiAction.Refresh)
        assertEquals(2, viewModel.state.value.items.size)
    }

    @Test
    fun successfulRemovalRefreshesAndEmitsVisibility() {
        val repository = FakeRepository(
            mutableListOf(ComposerPendingQueueItem(0, "1. only"))
        )
        val viewModel = ComposerPendingQueueViewModel(repository)

        val effect = viewModel.onAction(ComposerPendingQueueUiAction.Remove(0))

        assertEquals(ComposerPendingQueueUiEffect.QueueChanged(false), effect)
        assertFalse(viewModel.state.value.visible)
    }

    @Test
    fun invalidRemovalDoesNothing() {
        val viewModel = ComposerPendingQueueViewModel(FakeRepository(mutableListOf()))

        assertNull(viewModel.onAction(ComposerPendingQueueUiAction.Remove(7)))
        assertFalse(viewModel.state.value.visible)
    }
}
