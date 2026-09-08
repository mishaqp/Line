package cn.lineai.ui.component

import cn.lineai.ipc.IpcProviderConfig
import cn.lineai.ipc.IpcProviderType
import cn.lineai.ipc.ScannedProvider
import cn.lineai.ui.model.TerminalProviderInstalledUiItem
import cn.lineai.ui.model.TerminalProviderScanUiItem
import cn.lineai.ui.model.TerminalProvidersSettingsRepository
import cn.lineai.ui.model.TerminalProvidersSnapshot

interface TerminalProvidersLegacyGateway {
    fun installedProviders(): List<IpcProviderConfig>
    fun scanResults(): List<ScannedProvider>
    fun hasScanned(): Boolean
    fun scanProviders()
    fun saveProvider(config: IpcProviderConfig)
    fun setProviderEnabled(providerId: String, enabled: Boolean)
    fun deleteProvider(providerId: String)
}

class TerminalProvidersControllerRepository(
    private val gateway: TerminalProvidersLegacyGateway
) : TerminalProvidersSettingsRepository {

    override fun snapshot(): TerminalProvidersSnapshot = TerminalProvidersSnapshot(
        installed = gateway.installedProviders()
            .filter { it.providerType == IpcProviderType.TERMINAL.id }
            .map { config ->
                TerminalProviderInstalledUiItem(
                    id = config.id,
                    name = config.name,
                    packageName = config.packageName,
                    enabled = config.isEnabled
                )
            },
        scanResults = gateway.scanResults().map { provider ->
            TerminalProviderScanUiItem(
                providerType = provider.providerType,
                label = provider.label,
                packageName = provider.packageName,
                serviceClass = provider.serviceClass
            )
        },
        hasScanned = gateway.hasScanned()
    )

    override fun scan(): TerminalProvidersSnapshot {
        gateway.scanProviders()
        return snapshot()
    }

    override fun addProvider(
        providerType: String,
        name: String,
        packageName: String,
        serviceClass: String,
        enabled: Boolean
    ) {
        gateway.saveProvider(
            IpcProviderConfig.builder()
                .providerType(IpcProviderType.TERMINAL.id)
                .name(name)
                .packageName(packageName)
                .serviceClass(serviceClass)
                .enabled(enabled)
                .build()
        )
    }

    override fun setProviderEnabled(providerId: String, enabled: Boolean) {
        gateway.setProviderEnabled(providerId, enabled)
    }

    override fun deleteProvider(providerId: String) {
        gateway.deleteProvider(providerId)
    }
}
