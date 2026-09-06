package cn.lineai.ui.component

import cn.lineai.model.McpSettingsState
import cn.lineai.model.McpToolConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class McpSettingsControllerRepositoryTest {

    @Test
    fun snapshotReturnsFreshGatewayStateWithoutRewritingOrder() {
        val first = McpSettingsState(
            "local",
            listOf(group("shell", true), group("todo", false))
        )
        val gateway = RecordingGateway(first)
        val repository = McpSettingsControllerRepository(gateway)

        val firstRead = repository.snapshot()
        gateway.stateValue = McpSettingsState(
            "ssh",
            listOf(group("shell", false), group("todo", true))
        )
        val secondRead = repository.snapshot()

        assertEquals(listOf("shell", "todo"), firstRead.configs.map { it.id })
        assertEquals("ssh", secondRead.executionMode)
        assertEquals(listOf("shell", "todo"), secondRead.configs.map { it.id })
        assertEquals(2, gateway.snapshotReads)
        assertEquals(0, gateway.modeWrites)
        assertEquals(0, gateway.groupWrites)
        assertSame(gateway.stateValue, secondRead)
    }

    @Test
    fun mutationsDelegateExactArgumentsOnce() {
        val gateway = RecordingGateway(McpSettingsState("local", emptyList()))
        val repository = McpSettingsControllerRepository(gateway)

        repository.setExecutionMode("terminal_provider")
        repository.setToolGroupEnabled("web_search", false)

        assertEquals(1, gateway.modeWrites)
        assertEquals(1, gateway.groupWrites)
        assertEquals("terminal_provider", gateway.lastRequestedMode)
        assertEquals("web_search", gateway.lastRequestedGroupId)
        assertEquals(false, gateway.lastRequestedEnabled)
    }

    @Test
    fun adapterDoesNotInventGroups() {
        val original = listOf(group("agent", true))
        val gateway = RecordingGateway(McpSettingsState("root", original))

        val snapshot = McpSettingsControllerRepository(gateway).snapshot()

        assertEquals(1, snapshot.configs.size)
        assertEquals("agent", snapshot.configs[0].id)
        assertTrue(snapshot.configs[0].isEnabled)
    }

    private class RecordingGateway(
        var stateValue: McpSettingsState
    ) : McpSettingsLegacyGateway {
        var snapshotReads = 0
        var modeWrites = 0
        var groupWrites = 0
        var lastRequestedMode: String? = null
        var lastRequestedGroupId: String? = null
        var lastRequestedEnabled: Boolean? = null

        override fun mcpSettingsState(): McpSettingsState {
            snapshotReads++
            return stateValue
        }

        override fun setExecutionMode(mode: String) {
            modeWrites++
            lastRequestedMode = mode
        }

        override fun setToolGroupEnabled(id: String, enabled: Boolean) {
            groupWrites++
            lastRequestedGroupId = id
            lastRequestedEnabled = enabled
        }
    }

    companion object {
        private fun group(id: String, enabled: Boolean): McpToolConfig =
            McpToolConfig(id, id, "$id desc", enabled, arrayOf(id), null, id)
    }
}
