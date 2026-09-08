package cn.lineai.ui.component

import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.model.McpRequestHeader
import cn.lineai.model.McpToolSummary
import cn.lineai.ui.model.McpExtensionEditorRepository

interface McpExtensionEditorLegacyGateway {
    fun loadEditingMcp(): ExtensionMcpConfig?

    @Throws(Exception::class)
    fun queryTools(url: String, headers: List<McpRequestHeader>): List<McpToolSummary>

    fun saveMcpExtension(config: ExtensionMcpConfig)
}

class McpExtensionEditorControllerRepository(
    private val gateway: McpExtensionEditorLegacyGateway
) : McpExtensionEditorRepository {
    override fun loadEditingMcp(): ExtensionMcpConfig? = gateway.loadEditingMcp()

    override fun queryTools(url: String, headers: List<McpRequestHeader>): List<McpToolSummary> =
        gateway.queryTools(url, headers)

    override fun saveMcpExtension(config: ExtensionMcpConfig) {
        gateway.saveMcpExtension(config)
    }
}
