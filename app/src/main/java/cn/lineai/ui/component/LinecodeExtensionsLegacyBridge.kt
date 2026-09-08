package cn.lineai.ui.component

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.Toast
import cn.lineai.R
import cn.lineai.model.LipPackageRecord
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView
import java.io.File

object LinecodeExtensionsLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.Extension &&
            destination.kind == LinecodeExtensionsControllerRepository.LINECODE_KIND

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : LinecodeExtensionsLegacyGateway {
            override fun lipPackages(): List<LipPackageRecord> =
                controller.extensionOverview.lipPackages

            override fun suggestedPath(): String =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "package.lip"
                ).absolutePath

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

            override fun installFromPath(location: String, sourcePath: String) {
                controller.onLipInstalled(location, sourcePath)
            }

            override fun installFromUri(location: String, uri: String, displayName: String) {
                controller.onLipInstalledFromUri(location, uri, displayName)
            }
        }

        return LinecodeExtensionsHostView(
            context = context,
            repository = LinecodeExtensionsControllerRepository(gateway),
            listener = object : LinecodeExtensionsHostView.Listener {
                override fun onBack() {
                    view.handleScreenBack()
                }

                override fun openDocumentPicker(
                    onPicked: (String, String) -> Unit,
                    onCancelled: () -> Unit
                ): Boolean {
                    val host = context as? MainChatView.WorkspaceHost ?: return false
                    host.openDocumentPicker(
                        "*/*",
                        arrayOf("package.lip"),
                        object : MainChatView.DocumentPickCallback {
                            override fun onDocumentPicked(uri: String, displayName: String) {
                                onPicked(uri, displayName)
                            }

                            override fun onDocumentPickCancelled() {
                                onCancelled()
                            }
                        }
                    )
                    return true
                }

                override fun showPathRequired() {
                    Toast.makeText(
                        context,
                        context.getString(R.string.lip_path_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun showInvalidFile() {
                    Toast.makeText(
                        context,
                        context.getString(R.string.lip_pick_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
