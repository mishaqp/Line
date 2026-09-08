package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AdvancedFeatureKind {
    PHONE_CONTROL
}

data class AdvancedFeatureUiItem(
    val kind: AdvancedFeatureKind,
    val destination: LineDestination
)

data class AdvancedFeaturesUiState(
    val features: List<AdvancedFeatureUiItem> = emptyList()
)

sealed interface AdvancedFeaturesUiAction {
    data object Back : AdvancedFeaturesUiAction
    data class Open(val destination: LineDestination) : AdvancedFeaturesUiAction
}

sealed interface AdvancedFeaturesUiEffect {
    data object Back : AdvancedFeaturesUiEffect
    data class Navigate(val destination: LineDestination) : AdvancedFeaturesUiEffect
}

object AdvancedFeaturesCatalog {
    private val features = listOf(
        AdvancedFeatureUiItem(
            kind = AdvancedFeatureKind.PHONE_CONTROL,
            destination = LineDestination.PhoneControl
        )
    )

    fun items(): List<AdvancedFeatureUiItem> = features
}

class AdvancedFeaturesViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        AdvancedFeaturesUiState(features = AdvancedFeaturesCatalog.items())
    )
    val state: StateFlow<AdvancedFeaturesUiState> = _state.asStateFlow()

    fun onAction(action: AdvancedFeaturesUiAction): AdvancedFeaturesUiEffect = when (action) {
        AdvancedFeaturesUiAction.Back -> AdvancedFeaturesUiEffect.Back
        is AdvancedFeaturesUiAction.Open -> AdvancedFeaturesUiEffect.Navigate(action.destination)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AdvancedFeaturesViewModel::class.java)) {
                        return AdvancedFeaturesViewModel() as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
