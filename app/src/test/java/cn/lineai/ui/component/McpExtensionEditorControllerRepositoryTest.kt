package cn.lineai.ui.component

import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpRequestHeader
import cn.lineai.model.McpToolSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class McpExtensionEditorControllerRepositoryTest {

    @Test
    fun adapterDelegatesLoadQueryAndSaveExactlyOnce() {
        val editing = ExtensionMcpConfig(
            "id", true, "name", "https://example.test",
            emptyList(), emptyList(), 1L, 2L
        )
        val queried = listOf(McpToolSummary("tool", true, "desc", "{}"))
        val gateway = FakeGateway(editing, queried)
        val repository = McpExtensionEditorControllerRepository(gateway)
        val headers = listOf(McpRequestHeader("X-Test", "value"))

        assertSame(editing, repository.loadEditingMcp())
        assertEquals(queried, repository.queryTools("https://example.test", headers))
        repository.saveMcpExtension(editing)

        assertEquals(1, gateway.queryCallCount)
        assertEquals("https://example.test", gateway.queryUrl)
        assertEquals("X-Test", gateway.queryHeaders.single().name)
        assertSame(editing, gateway.savedConfig)
        assertEquals(1, gateway.saveCallCount)
    }

    private class FakeGateway(
        private val editingConfig: ExtensionMcpConfig?,
        private val toolsResult: List<McpToolSummary>
    ) : McpExtensionEditorLegacyGateway {
        var queryCallCount = 0
        var saveCallCount = 0
        var queryUrl = ""
        var queryHeaders: List<McpRequestHeader> = emptyList()
        var savedConfig: ExtensionMcpConfig? = null

        override fun loadEditingMcp(): ExtensionMcpConfig? = editingConfig

        override fun queryTools(url: String, headers: List<McpRequestHeader>): List<McpToolSummary> {
            queryCallCount++
            queryUrl = url
            queryHeaders = headers.toList()
            return toolsResult
        }

        override fun saveMcpExtension(config: ExtensionMcpConfig) {
            saveCallCount++
            savedConfig = config
        }
    }
}
