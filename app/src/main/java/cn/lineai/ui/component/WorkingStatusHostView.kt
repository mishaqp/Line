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
import cn.lineai.ui.model.WorkingStatusUiAction
import cn.lineai.ui.model.WorkingStatusViewModel

class WorkingStatusHostView(
    context: Context,
    repository: WorkingStatusLabelRepository
) : FrameLayout(context) {
    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val status = ViewModelProvider(
        hostViewModelStoreOwner,
        WorkingStatusViewModel.factory(repository)
    )["working-status", WorkingStatusViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(status) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        WorkingStatusContent(
                            state = status.state.collectAsStateWithLifecycle().value
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun bind(thinking: Boolean) {
        status.onAction(WorkingStatusUiAction.Bind(thinking))
    }

    fun startWorking() {
        status.onAction(WorkingStatusUiAction.Start)
    }

    fun stopWorking() {
        status.onAction(WorkingStatusUiAction.Stop)
    }

    fun isWorking(): Boolean = status.state.value.working
}
