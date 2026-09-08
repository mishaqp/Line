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
import cn.lineai.ui.model.SshSettingsRepository
import cn.lineai.ui.model.SshSettingsUiAction
import cn.lineai.ui.model.SshSettingsUiEffect
import cn.lineai.ui.model.SshSettingsViewModel

class SshSettingsHostView(
    context: Context,
    repository: SshSettingsRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onOpen(destination: LineDestination)
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val sshSettings = ViewModelProvider(
        hostViewModelStoreOwner,
        SshSettingsViewModel.factory(repository)
    )["ssh-settings", SshSettingsViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(sshSettings) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        SshSettingsScreenContent(
                            state = sshSettings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = sshSettings.onAction(action)) {
                                    SshSettingsUiEffect.Back -> listener.onBack()
                                    is SshSettingsUiEffect.Navigate -> listener.onOpen(effect.destination)
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
            sshSettings.onAction(SshSettingsUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
