package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.InputAttachment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserMessageAttachmentItem(
    val name: String
)

data class UserMessageAttachmentSnapshot(
    val items: List<UserMessageAttachmentItem> = emptyList()
)

data class UserMessageAttachmentUiState(
    val items: List<UserMessageAttachmentItem> = emptyList()
) {
    val visible: Boolean get() = items.isNotEmpty()
}

sealed interface UserMessageAttachmentUiAction {
    data class Bind(
        val attachments: List<InputAttachment>?
    ) : UserMessageAttachmentUiAction
}

interface UserMessageAttachmentStateRepository {
    fun snapshot(): UserMessageAttachmentSnapshot
    fun replaceAll(attachments: List<InputAttachment>?)
}

class UserMessageAttachmentViewModel(
    private val repository: UserMessageAttachmentStateRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<UserMessageAttachmentUiState> = _state.asStateFlow()

    fun onAction(action: UserMessageAttachmentUiAction) {
        when (action) {
            is UserMessageAttachmentUiAction.Bind -> {
                repository.replaceAll(action.attachments)
                _state.value = repository.snapshot().toUiState()
            }
        }
    }

    private fun UserMessageAttachmentSnapshot.toUiState() =
        UserMessageAttachmentUiState(items.toList())

    companion object {
        fun factory(
            repository: UserMessageAttachmentStateRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(
                        UserMessageAttachmentViewModel::class.java
                    )
                ) {
                    return UserMessageAttachmentViewModel(repository) as T
                }
                throw IllegalArgumentException(
                    "Unknown ViewModel class: " + modelClass.name
                )
            }
        }
    }
}
