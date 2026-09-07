package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUnderstandingModelPickerViewModelTest {

    @Test
    fun initialSnapshotPreservesOrderAndSelectionWithoutSelecting() {
        val repository = RecordingRepository(
            snapshot(
                listOf(item("int-a", "Alpha", "alpha-display"), item("int-b", "Beta", "beta-display")),
                "int-b"
            )
        )
        val viewModel = ImageUnderstandingModelPickerViewModel(repository)

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
        val viewModel = ImageUnderstandingModelPickerViewModel(RecordingRepository())

        assertTrue(viewModel.state.value.models.isEmpty())
        assertEquals("", viewModel.state.value.selectedInternalId)
    }

    @Test
    fun reloadReadsFreshListAndSelectedIdWithoutSelectOrBack() {
        val repository = RecordingRepository(
            snapshot(listOf(item("int-a", "Alpha", "alpha")), "int-a")
        )
        val viewModel = ImageUnderstandingModelPickerViewModel(repository)
        repository.stored = snapshot(
            listOf(
                item("int-a", "Alpha", "alpha"),
                item("int-c", "Gamma", "gamma-display")
            ),
            "int-c"
        )

        assertNull(viewModel.onAction(ImageUnderstandingModelPickerUiAction.Reload))

        assertEquals(listOf("int-a", "int-c"), viewModel.state.value.models.map { it.internalId })
        assertEquals("int-c", viewModel.state.value.selectedInternalId)
        assertTrue(viewModel.state.value.models[1].selected)
        assertEquals(2, repository.snapshotReads)
        assertEquals(0, repository.selectedIds.size)
    }

    @Test
    fun backIsOneShotAndReloadDoesNotReplayIt() {
        val viewModel = ImageUnderstandingModelPickerViewModel(RecordingRepository())

        assertEquals(
            ImageUnderstandingModelPickerUiEffect.Back,
            viewModel.onAction(ImageUnderstandingModelPickerUiAction.Back)
        )
        assertNull(viewModel.onAction(ImageUnderstandingModelPickerUiAction.Reload))
        assertEquals(
            ImageUnderstandingModelPickerUiEffect.Back,
            viewModel.onAction(ImageUnderstandingModelPickerUiAction.Back)
        )
    }

    @Test
    fun selectModelPassesInternalIdOnceAndNeverDisplayedModelId() {
        val repository = RecordingRepository(
            snapshot(listOf(item("internal-42", "Vision", "gpt-4o-display")), "internal-42")
        )
        val viewModel = ImageUnderstandingModelPickerViewModel(repository)

        assertNull(
            viewModel.onAction(ImageUnderstandingModelPickerUiAction.SelectModel("internal-42"))
        )
        assertEquals(listOf("internal-42"), repository.selectedIds)

        viewModel.onAction(ImageUnderstandingModelPickerUiAction.SelectModel("gpt-4o-display"))
        assertEquals(listOf("internal-42", "gpt-4o-display"), repository.selectedIds)
        assertEquals(1, repository.selectedIds.count { it == "internal-42" })
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
        val viewModel = ImageUnderstandingModelPickerViewModel(repository)
        val stateText = viewModel.state.value.toString()

        assertFalse(stateText.contains(secret))
        viewModel.state.value.models.forEach { item ->
            assertFalse(item.toString().contains(secret))
            assertEquals("int-a", item.internalId)
            assertEquals("safe-model-id", item.displayedModelId)
        }
    }

    private class RecordingRepository(
        var stored: ImageUnderstandingModelPickerSnapshot = ImageUnderstandingModelPickerSnapshot()
    ) : ImageUnderstandingModelPickerRepository {
        var snapshotReads = 0
        val selectedIds = mutableListOf<String>()

        override fun snapshot(): ImageUnderstandingModelPickerSnapshot {
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
        ): ImageUnderstandingModelItemUi = ImageUnderstandingModelItemUi(
            internalId = internalId,
            name = name,
            displayedModelId = displayedModelId,
            badgeLabel = "OpenAI",
            badgeColor = 0xFF10A37F.toInt(),
            selected = selected
        )

        private fun snapshot(
            models: List<ImageUnderstandingModelItemUi>,
            selectedInternalId: String
        ): ImageUnderstandingModelPickerSnapshot {
            return ImageUnderstandingModelPickerSnapshot(
                models = models.map { it.copy(selected = it.internalId == selectedInternalId) },
                selectedInternalId = selectedInternalId
            )
        }
    }
}
