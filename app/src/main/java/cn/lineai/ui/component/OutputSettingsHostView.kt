package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.OutputSettingsRepository
import cn.lineai.ui.model.OutputSettingsUiAction
import cn.lineai.ui.model.OutputSettingsViewModel

class OutputSettingsHostView(
    context: Context,
    repository: OutputSettingsRepository,
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
                        val settings: OutputSettingsViewModel = viewModel(
                            key = "output-settings",
                            factory = OutputSettingsViewModel.factory(repository)
                        )
                        OutputSettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    OutputSettingsUiAction.Back -> listener.onBack()
                                    else -> {
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
