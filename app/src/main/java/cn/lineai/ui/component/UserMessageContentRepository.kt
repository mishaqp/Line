package cn.lineai.ui.component

import cn.lineai.model.ChatMessage
import cn.lineai.ui.model.UserMessageContentSnapshot
import cn.lineai.ui.model.UserMessageContentStateRepository

class UserMessageContentRepository(
    private val attachedFilesLabel: String,
    private val maxWidthDp: Float
) : UserMessageContentStateRepository {
    private var content: String = ""

    override fun snapshot() = UserMessageContentSnapshot(
        content = content,
        maxWidthDp = maxWidthDp
    )

    override fun bind(message: ChatMessage?) {
        content = visibleContent(message)
    }

    private fun visibleContent(message: ChatMessage?): String {
        if (message == null) return ""
        val value = message.content
        if (value.isEmpty() && message.hasAttachments()) return ""
        if (
            value.trim() == attachedFilesLabel &&
            message.hasAttachments()
        ) {
            return ""
        }
        return value
    }
}
