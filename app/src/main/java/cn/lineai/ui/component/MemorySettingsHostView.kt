package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.R
import cn.lineai.ui.model.MemorySettingsRepository
import cn.lineai.ui.model.MemorySettingsViewModel
import cn.lineai.ui.model.MemoryUiEffect

class MemorySettingsHostView(
    context: Context,
    repository: MemorySettingsRepository,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()

    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }

    private val memory = ViewModelProvider(
        hostViewModelStoreOwner,
        MemorySettingsViewModel.factory(repository)
    )["memory-settings", MemorySettingsViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(memory) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        MemorySettingsScreenContent(
                            state = memory.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (memory.onAction(action)) {
                                    null -> Unit
                                    MemoryUiEffect.Back -> listener.onBack()
                                    MemoryUiEffect.EmptyContent -> Toast.makeText(
                                        context,
                                        R.string.screen_memory_empty_toast,
                                        Toast.LENGTH_SHORT
                                    ).show()
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
