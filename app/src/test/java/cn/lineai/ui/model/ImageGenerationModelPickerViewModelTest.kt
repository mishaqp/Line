package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageGenerationModelPickerViewModelTest {

    @Test
    fun initialSnapshotPreservesOrderAndSelectionWithoutSelecting() {
        val repository = RecordingRepository(
            snapshot(
                listOf(item("int-a", "Alpha", "alpha-display"), item("int-b", "Beta", "beta-display")),
                "int-b"
            )
        )
        val viewModel = ImageGenerationModelPickerViewModel(repository)

        assertEquals(listOf("int-a", "int-b"), viewModel.state.value.models.map { it.internalId })
        assertEquals(listOf("Alpha", "Beta"), viewModel.state.value.models.map { it.name })
        assertEquals("int-b", viewModel.state.value.selectedInternalId)
        assertFalse(viewModel.state.value.models[0].selected)
        assertTrue(viewModel.state.value.models[1].selected)
        assertEquals(1, repository.snapshotReads)
        assertEquals(0, repository.selectedIds.size)
    }

    @Test
    fun emptyListIsAllowed() {
        val viewModel = ImageGenerationModelPickerViewModel(RecordingRepository())

        assertTrue(viewModel.state.value.models.isEmpty())
        assertEquals("", viewModel.state.value.selectedInternalId)
    }

    @Test
    fun reloadReadsFreshListAndSelectedIdWithoutSelectOrBack() {
        val repository = RecordingRepository(
            snapshot(listOf(item("int-a", "Alpha", "alpha")), "int-a")
        )
        val viewModel = ImageGenerationModelPickerViewModel(repository)
        repository.stored = snapshot(
            listOf(
                item("int-a", "Alpha", "alpha"),
                item("int-c", "Gamma", "gamma-display")
            ),
            "int-c"
        )

        assertNull(viewModel.onAction(ImageGenerationModelPickerUiAction.Reload))

        assertEquals(listOf("int-a", "int-c"), viewModel.state.value.models.map { it.internalId })
        assertEquals("int-c", viewModel.state.value.selectedInternalId)
        assertTrue(viewModel.state.value.models[1].selected)
        assertEquals(2, repository.snapshotReads)
        assertEquals(0, repository.selectedIds.size)
    }

    @Test
    fun backIsOneShotAndReloadDoesNotReplayIt() {
        val viewModel = ImageGenerationModelPickerViewModel(RecordingRepository())

        assertEquals(
            ImageGenerationModelPickerUiEffect.Back,
            viewModel.onAction(ImageGenerationModelPickerUiAction.Back)
        )
        assertNull(viewModel.onAction(ImageGenerationModelPickerUiAction.Reload))
        assertEquals(
            ImageGenerationModelPickerUiEffect.Back,
            viewModel.onAction(ImageGenerationModelPickerUiAction.Back)
        )
    }

    @Test
    fun selectModelPassesInternalIdOnceAndDoesNotAutoSelectOnRead() {
        val repository = RecordingRepository(
            snapshot(listOf(item("internal-42", "Image", "dall-e-display")), "internal-42")
        )
        val viewModel = ImageGenerationModelPickerViewModel(repository)

        assertNull(
            viewModel.onAction(ImageGenerationModelPickerUiAction.SelectModel("internal-42"))
        )
        assertEquals(listOf("internal-42"), repository.selectedIds)
        assertEquals(1, repository.snapshotReads)

        viewModel.onAction(ImageGenerationModelPickerUiAction.Reload)
        assertEquals(listOf("internal-42"), repository.selectedIds)
        assertEquals(2, repository.snapshotReads)
    }

    @Test
    fun snapshotErrorDoesNotCrashViewModel() {
        val repository = object : ImageGenerationModelPickerRepository {
            override fun snapshot(): ImageGenerationModelPickerSnapshot {
                throw IllegalStateException("boom")
            }

            override fun selectModel(internalId: String) = Unit
        }

        val viewModel = ImageGenerationModelPickerViewModel(repository)
        assertTrue(viewModel.state.value.models.isEmpty())
        assertEquals("", viewModel.state.value.selectedInternalId)
        assertNull(viewModel.onAction(ImageGenerationModelPickerUiAction.Reload))
        assertTrue(viewModel.state.value.models.isEmpty())
    }

    @Test
    fun uiStateAndToStringOmitSecretValues() {
        val secret = "super-secret-token-xyz"
        val repository = RecordingRepository(
            snapshot(
                listOf(item("int-a", "Safe Name", "safe-model-id")),
                "int-a"
            )
        )
        val viewModel = ImageGenerationModelPickerViewModel(repository)
        val stateText = viewModel.state.value.toString()

        assertFalse(stateText.contains(secret))
        viewModel.state.value.models.forEach { item ->
            assertFalse(item.toString().contains(secret))
            assertEquals("int-a", item.internalId)
            assertEquals("safe-model-id", item.displayedModelId)
        }
    }

    private class RecordingRepository(
        var stored: ImageGenerationModelPickerSnapshot = ImageGenerationModelPickerSnapshot()
    ) : ImageGenerationModelPickerRepository {
        var snapshotReads = 0
        val selectedIds = mutableListOf<String>()

        override fun snapshot(): ImageGenerationModelPickerSnapshot {
            snapshotReads += 1
            return stored
        }

        override fun selectModel(internalId: String) {
            selectedIds += internalId
        }
    }

    companion object {
        private fun item(
            internalId: String,
            name: String,
            displayedModelId: String,
            selected: Boolean = false
        ): ImageGenerationModelItemUi = ImageGenerationModelItemUi(
            internalId = internalId,
            name = name,
            displayedModelId = displayedModelId,
            badgeLabel = "OpenAI",
            badgeColor = 0xFF10A37F.toInt(),
            selected = selected
        )

        private fun snapshot(
            models: List<ImageGenerationModelItemUi>,
            selectedInternalId: String
        ): ImageGenerationModelPickerSnapshot {
            return ImageGenerationModelPickerSnapshot(
                models = models.map { it.copy(selected = it.internalId == selectedInternalId) },
                selectedInternalId = selectedInternalId
            )
        }
    }
}
