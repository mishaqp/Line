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
import cn.lineai.ui.model.MessageActionBarUiAction
import cn.lineai.ui.model.MessageActionBarUiEffect
import cn.lineai.ui.model.MessageActionBarViewModel

class MessageActionBarHostView(
    context: Context,
    repository: MessageActionBarRepository,
    private val listener: Listener
) : FrameLayout(context) {
    interface Listener {
        fun onCopy()
        fun onQuote()
        fun onShare()
        fun onSelect()
        fun onMultiSelect()
        fun onRecall()
        fun onMore()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val actions = ViewModelProvider(
        hostViewModelStoreOwner,
        MessageActionBarViewModel.factory(repository)
    )["message-action-bar", MessageActionBarViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(actions) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        MessageActionBarContent(
                            state = actions.state.collectAsStateWithLifecycle().value,
                            onAction = ::dispatch
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun setExpanded(expanded: Boolean) {
        actions.onAction(MessageActionBarUiAction.SetExpanded(expanded))
    }

    fun isExpanded(): Boolean = actions.state.value.expanded

    fun setActionsVisible(visible: Boolean) {
        actions.onAction(MessageActionBarUiAction.SetActionsAllowed(visible))
    }

    private fun dispatch(action: MessageActionBarUiAction) {
        when (actions.onAction(action)) {
            MessageActionBarUiEffect.Copy -> listener.onCopy()
            MessageActionBarUiEffect.Quote -> listener.onQuote()
            MessageActionBarUiEffect.Share -> listener.onShare()
            MessageActionBarUiEffect.Select -> listener.onSelect()
            MessageActionBarUiEffect.MultiSelect -> listener.onMultiSelect()
            MessageActionBarUiEffect.Recall -> listener.onRecall()
            MessageActionBarUiEffect.More -> listener.onMore()
            null -> Unit
        }
    }
}
