package cn.lineai.ui.component

import cn.lineai.model.SshConfig
import cn.lineai.ui.model.SshSettingsRepository

interface SshSettingsLegacyGateway {
    fun loadConfig(): SshConfig
    fun saveConfig(config: SshConfig)

    @Throws(Exception::class)
    fun testConnection(config: SshConfig): String
}

class SshSettingsControllerRepository(
    private val gateway: SshSettingsLegacyGateway
) : SshSettingsRepository {

    override fun load(): SshConfig = gateway.loadConfig() ?: SshConfig.defaultConfig()

    override fun save(config: SshConfig) {
        gateway.saveConfig(config)
    }

    override fun testConnection(config: SshConfig): String = gateway.testConnection(config)
}
