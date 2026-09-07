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
import cn.lineai.ui.model.McpSettingsRepository
import cn.lineai.ui.model.McpSettingsUiAction
import cn.lineai.ui.model.McpSettingsUiEffect
import cn.lineai.ui.model.McpSettingsViewModel

class McpSettingsHostView(
    context: Context,
    repository: McpSettingsRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onOpen(destination: LineDestination)
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val mcpSettings = ViewModelProvider(
        hostViewModelStoreOwner,
        McpSettingsViewModel.factory(repository)
    )["mcp-settings", McpSettingsViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(mcpSettings) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        McpSettingsScreenContent(
                            state = mcpSettings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = mcpSettings.onAction(action)) {
                                    McpSettingsUiEffect.Back -> listener.onBack()
                                    is McpSettingsUiEffect.Navigate -> listener.onOpen(effect.destination)
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
            mcpSettings.onAction(McpSettingsUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
