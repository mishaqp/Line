package cn.lineai.ui.component

import android.content.Context
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandLegacyBoundaryTest {

    @Test
    fun bridgeHandlesOnlyTypedShellCommand() {
        assertTrue(ShellCommandLegacyBridge.handles(LineDestination.ShellCommand))
        assertTrue(
            ShellCommandLegacyBridge.handles(LineDestinations.fromScreenId("shellCommand"))
        )

        assertFalse(ShellCommandLegacyBridge.handles(LineDestination.ToolCallPreview))
        assertFalse(ShellCommandLegacyBridge.handles(LineDestination.Settings))
        assertFalse(ShellCommandLegacyBridge.handles(LineDestination.Chat))
        assertFalse(ShellCommandLegacyBridge.handles(null))
        assertFalse(
            ShellCommandLegacyBridge.handles(LineDestination.Browser("https://example.com"))
        )
        assertFalse(
            ShellCommandLegacyBridge.handles(LineDestinations.fromScreenId("legacyUnknown"))
        )
    }

    @Test
    fun screenViewKeepsLegacyAndLiveSourceConstructors() {
        val fixed = ShellCommandScreenView::class.java.getConstructor(
            Context::class.java,
            String::class.java,
            ShellCommandScreenView.Listener::class.java
        )
        val live = ShellCommandScreenView::class.java.getConstructor(
            Context::class.java,
            ShellCommandSource::class.java,
            ShellCommandScreenView.Listener::class.java
        )
        assertEquals(3, fixed.parameterTypes.size)
        assertEquals(3, live.parameterTypes.size)
        assertEquals(
            "cn.lineai.ui.component.ShellCommandHostView",
            ShellCommandHostView::class.java.name
        )
        assertEquals(
            "cn.lineai.ui.component.ShellCommandLegacyBridge",
            ShellCommandLegacyBridge::class.java.name
        )
    }

    @Test
    fun factoriesRemainFallbackAndTypedIdIsUnchanged() {
        assertEquals("shellCommand", ScreenFactories.ShellCommandScreenFactory().screenId())
        assertEquals("shellCommand", LineDestination.ShellCommand.screenId)
        assertEquals(
            LineDestination.ShellCommand,
            LineDestinations.fromScreenId("shellCommand")
        )
        assertFalse(LineDestinations.fromScreenId("shellCommand") is LineDestination.Legacy)
        assertEquals(
            ScreenFactories.ShellCommandScreenFactory::class.java,
            ScreenFactories.ShellCommandScreenFactory().javaClass
        )
        assertEquals(
            "cn.lineai.ui.component.ScreenFactories",
            ScreenFactories::class.java.name
        )
    }

    @Test
    fun parentAndEmptyStackFallbackReturnToChat() {
        assertEquals(
            LineDestination.Chat,
            LineDestinations.parentOf(LineDestination.ShellCommand)
        )
        assertEquals(
            LineDestination.Chat,
            LineDestinations.parentOf(LineDestinations.fromScreenId("shellCommand"))
        )
    }
}
