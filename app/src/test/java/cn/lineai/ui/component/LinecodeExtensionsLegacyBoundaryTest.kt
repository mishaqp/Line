package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinecodeExtensionsLegacyBoundaryTest {

    @Test
    fun onlyTypedLinecodeExtensionDestinationUsesNewHost() {
        assertTrue(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extension("linecode"))
        )

        assertFalse(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extension("skills"))
        )
        assertFalse(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extension("agent"))
        )
        assertFalse(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extension("mcp"))
        )
        assertFalse(
            LinecodeExtensionsLegacyBridge.handles(LineDestination.Extensions)
        )
        assertFalse(LinecodeExtensionsLegacyBridge.handles(null))
    }
}
