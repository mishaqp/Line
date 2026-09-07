package cn.lineai.ui.model

import cn.lineai.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserMessageContentViewModelTest {
    private class FakeRepository :
        UserMessageContentStateRepository {
        private var value =
            UserMessageContentSnapshot(maxWidthDp = 240f)

        override fun snapshot() = value

        override fun bind(message: ChatMessage?) {
            value = value.copy(
                content = message?.content.orEmpty()
            )
        }
    }

    @Test
    fun bindingPublishesContentAndFixedWidthBoundary() {
        val viewModel =
            UserMessageContentViewModel(FakeRepository())

        viewModel.onAction(
            UserMessageContentUiAction.Bind(
                ChatMessage(
                    "1",
                    ChatMessage.Role.USER,
                    "Hello",
                    false
                )
            )
        )

        assertTrue(viewModel.state.value.visible)
        assertEquals("Hello", viewModel.state.value.content)
        assertEquals(
            240f,
            viewModel.state.value.maxWidthDp
        )
    }

    @Test
    fun nullMessageHidesBubble() {
        val viewModel =
            UserMessageContentViewModel(FakeRepository())

        viewModel.onAction(
            UserMessageContentUiAction.Bind(null)
        )

        assertFalse(viewModel.state.value.visible)
    }
}
