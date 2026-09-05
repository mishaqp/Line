package cn.lineai.ui.component

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.PromptTemplateUi
import cn.lineai.ui.model.PromptTemplatesUiAction
import cn.lineai.ui.model.PromptTemplatesUiState
import cn.lineai.ui.theme.LineTheme

private const val GLYPH_RESET = "\u21ba"
private const val GLYPH_SAVE = "\u2398"

@Composable
internal fun PromptTemplatesScreenContent(
    state: PromptTemplatesUiState,
    onAction: (PromptTemplatesUiAction) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
            .imePadding()
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_prompt_templates_title,
            onBack = { onAction(PromptTemplatesUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_prompt_templates_section) {
                Text(
                    text = introText(state.templates),
                    color = Color(LineTheme.TEXT_SECONDARY),
                    fontSize = LineTheme.FONT_SM.sp,
                    lineHeight = (LineTheme.FONT_SM + 4).sp,
                    modifier = Modifier.padding(LineTheme.LG.dp)
                )
            }
            state.templates.forEach { template ->
                SettingsNamedGroup(template.title) {
                    PromptTemplateEditor(
                        template = template,
                        onDraftChange = { text ->
                            onAction(PromptTemplatesUiAction.UpdateDraft(template.id, text))
                        },
                        onSave = {
                            onAction(PromptTemplatesUiAction.Save(template.id))
                            Toast.makeText(
                                context,
                                context.getString(R.string.screen_prompt_templates_toast_saved),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onReset = {
                            onAction(PromptTemplatesUiAction.Reset(template.id))
                            Toast.makeText(
                                context,
                                context.getString(R.string.screen_prompt_templates_toast_reset),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PromptTemplateEditor(
    template: PromptTemplateUi,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Column(modifier = Modifier.padding(LineTheme.LG.dp)) {
        Text(
            text = template.description,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_SM.sp,
            lineHeight = (LineTheme.FONT_SM + 4).sp
        )
        Text(
            text = stringResource(
                R.string.screen_prompt_templates_source,
                template.sourceLabel,
                variablesText(template.variables)
            ),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp,
            modifier = Modifier.padding(top = LineTheme.SM.dp)
        )
        OutlinedTextField(
            value = template.currentText,
            onValueChange = onDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LineTheme.MD.dp)
                .heightIn(min = 220.dp),
            textStyle = TextStyle(
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_SM.sp,
                fontFamily = FontFamily.Monospace
            ),
            minLines = 8,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Text,
                autoCorrectEnabled = false
            ),
            shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(LineTheme.TEXT),
                unfocusedTextColor = Color(LineTheme.TEXT),
                focusedContainerColor = Color(LineTheme.CODE_BG),
                unfocusedContainerColor = Color(LineTheme.CODE_BG),
                disabledContainerColor = Color(LineTheme.CODE_BG),
                cursorColor = Color(LineTheme.ACCENT),
                focusedBorderColor = Color(LineTheme.CODE_BORDER),
                unfocusedBorderColor = Color(LineTheme.CODE_BORDER)
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = LineTheme.MD.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (template.customized) {
                        R.string.screen_prompt_templates_status_custom
                    } else {
                        R.string.screen_prompt_templates_status_built_in
                    }
                ),
                color = Color(if (template.customized) LineTheme.ACCENT else LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                modifier = Modifier.weight(1f)
            )
            TemplateActionButton(
                glyph = GLYPH_RESET,
                label = stringResource(R.string.common_reset),
                onClick = onReset
            )
            Spacer(Modifier.width(LineTheme.SM.dp))
            TemplateActionButton(
                glyph = GLYPH_SAVE,
                label = stringResource(R.string.common_save),
                onClick = onSave
            )
        }
    }
}

@Composable
private fun TemplateActionButton(
    glyph: String,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .border(
                width = 1.dp,
                color = Color(LineTheme.BORDER_LIGHT),
                shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .background(Color(LineTheme.SURFACE_LIGHT))
            .clickable(onClick = onClick)
            .padding(horizontal = LineTheme.SM.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = glyph,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp
        )
    }
}

@Composable
private fun introText(templates: List<PromptTemplateUi>): String {
    val variablesLabel = stringResource(R.string.screen_prompt_templates_variables)
    val separator = stringResource(R.string.screen_prompt_templates_item_separator)
    val builder = StringBuilder()
    builder.append(variablesLabel).append('\n')
    templates.forEach { item ->
        builder.append("\n- ")
            .append(item.title)
            .append(separator)
            .append(item.description)
        val variables = variablesText(item.variables)
        if (variables.isNotEmpty()) {
            builder.append(stringResource(R.string.screen_prompt_templates_item_variables, variables))
        }
    }
    return builder.toString().trim()
}

private fun variablesText(variables: List<String>): String {
    if (variables.isEmpty()) {
        return ""
    }
    return variables.joinToString(", ") { "{{$it}}" }
}
