package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpExtensionsLegacyBoundaryTest {

    @Test
    fun onlyTypedMcpExtensionDestinationUsesNewHost() {
        assertTrue(
            McpExtensionsLegacyBridge.handles(LineDestination.Extension("mcp"))
        )

        assertFalse(
            McpExtensionsLegacyBridge.handles(LineDestination.Extension("agent"))
        )
        assertFalse(
            McpExtensionsLegacyBridge.handles(LineDestination.Extension("skills"))
        )
        assertFalse(
            McpExtensionsLegacyBridge.handles(LineDestination.Extension("linecode"))
        )
        assertFalse(
            McpExtensionsLegacyBridge.handles(LineDestination.Extensions)
        )
        assertFalse(McpExtensionsLegacyBridge.handles(null))
    }
}
