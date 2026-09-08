package cn.lineai.ui.component

import cn.lineai.model.InputAttachment
import cn.lineai.ui.model.UserMessageAttachmentItem
import cn.lineai.ui.model.UserMessageAttachmentSnapshot
import cn.lineai.ui.model.UserMessageAttachmentStateRepository

class UserMessageAttachmentRepository : UserMessageAttachmentStateRepository {
    private var items: List<UserMessageAttachmentItem> = emptyList()

    override fun snapshot() = UserMessageAttachmentSnapshot(items.toList())

    override fun replaceAll(attachments: List<InputAttachment>?) {
        items = attachments.orEmpty().map { attachment ->
            UserMessageAttachmentItem(attachment.name.orEmpty())
        }
    }
}
