package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TerminalProviderScanUiItem(
    val providerType: String,
    val label: String,
    val packageName: String,
    val serviceClass: String
)

data class TerminalProviderInstalledUiItem(
    val id: String,
    val name: String,
    val packageName: String,
    val enabled: Boolean
)

data class TerminalProvidersSnapshot(
    val installed: List<TerminalProviderInstalledUiItem>,
    val scanResults: List<TerminalProviderScanUiItem>,
    val hasScanned: Boolean
)

interface TerminalProvidersSettingsRepository {
    fun snapshot(): TerminalProvidersSnapshot
    fun scan(): TerminalProvidersSnapshot
    fun addProvider(
        providerType: String,
        name: String,
        packageName: String,
        serviceClass: String,
        enabled: Boolean
    )
    fun setProviderEnabled(providerId: String, enabled: Boolean)
    fun deleteProvider(providerId: String)
}

sealed interface TerminalProvidersConfirmation {
    data class Add(val provider: TerminalProviderScanUiItem) : TerminalProvidersConfirmation
    data class Delete(val providerId: String, val providerName: String) : TerminalProvidersConfirmation
}

data class TerminalProvidersUiState(
    val installed: List<TerminalProviderInstalledUiItem> = emptyList(),
    val scanResults: List<TerminalProviderScanUiItem> = emptyList(),
    val hasScanned: Boolean = false,
    val confirmation: TerminalProvidersConfirmation? = null,
    val operationInProgress: Boolean = false,
    val operationFailed: Boolean = false
)

sealed interface TerminalProvidersUiAction {
    data object Back : TerminalProvidersUiAction
    data object Reload : TerminalProvidersUiAction
    data object Scan : TerminalProvidersUiAction
    data class RequestAdd(val provider: TerminalProviderScanUiItem) : TerminalProvidersUiAction
    data object ConfirmAdd : TerminalProvidersUiAction
    data class RequestDelete(val providerId: String) : TerminalProvidersUiAction
    data object ConfirmDelete : TerminalProvidersUiAction
    data object DismissDialog : TerminalProvidersUiAction
    data class SetEnabled(val providerId: String, val enabled: Boolean) : TerminalProvidersUiAction
}

sealed interface TerminalProvidersUiEffect {
    data object Back : TerminalProvidersUiEffect
}

class TerminalProvidersViewModel(
    private val repository: TerminalProvidersSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<TerminalProvidersUiState> = _state.asStateFlow()

    fun onAction(action: TerminalProvidersUiAction): TerminalProvidersUiEffect? = when (action) {
        TerminalProvidersUiAction.Back -> TerminalProvidersUiEffect.Back
        TerminalProvidersUiAction.Reload -> {
            reload()
            null
        }
        TerminalProvidersUiAction.Scan -> {
            scan()
            null
        }
        is TerminalProvidersUiAction.RequestAdd -> {
            requestAdd(action.provider)
            null
        }
        TerminalProvidersUiAction.ConfirmAdd -> {
            confirmAdd()
            null
        }
        is TerminalProvidersUiAction.RequestDelete -> {
            requestDelete(action.providerId)
            null
        }
        TerminalProvidersUiAction.ConfirmDelete -> {
            confirmDelete()
            null
        }
        TerminalProvidersUiAction.DismissDialog -> {
            if (!_state.value.operationInProgress) {
                _state.value = _state.value.copy(confirmation = null)
            }
            null
        }
        is TerminalProvidersUiAction.SetEnabled -> {
            setEnabled(action.providerId, action.enabled)
            null
        }
    }

    private fun initialState(): TerminalProvidersUiState = runCatching {
        repository.snapshot().toUiState()
    }.getOrElse {
        TerminalProvidersUiState(operationFailed = true)
    }

    private fun reload() {
        if (_state.value.operationInProgress) return
        val previous = _state.value
        _state.value = runCatching {
            repository.snapshot().toUiState()
        }.getOrElse {
            previous.copy(
                confirmation = null,
                operationInProgress = false,
                operationFailed = true
            )
        }
    }

    private fun scan() {
        if (_state.value.operationInProgress) return
        val previous = _state.value
        _state.value = previous.copy(
            confirmation = null,
            operationInProgress = true,
            operationFailed = false
        )
        _state.value = runCatching {
            repository.scan().toUiState()
        }.getOrElse {
            previous.copy(
                confirmation = null,
                operationInProgress = false,
                operationFailed = true
            )
        }
    }

    private fun requestAdd(provider: TerminalProviderScanUiItem) {
        val current = _state.value
        if (current.operationInProgress) return
        val fresh = current.scanResults.firstOrNull {
            it.providerType == provider.providerType &&
                it.packageName == provider.packageName &&
                it.serviceClass == provider.serviceClass
        } ?: return
        _state.value = current.copy(
            confirmation = TerminalProvidersConfirmation.Add(fresh),
            operationFailed = false
        )
    }

    private fun confirmAdd() {
        val current = _state.value
        if (current.operationInProgress) return
        val confirmation = current.confirmation as? TerminalProvidersConfirmation.Add ?: return
        _state.value = current.copy(operationInProgress = true, operationFailed = false)

        val result = runCatching {
            val provider = confirmation.provider
            repository.addProvider(
                providerType = provider.providerType,
                name = provider.label,
                packageName = provider.packageName,
                serviceClass = provider.serviceClass,
                enabled = true
            )
            repository.snapshot()
        }
        if (result.isSuccess) {
            _state.value = result.getOrThrow().toUiState()
            return
        }

        val refreshed = runCatching { repository.snapshot() }.getOrNull()
        val providerWasPersisted = refreshed?.installed?.any {
            it.name == confirmation.provider.label &&
                it.packageName == confirmation.provider.packageName
        } == true
        _state.value = if (refreshed != null) {
            refreshed.toUiState().copy(
                confirmation = if (providerWasPersisted) null else confirmation,
                operationFailed = true
            )
        } else {
            current.copy(
                confirmation = confirmation,
                operationInProgress = false,
                operationFailed = true
            )
        }
    }

    private fun requestDelete(providerId: String) {
        val current = _state.value
        if (current.operationInProgress) return
        val provider = current.installed.firstOrNull { it.id == providerId } ?: return
        _state.value = current.copy(
            confirmation = TerminalProvidersConfirmation.Delete(provider.id, provider.name),
            operationFailed = false
        )
    }

    private fun confirmDelete() {
        val current = _state.value
        if (current.operationInProgress) return
        val confirmation = current.confirmation as? TerminalProvidersConfirmation.Delete ?: return
        _state.value = current.copy(operationInProgress = true, operationFailed = false)

        val result = runCatching {
            repository.deleteProvider(confirmation.providerId)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState()
        } else {
            val refreshed = runCatching { repository.snapshot() }.getOrNull()
            if (refreshed != null) {
                refreshed.toUiState().copy(
                    confirmation = if (refreshed.installed.none { it.id == confirmation.providerId }) {
                        null
                    } else {
                        confirmation
                    },
                    operationFailed = true
                )
            } else {
                current.copy(
                    confirmation = confirmation,
                    operationInProgress = false,
                    operationFailed = true
                )
            }
        }
    }

    private fun setEnabled(providerId: String, enabled: Boolean) {
        val current = _state.value
        if (current.operationInProgress || current.installed.none { it.id == providerId }) return
        _state.value = current.copy(
            confirmation = null,
            operationInProgress = true,
            operationFailed = false
        )
        val result = runCatching {
            repository.setProviderEnabled(providerId, enabled)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState()
        } else {
            runCatching { repository.snapshot().toUiState().copy(operationFailed = true) }
                .getOrElse {
                    current.copy(
                        confirmation = null,
                        operationInProgress = false,
                        operationFailed = true
                    )
                }
        }
    }

    private fun TerminalProvidersSnapshot.toUiState(): TerminalProvidersUiState =
        TerminalProvidersUiState(
            installed = installed.toList(),
            scanResults = scanResults.toList(),
            hasScanned = hasScanned,
            confirmation = null,
            operationInProgress = false,
            operationFailed = false
        )

    companion object {
        fun factory(repository: TerminalProvidersSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(TerminalProvidersViewModel::class.java)) {
                        return TerminalProvidersViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
