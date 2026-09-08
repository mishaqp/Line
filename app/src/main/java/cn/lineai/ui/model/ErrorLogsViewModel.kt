package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ErrorLogItem(
    val file: File,
    val title: String,
    val subtitle: String,
    val timestamp: Long
)

interface ErrorLogsRepository {
    fun loadLogs(): List<ErrorLogItem>
    fun clearLogs()
    fun openLog(file: File): Boolean
}

enum class ErrorLogsMessage {
    CLEARED,
    OPEN_FAILED
}

data class ErrorLogsUiState(
    val logs: List<ErrorLogItem> = emptyList(),
    val message: ErrorLogsMessage? = null,
    val messageEventId: Long = 0L
)

sealed interface ErrorLogsUiAction {
    data object Back : ErrorLogsUiAction
    data object Refresh : ErrorLogsUiAction
    data object Clear : ErrorLogsUiAction
    data class Open(val item: ErrorLogItem) : ErrorLogsUiAction
    data object ConsumeMessage : ErrorLogsUiAction
}

/**
 * UDF owner for the error-log list. FileProvider and Intent work stay behind
 * the repository, keeping Android types out of the ViewModel.
 */
class ErrorLogsViewModel(
    private val repository: ErrorLogsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ErrorLogsUiState(logs = repository.loadLogs().toList())
    )
    val state: StateFlow<ErrorLogsUiState> = _state.asStateFlow()

    fun onAction(action: ErrorLogsUiAction) {
        when (action) {
            ErrorLogsUiAction.Back -> Unit
            ErrorLogsUiAction.Refresh -> reload()
            ErrorLogsUiAction.Clear -> {
                repository.clearLogs()
                _state.update {
                    it.copy(
                        logs = repository.loadLogs().toList(),
                        message = ErrorLogsMessage.CLEARED,
                        messageEventId = it.messageEventId + 1L
                    )
                }
            }
            is ErrorLogsUiAction.Open -> {
                if (!repository.openLog(action.item.file)) {
                    _state.update {
                        it.copy(
                            message = ErrorLogsMessage.OPEN_FAILED,
                            messageEventId = it.messageEventId + 1L
                        )
                    }
                }
            }
            ErrorLogsUiAction.ConsumeMessage -> {
                _state.update { it.copy(message = null) }
            }
        }
    }

    private fun reload() {
        _state.update { it.copy(logs = repository.loadLogs().toList()) }
    }

    companion object {
        fun factory(repository: ErrorLogsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ErrorLogsViewModel(repository) as T
                }
            }
    }
}
