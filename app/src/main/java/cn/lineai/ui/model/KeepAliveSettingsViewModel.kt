package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KeepAliveStoredSettings(
    val wakeLockEnabled: Boolean,
    val foregroundEnabled: Boolean,
    val fakeAudioEnabled: Boolean
)

interface KeepAliveSettingsRepository {
    fun loadSettings(): KeepAliveStoredSettings?
    fun setWakeLockEnabled(enabled: Boolean)
    fun setForegroundEnabled(enabled: Boolean)
    fun setFakeAudioEnabled(enabled: Boolean)
    fun updateService()
    fun notifySettingsChanged()
    fun hasPostNotificationsPermission(): Boolean
    fun isIgnoringBatteryOptimizations(): Boolean
}

data class KeepAliveSettingsUiState(
    val wakeLockEnabled: Boolean = true,
    val foregroundEnabled: Boolean = false,
    val fakeAudioEnabled: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = true
)

sealed interface KeepAliveSettingsUiAction {
    data object Back : KeepAliveSettingsUiAction
    data class WakeLockChanged(val enabled: Boolean) : KeepAliveSettingsUiAction
    data class ForegroundChanged(val enabled: Boolean) : KeepAliveSettingsUiAction
    data class FakeAudioChanged(val enabled: Boolean) : KeepAliveSettingsUiAction
    data class BatteryOptimizationChanged(val enabled: Boolean) : KeepAliveSettingsUiAction
    data object RefreshBatteryOptimization : KeepAliveSettingsUiAction
    data object NotificationPermissionHandled : KeepAliveSettingsUiAction
}

sealed interface KeepAliveSettingsUiEffect {
    data object Back : KeepAliveSettingsUiEffect
    data object RequestPostNotifications : KeepAliveSettingsUiEffect
    data object OpenBatteryOptimizationSettings : KeepAliveSettingsUiEffect
}

class KeepAliveSettingsViewModel(
    private val repository: KeepAliveSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<KeepAliveSettingsUiState> = _state.asStateFlow()

    private var pendingNotificationCompletion = false

    fun onAction(action: KeepAliveSettingsUiAction): KeepAliveSettingsUiEffect? = when (action) {
        KeepAliveSettingsUiAction.Back -> KeepAliveSettingsUiEffect.Back

        is KeepAliveSettingsUiAction.WakeLockChanged -> {
            _state.value = _state.value.copy(wakeLockEnabled = action.enabled)
            repository.setWakeLockEnabled(action.enabled)
            finishSettingsChange()
            null
        }

        is KeepAliveSettingsUiAction.ForegroundChanged -> {
            _state.value = _state.value.copy(foregroundEnabled = action.enabled)
            repository.setForegroundEnabled(action.enabled)
            afterNotificationSensitiveChange(action.enabled)
        }

        is KeepAliveSettingsUiAction.FakeAudioChanged -> {
            _state.value = _state.value.copy(fakeAudioEnabled = action.enabled)
            repository.setFakeAudioEnabled(action.enabled)
            afterNotificationSensitiveChange(action.enabled)
        }

        is KeepAliveSettingsUiAction.BatteryOptimizationChanged -> {
            refreshBatteryOptimization()
            if (action.enabled && !_state.value.ignoringBatteryOptimizations) {
                KeepAliveSettingsUiEffect.OpenBatteryOptimizationSettings
            } else {
                null
            }
        }

        KeepAliveSettingsUiAction.RefreshBatteryOptimization -> {
            refreshBatteryOptimization()
            null
        }

        KeepAliveSettingsUiAction.NotificationPermissionHandled -> {
            if (pendingNotificationCompletion) {
                pendingNotificationCompletion = false
                finishSettingsChange()
            }
            null
        }
    }

    private fun afterNotificationSensitiveChange(enabled: Boolean): KeepAliveSettingsUiEffect? {
        if (enabled && !hasPostNotificationsPermission()) {
            pendingNotificationCompletion = true
            return KeepAliveSettingsUiEffect.RequestPostNotifications
        }
        pendingNotificationCompletion = false
        finishSettingsChange()
        return null
    }

    private fun finishSettingsChange() {
        repository.updateService()
        repository.notifySettingsChanged()
    }

    private fun refreshBatteryOptimization() {
        val current = try {
            repository.isIgnoringBatteryOptimizations()
        } catch (_: Exception) {
            _state.value.ignoringBatteryOptimizations
        }
        _state.value = _state.value.copy(ignoringBatteryOptimizations = current)
    }

    private fun hasPostNotificationsPermission(): Boolean = try {
        repository.hasPostNotificationsPermission()
    } catch (_: Exception) {
        true
    }

    private fun loadState(): KeepAliveSettingsUiState {
        val settings = try {
            repository.loadSettings()
        } catch (_: Exception) {
            null
        }
        val batteryAllowed = try {
            repository.isIgnoringBatteryOptimizations()
        } catch (_: Exception) {
            true
        }
        return KeepAliveSettingsUiState(
            wakeLockEnabled = settings?.wakeLockEnabled ?: true,
            foregroundEnabled = settings?.foregroundEnabled ?: false,
            fakeAudioEnabled = settings?.fakeAudioEnabled ?: false,
            ignoringBatteryOptimizations = batteryAllowed
        )
    }

    companion object {
        fun factory(repository: KeepAliveSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(KeepAliveSettingsViewModel::class.java)) {
                        return KeepAliveSettingsViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
