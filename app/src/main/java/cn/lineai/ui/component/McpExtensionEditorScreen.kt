package cn.lineai.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.lineai.R
import cn.lineai.ui.model.McpExtensionEditorUiAction
import cn.lineai.ui.model.McpExtensionEditorUiState
import cn.lineai.ui.model.McpQueryStatus

@Composable
fun McpExtensionEditorScreen(
    state: McpExtensionEditorUiState,
    onAction: (McpExtensionEditorUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        EditorHeader(
            saving = state.isSaving,
            querying = state.isQuerying,
            onBack = { onAction(McpExtensionEditorUiAction.Back) },
            onSave = { onAction(McpExtensionEditorUiAction.Save) }
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "form-title") {
                SectionTitle(stringResource(R.string.screen_mcp_form_title))
            }
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onAction(McpExtensionEditorUiAction.SetName(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.screen_mcp_field_name)) },
                    placeholder = { Text(stringResource(R.string.screen_mcp_hint_name)) },
                    singleLine = true
                )
            }
            item(key = "url") {
                OutlinedTextField(
                    value = state.url,
                    onValueChange = { onAction(McpExtensionEditorUiAction.SetUrl(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.screen_mcp_field_http_url)) },
                    placeholder = { Text(stringResource(R.string.screen_mcp_hint_url)) },
                    supportingText = { Text(stringResource(R.string.screen_mcp_helper_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
            }

            item(key = "headers-title") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SectionTitle(stringResource(R.string.screen_mcp_section_headers))
                    TextButton(onClick = { onAction(McpExtensionEditorUiAction.AddHeader) }) {
                        Text(stringResource(R.string.screen_mcp_add_header))
                    }
                }
            }
            items(state.headers, key = { header -> "header:${header.key}" }) { header ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = header.name,
                            onValueChange = {
                                onAction(McpExtensionEditorUiAction.SetHeaderName(header.key, it))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.screen_extension_detail_field_name)) },
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = header.value,
                                onValueChange = {
                                    onAction(McpExtensionEditorUiAction.SetHeaderValue(header.key, it))
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.screen_mcp_header_value_hint)) },
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { onAction(McpExtensionEditorUiAction.RemoveHeader(header.key)) }
                            ) {
                                Text("×")
                            }
                        }
                    }
                }
            }

            item(key = "query-title") {
                SectionTitle(stringResource(R.string.screen_mcp_query_section_title))
            }
            item(key = "query-action") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.screen_mcp_query_tools),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = queryDescription(state),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            if (state.isQuerying) {
                                CircularProgressIndicator()
                            } else {
                                Button(onClick = { onAction(McpExtensionEditorUiAction.QueryTools) }) {
                                    Text(stringResource(R.string.screen_mcp_query_tools))
                                }
                            }
                        }
                        if (state.queryStatus == McpQueryStatus.ERROR && state.queryError.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = state.queryError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item(key = "tools-title") {
                SectionTitle(stringResource(R.string.screen_mcp_tools_count, state.enabledToolCount))
            }
            if (state.tools.isEmpty()) {
                item(key = "tools-empty") {
                    Text(
                        text = when {
                            state.isQuerying -> stringResource(R.string.screen_mcp_query_busy)
                            state.queryStatus == McpQueryStatus.EMPTY -> stringResource(R.string.screen_mcp_query_no_tools)
                            else -> stringResource(R.string.screen_mcp_query_pending)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (!state.toolsMatchCurrentRequest) {
                    item(key = "tools-stale") {
                        Text(
                            text = stringResource(R.string.screen_mcp_query_pending),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                itemsIndexed(
                    items = state.tools,
                    key = { index, tool -> "tool:$index:${tool.name}" }
                ) { index, tool ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = tool.description.ifBlank {
                                        stringResource(R.string.screen_mcp_tool_default_desc)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = tool.isEnabled,
                                onCheckedChange = {
                                    onAction(McpExtensionEditorUiAction.SetToolEnabled(index, it))
                                }
                            )
                        }
                    }
                }
            }
            item(key = "bottom-space") {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun EditorHeader(
    saving: Boolean,
    querying: Boolean,
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
            TextButton(onClick = onBack) {
                Text("‹")
            }
            Text(
                text = stringResource(R.string.screen_mcp_add_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.width(28.dp))
            } else {
                TextButton(onClick = onSave, enabled = !querying) {
                    Text(stringResource(R.string.screen_agent_save))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun queryDescription(state: McpExtensionEditorUiState): String {
    return when {
        state.isQuerying -> stringResource(R.string.screen_mcp_query_busy_desc)
        state.queryStatus == McpQueryStatus.ERROR -> stringResource(R.string.screen_mcp_query_empty_desc)
        state.queryStatus == McpQueryStatus.EMPTY -> stringResource(R.string.screen_mcp_query_no_tools)
        state.toolsMatchCurrentRequest && state.tools.isNotEmpty() ->
            stringResource(R.string.screen_mcp_query_done_desc, state.tools.size)
        else -> stringResource(R.string.screen_mcp_query_empty_desc)
    }
}
