package cn.lineai.ui.component

import android.content.Context
import android.os.Environment
import android.view.View
import android.widget.Toast
import cn.lineai.R
import cn.lineai.model.SkillRecord
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView
import cn.lineai.workspace.WorkspaceShareHelper
import java.io.File

object SkillsExtensionsLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.Extension &&
            destination.kind == SkillsExtensionsControllerRepository.SKILLS_KIND

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : SkillsExtensionsLegacyGateway {
            override fun skills(): List<SkillRecord> =
                controller.extensionOverview.skills

            override fun suggestedPath(): String =
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "skill.zip"
                ).absolutePath

            override fun setExtensionEnabled(
                kind: String,
                extensionId: String,
                enabled: Boolean
            ) {
                controller.onExtensionEnabledChanged(kind, extensionId, enabled)
            }

            override fun deleteExtensions(kind: String, extensionIds: List<String>) {
                controller.onExtensionsDeleted(kind, extensionIds)
            }

            override fun createSkill(
                location: String,
                name: String,
                description: String,
                content: String
            ) {
                controller.onSkillCreated(location, name, description, content)
            }

            override fun installFromPath(
                location: String,
                sourcePath: String,
                optionalName: String
            ) {
                controller.onSkillInstalled(location, sourcePath, optionalName)
            }

            override fun installFromUri(location: String, uri: String, displayName: String) {
                controller.onSkillInstalledFromUri(location, uri, displayName)
            }

            override fun installFromGitHub(location: String, githubUrl: String) {
                controller.onSkillInstalledFromGitHub(location, githubUrl)
            }
        }

        return SkillsExtensionsHostView(
            context = context,
            repository = SkillsExtensionsControllerRepository(gateway),
            listener = object : SkillsExtensionsHostView.Listener {
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
                        arrayOf("skill.zip"),
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

                override fun shareWorkspace() {
                    WorkspaceShareHelper.shareHome(context)
                }

                override fun showInvalidFile() {
                    Toast.makeText(
                        context,
                        context.getString(R.string.screen_extension_detail_pick_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun showInvalidGitHubUrl() {
                    Toast.makeText(
                        context,
                        context.getString(R.string.skill_github_invalid_url),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
