package cn.lineai.ui.component

import android.content.Context
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallPreviewLegacyBoundaryTest {

    @Test
    fun screenViewKeepsLegacyConstructor() {
        val constructor = ToolCallPreviewScreenView::class.java.getConstructor(
            Context::class.java,
            Runnable::class.java
        )
        assertEquals(2, constructor.parameterTypes.size)
        assertEquals(
            "cn.lineai.ui.component.ToolCallPreviewHostView",
            ToolCallPreviewHostView::class.java.name
        )
        assertFalse(
            android.widget.LinearLayout::class.java.isAssignableFrom(
                ToolCallPreviewScreenView::class.java
            )
        )
    }

    @Test
    fun factoryAndTypedDestinationStayOnToolCallPreview() {
        assertEquals(
            "toolcall_preview",
            ScreenFactories.ToolCallPreviewScreenFactory().screenId()
        )
        assertEquals(
            LineDestination.ToolCallPreview,
            LineDestinations.fromScreenId("toolcall_preview")
        )
        assertEquals("toolcall_preview", LineDestination.ToolCallPreview.screenId)
        assertEquals(
            "cn.lineai.ui.component.ScreenFactories",
            ScreenFactories::class.java.name
        )
    }

    @Test
    fun parentAndEmptyStackFallbackReturnToOutput() {
        assertEquals(
            LineDestination.Output,
            LineDestinations.parentOf(LineDestination.ToolCallPreview)
        )
        assertEquals(
            LineDestination.Output,
            LineDestinations.parentOf(LineDestinations.fromScreenId("toolcall_preview"))
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.Output)
        )
    }

    @Test
    fun outputStillOwnsTypedPreviewChild() {
        assertTrue(
            LineDestinations.parentOf(LineDestination.ToolCallPreview)
                === LineDestination.Output
        )
        assertFalse(
            LineDestinations.fromScreenId("toolcall_preview") is LineDestination.ImageGenerationModel
        )
        assertFalse(
            LineDestinations.fromScreenId("toolcall_preview") is LineDestination.ImageUnderstandingModel
        )
    }
}
