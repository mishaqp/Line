package cn.lineai.ui.component

import cn.lineai.model.ChatMessage
import cn.lineai.ui.model.ContextCompactRepository
import cn.lineai.ui.model.ContextCompactSnapshot
import cn.lineai.ui.model.ContextCompactStatus

class ContextCompactStatusRepository(
    label: String?
) : ContextCompactRepository {
    private val initial = ContextCompactSnapshot(label.orEmpty())

    override fun snapshot(): ContextCompactSnapshot = initial

    override fun normalizeStatus(status: String?): ContextCompactStatus = when (status) {
        ChatMessage.COMPACT_STATUS_ERROR -> ContextCompactStatus.ERROR
        ChatMessage.COMPACT_STATUS_DONE -> ContextCompactStatus.DONE
        else -> ContextCompactStatus.RUNNING
    }
}
