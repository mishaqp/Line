package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.WebSearchConfig
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ToolSettingsSnapshot(
    val imageUnderstandingLabel: String = "",
    val imageGenerationLabel: String = "",
    val webSearch: WebSearchConfig = WebSearchConfig.defaultConfig()
)

data class ToolSettingsUiState(
    val imageUnderstandingLabel: String = "",
    val imageGenerationLabel: String = "",
    val provider: String = WebSearchConfig.PROVIDER_BING_RSS_FREE,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val queryParam: String = "",
    val apiKeyHeader: String = "",
    val apiKeyParam: String = ""
) {
    val showSearchFields: Boolean
        get() = provider != WebSearchConfig.PROVIDER_BING_RSS_FREE

    override fun toString(): String {
        return "ToolSettingsUiState(" +
            "imageUnderstandingLabel=$imageUnderstandingLabel, " +
            "imageGenerationLabel=$imageGenerationLabel, " +
            "provider=$provider, " +
            "baseUrl=$baseUrl, " +
            "apiKey=${if (apiKey.isEmpty()) "" else "***"}, " +
            "model=$model, " +
            "queryParam=$queryParam, " +
            "apiKeyHeader=$apiKeyHeader, " +
            "apiKeyParam=$apiKeyParam, " +
            "showSearchFields=$showSearchFields)"
    }
}

sealed interface ToolSettingsUiAction {
    data object Back : ToolSettingsUiAction
    data object Reload : ToolSettingsUiAction
    data object OpenImageUnderstandingModel : ToolSettingsUiAction
    data object OpenImageGenerationModel : ToolSettingsUiAction
    data class SelectProvider(val provider: String) : ToolSettingsUiAction
    data class ChangeBaseUrl(val value: String) : ToolSettingsUiAction
    data class ChangeApiKey(val value: String) : ToolSettingsUiAction
    data class ChangeModel(val value: String) : ToolSettingsUiAction
    data class ChangeQueryParam(val value: String) : ToolSettingsUiAction
    data class ChangeApiKeyHeader(val value: String) : ToolSettingsUiAction
    data class ChangeApiKeyParam(val value: String) : ToolSettingsUiAction
}

sealed interface ToolSettingsUiEffect {
    data object Back : ToolSettingsUiEffect
    data class Navigate(val destination: LineDestination) : ToolSettingsUiEffect
}

interface ToolSettingsRepository {
    fun snapshot(): ToolSettingsSnapshot
    fun saveWebSearchConfig(config: WebSearchConfig)
}

class ToolSettingsViewModel(
    private val repository: ToolSettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ToolSettingsUiState> = _state.asStateFlow()

    fun onAction(action: ToolSettingsUiAction): ToolSettingsUiEffect? = when (action) {
        ToolSettingsUiAction.Back -> ToolSettingsUiEffect.Back
        ToolSettingsUiAction.Reload -> {
            reload()
            null
        }
        ToolSettingsUiAction.OpenImageUnderstandingModel ->
            ToolSettingsUiEffect.Navigate(LineDestination.ImageUnderstandingModel)
        ToolSettingsUiAction.OpenImageGenerationModel ->
            ToolSettingsUiEffect.Navigate(LineDestination.ImageGenerationModel)
        is ToolSettingsUiAction.SelectProvider -> {
            selectProvider(action.provider)
            null
        }
        is ToolSettingsUiAction.ChangeBaseUrl -> {
            saveFields { it.copy(baseUrl = action.value) }
            null
        }
        is ToolSettingsUiAction.ChangeApiKey -> {
            saveFields { it.copy(apiKey = action.value) }
            null
        }
        is ToolSettingsUiAction.ChangeModel -> {
            saveFields { it.copy(model = action.value) }
            null
        }
        is ToolSettingsUiAction.ChangeQueryParam -> {
            saveFields { it.copy(queryParam = action.value) }
            null
        }
        is ToolSettingsUiAction.ChangeApiKeyHeader -> {
            saveFields { it.copy(apiKeyHeader = action.value) }
            null
        }
        is ToolSettingsUiAction.ChangeApiKeyParam -> {
            saveFields { it.copy(apiKeyParam = action.value) }
            null
        }
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun selectProvider(provider: String) {
        val defaults = WebSearchConfig.defaultConfig(provider)
        repository.saveWebSearchConfig(defaults)
        _state.update { current ->
            current.copy(
                provider = defaults.provider,
                baseUrl = defaults.baseUrl,
                apiKey = defaults.apiKey,
                model = defaults.model,
                queryParam = defaults.queryParam,
                apiKeyHeader = defaults.apiKeyHeader,
                apiKeyParam = defaults.apiKeyParam
            )
        }
    }

    private fun saveFields(transform: (ToolSettingsUiState) -> ToolSettingsUiState) {
        val next = transform(_state.value)
        _state.value = next
        repository.saveWebSearchConfig(toConfig(next))
    }

    private fun readState(): ToolSettingsUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        fromSnapshot(ToolSettingsSnapshot())
    }

    companion object {
        fun toConfig(state: ToolSettingsUiState): WebSearchConfig = WebSearchConfig(
            state.provider,
            state.baseUrl,
            state.apiKey,
            state.model,
            state.queryParam,
            state.apiKeyHeader,
            state.apiKeyParam
        )

        fun fromSnapshot(snapshot: ToolSettingsSnapshot): ToolSettingsUiState {
            val config = snapshot.webSearch
            return ToolSettingsUiState(
                imageUnderstandingLabel = snapshot.imageUnderstandingLabel,
                imageGenerationLabel = snapshot.imageGenerationLabel,
                provider = config.provider,
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                model = config.model,
                queryParam = config.queryParam,
                apiKeyHeader = config.apiKeyHeader,
                apiKeyParam = config.apiKeyParam
            )
        }

        fun factory(repository: ToolSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ToolSettingsViewModel::class.java)) {
                        return ToolSettingsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
