package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserMessageContentSnapshot(
    val content: String = "",
    val maxWidthDp: Float = 0f
)

data class UserMessageContentUiState(
    val content: String = "",
    val maxWidthDp: Float = 0f
) {
    val visible: Boolean get() = content.isNotEmpty()
}

sealed interface UserMessageContentUiAction {
    data class Bind(
        val message: ChatMessage?
    ) : UserMessageContentUiAction
}

interface UserMessageContentStateRepository {
    fun snapshot(): UserMessageContentSnapshot
    fun bind(message: ChatMessage?)
}

class UserMessageContentViewModel(
    private val repository: UserMessageContentStateRepository
) : ViewModel() {
    private val _state =
        MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<UserMessageContentUiState> =
        _state.asStateFlow()

    fun onAction(action: UserMessageContentUiAction) {
        when (action) {
            is UserMessageContentUiAction.Bind -> {
                repository.bind(action.message)
                _state.value = repository.snapshot().toUiState()
            }
        }
    }

    private fun UserMessageContentSnapshot.toUiState() =
        UserMessageContentUiState(
            content = content,
            maxWidthDp = maxWidthDp
        )

    companion object {
        fun factory(
            repository: UserMessageContentStateRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    if (
                        modelClass.isAssignableFrom(
                            UserMessageContentViewModel::class.java
                        )
                    ) {
                        return UserMessageContentViewModel(
                            repository
                        ) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: " +
                            modelClass.name
                    )
                }
            }
    }
}
