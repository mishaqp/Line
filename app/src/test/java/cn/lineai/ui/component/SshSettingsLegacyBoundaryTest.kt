package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SshSettingsLegacyBoundaryTest {

    @Test
    fun sshSettingsUsesTypedDestinationWithMcpParent() {
        assertTrue(LineDestinations.fromScreenId("sshSettings") is LineDestination.SshSettings)
        assertEquals("sshSettings", LineDestination.SshSettings.screenId)
        assertEquals(
            LineDestination.SshSettings,
            LineDestinations.fromScreenId("sshSettings")
        )
        assertEquals(LineDestination.Mcp, LineDestinations.parentOf(LineDestination.SshSettings))
        assertFalse(LineDestinations.fromScreenId("sshSettings") is LineDestination.Legacy)
    }

    @Test
    fun termuxStaysLegacyTypedChildOfMcp() {
        assertEquals("termuxIntegration", LineDestination.TermuxIntegration.screenId)
        assertEquals(
            LineDestination.TermuxIntegration,
            LineDestinations.fromScreenId("termuxIntegration")
        )
        assertEquals(
            LineDestination.Mcp,
            LineDestinations.parentOf(LineDestination.TermuxIntegration)
        )
        assertFalse(LineDestinations.fromScreenId("termuxIntegration") is LineDestination.SshSettings)
    }
}
