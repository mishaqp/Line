package cn.lineai.ui.model

import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class KeepAliveSettingsViewModelTest {
    @Test
    fun loadsThreeSavedSettingsIntoStateFlow() {
        val viewModel = KeepAliveSettingsViewModel(
            FakeRepository(KeepAliveStoredSettings(false, true, true), batteryIgnoring = false)
        )

        val state: StateFlow<KeepAliveSettingsUiState> = viewModel.state
        assertEquals(KeepAliveSettingsUiState(false, true, true, false), state.value)
    }

    @Test
    fun missingSettingsUseExistingRepositoryDefaults() {
        val viewModel = KeepAliveSettingsViewModel(FakeRepository(settings = null))

        assertEquals(KeepAliveSettingsUiState(true, false, false, true), viewModel.state.value)
    }

    @Test
    fun wakeLockChangeSavesThenUpdatesServiceThenNotifies() {
        val repository = FakeRepository()
        val viewModel = KeepAliveSettingsViewModel(repository)

        assertNull(viewModel.onAction(KeepAliveSettingsUiAction.WakeLockChanged(false)))

        assertEquals(false, viewModel.state.value.wakeLockEnabled)
        assertEquals(listOf("saveWake:false", "updateService", "settingsChanged"), repository.events)
        assertEquals(1, repository.wakeSaveCount)
    }

    @Test
    fun foregroundWithPermissionSavesThenUpdatesAndNotifies() {
        val repository = FakeRepository(notificationPermission = true)
        val viewModel = KeepAliveSettingsViewModel(repository)

        assertNull(viewModel.onAction(KeepAliveSettingsUiAction.ForegroundChanged(true)))

        assertEquals(true, viewModel.state.value.foregroundEnabled)
        assertEquals(listOf("saveForeground:true", "updateService", "settingsChanged"), repository.events)
        assertEquals(1, repository.foregroundSaveCount)
    }

    @Test
    fun fakeAudioWithPermissionSavesThenUpdatesAndNotifies() {
        val repository = FakeRepository(notificationPermission = true)
        val viewModel = KeepAliveSettingsViewModel(repository)

        assertNull(viewModel.onAction(KeepAliveSettingsUiAction.FakeAudioChanged(true)))

        assertEquals(true, viewModel.state.value.fakeAudioEnabled)
        assertEquals(listOf("saveFakeAudio:true", "updateService", "settingsChanged"), repository.events)
        assertEquals(1, repository.fakeAudioSaveCount)
    }

    @Test
    fun foregroundRequestsNotificationsOnlyWhenEnablingWithoutPermission() {
        val repository = FakeRepository(notificationPermission = false)
        val viewModel = KeepAliveSettingsViewModel(repository)

        val effect = viewModel.onAction(KeepAliveSettingsUiAction.ForegroundChanged(true))

        assertSame(KeepAliveSettingsUiEffect.RequestPostNotifications, effect)
        assertEquals(listOf("saveForeground:true"), repository.events)
        assertEquals(true, viewModel.state.value.foregroundEnabled)

        assertNull(viewModel.onAction(KeepAliveSettingsUiAction.NotificationPermissionHandled))
        assertEquals(
            listOf("saveForeground:true", "updateService", "settingsChanged"),
            repository.events
        )
    }

    @Test
    fun fakeAudioRequestsNotificationsOnlyWhenEnablingWithoutPermission() {
        val repository = FakeRepository(notificationPermission = false)
        val viewModel = KeepAliveSettingsViewModel(repository)

        val effect = viewModel.onAction(KeepAliveSettingsUiAction.FakeAudioChanged(true))

        assertSame(KeepAliveSettingsUiEffect.RequestPostNotifications, effect)
        assertEquals(listOf("saveFakeAudio:true"), repository.events)
        viewModel.onAction(KeepAliveSettingsUiAction.NotificationPermissionHandled)
        assertEquals(
            listOf("saveFakeAudio:true", "updateService", "settingsChanged"),
            repository.events
        )
    }

    @Test
    fun disablingForegroundNeverRequestsNotificationPermission() {
        val repository = FakeRepository(
            settings = KeepAliveStoredSettings(true, true, false),
            notificationPermission = false
        )
        val viewModel = KeepAliveSettingsViewModel(repository)

        val effect = viewModel.onAction(KeepAliveSettingsUiAction.ForegroundChanged(false))

        assertNull(effect)
        assertEquals(listOf("saveForeground:false", "updateService", "settingsChanged"), repository.events)
    }

    @Test
    fun disablingFakeAudioNeverRequestsNotificationPermission() {
        val repository = FakeRepository(
            settings = KeepAliveStoredSettings(true, false, true),
            notificationPermission = false
        )
        val viewModel = KeepAliveSettingsViewModel(repository)

        val effect = viewModel.onAction(KeepAliveSettingsUiAction.FakeAudioChanged(false))

        assertNull(effect)
        assertEquals(listOf("saveFakeAudio:false", "updateService", "settingsChanged"), repository.events)
    }

    @Test
    fun batteryOptimizationOpensOnlyWhenExemptionIsMissing() {
        val missing = FakeRepository(batteryIgnoring = false)
        val missingViewModel = KeepAliveSettingsViewModel(missing)
        assertSame(
            KeepAliveSettingsUiEffect.OpenBatteryOptimizationSettings,
            missingViewModel.onAction(KeepAliveSettingsUiAction.BatteryOptimizationChanged(true))
        )

        val granted = FakeRepository(batteryIgnoring = true)
        val grantedViewModel = KeepAliveSettingsViewModel(granted)
        assertNull(grantedViewModel.onAction(KeepAliveSettingsUiAction.BatteryOptimizationChanged(true)))
        assertNull(grantedViewModel.onAction(KeepAliveSettingsUiAction.BatteryOptimizationChanged(false)))
    }

    @Test
    fun refreshBatteryOptimizationReadsActualState() {
        val repository = FakeRepository(batteryIgnoring = false)
        val viewModel = KeepAliveSettingsViewModel(repository)
        assertEquals(false, viewModel.state.value.ignoringBatteryOptimizations)

        repository.batteryIgnoring = true
        viewModel.onAction(KeepAliveSettingsUiAction.RefreshBatteryOptimization)

        assertEquals(true, viewModel.state.value.ignoringBatteryOptimizations)
    }

    @Test
    fun notificationEffectIsConsumedByExplicitCompletionOnlyOnce() {
        val repository = FakeRepository(notificationPermission = false)
        val viewModel = KeepAliveSettingsViewModel(repository)

        assertSame(
            KeepAliveSettingsUiEffect.RequestPostNotifications,
            viewModel.onAction(KeepAliveSettingsUiAction.ForegroundChanged(true))
        )
        assertNull(viewModel.onAction(KeepAliveSettingsUiAction.NotificationPermissionHandled))
        assertNull(viewModel.onAction(KeepAliveSettingsUiAction.NotificationPermissionHandled))

        assertEquals(1, repository.updateServiceCount)
        assertEquals(1, repository.settingsChangedCount)
    }

    @Test
    fun backReturnsEffectWithoutChangingSettings() {
        val repository = FakeRepository()
        val viewModel = KeepAliveSettingsViewModel(repository)
        val before = viewModel.state.value

        val effect = viewModel.onAction(KeepAliveSettingsUiAction.Back)

        assertSame(KeepAliveSettingsUiEffect.Back, effect)
        assertEquals(before, viewModel.state.value)
        assertEquals(emptyList<String>(), repository.events)
    }

    private class FakeRepository(
        var settings: KeepAliveStoredSettings? = KeepAliveStoredSettings(true, false, false),
        var notificationPermission: Boolean = true,
        var batteryIgnoring: Boolean = true
    ) : KeepAliveSettingsRepository {
        val events = mutableListOf<String>()
        var wakeSaveCount = 0
        var foregroundSaveCount = 0
        var fakeAudioSaveCount = 0
        var updateServiceCount = 0
        var settingsChangedCount = 0

        override fun loadSettings(): KeepAliveStoredSettings? = settings

        override fun setWakeLockEnabled(enabled: Boolean) {
            wakeSaveCount++
            events += "saveWake:$enabled"
            val current = settings ?: KeepAliveStoredSettings(true, false, false)
            settings = current.copy(wakeLockEnabled = enabled)
        }

        override fun setForegroundEnabled(enabled: Boolean) {
            foregroundSaveCount++
            events += "saveForeground:$enabled"
            val current = settings ?: KeepAliveStoredSettings(true, false, false)
            settings = current.copy(foregroundEnabled = enabled)
        }

        override fun setFakeAudioEnabled(enabled: Boolean) {
            fakeAudioSaveCount++
            events += "saveFakeAudio:$enabled"
            val current = settings ?: KeepAliveStoredSettings(true, false, false)
            settings = current.copy(fakeAudioEnabled = enabled)
        }

        override fun updateService() {
            updateServiceCount++
            events += "updateService"
        }

        override fun notifySettingsChanged() {
            settingsChangedCount++
            events += "settingsChanged"
        }

        override fun hasPostNotificationsPermission(): Boolean = notificationPermission

        override fun isIgnoringBatteryOptimizations(): Boolean = batteryIgnoring
    }
}
