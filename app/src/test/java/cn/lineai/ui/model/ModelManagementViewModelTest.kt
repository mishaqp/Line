package cn.lineai.ui.model

import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.model.ModelProviderPresets
import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelManagementViewModelTest {
    @Test
    fun selectModelUpdatesStateImmediately() {
        val repository = FakeRepository()
        val viewModel = ModelManagementViewModel(repository)

        viewModel.onAction(ModelManagementUiAction.SelectModel("m2"))

        assertEquals("m2", repository.selectedId)
        assertEquals("m2", viewModel.state.value.selectedModelId)
        assertTrue(viewModel.state.value.models.first { it.id == "m2" }.selected)
        assertFalse(viewModel.state.value.models.first { it.id == "m1" }.selected)
    }

    @Test
    fun enterAndExitMultiSelect() {
        val viewModel = ModelManagementViewModel(FakeRepository())

        viewModel.onAction(ModelManagementUiAction.LongPressModel("m1"))
        assertEquals("m1", viewModel.state.value.pendingActionModelId)

        viewModel.onAction(ModelManagementUiAction.StartMultiSelect)
        assertTrue(viewModel.state.value.multiSelectActive)
        assertEquals(setOf("m1"), viewModel.state.value.multiSelectedIds)
        assertEquals(null, viewModel.state.value.pendingActionModelId)

        viewModel.onAction(ModelManagementUiAction.ToggleMultiSelect("m2"))
        assertEquals(setOf("m1", "m2"), viewModel.state.value.multiSelectedIds)

        viewModel.onAction(ModelManagementUiAction.ToggleMultiSelect("m1"))
        assertEquals(setOf("m2"), viewModel.state.value.multiSelectedIds)

        viewModel.onAction(ModelManagementUiAction.ExitMultiSelect)
        assertFalse(viewModel.state.value.multiSelectActive)
        assertTrue(viewModel.state.value.multiSelectedIds.isEmpty())
    }

    @Test
    fun deleteModelsUpdatesStateWithoutReopening() {
        val repository = FakeRepository()
        val viewModel = ModelManagementViewModel(repository)

        viewModel.onAction(ModelManagementUiAction.LongPressModel("m1"))
        viewModel.onAction(ModelManagementUiAction.StartMultiSelect)
        viewModel.onAction(ModelManagementUiAction.ToggleMultiSelect("m2"))
        viewModel.onAction(ModelManagementUiAction.RequestDelete)
        assertTrue(viewModel.state.value.pendingDelete)

        viewModel.onAction(ModelManagementUiAction.ConfirmDelete)

        assertEquals(listOf("m1", "m2"), repository.deletedIds)
        assertEquals(listOf("m3"), viewModel.state.value.models.map { it.id })
        assertFalse(viewModel.state.value.multiSelectActive)
        assertFalse(viewModel.state.value.pendingDelete)
    }

    @Test
    fun addAndEditActionsProduceTypedDestinations() {
        val viewModel = ModelManagementViewModel(FakeRepository())

        assertEquals(
            LineDestination.ModelAddOptions,
            viewModel.onAction(ModelManagementUiAction.AddModel)
        )
        assertEquals(
            LineDestination.ModelAdd,
            viewModel.onAction(ModelManagementUiAction.AddCustom)
        )
        assertEquals(
            LineDestination.ModelAddLocal,
            viewModel.onAction(ModelManagementUiAction.AddLocal)
        )
        assertEquals(
            LineDestination.ModelAddPreset("codex"),
            viewModel.onAction(ModelManagementUiAction.AddPreset("codex"))
        )
        assertEquals(
            LineDestination.ModelAddPreset("deepseek"),
            viewModel.destinationForAdd("deepseek")
        )

        viewModel.onAction(ModelManagementUiAction.LongPressModel("m3"))
        assertEquals(
            LineDestination.ModelEdit("m3"),
            viewModel.onAction(ModelManagementUiAction.EditPendingModel)
        )
        assertEquals(LineDestination.ModelEdit("m2"), viewModel.destinationForEdit("m2"))
    }

    @Test
    fun stateSurvivesRepeatedReadsLikeRecomposition() {
        val viewModel = ModelManagementViewModel(FakeRepository())
        viewModel.onAction(ModelManagementUiAction.SelectModel("m2"))
        viewModel.onAction(ModelManagementUiAction.LongPressModel("m2"))
        viewModel.onAction(ModelManagementUiAction.StartMultiSelect)

        val first = viewModel.state.value
        val second = viewModel.state.value

        assertEquals(first, second)
        assertEquals("m2", second.selectedModelId)
        assertEquals(setOf("m2"), second.multiSelectedIds)
        assertEquals(first.addPresets, second.addPresets)
        assertEquals(ModelProviderPresets.all().size, second.addPresets.size)
    }

    @Test
    fun customProviderLabelFallsBackToProtocol() {
        val custom = model("c1", "Mine", ModelProtocolType.OPENAI_COMPATIBLE, "自定义")
        assertEquals("OpenAI", ModelManagementViewModel.displayProvider(custom))
        assertEquals(
            ModelManagementViewModel.BADGE_CODEX,
            ModelManagementViewModel.badgeColor(
                model("x", "Codex", ModelProtocolType.CODEX_RESPONSES, "Codex")
            )
        )
    }

    private class FakeRepository : ModelManagementRepository {
        var items: MutableList<ModelConfig> = mutableListOf(
            model("m1", "One"),
            model("m2", "Two"),
            model("m3", "Three")
        )
        var selectedId: String = "m1"
        var deletedIds: List<String> = emptyList()

        override fun models(): List<ModelConfig> = items.toList()

        override fun selectedModelId(): String = selectedId

        override fun selectModel(id: String) {
            selectedId = id
        }

        override fun deleteModels(ids: List<String>) {
            deletedIds = ids
            items.removeAll { ids.contains(it.id) }
            if (ids.contains(selectedId)) {
                selectedId = items.firstOrNull()?.id.orEmpty()
            }
        }
    }
}

private fun model(
    id: String,
    name: String,
    protocol: ModelProtocolType = ModelProtocolType.OPENAI_COMPATIBLE,
    provider: String = "OpenAI"
): ModelConfig {
    return ModelConfig.builder(
        id,
        name,
        protocol,
        provider,
        "https://example.invalid",
        "key",
        name.lowercase()
    ).build()
}
