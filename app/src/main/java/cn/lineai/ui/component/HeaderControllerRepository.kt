package cn.lineai.ui.component

import cn.lineai.model.ChatMode
import cn.lineai.model.ChatUiState
import cn.lineai.ui.model.HeaderRepository
import cn.lineai.ui.model.HeaderSnapshot

class HeaderControllerRepository(
    private val defaultProjectLabel: String
) : HeaderRepository {
    private var listener: HeaderView.Listener? = null

    override fun initialSnapshot(): HeaderSnapshot =
        HeaderSnapshot(projectLabel = defaultProjectLabel, chatMode = ChatMode.DEFAULT)

    fun setListener(listener: HeaderView.Listener?) {
        this.listener = listener
    }

    fun snapshot(state: ChatUiState?): HeaderSnapshot {
        if (state == null) return initialSnapshot()
        return HeaderSnapshot(
            projectLabel = state.projectLabel ?: "",
            executionTargetLabel = state.executionTargetLabel.orEmpty(),
            chatMode = ChatMode.normalize(state.chatMode)
        )
    }

    fun dispatch(effect: cn.lineai.ui.model.HeaderUiEffect) {
        val target = listener ?: return
        when (effect) {
            cn.lineai.ui.model.HeaderUiEffect.Menu -> target.onMenuClick()
            cn.lineai.ui.model.HeaderUiEffect.Project -> target.onProjectClick()
            is cn.lineai.ui.model.HeaderUiEffect.ChangeMode ->
                target.onModeChanged(effect.mode)
            cn.lineai.ui.model.HeaderUiEffect.Permission -> target.onPermissionClick()
            cn.lineai.ui.model.HeaderUiEffect.NewConversation ->
                target.onNewConversationClick()
            cn.lineai.ui.model.HeaderUiEffect.More -> target.onMoreClick()
        }
    }
}
