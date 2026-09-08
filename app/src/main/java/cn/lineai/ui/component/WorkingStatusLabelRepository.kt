package cn.lineai.ui.component

import cn.lineai.ui.model.WorkingStatusRepository
import cn.lineai.ui.model.WorkingStatusSnapshot

class WorkingStatusLabelRepository(
    workingLabel: String?,
    thinkingLabel: String?
) : WorkingStatusRepository {
    private val snapshot = WorkingStatusSnapshot(
        workingLabel = workingLabel.orEmpty(),
        thinkingLabel = thinkingLabel.orEmpty()
    )

    override fun snapshot(): WorkingStatusSnapshot = snapshot
}
