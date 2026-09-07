package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerAttachmentStripViewModelTest {
    private class FakeRepository(
        private val rows: MutableList<ComposerAttachmentItem>
    ) : ComposerAttachmentStateRepository {
        override fun snapshot() = ComposerAttachmentSnapshot(rows.toList())

        override fun removeAt(index: Int): Boolean {
            val position = rows.indexOfFirst { it.index == index }
            if (position < 0) return false
            rows.removeAt(position)
            return true
        }
    }

    @Test
    fun initialAndRefreshStatesAreImmutableSnapshots() {
        val rows = mutableListOf(ComposerAttachmentItem(0, "one.txt"))
        val viewModel = ComposerAttachmentStripViewModel(FakeRepository(rows))

        assertTrue(viewModel.state.value.visible)
        assertEquals("one.txt", viewModel.state.value.items.single().name)

        rows += ComposerAttachmentItem(1, "two.txt")
        assertEquals(1, viewModel.state.value.items.size)

        viewModel.onAction(ComposerAttachmentUiAction.Refresh)
        assertEquals(2, viewModel.state.value.items.size)
    }

    @Test
    fun successfulRemovalRefreshesAndEmitsVisibility() {
        val viewModel = ComposerAttachmentStripViewModel(
            FakeRepository(mutableListOf(ComposerAttachmentItem(0, "only.txt")))
        )

        val effect = viewModel.onAction(ComposerAttachmentUiAction.Remove(0))

        assertEquals(ComposerAttachmentUiEffect.AttachmentsChanged(false), effect)
        assertFalse(viewModel.state.value.visible)
    }

    @Test
    fun invalidRemovalDoesNothing() {
        val viewModel = ComposerAttachmentStripViewModel(FakeRepository(mutableListOf()))

        assertNull(viewModel.onAction(ComposerAttachmentUiAction.Remove(7)))
        assertFalse(viewModel.state.value.visible)
    }
}
