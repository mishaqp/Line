package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.lineai.R
import cn.lineai.model.InputSettings
import cn.lineai.ui.model.InputSettingsUiAction
import cn.lineai.ui.model.InputSettingsUiState
import cn.lineai.ui.theme.LineTheme

private const val GLYPH_SEND = "\u21a9"
private const val GLYPH_NEWLINE = "\u21b5"

@Composable
internal fun InputSettingsScreenContent(
    state: InputSettingsUiState,
    onAction: (InputSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_input_title,
            onBack = { onAction(InputSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_input_section_input) {
                SettingsChoiceRow(
                    glyph = GLYPH_SEND,
                    titleRes = R.string.screen_input_enter_send,
                    descRes = R.string.screen_input_enter_behavior_desc,
                    selected = state.enterKeyBehavior == InputSettings.ENTER_SEND,
                    onClick = {
                        onAction(InputSettingsUiAction.SetEnterKeyBehavior(InputSettings.ENTER_SEND))
                    }
                )
                SettingsGroupDivider()
                SettingsChoiceRow(
                    glyph = GLYPH_NEWLINE,
                    titleRes = R.string.screen_input_enter_newline,
                    descRes = R.string.screen_input_enter_behavior_label,
                    selected = state.enterKeyBehavior == InputSettings.ENTER_NEWLINE,
                    onClick = {
                        onAction(
                            InputSettingsUiAction.SetEnterKeyBehavior(InputSettings.ENTER_NEWLINE)
                        )
                    }
                )
            }
        }
    }
}
