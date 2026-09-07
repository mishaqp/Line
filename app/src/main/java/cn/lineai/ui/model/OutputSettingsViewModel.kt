package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.OutputSettings
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface OutputSettingsRepository {
    fun settings(): OutputSettings
    fun setCodeWrapEnabled(enabled: Boolean)
    fun setBrowserMode(mode: String)
    fun setBrowserJavaScriptEnabled(enabled: Boolean)
}

data class OutputSettingsUiState(
    val codeWrapEnabled: Boolean = false,
    val browserMode: String = OutputSettings.BROWSER_BUILTIN,
    val browserJavaScriptEnabled: Boolean = false
) {
    companion object {
        fun from(settings: OutputSettings?): OutputSettingsUiState {
            val value = settings ?: OutputSettings(false, OutputSettings.BROWSER_BUILTIN)
            return OutputSettingsUiState(
                codeWrapEnabled = value.isCodeWrapEnabled,
                browserMode = value.browserMode,
                browserJavaScriptEnabled = value.isBrowserJavaScriptEnabled
            )
        }
    }
}

sealed interface OutputSettingsUiAction {
    data object Back : OutputSettingsUiAction
    data class SetCodeWrap(val enabled: Boolean) : OutputSettingsUiAction
    data class SetBrowserMode(val mode: String) : OutputSettingsUiAction
    data class SetBrowserJavaScript(val enabled: Boolean) : OutputSettingsUiAction
    data object OpenToolCallPreview : OutputSettingsUiAction
}

/**
 * UDF owner for the Compose Output settings screen. Persistence stays behind
 * [OutputSettingsRepository]; Compose never talks to MainUiController.
 */
class OutputSettingsViewModel(
    private val repository: OutputSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(OutputSettingsUiState.from(repository.settings()))
    val state: StateFlow<OutputSettingsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = OutputSettingsUiState.from(repository.settings())
    }

    fun onAction(action: OutputSettingsUiAction): LineDestination? {
        return when (action) {
            OutputSettingsUiAction.Back -> null
            OutputSettingsUiAction.OpenToolCallPreview -> LineDestination.ToolCallPreview
            is OutputSettingsUiAction.SetCodeWrap -> {
                _state.update { it.copy(codeWrapEnabled = action.enabled) }
                repository.setCodeWrapEnabled(action.enabled)
                null
            }
            is OutputSettingsUiAction.SetBrowserMode -> {
                val mode = OutputSettings.normalizeBrowserMode(action.mode)
                _state.update { it.copy(browserMode = mode) }
                repository.setBrowserMode(mode)
                null
            }
            is OutputSettingsUiAction.SetBrowserJavaScript -> {
                _state.update { it.copy(browserJavaScriptEnabled = action.enabled) }
                repository.setBrowserJavaScriptEnabled(action.enabled)
                null
            }
        }
    }

    companion object {
        fun factory(repository: OutputSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OutputSettingsViewModel(repository) as T
                }
            }
    }
}
