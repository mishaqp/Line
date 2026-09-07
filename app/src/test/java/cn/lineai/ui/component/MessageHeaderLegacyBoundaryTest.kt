package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageHeaderLegacyBoundaryTest {
    @Test
    fun legacyMonogramContractIsPreserved() {
        assertEquals("K", MessageHeaderView.monogram("kimi-k3"))
        assertEquals("7", MessageHeaderView.monogram(" --7 model"))
        assertEquals("\u2022", MessageHeaderView.monogram(" -- "))
        assertEquals("\u2022", MessageHeaderView.monogram(null))
    }
}
