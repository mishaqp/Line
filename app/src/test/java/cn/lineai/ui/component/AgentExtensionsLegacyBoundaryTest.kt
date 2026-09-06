package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExtensionsLegacyBoundaryTest {

    @Test
    fun onlyTypedAgentExtensionDestinationUsesNewHost() {
        assertTrue(
            AgentExtensionsLegacyBridge.handles(LineDestination.Extension("agent"))
        )

        assertFalse(
            AgentExtensionsLegacyBridge.handles(LineDestination.Extension("mcp"))
        )
        assertFalse(
            AgentExtensionsLegacyBridge.handles(LineDestination.Extension("skills"))
        )
        assertFalse(
            AgentExtensionsLegacyBridge.handles(LineDestination.Extension("linecode"))
        )
        assertFalse(
            AgentExtensionsLegacyBridge.handles(LineDestination.Extensions)
        )
        assertFalse(AgentExtensionsLegacyBridge.handles(null))
    }
}
