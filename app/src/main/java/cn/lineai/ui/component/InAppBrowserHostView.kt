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
import cn.lineai.ui.model.InAppBrowserRepository
import cn.lineai.ui.model.InAppBrowserUiEffect
import cn.lineai.ui.model.InAppBrowserViewModel

class InAppBrowserHostView(
    context: Context,
    repository: InAppBrowserRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val browser = ViewModelProvider(
        hostViewModelStoreOwner,
        InAppBrowserViewModel.factory(repository)
    )["in-app-browser", InAppBrowserViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(browser) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        InAppBrowserScreenContent(
                            state = browser.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (browser.onAction(action)) {
                                    InAppBrowserUiEffect.Back -> listener.onBack()
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
}
