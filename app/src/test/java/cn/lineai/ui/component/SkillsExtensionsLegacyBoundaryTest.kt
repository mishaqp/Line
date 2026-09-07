package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsExtensionsLegacyBoundaryTest {

    @Test
    fun onlyTypedSkillsExtensionDestinationUsesNewHost() {
        assertTrue(
            SkillsExtensionsLegacyBridge.handles(LineDestination.Extension("skills"))
        )

        assertFalse(
            SkillsExtensionsLegacyBridge.handles(LineDestination.Extension("linecode"))
        )
        assertFalse(
            SkillsExtensionsLegacyBridge.handles(LineDestination.Extension("agent"))
        )
        assertFalse(
            SkillsExtensionsLegacyBridge.handles(LineDestination.Extension("mcp"))
        )
        assertFalse(
            SkillsExtensionsLegacyBridge.handles(LineDestination.Extension("extension:skills"))
        )
        assertFalse(
            SkillsExtensionsLegacyBridge.handles(LineDestination.Extensions)
        )
        assertFalse(SkillsExtensionsLegacyBridge.handles(null))
    }

    @Test
    fun neighborHostsStayOnOwnDestinations() {
        assertTrue(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extension("linecode"))
        )
        assertFalse(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extension("skills"))
        )
        assertTrue(
            AgentExtensionsLegacyBridge.handles(LineDestination.Extension("agent"))
        )
        assertTrue(
            McpExtensionsLegacyBridge.handles(LineDestination.Extension("mcp"))
        )
    }
}
