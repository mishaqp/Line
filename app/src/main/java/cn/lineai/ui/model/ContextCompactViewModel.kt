package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContextCompactStatus {
    RUNNING,
    DONE,
    ERROR
}

data class ContextCompactSnapshot(
    val label: String
)

data class ContextCompactUiState(
    val label: String = "",
    val status: ContextCompactStatus = ContextCompactStatus.RUNNING
)

sealed interface ContextCompactUiAction {
    data class Bind(val status: String?) : ContextCompactUiAction
}

interface ContextCompactRepository {
    fun snapshot(): ContextCompactSnapshot
    fun normalizeStatus(status: String?): ContextCompactStatus
}

class ContextCompactViewModel(
    private val repository: ContextCompactRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ContextCompactUiState(label = repository.snapshot().label)
    )
    val state: StateFlow<ContextCompactUiState> = _state.asStateFlow()

    fun onAction(action: ContextCompactUiAction) {
        when (action) {
            is ContextCompactUiAction.Bind -> {
                val normalized = repository.normalizeStatus(action.status)
                if (_state.value.status != normalized) {
                    _state.value = _state.value.copy(status = normalized)
                }
            }
        }
    }

    companion object {
        fun factory(repository: ContextCompactRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ContextCompactViewModel::class.java)) {
                        return ContextCompactViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
