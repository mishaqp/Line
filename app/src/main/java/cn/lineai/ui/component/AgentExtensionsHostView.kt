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
import cn.lineai.ui.model.AgentExtensionsRepository
import cn.lineai.ui.model.AgentExtensionsUiAction
import cn.lineai.ui.model.AgentExtensionsUiEffect
import cn.lineai.ui.model.AgentExtensionsViewModel

class AgentExtensionsHostView(
    context: Context,
    repository: AgentExtensionsRepository,
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
    private val agentExtensions = ViewModelProvider(
        hostViewModelStoreOwner,
        AgentExtensionsViewModel.factory(repository)
    )["agent-extensions", AgentExtensionsViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(agentExtensions) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        AgentExtensionsScreenContent(
                            state = agentExtensions.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = agentExtensions.onAction(action)) {
                                    AgentExtensionsUiEffect.Back -> listener.onBack()
                                    is AgentExtensionsUiEffect.Navigate ->
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
            agentExtensions.onAction(AgentExtensionsUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
