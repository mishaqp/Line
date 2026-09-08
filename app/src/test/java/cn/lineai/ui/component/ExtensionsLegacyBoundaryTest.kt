package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ExtensionsLegacyBoundaryTest {
    @Test
    fun wrapperConvertsTypedDestinationsToLegacyFactoryCallbackIds() {
        val agentId = ExtensionsScreenView.legacyOpenId(LineDestination.Extension("agent"))
        val mcpId = ExtensionsScreenView.legacyOpenId(LineDestination.Extension("mcp"))
        val skillsId = ExtensionsScreenView.legacyOpenId(LineDestination.Extension("skills"))
        val lineCodeId = ExtensionsScreenView.legacyOpenId(LineDestination.Extension("linecode"))
        val terminalProviderId = ExtensionsScreenView.legacyOpenId(LineDestination.TerminalProvider)

        assertEquals("agent", agentId)
        assertEquals("mcp", mcpId)
        assertEquals("skills", skillsId)
        assertEquals("linecode", lineCodeId)
        assertEquals("terminalProvider", terminalProviderId)

        assertFalse(agentId.startsWith("extension:"))
        assertFalse(mcpId.startsWith("extension:"))
        assertFalse(skillsId.startsWith("extension:"))
        assertFalse(lineCodeId.startsWith("extension:"))
    }
}
