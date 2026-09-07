package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.model.ExtensionAgentConfig
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView

object AgentExtensionsLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.Extension &&
            destination.kind == AgentExtensionsControllerRepository.AGENT_KIND

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : AgentExtensionsLegacyGateway {
            override fun agentExtensions(): List<ExtensionAgentConfig> =
                controller.extensionOverview.agents

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

        return AgentExtensionsHostView(
            context = context,
            repository = AgentExtensionsControllerRepository(gateway),
            listener = object : AgentExtensionsHostView.Listener {
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
