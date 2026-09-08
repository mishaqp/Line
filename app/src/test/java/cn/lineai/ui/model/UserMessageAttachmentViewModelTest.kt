package cn.lineai.ui.model

import cn.lineai.model.InputAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserMessageAttachmentViewModelTest {
    private class FakeRepository :
        UserMessageAttachmentStateRepository {
        private var items = emptyList<UserMessageAttachmentItem>()

        override fun snapshot() =
            UserMessageAttachmentSnapshot(items.toList())

        override fun replaceAll(
            attachments: List<InputAttachment>?
        ) {
            items = attachments.orEmpty().map {
                UserMessageAttachmentItem(it.name)
            }
        }
    }

    @Test
    fun bindPublishesImmutableOrderedItems() {
        val source = mutableListOf(
            InputAttachment("one.txt", "/one", "local"),
            InputAttachment("two.txt", "/two", "local")
        )
        val viewModel =
            UserMessageAttachmentViewModel(FakeRepository())

        viewModel.onAction(
            UserMessageAttachmentUiAction.Bind(source)
        )
        source.clear()

        assertTrue(viewModel.state.value.visible)
        assertEquals(
            listOf("one.txt", "two.txt"),
            viewModel.state.value.items.map { it.name }
        )
    }

    @Test
    fun nullBindingHidesList() {
        val viewModel =
            UserMessageAttachmentViewModel(FakeRepository())

        viewModel.onAction(
            UserMessageAttachmentUiAction.Bind(null)
        )

        assertFalse(viewModel.state.value.visible)
    }
}
