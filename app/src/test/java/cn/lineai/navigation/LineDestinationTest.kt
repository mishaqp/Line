package cn.lineai.navigation

import cn.lineai.ui.model.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(LineDestinations.fromScreenId("llm") is LineDestination.Llm)
        assertTrue(LineDestinations.fromScreenId("promptTemplates") is LineDestination.PromptTemplates)
        assertTrue(LineDestinations.fromScreenId("mcp") is LineDestination.Mcp)
        assertTrue(LineDestinations.fromScreenId("toolSettings") is LineDestination.ToolSettings)
        assertTrue(LineDestinations.fromScreenId("extensions") is LineDestination.Extensions)
        assertTrue(LineDestinations.fromScreenId("advancedFeatures") is LineDestination.AdvancedFeatures)
        assertTrue(LineDestinations.fromScreenId("input") is LineDestination.Input)
        assertTrue(LineDestinations.fromScreenId("theme") is LineDestination.Theme)
        assertTrue(LineDestinations.fromScreenId("output") is LineDestination.Output)
        assertTrue(LineDestinations.fromScreenId("toolcall_preview") is LineDestination.ToolCallPreview)
        assertTrue(LineDestinations.fromScreenId("security") is LineDestination.Security)
        assertTrue(LineDestinations.fromScreenId("storage") is LineDestination.Storage)
        assertTrue(LineDestinations.fromScreenId("memory") is LineDestination.Memory)
        assertTrue(LineDestinations.fromScreenId("data") is LineDestination.Data)
        assertTrue(LineDestinations.fromScreenId("errorLogs") is LineDestination.ErrorLogs)
        assertTrue(LineDestinations.fromScreenId("keepAlive") is LineDestination.KeepAlive)
        assertTrue(LineDestinations.fromScreenId("about") is LineDestination.About)
        assertTrue(LineDestinations.fromScreenId("licenses") is LineDestination.Licenses)
    }

    @Test
    fun settingsDestinationsRoundTripLegacyScreenIds() {
        SettingsCatalog.sections().flatMap { it.items }.forEach { item ->
            val decoded = LineDestinations.fromScreenId(item.destination.screenId)
            assertEquals(item.destination, decoded)
            assertEquals(item.destination.screenId, decoded.screenId)
            assertFalse(decoded is LineDestination.Legacy)
        }
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
        assertEquals(
            LineDestination.Extensions,
            LineDestinations.parentOf(LineDestination.Extension("skills"))
        )
        assertEquals(
            LineDestination.Llm,
            LineDestinations.parentOf(LineDestination.PromptTemplates)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.Input)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.Output)
        )
        assertEquals(
            LineDestination.Output,
            LineDestinations.parentOf(LineDestination.ToolCallPreview)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.Storage)
        )
        assertEquals(
            LineDestination.About,
            LineDestinations.parentOf(LineDestination.Licenses)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.About)
        )
    }

    @Test
    fun everyMainSettingsDestinationParentsToSettings() {
        val children = listOf(
            LineDestination.Models,
            LineDestination.CodexAccount,
            LineDestination.GrokAccount,
            LineDestination.Llm,
            LineDestination.Mcp,
            LineDestination.ToolSettings,
            LineDestination.Extensions,
            LineDestination.AdvancedFeatures,
            LineDestination.Input,
            LineDestination.Theme,
            LineDestination.Output,
            LineDestination.Security,
            LineDestination.Storage,
            LineDestination.Memory,
            LineDestination.Data,
            LineDestination.ErrorLogs,
            LineDestination.KeepAlive,
            LineDestination.About
        )
        children.forEach { destination ->
            assertEquals(
                destination.screenId,
                LineDestination.Settings,
                LineDestinations.parentOf(destination)
            )
        }
    }
}
