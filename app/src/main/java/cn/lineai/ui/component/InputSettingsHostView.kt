package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.InputSettingsRepository
import cn.lineai.ui.model.InputSettingsUiAction
import cn.lineai.ui.model.InputSettingsViewModel

class InputSettingsHostView(
    context: Context,
    repository: InputSettingsRepository,
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
                        val settings: InputSettingsViewModel = viewModel(
                            key = "input-settings",
                            factory = InputSettingsViewModel.factory(repository)
                        )
                        InputSettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    InputSettingsUiAction.Back -> listener.onBack()
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
