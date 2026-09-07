package cn.lineai.ui.component

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageGenerationModelPickerLegacyBoundaryTest {

    @Test
    fun bridgeHandlesOnlyImageGenerationModel() {
        assertTrue(
            ImageGenerationModelPickerLegacyBridge.handles(
                LineDestination.ImageGenerationModel
            )
        )
        assertTrue(
            ImageGenerationModelPickerLegacyBridge.handles(
                LineDestinations.fromScreenId("imageGenerationModel")
            )
        )

        assertFalse(
            ImageGenerationModelPickerLegacyBridge.handles(
                LineDestination.ImageUnderstandingModel
            )
        )
        assertFalse(ImageGenerationModelPickerLegacyBridge.handles(LineDestination.Models))
        assertFalse(ImageGenerationModelPickerLegacyBridge.handles(LineDestination.ToolSettings))
        assertFalse(ImageGenerationModelPickerLegacyBridge.handles(LineDestination.Settings))
        assertFalse(ImageGenerationModelPickerLegacyBridge.handles(null))
        assertFalse(
            ImageGenerationModelPickerLegacyBridge.handles(
                LineDestinations.fromScreenId("imageUnderstandingModel")
            )
        )
    }

    @Test
    fun understandingBridgeStillHandlesOnlyUnderstanding() {
        assertTrue(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestination.ImageUnderstandingModel
            )
        )
        assertFalse(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestination.ImageGenerationModel
            )
        )
        assertFalse(
            ImageUnderstandingModelPickerLegacyBridge.handles(
                LineDestinations.fromScreenId("imageGenerationModel")
            )
        )
    }

    @Test
    fun screenRegistryKeepsLegacyFactoriesAsFallbackAndTypedIdsUnchanged() {
        assertEquals(
            "cn.lineai.ui.component.ImageGenerationModelPickerHostView",
            ImageGenerationModelPickerHostView::class.java.name
        )
        assertEquals(
            "cn.lineai.ui.component.ImageGenerationModelPickerLegacyBridge",
            ImageGenerationModelPickerLegacyBridge::class.java.name
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
        assertEquals("imageUnderstandingModel", LineDestination.ImageUnderstandingModel.screenId)
        assertEquals("imageGenerationModel", LineDestination.ImageGenerationModel.screenId)
        assertEquals(
            "cn.lineai.ui.component.ScreenFactories",
            ScreenFactories::class.java.name
        )
    }

    @Test
    fun backAndEmptyStackFallbackReturnToToolSettings() {
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageGenerationModel)
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestinations.fromScreenId("imageGenerationModel"))
        )
        assertEquals(
            LineDestination.ToolSettings,
            LineDestinations.parentOf(LineDestination.ImageUnderstandingModel)
        )
    }
}
