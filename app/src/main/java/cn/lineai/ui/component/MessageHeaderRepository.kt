package cn.lineai.ui.component

import cn.lineai.ui.model.MessageHeaderSnapshot
import cn.lineai.ui.model.MessageHeaderStateRepository

class MessageHeaderRepository(
    private val outgoing: Boolean
) : MessageHeaderStateRepository {
    private var name: String = ""

    override fun snapshot(): MessageHeaderSnapshot = MessageHeaderSnapshot(
        outgoing = outgoing,
        name = name,
        monogram = MessageHeaderText.monogram(name)
    )

    override fun bind(name: String?) {
        this.name = MessageHeaderText.normalize(name)
    }
}

object MessageHeaderText {
    @JvmStatic
    fun normalize(name: String?): String = name?.trim().orEmpty()

    @JvmStatic
    fun monogram(name: String?): String {
        if (name == null) return "\u2022"
        name.forEach { character ->
            if (character.isLetterOrDigit()) {
                return character.uppercaseChar().toString()
            }
        }
        return "\u2022"
    }
}
