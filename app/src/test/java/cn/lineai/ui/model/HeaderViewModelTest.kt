package cn.lineai.ui.model

import cn.lineai.model.ChatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HeaderViewModelTest {
    private val repository = object : HeaderRepository {
        override fun initialSnapshot() =
            HeaderSnapshot("Project", "SSH", ChatMode.AGENT)
    }

    @Test
    fun initialStateComesFromRepositoryAndRenderReplacesVisibleValues() {
        val viewModel = HeaderViewModel(repository)

        assertEquals("Project", viewModel.state.value.projectLabel)
        assertEquals("SSH", viewModel.state.value.executionTargetLabel)

        viewModel.onAction(
            HeaderUiAction.Render(HeaderSnapshot("Next", "", ChatMode.PLAN))
        )

        assertEquals("Next", viewModel.state.value.projectLabel)
        assertEquals("", viewModel.state.value.executionTargetLabel)
        assertEquals(ChatMode.PLAN, viewModel.state.value.chatMode)
    }

    @Test
    fun modeMenuOpensDismissesAndRejectsDuplicateSelection() {
        val viewModel = HeaderViewModel(repository)

        viewModel.onAction(HeaderUiAction.ToggleModeMenu)
        assertTrue(viewModel.state.value.modeMenuVisible)

        assertNull(viewModel.onAction(HeaderUiAction.SelectMode(ChatMode.AGENT)))
        assertFalse(viewModel.state.value.modeMenuVisible)

        viewModel.onAction(HeaderUiAction.ToggleModeMenu)
        val effect = viewModel.onAction(HeaderUiAction.SelectMode(ChatMode.CHAT))
        assertEquals(HeaderUiEffect.ChangeMode(ChatMode.CHAT), effect)
        assertFalse(viewModel.state.value.modeMenuVisible)
    }

    @Test
    fun everyHeaderControlProducesTheMatchingEffect() {
        val viewModel = HeaderViewModel(repository)

        assertEquals(HeaderUiEffect.Menu, viewModel.onAction(HeaderUiAction.Menu))
        assertEquals(HeaderUiEffect.Project, viewModel.onAction(HeaderUiAction.Project))
        assertEquals(HeaderUiEffect.Permission, viewModel.onAction(HeaderUiAction.Permission))
        assertEquals(
            HeaderUiEffect.NewConversation,
            viewModel.onAction(HeaderUiAction.NewConversation)
        )
        assertEquals(HeaderUiEffect.More, viewModel.onAction(HeaderUiAction.More))
    }
}
