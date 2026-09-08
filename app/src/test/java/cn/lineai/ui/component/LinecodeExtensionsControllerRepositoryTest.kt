package cn.lineai.ui.component

import cn.lineai.model.LipPackageRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LinecodeExtensionsControllerRepositoryTest {

    @Test
    fun snapshotMapsLipPackageRecordPreservesOrderAndComponentCount() {
        val gateway = RecordingGateway(
            listOf(
                pack("one", "First", "", true, 1, 1, 0),
                pack("two", "Second", "3.0", false, 0, 0, 2)
            )
        )

        val snapshot = LinecodeExtensionsControllerRepository(gateway).snapshot()

        assertEquals(listOf("one", "two"), snapshot.items.map { it.id })
        assertEquals("First", snapshot.items.first().name)
        assertEquals("1.0", snapshot.items.first().displayVersion)
        assertEquals(2, snapshot.items.first().componentCount)
        assertEquals("v1.0 · 2 · one", snapshot.items.first().subtitle)
        assertTrue(snapshot.items.first().enabled)
        assertEquals("v3.0 · 2 · two", snapshot.items.last().subtitle)
        assertFalse(snapshot.items.last().enabled)
        assertEquals("/sdcard/Download/package.lip", snapshot.suggestedPath)
    }

    @Test
    fun mutationsUseExactLinecodeKindAndArgumentsOnce() {
        val gateway = RecordingGateway(emptyList())
        val repository = LinecodeExtensionsControllerRepository(gateway)

        repository.setEnabled("pkg-one", false)
        repository.delete("pkg-two")
        repository.installFromPath("project", "/tmp/a.lip")
        repository.installFromUri("app", "content://x", "b.zip")

        assertEquals(1, gateway.setEnabledCalls)
        assertEquals(1, gateway.deleteCalls)
        assertEquals(1, gateway.pathCalls)
        assertEquals(1, gateway.uriCalls)
        assertEquals("linecode", gateway.lastEnabledKind)
        assertEquals("pkg-one" to false, gateway.lastEnabledValue)
        assertEquals("linecode", gateway.lastDeletedKind)
        assertEquals("pkg-two", gateway.lastDeletedId)
        assertEquals("project", gateway.lastPathLocation)
        assertEquals("/tmp/a.lip", gateway.lastPath)
        assertEquals("app", gateway.lastUriLocation)
        assertEquals("content://x", gateway.lastUri)
        assertEquals("b.zip", gateway.lastDisplayName)
    }

    private class RecordingGateway(
        var packagesValue: List<LipPackageRecord>
    ) : LinecodeExtensionsLegacyGateway {
        var setEnabledCalls = 0
        var deleteCalls = 0
        var pathCalls = 0
        var uriCalls = 0
        var lastEnabledKind: String? = null
        var lastEnabledValue: Pair<String, Boolean>? = null
        var lastDeletedKind: String? = null
        var lastDeletedId: String? = null
        var lastPathLocation: String? = null
        var lastPath: String? = null
        var lastUriLocation: String? = null
        var lastUri: String? = null
        var lastDisplayName: String? = null

        override fun lipPackages(): List<LipPackageRecord> = packagesValue

        override fun suggestedPath(): String = "/sdcard/Download/package.lip"

        override fun setExtensionEnabled(
            kind: String,
            extensionId: String,
            enabled: Boolean
        ) {
            setEnabledCalls++
            lastEnabledKind = kind
            lastEnabledValue = extensionId to enabled
        }

        override fun deleteExtension(kind: String, extensionId: String) {
            deleteCalls++
            lastDeletedKind = kind
            lastDeletedId = extensionId
        }

        override fun installFromPath(location: String, sourcePath: String) {
            pathCalls++
            lastPathLocation = location
            lastPath = sourcePath
        }

        override fun installFromUri(location: String, uri: String, displayName: String) {
            uriCalls++
            lastUriLocation = location
            lastUri = uri
            lastDisplayName = displayName
        }
    }

    companion object {
        private fun pack(
            id: String,
            name: String,
            version: String,
            enabled: Boolean,
            skills: Int,
            agents: Int,
            mcps: Int
        ): LipPackageRecord = LipPackageRecord(
            id,
            name,
            version,
            "hidden-description",
            enabled,
            List(skills) { "skill-$it" },
            List(agents) { "agent-$it" },
            List(mcps) { "mcp-$it" },
            1L
        )
    }
}
