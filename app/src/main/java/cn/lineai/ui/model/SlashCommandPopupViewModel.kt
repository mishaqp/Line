package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SlashCommandRowData(
    val label: String,
    val description: String
)

data class SlashCommandPopupSnapshot(
    val title: String = "",
    val rows: List<SlashCommandRowData> = emptyList(),
    val selectedIndex: Int = -1
)

data class SlashCommandRowUi(
    val index: Int,
    val label: String,
    val description: String,
    val selected: Boolean
)

data class SlashCommandPopupUiState(
    val title: String = "",
    val rows: List<SlashCommandRowUi> = emptyList()
) {
    val visible: Boolean get() = rows.isNotEmpty()
}

sealed interface SlashCommandPopupUiAction {
    data class Bind(
        val title: String?,
        val rows: List<SlashCommandRowData>,
        val selectedIndex: Int
    ) : SlashCommandPopupUiAction

    data class SetSelectedIndex(
        val index: Int
    ) : SlashCommandPopupUiAction

    data class Select(
        val index: Int
    ) : SlashCommandPopupUiAction

    data object Clear : SlashCommandPopupUiAction
}

sealed interface SlashCommandPopupUiEffect {
    data class RowSelected(
        val index: Int
    ) : SlashCommandPopupUiEffect
}

interface SlashCommandPopupStateRepository {
    fun snapshot(): SlashCommandPopupSnapshot

    fun bind(
        title: String?,
        rows: List<SlashCommandRowData>,
        selectedIndex: Int
    )

    fun setSelectedIndex(index: Int)
    fun clear()
}

class SlashCommandPopupViewModel(
    private val repository: SlashCommandPopupStateRepository
) : ViewModel() {
    private val _state =
        MutableStateFlow(repository.snapshot().toUiState())
    val state: StateFlow<SlashCommandPopupUiState> =
        _state.asStateFlow()

    fun onAction(
        action: SlashCommandPopupUiAction
    ): SlashCommandPopupUiEffect? = when (action) {
        is SlashCommandPopupUiAction.Bind -> {
            repository.bind(
                action.title,
                action.rows,
                action.selectedIndex
            )
            refresh()
            null
        }
        is SlashCommandPopupUiAction.SetSelectedIndex -> {
            repository.setSelectedIndex(action.index)
            refresh()
            null
        }
        is SlashCommandPopupUiAction.Select -> {
            if (action.index in _state.value.rows.indices) {
                SlashCommandPopupUiEffect.RowSelected(
                    action.index
                )
            } else {
                null
            }
        }
        SlashCommandPopupUiAction.Clear -> {
            repository.clear()
            refresh()
            null
        }
    }

    private fun refresh() {
        _state.value = repository.snapshot().toUiState()
    }

    private fun SlashCommandPopupSnapshot.toUiState() =
        SlashCommandPopupUiState(
            title = title,
            rows = rows.mapIndexed { index, row ->
                SlashCommandRowUi(
                    index = index,
                    label = row.label,
                    description = row.description,
                    selected = index == selectedIndex
                )
            }
        )

    companion object {
        fun factory(
            repository: SlashCommandPopupStateRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    if (
                        modelClass.isAssignableFrom(
                            SlashCommandPopupViewModel::class.java
                        )
                    ) {
                        return SlashCommandPopupViewModel(
                            repository
                        ) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: " +
                            modelClass.name
                    )
                }
            }
    }
}
