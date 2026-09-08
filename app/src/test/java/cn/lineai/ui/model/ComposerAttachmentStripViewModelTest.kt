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

        override fun remove(id: ComposerAttachmentId): Boolean {
            val position = rows.indexOfFirst { it.id == id }
            if (position < 0) return false
            rows.removeAt(position)
            return true
        }
    }

    @Test
    fun initialAndRefreshStatesAreImmutableSnapshots() {
        val rows = mutableListOf(item("/one", "one.txt"))
        val viewModel = ComposerAttachmentStripViewModel(FakeRepository(rows))

        assertTrue(viewModel.state.value.visible)
        assertEquals("one.txt", viewModel.state.value.items.single().name)

        rows += item("/two", "two.txt")
        assertEquals(1, viewModel.state.value.items.size)

        viewModel.onAction(ComposerAttachmentUiAction.Refresh)
        assertEquals(2, viewModel.state.value.items.size)
    }

    @Test
    fun removalUsesStableIdentityAfterRowsShift() {
        val first = item("/first", "first.txt")
        val second = item("/second", "second.txt")
        val rows = mutableListOf(first, second)
        val viewModel = ComposerAttachmentStripViewModel(FakeRepository(rows))

        rows.removeAt(0)
        val effect = viewModel.onAction(ComposerAttachmentUiAction.Remove(second.id))

        assertEquals(ComposerAttachmentUiEffect.AttachmentsChanged(false), effect)
        assertFalse(viewModel.state.value.visible)
    }

    @Test
    fun invalidRemovalDoesNothing() {
        val viewModel = ComposerAttachmentStripViewModel(FakeRepository(mutableListOf()))

        assertNull(
            viewModel.onAction(
                ComposerAttachmentUiAction.Remove(
                    ComposerAttachmentId("/missing", "local")
                )
            )
        )
        assertFalse(viewModel.state.value.visible)
    }

    private fun item(path: String, name: String) =
        ComposerAttachmentItem(ComposerAttachmentId(path, "local"), name)
}
