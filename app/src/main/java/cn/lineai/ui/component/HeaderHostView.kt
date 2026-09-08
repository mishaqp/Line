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
import cn.lineai.ui.model.HeaderUiAction
import cn.lineai.ui.model.HeaderViewModel

class HeaderHostView(
    context: Context,
    private val repository: HeaderControllerRepository
) : FrameLayout(context) {
    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val header = ViewModelProvider(
        hostViewModelStoreOwner,
        HeaderViewModel.factory(repository)
    )["chat-header", HeaderViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(header) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        HeaderScreenContent(
                            state = header.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                header.onAction(action)?.let(repository::dispatch)
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun render(snapshot: cn.lineai.ui.model.HeaderSnapshot) {
        header.onAction(HeaderUiAction.Render(snapshot))
    }
}
