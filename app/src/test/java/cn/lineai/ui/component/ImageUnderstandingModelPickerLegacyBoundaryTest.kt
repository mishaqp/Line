package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUnderstandingModelPickerLegacyBoundaryTest {

    @Test
    fun bridgeHandlesOnlyImageUnderstandingModel() {
        assertTrue(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestination.ImageUnderstandingModel
            )
        )
        assertTrue(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestinations.fromScreenId("imageUnderstandingModel")
            )
        )

        assertFalse(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestination.ImageGenerationModel
            )
        )
        assertFalse(ImageUnderstandingModelPickerLegacyBridge.handles(LineDestination.Models))
        assertFalse(ImageUnderstandingModelPickerLegacyBridge.handles(LineDestination.ToolSettings))
        assertFalse(ImageUnderstandingModelPickerLegacyBridge.handles(LineDestination.Settings))
        assertFalse(ImageUnderstandingModelPickerLegacyBridge.handles(null))
        assertFalse(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestinations.fromScreenId("imageGenerationModel")
            )
        )
    }

    @Test
    fun screenRegistryRoutesUnderstandingToFoundationHostAndLeavesGenerationLegacy() {
        assertEquals(
            "cn.lineai.ui.component.ImageUnderstandingModelPickerHostView",
            ImageUnderstandingModelPickerHostView::class.java.name
        )
        assertEquals(
            "cn.lineai.ui.component.ImageUnderstandingModelPickerLegacyBridge",
            ImageUnderstandingModelPickerLegacyBridge::class.java.name
        )
        assertEquals(
            "imageUnderstandingModel",
            ScreenFactories.ImageUnderstandingModelScreenFactory().screenId()
        )
        assertEquals(
            "imageGenerationModel",
            ScreenFactories.ImageGenerationModelScreenFactory().screenId()
        )
        assertEquals(
            ScreenFactories.ImageUnderstandingModelScreenFactory::class.java,
            ScreenFactories.ImageUnderstandingModelScreenFactory().javaClass
        )
        assertEquals(
            ScreenFactories.ImageGenerationModelScreenFactory::class.java,
            ScreenFactories.ImageGenerationModelScreenFactory().javaClass
        )
        assertTrue(
            ImageUnderstandingModelPickerLegacyBridge.handles(LineDestination.ImageUnderstandingModel)
        )
        assertFalse(
            ImageUnderstandingModelPickerLegacyBridge.handles(LineDestination.ImageGenerationModel)
        )
        assertFalse(
            ImageUnderstandingModelPickerLegacyBridge.handles(LineDestination.Models)
        )
    }

    @Test
    fun screenFactoriesClassIsUnchangedFallback() {
        assertEquals(
            "cn.lineai.ui.component.ScreenFactories",
            ScreenFactories::class.java.name
        )
        assertEquals("imageUnderstandingModel", LineDestination.ImageUnderstandingModel.screenId)
        assertEquals("imageGenerationModel", LineDestination.ImageGenerationModel.screenId)
    }

    @Test
    fun backAndEmptyStackFallbackReturnToToolSettings() {
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageUnderstandingModel)
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestinations.fromScreenId("imageUnderstandingModel"))
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageGenerationModel)
        )
    }
}
