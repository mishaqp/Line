package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface DataSettingsRepository {
    fun exportAll()
    fun importLineCode()
}

data class DataSettingsUiState(
    val actionsEnabled: Boolean = true
)

sealed interface DataSettingsUiAction {
    data object Back : DataSettingsUiAction
    data object ExportAll : DataSettingsUiAction
    data object ImportLineCode : DataSettingsUiAction
}

/**
 * UDF owner for the Data archive screen. Android file pickers and archive
 * implementation stay behind the existing controller callbacks.
 */
class DataSettingsViewModel(
    private val repository: DataSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(DataSettingsUiState())
    val state: StateFlow<DataSettingsUiState> = _state.asStateFlow()

    fun onAction(action: DataSettingsUiAction) {
        when (action) {
            DataSettingsUiAction.Back -> Unit
            DataSettingsUiAction.ExportAll -> repository.exportAll()
            DataSettingsUiAction.ImportLineCode -> repository.importLineCode()
        }
    }

    companion object {
        fun factory(repository: DataSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DataSettingsViewModel(repository) as T
                }
            }
    }
}
