package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lineai.model.ContextSizeParser
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelContextParser
import cn.lineai.model.ModelProtocolType
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModelEditorViewModel(
    private val repository: ModelEditorRepository,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val snapshot = repository.loadSnapshot()
    private val presetBaseUrl = snapshot.presetBaseUrl
    private var mainGeneration = 0
    private var compressionGeneration = 0
    private val _state = MutableStateFlow(initialState(snapshot))
    val state = _state.asStateFlow()
    private val _effects = MutableSharedFlow<ModelEditorUiEffect>(extraBufferCapacity = 8)
    val effects = _effects.asSharedFlow()

    fun onAction(action: ModelEditorUiAction): ModelEditorUiEffect? = when (action) {
        ModelEditorUiAction.Back -> ModelEditorUiEffect.Back
        is ModelEditorUiAction.SetName -> updateForm { it.copy(name = action.value) }
        is ModelEditorUiAction.SetBaseUrl -> updateRequestField { it.copy(baseUrl = action.value) }
        is ModelEditorUiAction.SetApiKey -> updateRequestField { it.copy(apiKey = action.value) }
        is ModelEditorUiAction.SetModelId -> updateForm {
            it.copy(mainCatalog = it.mainCatalog.copy(customIdText = action.value))
        }
        is ModelEditorUiAction.SetToolLimit -> updateForm { it.copy(toolCallLimitText = action.value) }
        is ModelEditorUiAction.SetContextSize -> updateForm { it.copy(contextSizeText = action.value) }
        is ModelEditorUiAction.SetLocalContext -> updateForm { it.copy(localContextText = action.value) }
        is ModelEditorUiAction.SelectProvider -> selectProvider(action.index)
        is ModelEditorUiAction.SetCustomModelId -> updateMainCatalogMode(action.enabled)
        ModelEditorUiAction.QueryMainCatalog -> queryCatalog(ModelCatalogKind.MAIN)
        is ModelEditorUiAction.SelectMainModel -> selectCatalogId(ModelCatalogKind.MAIN, action.modelId)
        ModelEditorUiAction.SelectMainCustomRow -> selectCustomRow(ModelCatalogKind.MAIN)
        ModelEditorUiAction.DismissPicker -> updateForm { it.copy(picker = it.picker.copy(visible = false)) }
        is ModelEditorUiAction.SetCompressionEnabled -> updateCompressionMode {
            it.copy(enabled = action.enabled && it.supported)
        }
        is ModelEditorUiAction.SetCompressionAuto -> updateCompressionMode { it.copy(auto = action.auto) }
        is ModelEditorUiAction.SetCompressionCustom -> updateCompressionMode {
            it.copy(catalog = it.catalog.copy(customIdEnabled = action.enabled))
        }
        is ModelEditorUiAction.SetCompressionModelId -> updateCompression {
            it.copy(catalog = it.catalog.copy(customIdText = action.value))
        }
        ModelEditorUiAction.QueryCompressionCatalog -> queryCatalog(ModelCatalogKind.COMPRESSION)
        is ModelEditorUiAction.SelectCompressionModel ->
            selectCatalogId(ModelCatalogKind.COMPRESSION, action.modelId)
        ModelEditorUiAction.SelectCompressionCustomRow -> selectCustomRow(ModelCatalogKind.COMPRESSION)
        ModelEditorUiAction.LocalFilePlaceholder -> ModelEditorUiEffect.LocalPickerPending
        ModelEditorUiAction.Save -> save()
        ModelEditorUiAction.Test -> test()
    }

    private fun selectProvider(index: Int): ModelEditorUiEffect? {
        val current = _state.value
        val selectedIndex = when {
            current.local -> 3
            current.protocol == ModelProtocolType.CODEX_RESPONSES -> 1
            current.protocol == ModelProtocolType.ANTHROPIC_MESSAGES -> 2
            else -> 0
        }
        if (current.lockedPreset && index != selectedIndex) return null
        if (index == 3) return ModelEditorUiEffect.OpenLocalFormHint
        if (current.local) return ModelEditorUiEffect.OpenCustomFormHint
        if (current.lockedPreset) return null
        val next = protocolForIndex(index)
        if (next == current.protocol) return null
        mainGeneration++
        compressionGeneration++
        _state.update {
            val compressionSupported = next.supportsDedicatedCompression()
            it.copy(
                protocol = next,
                mainCatalog = it.mainCatalog.copy(
                    fetchedIds = emptyList(),
                    fetching = false,
                    selectedId = "",
                    customIdText = "",
                    lastRequest = null
                ),
                compression = it.compression.copy(
                    supported = compressionSupported,
                    enabled = if (compressionSupported) it.compression.enabled else false,
                    catalog = it.compression.catalog.copy(
                        fetchedIds = emptyList(),
                        fetching = false,
                        selectedId = "",
                        lastRequest = null
                    )
                ),
                picker = it.picker.copy(visible = false)
            )
        }
        return null
    }

    private fun updateForm(transform: (ModelEditorUiState) -> ModelEditorUiState): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        _state.update(transform)
        return null
    }

    private fun updateRequestField(transform: (ModelEditorUiState) -> ModelEditorUiState): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        mainGeneration++
        compressionGeneration++
        _state.update { current ->
            val next = transform(current)
            next.copy(
                mainCatalog = next.mainCatalog.copy(
                    fetchedIds = emptyList(),
                    fetching = false,
                    selectedId = if (next.mainCatalog.customIdEnabled) next.mainCatalog.selectedId else "",
                    lastRequest = null
                ),
                compression = next.compression.copy(
                    catalog = next.compression.catalog.copy(
                        fetchedIds = emptyList(),
                        fetching = false,
                        selectedId = if (next.compression.catalog.customIdEnabled) {
                            next.compression.catalog.selectedId
                        } else {
                            ""
                        },
                        lastRequest = null
                    )
                ),
                picker = next.picker.copy(visible = false)
            )
        }
        return null
    }

    private fun updateMainCatalogMode(customIdEnabled: Boolean): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        mainGeneration++
        _state.update { current ->
            current.copy(
                mainCatalog = current.mainCatalog.copy(
                    customIdEnabled = customIdEnabled,
                    fetching = false
                ),
                picker = if (current.picker.kind == ModelCatalogKind.MAIN) {
                    current.picker.copy(visible = false)
                } else {
                    current.picker
                }
            )
        }
        return null
    }

    private fun updateCompression(
        transform: (CompressionEditorUiState) -> CompressionEditorUiState
    ): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        _state.update { it.copy(compression = transform(it.compression)) }
        return null
    }

    private fun updateCompressionMode(
        transform: (CompressionEditorUiState) -> CompressionEditorUiState
    ): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        compressionGeneration++
        _state.update { current ->
            val next = transform(current.compression)
            current.copy(
                compression = next.copy(catalog = next.catalog.copy(fetching = false)),
                picker = if (current.picker.kind == ModelCatalogKind.COMPRESSION) {
                    current.picker.copy(visible = false)
                } else {
                    current.picker
                }
            )
        }
        return null
    }

    private fun queryCatalog(kind: ModelCatalogKind): ModelEditorUiEffect? {
        val current = _state.value
        if (current.local || current.isSaving) return null
        val catalog = if (kind == ModelCatalogKind.MAIN) current.mainCatalog else current.compression.catalog
        if (catalog.fetching) return null
        if (!canQuery(kind, current)) return null
        val request = currentRequest(current)
        if (catalog.fetchedIds.isNotEmpty() && catalog.lastRequest == request) {
            _state.update {
                it.copy(
                    picker = ModelPickerUiState(
                        visible = true, kind = kind, ids = catalog.fetchedIds, selectedId = catalog.selectedId
                    )
                )
            }
            return null
        }
        val generation = if (kind == ModelCatalogKind.MAIN) ++mainGeneration else ++compressionGeneration
        _state.update {
            if (kind == ModelCatalogKind.MAIN) it.copy(mainCatalog = it.mainCatalog.copy(fetching = true))
            else it.copy(compression = it.compression.copy(catalog = it.compression.catalog.copy(fetching = true)))
        }
        viewModelScope.launch(workDispatcher) {
            try {
                val ids = repository.fetchModelCatalog(request.protocol, request.baseUrl, request.apiKey)
                applyCatalogResult(kind, generation, request, ids, null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                applyCatalogResult(kind, generation, request, emptyList(), error)
            }
        }
        return null
    }

    private fun applyCatalogResult(
        kind: ModelCatalogKind,
        generation: Int,
        request: CatalogRequestSnapshot,
        ids: List<String>,
        error: Exception?
    ) {
        val currentGeneration = if (kind == ModelCatalogKind.MAIN) mainGeneration else compressionGeneration
        if (generation != currentGeneration) return
        if (currentRequest(_state.value) != request) return
        if (error != null) {
            _state.update { stopFetching(it, kind).copy(picker = it.picker.copy(visible = false)) }
            _effects.tryEmit(ModelEditorUiEffect.QueryFailed(error.message.orEmpty()))
            return
        }
        if (ids.isEmpty()) {
            _state.update { stopFetching(it, kind, ids, request).copy(picker = it.picker.copy(visible = false)) }
            _effects.tryEmit(ModelEditorUiEffect.FetchFailed)
            return
        }
        _state.update { state ->
            val stopped = stopFetching(state, kind, ids, request)
            stopped.copy(
                picker = ModelPickerUiState(
                    visible = true,
                    kind = kind,
                    ids = ids,
                    selectedId = if (kind == ModelCatalogKind.MAIN) {
                        stopped.mainCatalog.selectedId
                    } else {
                        stopped.compression.catalog.selectedId
                    }
                )
            )
        }
    }

    private fun stopFetching(
        state: ModelEditorUiState,
        kind: ModelCatalogKind,
        ids: List<String> = emptyList(),
        request: CatalogRequestSnapshot? = null
    ): ModelEditorUiState {
        return if (kind == ModelCatalogKind.MAIN) {
            state.copy(
                mainCatalog = state.mainCatalog.copy(
                    fetching = false, fetchedIds = ids, lastRequest = request ?: state.mainCatalog.lastRequest
                )
            )
        } else {
            state.copy(
                compression = state.compression.copy(
                    catalog = state.compression.catalog.copy(
                        fetching = false,
                        fetchedIds = ids,
                        lastRequest = request ?: state.compression.catalog.lastRequest
                    )
                )
            )
        }
    }

    private fun selectCatalogId(kind: ModelCatalogKind, modelId: String): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        _state.update { current ->
            if (kind == ModelCatalogKind.MAIN) {
                val fillName = current.name.trim().isEmpty() && current.hasPreset
                current.copy(
                    name = if (fillName) modelId else current.name,
                    mainCatalog = current.mainCatalog.copy(selectedId = modelId, customIdEnabled = false),
                    picker = current.picker.copy(visible = false)
                )
            } else {
                current.copy(
                    compression = current.compression.copy(
                        catalog = current.compression.catalog.copy(selectedId = modelId, customIdEnabled = false)
                    ),
                    picker = current.picker.copy(visible = false)
                )
            }
        }
        return null
    }

    private fun selectCustomRow(kind: ModelCatalogKind): ModelEditorUiEffect? {
        if (_state.value.isSaving) return null
        _state.update { current ->
            if (kind == ModelCatalogKind.MAIN) {
                current.copy(
                    mainCatalog = current.mainCatalog.copy(
                        selectedId = "", customIdEnabled = true, customIdText = ""
                    ),
                    picker = current.picker.copy(visible = false)
                )
            } else {
                current.copy(
                    compression = current.compression.copy(
                        catalog = current.compression.catalog.copy(
                            selectedId = "", customIdEnabled = true, customIdText = ""
                        )
                    ),
                    picker = current.picker.copy(visible = false)
                )
            }
        }
        return null
    }

    private fun save(): ModelEditorUiEffect? {
        val current = _state.value
        if (current.local || current.isSaving) return null
        val modelId = current.effectiveModelId
        var name = current.name.trim()
        if (name.isEmpty()) name = modelId
        if (modelId.isEmpty() || name.isEmpty()) return ModelEditorUiEffect.RequireNameId
        if (current.apiKey.isEmpty() && !canUseCodexOAuth(current)) return ModelEditorUiEffect.RequireApiKey
        val toolLimit = current.parsedToolCallLimit ?: return ModelEditorUiEffect.ToolCallRangeInvalid
        if (!current.compression.ready) return ModelEditorUiEffect.RequireCompactionId
        val config = buildConfig(current, name, modelId, toolLimit, current.parsedContextSize)
        _state.update { it.copy(isSaving = true) }
        return try {
            repository.saveModel(config)
            null
        } catch (error: Exception) {
            _state.update { it.copy(isSaving = false) }
            ModelEditorUiEffect.SaveFailed(error.message.orEmpty())
        }
    }

    private fun test(): ModelEditorUiEffect? {
        val current = _state.value
        if (current.local) return null
        val modelId = current.effectiveModelId
        var name = current.name.trim()
        if (name.isEmpty()) name = modelId
        val toolLimit = current.parsedToolCallLimit ?: ModelConfig.DEFAULT_TOOL_CALL_LIMIT
        if (effectiveBaseUrl(current).isEmpty() ||
            (current.apiKey.isEmpty() && !canUseCodexOAuth(current)) ||
            modelId.isEmpty()
        ) {
            return ModelEditorUiEffect.TestMissing
        }
        repository.testModel(buildConfig(current, name, modelId, toolLimit, current.parsedContextSize))
        return null
    }

    private fun buildConfig(
        current: ModelEditorUiState,
        name: String,
        modelId: String,
        toolLimit: Int,
        contextSize: Int
    ): ModelConfig {
        val label = current.providerLabel ?: current.protocol.label
        return ModelConfig(
            current.editingId,
            name,
            current.protocol,
            label,
            effectiveBaseUrl(current),
            current.apiKey,
            modelId,
            toolLimit,
            current.compression.enabled && current.compression.supported,
            current.compression.auto,
            current.compression.effectiveModelId,
            contextSize
        )
    }

    fun canSave(state: ModelEditorUiState = _state.value): Boolean {
        if (state.local || state.isSaving) return false
        val id = state.effectiveModelId
        return (state.name.isNotEmpty() || id.isNotEmpty()) &&
            id.isNotEmpty() &&
            (state.apiKey.isNotEmpty() || canUseCodexOAuth(state)) &&
            state.parsedToolCallLimit != null &&
            state.compression.ready
    }

    fun canQueryMain(state: ModelEditorUiState = _state.value): Boolean = canQuery(ModelCatalogKind.MAIN, state)
    fun canQueryCompression(state: ModelEditorUiState = _state.value): Boolean =
        canQuery(ModelCatalogKind.COMPRESSION, state)

    private fun canQuery(kind: ModelCatalogKind, state: ModelEditorUiState): Boolean {
        if (state.local || effectiveBaseUrl(state).isEmpty()) return false
        return if (kind == ModelCatalogKind.MAIN) {
            !state.mainCatalog.fetching && (state.apiKey.isNotEmpty() || canUseCodexOAuth(state))
        } else {
            state.compression.enabled && !state.compression.auto &&
                state.apiKey.isNotEmpty() && !state.compression.catalog.fetching
        }
    }

    private fun canUseCodexOAuth(state: ModelEditorUiState): Boolean =
        !state.local && state.protocol == ModelProtocolType.CODEX_RESPONSES && repository.isCodexAuthenticated()

    private fun currentRequest(state: ModelEditorUiState): CatalogRequestSnapshot =
        CatalogRequestSnapshot(state.protocol, effectiveBaseUrl(state), state.apiKey)

    private fun effectiveBaseUrl(state: ModelEditorUiState): String {
        val entered = state.baseUrl.trim()
        if (entered.isNotEmpty()) return entered
        if (presetBaseUrl.isNotEmpty()) return presetBaseUrl
        return defaultBaseUrlFor(state.protocol)
    }

    companion object {
        fun initialContextSizeText(model: ModelConfig?): String {
            if (model == null) return ""
            if (model.contextSize > 0) return ContextSizeParser.format(model.contextSize)
            val trimmed = model.modelId.trim()
            if (trimmed.isEmpty() || !trimmed.endsWith("]")) return ""
            val legacyTokens = ModelContextParser.parse(trimmed).contextTokens
            return if (legacyTokens <= 0) "" else ContextSizeParser.format(legacyTokens)
        }

        fun parseToolCallLimit(value: String): Int? {
            if (value.isEmpty()) return null
            return try {
                val limit = value.toInt()
                if (limit < ModelConfig.UNLIMITED_TOOL_CALLS) null else ModelConfig.normalizeToolCallLimit(limit)
            } catch (_: NumberFormatException) {
                null
            }
        }

        fun protocolForIndex(index: Int): ModelProtocolType = when (index) {
            1 -> ModelProtocolType.CODEX_RESPONSES
            2 -> ModelProtocolType.ANTHROPIC_MESSAGES
            else -> ModelProtocolType.OPENAI_COMPATIBLE
        }

        fun defaultBaseUrlFor(type: ModelProtocolType, baseUrlFallback: String = ""): String {
            if (baseUrlFallback.isNotEmpty()) return baseUrlFallback
            return if (type == ModelProtocolType.ANTHROPIC_MESSAGES) {
                "https://api.anthropic.com"
            } else {
                "https://api.openai.com/v1"
            }
        }

        fun placeholderFor(type: ModelProtocolType): String =
            if (type == ModelProtocolType.ANTHROPIC_MESSAGES) {
                "https://api.example.com/anthropic"
            } else {
                "https://api.example.com/v1"
            }

        fun cleanProviderLabel(label: String?, customSentinel: String): String? {
            if (label.isNullOrEmpty() || label == customSentinel) return null
            return label
        }

        private fun initialState(snapshot: ModelEditorSnapshot): ModelEditorUiState {
            val editingModel = snapshot.editingModel
            val editing = editingModel != null
            val local = snapshot.localRequested ||
                (editing && editingModel!!.protocolType == ModelProtocolType.LOCAL_GGUF)
            val hasPreset = snapshot.presetId != null
            val protocol = when {
                editing -> editingModel!!.protocolType
                local -> ModelProtocolType.LOCAL_GGUF
                snapshot.presetProtocol != null -> snapshot.presetProtocol
                else -> ModelProtocolType.OPENAI_COMPATIBLE
            }
            val compressionSupported = protocol.supportsDedicatedCompression()
            val compressionId = if (editing) editingModel!!.compressionModelId else ""
            return ModelEditorUiState(
                editing = editing,
                editingId = editingModel?.id.orEmpty(),
                local = local,
                lockedPreset = hasPreset || editing,
                hasPreset = hasPreset,
                protocol = protocol,
                providerLabel = when {
                    local -> snapshot.localProviderLabel
                    editing -> snapshot.cleanedEditingProviderLabel
                    hasPreset -> snapshot.presetLabel
                    else -> null
                },
                presetPlaceholder = snapshot.presetPlaceholder,
                presetHint = snapshot.presetHint,
                name = if (editing) editingModel!!.name else "",
                baseUrl = when {
                    editing -> editingModel!!.baseUrl
                    hasPreset -> snapshot.presetBaseUrl
                    else -> ""
                },
                apiKey = if (editing) editingModel!!.apiKey else "",
                toolCallLimitText = (if (editing) editingModel!!.toolCallLimit else ModelConfig.DEFAULT_TOOL_CALL_LIMIT).toString(),
                contextSizeText = if (editing) initialContextSizeText(editingModel) else "",
                mainCatalog = ModelCatalogUiState(
                    selectedId = if (editing) editingModel!!.modelId else "",
                    customIdEnabled = editing,
                    customIdText = if (editing) editingModel!!.modelId else ""
                ),
                compression = CompressionEditorUiState(
                    supported = compressionSupported,
                    enabled = editing && editingModel!!.isCompressionModelEnabled && compressionSupported,
                    auto = !editing || editingModel!!.isCompressionModelAuto,
                    catalog = ModelCatalogUiState(
                        selectedId = compressionId,
                        customIdEnabled = compressionId.isNotEmpty(),
                        customIdText = compressionId
                    )
                ),
                testVisible = !local
            )
        }

        fun factory(
            repository: ModelEditorRepository,
            workDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ModelEditorViewModel::class.java)) {
                    return ModelEditorViewModel(repository, workDispatcher) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
            }
        }
    }
}
