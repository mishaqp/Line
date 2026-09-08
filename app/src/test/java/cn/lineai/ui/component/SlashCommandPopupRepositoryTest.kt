package cn.lineai.ui.component

import cn.lineai.ui.model.SlashCommandRowData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SlashCommandPopupRepositoryTest {
    @Test
    fun bindCopiesRowsAndPreservesSelection() {
        val rows = mutableListOf(
            SlashCommandRowData("/chat", "Chat")
        )
        val repository = SlashCommandPopupRepository()

        repository.bind("Commands", rows, 0)
        rows.clear()
        val snapshot = repository.snapshot()

        assertEquals("Commands", snapshot.title)
        assertEquals("/chat", snapshot.rows.single().label)
        assertEquals(0, snapshot.selectedIndex)
    }

    @Test
    fun clearDropsAllPopupState() {
        val repository = SlashCommandPopupRepository()
        repository.bind(
            "Commands",
            listOf(SlashCommandRowData("/chat", "Chat")),
            0
        )

        repository.clear()

        assertTrue(repository.snapshot().rows.isEmpty())
        assertEquals(-1, repository.snapshot().selectedIndex)
    }
}
