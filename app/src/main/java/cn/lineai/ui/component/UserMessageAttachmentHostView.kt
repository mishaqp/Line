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
import cn.lineai.model.InputAttachment
import cn.lineai.ui.model.UserMessageAttachmentUiAction
import cn.lineai.ui.model.UserMessageAttachmentViewModel

class UserMessageAttachmentHostView(
    context: Context,
    repository: UserMessageAttachmentRepository
) : FrameLayout(context) {
    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val attachments = ViewModelProvider(
        hostViewModelStoreOwner,
        UserMessageAttachmentViewModel.factory(repository)
    )["user-message-attachments", UserMessageAttachmentViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(attachments) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        UserMessageAttachmentContent(
                            state = attachments.state
                                .collectAsStateWithLifecycle()
                                .value
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun bind(next: List<InputAttachment>?): Boolean {
        attachments.onAction(UserMessageAttachmentUiAction.Bind(next))
        return attachments.state.value.visible
    }
}
