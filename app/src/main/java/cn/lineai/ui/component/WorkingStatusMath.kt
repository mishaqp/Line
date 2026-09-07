package cn.lineai.ui.component

object WorkingStatusMath {
    private val frameOffsets = intArrayOf(1, 2, 5, 8, 7, 6, 3, 0)

    fun isThinking(reasoning: String?, content: String?): Boolean =
        !reasoning.isNullOrBlank() && content.isNullOrBlank()

    fun highlightColor(baseColor: Int): Int =
        (0xff shl 24) or
            (blendChannel(baseColor shr 16 and 0xff) shl 16) or
            (blendChannel(baseColor shr 8 and 0xff) shl 8) or
            blendChannel(baseColor and 0xff)

    fun dotAlphas(progress: Float): List<Float> {
        val frame = (progress.coerceIn(0f, 1f) * 1500f / 90f).toInt()
        val active = frameOffsets[frame % frameOffsets.size]
        val trailing = frameOffsets[(frame + frameOffsets.size - 1) % frameOffsets.size]
        return List(9) { offset ->
            when (offset) {
                active -> 1f
                trailing -> 0.52f
                else -> 0.18f
            }
        }
    }

    private fun blendChannel(value: Int): Int =
        kotlin.math.round(value + (255 - value) * 0.72f).toInt()
}
