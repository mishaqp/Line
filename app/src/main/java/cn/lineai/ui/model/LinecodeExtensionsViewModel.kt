package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.SkillRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LinecodePackageListItem(
    val id: String,
    val name: String,
    val displayVersion: String,
    val componentCount: Int,
    val subtitle: String,
    val enabled: Boolean
)

data class LinecodeExtensionsSnapshot(
    val items: List<LinecodePackageListItem>,
    val suggestedPath: String
)

interface LinecodeExtensionsRepository {
    fun snapshot(): LinecodeExtensionsSnapshot
    fun setEnabled(extensionId: String, enabled: Boolean)
    fun delete(extensionId: String)
    fun installFromPath(location: String, sourcePath: String)
    fun installFromUri(location: String, uri: String, displayName: String)
}

sealed interface LinecodeExtensionsSheet {
    data object InstallMethods : LinecodeExtensionsSheet
    data object ManualPath : LinecodeExtensionsSheet
    data object FileTarget : LinecodeExtensionsSheet
    data class Delete(
        val packageId: String,
        val packageName: String
    ) : LinecodeExtensionsSheet
}

data class PendingDocument(
    val uri: String,
    val displayName: String
)

data class LinecodeExtensionsUiState(
    val items: List<LinecodePackageListItem> = emptyList(),
    val suggestedPath: String = "",
    val sourcePath: String = "",
    val installLocation: String = SkillRecord.LOCATION_PROJECT,
    val pendingDocument: PendingDocument? = null,
    val sheet: LinecodeExtensionsSheet? = null,
    val operationInProgress: Boolean = false,
    val operationFailed: Boolean = false
)

sealed interface LinecodeExtensionsUiAction {
    data object Back : LinecodeExtensionsUiAction
    data object Reload : LinecodeExtensionsUiAction
    data object OpenInstallMethods : LinecodeExtensionsUiAction
    data object DismissSheet : LinecodeExtensionsUiAction
    data object PickFile : LinecodeExtensionsUiAction
    data class DocumentPicked(
        val uri: String,
        val displayName: String
    ) : LinecodeExtensionsUiAction
    data object DocumentPickCancelled : LinecodeExtensionsUiAction
    data object OpenPathInstall : LinecodeExtensionsUiAction
    data object OpenPathInstallFallback : LinecodeExtensionsUiAction
    data class SetPath(val path: String) : LinecodeExtensionsUiAction
    data class SetInstallLocation(val location: String) : LinecodeExtensionsUiAction
    data object ConfirmPathInstall : LinecodeExtensionsUiAction
    data object ConfirmUriInstall : LinecodeExtensionsUiAction
    data class SetEnabled(
        val packageId: String,
        val enabled: Boolean
    ) : LinecodeExtensionsUiAction
    data class RequestDelete(val packageId: String) : LinecodeExtensionsUiAction
    data object ConfirmDelete : LinecodeExtensionsUiAction
}

sealed interface LinecodeExtensionsUiEffect {
    data object Back : LinecodeExtensionsUiEffect
    data object OpenDocumentPicker : LinecodeExtensionsUiEffect
    data object PathRequired : LinecodeExtensionsUiEffect
    data object InvalidFile : LinecodeExtensionsUiEffect
}

class LinecodeExtensionsViewModel(
    private val repository: LinecodeExtensionsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<LinecodeExtensionsUiState> = _state.asStateFlow()

    fun onAction(action: LinecodeExtensionsUiAction): LinecodeExtensionsUiEffect? = when (action) {
        LinecodeExtensionsUiAction.Back -> LinecodeExtensionsUiEffect.Back
        LinecodeExtensionsUiAction.Reload -> {
            reload()
            null
        }
        LinecodeExtensionsUiAction.OpenInstallMethods -> {
            openSheet(LinecodeExtensionsSheet.InstallMethods)
            null
        }
        LinecodeExtensionsUiAction.DismissSheet -> {
            dismissSheet()
            null
        }
        LinecodeExtensionsUiAction.PickFile -> pickFile()
        is LinecodeExtensionsUiAction.DocumentPicked -> documentPicked(action.uri, action.displayName)
        LinecodeExtensionsUiAction.DocumentPickCancelled -> null
        LinecodeExtensionsUiAction.OpenPathInstall,
        LinecodeExtensionsUiAction.OpenPathInstallFallback -> {
            openSheet(LinecodeExtensionsSheet.ManualPath)
            null
        }
        is LinecodeExtensionsUiAction.SetPath -> {
            if (!_state.value.operationInProgress) {
                _state.value = _state.value.copy(sourcePath = action.path)
            }
            null
        }
        is LinecodeExtensionsUiAction.SetInstallLocation -> {
            setInstallLocation(action.location)
            null
        }
        LinecodeExtensionsUiAction.ConfirmPathInstall -> confirmPathInstall()
        LinecodeExtensionsUiAction.ConfirmUriInstall -> {
            confirmUriInstall()
            null
        }
        is LinecodeExtensionsUiAction.SetEnabled -> {
            setEnabled(action.packageId, action.enabled)
            null
        }
        is LinecodeExtensionsUiAction.RequestDelete -> {
            requestDelete(action.packageId)
            null
        }
        LinecodeExtensionsUiAction.ConfirmDelete -> {
            confirmDelete()
            null
        }
    }

    private fun initialState(): LinecodeExtensionsUiState = runCatching {
        repository.snapshot().toUiState()
    }.getOrElse {
        LinecodeExtensionsUiState(operationFailed = true)
    }

    private fun reload() {
        if (_state.value.operationInProgress) return
        val previous = _state.value
        _state.value = runCatching {
            repository.snapshot().toUiState(
                sourcePath = previous.sourcePath,
                installLocation = previous.installLocation
            )
        }.getOrElse {
            previous.copy(
                sheet = null,
                pendingDocument = null,
                operationInProgress = false,
                operationFailed = true
            )
        }
    }

    private fun openSheet(sheet: LinecodeExtensionsSheet) {
        val current = _state.value
        if (current.operationInProgress) return
        _state.value = current.copy(sheet = sheet, operationFailed = false)
    }

    private fun dismissSheet() {
        val current = _state.value
        if (current.operationInProgress) return
        _state.value = current.copy(sheet = null, pendingDocument = null)
    }

    private fun pickFile(): LinecodeExtensionsUiEffect? {
        val current = _state.value
        if (current.operationInProgress) return null
        _state.value = current.copy(sheet = null, operationFailed = false)
        return LinecodeExtensionsUiEffect.OpenDocumentPicker
    }

    private fun documentPicked(uri: String, displayName: String): LinecodeExtensionsUiEffect? {
        val current = _state.value
        if (current.operationInProgress) return null
        if (!isAcceptedPackageName(displayName)) {
            return LinecodeExtensionsUiEffect.InvalidFile
        }
        _state.value = current.copy(
            pendingDocument = PendingDocument(uri, displayName),
            sheet = LinecodeExtensionsSheet.FileTarget,
            operationFailed = false
        )
        return null
    }

    private fun setInstallLocation(location: String) {
        val current = _state.value
        if (current.operationInProgress) return
        val normalized = when (location) {
            SkillRecord.LOCATION_APP -> SkillRecord.LOCATION_APP
            else -> SkillRecord.LOCATION_PROJECT
        }
        _state.value = current.copy(installLocation = normalized)
    }

    private fun confirmPathInstall(): LinecodeExtensionsUiEffect? {
        val current = _state.value
        if (current.operationInProgress) return null
        val path = current.sourcePath.trim()
        if (path.isEmpty()) {
            return LinecodeExtensionsUiEffect.PathRequired
        }
        runCatching { repository.installFromPath(current.installLocation, path) }
        _state.value = current.copy(
            sheet = null,
            pendingDocument = null,
            sourcePath = "",
            operationFailed = false
        )
        return null
    }

    private fun confirmUriInstall() {
        val current = _state.value
        if (current.operationInProgress) return
        val document = current.pendingDocument ?: return
        runCatching {
            repository.installFromUri(
                current.installLocation,
                document.uri,
                document.displayName
            )
        }
        _state.value = current.copy(
            sheet = null,
            pendingDocument = null,
            operationFailed = false
        )
    }

    private fun requestDelete(packageId: String) {
        val current = _state.value
        if (current.operationInProgress) return
        val item = current.items.firstOrNull { it.id == packageId } ?: return
        _state.value = current.copy(
            sheet = LinecodeExtensionsSheet.Delete(item.id, item.name),
            operationFailed = false
        )
    }

    private fun confirmDelete() {
        val current = _state.value
        if (current.operationInProgress) return
        val confirmation = current.sheet as? LinecodeExtensionsSheet.Delete ?: return
        _state.value = current.copy(operationInProgress = true, operationFailed = false)

        val result = runCatching {
            repository.delete(confirmation.packageId)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState(current.sourcePath, current.installLocation)
        } else {
            val refreshed = runCatching { repository.snapshot() }.getOrNull()
            if (refreshed != null) {
                refreshed.toUiState(current.sourcePath, current.installLocation).copy(
                    sheet = if (refreshed.items.none { it.id == confirmation.packageId }) {
                        null
                    } else {
                        confirmation
                    },
                    operationFailed = true
                )
            } else {
                current.copy(
                    sheet = confirmation,
                    operationInProgress = false,
                    operationFailed = true
                )
            }
        }
    }

    private fun setEnabled(packageId: String, enabled: Boolean) {
        val current = _state.value
        if (current.operationInProgress || current.items.none { it.id == packageId }) return
        _state.value = current.copy(
            sheet = null,
            operationInProgress = true,
            operationFailed = false
        )
        val result = runCatching {
            repository.setEnabled(packageId, enabled)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState(current.sourcePath, current.installLocation)
        } else {
            runCatching {
                repository.snapshot().toUiState(current.sourcePath, current.installLocation)
                    .copy(operationFailed = true)
            }.getOrElse {
                current.copy(
                    sheet = null,
                    operationInProgress = false,
                    operationFailed = true
                )
            }
        }
    }

    private fun LinecodeExtensionsSnapshot.toUiState(
        sourcePath: String = "",
        installLocation: String = SkillRecord.LOCATION_PROJECT
    ): LinecodeExtensionsUiState = LinecodeExtensionsUiState(
        items = items.toList(),
        suggestedPath = suggestedPath,
        sourcePath = sourcePath,
        installLocation = installLocation,
        pendingDocument = null,
        sheet = null,
        operationInProgress = false,
        operationFailed = false
    )

    companion object {
        fun isAcceptedPackageName(displayName: String?): Boolean {
            val name = displayName.orEmpty().trim().lowercase()
            return name.endsWith(".lip") || name.endsWith(".zip")
        }

        fun factory(repository: LinecodeExtensionsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LinecodeExtensionsViewModel::class.java)) {
                        return LinecodeExtensionsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
