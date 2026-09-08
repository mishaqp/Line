package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.DataSettingsRepository
import cn.lineai.ui.model.DataSettingsUiAction
import cn.lineai.ui.model.DataSettingsViewModel

class DataSettingsHostView(
    context: Context,
    repository: DataSettingsRepository,
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
                        val settings: DataSettingsViewModel = viewModel(
                            key = "data-settings",
                            factory = DataSettingsViewModel.factory(repository)
                        )
                        DataSettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (action) {
                                    DataSettingsUiAction.Back -> listener.onBack()
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
