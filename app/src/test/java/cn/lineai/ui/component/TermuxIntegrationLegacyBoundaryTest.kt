package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxIntegrationLegacyBoundaryTest {

    @Test
    fun termuxIntegrationOpensTypedFoundationDestination() {
        val destination = LineDestinations.fromScreenId("termuxIntegration")
        assertTrue(destination is LineDestination.TermuxIntegration)
        assertEquals("termuxIntegration", LineDestination.TermuxIntegration.screenId)
        assertEquals(LineDestination.TermuxIntegration, destination)
        assertFalse(destination is LineDestination.Legacy)
        assertFalse(destination is LineDestination.SshSettings)
    }

    @Test
    fun sshSettingsStaysItsOwnFoundationDestination() {
        val destination = LineDestinations.fromScreenId("sshSettings")
        assertTrue(destination is LineDestination.SshSettings)
        assertEquals("sshSettings", LineDestination.SshSettings.screenId)
        assertFalse(destination is LineDestination.TermuxIntegration)
        assertFalse(destination is LineDestination.Legacy)
    }

    @Test
    fun otherRoutesAreNotInterceptedAsTermux() {
        assertFalse(LineDestinations.fromScreenId("mcp") is LineDestination.TermuxIntegration)
        assertFalse(LineDestinations.fromScreenId("settings") is LineDestination.TermuxIntegration)
        assertFalse(LineDestinations.fromScreenId("toolSettings") is LineDestination.TermuxIntegration)
        assertTrue(LineDestinations.fromScreenId("mcp") is LineDestination.Mcp)
        assertTrue(LineDestinations.fromScreenId("unknown-route") is LineDestination.Legacy)
    }

    @Test
    fun bothBackChainsKeepMcpFallbackParent() {
        assertEquals(
            LineDestination.Mcp,
            LineDestinations.parentOf(LineDestination.TermuxIntegration)
        )
        assertEquals(
            LineDestination.Mcp,
            LineDestinations.parentOf(LineDestination.SshSettings)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.Mcp)
        )
    }
}
