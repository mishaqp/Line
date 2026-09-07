package cn.lineai.ui.model

import cn.lineai.model.ChatScale
import cn.lineai.model.ThemePalette
import cn.lineai.model.ThemeSettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsViewModelTest {

    @Test
    fun loadsPersistedModesAndCustomColors() {
        val custom = mapOf(ThemePalette.KEY_ACCENT to "#112233")
        val repository = FakeRepository(
            state = ThemeSettingsState(
                ThemePalette.MODE_COFFEE,
                ThemePalette.MODE_COFFEE,
                custom,
                ThemePalette.forMode(ThemePalette.MODE_COFFEE)
            ),
            scale = ChatScale.MODE_COMPACT
        )

        val state = ThemeSettingsViewModel(repository).state.value

        assertEquals(ThemePalette.MODE_COFFEE, state.themeMode)
        assertEquals(ChatScale.MODE_COMPACT, state.chatScaleMode)
        assertEquals("#112233", state.draftColors[ThemePalette.KEY_ACCENT])
        assertEquals(ThemeSettingsUiState.STARTER_SAVED, state.activeStarter)
        assertTrue(state.isDraftValid)
    }

    @Test
    fun themeAndScaleChangesPersistImmediately() {
        val repository = FakeRepository()
        val viewModel = ThemeSettingsViewModel(repository)

        viewModel.onAction(ThemeSettingsUiAction.SelectThemeMode(ThemePalette.MODE_VSCODE))
        viewModel.onAction(ThemeSettingsUiAction.SelectChatScale(ChatScale.MODE_LARGE))

        assertEquals(ThemePalette.MODE_VSCODE, viewModel.state.value.themeMode)
        assertEquals(ChatScale.MODE_LARGE, viewModel.state.value.chatScaleMode)
        assertEquals(listOf(ThemePalette.MODE_VSCODE), repository.themeWrites)
        assertEquals(listOf(ChatScale.MODE_LARGE), repository.scaleWrites)
    }

    @Test
    fun editingOneColorDoesNotChangeOtherColors() {
        val repository = FakeRepository()
        val viewModel = ThemeSettingsViewModel(repository)
        val before = viewModel.state.value.draftColors[ThemePalette.KEY_BG]

        viewModel.onAction(
            ThemeSettingsUiAction.EditColor(ThemePalette.KEY_ACCENT, "a1b2c3")
        )

        val state = viewModel.state.value
        assertEquals("#A1B2C3", state.draftColors[ThemePalette.KEY_ACCENT]?.uppercase())
        assertEquals(before, state.draftColors[ThemePalette.KEY_BG])
        assertEquals(ThemePalette.KEY_ACCENT, state.activeKey)
        assertEquals(ThemeSettingsUiState.STARTER_EDITING, state.activeStarter)
    }

    @Test
    fun invalidDraftCannotBeSaved() {
        val repository = FakeRepository()
        val viewModel = ThemeSettingsViewModel(repository)

        viewModel.onAction(
            ThemeSettingsUiAction.EditColor(ThemePalette.KEY_ACCENT, "#12")
        )
        assertFalse(viewModel.state.value.isDraftValid)

        viewModel.onAction(ThemeSettingsUiAction.SaveCustomColors)

        assertTrue(repository.colorWrites.isEmpty())
    }

    @Test
    fun validDraftSavesCompleteIsolatedSnapshot() {
        val repository = FakeRepository()
        val viewModel = ThemeSettingsViewModel(repository)

        viewModel.onAction(
            ThemeSettingsUiAction.EditColor(ThemePalette.KEY_ACCENT, "#ABCDEF")
        )
        viewModel.onAction(ThemeSettingsUiAction.SaveCustomColors)

        assertEquals(1, repository.colorWrites.size)
        val saved = repository.colorWrites.single()
        assertEquals(ThemePalette.EDITABLE_KEYS.size, saved.size)
        assertEquals("#ABCDEF", saved[ThemePalette.KEY_ACCENT])
        assertEquals(ThemeSettingsUiState.STARTER_SAVED, viewModel.state.value.activeStarter)
        assertEquals(saved, viewModel.state.value.savedCustomColors)
    }

    @Test
    fun starterAndResetReplaceOnlyTheDraft() {
        val repository = FakeRepository()
        val viewModel = ThemeSettingsViewModel(repository)

        viewModel.onAction(ThemeSettingsUiAction.SelectStarter(ThemePalette.MODE_LIGHT))
        assertEquals(
            "#F2F2F7",
            viewModel.state.value.draftColors[ThemePalette.KEY_CODE_BG]
        )
        assertEquals(ThemePalette.MODE_LIGHT, viewModel.state.value.activeStarter)

        viewModel.onAction(ThemeSettingsUiAction.ResetCustomColors)

        assertEquals(
            ThemeSettingsUiState.STARTER_DEFAULT,
            viewModel.state.value.activeStarter
        )
        assertEquals(
            ThemePalette.forMode(ThemePalette.MODE_CUSTOM).editableHexMap(),
            viewModel.state.value.draftColors
        )
        assertTrue(repository.colorWrites.isEmpty())
    }

    @Test
    fun refreshReloadsRepositoryState() {
        val repository = FakeRepository()
        val viewModel = ThemeSettingsViewModel(repository)
        repository.state = ThemeSettingsState(
            ThemePalette.MODE_DARK,
            ThemePalette.MODE_DARK,
            mapOf(ThemePalette.KEY_ACCENT to "#010203"),
            ThemePalette.forMode(ThemePalette.MODE_DARK)
        )
        repository.scale = ChatScale.MODE_ULTRA_COMPACT

        viewModel.refresh()

        assertEquals(ThemePalette.MODE_DARK, viewModel.state.value.themeMode)
        assertEquals(ChatScale.MODE_ULTRA_COMPACT, viewModel.state.value.chatScaleMode)
        assertEquals("#010203", viewModel.state.value.draftColors[ThemePalette.KEY_ACCENT])
    }

    private class FakeRepository(
        var state: ThemeSettingsState = ThemeSettingsState(
            ThemePalette.MODE_SYSTEM,
            ThemePalette.MODE_DARK,
            emptyMap(),
            ThemePalette.forMode(ThemePalette.MODE_DARK)
        ),
        var scale: String = ChatScale.MODE_NORMAL
    ) : ThemeSettingsRepository {
        val themeWrites = mutableListOf<String>()
        val scaleWrites = mutableListOf<String>()
        val colorWrites = mutableListOf<Map<String, String>>()

        override fun themeSettings(): ThemeSettingsState = state

        override fun chatScaleMode(): String = scale

        override fun setThemeMode(mode: String) {
            themeWrites += mode
        }

        override fun saveCustomColors(colors: Map<String, String>) {
            colorWrites += LinkedHashMap(colors)
        }

        override fun setChatScaleMode(mode: String) {
            scaleWrites += mode
        }
    }
}
