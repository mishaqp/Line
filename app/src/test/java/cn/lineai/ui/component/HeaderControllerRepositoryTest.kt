package cn.lineai.ui.component

import cn.lineai.model.ChatMode
import cn.lineai.model.ChatUiState
import cn.lineai.ui.model.HeaderUiEffect
import java.util.Collections
import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderControllerRepositoryTest {
    @Test
    fun snapshotMapsOnlyHeaderFieldsAndNormalizesMode() {
        val repository = HeaderControllerRepository("Default")
        val state = ChatUiState(
            "Project",
            "/project",
            "Model",
            "Context",
            50,
            false,
            true,
            true,
            false,
            false,
            "builtin",
            "send",
            "unknown",
            "conversation",
            Collections.emptyList(),
            "",
            Collections.emptyList(),
            "SSH"
        )

        val snapshot = repository.snapshot(state)

        assertEquals("Project", snapshot.projectLabel)
        assertEquals("SSH", snapshot.executionTargetLabel)
        assertEquals(ChatMode.AGENT, snapshot.chatMode)
        assertEquals("Default", repository.snapshot(null).projectLabel)
    }

    @Test
    fun dispatchPreservesEveryLegacyCallback() {
        val calls = mutableListOf<String>()
        val repository = HeaderControllerRepository("Default")
        repository.setListener(object : HeaderView.Listener {
            override fun onMenuClick() { calls += "menu" }
            override fun onProjectClick() { calls += "project" }
            override fun onModeChanged(mode: String) { calls += "mode:$mode" }
            override fun onPermissionClick() { calls += "permission" }
            override fun onNewConversationClick() { calls += "new" }
            override fun onMoreClick() { calls += "more" }
        })

        repository.dispatch(HeaderUiEffect.Menu)
        repository.dispatch(HeaderUiEffect.Project)
        repository.dispatch(HeaderUiEffect.ChangeMode(ChatMode.PLAN))
        repository.dispatch(HeaderUiEffect.Permission)
        repository.dispatch(HeaderUiEffect.NewConversation)
        repository.dispatch(HeaderUiEffect.More)

        assertEquals(
            listOf("menu", "project", "mode:plan", "permission", "new", "more"),
            calls
        )
    }
}
