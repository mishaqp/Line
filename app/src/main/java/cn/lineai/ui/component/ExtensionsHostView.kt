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
import cn.lineai.ui.model.ExtensionsUiEffect
import cn.lineai.ui.model.ExtensionsViewModel

class ExtensionsHostView(
    context: Context,
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

    private val extensions = ViewModelProvider(
        hostViewModelStoreOwner,
        ExtensionsViewModel.factory()
    )["extensions", ExtensionsViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(extensions) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        ExtensionsScreenContent(
                            state = extensions.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = extensions.onAction(action)) {
                                    ExtensionsUiEffect.Back -> listener.onBack()
                                    is ExtensionsUiEffect.Navigate -> listener.onOpen(effect.destination)
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}
