package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerViewModelTest {

    @Test
    fun initialSnapshotPreservesConversationAndFileOrder() {
        val viewModel = DrawerViewModel(
            FakeRepository(
                snapshot(
                    conversations = listOf(
                        DrawerConversationUi("two", "Two", "2/2 02:02"),
                        DrawerConversationUi("one", "One", "1/1 01:01")
                    ),
                    fileRows = listOf(file("/root"), file("/root/a.kt", depth = 1))
                )
            )
        )

        assertEquals(listOf("two", "one"), viewModel.state.value.conversations.map { it.id })
        assertEquals(listOf("/root", "/root/a.kt"), viewModel.state.value.fileRows?.map { it.path })
        assertEquals(DrawerTab.CONVERSATIONS, viewModel.state.value.activeTab)
        assertFalse(viewModel.state.value.isOpen)
    }

    @Test
    fun selectingFilesActivatesTreeOnlyOnRealTransition() {
        val viewModel = DrawerViewModel(FakeRepository(snapshot()))

        assertSame(
            DrawerUiEffect.FileTreeActivated,
            viewModel.onAction(DrawerUiAction.SelectTab(DrawerTab.FILES))
        )
        assertNull(viewModel.onAction(DrawerUiAction.SelectTab(DrawerTab.FILES)))
        assertNull(viewModel.onAction(DrawerUiAction.SelectTab(DrawerTab.CONVERSATIONS)))
        assertEquals(DrawerTab.CONVERSATIONS, viewModel.state.value.activeTab)
    }

    @Test
    fun reloadReadsFreshSnapshotWithoutChangingOpenStateOrTab() {
        val repository = FakeRepository(snapshot(projectLabel = "First"))
        val viewModel = DrawerViewModel(repository)
        viewModel.onAction(DrawerUiAction.Open)
        viewModel.onAction(DrawerUiAction.SelectTab(DrawerTab.FILES))
        repository.current = snapshot(
            projectLabel = "Second",
            projectPath = "/new",
            fileRows = listOf(file("/new"))
        )

        assertNull(viewModel.onAction(DrawerUiAction.Reload))

        assertTrue(viewModel.state.value.isOpen)
        assertEquals(DrawerTab.FILES, viewModel.state.value.activeTab)
        assertEquals("Second", viewModel.state.value.projectLabel)
        assertEquals("/new", viewModel.state.value.projectPath)
    }

    @Test
    fun newConversationAndSelectionCloseBeforeReturningOneShotEffect() {
        val viewModel = DrawerViewModel(
            FakeRepository(
                snapshot(
                    conversations = listOf(DrawerConversationUi("known", "Known", ""))
                )
            )
        )
        viewModel.onAction(DrawerUiAction.Open)

        assertSame(
            DrawerUiEffect.NewConversation,
            viewModel.onAction(DrawerUiAction.NewConversation)
        )
        assertFalse(viewModel.state.value.isOpen)

        viewModel.onAction(DrawerUiAction.Open)
        assertEquals(
            DrawerUiEffect.SelectConversation("known"),
            viewModel.onAction(DrawerUiAction.SelectConversation("known"))
        )
        assertFalse(viewModel.state.value.isOpen)

        viewModel.onAction(DrawerUiAction.Open)
        assertNull(viewModel.onAction(DrawerUiAction.SelectConversation("missing")))
        assertTrue(viewModel.state.value.isOpen)
    }

    @Test
    fun deleteConversationDoesNotCloseDrawer() {
        val viewModel = DrawerViewModel(
            FakeRepository(
                snapshot(
                    conversations = listOf(DrawerConversationUi("known", "Known", ""))
                )
            )
        )
        viewModel.onAction(DrawerUiAction.Open)

        assertEquals(
            DrawerUiEffect.DeleteConversation("known"),
            viewModel.onAction(DrawerUiAction.DeleteConversation("known"))
        )
        assertTrue(viewModel.state.value.isOpen)
        assertNull(viewModel.onAction(DrawerUiAction.DeleteConversation("missing")))
    }

    @Test
    fun fileRefreshAndRowsAreAcceptedOnlyOnFilesTab() {
        val row = file("/root/a.kt", name = "a.kt", directory = false)
        val viewModel = DrawerViewModel(FakeRepository(snapshot(fileRows = listOf(row))))

        assertNull(viewModel.onAction(DrawerUiAction.RefreshFiles))
        viewModel.onAction(DrawerUiAction.SelectTab(DrawerTab.FILES))
        assertSame(
            DrawerUiEffect.RefreshFiles,
            viewModel.onAction(DrawerUiAction.RefreshFiles)
        )
        assertEquals(
            DrawerUiEffect.SelectFile("/root/a.kt", false),
            viewModel.onAction(DrawerUiAction.SelectFile("/root/a.kt", false))
        )
        assertEquals(
            DrawerUiEffect.LongPressFile("/root/a.kt", "a.kt", false, false),
            viewModel.onAction(
                DrawerUiAction.LongPressFile("/root/a.kt", "a.kt", false, false)
            )
        )
        assertNull(viewModel.onAction(DrawerUiAction.SelectFile("/missing", false)))
    }

    @Test
    fun removableProjectNeedsRequestAndConfirmation() {
        val viewModel = DrawerViewModel(
            FakeRepository(snapshot(projectRemovable = true))
        )

        assertNull(viewModel.onAction(DrawerUiAction.ConfirmRemoveProject))
        assertNull(viewModel.onAction(DrawerUiAction.RequestRemoveProject))
        assertTrue(viewModel.state.value.removeProjectDialogVisible)
        assertSame(
            DrawerUiEffect.RemoveCurrentProject,
            viewModel.onAction(DrawerUiAction.ConfirmRemoveProject)
        )
        assertFalse(viewModel.state.value.removeProjectDialogVisible)
    }

    @Test
    fun dismissAndCloseClearProjectConfirmation() {
        val viewModel = DrawerViewModel(
            FakeRepository(snapshot(projectRemovable = true))
        )
        viewModel.onAction(DrawerUiAction.Open)
        viewModel.onAction(DrawerUiAction.RequestRemoveProject)
        viewModel.onAction(DrawerUiAction.DismissRemoveProject)
        assertFalse(viewModel.state.value.removeProjectDialogVisible)

        viewModel.onAction(DrawerUiAction.RequestRemoveProject)
        viewModel.onAction(DrawerUiAction.Close)
        assertFalse(viewModel.state.value.isOpen)
        assertFalse(viewModel.state.value.removeProjectDialogVisible)
    }

    @Test
    fun stateToStringDoesNotExposePathsOrTitles() {
        val state = DrawerUiState(
            conversations = listOf(DrawerConversationUi("secret-id", "Secret title", "")),
            projectLabel = "Secret project",
            projectPath = "/private/secret",
            fileRows = listOf(file("/private/secret/key.pem"))
        )

        val rendered = state.toString()
        assertFalse(rendered.contains("secret-id"))
        assertFalse(rendered.contains("Secret title"))
        assertFalse(rendered.contains("Secret project"))
        assertFalse(rendered.contains("/private"))
    }

    private class FakeRepository(
        var current: DrawerSnapshot
    ) : DrawerRepository {
        override fun snapshot(): DrawerSnapshot = current
    }

    private fun snapshot(
        conversations: List<DrawerConversationUi> = emptyList(),
        projectLabel: String = "Project",
        projectPath: String = "/project",
        projectRemovable: Boolean = false,
        fileRows: List<DrawerFileUi>? = null
    ) = DrawerSnapshot(
        conversations = conversations,
        currentConversationId = conversations.firstOrNull()?.id.orEmpty(),
        projectLabel = projectLabel,
        projectPath = projectPath,
        projectRemovable = projectRemovable,
        fileRows = fileRows
    )

    private fun file(
        path: String,
        name: String = path.substringAfterLast('/'),
        directory: Boolean = true,
        depth: Int = 0
    ) = DrawerFileUi(
        path = path,
        name = name,
        directory = directory,
        expanded = directory,
        root = depth == 0,
        depth = depth,
        icon = if (directory) DrawerFileIcon.FOLDER_OPEN else DrawerFileIcon.CODE,
        color = if (directory) DrawerFileColor.ACCENT else DrawerFileColor.CODE_YELLOW
    )
}
