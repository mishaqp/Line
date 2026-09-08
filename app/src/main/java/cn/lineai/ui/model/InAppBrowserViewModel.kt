package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InAppBrowserSnapshot(
    val rawUrl: String? = null,
    val useDefaultTitle: Boolean = true,
    val normalizedUrl: String = "",
    val supported: Boolean = false,
    val javaScriptEnabled: Boolean = false
) {
    override fun toString(): String {
        return "InAppBrowserSnapshot(" +
            "useDefaultTitle=$useDefaultTitle, " +
            "supported=$supported, " +
            "javaScriptEnabled=$javaScriptEnabled, " +
            "raw=${InAppBrowserRedaction.redact(rawUrl)}, " +
            "normalized=${InAppBrowserRedaction.redact(normalizedUrl)})"
    }
}

data class InAppBrowserUiState(
    val useDefaultTitle: Boolean = true,
    val headerUrl: String = "",
    val normalizedUrl: String = "",
    val supported: Boolean = false,
    val javaScriptEnabled: Boolean = false
) {
    override fun toString(): String {
        return "InAppBrowserUiState(" +
            "useDefaultTitle=$useDefaultTitle, " +
            "supported=$supported, " +
            "javaScriptEnabled=$javaScriptEnabled, " +
            "header=${InAppBrowserRedaction.redact(headerUrl)}, " +
            "normalized=${InAppBrowserRedaction.redact(normalizedUrl)})"
    }
}

sealed interface InAppBrowserUiAction {
    data object Back : InAppBrowserUiAction
}

sealed interface InAppBrowserUiEffect {
    data object Back : InAppBrowserUiEffect
}

interface InAppBrowserRepository {
    fun snapshot(): InAppBrowserSnapshot
}

object InAppBrowserRedaction {
    fun redact(raw: String?): String {
        if (raw == null) {
            return "<null>"
        }
        val query = raw.indexOf('?')
        val fragment = raw.indexOf('#')
        val cut = when {
            query >= 0 && fragment >= 0 -> minOf(query, fragment)
            query >= 0 -> query
            fragment >= 0 -> fragment
            else -> -1
        }
        return if (cut < 0) raw else raw.substring(0, cut) + "…"
    }
}

class InAppBrowserViewModel(
    private val repository: InAppBrowserRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readState())
    val state: StateFlow<InAppBrowserUiState> = _state.asStateFlow()

    fun onAction(action: InAppBrowserUiAction): InAppBrowserUiEffect? = when (action) {
        InAppBrowserUiAction.Back -> InAppBrowserUiEffect.Back
    }

    private fun readState(): InAppBrowserUiState = runCatching {
        fromSnapshot(repository.snapshot())
    }.getOrElse {
        InAppBrowserUiState()
    }

    companion object {
        fun fromSnapshot(snapshot: InAppBrowserSnapshot): InAppBrowserUiState =
            InAppBrowserUiState(
                useDefaultTitle = snapshot.useDefaultTitle,
                headerUrl = if (snapshot.useDefaultTitle) "" else snapshot.rawUrl.orEmpty(),
                normalizedUrl = snapshot.normalizedUrl,
                supported = snapshot.supported,
                javaScriptEnabled = snapshot.javaScriptEnabled
            )

        fun factory(repository: InAppBrowserRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(InAppBrowserViewModel::class.java)) {
                        return InAppBrowserViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
                }
            }
    }
}
