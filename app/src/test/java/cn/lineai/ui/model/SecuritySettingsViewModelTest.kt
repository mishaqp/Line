package cn.lineai.ui.model

import cn.lineai.model.OutputSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySettingsViewModelTest {

    @Test
    fun initialStateReflectsPersistedSecurityValues() {
        val repository = FakeRepository(
            OutputSettings(
                true,
                OutputSettings.BROWSER_EXTERNAL,
                true,
                true,
                true
            ),
            fullAccess = true
        )

        val state = SecuritySettingsViewModel(repository).state.value

        assertTrue(state.allowAnyHttp)
        assertTrue(state.browserJavaScriptEnabled)
        assertTrue(state.bypassPathProtection)
        assertTrue(state.fullAccessEnabled)
        assertFalse(state.showBypassWarning)
    }

    @Test
    fun ordinaryTogglesUpdateStateAndRepositoryImmediately() {
        val repository = FakeRepository()
        val viewModel = SecuritySettingsViewModel(repository)

        viewModel.onAction(SecuritySettingsUiAction.SetAllowAnyHttp(true))
        viewModel.onAction(SecuritySettingsUiAction.SetBrowserJavaScript(true))
        viewModel.onAction(SecuritySettingsUiAction.SetFullAccess(true))

        val state = viewModel.state.value
        assertTrue(state.allowAnyHttp)
        assertTrue(state.browserJavaScriptEnabled)
        assertTrue(state.fullAccessEnabled)
        assertEquals(listOf(true), repository.allowAnyHttpWrites)
        assertEquals(listOf(true), repository.browserJavaScriptWrites)
        assertEquals(listOf(true), repository.fullAccessWrites)
    }

    @Test
    fun enablingPathBypassRequiresConfirmation() {
        val repository = FakeRepository()
        val viewModel = SecuritySettingsViewModel(repository)

        viewModel.onAction(SecuritySettingsUiAction.SetBypassPathProtection(true))

        assertTrue(viewModel.state.value.showBypassWarning)
        assertFalse(viewModel.state.value.bypassPathProtection)
        assertTrue(repository.bypassWrites.isEmpty())

        viewModel.onAction(SecuritySettingsUiAction.ConfirmBypassPathProtection)

        assertFalse(viewModel.state.value.showBypassWarning)
        assertTrue(viewModel.state.value.bypassPathProtection)
        assertEquals(listOf(true), repository.bypassWrites)
    }

    @Test
    fun dismissingWarningLeavesPathProtectionEnabled() {
        val repository = FakeRepository()
        val viewModel = SecuritySettingsViewModel(repository)

        viewModel.onAction(SecuritySettingsUiAction.SetBypassPathProtection(true))
        viewModel.onAction(SecuritySettingsUiAction.DismissBypassWarning)

        assertFalse(viewModel.state.value.showBypassWarning)
        assertFalse(viewModel.state.value.bypassPathProtection)
        assertTrue(repository.bypassWrites.isEmpty())
    }

    @Test
    fun disablingPathBypassIsImmediate() {
        val repository = FakeRepository(
            OutputSettings(
                false,
                OutputSettings.BROWSER_BUILTIN,
                false,
                false,
                true
            )
        )
        val viewModel = SecuritySettingsViewModel(repository)

        viewModel.onAction(SecuritySettingsUiAction.SetBypassPathProtection(false))

        assertFalse(viewModel.state.value.bypassPathProtection)
        assertFalse(viewModel.state.value.showBypassWarning)
        assertEquals(listOf(false), repository.bypassWrites)
    }

    private class FakeRepository(
        private var settings: OutputSettings =
            OutputSettings(false, OutputSettings.BROWSER_BUILTIN),
        private var fullAccess: Boolean = false
    ) : SecuritySettingsRepository {
        val allowAnyHttpWrites = mutableListOf<Boolean>()
        val browserJavaScriptWrites = mutableListOf<Boolean>()
        val bypassWrites = mutableListOf<Boolean>()
        val fullAccessWrites = mutableListOf<Boolean>()

        override fun outputSettings(): OutputSettings = settings

        override fun fullAccessEnabled(): Boolean = fullAccess

        override fun setAllowAnyHttp(enabled: Boolean) {
            allowAnyHttpWrites += enabled
        }

        override fun setBrowserJavaScriptEnabled(enabled: Boolean) {
            browserJavaScriptWrites += enabled
        }

        override fun setBypassPathProtection(enabled: Boolean) {
            bypassWrites += enabled
        }

        override fun setFullAccessEnabled(enabled: Boolean) {
            fullAccessWrites += enabled
        }
    }
}
