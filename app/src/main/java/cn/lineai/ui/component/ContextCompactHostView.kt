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
import cn.lineai.ui.model.ContextCompactUiAction
import cn.lineai.ui.model.ContextCompactViewModel

class ContextCompactHostView(
    context: Context,
    repository: ContextCompactStatusRepository
) : FrameLayout(context) {
    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val compact = ViewModelProvider(
        hostViewModelStoreOwner,
        ContextCompactViewModel.factory(repository)
    )["context-compact", ContextCompactViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(compact) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        ContextCompactContent(
                            state = compact.state.collectAsStateWithLifecycle().value
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun bind(status: String?) {
        compact.onAction(ContextCompactUiAction.Bind(status))
    }
}
