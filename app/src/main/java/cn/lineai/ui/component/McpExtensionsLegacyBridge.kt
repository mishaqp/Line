package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.model.ExtensionMcpConfig
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView

object McpExtensionsLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.Extension &&
            destination.kind == McpExtensionsControllerRepository.MCP_KIND

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : McpExtensionsLegacyGateway {
            override fun mcpExtensions(): List<ExtensionMcpConfig> =
                controller.extensionOverview.mcps

            override fun setExtensionEnabled(
                kind: String,
                extensionId: String,
                enabled: Boolean
            ) {
                controller.onExtensionEnabledChanged(kind, extensionId, enabled)
            }

            override fun deleteExtension(kind: String, extensionId: String) {
                controller.onExtensionDeleted(kind, extensionId)
            }
        }

        return McpExtensionsHostView(
            context = context,
            repository = McpExtensionsControllerRepository(gateway),
            listener = object : McpExtensionsHostView.Listener {
                override fun onBack() {
                    view.handleScreenBack()
                }

                override fun onNavigate(destination: LineDestination) {
                    controller.onSettingsItemSelected(destination.screenId)
                }
            }
        )
    }
}
