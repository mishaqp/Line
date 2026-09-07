package cn.lineai.ui.component

import cn.lineai.ui.model.ComposerPendingQueueItem
import cn.lineai.ui.model.ComposerPendingQueueRepository
import cn.lineai.ui.model.ComposerPendingQueueSnapshot

class ComposerQueueRepository(
    private val queue: ComposerQueue
) : ComposerPendingQueueRepository {
    override fun snapshot(): ComposerPendingQueueSnapshot =
        ComposerPendingQueueSnapshot(
            items = (0 until queue.visibleCount()).map { index ->
                ComposerPendingQueueItem(index, queue.previewLabel(index))
            },
            overflowCount = queue.overflowCount()
        )

    override fun removeAt(index: Int): Boolean = queue.removeAt(index)
}
