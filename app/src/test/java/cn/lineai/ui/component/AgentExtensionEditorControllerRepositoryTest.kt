package cn.lineai.ui.component

import cn.lineai.model.ExtensionAgentConfig
import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpToolConfig
import cn.lineai.model.McpToolSummary
import cn.lineai.tool.BaseTool
import cn.lineai.ui.model.AgentExtensionIdentity
import cn.lineai.ui.model.AgentExtensionSaveRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExtensionEditorControllerRepositoryTest {

    @Test
    fun snapshotPreservesEditingIdentityAndBuildsOrderedMcpOptions() {
        val editing = agent(
            id = "agent-id",
            enabled = false,
            toolNames = listOf("hidden-tool"),
            mcpIds = listOf("custom:hidden")
        )
        val builtIn = McpToolConfig(
            "builtin-id",
            "Built in",
            "",
            true,
            arrayOf("read", "write")
        )
        val enabledCustom = ExtensionMcpConfig(
            "custom-id",
            true,
            "Custom",
            "https://custom.test",
            emptyList(),
            listOf(
                McpToolSummary("one", true, "", "{}"),
                McpToolSummary("two", false, "", "{}")
            ),
            1L,
            2L
        )
        val disabledCustom = ExtensionMcpConfig(
            "disabled-id",
            false,
            "Disabled",
            "https://disabled.test",
            emptyList(),
            emptyList(),
            1L,
            2L
        )
        val gateway = FakeGateway(
            editingAgentValue = editing,
            builtInMcpsValue = listOf(builtIn),
            customMcpsValue = listOf(enabledCustom, disabledCustom)
        )

        val snapshot = AgentExtensionEditorControllerRepository(gateway).loadSnapshot()

        assertEquals("agent-id", snapshot.identity?.id)
        assertFalse(snapshot.identity?.enabled ?: true)
        assertEquals(listOf("hidden-tool"), snapshot.initialDraft?.toolNames)
        assertEquals(listOf("builtin:builtin-id", "custom:custom-id"), snapshot.mcps.map { it.id })
        assertEquals("read, write", snapshot.mcps.first().description)
        assertEquals("1/2 tools · https://custom.test", snapshot.mcps.last().description)
    }

    @Test
    fun generationAndSaveCrossTheLegacyBoundaryExactlyOnce() {
        val generated = agent(id = "generated", name = "Generated")
        val gateway = FakeGateway(generatedDraftValue = generated)
        val repository = AgentExtensionEditorControllerRepository(gateway)

        val draft = repository.generateDraft("description")
        repository.saveAgentExtension(
            AgentExtensionSaveRequest(
                identity = AgentExtensionIdentity("original", false, 7L, 8L),
                name = "Saved",
                slug = "saved",
                prompt = "Prompt",
                trigger = "Trigger",
                toolNames = listOf("tool"),
                mcpIds = listOf("builtin:mcp")
            )
        )

        assertEquals(1, gateway.generateCallCount)
        assertEquals("description", gateway.lastGenerateDescription)
        assertEquals("Generated", draft?.name)
        assertEquals(1, gateway.saveCallCount)
        val saved = gateway.savedConfigValue!!
        assertEquals("original", saved.id)
        assertFalse(saved.isEnabled)
        assertEquals(7L, saved.createdAt)
        assertEquals(8L, saved.updatedAt)
        assertEquals(listOf("tool"), saved.toolNames)
        assertEquals(listOf("builtin:mcp"), saved.mcpIds)
    }

    @Test
    fun newSaveUsesLegacyDefaults() {
        val gateway = FakeGateway()
        val repository = AgentExtensionEditorControllerRepository(gateway)

        repository.saveAgentExtension(
            AgentExtensionSaveRequest(
                identity = null,
                name = "New",
                slug = "new",
                prompt = "Prompt",
                trigger = "",
                toolNames = emptyList(),
                mcpIds = emptyList()
            )
        )

        val saved = gateway.savedConfigValue!!
        assertEquals("", saved.id)
        assertTrue(saved.isEnabled)
        assertEquals(0L, saved.createdAt)
        assertEquals(0L, saved.updatedAt)
    }

    private class FakeGateway(
        private val editingAgentValue: ExtensionAgentConfig? = null,
        private val availableToolsValue: List<BaseTool> = emptyList(),
        private val builtInMcpsValue: List<McpToolConfig> = emptyList(),
        private val customMcpsValue: List<ExtensionMcpConfig> = emptyList(),
        var generatedDraftValue: ExtensionAgentConfig? = null
    ) : AgentExtensionEditorLegacyGateway {
        var generateCallCount: Int = 0
        var saveCallCount: Int = 0
        var lastGenerateDescription: String = ""
        var savedConfigValue: ExtensionAgentConfig? = null

        override fun editingAgent(): ExtensionAgentConfig? = editingAgentValue

        override fun availableTools(): List<BaseTool> = availableToolsValue

        override fun builtInMcps(): List<McpToolConfig> = builtInMcpsValue

        override fun customMcps(): List<ExtensionMcpConfig> = customMcpsValue

        override fun generateAgentDraft(description: String): ExtensionAgentConfig? {
            generateCallCount++
            lastGenerateDescription = description
            return generatedDraftValue
        }

        override fun saveAgentExtension(config: ExtensionAgentConfig) {
            saveCallCount++
            savedConfigValue = config
        }
    }

    companion object {
        private fun agent(
            id: String,
            enabled: Boolean = true,
            name: String = "Agent",
            toolNames: List<String> = emptyList(),
            mcpIds: List<String> = emptyList()
        ) = ExtensionAgentConfig(
            id,
            enabled,
            name,
            "agent",
            "Prompt",
            "Trigger",
            toolNames,
            mcpIds,
            3L,
            4L
        )
    }
}
