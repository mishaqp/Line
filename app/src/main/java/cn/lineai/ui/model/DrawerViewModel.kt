package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DrawerTab {
    CONVERSATIONS,
    FILES
}

enum class DrawerFileIcon {
    FOLDER,
    FOLDER_OPEN,
    CODE,
    TEXT,
    FILE
}

enum class DrawerFileColor {
    ACCENT,
    SECONDARY,
    TERTIARY,
    WARNING,
    CODE_YELLOW
}

data class DrawerConversationUi(
    val id: String,
    val title: String,
    val time: String
)

data class DrawerFileUi(
    val path: String,
    val name: String,
    val directory: Boolean,
    val expanded: Boolean,
    val root: Boolean,
    val depth: Int,
    val icon: DrawerFileIcon,
    val color: DrawerFileColor
)

data class DrawerSnapshot(
    val conversations: List<DrawerConversationUi> = emptyList(),
    val currentConversationId: String = "",
    val projectLabel: String = "",
    val projectPath: String = "",
    val projectRemovable: Boolean = false,
    val fileRows: List<DrawerFileUi>? = null
) {
    override fun toString(): String =
        "DrawerSnapshot(conversations=${conversations.size}, currentConversation=${currentConversationId.isNotEmpty()}, " +
            "projectPathLength=${projectPath.length}, projectRemovable=$projectRemovable, " +
            "fileRows=${fileRows?.size})"
}

data class DrawerUiState(
    val isOpen: Boolean = false,
    val activeTab: DrawerTab = DrawerTab.CONVERSATIONS,
    val conversations: List<DrawerConversationUi> = emptyList(),
    val currentConversationId: String = "",
    val projectLabel: String = "",
    val projectPath: String = "",
    val projectRemovable: Boolean = false,
    val fileRows: List<DrawerFileUi>? = null,
    val removeProjectDialogVisible: Boolean = false
) {
    override fun toString(): String =
        "DrawerUiState(isOpen=$isOpen, activeTab=$activeTab, conversations=${conversations.size}, " +
            "currentConversation=${currentConversationId.isNotEmpty()}, projectPathLength=${projectPath.length}, " +
            "projectRemovable=$projectRemovable, fileRows=${fileRows?.size}, " +
            "removeProjectDialogVisible=$removeProjectDialogVisible)"
}

sealed interface DrawerUiAction {
    data object Reload : DrawerUiAction
    data object Open : DrawerUiAction
    data object Close : DrawerUiAction
    data class SelectTab(val tab: DrawerTab) : DrawerUiAction
    data object NewConversation : DrawerUiAction
    data class SelectConversation(val id: String) : DrawerUiAction
    data class DeleteConversation(val id: String) : DrawerUiAction
    data object RefreshFiles : DrawerUiAction
    data class SelectFile(val path: String, val directory: Boolean) : DrawerUiAction
    data class LongPressFile(
        val path: String,
        val name: String,
        val directory: Boolean,
        val root: Boolean
    ) : DrawerUiAction
    data object RequestRemoveProject : DrawerUiAction
    data object ConfirmRemoveProject : DrawerUiAction
    data object DismissRemoveProject : DrawerUiAction
}

sealed interface DrawerUiEffect {
    data object NewConversation : DrawerUiEffect
    data class SelectConversation(val id: String) : DrawerUiEffect
    data class DeleteConversation(val id: String) : DrawerUiEffect
    data object FileTreeActivated : DrawerUiEffect
    data object RefreshFiles : DrawerUiEffect
    data class SelectFile(val path: String, val directory: Boolean) : DrawerUiEffect
    data class LongPressFile(
        val path: String,
        val name: String,
        val directory: Boolean,
        val root: Boolean
    ) : DrawerUiEffect
    data object RemoveCurrentProject : DrawerUiEffect
}

interface DrawerRepository {
    fun snapshot(): DrawerSnapshot
}

class DrawerViewModel(
    private val repository: DrawerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(readInitialState())
    val state: StateFlow<DrawerUiState> = _state.asStateFlow()

    fun onAction(action: DrawerUiAction): DrawerUiEffect? = when (action) {
        DrawerUiAction.Reload -> {
            reload()
            null
        }
        DrawerUiAction.Open -> {
            _state.update { it.copy(isOpen = true) }
            null
        }
        DrawerUiAction.Close -> {
            _state.update {
                it.copy(isOpen = false, removeProjectDialogVisible = false)
            }
            null
        }
        is DrawerUiAction.SelectTab -> selectTab(action.tab)
        DrawerUiAction.NewConversation -> {
            closeForNavigation()
            DrawerUiEffect.NewConversation
        }
        is DrawerUiAction.SelectConversation -> {
            if (_state.value.conversations.none { it.id == action.id }) {
                null
            } else {
                closeForNavigation()
                DrawerUiEffect.SelectConversation(action.id)
            }
        }
        is DrawerUiAction.DeleteConversation -> {
            action.takeIf { requested ->
                _state.value.conversations.any { it.id == requested.id }
            }?.let { DrawerUiEffect.DeleteConversation(it.id) }
        }
        DrawerUiAction.RefreshFiles -> {
            if (_state.value.activeTab == DrawerTab.FILES) {
                DrawerUiEffect.RefreshFiles
            } else {
                null
            }
        }
        is DrawerUiAction.SelectFile -> {
            action.takeIf { requested ->
                _state.value.fileRows.orEmpty().any {
                    it.path == requested.path && it.directory == requested.directory
                }
            }?.let { DrawerUiEffect.SelectFile(it.path, it.directory) }
        }
        is DrawerUiAction.LongPressFile -> {
            action.takeIf { requested ->
                _state.value.fileRows.orEmpty().any {
                    it.path == requested.path &&
                        it.name == requested.name &&
                        it.directory == requested.directory &&
                        it.root == requested.root
                }
            }?.let {
                DrawerUiEffect.LongPressFile(
                    path = it.path,
                    name = it.name,
                    directory = it.directory,
                    root = it.root
                )
            }
        }
        DrawerUiAction.RequestRemoveProject -> {
            if (_state.value.projectRemovable) {
                _state.update { it.copy(removeProjectDialogVisible = true) }
            }
            null
        }
        DrawerUiAction.ConfirmRemoveProject -> {
            if (!_state.value.removeProjectDialogVisible) {
                null
            } else {
                _state.update { it.copy(removeProjectDialogVisible = false) }
                DrawerUiEffect.RemoveCurrentProject
            }
        }
        DrawerUiAction.DismissRemoveProject -> {
            _state.update { it.copy(removeProjectDialogVisible = false) }
            null
        }
    }

    private fun selectTab(tab: DrawerTab): DrawerUiEffect? {
        if (_state.value.activeTab == tab) return null
        _state.update {
            it.copy(activeTab = tab, removeProjectDialogVisible = false)
        }
        return if (tab == DrawerTab.FILES) {
            DrawerUiEffect.FileTreeActivated
        } else {
            null
        }
    }

    private fun closeForNavigation() {
        _state.update {
            it.copy(isOpen = false, removeProjectDialogVisible = false)
        }
    }

    private fun reload() {
        val snapshot = runCatching(repository::snapshot).getOrNull() ?: return
        _state.update { current ->
            current.copy(
                conversations = snapshot.conversations,
                currentConversationId = snapshot.currentConversationId,
                projectLabel = snapshot.projectLabel,
                projectPath = snapshot.projectPath,
                projectRemovable = snapshot.projectRemovable,
                fileRows = snapshot.fileRows,
                removeProjectDialogVisible =
                    current.removeProjectDialogVisible && snapshot.projectRemovable
            )
        }
    }

    private fun readInitialState(): DrawerUiState {
        val snapshot = runCatching(repository::snapshot).getOrDefault(DrawerSnapshot())
        return DrawerUiState(
            conversations = snapshot.conversations,
            currentConversationId = snapshot.currentConversationId,
            projectLabel = snapshot.projectLabel,
            projectPath = snapshot.projectPath,
            projectRemovable = snapshot.projectRemovable,
            fileRows = snapshot.fileRows
        )
    }

    companion object {
        fun factory(repository: DrawerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(DrawerViewModel::class.java)) {
                        return DrawerViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
