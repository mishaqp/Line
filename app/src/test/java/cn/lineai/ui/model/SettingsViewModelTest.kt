package cn.lineai.ui.model

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun catalogExposesFullTypedSettingsSet() {
        val viewModel = SettingsViewModel()
        val ids = viewModel.state.value.screenIds

        assertEquals(
            listOf(
                "models",
                "codexAccount",
                "grokAccount",
                "llm",
                "mcp",
                "toolSettings",
                "extensions",
                "advancedFeatures",
                "input",
                "theme",
                "output",
                "security",
                "storage",
                "memory",
                "data",
                "errorLogs",
                "keepAlive",
                "about"
            ),
            ids
        )
        assertEquals(18, ids.size)
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun catalogHasNoUnknownOrLegacyScreenIds() {
        val viewModel = SettingsViewModel()
        viewModel.state.value.destinations.forEach { destination ->
            assertFalse(destination is LineDestination.Legacy)
            assertEquals(destination, LineDestinations.fromScreenId(destination.screenId))
            assertTrue(destination.screenId.isNotBlank())
            assertFalse(destination.screenId.contains(" "))
        }
    }

    @Test
    fun openActionReturnsTypedDestination() {
        val viewModel = SettingsViewModel()

        assertEquals(
            LineDestination.Models,
            viewModel.onAction(SettingsUiAction.Open(LineDestination.Models))
        )
        assertEquals(
            LineDestination.CodexAccount,
            viewModel.onAction(SettingsUiAction.Open(LineDestination.CodexAccount))
        )
        assertEquals(
            LineDestination.GrokAccount,
            viewModel.onAction(SettingsUiAction.Open(LineDestination.GrokAccount))
        )
        assertEquals(
            LineDestination.About,
            viewModel.onAction(SettingsUiAction.Open(LineDestination.About))
        )
        assertNull(viewModel.onAction(SettingsUiAction.Back))
    }

    @Test
    fun unknownDestinationIsRejected() {
        val viewModel = SettingsViewModel()
        assertNull(viewModel.onAction(SettingsUiAction.Open(LineDestination.Chat)))
        assertNull(viewModel.onAction(SettingsUiAction.Open(LineDestination.Legacy("not-a-settings-row"))))
        assertNull(viewModel.onAction(SettingsUiAction.Open(LineDestination.ModelAdd)))
    }

    @Test
    fun stateSurvivesRepeatedReadsLikeRecomposition() {
        val viewModel = SettingsViewModel()
        viewModel.onAction(SettingsUiAction.Open(LineDestination.Theme))

        val first = viewModel.state.value
        val second = viewModel.state.value

        assertEquals(first, second)
        assertEquals(first.sections, second.sections)
        assertEquals(18, second.destinations.size)
    }
}
