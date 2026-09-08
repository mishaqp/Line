package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComposerPendingQueueItem(
    val index: Int,
    val preview: String
)

data class ComposerPendingQueueSnapshot(
    val items: List<ComposerPendingQueueItem> = emptyList(),
    val overflowCount: Int = 0
)

data class ComposerPendingQueueUiState(
    val items: List<ComposerPendingQueueItem> = emptyList(),
    val overflowCount: Int = 0
) {
    val visible: Boolean
        get() = items.isNotEmpty()
}

sealed interface ComposerPendingQueueUiAction {
    data object Refresh : ComposerPendingQueueUiAction
    data class Remove(val index: Int) : ComposerPendingQueueUiAction
}

sealed interface ComposerPendingQueueUiEffect {
    data class QueueChanged(val visible: Boolean) : ComposerPendingQueueUiEffect
}

interface ComposerPendingQueueRepository {
    fun snapshot(): ComposerPendingQueueSnapshot
    fun removeAt(index: Int): Boolean
}

class ComposerPendingQueueViewModel(
    private val repository: ComposerPendingQueueRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<ComposerPendingQueueUiState> = _state.asStateFlow()

    fun onAction(
        action: ComposerPendingQueueUiAction
    ): ComposerPendingQueueUiEffect? = when (action) {
        ComposerPendingQueueUiAction.Refresh -> {
            refresh()
            null
        }
        is ComposerPendingQueueUiAction.Remove -> {
            if (!repository.removeAt(action.index)) {
                null
            } else {
                refresh()
                ComposerPendingQueueUiEffect.QueueChanged(_state.value.visible)
            }
        }
    }

    private fun refresh() {
        _state.value = repository.snapshot().toUiState()
    }

    private fun ComposerPendingQueueSnapshot.toUiState() =
        ComposerPendingQueueUiState(
            items = items.toList(),
            overflowCount = overflowCount.coerceAtLeast(0)
        )

    companion object {
        fun factory(repository: ComposerPendingQueueRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ComposerPendingQueueViewModel::class.java)) {
                        return ComposerPendingQueueViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
