package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComposerAttachmentId(
    val path: String,
    val source: String
)

data class ComposerAttachmentItem(
    val id: ComposerAttachmentId,
    val name: String
)

data class ComposerAttachmentSnapshot(
    val items: List<ComposerAttachmentItem> = emptyList()
)

data class ComposerAttachmentUiState(
    val items: List<ComposerAttachmentItem> = emptyList()
) {
    val visible: Boolean
        get() = items.isNotEmpty()
}

sealed interface ComposerAttachmentUiAction {
    data object Refresh : ComposerAttachmentUiAction
    data class Remove(val id: ComposerAttachmentId) : ComposerAttachmentUiAction
}

sealed interface ComposerAttachmentUiEffect {
    data class AttachmentsChanged(val visible: Boolean) : ComposerAttachmentUiEffect
}

interface ComposerAttachmentStateRepository {
    fun snapshot(): ComposerAttachmentSnapshot
    fun remove(id: ComposerAttachmentId): Boolean
}

class ComposerAttachmentStripViewModel(
    private val repository: ComposerAttachmentStateRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<ComposerAttachmentUiState> = _state.asStateFlow()

    fun onAction(action: ComposerAttachmentUiAction): ComposerAttachmentUiEffect? = when (action) {
        ComposerAttachmentUiAction.Refresh -> {
            refresh()
            null
        }
        is ComposerAttachmentUiAction.Remove -> {
            if (!repository.remove(action.id)) {
                null
            } else {
                refresh()
                ComposerAttachmentUiEffect.AttachmentsChanged(_state.value.visible)
            }
        }
    }

    private fun refresh() {
        _state.value = repository.snapshot().toUiState()
    }

    private fun ComposerAttachmentSnapshot.toUiState() = ComposerAttachmentUiState(
        items = items.toList()
    )

    companion object {
        fun factory(repository: ComposerAttachmentStateRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ComposerAttachmentStripViewModel::class.java)) {
                        return ComposerAttachmentStripViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
