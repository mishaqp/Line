package cn.lineai.ui.model

import cn.lineai.R
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.component.ExtensionCardVisualSpec
import cn.lineai.ui.component.extensionCardVisualSpec
import cn.lineai.ui.theme.IconButtonView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ExtensionsViewModelTest {
    @Test
    fun catalogContainsExactlyFiveCardsInLegacyOrder() {
        val items = ExtensionsCatalog.items()

        assertEquals(5, items.size)
        assertEquals(
            listOf(
                ExtensionsUiItem(ExtensionsItemKind.AGENT, LineDestination.Extension("agent")),
                ExtensionsUiItem(ExtensionsItemKind.MCP, LineDestination.Extension("mcp")),
                ExtensionsUiItem(ExtensionsItemKind.SKILLS, LineDestination.Extension("skills")),
                ExtensionsUiItem(ExtensionsItemKind.LINECODE, LineDestination.Extension("linecode")),
                ExtensionsUiItem(ExtensionsItemKind.TERMINAL_PROVIDER, LineDestination.TerminalProvider)
            ),
            items
        )
    }

    @Test
    fun visualSpecsPreserveLegacyResourcesBadgesAndIcons() {
        val expected = listOf(
            ExtensionsItemKind.AGENT to ExtensionCardVisualSpec(
                R.string.screen_extensions_section_agent,
                R.string.screen_extensions_desc_agent,
                R.string.screen_extensions_badge_can_add,
                IconButtonView.BRAIN
            ),
            ExtensionsItemKind.MCP to ExtensionCardVisualSpec(
                R.string.screen_extensions_section_mcp,
                R.string.screen_extensions_desc_mcp,
                R.string.screen_extensions_badge_https,
                IconButtonView.MCP
            ),
            ExtensionsItemKind.SKILLS to ExtensionCardVisualSpec(
                R.string.screen_extensions_section_skills,
                R.string.screen_extensions_desc_skills,
                R.string.screen_extensions_badge_zip,
                IconButtonView.ARCHIVE
            ),
            ExtensionsItemKind.LINECODE to ExtensionCardVisualSpec(
                R.string.screen_extensions_section_linecode,
                R.string.screen_extensions_desc_linecode,
                R.string.screen_extensions_badge_lip,
                IconButtonView.PACKAGE
            ),
            ExtensionsItemKind.TERMINAL_PROVIDER to ExtensionCardVisualSpec(
                R.string.screen_extensions_section_terminal_provider,
                R.string.screen_extensions_desc_terminal_provider,
                R.string.screen_extensions_badge_terminal_provider,
                IconButtonView.TERMINAL
            )
        )

        expected.forEach { (kind, spec) ->
            assertEquals(spec, extensionCardVisualSpec(kind))
        }
    }

    @Test
    fun destinationsKeepMcpAgentAndTerminalProviderDistinct() {
        val items = ExtensionsCatalog.items().associateBy { it.kind }

        assertEquals(LineDestination.Extension("mcp"), items.getValue(ExtensionsItemKind.MCP).destination)
        assertEquals(LineDestination.Extension("agent"), items.getValue(ExtensionsItemKind.AGENT).destination)
        assertEquals(LineDestination.TerminalProvider, items.getValue(ExtensionsItemKind.TERMINAL_PROVIDER).destination)
        assertEquals("extension:mcp", items.getValue(ExtensionsItemKind.MCP).destination.screenId)
        assertEquals("extension:agent", items.getValue(ExtensionsItemKind.AGENT).destination.screenId)
        assertEquals("terminalProvider", items.getValue(ExtensionsItemKind.TERMINAL_PROVIDER).destination.screenId)
    }

    @Test
    fun backProducesBackEffect() {
        val viewModel = ExtensionsViewModel()

        assertSame(
            ExtensionsUiEffect.Back,
            viewModel.onAction(ExtensionsUiAction.Back)
        )
    }

    @Test
    fun everyCardProducesExactlyItsTypedNavigationEffect() {
        val viewModel = ExtensionsViewModel()

        ExtensionsCatalog.items().forEach { item ->
            assertEquals(
                ExtensionsUiEffect.Navigate(item.destination),
                viewModel.onAction(ExtensionsUiAction.Open(item.destination))
            )
        }
    }
}
