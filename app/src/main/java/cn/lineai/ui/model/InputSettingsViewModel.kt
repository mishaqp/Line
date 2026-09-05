package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.InputSettings
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface InputSettingsRepository {
    fun settings(): InputSettings
    fun setEnterKeyBehavior(behavior: String)
}

data class InputSettingsUiState(
    val enterKeyBehavior: String = InputSettings.ENTER_SEND
) {
    companion object {
        fun from(settings: InputSettings?): InputSettingsUiState {
            val value = settings ?: InputSettings(InputSettings.ENTER_SEND)
            return InputSettingsUiState(enterKeyBehavior = value.enterKeyBehavior)
        }
    }
}

sealed interface InputSettingsUiAction {
    data object Back : InputSettingsUiAction
    data class SetEnterKeyBehavior(val behavior: String) : InputSettingsUiAction
}

/**
 * UDF owner for the Compose Input settings screen. Persistence stays behind
 * [InputSettingsRepository]; Compose never talks to MainUiController.
 */
class InputSettingsViewModel(
    private val repository: InputSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(InputSettingsUiState.from(repository.settings()))
    val state: StateFlow<InputSettingsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = InputSettingsUiState.from(repository.settings())
    }

    fun onAction(action: InputSettingsUiAction): LineDestination? {
        return when (action) {
            InputSettingsUiAction.Back -> null
            is InputSettingsUiAction.SetEnterKeyBehavior -> {
                val behavior = InputSettings.normalizeEnterKeyBehavior(action.behavior)
                _state.update { it.copy(enterKeyBehavior = behavior) }
                repository.setEnterKeyBehavior(behavior)
                null
            }
        }
    }

    companion object {
        fun factory(repository: InputSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InputSettingsViewModel(repository) as T
                }
            }
    }
}
