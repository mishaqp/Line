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
import cn.lineai.ui.model.ImageUnderstandingModelPickerRepository
import cn.lineai.ui.model.ImageUnderstandingModelPickerUiAction
import cn.lineai.ui.model.ImageUnderstandingModelPickerUiEffect
import cn.lineai.ui.model.ImageUnderstandingModelPickerViewModel

class ImageUnderstandingModelPickerHostView(
    context: Context,
    repository: ImageUnderstandingModelPickerRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val picker = ViewModelProvider(
        hostViewModelStoreOwner,
        ImageUnderstandingModelPickerViewModel.factory(repository)
    )["image-understanding-model-picker", ImageUnderstandingModelPickerViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(picker) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        ImageUnderstandingModelPickerScreenContent(
                            state = picker.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (picker.onAction(action)) {
                                    ImageUnderstandingModelPickerUiEffect.Back -> listener.onBack()
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
            picker.onAction(ImageUnderstandingModelPickerUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
