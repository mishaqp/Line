package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.MemoryOverviewState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MemoryEditableSection {
    LONG_TERM,
    PROJECT,
    ENVIRONMENT
}

sealed interface MemoryDialogState {
    data class MemoryDetail(
        val section: MemoryEditableSection,
        val memory: MemoryOverviewState.Memory
    ) : MemoryDialogState

    data class WorkingDetail(
        val memory: MemoryOverviewState.WorkingMemory
    ) : MemoryDialogState

    data class HistoryDetail(
        val entry: MemoryOverviewState.HistoryEntry
    ) : MemoryDialogState

    data class Actions(
        val memory: MemoryOverviewState.Memory
    ) : MemoryDialogState

    data class Editor(
        val editingId: String?,
        val draftContent: String,
        val draftScope: String
    ) : MemoryDialogState

    data class DeleteConfirm(
        val memory: MemoryOverviewState.Memory
    ) : MemoryDialogState

    data class BatchDeleteConfirm(
        val ids: Set<String>
    ) : MemoryDialogState
}

data class MemoryUiState(
    val overview: MemoryOverviewState,
    val isMultiSelect: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val dialog: MemoryDialogState? = null
)

sealed interface MemoryUiAction {
    data object Back : MemoryUiAction
    data object Reload : MemoryUiAction
    data object OpenAddEditor : MemoryUiAction
    data class OpenMemoryDetail(
        val section: MemoryEditableSection,
        val id: String
    ) : MemoryUiAction
    data class OpenWorkingDetail(val id: String) : MemoryUiAction
    data class OpenHistoryDetail(val id: String) : MemoryUiAction
    data class OpenActions(val id: String) : MemoryUiAction
    data object EditActionMemory : MemoryUiAction
    data object MultiSelectActionMemory : MemoryUiAction
    data object DeleteActionMemory : MemoryUiAction
    data object DismissDialog : MemoryUiAction
    data class SetDraftContent(val content: String) : MemoryUiAction
    data class SetDraftScope(val scope: String) : MemoryUiAction
    data object SaveEditor : MemoryUiAction
    data object ConfirmDelete : MemoryUiAction
    data class ToggleSelected(val id: String) : MemoryUiAction
    data object ExitMultiSelect : MemoryUiAction
    data object OpenBatchDeleteConfirm : MemoryUiAction
    data object ConfirmBatchDelete : MemoryUiAction
}

sealed interface MemoryUiEffect {
    data object Back : MemoryUiEffect
    data object EmptyContent : MemoryUiEffect
}

interface MemorySettingsRepository {
    fun getMemoryOverview(): MemoryOverviewState
    fun onMemorySaved(id: String, scope: String, content: String)
    fun onMemoryDeleted(id: String)
    fun onMemoriesDeleted(ids: List<String>)
}

class MemorySettingsViewModel(
    private val repository: MemorySettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        MemoryUiState(overview = repository.getMemoryOverview())
    )
    val state: StateFlow<MemoryUiState> = _state.asStateFlow()

    fun onAction(action: MemoryUiAction): MemoryUiEffect? = when (action) {
        MemoryUiAction.Back -> MemoryUiEffect.Back
        MemoryUiAction.Reload -> {
            reloadOverview()
            null
        }
        MemoryUiAction.OpenAddEditor -> {
            if (!_state.value.isMultiSelect) {
                _state.value = _state.value.copy(
                    dialog = MemoryDialogState.Editor(
                        editingId = null,
                        draftContent = "",
                        draftScope = MemoryOverviewState.Memory.SCOPE_USER
                    )
                )
            }
            null
        }
        is MemoryUiAction.OpenMemoryDetail -> {
            if (_state.value.isMultiSelect) {
                toggleSelected(action.id)
            } else {
                findEditableMemory(action.id)?.let { memory ->
                    _state.value = _state.value.copy(
                        dialog = MemoryDialogState.MemoryDetail(action.section, memory)
                    )
                }
            }
            null
        }
        is MemoryUiAction.OpenWorkingDetail -> {
            _state.value.overview.shortTerm.firstOrNull { it.id == action.id }?.let { memory ->
                _state.value = _state.value.copy(
                    dialog = MemoryDialogState.WorkingDetail(memory)
                )
            }
            null
        }
        is MemoryUiAction.OpenHistoryDetail -> {
            _state.value.overview.history.firstOrNull { it.id == action.id }?.let { entry ->
                _state.value = _state.value.copy(
                    dialog = MemoryDialogState.HistoryDetail(entry)
                )
            }
            null
        }
        is MemoryUiAction.OpenActions -> {
            if (_state.value.isMultiSelect) {
                toggleSelected(action.id)
            } else {
                findEditableMemory(action.id)?.let { memory ->
                    _state.value = _state.value.copy(dialog = MemoryDialogState.Actions(memory))
                }
            }
            null
        }
        MemoryUiAction.EditActionMemory -> {
            val memory = (_state.value.dialog as? MemoryDialogState.Actions)?.memory
            if (memory != null) {
                _state.value = _state.value.copy(
                    dialog = MemoryDialogState.Editor(
                        editingId = memory.id,
                        draftContent = memory.content,
                        draftScope = normalizeScope(memory.scope)
                    )
                )
            }
            null
        }
        MemoryUiAction.MultiSelectActionMemory -> {
            val memory = (_state.value.dialog as? MemoryDialogState.Actions)?.memory
            if (memory != null) {
                _state.value = _state.value.copy(
                    isMultiSelect = true,
                    selectedIds = setOf(memory.id),
                    dialog = null
                )
            }
            null
        }
        MemoryUiAction.DeleteActionMemory -> {
            val memory = (_state.value.dialog as? MemoryDialogState.Actions)?.memory
            if (memory != null) {
                _state.value = _state.value.copy(
                    dialog = MemoryDialogState.DeleteConfirm(memory)
                )
            }
            null
        }
        MemoryUiAction.DismissDialog -> {
            _state.value = _state.value.copy(dialog = null)
            null
        }
        is MemoryUiAction.SetDraftContent -> {
            updateEditor { it.copy(draftContent = action.content) }
            null
        }
        is MemoryUiAction.SetDraftScope -> {
            updateEditor { it.copy(draftScope = normalizeScope(action.scope)) }
            null
        }
        MemoryUiAction.SaveEditor -> saveEditor()
        MemoryUiAction.ConfirmDelete -> {
            confirmDelete()
            null
        }
        is MemoryUiAction.ToggleSelected -> {
            toggleSelected(action.id)
            null
        }
        MemoryUiAction.ExitMultiSelect -> {
            _state.value = _state.value.copy(
                isMultiSelect = false,
                selectedIds = emptySet(),
                dialog = null
            )
            null
        }
        MemoryUiAction.OpenBatchDeleteConfirm -> {
            val selected = _state.value.selectedIds
            if (_state.value.isMultiSelect && selected.isNotEmpty()) {
                _state.value = _state.value.copy(
                    dialog = MemoryDialogState.BatchDeleteConfirm(selected.toSet())
                )
            }
            null
        }
        MemoryUiAction.ConfirmBatchDelete -> {
            confirmBatchDelete()
            null
        }
    }

    private fun saveEditor(): MemoryUiEffect? {
        val editor = _state.value.dialog as? MemoryDialogState.Editor ?: return null
        val content = editor.draftContent.trim()
        if (content.isEmpty()) {
            return MemoryUiEffect.EmptyContent
        }
        repository.onMemorySaved(
            editor.editingId.orEmpty(),
            normalizeScope(editor.draftScope),
            content
        )
        replaceOverview(
            overview = repository.getMemoryOverview(),
            dialog = null
        )
        return null
    }

    private fun confirmDelete() {
        val delete = _state.value.dialog as? MemoryDialogState.DeleteConfirm ?: return
        repository.onMemoryDeleted(delete.memory.id)
        val remaining = _state.value.selectedIds - delete.memory.id
        replaceOverview(
            overview = repository.getMemoryOverview(),
            isMultiSelect = remaining.isNotEmpty(),
            selectedIds = remaining,
            dialog = null
        )
    }

    private fun confirmBatchDelete() {
        val confirm = _state.value.dialog as? MemoryDialogState.BatchDeleteConfirm ?: return
        if (confirm.ids.isEmpty()) return
        repository.onMemoriesDeleted(confirm.ids.toList())
        replaceOverview(
            overview = repository.getMemoryOverview(),
            isMultiSelect = false,
            selectedIds = emptySet(),
            dialog = null
        )
    }

    private fun toggleSelected(id: String) {
        if (findEditableMemory(id) == null) return
        val current = _state.value
        val selected = if (id in current.selectedIds) {
            current.selectedIds - id
        } else {
            current.selectedIds + id
        }
        _state.value = current.copy(
            isMultiSelect = selected.isNotEmpty(),
            selectedIds = selected,
            dialog = null
        )
    }

    private fun updateEditor(
        transform: (MemoryDialogState.Editor) -> MemoryDialogState.Editor
    ) {
        val editor = _state.value.dialog as? MemoryDialogState.Editor ?: return
        _state.value = _state.value.copy(dialog = transform(editor))
    }

    private fun reloadOverview() {
        val overview = repository.getMemoryOverview()
        val validIds = editableMemories(overview).mapTo(HashSet()) { it.id }
        val selected = _state.value.selectedIds.filterTo(LinkedHashSet()) { it in validIds }
        replaceOverview(
            overview = overview,
            isMultiSelect = selected.isNotEmpty(),
            selectedIds = selected
        )
    }

    private fun replaceOverview(
        overview: MemoryOverviewState,
        isMultiSelect: Boolean = _state.value.isMultiSelect,
        selectedIds: Set<String> = _state.value.selectedIds,
        dialog: MemoryDialogState? = _state.value.dialog
    ) {
        _state.value = _state.value.copy(
            overview = overview,
            isMultiSelect = isMultiSelect,
            selectedIds = selectedIds.toSet(),
            dialog = dialog
        )
    }

    private fun findEditableMemory(id: String): MemoryOverviewState.Memory? =
        editableMemories(_state.value.overview).firstOrNull { it.id == id }

    private fun editableMemories(
        overview: MemoryOverviewState
    ): Sequence<MemoryOverviewState.Memory> = sequence {
        yieldAll(overview.longTerm)
        yieldAll(overview.project)
        yieldAll(overview.environment)
    }

    private fun normalizeScope(scope: String): String = when (scope) {
        MemoryOverviewState.Memory.SCOPE_PROJECT -> MemoryOverviewState.Memory.SCOPE_PROJECT
        MemoryOverviewState.Memory.SCOPE_ENVIRONMENT -> MemoryOverviewState.Memory.SCOPE_ENVIRONMENT
        else -> MemoryOverviewState.Memory.SCOPE_USER
    }

    companion object {
        fun factory(repository: MemorySettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MemorySettingsViewModel::class.java)) {
                        return MemorySettingsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
