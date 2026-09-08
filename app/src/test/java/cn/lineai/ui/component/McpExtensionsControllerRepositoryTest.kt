package cn.lineai.ui.component

import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpToolSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class McpExtensionsControllerRepositoryTest {

    @Test
    fun snapshotMapsOnlyListSafeFieldsAndPreservesOrder() {
        val gateway = FakeGateway(
            listOf(
                mcp("one", "First", "https://one.test", true, 2),
                mcp("two", "Second", "https://two.test", false, 0)
            )
        )

        val snapshot = McpExtensionsControllerRepository(gateway).snapshot()

        assertEquals(listOf("one", "two"), snapshot.items.map { it.id })
        assertEquals("First", snapshot.items.first().name)
        assertEquals("https://one.test", snapshot.items.first().url)
        assertEquals(2, snapshot.items.first().toolCount)
        assertTrue(snapshot.items.first().enabled)
        assertFalse(snapshot.items.last().enabled)
    }

    @Test
    fun mutationsUseExactMcpKindAndIdsOnce() {
        val gateway = FakeGateway(emptyList())
        val repository = McpExtensionsControllerRepository(gateway)

        repository.setEnabled("mcp-one", false)
        repository.delete("mcp-two")

        assertEquals(1, gateway.setEnabledCalls)
        assertEquals(1, gateway.deleteCalls)
        assertEquals("mcp", gateway.lastEnabledKind)
        assertEquals("mcp-one" to false, gateway.lastEnabledValue)
        assertEquals("mcp", gateway.lastDeletedKind)
        assertEquals("mcp-two", gateway.lastDeletedId)
    }

    private class FakeGateway(
        var extensionsValue: List<ExtensionMcpConfig>
    ) : McpExtensionsLegacyGateway {
        var setEnabledCalls = 0
        var deleteCalls = 0
        var lastEnabledKind: String? = null
        var lastEnabledValue: Pair<String, Boolean>? = null
        var lastDeletedKind: String? = null
        var lastDeletedId: String? = null

        override fun mcpExtensions(): List<ExtensionMcpConfig> = extensionsValue

        override fun setExtensionEnabled(
            kind: String,
            extensionId: String,
            enabled: Boolean
        ) {
            setEnabledCalls++
            lastEnabledKind = kind
            lastEnabledValue = extensionId to enabled
        }

        override fun deleteExtension(kind: String, extensionId: String) {
            deleteCalls++
            lastDeletedKind = kind
            lastDeletedId = extensionId
        }
    }

    companion object {
        private fun mcp(
            id: String,
            name: String,
            url: String,
            enabled: Boolean,
            toolCount: Int
        ): ExtensionMcpConfig = ExtensionMcpConfig(
            id,
            enabled,
            name,
            url,
            emptyList(),
            List(toolCount) { index ->
                McpToolSummary("tool-" + index, true, "", "{}")
            },
            1L,
            2L
        )
    }
}
