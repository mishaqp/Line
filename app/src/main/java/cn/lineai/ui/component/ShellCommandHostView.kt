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
import cn.lineai.ui.model.ShellCommandRepository
import cn.lineai.ui.model.ShellCommandUiAction
import cn.lineai.ui.model.ShellCommandUiEffect
import cn.lineai.ui.model.ShellCommandViewModel

class ShellCommandHostView(
    context: Context,
    repository: ShellCommandRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val viewer = ViewModelProvider(
        hostViewModelStoreOwner,
        ShellCommandViewModel.factory(repository)
    )["shell-command", ShellCommandViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(viewer) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        ShellCommandScreenContent(
                            state = viewer.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (viewer.onAction(action)) {
                                    ShellCommandUiEffect.Back -> listener.onBack()
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
            viewer.onAction(ShellCommandUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
