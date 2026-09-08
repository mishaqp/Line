package cn.lineai.ui.component

import cn.lineai.model.SkillRecord
import cn.lineai.ui.model.SkillsExtensionListItem
import cn.lineai.ui.model.SkillsExtensionsRepository
import cn.lineai.ui.model.SkillsExtensionsSnapshot

interface SkillsExtensionsLegacyGateway {
    fun skills(): List<SkillRecord>
    fun suggestedPath(): String
    fun setExtensionEnabled(kind: String, extensionId: String, enabled: Boolean)
    fun deleteExtensions(kind: String, extensionIds: List<String>)
    fun createSkill(location: String, name: String, description: String, content: String)
    fun installFromPath(location: String, sourcePath: String, optionalName: String)
    fun installFromUri(location: String, uri: String, displayName: String)
    fun installFromGitHub(location: String, githubUrl: String)
}

class SkillsExtensionsControllerRepository(
    private val gateway: SkillsExtensionsLegacyGateway
) : SkillsExtensionsRepository {

    override fun snapshot(): SkillsExtensionsSnapshot = SkillsExtensionsSnapshot(
        items = gateway.skills().map { record ->
            SkillsExtensionListItem(
                id = record.id,
                name = record.name,
                locationLabel = record.locationLabel,
                skillMdPath = record.skillMdPath,
                subtitle = record.locationLabel + " \u00b7 " + record.skillMdPath,
                enabled = record.isEnabled
            )
        },
        suggestedPath = gateway.suggestedPath()
    )

    override fun setEnabled(extensionId: String, enabled: Boolean) {
        gateway.setExtensionEnabled(SKILLS_KIND, extensionId, enabled)
    }

    override fun deleteMany(extensionIds: List<String>) {
        gateway.deleteExtensions(SKILLS_KIND, extensionIds.toList())
    }

    override fun create(location: String, name: String, description: String, content: String) {
        gateway.createSkill(location, name, description, content)
    }

    override fun installFromPath(location: String, sourcePath: String, optionalName: String) {
        gateway.installFromPath(location, sourcePath, optionalName)
    }

    override fun installFromUri(location: String, uri: String, displayName: String) {
        gateway.installFromUri(location, uri, displayName)
    }

    override fun installFromGitHub(location: String, githubUrl: String) {
        gateway.installFromGitHub(location, githubUrl)
    }

    companion object {
        const val SKILLS_KIND: String = "skills"
    }
}
