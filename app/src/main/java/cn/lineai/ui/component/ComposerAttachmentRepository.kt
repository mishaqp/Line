package cn.lineai.ui.component

import cn.lineai.model.InputAttachment
import cn.lineai.ui.model.ComposerAttachmentId
import cn.lineai.ui.model.ComposerAttachmentItem
import cn.lineai.ui.model.ComposerAttachmentSnapshot
import cn.lineai.ui.model.ComposerAttachmentStateRepository

class ComposerAttachmentRepository : ComposerAttachmentStateRepository {
    private val attachments = mutableListOf<InputAttachment>()

    fun attachments(): List<InputAttachment> = attachments.toList()

    fun isEmpty(): Boolean = attachments.isEmpty()

    fun replaceAll(next: List<InputAttachment>?) {
        attachments.clear()
        if (next != null) attachments.addAll(next)
    }

    fun clear() {
        attachments.clear()
    }

    fun toggle(attachment: InputAttachment?): Boolean {
        if (attachment == null || attachment.path.isEmpty()) return false
        val index = attachments.indexOfFirst {
            it.matches(attachment.path, attachment.source)
        }
        if (index >= 0) {
            attachments.removeAt(index)
        } else {
            attachments.add(attachment)
        }
        return true
    }

    fun pathsForSource(source: String?): List<String> {
        val normalized = if (InputAttachment.SOURCE_SSH == source) {
            InputAttachment.SOURCE_SSH
        } else {
            InputAttachment.SOURCE_LOCAL
        }
        return attachments
            .asSequence()
            .filter { it.source == normalized }
            .map { it.path }
            .toList()
    }

    override fun snapshot(): ComposerAttachmentSnapshot = ComposerAttachmentSnapshot(
        items = attachments.map { attachment ->
            ComposerAttachmentItem(
                id = ComposerAttachmentId(
                    path = attachment.path,
                    source = attachment.source
                ),
                name = attachment.name
            )
        }
    )

    override fun remove(id: ComposerAttachmentId): Boolean {
        val index = attachments.indexOfFirst {
            it.matches(id.path, id.source)
        }
        if (index < 0) return false
        attachments.removeAt(index)
        return true
    }
}
