package cn.lineai.ui.model

import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExtensionEditorViewModelTest {

    @Test
    fun existingEditorLoadsDraftAndPreservesIdentityAndHiddenSelectionsOnSave() {
        val identity = AgentExtensionIdentity("agent-id", false, 11L, 22L)
        val repository = FakeRepository(
            snapshot(
                identity = identity,
                draft = draft(
                    name = "Existing",
                    toolNames = listOf("visible", "hidden-tool"),
                    mcpIds = listOf("custom:visible", "custom:hidden")
                )
            )
        )
        val viewModel = AgentExtensionEditorViewModel(repository, QueuedDispatcher())

        assertEquals(setOf("visible", "hidden-tool"), viewModel.state.value.selectedToolNames)
        assertEquals(setOf("custom:visible", "custom:hidden"), viewModel.state.value.selectedMcpIds)

        viewModel.onAction(AgentExtensionEditorUiAction.ToggleTool("visible"))
        viewModel.onAction(AgentExtensionEditorUiAction.Save)

        val saved = repository.savedRequests.single()
        assertEquals(identity, saved.identity)
        assertFalse(saved.toolNames.contains("visible"))
        assertTrue(saved.toolNames.contains("hidden-tool"))
        assertTrue(saved.mcpIds.contains("custom:hidden"))
    }

    @Test
    fun newEditorSelectsOnlyAvailableDefaultTools() {
        val repository = FakeRepository(
            AgentExtensionEditorSnapshot(
                identity = null,
                initialDraft = null,
                tools = listOf(
                    AgentToolOption("read", "desc", true),
                    AgentToolOption("glob", "desc", true),
                    AgentToolOption("shell", "desc", false)
                ),
                mcps = emptyList()
            )
        )

        val state = AgentExtensionEditorViewModel(repository, QueuedDispatcher()).state.value

        assertEquals(setOf("read", "glob"), state.selectedToolNames)
        assertTrue(state.selectedMcpIds.isEmpty())
    }

    @Test
    fun saveTrimsFieldsAndUsesExactLegacySlugNormalization() {
        val repository = FakeRepository(snapshot())
        val viewModel = AgentExtensionEditorViewModel(repository, QueuedDispatcher())

        viewModel.onAction(AgentExtensionEditorUiAction.SetName(" 123 Hello__ "))
        viewModel.onAction(AgentExtensionEditorUiAction.SetPrompt(" prompt "))
        viewModel.onAction(AgentExtensionEditorUiAction.SetTrigger(" trigger "))
        viewModel.onAction(AgentExtensionEditorUiAction.Save)

        val saved = repository.savedRequests.single()
        assertNull(saved.identity)
        assertEquals("123 Hello__", saved.name)
        assertEquals("agent-123-hello", saved.slug)
        assertEquals("prompt", saved.prompt)
        assertEquals("trigger", saved.trigger)
    }

    @Test
    fun invalidSaveAndBackNeverMutateRepository() {
        val repository = FakeRepository(snapshot())
        val viewModel = AgentExtensionEditorViewModel(repository, QueuedDispatcher())

        val invalid = viewModel.onAction(AgentExtensionEditorUiAction.Save)
        val back = viewModel.onAction(AgentExtensionEditorUiAction.Back)

        assertTrue(invalid is AgentExtensionEditorUiEffect.SaveRequiresFields)
        assertTrue(back is AgentExtensionEditorUiEffect.Back)
        assertTrue(repository.savedRequests.isEmpty())
    }

    @Test
    fun blankAiDescriptionDoesNotGenerate() {
        val repository = FakeRepository(snapshot())
        val viewModel = AgentExtensionEditorViewModel(repository, QueuedDispatcher())
        viewModel.onAction(AgentExtensionEditorUiAction.OpenAiDialog)
        viewModel.onAction(AgentExtensionEditorUiAction.SetAiDescription("   "))

        val effect = viewModel.onAction(AgentExtensionEditorUiAction.GenerateDraft)

        assertTrue(effect is AgentExtensionEditorUiEffect.GenerateRequiresDescription)
        assertEquals(0, repository.generateCallCount)
        assertTrue(viewModel.state.value.showAiDialog)
        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun generationIsSingleFlightAndAtomicallyReplacesTheForm() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.generatedDraftValue = draft(
            name = "Generated",
            slug = "generated",
            prompt = "Generated prompt",
            trigger = "Generated trigger",
            toolNames = listOf("hidden-generated-tool"),
            mcpIds = listOf("custom:hidden-generated")
        )
        val viewModel = AgentExtensionEditorViewModel(repository, dispatcher)
        viewModel.onAction(AgentExtensionEditorUiAction.OpenAiDialog)
        viewModel.onAction(AgentExtensionEditorUiAction.SetAiDescription("Build an agent"))

        viewModel.onAction(AgentExtensionEditorUiAction.GenerateDraft)
        viewModel.onAction(AgentExtensionEditorUiAction.GenerateDraft)
        assertTrue(viewModel.state.value.isGenerating)
        dispatcher.runAll()

        assertEquals(1, repository.generateCallCount)
        assertEquals("Build an agent", repository.lastGenerateDescription)
        assertEquals("Generated", viewModel.state.value.name)
        assertEquals(setOf("hidden-generated-tool"), viewModel.state.value.selectedToolNames)
        assertEquals(setOf("custom:hidden-generated"), viewModel.state.value.selectedMcpIds)
        assertFalse(viewModel.state.value.showAiDialog)
        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun missingDraftKeepsDialogOpenAndCanRetry() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.generatedDraftValue = null
        val viewModel = AgentExtensionEditorViewModel(repository, dispatcher)
        viewModel.onAction(AgentExtensionEditorUiAction.OpenAiDialog)
        viewModel.onAction(AgentExtensionEditorUiAction.SetAiDescription("Description"))

        viewModel.onAction(AgentExtensionEditorUiAction.GenerateDraft)
        dispatcher.runAll()

        assertTrue(viewModel.state.value.showAiDialog)
        assertFalse(viewModel.state.value.isGenerating)
        viewModel.onAction(AgentExtensionEditorUiAction.GenerateDraft)
        dispatcher.runAll()
        assertEquals(2, repository.generateCallCount)
    }

    @Test
    fun successfulSaveIsProtectedFromDoubleTap() {
        val repository = FakeRepository(snapshot())
        val viewModel = AgentExtensionEditorViewModel(repository, QueuedDispatcher())
        viewModel.onAction(AgentExtensionEditorUiAction.SetName("Agent"))
        viewModel.onAction(AgentExtensionEditorUiAction.SetSlug("agent"))
        viewModel.onAction(AgentExtensionEditorUiAction.SetPrompt("Prompt"))

        viewModel.onAction(AgentExtensionEditorUiAction.Save)
        viewModel.onAction(AgentExtensionEditorUiAction.Save)

        assertEquals(1, repository.savedRequests.size)
        assertTrue(viewModel.state.value.isSaving)
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.addLast(block)
        }

        fun runAll() {
            while (queue.isNotEmpty()) {
                queue.removeFirst().run()
            }
        }
    }

    private class FakeRepository(
        private val snapshotValue: AgentExtensionEditorSnapshot
    ) : AgentExtensionEditorRepository {
        var generatedDraftValue: AgentExtensionDraft? = null
        var generateErrorValue: Exception? = null
        var saveErrorValue: Exception? = null
        var generateCallCount: Int = 0
        var lastGenerateDescription: String = ""
        val savedRequests = mutableListOf<AgentExtensionSaveRequest>()

        override fun loadSnapshot(): AgentExtensionEditorSnapshot = snapshotValue

        override fun generateDraft(description: String): AgentExtensionDraft? {
            generateCallCount++
            lastGenerateDescription = description
            generateErrorValue?.let { throw it }
            return generatedDraftValue
        }

        override fun saveAgentExtension(request: AgentExtensionSaveRequest) {
            saveErrorValue?.let { throw it }
            savedRequests += request
        }
    }

    companion object {
        private fun snapshot(
            identity: AgentExtensionIdentity? = null,
            draft: AgentExtensionDraft? = null
        ) = AgentExtensionEditorSnapshot(
            identity = identity,
            initialDraft = draft,
            tools = listOf(AgentToolOption("visible", "utility · visible", false)),
            mcps = listOf(AgentMcpOption("custom:visible", "Visible", "1/1 tools · url"))
        )

        private fun draft(
            name: String = "Agent",
            slug: String = "agent",
            prompt: String = "Prompt",
            trigger: String = "",
            toolNames: List<String> = emptyList(),
            mcpIds: List<String> = emptyList()
        ) = AgentExtensionDraft(name, slug, prompt, trigger, toolNames, mcpIds)
    }
}
