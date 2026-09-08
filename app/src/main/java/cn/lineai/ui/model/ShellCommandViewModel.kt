package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShellCommandSnapshot(
    val command: String = "",
    val hasCommand: Boolean = false
) {
    override fun toString(): String {
        return "ShellCommandSnapshot(hasCommand=$hasCommand, commandLength=${command.length})"
    }
}

data class ShellCommandUiState(
    val command: String = "",
    val hasCommand: Boolean = false
) {
    override fun toString(): String {
        return "ShellCommandUiState(hasCommand=$hasCommand, commandLength=${command.length})"
    }
}

sealed interface ShellCommandUiAction {
    data object Back : ShellCommandUiAction
    data object Reload : ShellCommandUiAction
}

sealed interface ShellCommandUiEffect {
    data object Back : ShellCommandUiEffect
}

interface ShellCommandRepository {
    fun snapshot(): ShellCommandSnapshot
}

class ShellCommandViewModel(
    private val repository: ShellCommandRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ShellCommandUiState> = _state.asStateFlow()

    fun onAction(action: ShellCommandUiAction): ShellCommandUiEffect? = when (action) {
        ShellCommandUiAction.Back -> ShellCommandUiEffect.Back
        ShellCommandUiAction.Reload -> {
            reload()
            null
        }
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun readState(): ShellCommandUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        ShellCommandUiState()
    }

    companion object {
        fun fromSnapshot(snapshot: ShellCommandSnapshot): ShellCommandUiState =
            ShellCommandUiState(
                command = snapshot.command,
                hasCommand = snapshot.hasCommand
            )

        fun factory(repository: ShellCommandRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ShellCommandViewModel::class.java)) {
                        return ShellCommandViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
