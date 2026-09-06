package cn.lineai.ui.component

import cn.lineai.model.ExtensionAgentConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExtensionsControllerRepositoryTest {

    @Test
    fun snapshotMapsOnlyListSafeFieldsAndPreservesOrder() {
        val gateway = FakeGateway(
            listOf(
                agent("one", "First", "first-agent", true, 2),
                agent("two", "Second", "second-agent", false, 0)
            )
        )

        val snapshot = AgentExtensionsControllerRepository(gateway).snapshot()

        assertEquals(listOf("one", "two"), snapshot.items.map { it.id })
        assertEquals("First", snapshot.items.first().name)
        assertEquals("first-agent", snapshot.items.first().slug)
        assertEquals(2, snapshot.items.first().toolCount)
        assertTrue(snapshot.items.first().enabled)
        assertFalse(snapshot.items.last().enabled)
    }

    @Test
    fun mutationsUseExactAgentKindAndIdsOnce() {
        val gateway = FakeGateway(emptyList())
        val repository = AgentExtensionsControllerRepository(gateway)

        repository.setEnabled("agent-one", false)
        repository.delete("agent-two")

        assertEquals(1, gateway.setEnabledCalls)
        assertEquals(1, gateway.deleteCalls)
        assertEquals("agent", gateway.lastEnabledKind)
        assertEquals("agent-one" to false, gateway.lastEnabledValue)
        assertEquals("agent", gateway.lastDeletedKind)
        assertEquals("agent-two", gateway.lastDeletedId)
    }

    private class FakeGateway(
        var extensionsValue: List<ExtensionAgentConfig>
    ) : AgentExtensionsLegacyGateway {
        var setEnabledCalls = 0
        var deleteCalls = 0
        var lastEnabledKind: String? = null
        var lastEnabledValue: Pair<String, Boolean>? = null
        var lastDeletedKind: String? = null
        var lastDeletedId: String? = null

        override fun agentExtensions(): List<ExtensionAgentConfig> = extensionsValue

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
        private fun agent(
            id: String,
            name: String,
            slug: String,
            enabled: Boolean,
            toolCount: Int
        ): ExtensionAgentConfig = ExtensionAgentConfig(
            id,
            enabled,
            name,
            slug,
            "prompt",
            "trigger",
            List(toolCount) { index -> "tool-" + index },
            emptyList(),
            1L,
            2L
        )
    }
}
