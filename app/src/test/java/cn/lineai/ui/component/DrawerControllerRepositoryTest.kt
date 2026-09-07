package cn.lineai.ui.component

import cn.lineai.model.ConversationUiModel
import cn.lineai.model.FileTreeNode
import cn.lineai.ui.model.DrawerFileColor
import cn.lineai.ui.model.DrawerFileIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DrawerControllerRepositoryTest {

    @Test
    fun replaceCopiesConversationOrderAndNormalizesNullableValues() {
        val repository = DrawerControllerRepository()

        repository.replace(
            conversations = listOf(
                ConversationUiModel("second", "Second", 0L),
                ConversationUiModel("first", "First", 0L)
            ),
            currentConversationId = null,
            projectLabel = null,
            projectPath = null,
            projectRemovable = false,
            fileTree = null
        )

        val snapshot = repository.snapshot()
        assertEquals(listOf("second", "first"), snapshot.conversations.map { it.id })
        assertEquals(listOf("", ""), snapshot.conversations.map { it.time })
        assertEquals("", snapshot.currentConversationId)
        assertEquals("", snapshot.projectLabel)
        assertEquals("", snapshot.projectPath)
        assertNull(snapshot.fileRows)
    }

    @Test
    fun expandedTreeIsFlattenedInVisualOrderAndCollapsedChildrenStayHidden() {
        val hidden = FileTreeNode(
            "hidden.kt",
            "/root/collapsed/hidden.kt",
            false,
            false,
            emptyList()
        )
        val collapsed = FileTreeNode(
            "collapsed",
            "/root/collapsed",
            true,
            false,
            listOf(hidden)
        )
        val visible = FileTreeNode(
            "visible.kt",
            "/root/visible.kt",
            false,
            false,
            emptyList()
        )
        val root = FileTreeNode(
            "root",
            "/root",
            true,
            true,
            listOf(collapsed, visible)
        )
        val repository = DrawerControllerRepository()

        repository.replace(
            emptyList(),
            "",
            "Project",
            "/root",
            true,
            root
        )

        val rows = repository.snapshot().fileRows.orEmpty()
        assertEquals(
            listOf("/root", "/root/collapsed", "/root/visible.kt"),
            rows.map { it.path }
        )
        assertEquals(listOf(0, 1, 1), rows.map { it.depth })
        assertEquals(listOf(true, false, false), rows.map { it.root })
        assertEquals(DrawerFileIcon.FOLDER_OPEN, rows[0].icon)
        assertEquals(DrawerFileIcon.FOLDER, rows[1].icon)
        assertEquals(DrawerFileIcon.CODE, rows[2].icon)
    }

    @Test
    fun fileKindsAndColorsMatchLegacyRulesCaseInsensitively() {
        val root = FileTreeNode(
            "root",
            "/root",
            true,
            true,
            listOf(
                file("layout.XML"),
                file("Source.KT"),
                file("README.MD"),
                file("archive.bin")
            )
        )
        val repository = DrawerControllerRepository()
        repository.replace(emptyList(), "", "Project", "/root", false, root)

        val rows = repository.snapshot().fileRows.orEmpty().drop(1)
        assertEquals(
            listOf(
                DrawerFileIcon.CODE,
                DrawerFileIcon.CODE,
                DrawerFileIcon.TEXT,
                DrawerFileIcon.FILE
            ),
            rows.map { it.icon }
        )
        assertEquals(
            listOf(
                DrawerFileColor.WARNING,
                DrawerFileColor.CODE_YELLOW,
                DrawerFileColor.SECONDARY,
                DrawerFileColor.TERTIARY
            ),
            rows.map { it.color }
        )
    }

    @Test
    fun eachReplacePublishesACompleteFreshSnapshot() {
        val repository = DrawerControllerRepository()
        repository.replace(
            listOf(ConversationUiModel("old", "Old", 0L)),
            "old",
            "Old project",
            "/old",
            false,
            null
        )
        repository.replace(
            listOf(ConversationUiModel("new", "New", 0L)),
            "new",
            "New project",
            "/new",
            true,
            FileTreeNode("new", "/new", true, false, emptyList())
        )

        val snapshot = repository.snapshot()
        assertEquals(listOf("new"), snapshot.conversations.map { it.id })
        assertEquals("new", snapshot.currentConversationId)
        assertEquals("New project", snapshot.projectLabel)
        assertEquals("/new", snapshot.projectPath)
        assertEquals(true, snapshot.projectRemovable)
        assertEquals(listOf("/new"), snapshot.fileRows?.map { it.path })
    }

    private fun file(name: String) = FileTreeNode(
        name,
        "/root/$name",
        false,
        false,
        emptyList()
    )
}
