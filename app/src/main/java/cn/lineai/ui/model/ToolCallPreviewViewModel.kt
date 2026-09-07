package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ToolCallPreviewRowUi(
    val renderId: String,
    val categoryLabel: String,
    val running: Boolean = false
)

data class ToolCallPreviewSnapshot(
    val rows: List<ToolCallPreviewRowUi> = emptyList(),
    val registryAvailable: Boolean = false
)

data class ToolCallPreviewUiState(
    val rows: List<ToolCallPreviewRowUi> = emptyList(),
    val registryAvailable: Boolean = false
)

sealed interface ToolCallPreviewUiAction {
    data object Back : ToolCallPreviewUiAction
    data object Reload : ToolCallPreviewUiAction
}

sealed interface ToolCallPreviewUiEffect {
    data object Back : ToolCallPreviewUiEffect
}

interface ToolCallPreviewRepository {
    fun snapshot(): ToolCallPreviewSnapshot
}

class ToolCallPreviewViewModel(
    private val repository: ToolCallPreviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ToolCallPreviewUiState> = _state.asStateFlow()

    fun onAction(action: ToolCallPreviewUiAction): ToolCallPreviewUiEffect? = when (action) {
        ToolCallPreviewUiAction.Back -> ToolCallPreviewUiEffect.Back
        ToolCallPreviewUiAction.Reload -> {
            reload()
            null
        }
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun readState(): ToolCallPreviewUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        ToolCallPreviewUiState()
    }

    companion object {
        fun fromSnapshot(snapshot: ToolCallPreviewSnapshot): ToolCallPreviewUiState =
            ToolCallPreviewUiState(
                rows = snapshot.rows,
                registryAvailable = snapshot.registryAvailable
            )

        fun factory(repository: ToolCallPreviewRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ToolCallPreviewViewModel::class.java)) {
                        return ToolCallPreviewViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
