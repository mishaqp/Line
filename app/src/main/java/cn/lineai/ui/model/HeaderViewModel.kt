package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.ChatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HeaderSnapshot(
    val projectLabel: String = "",
    val executionTargetLabel: String = "",
    val chatMode: String = ChatMode.DEFAULT
)

data class HeaderUiState(
    val projectLabel: String = "",
    val executionTargetLabel: String = "",
    val chatMode: String = ChatMode.DEFAULT,
    val modeMenuVisible: Boolean = false
)

sealed interface HeaderUiAction {
    data class Render(val snapshot: HeaderSnapshot) : HeaderUiAction
    data object Menu : HeaderUiAction
    data object Project : HeaderUiAction
    data object ToggleModeMenu : HeaderUiAction
    data object DismissModeMenu : HeaderUiAction
    data class SelectMode(val mode: String) : HeaderUiAction
    data object Permission : HeaderUiAction
    data object NewConversation : HeaderUiAction
    data object More : HeaderUiAction
}

sealed interface HeaderUiEffect {
    data object Menu : HeaderUiEffect
    data object Project : HeaderUiEffect
    data class ChangeMode(val mode: String) : HeaderUiEffect
    data object Permission : HeaderUiEffect
    data object NewConversation : HeaderUiEffect
    data object More : HeaderUiEffect
}

interface HeaderRepository {
    fun initialSnapshot(): HeaderSnapshot
}

class HeaderViewModel(
    repository: HeaderRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.initialSnapshot().toUiState())
    val state: StateFlow<HeaderUiState> = _state.asStateFlow()

    fun onAction(action: HeaderUiAction): HeaderUiEffect? = when (action) {
        is HeaderUiAction.Render -> {
            _state.value = action.snapshot.toUiState(
                modeMenuVisible = _state.value.modeMenuVisible
            )
            null
        }
        HeaderUiAction.Menu -> HeaderUiEffect.Menu
        HeaderUiAction.Project -> HeaderUiEffect.Project
        HeaderUiAction.ToggleModeMenu -> {
            _state.value = _state.value.copy(modeMenuVisible = !_state.value.modeMenuVisible)
            null
        }
        HeaderUiAction.DismissModeMenu -> {
            _state.value = _state.value.copy(modeMenuVisible = false)
            null
        }
        is HeaderUiAction.SelectMode -> selectMode(action.mode)
        HeaderUiAction.Permission -> HeaderUiEffect.Permission
        HeaderUiAction.NewConversation -> HeaderUiEffect.NewConversation
        HeaderUiAction.More -> HeaderUiEffect.More
    }

    private fun selectMode(mode: String): HeaderUiEffect? {
        val normalized = ChatMode.normalize(mode)
        val current = _state.value
        _state.value = current.copy(modeMenuVisible = false)
        return if (normalized == current.chatMode) null else HeaderUiEffect.ChangeMode(normalized)
    }

    private fun HeaderSnapshot.toUiState(modeMenuVisible: Boolean = false) =
        HeaderUiState(
            projectLabel = projectLabel,
            executionTargetLabel = executionTargetLabel,
            chatMode = ChatMode.normalize(chatMode),
            modeMenuVisible = modeMenuVisible
        )

    companion object {
        fun factory(repository: HeaderRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(HeaderViewModel::class.java)) {
                        return HeaderViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
