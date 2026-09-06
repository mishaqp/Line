package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LicenseUiItem(
    val titleResId: Int,
    val meta: String,
    val descriptionResId: Int
)

data class LicensesUiState(
    val licenses: List<LicenseUiItem> = emptyList()
)

sealed interface LicensesUiAction {
    data object Back : LicensesUiAction
}

sealed interface LicensesUiEffect {
    data object Back : LicensesUiEffect
}

class LicensesViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        LicensesUiState(
            licenses = listOf(
                LicenseUiItem(
                    titleResId = R.string.screen_licenses_commonmark_core,
                    meta = "org.commonmark:commonmark:0.28.0 · BSD-2-Clause",
                    descriptionResId = R.string.screen_licenses_commonmark_core_desc
                ),
                LicenseUiItem(
                    titleResId = R.string.screen_licenses_commonmark_gfm,
                    meta = "org.commonmark:commonmark-ext-gfm-tables:0.28.0 · BSD-2-Clause",
                    descriptionResId = R.string.screen_licenses_commonmark_gfm_desc
                ),
                LicenseUiItem(
                    titleResId = R.string.screen_licenses_jsch,
                    meta = "com.github.mwiede:jsch:2.28.2 · Revised BSD / ISC",
                    descriptionResId = R.string.screen_licenses_jsch_desc
                ),
                LicenseUiItem(
                    titleResId = R.string.screen_licenses_lucide,
                    meta = "lucide-react-native:1.14.0 · ISC / MIT",
                    descriptionResId = R.string.screen_licenses_lucide_desc
                )
            )
        )
    )
    val state: StateFlow<LicensesUiState> = _state.asStateFlow()

    fun onAction(action: LicensesUiAction): LicensesUiEffect = when (action) {
        LicensesUiAction.Back -> LicensesUiEffect.Back
    }

    companion object {
        fun factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LicensesViewModel::class.java)) {
                        return LicensesViewModel() as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
