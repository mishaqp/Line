package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.ui.model.TerminalProvidersSettingsRepository
import cn.lineai.ui.model.TerminalProvidersUiAction
import cn.lineai.ui.model.TerminalProvidersUiEffect
import cn.lineai.ui.model.TerminalProvidersViewModel

class TerminalProvidersHostView(
    context: Context,
    repository: TerminalProvidersSettingsRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()

    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }

    private val terminalProviders = ViewModelProvider(
        hostViewModelStoreOwner,
        TerminalProvidersViewModel.factory(repository)
    )["terminal-providers", TerminalProvidersViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(terminalProviders) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        TerminalProvidersScreenContent(
                            state = terminalProviders.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (terminalProviders.onAction(action)) {
                                    TerminalProvidersUiEffect.Back -> listener.onBack()
                                    null -> Unit
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun refresh() {
        terminalProviders.onAction(TerminalProvidersUiAction.Reload)
    }
}
