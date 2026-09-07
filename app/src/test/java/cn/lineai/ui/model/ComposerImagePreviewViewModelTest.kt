package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerImagePreviewViewModelTest {
    @Test
    fun showStoresImmutableUiStateAndMarksPreviewVisible() {
        val viewModel = ComposerImagePreviewViewModel()

        val effect = viewModel.onAction(
            ComposerImagePreviewUiAction.Show(
                uri = "content://image/1",
                base64 = "encoded",
                mimeType = "image/png",
                name = "preview.png"
            )
        )

        assertEquals(ComposerImagePreviewUiEffect.ImageStateChanged(true), effect)
        assertTrue(viewModel.state.value.visible)
        assertTrue(viewModel.state.value.hasImage)
        assertEquals("content://image/1", viewModel.state.value.uri)
        assertEquals("encoded", viewModel.state.value.base64)
        assertEquals("image/png", viewModel.state.value.mimeType)
        assertEquals("preview.png", viewModel.state.value.name)
    }

    @Test
    fun showNormalizesNullablePayloadFieldsWithoutHidingPreview() {
        val viewModel = ComposerImagePreviewViewModel()

        viewModel.onAction(
            ComposerImagePreviewUiAction.Show(
                uri = null,
                base64 = null,
                mimeType = null,
                name = null
            )
        )

        assertTrue(viewModel.state.value.visible)
        assertFalse(viewModel.state.value.hasImage)
        assertEquals("", viewModel.state.value.base64)
        assertEquals("", viewModel.state.value.mimeType)
        assertEquals("", viewModel.state.value.name)
    }

    @Test
    fun clearDropsAllStagedImageState() {
        val viewModel = ComposerImagePreviewViewModel()
        viewModel.onAction(
            ComposerImagePreviewUiAction.Show(
                uri = "content://image/2",
                base64 = "encoded",
                mimeType = "image/jpeg",
                name = "photo.jpg"
            )
        )

        val effect = viewModel.onAction(ComposerImagePreviewUiAction.Clear)

        assertEquals(ComposerImagePreviewUiEffect.ImageStateChanged(false), effect)
        assertFalse(viewModel.state.value.visible)
        assertFalse(viewModel.state.value.hasImage)
        assertEquals(null, viewModel.state.value.uri)
        assertEquals("", viewModel.state.value.base64)
        assertEquals("", viewModel.state.value.mimeType)
        assertEquals("", viewModel.state.value.name)
    }
}
