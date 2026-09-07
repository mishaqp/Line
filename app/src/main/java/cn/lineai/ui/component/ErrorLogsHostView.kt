package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.R
import cn.lineai.ui.model.ErrorLogsMessage
import cn.lineai.ui.model.ErrorLogsRepository
import cn.lineai.ui.model.ErrorLogsUiAction
import cn.lineai.ui.model.ErrorLogsViewModel

class ErrorLogsHostView(
    context: Context,
    repository: ErrorLogsRepository,
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
                        val settings: ErrorLogsViewModel = viewModel(
                            key = "error-logs",
                            factory = ErrorLogsViewModel.factory(repository)
                        )
                        val state = settings.state.collectAsStateWithLifecycle().value
                        LaunchedEffect(state.messageEventId) {
                            val messageRes = when (state.message) {
                                ErrorLogsMessage.CLEARED -> R.string.screen_error_logs_cleared
                                ErrorLogsMessage.OPEN_FAILED -> R.string.screen_error_logs_open_failed
                                null -> null
                            }
                            if (messageRes != null) {
                                Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
                                settings.onAction(ErrorLogsUiAction.ConsumeMessage)
                            }
                        }
                        ErrorLogsScreenContent(
                            state = state,
                            onAction = { action ->
                                when (action) {
                                    ErrorLogsUiAction.Back -> listener.onBack()
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
