package cn.lineai.ui.component

import cn.lineai.model.ExtensionAgentConfig
import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpToolConfig
import cn.lineai.tool.BaseTool
import cn.lineai.tool.builtin.FileReadTool
import cn.lineai.tool.builtin.GlobTool
import cn.lineai.ui.model.AgentExtensionDraft
import cn.lineai.ui.model.AgentExtensionEditorRepository
import cn.lineai.ui.model.AgentExtensionEditorSnapshot
import cn.lineai.ui.model.AgentExtensionIdentity
import cn.lineai.ui.model.AgentExtensionSaveRequest
import cn.lineai.ui.model.AgentMcpOption
import cn.lineai.ui.model.AgentToolOption
import java.util.Locale

interface AgentExtensionEditorLegacyGateway {
    fun editingAgent(): ExtensionAgentConfig?
    fun availableTools(): List<BaseTool>
    fun builtInMcps(): List<McpToolConfig>
    fun customMcps(): List<ExtensionMcpConfig>

    @Throws(Exception::class)
    fun generateAgentDraft(description: String): ExtensionAgentConfig?

    fun saveAgentExtension(config: ExtensionAgentConfig)
}

class AgentExtensionEditorControllerRepository(
    private val gateway: AgentExtensionEditorLegacyGateway
) : AgentExtensionEditorRepository {

    override fun loadSnapshot(): AgentExtensionEditorSnapshot {
        val editing = gateway.editingAgent()
        val tools = gateway.availableTools().map { tool ->
            AgentToolOption(
                name = tool.name,
                description = tool.category.name.lowercase(Locale.ROOT) + " · " + tool.description,
                selectedByDefault =
                    tool.name == FileReadTool.NAME || tool.name == GlobTool.NAME
            )
        }
        val builtInOptions = gateway.builtInMcps().map { config ->
            AgentMcpOption(
                id = "builtin:" + config.id,
                label = config.name,
                description = config.tools.joinToString(", ")
            )
        }
        val customOptions = gateway.customMcps()
            .filter { it.isEnabled }
            .map { config ->
                val enabledCount = config.tools.count { it.isEnabled }
                AgentMcpOption(
                    id = "custom:" + config.id,
                    label = config.name,
                    description = enabledCount.toString() + "/" + config.tools.size +
                        " tools · " + config.url
                )
            }

        return AgentExtensionEditorSnapshot(
            identity = editing?.let {
                AgentExtensionIdentity(
                    id = it.id,
                    enabled = it.isEnabled,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            initialDraft = editing?.toDraft(),
            tools = tools,
            mcps = builtInOptions + customOptions
        )
    }

    override fun generateDraft(description: String): AgentExtensionDraft? =
        gateway.generateAgentDraft(description)?.toDraft()

    override fun saveAgentExtension(request: AgentExtensionSaveRequest) {
        val identity = request.identity
        gateway.saveAgentExtension(
            ExtensionAgentConfig(
                identity?.id.orEmpty(),
                identity?.enabled ?: true,
                request.name,
                request.slug,
                request.prompt,
                request.trigger,
                request.toolNames,
                request.mcpIds,
                identity?.createdAt ?: 0L,
                identity?.updatedAt ?: 0L
            )
        )
    }

    private fun ExtensionAgentConfig.toDraft(): AgentExtensionDraft =
        AgentExtensionDraft(
            name = name,
            slug = slug,
            prompt = prompt,
            trigger = trigger,
            toolNames = toolNames.toList(),
            mcpIds = mcpIds.toList()
        )
}
