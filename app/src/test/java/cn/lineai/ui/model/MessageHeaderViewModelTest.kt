package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageHeaderViewModelTest {
    private class FakeRepository(
        private val outgoing: Boolean
    ) : MessageHeaderStateRepository {
        private var name = ""

        override fun snapshot() = MessageHeaderSnapshot(
            outgoing = outgoing,
            name = name,
            monogram = name.firstOrNull { it.isLetterOrDigit() }
                ?.uppercaseChar()
                ?.toString()
                ?: "\u2022"
        )

        override fun bind(name: String?) {
            this.name = name?.trim().orEmpty()
        }
    }

    @Test
    fun incomingHeaderBindsTrimmedNameAndMonogram() {
        val viewModel = MessageHeaderViewModel(FakeRepository(false))

        viewModel.onAction(MessageHeaderUiAction.Bind("  kimi-k3  "))

        assertFalse(viewModel.state.value.outgoing)
        assertEquals("kimi-k3", viewModel.state.value.name)
        assertEquals("K", viewModel.state.value.monogram)
    }

    @Test
    fun outgoingHeaderKeepsDirectionAcrossBindings() {
        val viewModel = MessageHeaderViewModel(FakeRepository(true))

        viewModel.onAction(MessageHeaderUiAction.Bind("You"))
        viewModel.onAction(MessageHeaderUiAction.Bind("User"))

        assertTrue(viewModel.state.value.outgoing)
        assertEquals("User", viewModel.state.value.name)
    }
}
