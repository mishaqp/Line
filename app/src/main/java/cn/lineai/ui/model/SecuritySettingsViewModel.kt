package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.OutputSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface SecuritySettingsRepository {
    fun outputSettings(): OutputSettings
    fun fullAccessEnabled(): Boolean
    fun setAllowAnyHttp(enabled: Boolean)
    fun setBrowserJavaScriptEnabled(enabled: Boolean)
    fun setBypassPathProtection(enabled: Boolean)
    fun setFullAccessEnabled(enabled: Boolean)
}

data class SecuritySettingsUiState(
    val allowAnyHttp: Boolean = false,
    val browserJavaScriptEnabled: Boolean = false,
    val bypassPathProtection: Boolean = false,
    val fullAccessEnabled: Boolean = false,
    val showBypassWarning: Boolean = false
) {
    companion object {
        fun from(
            settings: OutputSettings?,
            fullAccessEnabled: Boolean
        ): SecuritySettingsUiState {
            val value = settings ?: OutputSettings(false, OutputSettings.BROWSER_BUILTIN)
            return SecuritySettingsUiState(
                allowAnyHttp = value.isAllowAnyHttp,
                browserJavaScriptEnabled = value.isBrowserJavaScriptEnabled,
                bypassPathProtection = value.isBypassPathProtection,
                fullAccessEnabled = fullAccessEnabled
            )
        }
    }
}

sealed interface SecuritySettingsUiAction {
    data object Back : SecuritySettingsUiAction
    data class SetAllowAnyHttp(val enabled: Boolean) : SecuritySettingsUiAction
    data class SetBrowserJavaScript(val enabled: Boolean) : SecuritySettingsUiAction
    data class SetBypassPathProtection(val enabled: Boolean) : SecuritySettingsUiAction
    data object ConfirmBypassPathProtection : SecuritySettingsUiAction
    data object DismissBypassWarning : SecuritySettingsUiAction
    data class SetFullAccess(val enabled: Boolean) : SecuritySettingsUiAction
}

/**
 * UDF owner for Security settings. Android dialogs and resources remain in
 * Compose; persistence and controller callbacks remain behind the repository.
 */
class SecuritySettingsViewModel(
    private val repository: SecuritySettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        SecuritySettingsUiState.from(
            repository.outputSettings(),
            repository.fullAccessEnabled()
        )
    )
    val state: StateFlow<SecuritySettingsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = SecuritySettingsUiState.from(
            repository.outputSettings(),
            repository.fullAccessEnabled()
        )
    }

    fun onAction(action: SecuritySettingsUiAction) {
        when (action) {
            SecuritySettingsUiAction.Back -> Unit
            is SecuritySettingsUiAction.SetAllowAnyHttp -> {
                _state.update { it.copy(allowAnyHttp = action.enabled) }
                repository.setAllowAnyHttp(action.enabled)
            }
            is SecuritySettingsUiAction.SetBrowserJavaScript -> {
                _state.update { it.copy(browserJavaScriptEnabled = action.enabled) }
                repository.setBrowserJavaScriptEnabled(action.enabled)
            }
            is SecuritySettingsUiAction.SetBypassPathProtection -> {
                if (action.enabled) {
                    _state.update { it.copy(showBypassWarning = true) }
                } else {
                    _state.update {
                        it.copy(
                            bypassPathProtection = false,
                            showBypassWarning = false
                        )
                    }
                    repository.setBypassPathProtection(false)
                }
            }
            SecuritySettingsUiAction.ConfirmBypassPathProtection -> {
                _state.update {
                    it.copy(
                        bypassPathProtection = true,
                        showBypassWarning = false
                    )
                }
                repository.setBypassPathProtection(true)
            }
            SecuritySettingsUiAction.DismissBypassWarning -> {
                _state.update { it.copy(showBypassWarning = false) }
            }
            is SecuritySettingsUiAction.SetFullAccess -> {
                _state.update { it.copy(fullAccessEnabled = action.enabled) }
                repository.setFullAccessEnabled(action.enabled)
            }
        }
    }

    companion object {
        fun factory(repository: SecuritySettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SecuritySettingsViewModel(repository) as T
                }
            }
    }
}
