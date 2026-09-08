package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlashCommandPopupViewModelTest {
    private class FakeRepository :
        SlashCommandPopupStateRepository {
        private var value = SlashCommandPopupSnapshot()

        override fun snapshot() = value

        override fun bind(
            title: String?,
            rows: List<SlashCommandRowData>,
            selectedIndex: Int
        ) {
            value = SlashCommandPopupSnapshot(
                title.orEmpty(),
                rows.toList(),
                selectedIndex
            )
        }

        override fun setSelectedIndex(index: Int) {
            value = value.copy(selectedIndex = index)
        }

        override fun clear() {
            value = SlashCommandPopupSnapshot()
        }
    }

    @Test
    fun bindKeepsOrderAndMarksSelectedRow() {
        val viewModel =
            SlashCommandPopupViewModel(FakeRepository())
        val rows = listOf(
            SlashCommandRowData("/chat", "Chat"),
            SlashCommandRowData("/agent", "Agent")
        )

        viewModel.onAction(
            SlashCommandPopupUiAction.Bind(
                "Commands",
                rows,
                1
            )
        )

        assertTrue(viewModel.state.value.visible)
        assertEquals(
            listOf("/chat", "/agent"),
            viewModel.state.value.rows.map { it.label }
        )
        assertFalse(viewModel.state.value.rows[0].selected)
        assertTrue(viewModel.state.value.rows[1].selected)
    }

    @Test
    fun validSelectionEmitsOneIndexedEffect() {
        val viewModel =
            SlashCommandPopupViewModel(FakeRepository())
        viewModel.onAction(
            SlashCommandPopupUiAction.Bind(
                "",
                listOf(SlashCommandRowData("/chat", "")),
                -1
            )
        )

        assertEquals(
            SlashCommandPopupUiEffect.RowSelected(0),
            viewModel.onAction(
                SlashCommandPopupUiAction.Select(0)
            )
        )
        assertNull(
            viewModel.onAction(
                SlashCommandPopupUiAction.Select(2)
            )
        )
    }

    @Test
    fun clearHidesPopupState() {
        val viewModel =
            SlashCommandPopupViewModel(FakeRepository())
        viewModel.onAction(
            SlashCommandPopupUiAction.Bind(
                "Commands",
                listOf(SlashCommandRowData("/chat", "")),
                0
            )
        )

        viewModel.onAction(SlashCommandPopupUiAction.Clear)

        assertFalse(viewModel.state.value.visible)
    }
}
