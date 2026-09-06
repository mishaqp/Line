package cn.lineai.ui.component

import cn.lineai.model.WebSearchConfig
import cn.lineai.ui.model.ToolSettingsRepository
import cn.lineai.ui.model.ToolSettingsSnapshot

interface ToolSettingsLegacyGateway {
    fun imageUnderstandingLabel(): String
    fun imageGenerationLabel(): String
    fun webSearchConfig(): WebSearchConfig
    fun saveWebSearchConfig(config: WebSearchConfig)
}

class ToolSettingsControllerRepository(
    private val gateway: ToolSettingsLegacyGateway
) : ToolSettingsRepository {

    override fun snapshot(): ToolSettingsSnapshot {
        return ToolSettingsSnapshot(
            imageUnderstandingLabel = gateway.imageUnderstandingLabel(),
            imageGenerationLabel = gateway.imageGenerationLabel(),
            webSearch = gateway.webSearchConfig() ?: WebSearchConfig.defaultConfig()
        )
    }

    override fun saveWebSearchConfig(config: WebSearchConfig) {
        gateway.saveWebSearchConfig(config)
    }
}
