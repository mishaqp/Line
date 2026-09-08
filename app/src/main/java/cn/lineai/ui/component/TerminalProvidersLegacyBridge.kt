package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.ipc.IpcProviderConfig
import cn.lineai.ipc.ScannedProvider
import cn.lineai.mvp.MainUiController
import cn.lineai.ui.MainChatView

object TerminalProvidersLegacyBridge {
    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : TerminalProvidersLegacyGateway {
            override fun installedProviders(): List<IpcProviderConfig> =
                controller.extensionOverview.ipcProviders

            override fun scanResults(): List<ScannedProvider> =
                controller.terminalProviderScanResults

            override fun hasScanned(): Boolean =
                controller.hasTerminalProviderScanned()

            override fun scanProviders() {
                controller.onTerminalProviderScan()
            }

            override fun saveProvider(config: IpcProviderConfig) {
                controller.onTerminalProviderSaved(config)
            }

            override fun setProviderEnabled(providerId: String, enabled: Boolean) {
                controller.onTerminalProviderEnabledChanged(providerId, enabled)
            }

            override fun deleteProvider(providerId: String) {
                controller.onTerminalProviderDeleted(providerId)
            }
        }

        return TerminalProviderDetailScreenView(
            context,
            TerminalProvidersControllerRepository(gateway),
            Runnable { view.handleScreenBack() }
        )
    }
}
