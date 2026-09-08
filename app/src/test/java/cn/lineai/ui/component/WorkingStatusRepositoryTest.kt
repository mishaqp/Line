package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkingStatusRepositoryTest {
    @Test
    fun repositorySanitizesNullLabelsAndReturnsStableSnapshot() {
        val repository = WorkingStatusLabelRepository(null, null)

        assertEquals("", repository.snapshot().workingLabel)
        assertEquals("", repository.snapshot().thinkingLabel)
        assertTrue(repository.snapshot() === repository.snapshot())
    }

    @Test
    fun animationMathPreservesLegacyDotAndHighlightRules() {
        val alphas = WorkingStatusMath.dotAlphas(0f)
        assertEquals(1, alphas.count { it == 1f })
        assertEquals(1, alphas.count { it == 0.52f })
        assertEquals(7, alphas.count { it == 0.18f })

        val base = 0xff505050.toInt()
        val highlight = WorkingStatusMath.highlightColor(base)
        assertTrue((highlight shr 16 and 0xff) > (base shr 16 and 0xff))
        assertTrue((highlight shr 8 and 0xff) > (base shr 8 and 0xff))
        assertTrue((highlight and 0xff) > (base and 0xff))
    }
}
