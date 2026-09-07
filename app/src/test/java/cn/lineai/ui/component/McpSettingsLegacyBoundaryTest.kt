package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpSettingsLegacyBoundaryTest {

    @Test
    fun toolsAndExecutionUsesTypedMcpDestination() {
        assertTrue(LineDestinations.fromScreenId("mcp") is LineDestination.Mcp)
        assertEquals("mcp", LineDestination.Mcp.screenId)
        assertFalse(LineDestinations.fromScreenId("mcp") is LineDestination.Extension)
    }

    @Test
    fun mcpExtensionsStayOnExtensionDestination() {
        val destination = LineDestinations.fromScreenId("extension:mcp")
        assertTrue(destination is LineDestination.Extension)
        assertEquals("mcp", (destination as LineDestination.Extension).kind)
        assertFalse(destination is LineDestination.Mcp)
        assertTrue(McpExtensionsLegacyBridge.handles(destination))
        assertFalse(McpExtensionsLegacyBridge.handles(LineDestination.Mcp))
    }

    @Test
    fun childSshAndTermuxStayLegacyScreensWithMcpParent() {
        assertEquals("sshSettings", LineDestination.SshSettings.screenId)
        assertEquals("termuxIntegration", LineDestination.TermuxIntegration.screenId)
        assertEquals(
            LineDestination.SshSettings,
            LineDestinations.fromScreenId("sshSettings")
        )
        assertEquals(
            LineDestination.TermuxIntegration,
            LineDestinations.fromScreenId("termuxIntegration")
        )
        assertEquals(LineDestination.Mcp, LineDestinations.parentOf(LineDestination.SshSettings))
        assertEquals(
            LineDestination.Mcp,
            LineDestinations.parentOf(LineDestination.TermuxIntegration)
        )
        assertFalse(McpExtensionsLegacyBridge.handles(LineDestination.SshSettings))
        assertFalse(McpExtensionsLegacyBridge.handles(LineDestination.TermuxIntegration))
    }
}
