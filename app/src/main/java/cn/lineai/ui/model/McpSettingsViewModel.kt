package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.McpSettingsState
import cn.lineai.model.McpToolConfig
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class McpToolGroupUiModel(
    val id: String,
    val name: String,
    val description: String,
    val enabled: Boolean,
    val tools: List<String>,
    val iconKey: String
)

data class McpSettingsUiState(
    val executionMode: String = "local",
    val groups: List<McpToolGroupUiModel> = emptyList()
) {
    val showSshActions: Boolean get() = executionMode == "ssh"
}

sealed interface McpSettingsUiAction {
    data object Back : McpSettingsUiAction
    data object Reload : McpSettingsUiAction
    data class SetExecutionMode(val mode: String) : McpSettingsUiAction
    data class SetToolGroupEnabled(val id: String, val enabled: Boolean) : McpSettingsUiAction
    data object OpenSshSettings : McpSettingsUiAction
    data object OpenTermuxIntegration : McpSettingsUiAction
}

sealed interface McpSettingsUiEffect {
    data object Back : McpSettingsUiEffect
    data class Navigate(val destination: LineDestination) : McpSettingsUiEffect
}

interface McpSettingsRepository {
    fun snapshot(): McpSettingsState
    fun setExecutionMode(mode: String)
    fun setToolGroupEnabled(id: String, enabled: Boolean)
}

class McpSettingsViewModel(
    private val repository: McpSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<McpSettingsUiState> = _state.asStateFlow()

    fun onAction(action: McpSettingsUiAction): McpSettingsUiEffect? = when (action) {
        McpSettingsUiAction.Back -> McpSettingsUiEffect.Back
        McpSettingsUiAction.Reload -> {
            reload()
            null
        }
        is McpSettingsUiAction.SetExecutionMode -> {
            setExecutionMode(action.mode)
            null
        }
        is McpSettingsUiAction.SetToolGroupEnabled -> {
            setToolGroupEnabled(action.id, action.enabled)
            null
        }
        McpSettingsUiAction.OpenSshSettings ->
            McpSettingsUiEffect.Navigate(LineDestination.SshSettings)
        McpSettingsUiAction.OpenTermuxIntegration ->
            McpSettingsUiEffect.Navigate(LineDestination.TermuxIntegration)
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun setExecutionMode(mode: String) {
        repository.setExecutionMode(normalizeMode(mode))
        _state.value = readState()
    }

    private fun setToolGroupEnabled(id: String, enabled: Boolean) {
        if (id.isEmpty()) return
        repository.setToolGroupEnabled(id, enabled)
        _state.value = readState()
    }

    private fun readState(): McpSettingsUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        McpSettingsUiState()
    }

    companion object {
        const val MODE_LOCAL: String = "local"
        const val MODE_SSH: String = "ssh"
        const val MODE_TERMINAL_PROVIDER: String = "terminal_provider"
        const val MODE_ROOT: String = "root"

        fun normalizeMode(mode: String?): String = when (mode) {
            MODE_SSH -> MODE_SSH
            MODE_TERMINAL_PROVIDER -> MODE_TERMINAL_PROVIDER
            MODE_ROOT -> MODE_ROOT
            else -> MODE_LOCAL
        }

        fun fromSnapshot(snapshot: McpSettingsState?): McpSettingsUiState {
            val state = snapshot ?: McpSettingsState(MODE_LOCAL, null)
            val mode = normalizeMode(state.executionMode)
            return McpSettingsUiState(
                executionMode = mode,
                groups = state.configs.mapNotNull { config ->
                    if (!config.shouldShowForMode(mode)) {
                        null
                    } else {
                        toUiModel(config)
                    }
                }
            )
        }

        fun toUiModel(config: McpToolConfig): McpToolGroupUiModel = McpToolGroupUiModel(
            id = config.id,
            name = config.name,
            description = config.description,
            enabled = config.isEnabled,
            tools = config.tools.toList(),
            iconKey = config.iconKey
        )

        fun factory(repository: McpSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(McpSettingsViewModel::class.java)) {
                        return McpSettingsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
