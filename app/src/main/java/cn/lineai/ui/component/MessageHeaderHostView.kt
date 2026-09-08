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
import cn.lineai.ui.model.MessageHeaderUiAction
import cn.lineai.ui.model.MessageHeaderViewModel

class MessageHeaderHostView(
    context: Context,
    repository: MessageHeaderRepository
) : FrameLayout(context) {
    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val header = ViewModelProvider(
        hostViewModelStoreOwner,
        MessageHeaderViewModel.factory(repository)
    )["message-header", MessageHeaderViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(header) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        MessageHeaderContent(
                            state = header.state.collectAsStateWithLifecycle().value
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun bind(name: String?) {
        header.onAction(MessageHeaderUiAction.Bind(name))
    }
}
