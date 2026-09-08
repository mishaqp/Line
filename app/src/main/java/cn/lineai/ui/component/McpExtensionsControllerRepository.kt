package cn.lineai.ui.component

import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.ui.model.McpExtensionListItem
import cn.lineai.ui.model.McpExtensionsRepository
import cn.lineai.ui.model.McpExtensionsSnapshot

interface McpExtensionsLegacyGateway {
    fun mcpExtensions(): List<ExtensionMcpConfig>
    fun setExtensionEnabled(kind: String, extensionId: String, enabled: Boolean)
    fun deleteExtension(kind: String, extensionId: String)
}

class McpExtensionsControllerRepository(
    private val gateway: McpExtensionsLegacyGateway
) : McpExtensionsRepository {

    override fun snapshot(): McpExtensionsSnapshot = McpExtensionsSnapshot(
        items = gateway.mcpExtensions().map { config ->
            McpExtensionListItem(
                id = config.id,
                name = config.name,
                url = config.url,
                toolCount = config.tools.size,
                enabled = config.isEnabled
            )
        }
    )

    override fun setEnabled(extensionId: String, enabled: Boolean) {
        gateway.setExtensionEnabled(MCP_KIND, extensionId, enabled)
    }

    override fun delete(extensionId: String) {
        gateway.deleteExtension(MCP_KIND, extensionId)
    }

    companion object {
        const val MCP_KIND: String = "mcp"
    }
}
