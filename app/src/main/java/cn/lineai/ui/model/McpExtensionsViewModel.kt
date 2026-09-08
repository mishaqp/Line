package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class McpExtensionListItem(
    val id: String,
    val name: String,
    val url: String,
    val toolCount: Int,
    val enabled: Boolean
)

data class McpExtensionsSnapshot(
    val items: List<McpExtensionListItem>
)

interface McpExtensionsRepository {
    fun snapshot(): McpExtensionsSnapshot
    fun setEnabled(extensionId: String, enabled: Boolean)
    fun delete(extensionId: String)
}

sealed interface McpExtensionsSheet {
    data class Actions(
        val extensionId: String,
        val extensionName: String
    ) : McpExtensionsSheet

    data class Delete(
        val extensionId: String,
        val extensionName: String
    ) : McpExtensionsSheet
}

data class McpExtensionsUiState(
    val items: List<McpExtensionListItem> = emptyList(),
    val sheet: McpExtensionsSheet? = null,
    val operationInProgress: Boolean = false,
    val operationFailed: Boolean = false
)

sealed interface McpExtensionsUiAction {
    data object Back : McpExtensionsUiAction
    data object Reload : McpExtensionsUiAction
    data object Add : McpExtensionsUiAction
    data class RequestActions(val extensionId: String) : McpExtensionsUiAction
    data class Modify(val extensionId: String) : McpExtensionsUiAction
    data class RequestDelete(val extensionId: String) : McpExtensionsUiAction
    data object ConfirmDelete : McpExtensionsUiAction
    data object DismissSheet : McpExtensionsUiAction
    data class SetEnabled(
        val extensionId: String,
        val enabled: Boolean
    ) : McpExtensionsUiAction
}

sealed interface McpExtensionsUiEffect {
    data object Back : McpExtensionsUiEffect
    data class Navigate(val destination: LineDestination) : McpExtensionsUiEffect
}

class McpExtensionsViewModel(
    private val repository: McpExtensionsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<McpExtensionsUiState> = _state.asStateFlow()

    fun onAction(action: McpExtensionsUiAction): McpExtensionsUiEffect? = when (action) {
        McpExtensionsUiAction.Back -> McpExtensionsUiEffect.Back
        McpExtensionsUiAction.Reload -> {
            reload()
            null
        }
        McpExtensionsUiAction.Add ->
            McpExtensionsUiEffect.Navigate(LineDestination.McpEdit(null))
        is McpExtensionsUiAction.RequestActions -> {
            requestActions(action.extensionId)
            null
        }
        is McpExtensionsUiAction.Modify -> modify(action.extensionId)
        is McpExtensionsUiAction.RequestDelete -> {
            requestDelete(action.extensionId)
            null
        }
        McpExtensionsUiAction.ConfirmDelete -> {
            confirmDelete()
            null
        }
        McpExtensionsUiAction.DismissSheet -> {
            if (!_state.value.operationInProgress) {
                _state.value = _state.value.copy(sheet = null)
            }
            null
        }
        is McpExtensionsUiAction.SetEnabled -> {
            setEnabled(action.extensionId, action.enabled)
            null
        }
    }

    private fun initialState(): McpExtensionsUiState = runCatching {
        repository.snapshot().toUiState()
    }.getOrElse {
        McpExtensionsUiState(operationFailed = true)
    }

    private fun reload() {
        if (_state.value.operationInProgress) return
        val previous = _state.value
        _state.value = runCatching {
            repository.snapshot().toUiState()
        }.getOrElse {
            previous.copy(
                sheet = null,
                operationInProgress = false,
                operationFailed = true
            )
        }
    }

    private fun requestActions(extensionId: String) {
        val current = _state.value
        if (current.operationInProgress) return
        val item = current.items.firstOrNull { it.id == extensionId } ?: return
        _state.value = current.copy(
            sheet = McpExtensionsSheet.Actions(item.id, item.name),
            operationFailed = false
        )
    }

    private fun modify(extensionId: String): McpExtensionsUiEffect? {
        val current = _state.value
        if (current.operationInProgress || current.items.none { it.id == extensionId }) return null
        _state.value = current.copy(sheet = null, operationFailed = false)
        return McpExtensionsUiEffect.Navigate(LineDestination.McpEdit(extensionId))
    }

    private fun requestDelete(extensionId: String) {
        val current = _state.value
        if (current.operationInProgress) return
        val item = current.items.firstOrNull { it.id == extensionId } ?: return
        _state.value = current.copy(
            sheet = McpExtensionsSheet.Delete(item.id, item.name),
            operationFailed = false
        )
    }

    private fun confirmDelete() {
        val current = _state.value
        if (current.operationInProgress) return
        val confirmation = current.sheet as? McpExtensionsSheet.Delete ?: return
        _state.value = current.copy(operationInProgress = true, operationFailed = false)

        val result = runCatching {
            repository.delete(confirmation.extensionId)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState()
        } else {
            val refreshed = runCatching { repository.snapshot() }.getOrNull()
            if (refreshed != null) {
                refreshed.toUiState().copy(
                    sheet = if (refreshed.items.none { it.id == confirmation.extensionId }) {
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

    private fun setEnabled(extensionId: String, enabled: Boolean) {
        val current = _state.value
        if (current.operationInProgress || current.items.none { it.id == extensionId }) return
        _state.value = current.copy(
            sheet = null,
            operationInProgress = true,
            operationFailed = false
        )
        val result = runCatching {
            repository.setEnabled(extensionId, enabled)
            repository.snapshot()
        }
        _state.value = if (result.isSuccess) {
            result.getOrThrow().toUiState()
        } else {
            runCatching {
                repository.snapshot().toUiState().copy(operationFailed = true)
            }.getOrElse {
                current.copy(
                    sheet = null,
                    operationInProgress = false,
                    operationFailed = true
                )
            }
        }
    }

    private fun McpExtensionsSnapshot.toUiState(): McpExtensionsUiState =
        McpExtensionsUiState(
            items = items.toList(),
            sheet = null,
            operationInProgress = false,
            operationFailed = false
        )

    companion object {
        fun factory(repository: McpExtensionsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(McpExtensionsViewModel::class.java)) {
                        return McpExtensionsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
