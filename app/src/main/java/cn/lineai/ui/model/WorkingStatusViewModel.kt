package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkingStatusSnapshot(
    val workingLabel: String,
    val thinkingLabel: String
)

data class WorkingStatusUiState(
    val workingLabel: String = "",
    val thinkingLabel: String = "",
    val working: Boolean = false,
    val thinking: Boolean = false
) {
    val displayLabel: String
        get() = if (thinking) thinkingLabel else workingLabel
}

sealed interface WorkingStatusUiAction {
    data class Bind(val thinking: Boolean) : WorkingStatusUiAction
    data object Start : WorkingStatusUiAction
    data object Stop : WorkingStatusUiAction
}

interface WorkingStatusRepository {
    fun snapshot(): WorkingStatusSnapshot
}

class WorkingStatusViewModel(
    repository: WorkingStatusRepository
) : ViewModel() {
    private val initial = repository.snapshot()
    private val _state = MutableStateFlow(
        WorkingStatusUiState(
            workingLabel = initial.workingLabel,
            thinkingLabel = initial.thinkingLabel
        )
    )
    val state: StateFlow<WorkingStatusUiState> = _state.asStateFlow()

    fun onAction(action: WorkingStatusUiAction) {
        when (action) {
            is WorkingStatusUiAction.Bind -> {
                if (_state.value.thinking != action.thinking) {
                    _state.value = _state.value.copy(thinking = action.thinking)
                }
            }
            WorkingStatusUiAction.Start -> {
                if (!_state.value.working) {
                    _state.value = _state.value.copy(working = true)
                }
            }
            WorkingStatusUiAction.Stop -> {
                if (_state.value.working) {
                    _state.value = _state.value.copy(working = false)
                }
            }
        }
    }

    companion object {
        fun factory(repository: WorkingStatusRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(WorkingStatusViewModel::class.java)) {
                        return WorkingStatusViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
