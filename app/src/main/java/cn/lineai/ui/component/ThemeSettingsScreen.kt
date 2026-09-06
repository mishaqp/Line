package cn.lineai.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.model.ChatScale
import cn.lineai.model.ThemePalette
import cn.lineai.ui.model.ThemeSettingsUiAction
import cn.lineai.ui.model.ThemeSettingsUiState
import cn.lineai.ui.theme.LineTheme

private data class ThemeChoice(
    val mode: String,
    val title: Int,
    val description: Int,
    val glyph: String
)

private data class ColorChoice(
    val key: String,
    val title: Int,
    val description: Int
)

private val themeChoices = listOf(
    ThemeChoice(ThemePalette.MODE_SYSTEM, R.string.screen_theme_system, R.string.screen_theme_system_desc, "◐"),
    ThemeChoice(ThemePalette.MODE_LIGHT, R.string.screen_theme_light, R.string.screen_theme_light_desc, "☀"),
    ThemeChoice(ThemePalette.MODE_DARK, R.string.screen_theme_dark, R.string.screen_theme_dark_desc, "☾"),
    ThemeChoice(ThemePalette.MODE_COFFEE, R.string.screen_theme_coffee, R.string.screen_theme_coffee_desc, "☕"),
    ThemeChoice(ThemePalette.MODE_VSCODE, R.string.screen_theme_vscode, R.string.screen_theme_vscode_desc, "<>"),
    ThemeChoice(ThemePalette.MODE_GITHUB_DARK, R.string.screen_theme_github_dark, R.string.screen_theme_github_dark_desc, "GH"),
    ThemeChoice(ThemePalette.MODE_GRUVBOX, R.string.screen_theme_gruvbox, R.string.screen_theme_gruvbox_desc, "GB"),
    ThemeChoice(ThemePalette.MODE_HIGH_CONTRAST, R.string.screen_theme_high_contrast, R.string.screen_theme_high_contrast_desc, "◩"),
    ThemeChoice(ThemePalette.MODE_DYNAMIC_COLOR, R.string.screen_theme_dynamic_color, R.string.screen_theme_dynamic_color_desc, "◈"),
    ThemeChoice(ThemePalette.MODE_CUSTOM, R.string.screen_theme_custom, R.string.screen_theme_custom_desc, "✎")
)

private val scaleChoices = listOf(
    ThemeChoice(ChatScale.MODE_ULTRA_COMPACT, R.string.screen_theme_scale_ultra_compact, R.string.screen_theme_scale_ultra_compact_desc, "·"),
    ThemeChoice(ChatScale.MODE_COMPACT, R.string.screen_theme_scale_compact, R.string.screen_theme_scale_compact_desc, "▪"),
    ThemeChoice(ChatScale.MODE_NORMAL, R.string.screen_theme_scale_normal, R.string.screen_theme_scale_normal_desc, "▣"),
    ThemeChoice(ChatScale.MODE_LARGE, R.string.screen_theme_scale_large, R.string.screen_theme_scale_large_desc, "▰")
)

private val colorChoices = listOf(
    ColorChoice(ThemePalette.KEY_BG, R.string.screen_theme_color_background, R.string.screen_theme_color_background_desc),
    ColorChoice(ThemePalette.KEY_SURFACE, R.string.screen_theme_color_surface, R.string.screen_theme_color_surface_desc),
    ColorChoice(ThemePalette.KEY_SURFACE_ELEVATED, R.string.screen_theme_color_panel, R.string.screen_theme_color_panel_desc),
    ColorChoice(ThemePalette.KEY_SURFACE_LIGHT, R.string.screen_theme_color_panel_light, R.string.screen_theme_color_panel_light_desc),
    ColorChoice(ThemePalette.KEY_INPUT_BG, R.string.screen_theme_color_input, R.string.screen_theme_color_input_desc),
    ColorChoice(ThemePalette.KEY_TEXT, R.string.screen_theme_color_text, R.string.screen_theme_color_text_desc),
    ColorChoice(ThemePalette.KEY_TEXT_SECONDARY, R.string.screen_theme_color_text_secondary, R.string.screen_theme_color_text_secondary_desc),
    ColorChoice(ThemePalette.KEY_TEXT_TERTIARY, R.string.screen_theme_color_text_tertiary, R.string.screen_theme_color_text_tertiary_desc),
    ColorChoice(ThemePalette.KEY_TEXT_ON_COLOR, R.string.screen_theme_color_text_on_color, R.string.screen_theme_color_text_on_color_desc),
    ColorChoice(ThemePalette.KEY_ACCENT, R.string.screen_theme_color_accent, R.string.screen_theme_color_accent_desc),
    ColorChoice(ThemePalette.KEY_USER_BUBBLE, R.string.screen_theme_color_user_bubble, R.string.screen_theme_color_user_bubble_desc),
    ColorChoice(ThemePalette.KEY_AI_BUBBLE, R.string.screen_theme_color_ai_bubble, R.string.screen_theme_color_ai_bubble_desc),
    ColorChoice(ThemePalette.KEY_BORDER, R.string.screen_theme_color_border, R.string.screen_theme_color_border_desc),
    ColorChoice(ThemePalette.KEY_BORDER_LIGHT, R.string.screen_theme_color_border_light, R.string.screen_theme_color_border_light_desc),
    ColorChoice(ThemePalette.KEY_CODE_BG, R.string.screen_theme_color_code_background, R.string.screen_theme_color_code_background_desc),
    ColorChoice(ThemePalette.KEY_CODE_BORDER, R.string.screen_theme_color_code_border, R.string.screen_theme_color_code_border_desc),
    ColorChoice(ThemePalette.KEY_DANGER, R.string.screen_theme_color_danger, R.string.screen_theme_color_danger_desc),
    ColorChoice(ThemePalette.KEY_WARNING, R.string.screen_theme_color_warning, R.string.screen_theme_color_warning_desc),
    ColorChoice(ThemePalette.KEY_SUCCESS, R.string.screen_theme_color_success, R.string.screen_theme_color_success_desc)
)

private val swatches = listOf(
    "#F4EFE6", "#FBF7EF", "#EEE5D8", "#E7DCCA",
    "#2B2118", "#6C5A49", "#9B8976", "#D97757",
    "#B86F50", "#EFE4D4", "#DDD0BF", "#6A7F46",
    "#0A0A0A", "#1C1C1E", "#FFFFFF", "#0A84FF",
    "#1E1E1E", "#252526", "#007ACC", "#D4D4D4",
    "#0D1117", "#161B22", "#2F81F7", "#E6EDF3",
    "#282828", "#FABD2F", "#EBDBB2", "#458588",
    "#64D2FF", "#FFD60A", "#30D158", "#FF453A"
)

@Composable
internal fun ThemeSettingsScreenContent(
    state: ThemeSettingsUiState,
    onAction: (ThemeSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_theme_section_themes,
            onBack = { onAction(ThemeSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_theme_section_themes) {
                themeChoices.forEachIndexed { index, choice ->
                    SettingsChoiceRow(
                        glyph = choice.glyph,
                        titleRes = choice.title,
                        descRes = choice.description,
                        selected = state.themeMode == choice.mode,
                        onClick = {
                            onAction(ThemeSettingsUiAction.SelectThemeMode(choice.mode))
                        }
                    )
                    if (index < themeChoices.lastIndex) SettingsGroupDivider()
                }
            }

            SettingsGroup(R.string.screen_theme_section_chat_scale) {
                scaleChoices.forEachIndexed { index, choice ->
                    SettingsChoiceRow(
                        glyph = choice.glyph,
                        titleRes = choice.title,
                        descRes = choice.description,
                        selected = state.chatScaleMode == choice.mode,
                        onClick = {
                            onAction(ThemeSettingsUiAction.SelectChatScale(choice.mode))
                        }
                    )
                    if (index < scaleChoices.lastIndex) SettingsGroupDivider()
                }
            }

            CustomColorsHeader(
                canSave = state.isDraftValid,
                onReset = { onAction(ThemeSettingsUiAction.ResetCustomColors) },
                onSave = { onAction(ThemeSettingsUiAction.SaveCustomColors) }
            )
            StarterPanel(state, onAction)
            PreviewPanel(state)
            SwatchPanel(state, onAction)
            ColorEditor(state, onAction)
        }
    }
}

@Composable
private fun CustomColorsHeader(
    canSave: Boolean,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.screen_theme_custom_colors).uppercase(),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onReset) {
            Text("↺", fontSize = 20.sp)
        }
        Button(
            onClick = onSave,
            enabled = canSave,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(LineTheme.ACCENT),
                contentColor = Color(LineTheme.TEXT_ON_COLOR),
                disabledContainerColor = Color(LineTheme.SURFACE_LIGHT),
                disabledContentColor = Color(LineTheme.TEXT_TERTIARY)
            )
        ) {
            Text(stringResource(R.string.screen_theme_color_save))
        }
    }
}

private data class StarterChoice(val id: String, val title: Int, val glyph: String)

@Composable
private fun StarterPanel(
    state: ThemeSettingsUiState,
    onAction: (ThemeSettingsUiAction) -> Unit
) {
    val starters = buildList {
        add(StarterChoice(ThemeSettingsUiState.STARTER_DEFAULT, R.string.screen_theme_starter_default, "✎"))
        add(StarterChoice(ThemePalette.MODE_LIGHT, R.string.screen_theme_starter_light, "☀"))
        add(StarterChoice(ThemePalette.MODE_DARK, R.string.screen_theme_starter_dark, "☾"))
        add(StarterChoice(ThemePalette.MODE_COFFEE, R.string.screen_theme_starter_coffee, "☕"))
        add(StarterChoice(ThemePalette.MODE_VSCODE, R.string.screen_theme_starter_vscode, "<>"))
        add(StarterChoice(ThemePalette.MODE_GITHUB_DARK, R.string.screen_theme_starter_github, "GH"))
        add(StarterChoice(ThemePalette.MODE_GRUVBOX, R.string.screen_theme_starter_gruvbox, "GB"))
        add(StarterChoice(ThemePalette.MODE_HIGH_CONTRAST, R.string.screen_theme_starter_high_contrast, "◩"))
        if (state.savedCustomColors.isNotEmpty()) {
            add(StarterChoice(ThemeSettingsUiState.STARTER_SAVED, R.string.screen_theme_starter_saved, "✓"))
        }
    }
    SettingsNamedGroup(stringResource(R.string.screen_theme_starter_section)) {
        Column(Modifier.padding(8.dp)) {
            starters.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { starter ->
                        StarterCard(
                            modifier = Modifier.weight(1f),
                            choice = starter,
                            state = state,
                            onClick = {
                                onAction(ThemeSettingsUiAction.SelectStarter(starter.id))
                            }
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StarterCard(
    modifier: Modifier,
    choice: StarterChoice,
    state: ThemeSettingsUiState,
    onClick: () -> Unit
) {
    val selected = state.activeStarter == choice.id
    val preview = ThemeSettingsUiState.starterDraft(choice.id, state.savedCustomColors)
    val fallback = ThemePalette.forMode(ThemePalette.MODE_CUSTOM)
    val previewColors = listOf(
        ThemePalette.parseHex(preview[ThemePalette.KEY_BG], fallback.bg),
        ThemePalette.parseHex(preview[ThemePalette.KEY_AI_BUBBLE], fallback.aiBubble),
        ThemePalette.parseHex(preview[ThemePalette.KEY_ACCENT], fallback.accent)
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(Color(if (selected) LineTheme.ACCENT_MUTED else LineTheme.SURFACE))
            .border(
                BorderStroke(
                    1.dp,
                    Color(if (selected) LineTheme.ACCENT else LineTheme.BORDER_LIGHT)
                ),
                RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .clickable(onClick = onClick)
            .padding(9.dp)
    ) {
        Row {
            previewColors.forEachIndexed { index, color ->
                Box(
                    Modifier
                        .size(18.dp)
                        .then(if (index > 0) Modifier.padding(start = 1.dp) else Modifier)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(color))
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = choice.glyph,
            color = Color(if (selected) LineTheme.ACCENT else LineTheme.TEXT_SECONDARY),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(choice.title),
            color = Color(if (selected) LineTheme.ACCENT else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PreviewPanel(state: ThemeSettingsUiState) {
    SettingsNamedGroup(stringResource(R.string.screen_theme_section_preview)) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
                .background(Color(state.color(ThemePalette.KEY_BG)))
                .border(
                    1.dp,
                    Color(state.color(ThemePalette.KEY_BORDER)),
                    RoundedCornerShape(LineTheme.SHAPE_MD.dp)
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                    .background(Color(state.color(ThemePalette.KEY_AI_BUBBLE)))
                    .padding(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.screen_theme_section_preview),
                    color = Color(state.color(ThemePalette.KEY_TEXT)),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.screen_theme_section_preview_desc),
                    color = Color(state.color(ThemePalette.KEY_TEXT_SECONDARY)),
                    fontSize = LineTheme.FONT_SM.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.screen_theme_color_accent),
                color = Color(ThemePalette.forMode(ThemePalette.MODE_CUSTOM).textOnColor),
                fontSize = LineTheme.FONT_XS.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(state.color(ThemePalette.KEY_ACCENT)))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun SwatchPanel(
    state: ThemeSettingsUiState,
    onAction: (ThemeSettingsUiAction) -> Unit
) {
    val activeLabel = colorChoices.firstOrNull { it.key == state.activeKey }?.title
        ?: R.string.screen_theme_color_accent
    SettingsNamedGroup(
        stringResource(
            R.string.theme_current_editing,
            stringResource(activeLabel)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            swatches.chunked(7).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    rowItems.forEach { value ->
                        val selected = value.equals(
                            state.draftColors[state.activeKey],
                            ignoreCase = true
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                                .background(Color(ThemePalette.parseHex(value, LineTheme.SURFACE_LIGHT)))
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    Color(if (selected) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                                    RoundedCornerShape(LineTheme.SHAPE_LG.dp)
                                )
                                .clickable {
                                    onAction(ThemeSettingsUiAction.SelectSwatch(value))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Text(
                                    "✓",
                                    color = Color(LineTheme.TEXT_ON_COLOR),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            }
        }
    }
}

@Composable
private fun ColorEditor(
    state: ThemeSettingsUiState,
    onAction: (ThemeSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
    ) {
        colorChoices.forEachIndexed { index, field ->
            ColorEditorRow(
                field = field,
                value = state.draftColors[field.key].orEmpty(),
                selected = state.activeKey == field.key,
                onSelect = {
                    onAction(ThemeSettingsUiAction.SelectColorKey(field.key))
                },
                onValueChange = {
                    onAction(ThemeSettingsUiAction.EditColor(field.key, it))
                }
            )
            if (index < colorChoices.lastIndex) SettingsGroupDivider()
        }
    }
}

@Composable
private fun ColorEditorRow(
    field: ColorChoice,
    value: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onValueChange: (String) -> Unit
) {
    val valid = ThemePalette.isHexColor(value)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(if (selected) LineTheme.ACCENT_MUTED else 0x00000000))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                .background(
                    Color(
                        ThemePalette.parseHex(
                            value,
                            LineTheme.SURFACE_LIGHT
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(LineTheme.BORDER_LIGHT),
                    RoundedCornerShape(LineTheme.SHAPE_LG.dp)
                )
                .clickable(onClick = onSelect)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelect)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = stringResource(field.title),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(
                    if (valid) field.description else R.string.screen_theme_color_hex_hint
                ),
                color = Color(if (valid) LineTheme.TEXT_TERTIARY else LineTheme.DANGER),
                fontSize = LineTheme.FONT_XS.sp
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(9)) },
            modifier = Modifier.width(112.dp),
            singleLine = true,
            isError = !valid,
            placeholder = {
                Text(stringResource(R.string.screen_theme_color_hex_placeholder))
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = LineTheme.FONT_SM.sp,
                color = Color(if (valid) LineTheme.TEXT else LineTheme.DANGER)
            )
        )
    }
}
