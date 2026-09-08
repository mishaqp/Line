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
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.AdvancedFeaturesUiEffect
import cn.lineai.ui.model.AdvancedFeaturesViewModel

class AdvancedFeaturesHostView(
    context: Context,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onOpen(destination: LineDestination)
    }

    private val hostViewModelStore = ViewModelStore()

    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }

    private val advancedFeatures = ViewModelProvider(
        hostViewModelStoreOwner,
        AdvancedFeaturesViewModel.factory()
    )["advanced-features", AdvancedFeaturesViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(advancedFeatures) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        AdvancedFeaturesScreenContent(
                            state = advancedFeatures.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = advancedFeatures.onAction(action)) {
                                    AdvancedFeaturesUiEffect.Back -> listener.onBack()
                                    is AdvancedFeaturesUiEffect.Navigate -> listener.onOpen(effect.destination)
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}
