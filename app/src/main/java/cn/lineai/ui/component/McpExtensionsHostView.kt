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
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.McpExtensionsRepository
import cn.lineai.ui.model.McpExtensionsUiAction
import cn.lineai.ui.model.McpExtensionsUiEffect
import cn.lineai.ui.model.McpExtensionsViewModel

class McpExtensionsHostView(
    context: Context,
    repository: McpExtensionsRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onNavigate(destination: LineDestination)
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val mcpExtensions = ViewModelProvider(
        hostViewModelStoreOwner,
        McpExtensionsViewModel.factory(repository)
    )["mcp-extensions", McpExtensionsViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(mcpExtensions) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        McpExtensionsScreenContent(
                            state = mcpExtensions.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = mcpExtensions.onAction(action)) {
                                    McpExtensionsUiEffect.Back -> listener.onBack()
                                    is McpExtensionsUiEffect.Navigate ->
                                        listener.onNavigate(effect.destination)
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (attachedOnce) {
            mcpExtensions.onAction(McpExtensionsUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
