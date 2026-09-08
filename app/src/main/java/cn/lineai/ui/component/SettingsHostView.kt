package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.SettingsUiAction
import cn.lineai.ui.model.SettingsViewModel

/**
 * Compose host for the main Settings hub. Child settings screens stay on the
 * existing ScreenRegistry / typed destination bridge.
 */
class SettingsHostView(
    context: Context,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onOpen(destination: LineDestination)
    }

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val settings: SettingsViewModel = viewModel(
                            key = "settings-hub",
                            factory = SettingsViewModel.factory()
                        )
                        SettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    SettingsUiAction.Back -> listener.onBack()
                                    is SettingsUiAction.Open -> {
                                        settings.onAction(action)?.let(listener::onOpen)
                                    }
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
