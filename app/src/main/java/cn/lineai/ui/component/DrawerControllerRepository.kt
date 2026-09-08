package cn.lineai.ui.component

import cn.lineai.model.ConversationUiModel
import cn.lineai.model.FileTreeNode
import cn.lineai.ui.model.DrawerConversationUi
import cn.lineai.ui.model.DrawerFileColor
import cn.lineai.ui.model.DrawerFileIcon
import cn.lineai.ui.model.DrawerFileUi
import cn.lineai.ui.model.DrawerRepository
import cn.lineai.ui.model.DrawerSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrawerControllerRepository : DrawerRepository {
    @Volatile
    private var current = DrawerSnapshot()

    override fun snapshot(): DrawerSnapshot = current

    fun replace(
        conversations: List<ConversationUiModel>?,
        currentConversationId: String?,
        projectLabel: String?,
        projectPath: String?,
        projectRemovable: Boolean,
        fileTree: FileTreeNode?
    ) {
        current = DrawerSnapshot(
            conversations = conversations.orEmpty().map { conversation ->
                DrawerConversationUi(
                    id = conversation.id,
                    title = conversation.title,
                    time = formatTime(conversation.updatedAt)
                )
            },
            currentConversationId = currentConversationId.orEmpty(),
            projectLabel = projectLabel.orEmpty(),
            projectPath = projectPath.orEmpty(),
            projectRemovable = projectRemovable,
            fileRows = fileTree?.let(::flatten)
        )
    }

    private fun flatten(root: FileTreeNode): List<DrawerFileUi> {
        val result = mutableListOf<DrawerFileUi>()
        appendNode(result, root, depth = 0, root = true)
        return result.toList()
    }

    private fun appendNode(
        result: MutableList<DrawerFileUi>,
        node: FileTreeNode,
        depth: Int,
        root: Boolean
    ) {
        val icon = iconFor(node)
        result += DrawerFileUi(
            path = node.path,
            name = node.name,
            directory = node.isDirectory,
            expanded = node.isExpanded,
            root = root,
            depth = depth,
            icon = icon,
            color = colorFor(node, icon)
        )
        if (node.isDirectory && node.isExpanded) {
            node.children.forEach { child ->
                appendNode(result, child, depth + 1, root = false)
            }
        }
    }

    private fun iconFor(node: FileTreeNode): DrawerFileIcon {
        if (node.isDirectory) {
            return if (node.isExpanded) DrawerFileIcon.FOLDER_OPEN else DrawerFileIcon.FOLDER
        }
        val lower = node.name.lowercase(Locale.US)
        return when {
            CODE_EXTENSIONS.any(lower::endsWith) -> DrawerFileIcon.CODE
            TEXT_EXTENSIONS.any(lower::endsWith) -> DrawerFileIcon.TEXT
            else -> DrawerFileIcon.FILE
        }
    }

    private fun colorFor(node: FileTreeNode, icon: DrawerFileIcon): DrawerFileColor =
        when {
            node.isDirectory && node.isExpanded -> DrawerFileColor.ACCENT
            node.isDirectory -> DrawerFileColor.SECONDARY
            icon == DrawerFileIcon.CODE && node.name.lowercase(Locale.US).endsWith(".xml") ->
                DrawerFileColor.WARNING
            icon == DrawerFileIcon.CODE -> DrawerFileColor.CODE_YELLOW
            icon == DrawerFileIcon.TEXT -> DrawerFileColor.SECONDARY
            else -> DrawerFileColor.TERTIARY
        }

    private fun formatTime(updatedAt: Long): String {
        if (updatedAt <= 0L) return ""
        return SimpleDateFormat("M/d HH:mm", Locale.US).format(Date(updatedAt))
    }

    private companion object {
        val CODE_EXTENSIONS = listOf(
            ".java", ".kt", ".js", ".ts", ".tsx", ".jsx", ".xml", ".json", ".gradle"
        )
        val TEXT_EXTENSIONS = listOf(".md", ".txt", ".log")
    }
}
