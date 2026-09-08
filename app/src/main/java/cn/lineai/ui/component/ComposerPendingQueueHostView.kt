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
import cn.lineai.ui.model.ComposerPendingQueueUiAction
import cn.lineai.ui.model.ComposerPendingQueueUiEffect
import cn.lineai.ui.model.ComposerPendingQueueViewModel

class ComposerPendingQueueHostView(
    context: Context,
    repository: ComposerQueueRepository,
    private val listener: Listener
) : FrameLayout(context) {
    interface Listener {
        fun onQueueChanged(visible: Boolean)
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val pendingQueue = ViewModelProvider(
        hostViewModelStoreOwner,
        ComposerPendingQueueViewModel.factory(repository)
    )["composer-pending-queue", ComposerPendingQueueViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(pendingQueue) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        ComposerPendingQueueContent(
                            state = pendingQueue.state.collectAsStateWithLifecycle().value,
                            onRemove = { index ->
                                when (
                                    val effect = pendingQueue.onAction(
                                        ComposerPendingQueueUiAction.Remove(index)
                                    )
                                ) {
                                    is ComposerPendingQueueUiEffect.QueueChanged ->
                                        listener.onQueueChanged(effect.visible)
                                    null -> Unit
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun refresh(): Boolean {
        pendingQueue.onAction(ComposerPendingQueueUiAction.Refresh)
        return pendingQueue.state.value.visible
    }
}
