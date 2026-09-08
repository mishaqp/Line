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
import cn.lineai.ui.model.LicensesUiEffect
import cn.lineai.ui.model.LicensesViewModel

class LicensesHostView(
    context: Context,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()

    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }

    private val licenses = ViewModelProvider(
        hostViewModelStoreOwner,
        LicensesViewModel.factory()
    )["licenses", LicensesViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(licenses) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        LicensesScreenContent(
                            state = licenses.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (licenses.onAction(action)) {
                                    LicensesUiEffect.Back -> listener.onBack()
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
