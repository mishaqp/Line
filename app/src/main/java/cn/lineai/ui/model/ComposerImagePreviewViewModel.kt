package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComposerImagePreviewSnapshot(
    val visible: Boolean = false,
    val uri: String? = null,
    val base64: String = "",
    val mimeType: String = "",
    val name: String = "",
    val revision: Long = 0L
)

data class ComposerImagePreviewUiState(
    val visible: Boolean = false,
    val uri: String? = null,
    val base64: String = "",
    val mimeType: String = "",
    val name: String = "",
    val revision: Long = 0L
) {
    val hasImage: Boolean
        get() = base64.isNotEmpty()
}

sealed interface ComposerImagePreviewUiAction {
    data class Show(
        val uri: String?,
        val base64: String?,
        val mimeType: String?,
        val name: String?
    ) : ComposerImagePreviewUiAction

    data object Clear : ComposerImagePreviewUiAction
}

sealed interface ComposerImagePreviewUiEffect {
    data class ImageStateChanged(val visible: Boolean) : ComposerImagePreviewUiEffect
}

interface ComposerImagePreviewStateRepository {
    fun snapshot(): ComposerImagePreviewSnapshot
    fun show(uri: String?, base64: String?, mimeType: String?, name: String?)
    fun clear()
}

class ComposerImagePreviewViewModel(
    private val repository: ComposerImagePreviewStateRepository
) : ViewModel() {
    private val _state = MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<ComposerImagePreviewUiState> = _state.asStateFlow()

    fun onAction(action: ComposerImagePreviewUiAction): ComposerImagePreviewUiEffect = when (action) {
        is ComposerImagePreviewUiAction.Show -> {
            repository.show(action.uri, action.base64, action.mimeType, action.name)
            refresh()
            ComposerImagePreviewUiEffect.ImageStateChanged(_state.value.visible)
        }

        ComposerImagePreviewUiAction.Clear -> {
            repository.clear()
            refresh()
            ComposerImagePreviewUiEffect.ImageStateChanged(_state.value.visible)
        }
    }

    private fun refresh() {
        _state.value = repository.snapshot().toUiState()
    }

    private fun ComposerImagePreviewSnapshot.toUiState() =
        ComposerImagePreviewUiState(
            visible = visible,
            uri = uri,
            base64 = base64,
            mimeType = mimeType,
            name = name,
            revision = revision
        )

    companion object {
        fun factory(
            repository: ComposerImagePreviewStateRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ComposerImagePreviewViewModel::class.java)) {
                    return ComposerImagePreviewViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
            }
        }
    }
}
