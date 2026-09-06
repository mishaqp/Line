package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.ChatScale
import cn.lineai.model.ThemePalette
import cn.lineai.model.ThemeSettingsState
import java.util.LinkedHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ThemeSettingsRepository {
    fun themeSettings(): ThemeSettingsState?
    fun chatScaleMode(): String?
    fun setThemeMode(mode: String)
    fun saveCustomColors(colors: Map<String, String>)
    fun setChatScaleMode(mode: String)
}

data class ThemeSettingsUiState(
    val themeMode: String,
    val chatScaleMode: String,
    val draftColors: Map<String, String>,
    val savedCustomColors: Map<String, String>,
    val activeKey: String = ThemePalette.KEY_ACCENT,
    val activeStarter: String = STARTER_DEFAULT
) {
    val isDraftValid: Boolean
        get() = ThemePalette.EDITABLE_KEYS.all { ThemePalette.isHexColor(draftColors[it]) }

    fun color(key: String): Int = ThemePalette.parseHex(
        draftColors[key],
        ThemePalette.forMode(ThemePalette.MODE_CUSTOM).colorForKey(key)
    )

    companion object {
        const val STARTER_DEFAULT = "default"
        const val STARTER_SAVED = "saved"
        const val STARTER_EDITING = "custom-editing"

        fun from(
            settings: ThemeSettingsState?,
            chatScaleMode: String?
        ): ThemeSettingsUiState {
            val safe = settings ?: ThemeSettingsState(
                ThemePalette.MODE_SYSTEM,
                ThemePalette.MODE_DARK,
                emptyMap(),
                ThemePalette.forMode(ThemePalette.MODE_DARK)
            )
            val saved = LinkedHashMap(safe.customColors)
            return ThemeSettingsUiState(
                themeMode = ThemePalette.normalizeMode(safe.themeMode),
                chatScaleMode = ChatScale.normalizeMode(chatScaleMode),
                draftColors = createDraft(
                    ThemePalette.forMode(ThemePalette.MODE_CUSTOM),
                    saved
                ),
                savedCustomColors = saved,
                activeStarter = if (saved.isEmpty()) STARTER_DEFAULT else STARTER_SAVED
            )
        }

        fun createDraft(
            base: ThemePalette,
            stored: Map<String, String>?
        ): Map<String, String> {
            val values = LinkedHashMap(base.editableHexMap())
            stored?.forEach { (key, value) ->
                if (key in ThemePalette.EDITABLE_KEYS && ThemePalette.isHexColor(value)) {
                    values[key] = value
                }
            }
            return values
        }

        fun starterDraft(
            id: String,
            savedCustomColors: Map<String, String>
        ): Map<String, String> {
            if (id == STARTER_SAVED) {
                return createDraft(
                    ThemePalette.forMode(ThemePalette.MODE_CUSTOM),
                    savedCustomColors
                )
            }
            val palette = when (id) {
                ThemePalette.MODE_LIGHT -> ThemePalette.forMode(ThemePalette.MODE_LIGHT)
                ThemePalette.MODE_DARK -> ThemePalette.forMode(ThemePalette.MODE_DARK)
                ThemePalette.MODE_COFFEE -> ThemePalette.forMode(ThemePalette.MODE_COFFEE)
                ThemePalette.MODE_VSCODE -> ThemePalette.forMode(ThemePalette.MODE_VSCODE)
                ThemePalette.MODE_GITHUB_DARK -> ThemePalette.forMode(ThemePalette.MODE_GITHUB_DARK)
                ThemePalette.MODE_GRUVBOX -> ThemePalette.forMode(ThemePalette.MODE_GRUVBOX)
                ThemePalette.MODE_HIGH_CONTRAST -> ThemePalette.forMode(ThemePalette.MODE_HIGH_CONTRAST)
                else -> ThemePalette.forMode(ThemePalette.MODE_CUSTOM)
            }
            val values = LinkedHashMap(palette.editableHexMap())
            when (id) {
                ThemePalette.MODE_LIGHT -> values[ThemePalette.KEY_CODE_BG] = "#F2F2F7"
                ThemePalette.MODE_DARK -> values[ThemePalette.KEY_CODE_BG] = "#151515"
                ThemePalette.MODE_COFFEE -> values[ThemePalette.KEY_CODE_BG] = "#EFE4D4"
            }
            return values
        }
    }
}

sealed interface ThemeSettingsUiAction {
    data object Back : ThemeSettingsUiAction
    data class SelectThemeMode(val mode: String) : ThemeSettingsUiAction
    data class SelectChatScale(val mode: String) : ThemeSettingsUiAction
    data class SelectStarter(val id: String) : ThemeSettingsUiAction
    data object ResetCustomColors : ThemeSettingsUiAction
    data class SelectColorKey(val key: String) : ThemeSettingsUiAction
    data class EditColor(val key: String, val value: String) : ThemeSettingsUiAction
    data class SelectSwatch(val value: String) : ThemeSettingsUiAction
    data object SaveCustomColors : ThemeSettingsUiAction
}

class ThemeSettingsViewModel(
    private val repository: ThemeSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(
        ThemeSettingsUiState.from(
            repository.themeSettings(),
            repository.chatScaleMode()
        )
    )
    val state: StateFlow<ThemeSettingsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = ThemeSettingsUiState.from(
            repository.themeSettings(),
            repository.chatScaleMode()
        )
    }

    fun onAction(action: ThemeSettingsUiAction) {
        when (action) {
            ThemeSettingsUiAction.Back -> Unit
            is ThemeSettingsUiAction.SelectThemeMode -> {
                val mode = ThemePalette.normalizeMode(action.mode)
                _state.update { it.copy(themeMode = mode) }
                repository.setThemeMode(mode)
            }
            is ThemeSettingsUiAction.SelectChatScale -> {
                val mode = ChatScale.normalizeMode(action.mode)
                _state.update { it.copy(chatScaleMode = mode) }
                repository.setChatScaleMode(mode)
            }
            is ThemeSettingsUiAction.SelectStarter -> {
                _state.update {
                    it.copy(
                        draftColors = ThemeSettingsUiState.starterDraft(
                            action.id,
                            it.savedCustomColors
                        ),
                        activeStarter = action.id
                    )
                }
            }
            ThemeSettingsUiAction.ResetCustomColors -> {
                _state.update {
                    it.copy(
                        draftColors = ThemeSettingsUiState.starterDraft(
                            ThemeSettingsUiState.STARTER_DEFAULT,
                            it.savedCustomColors
                        ),
                        activeStarter = ThemeSettingsUiState.STARTER_DEFAULT
                    )
                }
            }
            is ThemeSettingsUiAction.SelectColorKey -> {
                if (action.key in ThemePalette.EDITABLE_KEYS) {
                    _state.update { it.copy(activeKey = action.key) }
                }
            }
            is ThemeSettingsUiAction.EditColor -> {
                if (action.key !in ThemePalette.EDITABLE_KEYS) return
                val normalized = normalizeColorInput(action.value)
                _state.update {
                    it.copy(
                        draftColors = LinkedHashMap(it.draftColors).apply {
                            put(action.key, normalized)
                        },
                        activeKey = action.key,
                        activeStarter = ThemeSettingsUiState.STARTER_EDITING
                    )
                }
            }
            is ThemeSettingsUiAction.SelectSwatch -> {
                if (!ThemePalette.isHexColor(action.value)) return
                _state.update {
                    it.copy(
                        draftColors = LinkedHashMap(it.draftColors).apply {
                            put(it.activeKey, action.value)
                        },
                        activeStarter = ThemeSettingsUiState.STARTER_EDITING
                    )
                }
            }
            ThemeSettingsUiAction.SaveCustomColors -> {
                val current = _state.value
                if (!current.isDraftValid) return
                val snapshot = LinkedHashMap(current.draftColors)
                _state.update {
                    it.copy(
                        savedCustomColors = snapshot,
                        activeStarter = ThemeSettingsUiState.STARTER_SAVED
                    )
                }
                repository.saveCustomColors(snapshot)
            }
        }
    }

    private fun normalizeColorInput(raw: String): String {
        val trimmed = raw.trim()
        val prefixed = when {
            trimmed.isEmpty() -> ""
            trimmed.startsWith("#") -> trimmed
            else -> "#$trimmed"
        }
        return prefixed.take(9)
    }

    companion object {
        fun factory(repository: ThemeSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ThemeSettingsViewModel(repository) as T
                }
            }
    }
}
