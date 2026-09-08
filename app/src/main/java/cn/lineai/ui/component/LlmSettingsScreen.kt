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
import cn.lineai.model.AiBehaviorSettings
import cn.lineai.ui.model.LlmSettingsUiAction
import cn.lineai.ui.model.LlmSettingsUiState
import cn.lineai.ui.theme.LineTheme

private const val GLYPH_SPARKLES = "\u2728"
private const val GLYPH_BRAIN = "\u25c9"
private const val GLYPH_ROTATE = "\u21ba"
private const val GLYPH_ZAP = "\u26a1"
private const val GLYPH_SMILE = "\u263a"
private const val GLYPH_FILE = "\u270e"
private const val GLYPH_SCROLL = "\u2630"
private const val GLYPH_EXPAND = "\u2922"

@Composable
internal fun LlmSettingsScreenContent(
    state: LlmSettingsUiState,
    onAction: (LlmSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_llm_title,
            onBack = { onAction(LlmSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_llm_section_thinking) {
                ReasoningRow(
                    state,
                    AiBehaviorSettings.REASONING_OFF,
                    R.string.screen_llm_thinking_off_label,
                    R.string.screen_llm_thinking_off_desc,
                    onAction,
                    divider = true
                )
                ReasoningRow(
                    state,
                    AiBehaviorSettings.REASONING_AUTO,
                    R.string.screen_llm_thinking_auto_label,
                    R.string.screen_llm_thinking_auto,
                    onAction,
                    divider = true
                )
                ReasoningRow(
                    state,
                    AiBehaviorSettings.REASONING_LOW,
                    R.string.screen_llm_thinking_low_label,
                    R.string.screen_llm_thinking_low,
                    onAction,
                    divider = true
                )
                ReasoningRow(
                    state,
                    AiBehaviorSettings.REASONING_MEDIUM,
                    R.string.screen_llm_thinking_medium_label,
                    R.string.screen_llm_thinking_medium,
                    onAction,
                    divider = true
                )
                ReasoningRow(
                    state,
                    AiBehaviorSettings.REASONING_HIGH,
                    R.string.screen_llm_thinking_high_label,
                    R.string.screen_llm_thinking_high,
                    onAction,
                    divider = true
                )
                ReasoningRow(
                    state,
                    AiBehaviorSettings.REASONING_MAX,
                    R.string.screen_llm_thinking_max_label,
                    R.string.screen_llm_thinking_max,
                    onAction,
                    divider = false
                )
            }

            SettingsGroup(R.string.screen_llm_section_learning) {
                SettingsToggleRow(
                    glyph = GLYPH_BRAIN,
                    titleRes = R.string.screen_llm_learning_label,
                    descRes = R.string.screen_llm_learning_desc,
                    checked = state.learningModeEnabled,
                    onCheckedChange = { onAction(LlmSettingsUiAction.SetLearningMode(it)) }
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    glyph = GLYPH_ROTATE,
                    titleRes = R.string.screen_llm_soft_compact_label,
                    descRes = R.string.screen_llm_soft_compact_desc,
                    checked = state.softCompactionEnabled,
                    onCheckedChange = { onAction(LlmSettingsUiAction.SetSoftCompaction(it)) }
                )
            }

            SettingsGroup(R.string.screen_llm_section_tone) {
                SettingsChoiceRow(
                    glyph = GLYPH_ZAP,
                    titleRes = R.string.screen_llm_tone_coding,
                    descRes = R.string.screen_llm_tone_coding_desc,
                    selected = state.toneMode == AiBehaviorSettings.TONE_CODING,
                    onClick = {
                        onAction(LlmSettingsUiAction.SetToneMode(AiBehaviorSettings.TONE_CODING))
                    }
                )
                SettingsGroupDivider()
                SettingsChoiceRow(
                    glyph = GLYPH_SMILE,
                    titleRes = R.string.screen_llm_tone_chat,
                    descRes = R.string.screen_llm_tone_chat_desc,
                    selected = state.toneMode == AiBehaviorSettings.TONE_CHAT,
                    onClick = {
                        onAction(LlmSettingsUiAction.SetToneMode(AiBehaviorSettings.TONE_CHAT))
                    }
                )
            }

            SettingsGroup(R.string.screen_llm_section_prompts) {
                SettingsNavRow(
                    glyph = GLYPH_FILE,
                    titleRes = R.string.screen_llm_prompts_label,
                    descRes = R.string.screen_llm_prompts_desc,
                    onClick = { onAction(LlmSettingsUiAction.OpenPromptTemplates) }
                )
            }

            SettingsGroup(R.string.screen_llm_section_thinking_display) {
                SettingsToggleRow(
                    glyph = GLYPH_SCROLL,
                    titleRes = R.string.screen_llm_scroll_label,
                    descRes = R.string.screen_llm_scroll_desc,
                    checked = state.thinkingScrollEnabled,
                    onCheckedChange = { onAction(LlmSettingsUiAction.SetThinkingScroll(it)) }
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    glyph = GLYPH_EXPAND,
                    titleRes = R.string.screen_llm_auto_expand_label,
                    descRes = R.string.screen_llm_auto_expand_desc,
                    checked = state.thinkingAutoExpandEnabled,
                    onCheckedChange = { onAction(LlmSettingsUiAction.SetThinkingAutoExpand(it)) }
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    glyph = GLYPH_BRAIN,
                    titleRes = R.string.screen_llm_keep_reasoning_label,
                    descRes = R.string.screen_llm_keep_reasoning_desc,
                    checked = state.preserveReasoningEnabled,
                    onCheckedChange = { onAction(LlmSettingsUiAction.SetPreserveReasoning(it)) }
                )
            }
        }
    }
}

@Composable
private fun ReasoningRow(
    state: LlmSettingsUiState,
    effort: String,
    titleRes: Int,
    descRes: Int,
    onAction: (LlmSettingsUiAction) -> Unit,
    divider: Boolean
) {
    SettingsChoiceRow(
        glyph = GLYPH_SPARKLES,
        titleRes = titleRes,
        descRes = descRes,
        selected = state.reasoningEffort == effort,
        onClick = { onAction(LlmSettingsUiAction.SetReasoningEffort(effort)) }
    )
    if (divider) {
        SettingsGroupDivider()
    }
}
