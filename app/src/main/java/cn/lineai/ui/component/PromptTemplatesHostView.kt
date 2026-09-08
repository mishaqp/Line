package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.PromptTemplatesRepository
import cn.lineai.ui.model.PromptTemplatesUiAction
import cn.lineai.ui.model.PromptTemplatesViewModel

/**
 * Compose host for the Prompt Templates child screen. Persistence stays on the
 * existing controller callbacks through [PromptTemplatesRepository].
 */
class PromptTemplatesHostView(
    context: Context,
    repository: PromptTemplatesRepository,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val settings: PromptTemplatesViewModel = viewModel(
                            key = "prompt-templates",
                            factory = PromptTemplatesViewModel.factory(repository)
                        )
                        PromptTemplatesScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    PromptTemplatesUiAction.Back -> listener.onBack()
                                    else -> {
                                        settings.onAction(action)
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
