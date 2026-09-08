package cn.lineai.ui.component

import cn.lineai.ui.model.SlashCommandPopupSnapshot
import cn.lineai.ui.model.SlashCommandPopupStateRepository
import cn.lineai.ui.model.SlashCommandRowData

class SlashCommandPopupRepository :
    SlashCommandPopupStateRepository {
    private var title = ""
    private var rows: List<SlashCommandRowData> = emptyList()
    private var selectedIndex = -1

    override fun snapshot() = SlashCommandPopupSnapshot(
        title = title,
        rows = rows.toList(),
        selectedIndex = selectedIndex
    )

    override fun bind(
        title: String?,
        rows: List<SlashCommandRowData>,
        selectedIndex: Int
    ) {
        this.title = title.orEmpty()
        this.rows = rows.toList()
        this.selectedIndex = selectedIndex
    }

    override fun setSelectedIndex(index: Int) {
        selectedIndex = index
    }

    override fun clear() {
        title = ""
        rows = emptyList()
        selectedIndex = -1
    }
}
