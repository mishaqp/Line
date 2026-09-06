package cn.lineai.ui.model

import cn.lineai.model.MemoryOverviewState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySettingsViewModelTest {
    @Test
    fun initialLoadContainsAllFiveSections() {
        val viewModel = viewModel()
        val overview = viewModel.state.value.overview

        assertEquals("/workspace/project", overview.projectId)
        assertEquals(listOf("u1"), overview.longTerm.map { it.id })
        assertEquals(listOf("p1"), overview.project.map { it.id })
        assertEquals(listOf("e1"), overview.environment.map { it.id })
        assertEquals(listOf("w1"), overview.shortTerm.map { it.id })
        assertEquals(listOf("h1"), overview.history.map { it.id })
    }

    @Test
    fun newMemoryUsesEmptyIdAndUpdatesStateImmediately() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenAddEditor)
        viewModel.onAction(MemoryUiAction.SetDraftContent("new memory"))
        viewModel.onAction(MemoryUiAction.SaveEditor)

        assertEquals(SaveCall("", MemoryOverviewState.Memory.SCOPE_USER, "new memory"), repository.saves.single())
        assertTrue(viewModel.state.value.overview.longTerm.any { it.content == "new memory" })
        assertEquals(null, viewModel.state.value.dialog)
    }

    @Test
    fun editingPreservesOriginalId() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        openEditor(viewModel, "u1")
        viewModel.onAction(MemoryUiAction.SetDraftContent("edited user"))
        viewModel.onAction(MemoryUiAction.SaveEditor)

        assertEquals("u1", repository.saves.single().id)
        assertEquals("edited user", viewModel.state.value.overview.longTerm.single().content)
    }

    @Test
    fun editorCanChangeScope() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        openEditor(viewModel, "u1")
        viewModel.onAction(MemoryUiAction.SetDraftScope(MemoryOverviewState.Memory.SCOPE_PROJECT))
        viewModel.onAction(MemoryUiAction.SaveEditor)

        assertEquals(MemoryOverviewState.Memory.SCOPE_PROJECT, repository.saves.single().scope)
        assertTrue(viewModel.state.value.overview.project.any { it.id == "u1" })
        assertFalse(viewModel.state.value.overview.longTerm.any { it.id == "u1" })
    }

    @Test
    fun saveTrimsContent() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenAddEditor)
        viewModel.onAction(MemoryUiAction.SetDraftContent("  line one\nline two  "))
        viewModel.onAction(MemoryUiAction.SaveEditor)

        assertEquals("line one\nline two", repository.saves.single().content)
    }

    @Test
    fun blankContentIsRejectedWithoutRepositoryCall() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenAddEditor)
        viewModel.onAction(MemoryUiAction.SetDraftContent("  \n  "))
        val effect = viewModel.onAction(MemoryUiAction.SaveEditor)

        assertSame(MemoryUiEffect.EmptyContent, effect)
        assertTrue(repository.saves.isEmpty())
        assertTrue(viewModel.state.value.dialog is MemoryDialogState.Editor)
    }

    @Test
    fun editingOneMemoryDoesNotChangeOtherMemories() {
        val second = memory("u2", MemoryOverviewState.Memory.SCOPE_USER, "second")
        val repository = FakeMemoryRepository(
            overviewWith(longTerm = listOf(memory("u1", MemoryOverviewState.Memory.SCOPE_USER, "first"), second))
        )
        val viewModel = MemorySettingsViewModel(repository)

        openEditor(viewModel, "u1")
        viewModel.onAction(MemoryUiAction.SetDraftContent("first changed"))
        assertEquals("first", viewModel.state.value.overview.longTerm.first { it.id == "u1" }.content)
        assertEquals("second", viewModel.state.value.overview.longTerm.first { it.id == "u2" }.content)

        viewModel.onAction(MemoryUiAction.SaveEditor)

        assertEquals("first changed", viewModel.state.value.overview.longTerm.first { it.id == "u1" }.content)
        assertEquals("second", viewModel.state.value.overview.longTerm.first { it.id == "u2" }.content)
    }

    @Test
    fun singleDeleteRequiresConfirmActionAndReloadsState() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.DeleteActionMemory)
        assertTrue(repository.deletes.isEmpty())

        viewModel.onAction(MemoryUiAction.ConfirmDelete)

        assertEquals(listOf("u1"), repository.deletes)
        assertFalse(viewModel.state.value.overview.longTerm.any { it.id == "u1" })
    }

    @Test
    fun multiSelectStartsFromActionMemory() {
        val viewModel = viewModel()

        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.MultiSelectActionMemory)

        assertTrue(viewModel.state.value.isMultiSelect)
        assertEquals(setOf("u1"), viewModel.state.value.selectedIds)
        assertEquals(null, viewModel.state.value.dialog)
    }

    @Test
    fun multiSelectAddsAndRemovesIdsAcrossEditableSections() {
        val viewModel = viewModel()

        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.MultiSelectActionMemory)
        viewModel.onAction(MemoryUiAction.ToggleSelected("p1"))
        assertEquals(setOf("u1", "p1"), viewModel.state.value.selectedIds)

        viewModel.onAction(MemoryUiAction.ToggleSelected("p1"))
        assertEquals(setOf("u1"), viewModel.state.value.selectedIds)
        assertTrue(viewModel.state.value.isMultiSelect)
    }

    @Test
    fun removingLastSelectedIdExitsMultiSelect() {
        val viewModel = viewModel()

        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.MultiSelectActionMemory)
        viewModel.onAction(MemoryUiAction.ToggleSelected("u1"))

        assertFalse(viewModel.state.value.isMultiSelect)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
    }

    @Test
    fun exitMultiSelectClearsAllSelectedIds() {
        val viewModel = viewModel()

        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.MultiSelectActionMemory)
        viewModel.onAction(MemoryUiAction.ToggleSelected("e1"))
        viewModel.onAction(MemoryUiAction.ExitMultiSelect)

        assertFalse(viewModel.state.value.isMultiSelect)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
    }

    @Test
    fun batchDeleteUsesExactSelectedIdSnapshot() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.MultiSelectActionMemory)
        viewModel.onAction(MemoryUiAction.ToggleSelected("p1"))
        viewModel.onAction(MemoryUiAction.OpenBatchDeleteConfirm)

        val confirm = viewModel.state.value.dialog as MemoryDialogState.BatchDeleteConfirm
        assertEquals(setOf("u1", "p1"), confirm.ids)

        viewModel.onAction(MemoryUiAction.ConfirmBatchDelete)

        assertEquals(setOf("u1", "p1"), repository.batchDeletes.single().toSet())
        assertFalse(viewModel.state.value.isMultiSelect)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
        assertFalse(viewModel.state.value.overview.longTerm.any { it.id == "u1" })
        assertFalse(viewModel.state.value.overview.project.any { it.id == "p1" })
    }

    @Test
    fun cancelClosesDialogWithoutRepositoryMutation() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenAddEditor)
        viewModel.onAction(MemoryUiAction.SetDraftContent("unsaved"))
        viewModel.onAction(MemoryUiAction.DismissDialog)

        assertTrue(repository.saves.isEmpty())
        assertTrue(repository.deletes.isEmpty())
        assertTrue(repository.batchDeletes.isEmpty())
        assertEquals(null, viewModel.state.value.dialog)
        assertFalse(viewModel.state.value.overview.longTerm.any { it.content == "unsaved" })
    }

    @Test
    fun workingMemoryAndHistoryAreReadOnlyDetailDialogs() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)

        viewModel.onAction(MemoryUiAction.OpenWorkingDetail("w1"))
        assertTrue(viewModel.state.value.dialog is MemoryDialogState.WorkingDetail)
        viewModel.onAction(MemoryUiAction.DismissDialog)

        viewModel.onAction(MemoryUiAction.OpenHistoryDetail("h1"))
        assertTrue(viewModel.state.value.dialog is MemoryDialogState.HistoryDetail)

        assertTrue(repository.saves.isEmpty())
        assertTrue(repository.deletes.isEmpty())
        assertTrue(repository.batchDeletes.isEmpty())
        assertFalse(viewModel.state.value.isMultiSelect)
    }

    @Test
    fun stateFlowReflectsRepositoryImmediatelyAfterMutation() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)
        val stateFlow = viewModel.state

        viewModel.onAction(MemoryUiAction.OpenAddEditor)
        viewModel.onAction(MemoryUiAction.SetDraftScope(MemoryOverviewState.Memory.SCOPE_ENVIRONMENT))
        viewModel.onAction(MemoryUiAction.SetDraftContent("environment now"))
        viewModel.onAction(MemoryUiAction.SaveEditor)

        assertTrue(stateFlow.value.overview.environment.any { it.content == "environment now" })
        assertTrue(repository.loadCount >= 2)
    }

    @Test
    fun reloadReadsFreshOverviewAndDropsStaleSelection() {
        val repository = FakeMemoryRepository(initialOverview())
        val viewModel = MemorySettingsViewModel(repository)
        viewModel.onAction(MemoryUiAction.OpenActions("u1"))
        viewModel.onAction(MemoryUiAction.MultiSelectActionMemory)

        repository.overview = overviewWith(
            longTerm = listOf(memory("u-new", MemoryOverviewState.Memory.SCOPE_USER, "fresh")),
            project = emptyList(),
            environment = emptyList(),
            shortTerm = emptyList(),
            history = emptyList()
        )
        viewModel.onAction(MemoryUiAction.Reload)

        assertEquals(listOf("u-new"), viewModel.state.value.overview.longTerm.map { it.id })
        assertFalse(viewModel.state.value.isMultiSelect)
        assertTrue(viewModel.state.value.selectedIds.isEmpty())
    }

    @Test
    fun backDoesNotChangeState() {
        val viewModel = viewModel()
        val before = viewModel.state.value

        val effect = viewModel.onAction(MemoryUiAction.Back)

        assertSame(MemoryUiEffect.Back, effect)
        assertSame(before, viewModel.state.value)
    }

    @Test
    fun viewModelHasNoAndroidFrameworkDependency() {
        val constructorTypes = MemorySettingsViewModel::class.java.declaredConstructors
            .flatMap { constructor -> constructor.parameterTypes.toList() }
            .map { it.name }
        val fieldTypes = MemorySettingsViewModel::class.java.declaredFields.map { it.type.name }

        assertEquals(listOf(MemorySettingsRepository::class.java.name), constructorTypes)
        assertTrue((constructorTypes + fieldTypes).none { it.startsWith("android.") })
    }

    private fun viewModel(): MemorySettingsViewModel =
        MemorySettingsViewModel(FakeMemoryRepository(initialOverview()))

    private fun openEditor(viewModel: MemorySettingsViewModel, id: String) {
        viewModel.onAction(MemoryUiAction.OpenActions(id))
        viewModel.onAction(MemoryUiAction.EditActionMemory)
        assertTrue(viewModel.state.value.dialog is MemoryDialogState.Editor)
    }

    private data class SaveCall(val id: String, val scope: String, val content: String)

    private class FakeMemoryRepository(
        var overview: MemoryOverviewState
    ) : MemorySettingsRepository {
        val saves = mutableListOf<SaveCall>()
        val deletes = mutableListOf<String>()
        val batchDeletes = mutableListOf<List<String>>()
        var loadCount: Int = 0

        override fun getMemoryOverview(): MemoryOverviewState {
            loadCount++
            return overview
        }

        override fun onMemorySaved(id: String, scope: String, content: String) {
            saves += SaveCall(id, scope, content)
            val longTerm = overview.longTerm.toMutableList()
            val project = overview.project.toMutableList()
            val environment = overview.environment.toMutableList()
            var previous: MemoryOverviewState.Memory? = null

            listOf(longTerm, project, environment).forEach { list ->
                val index = list.indexOfFirst { it.id == id && id.isNotEmpty() }
                if (index >= 0) {
                    previous = list.removeAt(index)
                }
            }

            val memory = MemoryOverviewState.Memory(
                if (id.isEmpty()) "new-${saves.size}" else id,
                scope,
                if (scope == MemoryOverviewState.Memory.SCOPE_PROJECT) overview.projectId else previous?.projectId.orEmpty(),
                content,
                previous?.source ?: "manual",
                previous?.confidence ?: 1.0,
                previous?.createdAt ?: 100L,
                200L + saves.size,
                previous?.lastUsedAt ?: 0L,
                previous?.useCount ?: 0
            )
            when (scope) {
                MemoryOverviewState.Memory.SCOPE_PROJECT -> project += memory
                MemoryOverviewState.Memory.SCOPE_ENVIRONMENT -> environment += memory
                else -> longTerm += memory
            }
            overview = overviewWith(
                projectId = overview.projectId,
                longTerm = longTerm,
                project = project,
                environment = environment,
                shortTerm = overview.shortTerm,
                history = overview.history
            )
        }

        override fun onMemoryDeleted(id: String) {
            deletes += id
            overview = withoutIds(setOf(id))
        }

        override fun onMemoriesDeleted(ids: List<String>) {
            batchDeletes += ids.toList()
            overview = withoutIds(ids.toSet())
        }

        private fun withoutIds(ids: Set<String>): MemoryOverviewState = overviewWith(
            projectId = overview.projectId,
            longTerm = overview.longTerm.filterNot { it.id in ids },
            project = overview.project.filterNot { it.id in ids },
            environment = overview.environment.filterNot { it.id in ids },
            shortTerm = overview.shortTerm,
            history = overview.history
        )
    }

    companion object {
        private fun initialOverview(): MemoryOverviewState = overviewWith()

        private fun overviewWith(
            projectId: String = "/workspace/project",
            longTerm: List<MemoryOverviewState.Memory> = listOf(
                memory("u1", MemoryOverviewState.Memory.SCOPE_USER, "user memory")
            ),
            project: List<MemoryOverviewState.Memory> = listOf(
                memory("p1", MemoryOverviewState.Memory.SCOPE_PROJECT, "project memory")
            ),
            environment: List<MemoryOverviewState.Memory> = listOf(
                memory("e1", MemoryOverviewState.Memory.SCOPE_ENVIRONMENT, "environment memory")
            ),
            shortTerm: List<MemoryOverviewState.WorkingMemory> = listOf(
                MemoryOverviewState.WorkingMemory(
                    "w1",
                    "/workspace/project",
                    "working memory",
                    "runtime",
                    900L,
                    100L,
                    200L
                )
            ),
            history: List<MemoryOverviewState.HistoryEntry> = listOf(
                MemoryOverviewState.HistoryEntry(
                    "h1",
                    "/workspace/project",
                    "conversation-1",
                    "message-1",
                    "user",
                    "history text",
                    "History title",
                    100L,
                    200L
                )
            )
        ): MemoryOverviewState = MemoryOverviewState(
            projectId,
            longTerm,
            project,
            environment,
            shortTerm,
            history
        )

        private fun memory(
            id: String,
            scope: String,
            content: String
        ): MemoryOverviewState.Memory = MemoryOverviewState.Memory(
            id,
            scope,
            if (scope == MemoryOverviewState.Memory.SCOPE_USER) "" else "/workspace/project",
            content,
            "manual",
            0.75,
            100L,
            200L,
            150L,
            3
        )
    }
}
