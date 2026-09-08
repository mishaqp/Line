package cn.lineai.ui.component

import cn.lineai.ui.model.ShellCommandRepository
import cn.lineai.ui.model.ShellCommandSnapshot

fun interface ShellCommandSource {
    fun currentCommand(): String?
}

class ShellCommandControllerRepository(
    private val source: ShellCommandSource
) : ShellCommandRepository {

    override fun snapshot(): ShellCommandSnapshot {
        val command = source.currentCommand()
        val empty = command == null || command.length == 0
        return ShellCommandSnapshot(
            command = if (empty) "" else command,
            hasCommand = !empty
        )
    }
}
