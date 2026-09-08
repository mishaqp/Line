package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.SkillRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SkillsExtensionListItem(
    val id: String,
    val name: String,
    val locationLabel: String,
    val skillMdPath: String,
    val subtitle: String,
    val enabled: Boolean
)

data class SkillsExtensionsSnapshot(
    val items: List<SkillsExtensionListItem>,
    val suggestedPath: String
)

interface SkillsExtensionsRepository {
    fun snapshot(): SkillsExtensionsSnapshot
    fun setEnabled(extensionId: String, enabled: Boolean)
    fun deleteMany(extensionIds: List<String>)
    fun create(location: String, name: String, description: String, content: String)
    fun installFromPath(location: String, sourcePath: String, optionalName: String)
    fun installFromUri(location: String, uri: String, displayName: String)
    fun installFromGitHub(location: String, githubUrl: String)
}

sealed interface SkillsExtensionsSheet {
    data object Actions : SkillsExtensionsSheet
    data object FileTarget : SkillsExtensionsSheet
    data object GitHub : SkillsExtensionsSheet
    data object Create : SkillsExtensionsSheet
    data object Path : SkillsExtensionsSheet
    data object DeleteConfirm : SkillsExtensionsSheet
}

data class SkillsPendingDocument(
    val uri: String,
    val displayName: String
)

data class SkillsExtensionsUiState(
    val items: List<SkillsExtensionListItem> = emptyList(),
    val suggestedPath: String = "",
    val selectedIds: Set<String> = emptySet(),
    val sheet: SkillsExtensionsSheet? = null,
    val pendingDocument: SkillsPendingDocument? = null,
    val installLocation: String = SkillRecord.LOCATION_PROJECT,
    val githubUrl: String = "",
    val createName: String = "",
    val createDescription: String = "",
    val createContent: String = "",
    val sourcePath: String = "",
    val optionalName: String = "",
    val operationFailed: Boolean = false
) {
    val multiSelect: Boolean get() = selectedIds.isNotEmpty()
    val canEnterMultiSelect: Boolean get() = items.isNotEmpty()
}

sealed interface SkillsExtensionsUiAction {
    data object Back : SkillsExtensionsUiAction
    data object Reload : SkillsExtensionsUiAction
    data object OpenActions : SkillsExtensionsUiAction
    data object Dismiss : SkillsExtensionsUiAction
    data object PickFile : SkillsExtensionsUiAction
    data class DocumentPicked(
        val uri: String,
        val displayName: String
    ) : SkillsExtensionsUiAction
    data object DocumentPickCancelled : SkillsExtensionsUiAction
    data object OpenPathInstallFallback : SkillsExtensionsUiAction
    data class ChooseFileTarget(val location: String) : SkillsExtensionsUiAction
    data object OpenGitHubInstall : SkillsExtensionsUiAction
    data class SetGitHubUrl(val url: String) : SkillsExtensionsUiAction
    data object ConfirmGitHubInstall : SkillsExtensionsUiAction
    data object OpenCreateSkill : SkillsExtensionsUiAction
    data class SetCreateName(val name: String) : SkillsExtensionsUiAction
    data class SetCreateDescription(val description: String) : SkillsExtensionsUiAction
    data class SetCreateContent(val content: String) : SkillsExtensionsUiAction
    data object ConfirmCreateSkill : SkillsExtensionsUiAction
    data object OpenPathInstall : SkillsExtensionsUiAction
    data class SetPath(val path: String) : SkillsExtensionsUiAction
    data class SetOptionalName(val name: String) : SkillsExtensionsUiAction
    data object ConfirmPathInstall : SkillsExtensionsUiAction
    data class SetLocation(val location: String) : SkillsExtensionsUiAction
    data object ShareWorkspace : SkillsExtensionsUiAction
    data class SetEnabled(
        val extensionId: String,
        val enabled: Boolean
    ) : SkillsExtensionsUiAction
    data class EnterMultiSelect(val extensionId: String? = null) : SkillsExtensionsUiAction
    data class ToggleSelection(val extensionId: String) : SkillsExtensionsUiAction
    data object CancelSelection : SkillsExtensionsUiAction
    data object RequestDeleteSelected : SkillsExtensionsUiAction
    data object ConfirmDeleteSelected : SkillsExtensionsUiAction
}

sealed interface SkillsExtensionsUiEffect {
    data object Back : SkillsExtensionsUiEffect
    data object OpenDocumentPicker : SkillsExtensionsUiEffect
    data object ShareWorkspace : SkillsExtensionsUiEffect
    data object InvalidFile : SkillsExtensionsUiEffect
    data object InvalidGitHubUrl : SkillsExtensionsUiEffect
}

class SkillsExtensionsViewModel(
    private val repository: SkillsExtensionsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<SkillsExtensionsUiState> = _state.asStateFlow()

    fun onAction(action: SkillsExtensionsUiAction): SkillsExtensionsUiEffect? = when (action) {
        SkillsExtensionsUiAction.Back -> SkillsExtensionsUiEffect.Back
        SkillsExtensionsUiAction.Reload -> {
            reload()
            null
        }
        SkillsExtensionsUiAction.OpenActions -> {
            openSheet(SkillsExtensionsSheet.Actions)
            null
        }
        SkillsExtensionsUiAction.Dismiss -> {
            dismissSheet()
            null
        }
        SkillsExtensionsUiAction.PickFile -> pickFile()
        is SkillsExtensionsUiAction.DocumentPicked ->
            documentPicked(action.uri, action.displayName)
        SkillsExtensionsUiAction.DocumentPickCancelled -> null
        SkillsExtensionsUiAction.OpenPathInstall,
        SkillsExtensionsUiAction.OpenPathInstallFallback -> {
            openSheet(SkillsExtensionsSheet.Path)
            null
        }
        is SkillsExtensionsUiAction.ChooseFileTarget -> {
            chooseFileTarget(action.location)
            null
        }
        SkillsExtensionsUiAction.OpenGitHubInstall -> {
            openSheet(SkillsExtensionsSheet.GitHub)
            null
        }
        is SkillsExtensionsUiAction.SetGitHubUrl -> {
            _state.value = _state.value.copy(githubUrl = action.url)
            null
        }
        SkillsExtensionsUiAction.ConfirmGitHubInstall -> confirmGitHubInstall()
        SkillsExtensionsUiAction.OpenCreateSkill -> {
            openSheet(SkillsExtensionsSheet.Create)
            null
        }
        is SkillsExtensionsUiAction.SetCreateName -> {
            _state.value = _state.value.copy(createName = action.name)
            null
        }
        is SkillsExtensionsUiAction.SetCreateDescription -> {
            _state.value = _state.value.copy(createDescription = action.description)
            null
        }
        is SkillsExtensionsUiAction.SetCreateContent -> {
            _state.value = _state.value.copy(createContent = action.content)
            null
        }
        SkillsExtensionsUiAction.ConfirmCreateSkill -> {
            confirmCreateSkill()
            null
        }
        is SkillsExtensionsUiAction.SetPath -> {
            _state.value = _state.value.copy(sourcePath = action.path)
            null
        }
        is SkillsExtensionsUiAction.SetOptionalName -> {
            _state.value = _state.value.copy(optionalName = action.name)
            null
        }
        SkillsExtensionsUiAction.ConfirmPathInstall -> {
            confirmPathInstall()
            null
        }
        is SkillsExtensionsUiAction.SetLocation -> {
            setLocation(action.location)
            null
        }
        SkillsExtensionsUiAction.ShareWorkspace -> SkillsExtensionsUiEffect.ShareWorkspace
        is SkillsExtensionsUiAction.SetEnabled -> {
            setEnabled(action.extensionId, action.enabled)
            null
        }
        is SkillsExtensionsUiAction.EnterMultiSelect -> {
            enterMultiSelect(action.extensionId)
            null
        }
        is SkillsExtensionsUiAction.ToggleSelection -> {
            toggleSelection(action.extensionId)
            null
        }
        SkillsExtensionsUiAction.CancelSelection -> {
            cancelSelection()
            null
        }
        SkillsExtensionsUiAction.RequestDeleteSelected -> {
            requestDeleteSelected()
            null
        }
        SkillsExtensionsUiAction.ConfirmDeleteSelected -> {
            confirmDeleteSelected()
            null
        }
    }

    private fun initialState(): SkillsExtensionsUiState = runCatching {
        repository.snapshot().toUiState()
    }.getOrElse {
        SkillsExtensionsUiState(operationFailed = true)
    }

    private fun reload() {
        val previous = _state.value
        _state.value = runCatching {
            repository.snapshot().toUiState(previous)
        }.getOrElse {
            previous.copy(operationFailed = true)
        }
    }

    private fun openSheet(sheet: SkillsExtensionsSheet) {
        _state.value = _state.value.copy(sheet = sheet, operationFailed = false)
    }

    private fun dismissSheet() {
        val current = _state.value
        _state.value = current.copy(
            sheet = null,
            pendingDocument = if (current.sheet == SkillsExtensionsSheet.FileTarget) {
                null
            } else {
                current.pendingDocument
            }
        )
    }

    private fun pickFile(): SkillsExtensionsUiEffect {
        _state.value = _state.value.copy(sheet = null, operationFailed = false)
        return SkillsExtensionsUiEffect.OpenDocumentPicker
    }

    private fun documentPicked(uri: String, displayName: String): SkillsExtensionsUiEffect? {
        if (!isAcceptedSkillName(displayName)) {
            return SkillsExtensionsUiEffect.InvalidFile
        }
        _state.value = _state.value.copy(
            pendingDocument = SkillsPendingDocument(uri, displayName),
            sheet = SkillsExtensionsSheet.FileTarget,
            operationFailed = false
        )
        return null
    }

    private fun chooseFileTarget(location: String) {
        val current = _state.value
        val document = current.pendingDocument ?: return
        val target = normalizeLocation(location)
        runCatching {
            repository.installFromUri(target, document.uri, document.displayName)
        }
        _state.value = current.copy(
            sheet = null,
            pendingDocument = null,
            installLocation = target,
            operationFailed = false
        )
    }

    private fun confirmGitHubInstall(): SkillsExtensionsUiEffect? {
        val current = _state.value
        if (current.sheet != SkillsExtensionsSheet.GitHub) return null
        val url = current.githubUrl.trim()
        if (url.isEmpty()) {
            return SkillsExtensionsUiEffect.InvalidGitHubUrl
        }
        runCatching {
            repository.installFromGitHub(current.installLocation, url)
        }
        _state.value = current.copy(
            sheet = null,
            githubUrl = "",
            operationFailed = false
        )
        return null
    }

    private fun confirmCreateSkill() {
        val current = _state.value
        if (current.sheet != SkillsExtensionsSheet.Create) return
        runCatching {
            repository.create(
                current.installLocation,
                current.createName.trim(),
                current.createDescription.trim(),
                current.createContent.trim()
            )
        }
        _state.value = current.copy(
            sheet = null,
            createName = "",
            createDescription = "",
            createContent = "",
            operationFailed = false
        )
    }

    private fun confirmPathInstall() {
        val current = _state.value
        if (current.sheet != SkillsExtensionsSheet.Path) return
        runCatching {
            repository.installFromPath(
                current.installLocation,
                current.sourcePath.trim(),
                current.optionalName.trim()
            )
        }
        _state.value = current.copy(
            sheet = null,
            sourcePath = "",
            optionalName = "",
            operationFailed = false
        )
    }

    private fun setLocation(location: String) {
        _state.value = _state.value.copy(installLocation = normalizeLocation(location))
    }

    private fun setEnabled(extensionId: String, enabled: Boolean) {
        val current = _state.value
        if (current.items.none { it.id == extensionId }) return
        val result = runCatching {
            repository.setEnabled(extensionId, enabled)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState(current)
        } else {
            runCatching {
                repository.snapshot().toUiState(current).copy(operationFailed = true)
            }.getOrElse {
                current.copy(operationFailed = true)
            }
        }
    }

    private fun enterMultiSelect(extensionId: String?) {
        val current = _state.value
        if (current.items.isEmpty()) return
        val selected = when {
            extensionId != null && current.items.any { it.id == extensionId } ->
                setOf(extensionId)
            current.selectedIds.isNotEmpty() -> current.selectedIds
            else -> setOf(current.items.first().id)
        }
        _state.value = current.copy(
            selectedIds = selected,
            sheet = null,
            operationFailed = false
        )
    }

    private fun toggleSelection(extensionId: String) {
        val current = _state.value
        if (current.selectedIds.isEmpty()) return
        if (current.items.none { it.id == extensionId }) return
        val next = current.selectedIds.toMutableSet()
        if (!next.add(extensionId)) {
            next.remove(extensionId)
        }
        _state.value = current.copy(
            selectedIds = next,
            sheet = if (next.isEmpty() && current.sheet == SkillsExtensionsSheet.DeleteConfirm) {
                null
            } else {
                current.sheet
            }
        )
    }

    private fun cancelSelection() {
        _state.value = _state.value.copy(selectedIds = emptySet(), sheet = null)
    }

    private fun requestDeleteSelected() {
        val current = _state.value
        if (current.selectedIds.isEmpty()) return
        _state.value = current.copy(sheet = SkillsExtensionsSheet.DeleteConfirm)
    }

    private fun confirmDeleteSelected() {
        val current = _state.value
        if (current.sheet != SkillsExtensionsSheet.DeleteConfirm) return
        val ids = current.selectedIds.toList()
        if (ids.isEmpty()) return
        val result = runCatching {
            repository.deleteMany(ids)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState(current).copy(
                selectedIds = emptySet(),
                sheet = null
            )
        } else {
            current.copy(operationFailed = true)
        }
    }

    private fun SkillsExtensionsSnapshot.toUiState(
        previous: SkillsExtensionsUiState? = null
    ): SkillsExtensionsUiState {
        val livingIds = items.map { it.id }.toSet()
        return SkillsExtensionsUiState(
            items = items.toList(),
            suggestedPath = suggestedPath,
            selectedIds = previous?.selectedIds.orEmpty().filter { it in livingIds }.toSet(),
            sheet = previous?.sheet,
            pendingDocument = previous?.pendingDocument,
            installLocation = previous?.installLocation ?: SkillRecord.LOCATION_PROJECT,
            githubUrl = previous?.githubUrl.orEmpty(),
            createName = previous?.createName.orEmpty(),
            createDescription = previous?.createDescription.orEmpty(),
            createContent = previous?.createContent.orEmpty(),
            sourcePath = previous?.sourcePath.orEmpty(),
            optionalName = previous?.optionalName.orEmpty(),
            operationFailed = false
        )
    }

    companion object {
        fun isAcceptedSkillName(displayName: String?): Boolean {
            val name = displayName.orEmpty().trim().lowercase()
            return name.endsWith(".zip") || name.endsWith(".md")
        }

        fun factory(repository: SkillsExtensionsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SkillsExtensionsViewModel::class.java)) {
                        return SkillsExtensionsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }

        private fun normalizeLocation(location: String): String =
            if (location == SkillRecord.LOCATION_APP) {
                SkillRecord.LOCATION_APP
            } else {
                SkillRecord.LOCATION_PROJECT
            }
    }
}
