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
import cn.lineai.ui.model.ComposerAttachmentId
import cn.lineai.ui.model.ComposerAttachmentStripViewModel
import cn.lineai.ui.model.ComposerAttachmentUiAction
import cn.lineai.ui.model.ComposerAttachmentUiEffect

class ComposerAttachmentStripHostView(
    context: Context,
    repository: ComposerAttachmentRepository,
    private val listener: Listener
) : FrameLayout(context) {
    interface Listener {
        fun onAttachmentsChanged(visible: Boolean)
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val attachments = ViewModelProvider(
        hostViewModelStoreOwner,
        ComposerAttachmentStripViewModel.factory(repository)
    )["composer-attachment-strip", ComposerAttachmentStripViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(attachments) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        ComposerAttachmentStripContent(
                            state = attachments.state.collectAsStateWithLifecycle().value,
                            onRemove = ::remove
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun refresh(): Boolean {
        attachments.onAction(ComposerAttachmentUiAction.Refresh)
        return attachments.state.value.visible
    }

    private fun remove(id: ComposerAttachmentId) {
        when (val effect = attachments.onAction(ComposerAttachmentUiAction.Remove(id))) {
            is ComposerAttachmentUiEffect.AttachmentsChanged ->
                listener.onAttachmentsChanged(effect.visible)
            null -> Unit
        }
    }
}
