package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AgentExtensionListItem(
    val id: String,
    val name: String,
    val slug: String,
    val toolCount: Int,
    val enabled: Boolean
)

data class AgentExtensionsSnapshot(
    val items: List<AgentExtensionListItem>
)

interface AgentExtensionsRepository {
    fun snapshot(): AgentExtensionsSnapshot
    fun setEnabled(extensionId: String, enabled: Boolean)
    fun delete(extensionId: String)
}

sealed interface AgentExtensionsSheet {
    data class Actions(
        val extensionId: String,
        val extensionName: String
    ) : AgentExtensionsSheet

    data class Delete(
        val extensionId: String,
        val extensionName: String
    ) : AgentExtensionsSheet
}

data class AgentExtensionsUiState(
    val items: List<AgentExtensionListItem> = emptyList(),
    val sheet: AgentExtensionsSheet? = null,
    val operationInProgress: Boolean = false,
    val operationFailed: Boolean = false
)

sealed interface AgentExtensionsUiAction {
    data object Back : AgentExtensionsUiAction
    data object Reload : AgentExtensionsUiAction
    data object Add : AgentExtensionsUiAction
    data class RequestActions(val extensionId: String) : AgentExtensionsUiAction
    data class Modify(val extensionId: String) : AgentExtensionsUiAction
    data class RequestDelete(val extensionId: String) : AgentExtensionsUiAction
    data object ConfirmDelete : AgentExtensionsUiAction
    data object DismissSheet : AgentExtensionsUiAction
    data class SetEnabled(
        val extensionId: String,
        val enabled: Boolean
    ) : AgentExtensionsUiAction
}

sealed interface AgentExtensionsUiEffect {
    data object Back : AgentExtensionsUiEffect
    data class Navigate(val destination: LineDestination) : AgentExtensionsUiEffect
}

class AgentExtensionsViewModel(
    private val repository: AgentExtensionsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<AgentExtensionsUiState> = _state.asStateFlow()

    fun onAction(action: AgentExtensionsUiAction): AgentExtensionsUiEffect? = when (action) {
        AgentExtensionsUiAction.Back -> AgentExtensionsUiEffect.Back
        AgentExtensionsUiAction.Reload -> {
            reload()
            null
        }
        AgentExtensionsUiAction.Add ->
            AgentExtensionsUiEffect.Navigate(LineDestination.AgentEdit(null))
        is AgentExtensionsUiAction.RequestActions -> {
            requestActions(action.extensionId)
            null
        }
        is AgentExtensionsUiAction.Modify -> modify(action.extensionId)
        is AgentExtensionsUiAction.RequestDelete -> {
            requestDelete(action.extensionId)
            null
        }
        AgentExtensionsUiAction.ConfirmDelete -> {
            confirmDelete()
            null
        }
        AgentExtensionsUiAction.DismissSheet -> {
            if (!_state.value.operationInProgress) {
                _state.value = _state.value.copy(sheet = null)
            }
            null
        }
        is AgentExtensionsUiAction.SetEnabled -> {
            setEnabled(action.extensionId, action.enabled)
            null
        }
    }

    private fun initialState(): AgentExtensionsUiState = runCatching {
        repository.snapshot().toUiState()
    }.getOrElse {
        AgentExtensionsUiState(operationFailed = true)
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
            sheet = AgentExtensionsSheet.Actions(item.id, item.name),
            operationFailed = false
        )
    }

    private fun modify(extensionId: String): AgentExtensionsUiEffect? {
        val current = _state.value
        if (current.operationInProgress || current.items.none { it.id == extensionId }) return null
        _state.value = current.copy(sheet = null, operationFailed = false)
        return AgentExtensionsUiEffect.Navigate(LineDestination.AgentEdit(extensionId))
    }

    private fun requestDelete(extensionId: String) {
        val current = _state.value
        if (current.operationInProgress) return
        val item = current.items.firstOrNull { it.id == extensionId } ?: return
        _state.value = current.copy(
            sheet = AgentExtensionsSheet.Delete(item.id, item.name),
            operationFailed = false
        )
    }

    private fun confirmDelete() {
        val current = _state.value
        if (current.operationInProgress) return
        val confirmation = current.sheet as? AgentExtensionsSheet.Delete ?: return
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

    private fun AgentExtensionsSnapshot.toUiState(): AgentExtensionsUiState =
        AgentExtensionsUiState(
            items = items.toList(),
            sheet = null,
            operationInProgress = false,
            operationFailed = false
        )

    companion object {
        fun factory(repository: AgentExtensionsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AgentExtensionsViewModel::class.java)) {
                        return AgentExtensionsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
