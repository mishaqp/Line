package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ExtensionsItemKind {
    AGENT,
    MCP,
    SKILLS,
    LINECODE,
    TERMINAL_PROVIDER
}

data class ExtensionsUiItem(
    val kind: ExtensionsItemKind,
    val destination: LineDestination
)

data class ExtensionsUiState(
    val items: List<ExtensionsUiItem> = emptyList()
)

sealed interface ExtensionsUiAction {
    data object Back : ExtensionsUiAction
    data class Open(val destination: LineDestination) : ExtensionsUiAction
}

sealed interface ExtensionsUiEffect {
    data object Back : ExtensionsUiEffect
    data class Navigate(val destination: LineDestination) : ExtensionsUiEffect
}

object ExtensionsCatalog {
    private val items = listOf(
        ExtensionsUiItem(
            kind = ExtensionsItemKind.AGENT,
            destination = LineDestination.Extension("agent")
        ),
        ExtensionsUiItem(
            kind = ExtensionsItemKind.MCP,
            destination = LineDestination.Extension("mcp")
        ),
        ExtensionsUiItem(
            kind = ExtensionsItemKind.SKILLS,
            destination = LineDestination.Extension("skills")
        ),
        ExtensionsUiItem(
            kind = ExtensionsItemKind.LINECODE,
            destination = LineDestination.Extension("linecode")
        ),
        ExtensionsUiItem(
            kind = ExtensionsItemKind.TERMINAL_PROVIDER,
            destination = LineDestination.TerminalProvider
        )
    )

    fun items(): List<ExtensionsUiItem> = items
}

class ExtensionsViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        ExtensionsUiState(items = ExtensionsCatalog.items())
    )
    val state: StateFlow<ExtensionsUiState> = _state.asStateFlow()

    fun onAction(action: ExtensionsUiAction): ExtensionsUiEffect = when (action) {
        ExtensionsUiAction.Back -> ExtensionsUiEffect.Back
        is ExtensionsUiAction.Open -> ExtensionsUiEffect.Navigate(action.destination)
    }

    companion object {
        fun factory(): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ExtensionsViewModel::class.java)) {
                        return ExtensionsViewModel() as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
