package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MessageHeaderSnapshot(
    val outgoing: Boolean,
    val name: String,
    val monogram: String
)

data class MessageHeaderUiState(
    val outgoing: Boolean = false,
    val name: String = "",
    val monogram: String = "\u2022"
)

sealed interface MessageHeaderUiAction {
    data class Bind(val name: String?) : MessageHeaderUiAction
}

interface MessageHeaderStateRepository {
    fun snapshot(): MessageHeaderSnapshot
    fun bind(name: String?)
}

class MessageHeaderViewModel(
    private val repository: MessageHeaderStateRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<MessageHeaderUiState> = _state.asStateFlow()

    fun onAction(action: MessageHeaderUiAction) {
        when (action) {
            is MessageHeaderUiAction.Bind -> {
                repository.bind(action.name)
                _state.value = repository.snapshot().toUiState()
            }
        }
    }

    private fun MessageHeaderSnapshot.toUiState() = MessageHeaderUiState(
        outgoing = outgoing,
        name = name,
        monogram = monogram
    )

    companion object {
        fun factory(repository: MessageHeaderStateRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MessageHeaderViewModel::class.java)) {
                        return MessageHeaderViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
