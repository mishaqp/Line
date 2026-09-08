package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandControllerRepositoryTest {

    @Test
    fun snapshotReadsSourceOnEveryCall() {
        val source = MutableSource("pwd")
        val repository = ShellCommandControllerRepository(source)

        val first = repository.snapshot()
        source.commandValue = "id"
        val second = repository.snapshot()

        assertEquals("pwd", first.command)
        assertTrue(first.hasCommand)
        assertEquals("id", second.command)
        assertTrue(second.hasCommand)
        assertEquals(2, source.reads)
    }

    @Test
    fun nullAndEmptyAreEmptyWithoutTrimmingSpaces() {
        val source = MutableSource(null)
        val repository = ShellCommandControllerRepository(source)

        val missing = repository.snapshot()
        assertEquals("", missing.command)
        assertFalse(missing.hasCommand)

        source.commandValue = ""
        val empty = repository.snapshot()
        assertEquals("", empty.command)
        assertFalse(empty.hasCommand)

        source.commandValue = "   "
        val spaces = repository.snapshot()
        assertEquals("   ", spaces.command)
        assertTrue(spaces.hasCommand)

        source.commandValue = "pwd\nid"
        val multiline = repository.snapshot()
        assertEquals("pwd\nid", multiline.command)
        assertTrue(multiline.hasCommand)
    }

    @Test
    fun snapshotDoesNotMutateSource() {
        val source = MutableSource("  pwd  ")
        val repository = ShellCommandControllerRepository(source)
        val snapshot = repository.snapshot()
        assertEquals("  pwd  ", snapshot.command)
        assertEquals("  pwd  ", source.commandValue)
        assertEquals(1, source.reads)
    }

    @Test
    fun snapshotToStringOmitsSecretToken() {
        val secret = "SECRET_TOKEN_FOR_TEST"
        val text = ShellCommandControllerRepository(MutableSource(secret)).snapshot().toString()
        assertFalse(text.contains(secret))
        assertTrue(text.contains("commandLength=${secret.length}"))
    }

    private class MutableSource(
        var commandValue: String?
    ) : ShellCommandSource {
        var reads = 0

        override fun currentCommand(): String? {
            reads += 1
            return commandValue
        }
    }
}
