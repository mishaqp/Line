package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImageUnderstandingModelItemUi(
    val internalId: String,
    val name: String,
    val displayedModelId: String,
    val badgeLabel: String,
    val badgeColor: Int,
    val selected: Boolean
)

data class ImageUnderstandingModelPickerSnapshot(
    val models: List<ImageUnderstandingModelItemUi> = emptyList(),
    val selectedInternalId: String = ""
)

data class ImageUnderstandingModelPickerUiState(
    val models: List<ImageUnderstandingModelItemUi> = emptyList(),
    val selectedInternalId: String = ""
)

sealed interface ImageUnderstandingModelPickerUiAction {
    data object Back : ImageUnderstandingModelPickerUiAction
    data object Reload : ImageUnderstandingModelPickerUiAction
    data class SelectModel(val internalId: String) : ImageUnderstandingModelPickerUiAction
}

sealed interface ImageUnderstandingModelPickerUiEffect {
    data object Back : ImageUnderstandingModelPickerUiEffect
}

interface ImageUnderstandingModelPickerRepository {
    fun snapshot(): ImageUnderstandingModelPickerSnapshot
    fun selectModel(internalId: String)
}

class ImageUnderstandingModelPickerViewModel(
    private val repository: ImageUnderstandingModelPickerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ImageUnderstandingModelPickerUiState> = _state.asStateFlow()

    fun onAction(
        action: ImageUnderstandingModelPickerUiAction
    ): ImageUnderstandingModelPickerUiEffect? = when (action) {
        ImageUnderstandingModelPickerUiAction.Back -> ImageUnderstandingModelPickerUiEffect.Back
        ImageUnderstandingModelPickerUiAction.Reload -> {
            reload()
            null
        }
        is ImageUnderstandingModelPickerUiAction.SelectModel -> {
            repository.selectModel(action.internalId)
            null
        }
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun readState(): ImageUnderstandingModelPickerUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        ImageUnderstandingModelPickerUiState()
    }

    companion object {
        fun fromSnapshot(
            snapshot: ImageUnderstandingModelPickerSnapshot
        ): ImageUnderstandingModelPickerUiState = ImageUnderstandingModelPickerUiState(
            models = snapshot.models,
            selectedInternalId = snapshot.selectedInternalId
        )

        fun factory(
            repository: ImageUnderstandingModelPickerRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ImageUnderstandingModelPickerViewModel::class.java)) {
                        return ImageUnderstandingModelPickerViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
