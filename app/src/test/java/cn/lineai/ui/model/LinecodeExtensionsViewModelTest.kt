package cn.lineai.ui.model

import cn.lineai.model.SkillRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LinecodeExtensionsViewModelTest {

    @Test
    fun loadsSnapshotPreservesOrderAndVersionFallback() {
        val repository = RecordingRepository(
            snapshotValue = snapshot(
                item("one", "First", "", 2, true),
                item("two", "Second", "2.1", 0, false)
            )
        )

        val state = LinecodeExtensionsViewModel(repository).state.value

        assertEquals(listOf("one", "two"), state.items.map { it.id })
        assertEquals("1.0", state.items.first().displayVersion)
        assertEquals("v1.0 · 2 · one", state.items.first().subtitle)
        assertEquals("v2.1 · 0 · two", state.items.last().subtitle)
        assertEquals("/Download/package.lip", state.suggestedPath)
        assertEquals(SkillRecord.LOCATION_PROJECT, state.installLocation)
    }

    @Test
    fun backIsOneShotEffect() {
        val viewModel = LinecodeExtensionsViewModel(RecordingRepository(snapshot()))
        assertEquals(
            LinecodeExtensionsUiEffect.Back,
            viewModel.onAction(LinecodeExtensionsUiAction.Back)
        )
        assertEquals(
            LinecodeExtensionsUiEffect.Back,
            viewModel.onAction(LinecodeExtensionsUiAction.Back)
        )
    }

    @Test
    fun reloadUpdatesCachedListWithoutMutation() {
        val repository = RecordingRepository(snapshot(item("one", enabled = true)))
        val viewModel = LinecodeExtensionsViewModel(repository)
        viewModel.onAction(LinecodeExtensionsUiAction.OpenInstallMethods)
        repository.snapshotValue = snapshot(item("two", enabled = false))

        viewModel.onAction(LinecodeExtensionsUiAction.Reload)

        assertEquals(listOf("two"), viewModel.state.value.items.map { it.id })
        assertNull(viewModel.state.value.sheet)
        assertEquals(0, repository.setEnabledCalls)
        assertEquals(0, repository.deleteCalls)
        assertEquals(0, repository.pathInstallCalls)
        assertEquals(0, repository.uriInstallCalls)
    }

    @Test
    fun bothAddActionsOpenSameInstallSheet() {
        val viewModel = LinecodeExtensionsViewModel(RecordingRepository(snapshot()))

        viewModel.onAction(LinecodeExtensionsUiAction.OpenInstallMethods)
        val first = viewModel.state.value.sheet
        viewModel.onAction(LinecodeExtensionsUiAction.DismissSheet)
        viewModel.onAction(LinecodeExtensionsUiAction.OpenInstallMethods)

        assertTrue(first is LinecodeExtensionsSheet.InstallMethods)
        assertTrue(viewModel.state.value.sheet is LinecodeExtensionsSheet.InstallMethods)
    }

    @Test
    fun pickFileEmitsSinglePickerEffectAndRecompositionDoesNotRepeatIt() {
        val viewModel = LinecodeExtensionsViewModel(RecordingRepository(snapshot()))

        assertEquals(
            LinecodeExtensionsUiEffect.OpenDocumentPicker,
            viewModel.onAction(LinecodeExtensionsUiAction.PickFile)
        )
        assertNull(viewModel.state.value.sheet)
        val afterRead = viewModel.state.value
        assertSame(afterRead, viewModel.state.value)
        assertNull(viewModel.onAction(LinecodeExtensionsUiAction.DocumentPickCancelled))
        assertEquals(0, RecordingRepository(snapshot()).pathInstallCalls)
    }

    @Test
    fun cancelledPickerDoesNotInstall() {
        val repository = RecordingRepository(snapshot())
        val viewModel = LinecodeExtensionsViewModel(repository)

        viewModel.onAction(LinecodeExtensionsUiAction.PickFile)
        viewModel.onAction(LinecodeExtensionsUiAction.DocumentPickCancelled)

        assertEquals(0, repository.uriInstallCalls)
        assertEquals(0, repository.pathInstallCalls)
        assertNull(viewModel.state.value.pendingDocument)
    }

    @Test
    fun acceptedExtensionsAreCaseInsensitive() {
        assertTrue(LinecodeExtensionsViewModel.isAcceptedPackageName("Package.LIP"))
        assertTrue(LinecodeExtensionsViewModel.isAcceptedPackageName("bundle.ZIP"))
        assertFalse(LinecodeExtensionsViewModel.isAcceptedPackageName("notes.txt"))
    }

    @Test
    fun invalidExtensionDoesNotInstall() {
        val repository = RecordingRepository(snapshot())
        val viewModel = LinecodeExtensionsViewModel(repository)

        assertEquals(
            LinecodeExtensionsUiEffect.InvalidFile,
            viewModel.onAction(
                LinecodeExtensionsUiAction.DocumentPicked("content://x", "notes.txt")
            )
        )
        assertEquals(0, repository.uriInstallCalls)
        assertNull(viewModel.state.value.pendingDocument)
    }

    @Test
    fun acceptedDocumentOpensTargetSheetWithProjectDefault() {
        val viewModel = LinecodeExtensionsViewModel(RecordingRepository(snapshot()))

        assertNull(
            viewModel.onAction(
                LinecodeExtensionsUiAction.DocumentPicked("content://x", "pkg.LiP")
            )
        )
        assertTrue(viewModel.state.value.sheet is LinecodeExtensionsSheet.FileTarget)
        assertEquals(SkillRecord.LOCATION_PROJECT, viewModel.state.value.installLocation)
        assertEquals("content://x", viewModel.state.value.pendingDocument?.uri)
    }

    @Test
    fun emptyPathEmitsValidationAndDoesNotInstall() {
        val repository = RecordingRepository(snapshot())
        val viewModel = LinecodeExtensionsViewModel(repository)
        viewModel.onAction(LinecodeExtensionsUiAction.OpenPathInstall)

        assertEquals(
            LinecodeExtensionsUiEffect.PathRequired,
            viewModel.onAction(LinecodeExtensionsUiAction.ConfirmPathInstall)
        )
        assertEquals(0, repository.pathInstallCalls)
    }

    @Test
    fun pathInstallRunsOnceWithExactLocation() {
        val repository = RecordingRepository(snapshot())
        val viewModel = LinecodeExtensionsViewModel(repository)
        viewModel.onAction(LinecodeExtensionsUiAction.OpenPathInstall)
        viewModel.onAction(LinecodeExtensionsUiAction.SetPath("/sdcard/package.lip"))
        viewModel.onAction(LinecodeExtensionsUiAction.SetInstallLocation(SkillRecord.LOCATION_APP))

        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmPathInstall)
        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmPathInstall)

        assertEquals(1, repository.pathInstallCalls)
        assertEquals(SkillRecord.LOCATION_APP, repository.lastPathLocation)
        assertEquals("/sdcard/package.lip", repository.lastPath)
        assertNull(viewModel.state.value.sheet)
    }

    @Test
    fun uriInstallPassesExactValuesOnce() {
        val repository = RecordingRepository(snapshot())
        val viewModel = LinecodeExtensionsViewModel(repository)
        viewModel.onAction(
            LinecodeExtensionsUiAction.DocumentPicked("content://doc/1", "pack.zip")
        )
        viewModel.onAction(LinecodeExtensionsUiAction.SetInstallLocation(SkillRecord.LOCATION_PROJECT))

        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmUriInstall)
        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmUriInstall)

        assertEquals(1, repository.uriInstallCalls)
        assertEquals(SkillRecord.LOCATION_PROJECT, repository.lastUriLocation)
        assertEquals("content://doc/1", repository.lastUri)
        assertEquals("pack.zip", repository.lastDisplayName)
    }

    @Test
    fun toggleCallsRepositoryOnce() {
        val repository = RecordingRepository(snapshot(item("one", enabled = true)))
        repository.onSetEnabled = { id, enabled ->
            repository.snapshotValue = snapshot(
                repository.snapshotValue.items.map {
                    if (it.id == id) it.copy(enabled = enabled) else it
                }
            )
        }
        val viewModel = LinecodeExtensionsViewModel(repository)

        viewModel.onAction(LinecodeExtensionsUiAction.SetEnabled("one", false))

        assertEquals(1, repository.setEnabledCalls)
        assertEquals("one" to false, repository.lastEnabledCall)
        assertFalse(viewModel.state.value.items.single().enabled)
    }

    @Test
    fun deleteRequiresConfirmationAndCancelDoesNothing() {
        val repository = RecordingRepository(snapshot(item("one", name = "First", enabled = true)))
        val viewModel = LinecodeExtensionsViewModel(repository)

        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmDelete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(LinecodeExtensionsUiAction.RequestDelete("one"))
        viewModel.onAction(LinecodeExtensionsUiAction.DismissSheet)
        assertEquals(0, repository.deleteCalls)
        assertNull(viewModel.state.value.sheet)

        viewModel.onAction(LinecodeExtensionsUiAction.RequestDelete("one"))
        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmDelete)
        viewModel.onAction(LinecodeExtensionsUiAction.ConfirmDelete)

        assertEquals(1, repository.deleteCalls)
        assertEquals("one", repository.lastDeletedId)
        assertTrue(viewModel.state.value.items.isEmpty())
    }

    private class RecordingRepository(
        var snapshotValue: LinecodeExtensionsSnapshot
    ) : LinecodeExtensionsRepository {
        var setEnabledCalls = 0
        var deleteCalls = 0
        var pathInstallCalls = 0
        var uriInstallCalls = 0
        var lastEnabledCall: Pair<String, Boolean>? = null
        var lastDeletedId: String? = null
        var lastPathLocation: String? = null
        var lastPath: String? = null
        var lastUriLocation: String? = null
        var lastUri: String? = null
        var lastDisplayName: String? = null
        var onSetEnabled: ((String, Boolean) -> Unit)? = null
        var onDelete: ((String) -> Unit)? = null

        override fun snapshot(): LinecodeExtensionsSnapshot = snapshotValue

        override fun setEnabled(extensionId: String, enabled: Boolean) {
            setEnabledCalls++
            lastEnabledCall = extensionId to enabled
            onSetEnabled?.invoke(extensionId, enabled)
        }

        override fun delete(extensionId: String) {
            deleteCalls++
            lastDeletedId = extensionId
            onDelete?.invoke(extensionId)
            snapshotValue = LinecodeExtensionsSnapshot(
                snapshotValue.items.filterNot { it.id == extensionId },
                snapshotValue.suggestedPath
            )
        }

        override fun installFromPath(location: String, sourcePath: String) {
            pathInstallCalls++
            lastPathLocation = location
            lastPath = sourcePath
        }

        override fun installFromUri(location: String, uri: String, displayName: String) {
            uriInstallCalls++
            lastUriLocation = location
            lastUri = uri
            lastDisplayName = displayName
        }
    }

    companion object {
        private fun snapshot(vararg items: LinecodePackageListItem) =
            LinecodeExtensionsSnapshot(items.toList(), "/Download/package.lip")

        private fun item(
            id: String,
            name: String = "Pack",
            version: String = "1.0",
            componentCount: Int = 1,
            enabled: Boolean
        ): LinecodePackageListItem {
            val display = if (version.isEmpty()) "1.0" else version
            return LinecodePackageListItem(
                id = id,
                name = name,
                displayVersion = display,
                componentCount = componentCount,
                subtitle = "v$display · $componentCount · $id",
                enabled = enabled
            )
        }
    }
}
