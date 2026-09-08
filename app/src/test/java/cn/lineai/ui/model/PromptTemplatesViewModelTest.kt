package cn.lineai.ui.model

import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplatesViewModelTest {
    @Test
    fun initialStateLoadsRepositoryTemplates() {
        val viewModel = PromptTemplatesViewModel(FakeRepository())

        val templates = viewModel.state.value.templates
        assertEquals(2, templates.size)
        assertEquals("systemPrompt", templates[0].id)
        assertEquals("Default system", templates[0].currentText)
        assertFalse(templates[0].customized)
        assertEquals("toneChat", templates[1].id)
        assertEquals("Chat tone", templates[1].currentText)
        assertTrue(templates[1].customized)
    }

    @Test
    fun updatingOneDraftDoesNotChangeOtherTemplatesOrPersist() {
        val repository = FakeRepository()
        val viewModel = PromptTemplatesViewModel(repository)
        val otherBefore = viewModel.state.value.templates[1]

        viewModel.onAction(PromptTemplatesUiAction.UpdateDraft("systemPrompt", "Edited system"))

        val state = viewModel.state.value
        assertEquals("Edited system", state.templates[0].currentText)
        assertEquals(otherBefore, state.templates[1])
        assertTrue(repository.writes.isEmpty())
        assertEquals("Default system", repository.template("systemPrompt").currentText)
    }

    @Test
    fun savePersistsOnlyTheEditedTemplateAndUpdatesStatusImmediately() {
        val repository = FakeRepository()
        val viewModel = PromptTemplatesViewModel(repository)
        val otherBefore = viewModel.state.value.templates[1]

        viewModel.onAction(PromptTemplatesUiAction.UpdateDraft("systemPrompt", "Saved system"))
        viewModel.onAction(PromptTemplatesUiAction.Save("systemPrompt"))

        val saved = viewModel.state.value.templates[0]
        assertEquals("Saved system", saved.currentText)
        assertTrue(saved.customized)
        assertEquals("Saved system", repository.template("systemPrompt").currentText)
        assertEquals(listOf("save:systemPrompt:Saved system"), repository.writes)
        assertEquals(otherBefore, viewModel.state.value.templates[1])
        assertEquals("Chat tone", repository.template("toneChat").currentText)
    }

    @Test
    fun resetRestoresDefaultWithoutChangingOtherTemplates() {
        val repository = FakeRepository()
        val viewModel = PromptTemplatesViewModel(repository)
        val otherBefore = viewModel.state.value.templates[0]

        viewModel.onAction(PromptTemplatesUiAction.Reset("toneChat"))

        val reset = viewModel.state.value.templates[1]
        assertEquals("Default chat", reset.currentText)
        assertFalse(reset.customized)
        assertEquals("Default chat", repository.template("toneChat").currentText)
        assertEquals(listOf("reset:toneChat"), repository.writes)
        assertEquals(otherBefore, viewModel.state.value.templates[0])
    }

    @Test
    fun recreatingViewModelReloadsPersistedValues() {
        val repository = FakeRepository()
        val first = PromptTemplatesViewModel(repository)
        first.onAction(PromptTemplatesUiAction.UpdateDraft("systemPrompt", "Persisted"))
        first.onAction(PromptTemplatesUiAction.Save("systemPrompt"))
        first.onAction(PromptTemplatesUiAction.Reset("toneChat"))

        val second = PromptTemplatesViewModel(repository)
        assertEquals("Persisted", second.state.value.templates[0].currentText)
        assertTrue(second.state.value.templates[0].customized)
        assertEquals("Default chat", second.state.value.templates[1].currentText)
        assertFalse(second.state.value.templates[1].customized)
        assertEquals(first.state.value, second.state.value)
    }

    @Test
    fun stateSurvivesRepeatedReadsLikeRecomposition() {
        val viewModel = PromptTemplatesViewModel(FakeRepository())
        viewModel.onAction(PromptTemplatesUiAction.UpdateDraft("systemPrompt", "Draft"))

        val first = viewModel.state.value
        val second = viewModel.state.value
        assertEquals(first, second)
        assertEquals("Draft", second.templates[0].currentText)
        assertEquals("Chat tone", second.templates[1].currentText)
    }

    @Test
    fun promptTemplatesUsesTypedLlmParent() {
        val viewModel = PromptTemplatesViewModel(FakeRepository())

        assertNull(viewModel.onAction(PromptTemplatesUiAction.Back))
        assertEquals(
            LineDestination.PromptTemplates,
            LineDestinations.fromScreenId("promptTemplates")
        )
        assertEquals(
            LineDestination.Llm,
            LineDestinations.parentOf(LineDestination.PromptTemplates)
        )
        assertFalse(LineDestinations.fromScreenId("promptTemplates") is LineDestination.Legacy)
    }

    private class FakeRepository : PromptTemplatesRepository {
        private var items = listOf(
            PromptTemplateUi(
                id = "systemPrompt",
                title = "System",
                description = "System desc",
                sourceLabel = "prompts/system.txt",
                variables = listOf("TOOLS_CONTEXT"),
                defaultText = "Default system",
                currentText = "Default system",
                customized = false
            ),
            PromptTemplateUi(
                id = "toneChat",
                title = "Chat tone",
                description = "Tone desc",
                sourceLabel = "prompts/tone-chat.txt",
                variables = emptyList(),
                defaultText = "Default chat",
                currentText = "Chat tone",
                customized = true
            )
        )
        val writes = mutableListOf<String>()

        override fun templates(): List<PromptTemplateUi> = items

        override fun saveTemplate(id: String, value: String) {
            writes += "save:$id:$value"
            items = items.map { item ->
                if (item.id == id) {
                    item.copy(currentText = value, customized = value != item.defaultText)
                } else {
                    item
                }
            }
        }

        override fun resetTemplate(id: String) {
            writes += "reset:$id"
            items = items.map { item ->
                if (item.id == id) {
                    item.copy(currentText = item.defaultText, customized = false)
                } else {
                    item
                }
            }
        }

        fun template(id: String): PromptTemplateUi = items.first { it.id == id }
    }
}
