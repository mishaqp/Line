package cn.lineai.ui.component

import cn.lineai.model.ExtensionAgentConfig
import cn.lineai.ui.model.AgentExtensionListItem
import cn.lineai.ui.model.AgentExtensionsRepository
import cn.lineai.ui.model.AgentExtensionsSnapshot

interface AgentExtensionsLegacyGateway {
    fun agentExtensions(): List<ExtensionAgentConfig>
    fun setExtensionEnabled(kind: String, extensionId: String, enabled: Boolean)
    fun deleteExtension(kind: String, extensionId: String)
}

class AgentExtensionsControllerRepository(
    private val gateway: AgentExtensionsLegacyGateway
) : AgentExtensionsRepository {

    override fun snapshot(): AgentExtensionsSnapshot = AgentExtensionsSnapshot(
        items = gateway.agentExtensions().map { config ->
            AgentExtensionListItem(
                id = config.id,
                name = config.name,
                slug = config.slug,
                toolCount = config.toolNames.size,
                enabled = config.isEnabled
            )
        }
    )

    override fun setEnabled(extensionId: String, enabled: Boolean) {
        gateway.setExtensionEnabled(AGENT_KIND, extensionId, enabled)
    }

    override fun delete(extensionId: String) {
        gateway.deleteExtension(AGENT_KIND, extensionId)
    }

    companion object {
        const val AGENT_KIND: String = "agent"
    }
}
