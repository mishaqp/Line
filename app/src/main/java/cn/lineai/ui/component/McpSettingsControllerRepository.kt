package cn.lineai.ui.component

import cn.lineai.model.McpSettingsState
import cn.lineai.ui.model.McpSettingsRepository

interface McpSettingsLegacyGateway {
    fun mcpSettingsState(): McpSettingsState
    fun setExecutionMode(mode: String)
    fun setToolGroupEnabled(id: String, enabled: Boolean)
}

class McpSettingsControllerRepository(
    private val gateway: McpSettingsLegacyGateway
) : McpSettingsRepository {

    override fun snapshot(): McpSettingsState = gateway.mcpSettingsState()

    override fun setExecutionMode(mode: String) {
        gateway.setExecutionMode(mode)
    }

    override fun setToolGroupEnabled(id: String, enabled: Boolean) {
        gateway.setToolGroupEnabled(id, enabled)
    }
}
