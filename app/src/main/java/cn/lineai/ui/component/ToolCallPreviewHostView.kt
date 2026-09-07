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
import cn.lineai.ui.model.ToolCallPreviewRepository
import cn.lineai.ui.model.ToolCallPreviewUiAction
import cn.lineai.ui.model.ToolCallPreviewUiEffect
import cn.lineai.ui.model.ToolCallPreviewViewModel

class ToolCallPreviewHostView(
    context: Context,
    repository: ToolCallPreviewRepository,
    renderer: ToolCallPreviewCardRenderer,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val preview = ViewModelProvider(
        hostViewModelStoreOwner,
        ToolCallPreviewViewModel.factory(repository)
    )["tool-call-preview", ToolCallPreviewViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(preview) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        ToolCallPreviewScreenContent(
                            state = preview.state.collectAsStateWithLifecycle().value,
                            renderer = renderer,
                            onAction = { action ->
                                when (preview.onAction(action)) {
                                    ToolCallPreviewUiEffect.Back -> listener.onBack()
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
            preview.onAction(ToolCallPreviewUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
