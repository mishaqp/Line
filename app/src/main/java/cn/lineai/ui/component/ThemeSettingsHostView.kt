package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.ThemeSettingsRepository
import cn.lineai.ui.model.ThemeSettingsUiAction
import cn.lineai.ui.model.ThemeSettingsViewModel

class ThemeSettingsHostView(
    context: Context,
    repository: ThemeSettingsRepository,
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
                        val settings: ThemeSettingsViewModel = viewModel(
                            key = "theme-settings",
                            factory = ThemeSettingsViewModel.factory(repository)
                        )
                        ThemeSettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    ThemeSettingsUiAction.Back -> listener.onBack()
                                    else -> settings.onAction(action)
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
