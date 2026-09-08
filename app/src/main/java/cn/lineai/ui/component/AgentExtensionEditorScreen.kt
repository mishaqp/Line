package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import cn.lineai.R
import cn.lineai.ui.model.AgentExtensionEditorUiAction
import cn.lineai.ui.model.AgentExtensionEditorUiState
import cn.lineai.ui.model.AgentMcpOption
import cn.lineai.ui.model.AgentToolOption
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
fun AgentExtensionEditorScreen(
    state: AgentExtensionEditorUiState,
    onAction: (AgentExtensionEditorUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
            .navigationBarsPadding()
            .imePadding()
    ) {
        AgentEditorHeader(
            busy = state.isBusy,
            saving = state.isSaving,
            onBack = { onAction(AgentExtensionEditorUiAction.Back) },
            onSave = { onAction(AgentExtensionEditorUiAction.Save) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "quick-title") {
                AgentEditorSectionTitle(stringResource(R.string.screen_agent_quick_create))
            }
            item(key = "quick-create") {
                AgentQuickCreateRow(
                    enabled = !state.isBusy,
                    onClick = { onAction(AgentExtensionEditorUiAction.OpenAiDialog) }
                )
            }

            item(key = "basic-title") {
                AgentEditorSectionTitle(stringResource(R.string.screen_agent_form_basic))
            }
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = {
                        onAction(AgentExtensionEditorUiAction.SetName(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    label = { Text(stringResource(R.string.screen_agent_field_name)) },
                    placeholder = { Text(stringResource(R.string.screen_agent_hint_name)) },
                    singleLine = true
                )
            }
            item(key = "slug") {
                OutlinedTextField(
                    value = state.slug,
                    onValueChange = {
                        onAction(AgentExtensionEditorUiAction.SetSlug(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    label = { Text(stringResource(R.string.screen_agent_field_identifier)) },
                    placeholder = { Text(stringResource(R.string.screen_agent_hint_slug)) },
                    supportingText = { Text(stringResource(R.string.screen_agent_helper_slug)) },
                    singleLine = true
                )
            }

            item(key = "behavior-title") {
                AgentEditorSectionTitle(stringResource(R.string.screen_agent_form_behavior))
            }
            item(key = "prompt") {
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = {
                        onAction(AgentExtensionEditorUiAction.SetPrompt(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    label = { Text(stringResource(R.string.screen_agent_field_prompt)) },
                    placeholder = { Text(stringResource(R.string.screen_agent_hint_prompt)) },
                    minLines = 5
                )
            }
            item(key = "trigger") {
                OutlinedTextField(
                    value = state.trigger,
                    onValueChange = {
                        onAction(AgentExtensionEditorUiAction.SetTrigger(it))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy,
                    label = { Text(stringResource(R.string.screen_agent_field_trigger)) },
                    placeholder = { Text(stringResource(R.string.screen_agent_hint_trigger)) },
                    minLines = 5
                )
            }

            item(key = "tools-title") {
                AgentEditorSectionTitle(
                    stringResource(
                        R.string.screen_agent_tools_count,
                        stringResource(R.string.screen_agent_section_tools),
                        stringResource(R.string.screen_agent_tools_selected),
                        state.selectedToolCount
                    )
                )
            }
            if (state.tools.isEmpty()) {
                item(key = "tools-empty") {
                    AgentEditorEmpty(stringResource(R.string.screen_agent_tools_empty))
                }
            } else {
                itemsIndexed(
                    items = state.tools,
                    key = { index, tool -> "tool:" + index + ":" + tool.name }
                ) { _, tool ->
                    AgentToolRow(
                        option = tool,
                        active = state.selectedToolNames.contains(tool.name),
                        enabled = !state.isBusy,
                        onClick = {
                            onAction(AgentExtensionEditorUiAction.ToggleTool(tool.name))
                        }
                    )
                }
            }

            item(key = "mcps-title") {
                AgentEditorSectionTitle(
                    stringResource(
                        R.string.screen_agent_tools_count,
                        stringResource(R.string.screen_agent_section_mcps),
                        stringResource(R.string.screen_agent_tools_selected),
                        state.selectedMcpCount
                    )
                )
            }
            if (state.mcps.isEmpty()) {
                item(key = "mcps-empty") {
                    AgentEditorEmpty(stringResource(R.string.screen_agent_mcps_empty))
                }
            } else {
                itemsIndexed(
                    items = state.mcps,
                    key = { index, mcp -> "mcp:" + index + ":" + mcp.id }
                ) { _, mcp ->
                    AgentMcpRow(
                        option = mcp,
                        active = state.selectedMcpIds.contains(mcp.id),
                        enabled = !state.isBusy,
                        onClick = {
                            onAction(AgentExtensionEditorUiAction.ToggleMcp(mcp.id))
                        }
                    )
                }
            }

            item(key = "bottom-space") {
                Spacer(Modifier.height(90.dp))
            }
        }
    }

    if (state.showAiDialog) {
        AgentAiWriterDialog(state = state, onAction = onAction)
    }
}

@Composable
private fun AgentEditorHeader(
    busy: Boolean,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = !busy) {
                Text("‹")
            }
            Text(
                text = stringResource(R.string.screen_agent_add_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                TextButton(onClick = onSave, enabled = !busy) {
                    Text(stringResource(R.string.screen_agent_save))
                }
            }
        }
    }
}

@Composable
private fun AgentQuickCreateRow(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                    .background(Color(LineTheme.ACCENT_MUTED)),
                contentAlignment = Alignment.Center
            ) {
                AgentEditorIcon(
                    iconType = IconButtonView.SPARKLES,
                    color = LineTheme.ACCENT,
                    size = 20
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.screen_agent_let_ai_write),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.screen_agent_let_ai_write_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AgentToolRow(
    option: AgentToolOption,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AgentOptionRow(
        iconType = IconButtonView.SETTINGS,
        label = option.name,
        description = option.description,
        active = active,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun AgentMcpRow(
    option: AgentMcpOption,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AgentOptionRow(
        iconType = IconButtonView.MCP,
        label = option.label,
        description = option.description.ifBlank {
            stringResource(R.string.screen_agent_join_empty)
        },
        active = active,
        enabled = enabled,
        onClick = onClick
    )
}

@Composable
private fun AgentOptionRow(
    iconType: Int,
    label: String,
    description: String,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (active) Color(LineTheme.ACCENT) else Color(LineTheme.TEXT)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(LineTheme.BORDER_LIGHT),
                shape = MaterialTheme.shapes.medium
            )
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (active) Color(LineTheme.ACCENT_MUTED) else Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AgentEditorIcon(
                iconType = iconType,
                color = if (active) LineTheme.ACCENT else LineTheme.TEXT_SECONDARY,
                size = 20
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = label,
                    color = contentColor,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (description.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = Color(LineTheme.TEXT_TERTIARY),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentEditorSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun AgentEditorEmpty(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(LineTheme.BORDER_LIGHT),
                shape = MaterialTheme.shapes.medium
            ),
        shape = MaterialTheme.shapes.medium,
        color = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(20.dp),
            color = Color(LineTheme.TEXT_TERTIARY),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AgentAiWriterDialog(
    state: AgentExtensionEditorUiState,
    onAction: (AgentExtensionEditorUiAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isGenerating) {
                onAction(AgentExtensionEditorUiAction.DismissAiDialog)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.screen_agent_let_ai_dialog_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.screen_agent_ai_dialog_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        onAction(AgentExtensionEditorUiAction.DismissAiDialog)
                    },
                    enabled = !state.isGenerating
                ) {
                    Text("×")
                }
            }
        },
        text = {
            OutlinedTextField(
                value = state.aiDescription,
                onValueChange = {
                    onAction(AgentExtensionEditorUiAction.SetAiDescription(it))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isGenerating,
                placeholder = { Text(stringResource(R.string.screen_agent_ai_hint)) },
                minLines = 5
            )
        },
        confirmButton = {
            Button(
                onClick = { onAction(AgentExtensionEditorUiAction.GenerateDraft) },
                enabled = !state.isGenerating
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    AgentEditorIcon(
                        iconType = IconButtonView.SPARKLES,
                        color = LineTheme.TEXT_ON_COLOR,
                        size = 18
                    )
                    Text(
                        text = stringResource(R.string.screen_agent_let_ai_button),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !state.isGenerating,
            dismissOnClickOutside = !state.isGenerating
        )
    )
}

@Composable
private fun AgentEditorIcon(
    iconType: Int,
    color: Int,
    size: Int
) {
    AndroidView(
        modifier = Modifier.size(size.dp),
        factory = { context ->
            IconButtonView(context, iconType).apply {
                setIconSizeDp(size, size)
                isClickable = false
            }
        },
        update = { view ->
            view.setIconColor(color)
        }
    )
}
