package cn.lineai.ui.model

import cn.lineai.model.SkillRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillsExtensionsViewModelTest {

    @Test
    fun loadsSnapshotPreservesOrderAndExactSubtitle() {
        val viewModel = SkillsExtensionsViewModel(
            RecordingRepository(
                snapshot(
                    item("one", "First", "Project", "/one/SKILL.md", true),
                    item("two", "Second", "Global", "/two/SKILL.md", false)
                )
            )
        )
        val state = viewModel.state.value
        assertEquals(listOf("one", "two"), state.items.map { it.id })
        assertEquals("Project \u00b7 /one/SKILL.md", state.items.first().subtitle)
        assertEquals("Global \u00b7 /two/SKILL.md", state.items.last().subtitle)
        assertEquals("/sdcard/Download/skill.zip", state.suggestedPath)
        assertEquals(SkillRecord.LOCATION_PROJECT, state.installLocation)
        assertFalse(state.multiSelect)
    }

    @Test
    fun backIsOneShotEffect() {
        val viewModel = SkillsExtensionsViewModel(RecordingRepository(snapshot()))
        assertEquals(SkillsExtensionsUiEffect.Back, viewModel.onAction(SkillsExtensionsUiAction.Back))
        assertEquals(SkillsExtensionsUiEffect.Back, viewModel.onAction(SkillsExtensionsUiAction.Back))
    }

    @Test
    fun reloadUpdatesListWithoutMutations() {
        val repository = RecordingRepository(snapshot(item("one")))
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.OpenActions)
        repository.snapshotValue = snapshot(item("two"))

        viewModel.onAction(SkillsExtensionsUiAction.Reload)

        assertEquals(listOf("two"), viewModel.state.value.items.map { it.id })
        assertEquals(0, repository.setEnabledCalls)
        assertEquals(0, repository.deleteCalls)
        assertEquals(0, repository.createCalls)
        assertEquals(0, repository.pathCalls)
        assertEquals(0, repository.uriCalls)
        assertEquals(0, repository.githubCalls)
    }

    @Test
    fun bothAddPointsOpenSameActionsSheet() {
        val viewModel = SkillsExtensionsViewModel(RecordingRepository(snapshot(item("one"))))
        viewModel.onAction(SkillsExtensionsUiAction.OpenActions)
        val first = viewModel.state.value.sheet
        viewModel.onAction(SkillsExtensionsUiAction.Dismiss)
        viewModel.onAction(SkillsExtensionsUiAction.OpenActions)
        assertTrue(first is SkillsExtensionsSheet.Actions)
        assertTrue(viewModel.state.value.sheet is SkillsExtensionsSheet.Actions)
        assertTrue(viewModel.state.value.canEnterMultiSelect)
    }

    @Test
    fun multiSelectActionHiddenWhenListEmpty() {
        val viewModel = SkillsExtensionsViewModel(RecordingRepository(snapshot()))
        viewModel.onAction(SkillsExtensionsUiAction.OpenActions)
        assertFalse(viewModel.state.value.canEnterMultiSelect)
    }

    @Test
    fun pickFileEmitsSinglePickerEffect() {
        val viewModel = SkillsExtensionsViewModel(RecordingRepository(snapshot()))
        assertEquals(
            SkillsExtensionsUiEffect.OpenDocumentPicker,
            viewModel.onAction(SkillsExtensionsUiAction.PickFile)
        )
        assertNull(viewModel.state.value.sheet)
        val afterRead = viewModel.state.value
        assertSame(afterRead, viewModel.state.value)
    }

    @Test
    fun cancelledPickerDoesNotInstall() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.PickFile)
        viewModel.onAction(SkillsExtensionsUiAction.DocumentPickCancelled)
        assertEquals(0, repository.uriCalls)
        assertNull(viewModel.state.value.pendingDocument)
    }

    @Test
    fun acceptedExtensionsAreCaseInsensitive() {
        assertTrue(SkillsExtensionsViewModel.isAcceptedSkillName("Skill.ZIP"))
        assertTrue(SkillsExtensionsViewModel.isAcceptedSkillName("notes.MD"))
        assertFalse(SkillsExtensionsViewModel.isAcceptedSkillName("notes.txt"))
    }

    @Test
    fun invalidFileEmitsErrorAndDoesNotInstall() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        assertEquals(
            SkillsExtensionsUiEffect.InvalidFile,
            viewModel.onAction(SkillsExtensionsUiAction.DocumentPicked("content://x", "notes.txt"))
        )
        assertEquals(0, repository.uriCalls)
        assertNull(viewModel.state.value.pendingDocument)
    }

    @Test
    fun acceptedDocumentOpensTargetSheetWithProjectDefault() {
        val viewModel = SkillsExtensionsViewModel(RecordingRepository(snapshot()))
        assertNull(
            viewModel.onAction(SkillsExtensionsUiAction.DocumentPicked("content://x", "skill.Zip"))
        )
        assertTrue(viewModel.state.value.sheet is SkillsExtensionsSheet.FileTarget)
        assertEquals(SkillRecord.LOCATION_PROJECT, viewModel.state.value.installLocation)
        assertEquals("content://x", viewModel.state.value.pendingDocument?.uri)
    }

    @Test
    fun uriTargetPassesExactValuesOnce() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.DocumentPicked("content://doc/1", "pack.md"))
        viewModel.onAction(SkillsExtensionsUiAction.ChooseFileTarget(SkillRecord.LOCATION_APP))
        viewModel.onAction(SkillsExtensionsUiAction.ChooseFileTarget(SkillRecord.LOCATION_APP))
        assertEquals(1, repository.uriCalls)
        assertEquals(SkillRecord.LOCATION_APP, repository.lastUriLocation)
        assertEquals("content://doc/1", repository.lastUri)
        assertEquals("pack.md", repository.lastDisplayName)
        assertNull(viewModel.state.value.sheet)
    }

    @Test
    fun githubBlankValidationDoesNotInstall() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.OpenGitHubInstall)
        viewModel.onAction(SkillsExtensionsUiAction.SetGitHubUrl("   "))
        assertEquals(
            SkillsExtensionsUiEffect.InvalidGitHubUrl,
            viewModel.onAction(SkillsExtensionsUiAction.ConfirmGitHubInstall)
        )
        assertEquals(0, repository.githubCalls)
    }

    @Test
    fun githubInstallRunsOnceWithTrimmedUrl() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.OpenGitHubInstall)
        viewModel.onAction(SkillsExtensionsUiAction.SetGitHubUrl(" https://github.com/a/b "))
        viewModel.onAction(SkillsExtensionsUiAction.SetLocation(SkillRecord.LOCATION_APP))
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmGitHubInstall)
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmGitHubInstall)
        assertEquals(1, repository.githubCalls)
        assertEquals(SkillRecord.LOCATION_APP, repository.lastGithubLocation)
        assertEquals("https://github.com/a/b", repository.lastGithubUrl)
    }

    @Test
    fun createPassesTrimmedFieldsOnce() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.OpenCreateSkill)
        viewModel.onAction(SkillsExtensionsUiAction.SetCreateName("  demo  "))
        viewModel.onAction(SkillsExtensionsUiAction.SetCreateDescription("  desc  "))
        viewModel.onAction(SkillsExtensionsUiAction.SetCreateContent("  body  "))
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmCreateSkill)
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmCreateSkill)
        assertEquals(1, repository.createCalls)
        assertEquals(SkillRecord.LOCATION_PROJECT, repository.lastCreateLocation)
        assertEquals("demo", repository.lastCreateName)
        assertEquals("desc", repository.lastCreateDescription)
        assertEquals("body", repository.lastCreateContent)
    }

    @Test
    fun pathInstallPassesPathAndOptionalNameWithoutEmptyCheck() {
        val repository = RecordingRepository(snapshot())
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.OpenPathInstall)
        viewModel.onAction(SkillsExtensionsUiAction.SetPath(" /tmp/skill.zip "))
        viewModel.onAction(SkillsExtensionsUiAction.SetOptionalName("  custom  "))
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmPathInstall)
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmPathInstall)
        assertEquals(1, repository.pathCalls)
        assertEquals("/tmp/skill.zip", repository.lastPath)
        assertEquals("custom", repository.lastOptionalName)
        assertEquals(SkillRecord.LOCATION_PROJECT, repository.lastPathLocation)
    }

    @Test
    fun shareIsOneShotEffect() {
        val viewModel = SkillsExtensionsViewModel(RecordingRepository(snapshot()))
        assertEquals(
            SkillsExtensionsUiEffect.ShareWorkspace,
            viewModel.onAction(SkillsExtensionsUiAction.ShareWorkspace)
        )
        assertEquals(
            SkillsExtensionsUiEffect.ShareWorkspace,
            viewModel.onAction(SkillsExtensionsUiAction.ShareWorkspace)
        )
    }

    @Test
    fun toggleCallsRepositoryOnce() {
        val repository = RecordingRepository(snapshot(item("one", enabled = true)))
        repository.onSetEnabled = { id, enabled ->
            repository.snapshotValue = SkillsExtensionsSnapshot(
                repository.snapshotValue.items.map {
                    if (it.id == id) it.copy(enabled = enabled) else it
                },
                repository.snapshotValue.suggestedPath
            )
        }
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.SetEnabled("one", false))
        assertEquals(1, repository.setEnabledCalls)
        assertEquals("one" to false, repository.lastEnabledCall)
        assertFalse(viewModel.state.value.items.single().enabled)
    }

    @Test
    fun longPressSelectsOneItem() {
        val viewModel = SkillsExtensionsViewModel(
            RecordingRepository(snapshot(item("one"), item("two")))
        )
        viewModel.onAction(SkillsExtensionsUiAction.EnterMultiSelect("two"))
        assertEquals(setOf("two"), viewModel.state.value.selectedIds)
        assertTrue(viewModel.state.value.multiSelect)
    }

    @Test
    fun actionsSheetMultiSelectPicksFirstWhenNothingSelected() {
        val viewModel = SkillsExtensionsViewModel(
            RecordingRepository(snapshot(item("one"), item("two")))
        )
        viewModel.onAction(SkillsExtensionsUiAction.EnterMultiSelect())
        assertEquals(setOf("one"), viewModel.state.value.selectedIds)
    }

    @Test
    fun togglingLastSelectionReturnsToNormalMode() {
        val viewModel = SkillsExtensionsViewModel(
            RecordingRepository(snapshot(item("one"), item("two")))
        )
        viewModel.onAction(SkillsExtensionsUiAction.EnterMultiSelect("one"))
        viewModel.onAction(SkillsExtensionsUiAction.ToggleSelection("two"))
        assertEquals(setOf("one", "two"), viewModel.state.value.selectedIds)
        viewModel.onAction(SkillsExtensionsUiAction.ToggleSelection("two"))
        viewModel.onAction(SkillsExtensionsUiAction.ToggleSelection("one"))
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
        assertFalse(viewModel.state.value.multiSelect)
    }

    @Test
    fun cancelClearsSelectionWithoutDelete() {
        val repository = RecordingRepository(snapshot(item("one"), item("two")))
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.EnterMultiSelect("one"))
        viewModel.onAction(SkillsExtensionsUiAction.CancelSelection)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
        assertEquals(0, repository.deleteCalls)
    }

    @Test
    fun deleteRequiresConfirmationAndPassesIdsOnce() {
        val repository = RecordingRepository(snapshot(item("one"), item("two"), item("three")))
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmDeleteSelected)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(SkillsExtensionsUiAction.EnterMultiSelect("one"))
        viewModel.onAction(SkillsExtensionsUiAction.ToggleSelection("two"))
        viewModel.onAction(SkillsExtensionsUiAction.RequestDeleteSelected)
        assertTrue(viewModel.state.value.sheet is SkillsExtensionsSheet.DeleteConfirm)

        viewModel.onAction(SkillsExtensionsUiAction.Dismiss)
        assertEquals(0, repository.deleteCalls)
        assertEquals(setOf("one", "two"), viewModel.state.value.selectedIds)

        viewModel.onAction(SkillsExtensionsUiAction.RequestDeleteSelected)
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmDeleteSelected)
        viewModel.onAction(SkillsExtensionsUiAction.ConfirmDeleteSelected)

        assertEquals(1, repository.deleteCalls)
        assertEquals(listOf("one", "two"), repository.lastDeletedIds)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
        assertEquals(listOf("three"), viewModel.state.value.items.map { it.id })
    }

    @Test
    fun reloadDropsStaleSelectedIds() {
        val repository = RecordingRepository(snapshot(item("one"), item("two")))
        val viewModel = SkillsExtensionsViewModel(repository)
        viewModel.onAction(SkillsExtensionsUiAction.EnterMultiSelect("one"))
        viewModel.onAction(SkillsExtensionsUiAction.ToggleSelection("two"))
        repository.snapshotValue = snapshot(item("two"))
        viewModel.onAction(SkillsExtensionsUiAction.Reload)
        assertEquals(setOf("two"), viewModel.state.value.selectedIds)
    }

    private class RecordingRepository(
        var snapshotValue: SkillsExtensionsSnapshot
    ) : SkillsExtensionsRepository {
        var setEnabledCalls = 0
        var deleteCalls = 0
        var createCalls = 0
        var pathCalls = 0
        var uriCalls = 0
        var githubCalls = 0
        var lastEnabledCall: Pair<String, Boolean>? = null
        var lastDeletedIds: List<String>? = null
        var lastCreateLocation: String? = null
        var lastCreateName: String? = null
        var lastCreateDescription: String? = null
        var lastCreateContent: String? = null
        var lastPathLocation: String? = null
        var lastPath: String? = null
        var lastOptionalName: String? = null
        var lastUriLocation: String? = null
        var lastUri: String? = null
        var lastDisplayName: String? = null
        var lastGithubLocation: String? = null
        var lastGithubUrl: String? = null
        var onSetEnabled: ((String, Boolean) -> Unit)? = null

        override fun snapshot(): SkillsExtensionsSnapshot = snapshotValue

        override fun setEnabled(extensionId: String, enabled: Boolean) {
            setEnabledCalls++
            lastEnabledCall = extensionId to enabled
            onSetEnabled?.invoke(extensionId, enabled)
        }

        override fun deleteMany(extensionIds: List<String>) {
            deleteCalls++
            lastDeletedIds = extensionIds.toList()
            snapshotValue = SkillsExtensionsSnapshot(
                snapshotValue.items.filterNot { it.id in extensionIds },
                snapshotValue.suggestedPath
            )
        }

        override fun create(location: String, name: String, description: String, content: String) {
            createCalls++
            lastCreateLocation = location
            lastCreateName = name
            lastCreateDescription = description
            lastCreateContent = content
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
        private fun snapshot(vararg items: SkillsExtensionListItem) =
            SkillsExtensionsSnapshot(items.toList(), "/sdcard/Download/skill.zip")

        private fun item(
            id: String,
            name: String = "Skill",
            locationLabel: String = "App .linecode/skills",
            skillMdPath: String = "/$id/SKILL.md",
            enabled: Boolean = true
        ) = SkillsExtensionListItem(
            id = id,
            name = name,
            locationLabel = locationLabel,
            skillMdPath = skillMdPath,
            subtitle = locationLabel + " \u00b7 " + skillMdPath,
            enabled = enabled
        )
    }
}
