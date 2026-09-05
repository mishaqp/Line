package cn.lineai.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LineDestinationTest {
    @Test
    fun dynamicDestinationsRoundTripWithoutParsingAtCallSites() {
        val ids = listOf(
            "modelAdd:preset:codex",
            "modelEdit:model-42",
            "browser:https://example.com/path?a=1",
            "extension:skills",
            "agentEdit:agent-7",
            "mcpEdit:server-3"
        )

        ids.forEach { id ->
            assertEquals(id, LineDestinations.fromScreenId(id).screenId)
        }
    }

    @Test
    fun knownRoutesUseTypedKeys() {
        assertTrue(LineDestinations.fromScreenId("settings") is LineDestination.Settings)
        assertTrue(LineDestinations.fromScreenId("codexAccount") is LineDestination.CodexAccount)
        assertTrue(LineDestinations.fromScreenId("grokAccount") is LineDestination.GrokAccount)
        assertTrue(LineDestinations.fromScreenId("modelEdit:m1") is LineDestination.ModelEdit)
    }

    @Test
    fun typedParentsPreserveLegacyBackBehavior() {
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.CodexAccount)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.GrokAccount)
        )
        assertEquals(
            LineDestination.Models,
            LineDestinations.parentOf(LineDestination.ModelEdit("m1"))
        )
        assertEquals(
            LineDestination.ModelAddOptions,
            LineDestinations.parentOf(LineDestination.ModelAddPreset("codex"))
        )
    }
}
