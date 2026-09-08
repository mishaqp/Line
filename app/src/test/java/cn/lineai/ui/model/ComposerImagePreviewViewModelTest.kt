package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerImagePreviewViewModelTest {
    private class FakeRepository : ComposerImagePreviewStateRepository {
        private var current = ComposerImagePreviewSnapshot()

        override fun snapshot() = current

        override fun show(uri: String?, base64: String?, mimeType: String?, name: String?) {
            current = ComposerImagePreviewSnapshot(
                visible = true,
                uri = uri,
                base64 = base64.orEmpty(),
                mimeType = mimeType.orEmpty(),
                name = name.orEmpty(),
                revision = current.revision + 1
            )
        }

        override fun clear() {
            current = ComposerImagePreviewSnapshot(revision = current.revision + 1)
        }
    }

    @Test
    fun showStoresRepositorySnapshotAndMarksPreviewVisible() {
        val viewModel = ComposerImagePreviewViewModel(FakeRepository())

        val effect = viewModel.onAction(
            ComposerImagePreviewUiAction.Show(
                "content://image/1",
                "encoded",
                "image/png",
                "preview.png"
            )
        )

        assertEquals(ComposerImagePreviewUiEffect.ImageStateChanged(true), effect)
        assertTrue(viewModel.state.value.visible)
        assertTrue(viewModel.state.value.hasImage)
        assertEquals("preview.png", viewModel.state.value.name)
    }

    @Test
    fun repeatedShowAlwaysAdvancesRevision() {
        val viewModel = ComposerImagePreviewViewModel(FakeRepository())
        val action = ComposerImagePreviewUiAction.Show(
            "content://image/1",
            "encoded",
            "image/png",
            "preview.png"
        )

        viewModel.onAction(action)
        val firstRevision = viewModel.state.value.revision
        viewModel.onAction(action)

        assertTrue(viewModel.state.value.revision > firstRevision)
    }

    @Test
    fun clearDropsAllStagedImageState() {
        val viewModel = ComposerImagePreviewViewModel(FakeRepository())
        viewModel.onAction(
            ComposerImagePreviewUiAction.Show(
                "content://image/2",
                "encoded",
                "image/jpeg",
                "photo.jpg"
            )
        )

        val effect = viewModel.onAction(ComposerImagePreviewUiAction.Clear)

        assertEquals(ComposerImagePreviewUiEffect.ImageStateChanged(false), effect)
        assertFalse(viewModel.state.value.visible)
        assertFalse(viewModel.state.value.hasImage)
        assertEquals(null, viewModel.state.value.uri)
    }
}
