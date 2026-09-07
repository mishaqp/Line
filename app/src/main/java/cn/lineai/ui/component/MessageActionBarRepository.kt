package cn.lineai.ui.component

import cn.lineai.ui.model.MessageActionBarSnapshot
import cn.lineai.ui.model.MessageActionBarStateRepository

class MessageActionBarRepository(
    private val alignRight: Boolean,
    private val recallEnabled: Boolean,
    actionsAllowed: Boolean
) : MessageActionBarStateRepository {
    private var actionsAllowed = actionsAllowed
    private var expanded = false

    override fun snapshot() = MessageActionBarSnapshot(
        alignRight = alignRight,
        recallEnabled = recallEnabled,
        actionsAllowed = actionsAllowed,
        expanded = expanded
    )

    override fun setExpanded(expanded: Boolean) {
        this.expanded = expanded
    }

    override fun setActionsAllowed(allowed: Boolean) {
        actionsAllowed = allowed
        if (!allowed) expanded = false
    }
}
