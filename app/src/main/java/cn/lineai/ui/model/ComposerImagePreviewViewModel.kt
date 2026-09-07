package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ComposerImagePreviewUiState(
    val visible: Boolean = false,
    val uri: String? = null,
    val base64: String = "",
    val mimeType: String = "",
    val name: String = ""
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

class ComposerImagePreviewViewModel : ViewModel() {
    private val _state = MutableStateFlow(ComposerImagePreviewUiState())
    val state: StateFlow<ComposerImagePreviewUiState> = _state.asStateFlow()

    fun onAction(action: ComposerImagePreviewUiAction): ComposerImagePreviewUiEffect = when (action) {
        is ComposerImagePreviewUiAction.Show -> {
            _state.value = ComposerImagePreviewUiState(
                visible = true,
                uri = action.uri,
                base64 = action.base64.orEmpty(),
                mimeType = action.mimeType.orEmpty(),
                name = action.name.orEmpty()
            )
            ComposerImagePreviewUiEffect.ImageStateChanged(true)
        }

        ComposerImagePreviewUiAction.Clear -> {
            _state.value = ComposerImagePreviewUiState()
            ComposerImagePreviewUiEffect.ImageStateChanged(false)
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ComposerImagePreviewViewModel::class.java)) {
                    return ComposerImagePreviewViewModel() as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
            }
        }
    }
}
