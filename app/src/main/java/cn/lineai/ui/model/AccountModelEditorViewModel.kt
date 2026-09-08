package cn.lineai.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lineai.model.ContextSizeParser
import cn.lineai.model.ModelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ModelEditorIssue {
    MODEL_REQUIRED,
    TOOL_LIMIT_INVALID,
    COMPRESSION_MODEL_REQUIRED
}

data class AccountModelEditorState(
    val providerLabel: String,
    val authenticated: Boolean = false,
    val email: String = "",
    val plan: String = "",
    val loadingModels: Boolean = false,
    val models: List<String> = emptyList(),
    val selectedModelId: String = "",
    val useCustomModelId: Boolean = false,
    val customModelId: String = "",
    val name: String = "",
    val advancedExpanded: Boolean = false,
    val toolCallLimit: String = ModelConfig.DEFAULT_TOOL_CALL_LIMIT.toString(),
    val contextSize: String = "",
    val compressionEnabled: Boolean = false,
    val compressionAuto: Boolean = true,
    val compressionModelId: String = "",
    val loadError: Boolean = false,
    val issue: ModelEditorIssue? = null
) {
    val effectiveModelId: String
        get() = if (useCustomModelId) customModelId.trim() else selectedModelId.trim()
}

class AccountModelEditorViewModel(
    private val repository: AccountRepository,
    private val editingModel: ModelConfig?
) : ViewModel() {
    private val provider = repository.provider
    private val initialModelId = editingModel?.modelId.orEmpty()
    private val initialIdentity = repository.identity()

    private val _state = MutableStateFlow(
        AccountModelEditorState(
            providerLabel = provider.label,
            authenticated = initialIdentity.authenticated,
            email = initialIdentity.email,
            plan = initialIdentity.plan,
            selectedModelId = initialModelId,
            customModelId = initialModelId,
            name = editingModel?.name.orEmpty(),
            toolCallLimit = (editingModel?.toolCallLimit ?: ModelConfig.DEFAULT_TOOL_CALL_LIMIT).toString(),
            contextSize = editingModel?.contextSize?.let(ContextSizeParser::format).orEmpty(),
            compressionEnabled = editingModel?.isCompressionModelEnabled ?: false,
            compressionAuto = editingModel?.isCompressionModelAuto ?: true,
            compressionModelId = editingModel?.compressionModelId.orEmpty()
        )
    )
    val state: StateFlow<AccountModelEditorState> = _state.asStateFlow()

    init {
        refreshModels()
    }

    fun refreshModels() {
        val identity = repository.identity()
        _state.update {
            it.copy(
                authenticated = identity.authenticated,
                email = identity.email,
                plan = identity.plan,
                loadingModels = identity.authenticated,
                loadError = false,
                issue = null
            )
        }
        if (!identity.authenticated) return

        viewModelScope.launch {
            try {
                val ids = repository.fetchModelIds().distinct()
                _state.update { current ->
                    val existing = current.effectiveModelId.ifBlank { initialModelId }
                    val existsInCatalog = existing.isNotBlank() && ids.contains(existing)
                    current.copy(
                        loadingModels = false,
                        models = ids,
                        selectedModelId = if (existsInCatalog) existing else "",
                        useCustomModelId = existing.isNotBlank() && !existsInCatalog,
                        customModelId = existing,
                        loadError = false
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(loadingModels = false, loadError = true) }
            }
        }
    }

    fun selectModel(modelId: String) {
        _state.update {
            it.copy(selectedModelId = modelId, useCustomModelId = false, issue = null)
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value, issue = null) }
    fun setAdvancedExpanded(value: Boolean) = _state.update { it.copy(advancedExpanded = value) }
    fun setUseCustomModelId(value: Boolean) = _state.update { current ->
        current.copy(
            useCustomModelId = value,
            customModelId = if (value && current.customModelId.isBlank()) current.selectedModelId else current.customModelId,
            issue = null
        )
    }
    fun setCustomModelId(value: String) = _state.update { it.copy(customModelId = value, issue = null) }
    fun setToolCallLimit(value: String) = _state.update { it.copy(toolCallLimit = value, issue = null) }
    fun setContextSize(value: String) = _state.update { it.copy(contextSize = value, issue = null) }
    fun setCompressionEnabled(value: Boolean) = _state.update { it.copy(compressionEnabled = value, issue = null) }
    fun setCompressionAuto(value: Boolean) = _state.update { it.copy(compressionAuto = value, issue = null) }
    fun setCompressionModelId(value: String) = _state.update { it.copy(compressionModelId = value, issue = null) }

    fun buildModel(): ModelConfig? {
        val current = _state.value
        val modelId = current.effectiveModelId
        if (modelId.isBlank()) {
            _state.update { it.copy(issue = ModelEditorIssue.MODEL_REQUIRED) }
            return null
        }

        val toolLimit = current.toolCallLimit.trim().toIntOrNull()
        if (toolLimit == null || toolLimit < ModelConfig.UNLIMITED_TOOL_CALLS) {
            _state.update { it.copy(issue = ModelEditorIssue.TOOL_LIMIT_INVALID) }
            return null
        }

        if (current.compressionEnabled && !current.compressionAuto && current.compressionModelId.isBlank()) {
            _state.update { it.copy(issue = ModelEditorIssue.COMPRESSION_MODEL_REQUIRED) }
            return null
        }

        val displayName = current.name.trim().ifBlank { modelId }
        return ModelConfig.builder(
            editingModel?.id.orEmpty(),
            displayName,
            provider.protocolType,
            provider.label,
            provider.baseUrl,
            "",
            modelId
        )
            .toolCallLimit(toolLimit)
            .contextSize(ContextSizeParser.parse(current.contextSize))
            .compressionModelEnabled(current.compressionEnabled)
            .compressionModelAuto(current.compressionAuto)
            .compressionModelId(current.compressionModelId.trim())
            .build()
    }

    companion object {
        @JvmStatic
        fun factory(
            context: Context,
            provider: AccountModelProvider,
            editingModel: ModelConfig?
        ): ViewModelProvider.Factory =
            factory(AndroidAccountRepository(context, provider), editingModel)

        internal fun factory(
            repository: AccountRepository,
            editingModel: ModelConfig?
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountModelEditorViewModel(repository, editingModel) as T
            }
        }
    }
}
