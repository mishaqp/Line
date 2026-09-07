package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.model.ModelProviderPresets
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ModelListItemUi(
    val id: String,
    val name: String,
    val modelId: String,
    val badgeLabel: String,
    val badgeColor: Int,
    val selected: Boolean
)

data class ModelAddPresetUi(
    val id: String,
    val protocolType: ModelProtocolType
)

data class ModelManagementUiState(
    val models: List<ModelListItemUi> = emptyList(),
    val selectedModelId: String = "",
    val multiSelectedIds: Set<String> = emptySet(),
    val allowManagement: Boolean = true,
    val pendingActionModelId: String? = null,
    val pendingDelete: Boolean = false,
    val addPresets: List<ModelAddPresetUi> = emptyList()
) {
    val multiSelectActive: Boolean get() = multiSelectedIds.isNotEmpty()

    val pendingActionModel: ModelListItemUi?
        get() = pendingActionModelId?.let { id -> models.firstOrNull { it.id == id } }
}

sealed interface ModelManagementUiAction {
    data object Back : ModelManagementUiAction
    data object AddModel : ModelManagementUiAction
    data class SelectModel(val id: String) : ModelManagementUiAction
    data class LongPressModel(val id: String) : ModelManagementUiAction
    data object DismissActions : ModelManagementUiAction
    data object StartMultiSelect : ModelManagementUiAction
    data object EditPendingModel : ModelManagementUiAction
    data class ToggleMultiSelect(val id: String) : ModelManagementUiAction
    data object ExitMultiSelect : ModelManagementUiAction
    data object RequestDelete : ModelManagementUiAction
    data object ConfirmDelete : ModelManagementUiAction
    data object CancelDelete : ModelManagementUiAction
    data object AddCustom : ModelManagementUiAction
    data object AddLocal : ModelManagementUiAction
    data class AddPreset(val providerId: String) : ModelManagementUiAction
}

/**
 * UDF owner for the Compose model list and add-options screens.
 */
class ModelManagementViewModel(
    private val repository: ModelManagementRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ModelManagementUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { current ->
            val snapshot = repository.models().orEmpty()
            val selectedId = repository.selectedModelId().orEmpty()
            current.copy(
                models = snapshot.map { toItem(it, selectedId) },
                selectedModelId = selectedId,
                multiSelectedIds = current.multiSelectedIds.filter { id ->
                    snapshot.any { it.id == id }
                }.toSet(),
                addPresets = presets()
            )
        }
    }

    fun onAction(action: ModelManagementUiAction): LineDestination? {
        return when (action) {
            ModelManagementUiAction.Back -> null
            ModelManagementUiAction.AddModel -> LineDestination.ModelAddOptions
            ModelManagementUiAction.AddCustom -> LineDestination.ModelAdd
            ModelManagementUiAction.AddLocal -> LineDestination.ModelAddLocal
            is ModelManagementUiAction.AddPreset ->
                LineDestination.ModelAddPreset(action.providerId)

            is ModelManagementUiAction.SelectModel -> {
                if (_state.value.multiSelectActive) {
                    toggleMultiSelect(action.id)
                } else {
                    selectModel(action.id)
                }
                null
            }

            is ModelManagementUiAction.LongPressModel -> {
                if (!_state.value.allowManagement) {
                    return null
                }
                if (_state.value.multiSelectActive) {
                    toggleMultiSelect(action.id)
                } else {
                    _state.update { it.copy(pendingActionModelId = action.id, pendingDelete = false) }
                }
                null
            }

            ModelManagementUiAction.DismissActions -> {
                _state.update { it.copy(pendingActionModelId = null) }
                null
            }

            ModelManagementUiAction.StartMultiSelect -> {
                val id = _state.value.pendingActionModelId
                if (!id.isNullOrBlank()) {
                    _state.update {
                        it.copy(
                            multiSelectedIds = setOf(id),
                            pendingActionModelId = null,
                            pendingDelete = false
                        )
                    }
                }
                null
            }

            ModelManagementUiAction.EditPendingModel -> {
                val id = _state.value.pendingActionModelId ?: return null
                _state.update { it.copy(pendingActionModelId = null) }
                destinationForEdit(id)
            }

            is ModelManagementUiAction.ToggleMultiSelect -> {
                toggleMultiSelect(action.id)
                null
            }

            ModelManagementUiAction.ExitMultiSelect -> {
                _state.update { it.copy(multiSelectedIds = emptySet(), pendingDelete = false) }
                null
            }

            ModelManagementUiAction.RequestDelete -> {
                if (_state.value.multiSelectedIds.isNotEmpty()) {
                    _state.update { it.copy(pendingDelete = true) }
                }
                null
            }

            ModelManagementUiAction.CancelDelete -> {
                _state.update { it.copy(pendingDelete = false) }
                null
            }

            ModelManagementUiAction.ConfirmDelete -> {
                val ids = _state.value.multiSelectedIds.toList()
                if (ids.isNotEmpty()) {
                    repository.deleteModels(ids)
                    _state.update { it.copy(multiSelectedIds = emptySet(), pendingDelete = false) }
                    refresh()
                }
                null
            }
        }
    }

    fun destinationForAdd(providerId: String): LineDestination = when (providerId) {
        ADD_CUSTOM -> LineDestination.ModelAdd
        ADD_LOCAL -> LineDestination.ModelAddLocal
        else -> LineDestination.ModelAddPreset(providerId)
    }

    fun destinationForEdit(modelId: String): LineDestination = LineDestination.ModelEdit(modelId)

    private fun selectModel(id: String) {
        repository.selectModel(id)
        _state.update { current ->
            current.copy(
                selectedModelId = id,
                models = current.models.map { it.copy(selected = it.id == id) }
            )
        }
        refresh()
    }

    private fun toggleMultiSelect(id: String) {
        _state.update { current ->
            val next = current.multiSelectedIds.toMutableSet()
            if (!next.add(id)) {
                next.remove(id)
            }
            current.copy(multiSelectedIds = next, pendingDelete = false)
        }
    }

    private fun initialState(): ModelManagementUiState {
        val snapshot = repository.models().orEmpty()
        val selectedId = repository.selectedModelId().orEmpty()
        return ModelManagementUiState(
            models = snapshot.map { toItem(it, selectedId) },
            selectedModelId = selectedId,
            addPresets = presets()
        )
    }

    private fun presets(): List<ModelAddPresetUi> =
        ModelProviderPresets.all().map { ModelAddPresetUi(it.id, it.protocolType) }

    companion object {
        const val ADD_CUSTOM = "custom"
        const val ADD_LOCAL = "local"

        internal const val CUSTOM_PROVIDER_LABEL = "自定义"

        internal const val BADGE_CODEX = 0xFF4B8BFF.toInt()
        internal const val BADGE_ANTHROPIC = 0xFFB86F50.toInt()
        internal const val BADGE_LOCAL = 0xFF2E7D62.toInt()
        internal const val BADGE_DEFAULT = 0xFF10A37F.toInt()

        fun displayProvider(model: ModelConfig): String {
            val provider = model.providerLabel
            return if (provider.isNullOrEmpty() || CUSTOM_PROVIDER_LABEL == provider) {
                model.protocolType.label
            } else {
                provider
            }
        }

        fun badgeColor(model: ModelConfig): Int = when (model.protocolType) {
            ModelProtocolType.CODEX_RESPONSES -> BADGE_CODEX
            ModelProtocolType.ANTHROPIC_MESSAGES -> BADGE_ANTHROPIC
            ModelProtocolType.LOCAL_GGUF -> BADGE_LOCAL
            ModelProtocolType.OPENAI_COMPATIBLE,
            ModelProtocolType.GROK_RESPONSES -> BADGE_DEFAULT
        }

        private fun toItem(model: ModelConfig, selectedId: String): ModelListItemUi =
            ModelListItemUi(
                id = model.id,
                name = model.name,
                modelId = model.modelId,
                badgeLabel = displayProvider(model),
                badgeColor = badgeColor(model),
                selected = model.id == selectedId
            )

        fun factory(repository: ModelManagementRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ModelManagementViewModel(repository) as T
                }
            }
    }
}
