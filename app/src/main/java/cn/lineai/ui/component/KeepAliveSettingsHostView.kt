package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.KeepAliveSettingsRepository
import cn.lineai.ui.model.KeepAliveSettingsUiAction
import cn.lineai.ui.model.KeepAliveSettingsUiEffect
import cn.lineai.ui.model.KeepAliveSettingsViewModel

class KeepAliveSettingsHostView(
    context: Context,
    repository: KeepAliveSettingsRepository,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onRequestPostNotifications()
        fun onOpenBatteryOptimizationSettings()
    }

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val settings: KeepAliveSettingsViewModel = viewModel(
                            key = "keep-alive-settings",
                            factory = KeepAliveSettingsViewModel.factory(repository)
                        )
                        val lifecycleOwner = LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner, settings) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    settings.onAction(KeepAliveSettingsUiAction.RefreshBatteryOptimization)
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }

                        fun handleEffect(effect: KeepAliveSettingsUiEffect?) {
                            when (effect) {
                                null -> Unit
                                KeepAliveSettingsUiEffect.Back -> listener.onBack()
                                KeepAliveSettingsUiEffect.OpenBatteryOptimizationSettings ->
                                    listener.onOpenBatteryOptimizationSettings()
                                KeepAliveSettingsUiEffect.RequestPostNotifications -> {
                                    listener.onRequestPostNotifications()
                                    settings.onAction(KeepAliveSettingsUiAction.NotificationPermissionHandled)
                                }
                            }
                        }

                        KeepAliveSettingsScreenContent(
                            state = settings.state.collectAsStateWithLifecycle().value,
                            onAction = { action -> handleEffect(settings.onAction(action)) }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}
