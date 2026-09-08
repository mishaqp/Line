package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageHeaderRepositoryTest {
    @Test
    fun snapshotIsNormalizedAndKeepsDirection() {
        val repository = MessageHeaderRepository(false)

        repository.bind("  model 7  ")
        val snapshot = repository.snapshot()

        assertFalse(snapshot.outgoing)
        assertEquals("model 7", snapshot.name)
        assertEquals("M", snapshot.monogram)
    }

    @Test
    fun outgoingRepositoryKeepsOutgoingIdentity() {
        val repository = MessageHeaderRepository(true)
        repository.bind(null)

        val snapshot = repository.snapshot()

        assertTrue(snapshot.outgoing)
        assertEquals("", snapshot.name)
        assertEquals("\u2022", snapshot.monogram)
    }
}
