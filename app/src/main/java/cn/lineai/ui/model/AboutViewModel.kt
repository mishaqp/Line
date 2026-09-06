package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AboutAppInfo(
    val appLabel: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null
)

interface AboutRepository {
    fun loadAppInfo(): AboutAppInfo
}

data class AboutUiState(
    val appLabel: String = "LineCode Pro",
    val versionName: String = "unknown",
    val versionCode: Long = 0L
)

sealed interface AboutUiAction {
    data object Back : AboutUiAction
    data object OpenGithub : AboutUiAction
    data object OpenLicenses : AboutUiAction
}

sealed interface AboutUiEffect {
    data object Back : AboutUiEffect
    data object OpenGithub : AboutUiEffect
    data class OpenDestination(val destination: LineDestination) : AboutUiEffect
}

class AboutViewModel(
    private val repository: AboutRepository
) : ViewModel() {
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AboutUiState> = _state.asStateFlow()

    fun onAction(action: AboutUiAction): AboutUiEffect = when (action) {
        AboutUiAction.Back -> AboutUiEffect.Back
        AboutUiAction.OpenGithub -> AboutUiEffect.OpenGithub
        AboutUiAction.OpenLicenses -> AboutUiEffect.OpenDestination(LineDestination.Licenses)
    }

    private fun loadState(): AboutUiState {
        val info = try {
            repository.loadAppInfo()
        } catch (_: Exception) {
            AboutAppInfo()
        }
        return AboutUiState(
            appLabel = info.appLabel?.takeIf { it.isNotEmpty() } ?: "LineCode Pro",
            versionName = info.versionName?.takeIf { it.isNotEmpty() } ?: "unknown",
            versionCode = info.versionCode ?: 0L
        )
    }

    companion object {
        fun factory(repository: AboutRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AboutViewModel::class.java)) {
                        return AboutViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
