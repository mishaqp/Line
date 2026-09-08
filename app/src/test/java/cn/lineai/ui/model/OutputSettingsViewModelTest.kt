package cn.lineai.ui.model

import cn.lineai.model.OutputSettings
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutputSettingsViewModelTest {
    @Test
    fun initialStateMirrorsRepositorySettings() {
        val stored = OutputSettings(true, OutputSettings.BROWSER_EXTERNAL, true)
        val viewModel = OutputSettingsViewModel(FakeRepository(stored))

        val state = viewModel.state.value
        assertTrue(state.codeWrapEnabled)
        assertEquals(OutputSettings.BROWSER_EXTERNAL, state.browserMode)
        assertTrue(state.browserJavaScriptEnabled)
    }

    @Test
    fun eachOutputChangeUpdatesStateAndRepositoryImmediately() {
        val repository = FakeRepository()
        val viewModel = OutputSettingsViewModel(repository)

        viewModel.onAction(OutputSettingsUiAction.SetCodeWrap(true))
        assertTrue(viewModel.state.value.codeWrapEnabled)
        assertTrue(repository.settings().isCodeWrapEnabled)

        viewModel.onAction(OutputSettingsUiAction.SetBrowserMode(OutputSettings.BROWSER_EXTERNAL))
        assertEquals(OutputSettings.BROWSER_EXTERNAL, viewModel.state.value.browserMode)
        assertEquals(OutputSettings.BROWSER_EXTERNAL, repository.settings().browserMode)

        viewModel.onAction(OutputSettingsUiAction.SetBrowserJavaScript(true))
        assertTrue(viewModel.state.value.browserJavaScriptEnabled)
        assertTrue(repository.settings().isBrowserJavaScriptEnabled)

        assertEquals(
            listOf("wrap:true", "browser:external", "js:true"),
            repository.writes
        )
    }

    @Test
    fun openToolCallPreviewReturnsTypedDestination() {
        val viewModel = OutputSettingsViewModel(FakeRepository())
        assertEquals(
            LineDestination.ToolCallPreview,
            viewModel.onAction(OutputSettingsUiAction.OpenToolCallPreview)
        )
        assertEquals(
            "toolcall_preview",
            viewModel.onAction(OutputSettingsUiAction.OpenToolCallPreview)?.screenId
        )
        assertFalse(
            viewModel.onAction(OutputSettingsUiAction.OpenToolCallPreview) is LineDestination.Legacy
        )
        assertEquals(
            LineDestination.Output,
            LineDestinations.parentOf(LineDestination.ToolCallPreview)
        )
        assertNull(viewModel.onAction(OutputSettingsUiAction.Back))
    }

    @Test
    fun recreatingViewModelReloadsPersistedValues() {
        val repository = FakeRepository()
        val first = OutputSettingsViewModel(repository)
        first.onAction(OutputSettingsUiAction.SetCodeWrap(true))
        first.onAction(OutputSettingsUiAction.SetBrowserMode(OutputSettings.BROWSER_EXTERNAL))
        first.onAction(OutputSettingsUiAction.SetBrowserJavaScript(true))

        val second = OutputSettingsViewModel(repository)
        assertTrue(second.state.value.codeWrapEnabled)
        assertEquals(OutputSettings.BROWSER_EXTERNAL, second.state.value.browserMode)
        assertTrue(second.state.value.browserJavaScriptEnabled)
        assertEquals(first.state.value, second.state.value)
    }

    @Test
    fun outputParentsToSettings() {
        assertEquals(LineDestination.Settings, LineDestinations.parentOf(LineDestination.Output))
        assertTrue(LineDestinations.fromScreenId("output") is LineDestination.Output)
        assertTrue(LineDestinations.fromScreenId("toolcall_preview") is LineDestination.ToolCallPreview)
    }

    private class FakeRepository(
        initial: OutputSettings = OutputSettings(false, OutputSettings.BROWSER_BUILTIN)
    ) : OutputSettingsRepository {
        private var current = initial
        val writes = mutableListOf<String>()

        override fun settings(): OutputSettings = current

        override fun setCodeWrapEnabled(enabled: Boolean) {
            writes += "wrap:$enabled"
            current = OutputSettings(
                enabled,
                current.browserMode,
                current.isBrowserJavaScriptEnabled,
                current.isAllowAnyHttp,
                current.isBypassPathProtection
            )
        }

        override fun setBrowserMode(mode: String) {
            writes += "browser:$mode"
            current = OutputSettings(
                current.isCodeWrapEnabled,
                mode,
                current.isBrowserJavaScriptEnabled,
                current.isAllowAnyHttp,
                current.isBypassPathProtection
            )
        }

        override fun setBrowserJavaScriptEnabled(enabled: Boolean) {
            writes += "js:$enabled"
            current = OutputSettings(
                current.isCodeWrapEnabled,
                current.browserMode,
                enabled,
                current.isAllowAnyHttp,
                current.isBypassPathProtection
            )
        }
    }
}
