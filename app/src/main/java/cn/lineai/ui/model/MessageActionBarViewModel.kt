package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MessageActionBarSnapshot(
    val alignRight: Boolean,
    val recallEnabled: Boolean,
    val actionsAllowed: Boolean,
    val expanded: Boolean
)

data class MessageActionBarUiState(
    val alignRight: Boolean = false,
    val recallEnabled: Boolean = false,
    val actionsAllowed: Boolean = true,
    val expanded: Boolean = false
) {
    val copyVisible: Boolean get() = actionsAllowed
    val secondaryVisible: Boolean get() = actionsAllowed && expanded
    val moreVisible: Boolean get() = actionsAllowed
}

sealed interface MessageActionBarUiAction {
    data class SetExpanded(val expanded: Boolean) : MessageActionBarUiAction
    data class SetActionsAllowed(val allowed: Boolean) : MessageActionBarUiAction
    data object Copy : MessageActionBarUiAction
    data object Quote : MessageActionBarUiAction
    data object Share : MessageActionBarUiAction
    data object Select : MessageActionBarUiAction
    data object MultiSelect : MessageActionBarUiAction
    data object Recall : MessageActionBarUiAction
    data object More : MessageActionBarUiAction
}

sealed interface MessageActionBarUiEffect {
    data object Copy : MessageActionBarUiEffect
    data object Quote : MessageActionBarUiEffect
    data object Share : MessageActionBarUiEffect
    data object Select : MessageActionBarUiEffect
    data object MultiSelect : MessageActionBarUiEffect
    data object Recall : MessageActionBarUiEffect
    data object More : MessageActionBarUiEffect
}

interface MessageActionBarStateRepository {
    fun snapshot(): MessageActionBarSnapshot
    fun setExpanded(expanded: Boolean)
    fun setActionsAllowed(allowed: Boolean)
}

class MessageActionBarViewModel(
    private val repository: MessageActionBarStateRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<MessageActionBarUiState> = _state.asStateFlow()

    fun onAction(action: MessageActionBarUiAction): MessageActionBarUiEffect? =
        when (action) {
            is MessageActionBarUiAction.SetExpanded -> {
                repository.setExpanded(action.expanded)
                refresh()
                null
            }
            is MessageActionBarUiAction.SetActionsAllowed -> {
                repository.setActionsAllowed(action.allowed)
                refresh()
                null
            }
            MessageActionBarUiAction.Copy ->
                MessageActionBarUiEffect.Copy.takeIf { _state.value.copyVisible }
            MessageActionBarUiAction.Quote ->
                MessageActionBarUiEffect.Quote.takeIf { _state.value.secondaryVisible }
            MessageActionBarUiAction.Share ->
                MessageActionBarUiEffect.Share.takeIf { _state.value.secondaryVisible }
            MessageActionBarUiAction.Select ->
                MessageActionBarUiEffect.Select.takeIf { _state.value.secondaryVisible }
            MessageActionBarUiAction.MultiSelect ->
                MessageActionBarUiEffect.MultiSelect.takeIf { _state.value.secondaryVisible }
            MessageActionBarUiAction.Recall ->
                MessageActionBarUiEffect.Recall.takeIf {
                    _state.value.secondaryVisible && _state.value.recallEnabled
                }
            MessageActionBarUiAction.More ->
                MessageActionBarUiEffect.More.takeIf { _state.value.moreVisible }
        }

    private fun refresh() {
        _state.value = repository.snapshot().toUiState()
    }

    private fun MessageActionBarSnapshot.toUiState() = MessageActionBarUiState(
        alignRight = alignRight,
        recallEnabled = recallEnabled,
        actionsAllowed = actionsAllowed,
        expanded = expanded
    )

    companion object {
        fun factory(repository: MessageActionBarStateRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MessageActionBarViewModel::class.java)) {
                        return MessageActionBarViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
