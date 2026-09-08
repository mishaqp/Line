package cn.lineai.ui.model

import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpRequestHeader
import cn.lineai.model.McpToolSummary
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class McpExtensionEditorViewModelTest {

    @Test
    fun existingEditorPreservesIdentityDefaultsAndAllowsRenameWithoutQuery() {
        val original = existingConfig()
        val repository = FakeRepository(original)
        val viewModel = McpExtensionEditorViewModel(repository, QueuedDispatcher())

        viewModel.onAction(McpExtensionEditorUiAction.SetName(" Renamed "))
        viewModel.onAction(McpExtensionEditorUiAction.Save)

        val saved = repository.saved.single()
        assertEquals(original.id, saved.id)
        assertEquals(original.isEnabled, saved.isEnabled)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals(original.updatedAt, saved.updatedAt)
        assertEquals("Renamed", saved.name)
        assertEquals(original.tools.size, saved.tools.size)
    }

    @Test
    fun newEditorUsesBackendDefaultsInInput() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(null)
        repository.queryResult = listOf(tool("one"))
        val viewModel = McpExtensionEditorViewModel(repository, dispatcher)

        viewModel.onAction(McpExtensionEditorUiAction.SetName("New MCP"))
        viewModel.onAction(McpExtensionEditorUiAction.SetUrl("https://example.test/"))
        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)
        dispatcher.runAll()
        viewModel.onAction(McpExtensionEditorUiAction.Save)

        val saved = repository.saved.single()
        assertEquals("", saved.id)
        assertTrue(saved.isEnabled)
        assertEquals(0L, saved.createdAt)
        assertEquals(0L, saved.updatedAt)
        assertEquals("https://example.test", saved.url)
    }

    @Test
    fun headersHaveStableKeysAndRemovingOneDoesNotMoveNeighborState() {
        val repository = FakeRepository(existingConfig())
        val viewModel = McpExtensionEditorViewModel(repository, QueuedDispatcher())
        val before = viewModel.state.value.headers
        val firstKey = before[0].key
        val secondKey = before[1].key

        viewModel.onAction(McpExtensionEditorUiAction.SetHeaderValue(secondKey, "changed"))
        viewModel.onAction(McpExtensionEditorUiAction.RemoveHeader(firstKey))

        val after = viewModel.state.value.headers
        assertEquals(1, after.size)
        assertEquals(secondKey, after.single().key)
        assertEquals("changed", after.single().value)
    }

    @Test
    fun queryUsesNormalizedUrlAndFilteredOrderedHeaderSnapshot() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(null)
        repository.queryResult = listOf(tool("one"))
        val viewModel = McpExtensionEditorViewModel(repository, dispatcher)

        viewModel.onAction(McpExtensionEditorUiAction.SetUrl(" https://example.test/// "))
        viewModel.onAction(McpExtensionEditorUiAction.AddHeader)
        val first = viewModel.state.value.headers.single().key
        viewModel.onAction(McpExtensionEditorUiAction.SetHeaderName(first, " X-First "))
        viewModel.onAction(McpExtensionEditorUiAction.SetHeaderValue(first, " value one "))
        viewModel.onAction(McpExtensionEditorUiAction.AddHeader)
        val blank = viewModel.state.value.headers.last().key
        viewModel.onAction(McpExtensionEditorUiAction.SetHeaderValue(blank, "ignored"))
        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)
        dispatcher.runAll()

        assertEquals("https://example.test", repository.lastQueryUrl)
        assertEquals(1, repository.lastQueryHeaders.size)
        assertEquals("X-First", repository.lastQueryHeaders[0].name)
        assertEquals("value one", repository.lastQueryHeaders[0].value)
    }

    @Test
    fun secondQueryDoesNotStartWhileFirstIsActive() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(null)
        repository.queryResult = listOf(tool("one"))
        val viewModel = McpExtensionEditorViewModel(repository, dispatcher)
        viewModel.onAction(McpExtensionEditorUiAction.SetUrl("https://one.test"))

        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)
        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)
        dispatcher.runAll()

        assertEquals(1, repository.queryCalls)
    }

    @Test
    fun lateResultForChangedUrlIsRejectedAndCannotSave() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(null)
        repository.queryResult = listOf(tool("old"))
        val viewModel = McpExtensionEditorViewModel(repository, dispatcher)
        viewModel.onAction(McpExtensionEditorUiAction.SetName("MCP"))
        viewModel.onAction(McpExtensionEditorUiAction.SetUrl("https://old.test"))
        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)

        viewModel.onAction(McpExtensionEditorUiAction.SetUrl("https://new.test"))
        dispatcher.runAll()
        val effect = viewModel.onAction(McpExtensionEditorUiAction.Save)

        assertFalse(viewModel.state.value.toolsMatchCurrentRequest)
        assertTrue(effect is McpExtensionEditorUiEffect.SaveRequiresCurrentTools)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun changedUrlBecomesSaveableAfterSuccessfulCurrentQuery() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(existingConfig())
        repository.queryResult = listOf(tool("new-tool"))
        val viewModel = McpExtensionEditorViewModel(repository, dispatcher)

        viewModel.onAction(McpExtensionEditorUiAction.SetUrl("https://new.test/"))
        assertFalse(viewModel.state.value.toolsMatchCurrentRequest)
        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)
        dispatcher.runAll()
        viewModel.onAction(McpExtensionEditorUiAction.Save)

        assertEquals("https://new.test", repository.saved.single().url)
        assertEquals("new-tool", repository.saved.single().tools.single().name)
    }

    @Test
    fun headerChangeInvalidatesConfirmationButNameChangeDoesNot() {
        val repository = FakeRepository(existingConfig())
        val viewModel = McpExtensionEditorViewModel(repository, QueuedDispatcher())
        assertTrue(viewModel.state.value.toolsMatchCurrentRequest)

        viewModel.onAction(McpExtensionEditorUiAction.SetName("Only name"))
        assertTrue(viewModel.state.value.toolsMatchCurrentRequest)

        val key = viewModel.state.value.headers.first().key
        viewModel.onAction(McpExtensionEditorUiAction.SetHeaderValue(key, "different"))
        assertFalse(viewModel.state.value.toolsMatchCurrentRequest)
    }

    @Test
    fun queryErrorKeepsDraftAndPreviousTools() {
        val dispatcher = QueuedDispatcher()
        val original = existingConfig()
        val repository = FakeRepository(original)
        repository.queryError = IllegalStateException("boom")
        val viewModel = McpExtensionEditorViewModel(repository, dispatcher)
        viewModel.onAction(McpExtensionEditorUiAction.SetName("Draft name"))
        viewModel.onAction(McpExtensionEditorUiAction.SetUrl("https://changed.test"))
        viewModel.onAction(McpExtensionEditorUiAction.QueryTools)
        dispatcher.runAll()

        assertEquals("Draft name", viewModel.state.value.name)
        assertEquals(original.tools.map { it.name }, viewModel.state.value.tools.map { it.name })
        assertEquals(McpQueryStatus.ERROR, viewModel.state.value.queryStatus)
        assertFalse(viewModel.state.value.toolsMatchCurrentRequest)
    }

    @Test
    fun togglingToolPreservesDescriptionAndSchemaAndAllCanBeDisabled() {
        val original = existingConfig()
        val repository = FakeRepository(original)
        val viewModel = McpExtensionEditorViewModel(repository, QueuedDispatcher())
        val before = viewModel.state.value.tools.first()

        viewModel.onAction(McpExtensionEditorUiAction.SetToolEnabled(0, false))
        viewModel.onAction(McpExtensionEditorUiAction.SetToolEnabled(1, false))
        val after = viewModel.state.value.tools.first()

        assertFalse(after.isEnabled)
        assertEquals(before.name, after.name)
        assertEquals(before.description, after.description)
        assertEquals(before.inputSchemaJson, after.inputSchemaJson)
        assertEquals(2, viewModel.state.value.tools.size)
        assertEquals(0, viewModel.state.value.enabledToolCount)
        viewModel.onAction(McpExtensionEditorUiAction.Save)
        assertEquals(2, repository.saved.single().tools.size)
    }

    @Test
    fun invalidSaveAndBackNeverCallRepositoryAndDoubleSaveIsBlocked() {
        val repository = FakeRepository(existingConfig())
        val viewModel = McpExtensionEditorViewModel(repository, QueuedDispatcher())

        viewModel.onAction(McpExtensionEditorUiAction.SetName(""))
        val invalid = viewModel.onAction(McpExtensionEditorUiAction.Save)
        assertTrue(invalid is McpExtensionEditorUiEffect.SaveRequiresNameAndUrl)
        assertTrue(repository.saved.isEmpty())

        viewModel.onAction(McpExtensionEditorUiAction.SetName("Valid"))
        val back = viewModel.onAction(McpExtensionEditorUiAction.Back)
        assertTrue(back is McpExtensionEditorUiEffect.Back)
        assertTrue(repository.saved.isEmpty())

        viewModel.onAction(McpExtensionEditorUiAction.Save)
        viewModel.onAction(McpExtensionEditorUiAction.Save)
        assertEquals(1, repository.saved.size)
    }

    @Test
    fun changedHeaderRequiresFreshQueryBeforeSave() {
        val repository = FakeRepository(existingConfig())
        val viewModel = McpExtensionEditorViewModel(repository, QueuedDispatcher())
        val key = viewModel.state.value.headers.first().key
        viewModel.onAction(McpExtensionEditorUiAction.SetHeaderName(key, "X-New"))

        val effect = viewModel.onAction(McpExtensionEditorUiAction.Save)

        assertTrue(effect is McpExtensionEditorUiEffect.SaveRequiresCurrentTools)
        assertTrue(repository.saved.isEmpty())
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
        private val editing: ExtensionMcpConfig?
    ) : McpExtensionEditorRepository {
        var queryResult: List<McpToolSummary> = emptyList()
        var queryError: Exception? = null
        var queryCalls: Int = 0
        var lastQueryUrl: String = ""
        var lastQueryHeaders: List<McpRequestHeader> = emptyList()
        val saved = mutableListOf<ExtensionMcpConfig>()

        override fun loadEditingMcp(): ExtensionMcpConfig? = editing

        override fun queryTools(url: String, headers: List<McpRequestHeader>): List<McpToolSummary> {
            queryCalls++
            lastQueryUrl = url
            lastQueryHeaders = headers.toList()
            queryError?.let { throw it }
            return queryResult
        }

        override fun saveMcpExtension(config: ExtensionMcpConfig) {
            saved += config
        }
    }

    private fun existingConfig(): ExtensionMcpConfig = ExtensionMcpConfig(
        "mcp-1",
        false,
        "Existing",
        "https://old.test",
        listOf(
            McpRequestHeader("X-First", "one"),
            McpRequestHeader("X-Second", "two")
        ),
        listOf(
            McpToolSummary("alpha", true, "Alpha description", "{\"type\":\"object\"}"),
            McpToolSummary("beta", true, "Beta description", "{\"type\":\"string\"}")
        ),
        111L,
        222L
    )

    private fun tool(name: String): McpToolSummary =
        McpToolSummary(name, true, "$name description", "{\"type\":\"object\"}")
}
