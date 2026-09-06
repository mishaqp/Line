package cn.lineai.ui.component

import cn.lineai.model.LipPackageRecord
import cn.lineai.ui.model.LinecodeExtensionsRepository
import cn.lineai.ui.model.LinecodeExtensionsSnapshot
import cn.lineai.ui.model.LinecodePackageListItem

interface LinecodeExtensionsLegacyGateway {
    fun lipPackages(): List<LipPackageRecord>
    fun suggestedPath(): String
    fun setExtensionEnabled(kind: String, extensionId: String, enabled: Boolean)
    fun deleteExtension(kind: String, extensionId: String)
    fun installFromPath(location: String, sourcePath: String)
    fun installFromUri(location: String, uri: String, displayName: String)
}

class LinecodeExtensionsControllerRepository(
    private val gateway: LinecodeExtensionsLegacyGateway
) : LinecodeExtensionsRepository {

    override fun snapshot(): LinecodeExtensionsSnapshot = LinecodeExtensionsSnapshot(
        items = gateway.lipPackages().map { record ->
            val version = if (record.version.isEmpty()) "1.0" else record.version
            LinecodePackageListItem(
                id = record.id,
                name = record.name,
                displayVersion = version,
                componentCount = record.componentCount(),
                subtitle = "v$version · ${record.componentCount()} · ${record.id}",
                enabled = record.isEnabled
            )
        },
        suggestedPath = gateway.suggestedPath()
    )

    override fun setEnabled(extensionId: String, enabled: Boolean) {
        gateway.setExtensionEnabled(LINECODE_KIND, extensionId, enabled)
    }

    override fun delete(extensionId: String) {
        gateway.deleteExtension(LINECODE_KIND, extensionId)
    }

    override fun installFromPath(location: String, sourcePath: String) {
        gateway.installFromPath(location, sourcePath)
    }

    override fun installFromUri(location: String, uri: String, displayName: String) {
        gateway.installFromUri(location, uri, displayName)
    }

    companion object {
        const val LINECODE_KIND: String = "linecode"
    }
}
