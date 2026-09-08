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
import cn.lineai.ui.model.ImageGenerationModelPickerRepository
import cn.lineai.ui.model.ImageGenerationModelPickerUiAction
import cn.lineai.ui.model.ImageGenerationModelPickerUiEffect
import cn.lineai.ui.model.ImageGenerationModelPickerViewModel

class ImageGenerationModelPickerHostView(
    context: Context,
    repository: ImageGenerationModelPickerRepository,
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
        ImageGenerationModelPickerViewModel.factory(repository)
    )["image-generation-model-picker", ImageGenerationModelPickerViewModel::class.java]
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
                        ImageGenerationModelPickerScreenContent(
                            state = picker.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (picker.onAction(action)) {
                                    ImageGenerationModelPickerUiEffect.Back -> listener.onBack()
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
            picker.onAction(ImageGenerationModelPickerUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
