package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView

object ShellCommandLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.ShellCommand

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        return ShellCommandScreenView(
            context,
            ShellCommandSource { view.getShellCommandText() },
            object : ShellCommandScreenView.Listener {
                override fun onBack() {
                    view.handleScreenBack()
                }
            }
        )
    }
}
