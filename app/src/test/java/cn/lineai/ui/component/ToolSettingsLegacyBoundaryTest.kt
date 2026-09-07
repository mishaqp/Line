package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSettingsLegacyBoundaryTest {

    @Test
    fun toolSettingsOpensTypedFoundationDestination() {
        val destination = LineDestinations.fromScreenId("toolSettings")
        assertTrue(destination is LineDestination.ToolSettings)
        assertEquals("toolSettings", LineDestination.ToolSettings.screenId)
        assertEquals(LineDestination.ToolSettings, destination)
        assertFalse(destination is LineDestination.Legacy)
        assertEquals(
            "cn.lineai.ui.component.ToolSettingsScreenView",
            ToolSettingsScreenView::class.java.name
        )
        assertEquals(
            "cn.lineai.ui.component.ToolSettingsHostView",
            ToolSettingsHostView::class.java.name
        )
    }

    @Test
    fun imagePickersStayTypedChildrenAndKeepLegacyScreenIds() {
        assertTrue(
            LineDestinations.fromScreenId("imageUnderstandingModel")
                is LineDestination.ImageUnderstandingModel
        )
        assertTrue(
            LineDestinations.fromScreenId("imageGenerationModel")
                is LineDestination.ImageGenerationModel
        )
        assertEquals("imageUnderstandingModel", LineDestination.ImageUnderstandingModel.screenId)
        assertEquals("imageGenerationModel", LineDestination.ImageGenerationModel.screenId)
        assertFalse(
            LineDestinations.fromScreenId("imageUnderstandingModel") is LineDestination.ToolSettings
        )
        assertFalse(
            LineDestinations.fromScreenId("imageGenerationModel") is LineDestination.Legacy
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageUnderstandingModel)
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageGenerationModel)
        )
    }

    @Test
    fun backChainsReturnToToolSettingsThenSettings() {
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageUnderstandingModel)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.ToolSettings)
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageGenerationModel)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(
                LineDestinations.parentOf(LineDestination.ImageGenerationModel)
            )
        )
    }

    @Test
    fun emptyStackFallbacksMatchLegacyParents() {
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestinations.fromScreenId("imageUnderstandingModel"))
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestinations.fromScreenId("imageGenerationModel"))
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestinations.fromScreenId("toolSettings"))
        )
    }

    @Test
    fun neighboringRoutesAreUnchanged() {
        assertEquals(LineDestination.Mcp, LineDestinations.fromScreenId("mcp"))
        assertEquals(LineDestination.SshSettings, LineDestinations.fromScreenId("sshSettings"))
        assertEquals(
            LineDestination.TermuxIntegration,
            LineDestinations.fromScreenId("termuxIntegration")
        )
        assertEquals(LineDestination.Models, LineDestinations.fromScreenId("models"))
        assertEquals(LineDestination.Mcp, LineDestinations.parentOf(LineDestination.SshSettings))
        assertEquals(
            LineDestination.Mcp,
            LineDestinations.parentOf(LineDestination.TermuxIntegration)
        )
        assertEquals(LineDestination.Settings, LineDestinations.parentOf(LineDestination.Mcp))
        assertEquals(LineDestination.Settings, LineDestinations.parentOf(LineDestination.Models))
        assertFalse(LineDestinations.fromScreenId("mcp") is LineDestination.ToolSettings)
        assertFalse(LineDestinations.fromScreenId("models") is LineDestination.ImageUnderstandingModel)
    }
}
