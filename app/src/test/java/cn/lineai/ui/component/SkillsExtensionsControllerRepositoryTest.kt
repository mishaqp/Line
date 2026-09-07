package cn.lineai.ui.component

import cn.lineai.model.SkillRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsExtensionsControllerRepositoryTest {

    @Test
    fun snapshotMapsSkillRecordPreservesOrderAndExactSubtitle() {
        val gateway = RecordingGateway(
            listOf(
                skill("one", "First", "Current workspace .linecode/skills", "/one/SKILL.md", true),
                skill("two", "Second", "App .linecode/skills", "/two/SKILL.md", false)
            )
        )

        val snapshot = SkillsExtensionsControllerRepository(gateway).snapshot()

        assertEquals(listOf("one", "two"), snapshot.items.map { it.id })
        assertEquals("First", snapshot.items.first().name)
        assertTrue(snapshot.items.first().enabled)
        assertEquals("Current workspace .linecode/skills", snapshot.items.first().locationLabel)
        assertEquals("/one/SKILL.md", snapshot.items.first().skillMdPath)
        assertEquals(
            "Current workspace .linecode/skills \u00b7 /one/SKILL.md",
            snapshot.items.first().subtitle
        )
        assertFalse(snapshot.items.last().enabled)
        assertEquals("/sdcard/Download/skill.zip", snapshot.suggestedPath)
    }

    @Test
    fun mutationsUseExactSkillsKindAndArgumentsOnce() {
        val gateway = RecordingGateway(emptyList())
        val repository = SkillsExtensionsControllerRepository(gateway)

        repository.setEnabled("skill-one", false)
        repository.deleteMany(listOf("a", "b"))
        repository.create("project", "n", "d", "c")
        repository.installFromPath("app", "/tmp/skill.zip", "custom")
        repository.installFromUri("project", "content://x", "skill.md")
        repository.installFromGitHub("app", "https://github.com/a/b")

        assertEquals(1, gateway.setEnabledCalls)
        assertEquals(1, gateway.deleteCalls)
        assertEquals(1, gateway.createCalls)
        assertEquals(1, gateway.pathCalls)
        assertEquals(1, gateway.uriCalls)
        assertEquals(1, gateway.githubCalls)
        assertEquals("skills", gateway.lastEnabledKind)
        assertEquals("skill-one" to false, gateway.lastEnabledValue)
        assertEquals("skills", gateway.lastDeletedKind)
        assertEquals(listOf("a", "b"), gateway.lastDeletedIds)
        assertEquals("project", gateway.lastCreateLocation)
        assertEquals(listOf("n", "d", "c"), gateway.lastCreateFields)
        assertEquals("app", gateway.lastPathLocation)
        assertEquals("/tmp/skill.zip", gateway.lastPath)
        assertEquals("custom", gateway.lastOptionalName)
        assertEquals("project", gateway.lastUriLocation)
        assertEquals("content://x", gateway.lastUri)
        assertEquals("skill.md", gateway.lastDisplayName)
        assertEquals("app", gateway.lastGithubLocation)
        assertEquals("https://github.com/a/b", gateway.lastGithubUrl)
    }

    private class RecordingGateway(
        var skillsValue: List<SkillRecord>
    ) : SkillsExtensionsLegacyGateway {
        var setEnabledCalls = 0
        var deleteCalls = 0
        var createCalls = 0
        var pathCalls = 0
        var uriCalls = 0
        var githubCalls = 0
        var lastEnabledKind: String? = null
        var lastEnabledValue: Pair<String, Boolean>? = null
        var lastDeletedKind: String? = null
        var lastDeletedIds: List<String>? = null
        var lastCreateLocation: String? = null
        var lastCreateFields: List<String>? = null
        var lastPathLocation: String? = null
        var lastPath: String? = null
        var lastOptionalName: String? = null
        var lastUriLocation: String? = null
        var lastUri: String? = null
        var lastDisplayName: String? = null
        var lastGithubLocation: String? = null
        var lastGithubUrl: String? = null

        override fun skills(): List<SkillRecord> = skillsValue

        override fun suggestedPath(): String = "/sdcard/Download/skill.zip"

        override fun setExtensionEnabled(kind: String, extensionId: String, enabled: Boolean) {
            setEnabledCalls++
            lastEnabledKind = kind
            lastEnabledValue = extensionId to enabled
        }

        override fun deleteExtensions(kind: String, extensionIds: List<String>) {
            deleteCalls++
            lastDeletedKind = kind
            lastDeletedIds = extensionIds.toList()
        }

        override fun createSkill(
            location: String,
            name: String,
            description: String,
            content: String
        ) {
            createCalls++
            lastCreateLocation = location
            lastCreateFields = listOf(name, description, content)
        }

        override fun installFromPath(location: String, sourcePath: String, optionalName: String) {
            pathCalls++
            lastPathLocation = location
            lastPath = sourcePath
            lastOptionalName = optionalName
        }

        override fun installFromUri(location: String, uri: String, displayName: String) {
            uriCalls++
            lastUriLocation = location
            lastUri = uri
            lastDisplayName = displayName
        }

        override fun installFromGitHub(location: String, githubUrl: String) {
            githubCalls++
            lastGithubLocation = location
            lastGithubUrl = githubUrl
        }
    }

    companion object {
        private fun skill(
            id: String,
            name: String,
            locationLabel: String,
            path: String,
            enabled: Boolean
        ): SkillRecord {
            val location = if (locationLabel.startsWith("Current")) {
                SkillRecord.LOCATION_PROJECT
            } else {
                SkillRecord.LOCATION_APP
            }
            return SkillRecord(
                id,
                name,
                "hidden",
                "/root/" + id,
                path,
                location,
                enabled,
                1L,
                2L
            )
        }
    }
}
