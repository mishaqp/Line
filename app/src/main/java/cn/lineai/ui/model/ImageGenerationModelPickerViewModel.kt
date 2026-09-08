package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImageGenerationModelItemUi(
    val internalId: String,
    val name: String,
    val displayedModelId: String,
    val badgeLabel: String,
    val badgeColor: Int,
    val selected: Boolean
)

data class ImageGenerationModelPickerSnapshot(
    val models: List<ImageGenerationModelItemUi> = emptyList(),
    val selectedInternalId: String = ""
)

data class ImageGenerationModelPickerUiState(
    val models: List<ImageGenerationModelItemUi> = emptyList(),
    val selectedInternalId: String = ""
)

sealed interface ImageGenerationModelPickerUiAction {
    data object Back : ImageGenerationModelPickerUiAction
    data object Reload : ImageGenerationModelPickerUiAction
    data class SelectModel(val internalId: String) : ImageGenerationModelPickerUiAction
}

sealed interface ImageGenerationModelPickerUiEffect {
    data object Back : ImageGenerationModelPickerUiEffect
}

interface ImageGenerationModelPickerRepository {
    fun snapshot(): ImageGenerationModelPickerSnapshot
    fun selectModel(internalId: String)
}

class ImageGenerationModelPickerViewModel(
    private val repository: ImageGenerationModelPickerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ImageGenerationModelPickerUiState> = _state.asStateFlow()

    fun onAction(
        action: ImageGenerationModelPickerUiAction
    ): ImageGenerationModelPickerUiEffect? = when (action) {
        ImageGenerationModelPickerUiAction.Back -> ImageGenerationModelPickerUiEffect.Back
        ImageGenerationModelPickerUiAction.Reload -> {
            reload()
            null
        }
        is ImageGenerationModelPickerUiAction.SelectModel -> {
            repository.selectModel(action.internalId)
            null
        }
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun readState(): ImageGenerationModelPickerUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        ImageGenerationModelPickerUiState()
    }

    companion object {
        fun fromSnapshot(
            snapshot: ImageGenerationModelPickerSnapshot
        ): ImageGenerationModelPickerUiState = ImageGenerationModelPickerUiState(
            models = snapshot.models,
            selectedInternalId = snapshot.selectedInternalId
        )

        fun factory(
            repository: ImageGenerationModelPickerRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ImageGenerationModelPickerViewModel::class.java)) {
                        return ImageGenerationModelPickerViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
