package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.SecuritySettingsRepository
import cn.lineai.ui.model.SecuritySettingsUiAction
import cn.lineai.ui.model.SecuritySettingsViewModel

class SecuritySettingsHostView(
    context: Context,
    repository: SecuritySettingsRepository,
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
                        val settings: SecuritySettingsViewModel = viewModel(
                            key = "security-settings",
                            factory = SecuritySettingsViewModel.factory(repository)
                        )
                        SecuritySettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    SecuritySettingsUiAction.Back -> listener.onBack()
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
