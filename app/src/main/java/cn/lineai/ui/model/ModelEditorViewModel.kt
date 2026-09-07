package cn.lineai.ui.model

import cn.lineai.model.ContextSizeParser
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType

interface ModelEditorRepository {
    fun loadSnapshot(): ModelEditorSnapshot
    fun isCodexAuthenticated(): Boolean
    @Throws(Exception::class)
    fun fetchModelCatalog(type: ModelProtocolType, baseUrl: String, apiKey: String): List<String>
    fun saveModel(config: ModelConfig)
    fun testModel(config: ModelConfig)
}

data class ModelEditorSnapshot(
    val presetId: String?,
    val presetProtocol: ModelProtocolType?,
    val presetBaseUrl: String,
    val presetPlaceholder: String,
    val presetHint: String,
    val presetLabel: String?,
    val localRequested: Boolean,
    val editingModel: ModelConfig?,
    val cleanedEditingProviderLabel: String?,
    val customProviderLabelSentinel: String,
    val localProviderLabel: String,
    val codexAuthenticated: Boolean
) {
    override fun toString(): String =
        "ModelEditorSnapshot(presetId=$presetId, presetProtocol=$presetProtocol, presetBaseUrl=$presetBaseUrl, localRequested=$localRequested, editingId=${editingModel?.id.orEmpty()}, cleanedEditingProviderLabel=$cleanedEditingProviderLabel, codexAuthenticated=$codexAuthenticated)"
}

data class CatalogRequestSnapshot(
    val protocol: ModelProtocolType,
    val baseUrl: String,
    val apiKey: String
) {
    override fun toString(): String =
        "CatalogRequestSnapshot(protocol=$protocol, baseUrl=$baseUrl, hasApiKey=${apiKey.isNotEmpty()}, apiKeyLength=${apiKey.length})"
}

enum class ModelCatalogKind { MAIN, COMPRESSION }

data class ModelCatalogUiState(
    val fetchedIds: List<String> = emptyList(),
    val fetching: Boolean = false,
    val selectedId: String = "",
    val customIdEnabled: Boolean = false,
    val customIdText: String = "",
    val lastRequest: CatalogRequestSnapshot? = null
) {
    override fun toString(): String =
        "ModelCatalogUiState(fetchedCount=${fetchedIds.size}, fetching=$fetching, selectedId=$selectedId, customIdEnabled=$customIdEnabled, customIdText=$customIdText, lastRequest=$lastRequest)"
}

data class CompressionEditorUiState(
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val auto: Boolean = true,
    val catalog: ModelCatalogUiState = ModelCatalogUiState()
) {
    val ready: Boolean get() = !enabled || auto || effectiveModelId.isNotEmpty()
    val effectiveModelId: String get() = if (catalog.customIdEnabled) catalog.customIdText.trim() else catalog.selectedId.trim()
    val detailsVisible: Boolean get() = supported && enabled
    val manualVisible: Boolean get() = detailsVisible && !auto
    override fun toString(): String =
        "CompressionEditorUiState(supported=$supported, enabled=$enabled, auto=$auto, catalog=$catalog)"
}

data class ModelPickerUiState(
    val visible: Boolean = false,
    val kind: ModelCatalogKind = ModelCatalogKind.MAIN,
    val ids: List<String> = emptyList(),
    val selectedId: String = ""
)

data class ModelEditorUiState(
    val editing: Boolean = false,
    val editingId: String = "",
    val local: Boolean = false,
    val lockedPreset: Boolean = false,
    val hasPreset: Boolean = false,
    val protocol: ModelProtocolType = ModelProtocolType.OPENAI_COMPATIBLE,
    val providerLabel: String? = null,
    val presetPlaceholder: String = "",
    val presetHint: String = "",
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val toolCallLimitText: String = ModelConfig.DEFAULT_TOOL_CALL_LIMIT.toString(),
    val contextSizeText: String = "",
    val localContextText: String = "4096",
    val mainCatalog: ModelCatalogUiState = ModelCatalogUiState(),
    val compression: CompressionEditorUiState = CompressionEditorUiState(),
    val picker: ModelPickerUiState = ModelPickerUiState(),
    val isSaving: Boolean = false,
    val testVisible: Boolean = true
) {
    val effectiveModelId: String
        get() = if (mainCatalog.customIdEnabled) mainCatalog.customIdText.trim() else mainCatalog.selectedId.trim()
    val parsedToolCallLimit: Int? get() = ModelEditorViewModel.parseToolCallLimit(toolCallLimitText)
    val parsedContextSize: Int get() = ContextSizeParser.parse(contextSizeText)
    override fun toString(): String =
        "ModelEditorUiState(editing=$editing, editingId=$editingId, local=$local, lockedPreset=$lockedPreset, hasPreset=$hasPreset, protocol=$protocol, providerLabel=$providerLabel, name=$name, baseUrl=$baseUrl, hasApiKey=${apiKey.isNotEmpty()}, apiKeyLength=${apiKey.length}, toolCallLimitText=$toolCallLimitText, contextSizeText=$contextSizeText, mainCatalog=$mainCatalog, compression=$compression, pickerVisible=${picker.visible}, isSaving=$isSaving)"
}

sealed interface ModelEditorUiAction {
    data object Back : ModelEditorUiAction
    data class SetName(val value: String) : ModelEditorUiAction
    data class SetBaseUrl(val value: String) : ModelEditorUiAction
    data class SetApiKey(val value: String) : ModelEditorUiAction
    data class SetModelId(val value: String) : ModelEditorUiAction
    data class SetToolLimit(val value: String) : ModelEditorUiAction
    data class SetContextSize(val value: String) : ModelEditorUiAction
    data class SelectProvider(val index: Int) : ModelEditorUiAction
    data class SetCustomModelId(val enabled: Boolean) : ModelEditorUiAction
    data object QueryMainCatalog : ModelEditorUiAction
    data class SelectMainModel(val modelId: String) : ModelEditorUiAction
    data object SelectMainCustomRow : ModelEditorUiAction
    data object DismissPicker : ModelEditorUiAction
    data class SetCompressionEnabled(val enabled: Boolean) : ModelEditorUiAction
    data class SetCompressionAuto(val auto: Boolean) : ModelEditorUiAction
    data class SetCompressionCustom(val enabled: Boolean) : ModelEditorUiAction
    data class SetCompressionModelId(val value: String) : ModelEditorUiAction
    data object QueryCompressionCatalog : ModelEditorUiAction
    data class SelectCompressionModel(val modelId: String) : ModelEditorUiAction
    data object SelectCompressionCustomRow : ModelEditorUiAction
    data object LocalFilePlaceholder : ModelEditorUiAction
    data object Save : ModelEditorUiAction
    data object Test : ModelEditorUiAction
}

sealed interface ModelEditorUiEffect {
    data object Back : ModelEditorUiEffect
    data object OpenLocalFormHint : ModelEditorUiEffect
    data object OpenCustomFormHint : ModelEditorUiEffect
    data object LocalPickerPending : ModelEditorUiEffect
    data object RequireNameId : ModelEditorUiEffect
    data object RequireApiKey : ModelEditorUiEffect
    data object ToolCallRangeInvalid : ModelEditorUiEffect
    data object RequireCompactionId : ModelEditorUiEffect
    data object TestMissing : ModelEditorUiEffect
    data object FetchFailed : ModelEditorUiEffect
    data class QueryFailed(val message: String) : ModelEditorUiEffect
    data class SaveFailed(val message: String) : ModelEditorUiEffect
}
