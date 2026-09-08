package cn.lineai.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlashCommandPopupLegacyBoundaryTest {
    @Test
    fun javaRowsMapWithoutCallbacksEnteringUiState() {
        val marker = Runnable { error("must not run") }
        val snapshot = SlashCommandPopup.snapshotOf(
            "Commands",
            listOf(
                SlashCommandPopup.Row(
                    "/chat",
                    "Chat mode",
                    marker
                ),
                SlashCommandPopup.Row(
                    null,
                    null,
                    null
                )
            ),
            0
        )

        assertEquals("Commands", snapshot.title)
        assertEquals(
            listOf("/chat", ""),
            snapshot.rows.map { it.label }
        )
        assertEquals(
            listOf("Chat mode", ""),
            snapshot.rows.map { it.description }
        )
        assertEquals(0, snapshot.selectedIndex)
        assertTrue(
            snapshot.toString().contains("Runnable").not()
        )
    }
}
