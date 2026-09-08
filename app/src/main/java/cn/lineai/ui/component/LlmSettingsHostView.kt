package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.LlmSettingsRepository
import cn.lineai.ui.model.LlmSettingsUiAction
import cn.lineai.ui.model.LlmSettingsViewModel

/**
 * Compose host for the LLM settings child screen. Prompt Templates stays on
 * the existing ScreenRegistry factory and is opened through a typed destination.
 */
class LlmSettingsHostView(
    context: Context,
    repository: LlmSettingsRepository,
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
                        val settings: LlmSettingsViewModel = viewModel(
                            key = "llm-settings",
                            factory = LlmSettingsViewModel.factory(repository)
                        )
                        LlmSettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    LlmSettingsUiAction.Back -> listener.onBack()
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
