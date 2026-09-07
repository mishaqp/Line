package cn.lineai.ui.component

import android.content.Context
import android.net.Uri
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.ui.model.ComposerImagePreviewUiAction
import cn.lineai.ui.model.ComposerImagePreviewUiEffect
import cn.lineai.ui.model.ComposerImagePreviewViewModel

class ComposerImagePreviewHostView(
    context: Context,
    private val listener: Listener
) : FrameLayout(context) {
    interface Listener {
        fun onImageStateChanged(visible: Boolean)
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val preview = ViewModelProvider(
        hostViewModelStoreOwner,
        ComposerImagePreviewViewModel.factory()
    )["composer-image-preview", ComposerImagePreviewViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(preview) {
                            onDispose { hostViewModelStore.clear() }
                        }
                        ComposerImagePreviewContent(
                            state = preview.state.collectAsStateWithLifecycle().value,
                            onRemove = { clear() }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun show(
        uri: Uri?,
        encodedBase64: String?,
        encodedMimeType: String?,
        displayName: String?
    ): Boolean {
        dispatch(
            ComposerImagePreviewUiAction.Show(
                uri = uri?.toString(),
                base64 = encodedBase64,
                mimeType = encodedMimeType,
                name = displayName
            )
        )
        return preview.state.value.visible
    }

    fun clear(): Boolean {
        dispatch(ComposerImagePreviewUiAction.Clear)
        return preview.state.value.visible
    }

    fun hasImage(): Boolean = preview.state.value.hasImage

    fun uri(): Uri? = preview.state.value.uri?.let(Uri::parse)

    fun base64(): String = preview.state.value.base64

    fun mimeType(): String = preview.state.value.mimeType

    fun name(): String = preview.state.value.name

    private fun dispatch(action: ComposerImagePreviewUiAction) {
        when (val effect = preview.onAction(action)) {
            is ComposerImagePreviewUiEffect.ImageStateChanged ->
                listener.onImageStateChanged(effect.visible)
        }
    }
}
