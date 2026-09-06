package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.model.MemoryOverviewState
import cn.lineai.ui.model.MemoryDialogState
import cn.lineai.ui.model.MemoryEditableSection
import cn.lineai.ui.model.MemoryUiAction
import cn.lineai.ui.model.MemoryUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun MemorySettingsScreenContent(
    state: MemoryUiState,
    onAction: (MemoryUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        MemoryHeader(state = state, onAction = onAction)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            MemoryProjectHint(state.overview.projectId)
            EditableMemorySection(
                titleRes = R.string.screen_memory_section_long_term,
                iconType = IconButtonView.DATABASE,
                section = MemoryEditableSection.LONG_TERM,
                rows = state.overview.longTerm,
                state = state,
                onAction = onAction
            )
            EditableMemorySection(
                titleRes = R.string.screen_memory_section_project,
                iconType = IconButtonView.FOLDER_OPEN,
                section = MemoryEditableSection.PROJECT,
                rows = state.overview.project,
                state = state,
                onAction = onAction
            )
            EditableMemorySection(
                titleRes = R.string.screen_memory_section_environment,
                iconType = IconButtonView.GLOBE,
                section = MemoryEditableSection.ENVIRONMENT,
                rows = state.overview.environment,
                state = state,
                onAction = onAction
            )
            WorkingMemorySection(
                rows = state.overview.shortTerm,
                onAction = onAction
            )
            HistoryMemorySection(
                rows = state.overview.history,
                onAction = onAction
            )
        }
    }

    when (val dialog = state.dialog) {
        null -> Unit
        is MemoryDialogState.MemoryDetail -> MemoryDetailDialog(dialog, onAction)
        is MemoryDialogState.WorkingDetail -> WorkingMemoryDetailDialog(dialog.memory, onAction)
        is MemoryDialogState.HistoryDetail -> HistoryDetailDialog(dialog.entry, onAction)
        is MemoryDialogState.Actions -> MemoryActionDialog(onAction)
        is MemoryDialogState.Editor -> MemoryEditorDialog(dialog, onAction)
        is MemoryDialogState.DeleteConfirm -> MemoryDeleteDialog(dialog.memory, onAction)
        is MemoryDialogState.BatchDeleteConfirm -> MemoryBatchDeleteDialog(dialog.ids.size, onAction)
    }
}

@Composable
private fun MemoryHeader(
    state: MemoryUiState,
    onAction: (MemoryUiAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isMultiSelect) {
            MemoryIconButton(
                iconType = IconButtonView.CLOSE,
                color = LineTheme.TEXT,
                onClick = { onAction(MemoryUiAction.ExitMultiSelect) }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onAction(MemoryUiAction.Back) },
                contentAlignment = Alignment.Center
            ) {
                Text("\u2039", color = Color(LineTheme.TEXT), fontSize = 22.sp)
            }
        }

        Text(
            text = if (state.isMultiSelect) {
                stringResource(R.string.screen_memory_selected_count, state.selectedIds.size)
            } else {
                stringResource(R.string.screen_memory_title)
            },
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (state.isMultiSelect) {
            MemoryIconButton(
                iconType = IconButtonView.TRASH_2,
                color = LineTheme.DANGER,
                onClick = { onAction(MemoryUiAction.OpenBatchDeleteConfirm) }
            )
        } else {
            MemoryIconButton(
                iconType = IconButtonView.PLUS,
                color = LineTheme.ACCENT,
                onClick = { onAction(MemoryUiAction.OpenAddEditor) }
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER))
    )
}

@Composable
private fun MemoryIconButton(
    iconType: Int,
    color: Int,
    onClick: () -> Unit
) {
    AndroidView(
        factory = { context ->
            IconButtonView(context, iconType).apply {
                setIconColor(color)
                setIconSizeDp(36, 20)
                setOnClickListener { onClick() }
            }
        },
        update = { button ->
            button.setIconColor(color)
            button.setOnClickListener { onClick() }
        },
        modifier = Modifier.size(36.dp)
    )
}

@Composable
private fun MemoryProjectHint(projectId: String) {
    val project = projectId.ifEmpty { stringResource(R.string.screen_memory_project_unselected) }
    Text(
        text = stringResource(R.string.screen_memory_current_project_prefix) + project,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_XS.sp
    )
}

@Composable
private fun EditableMemorySection(
    titleRes: Int,
    iconType: Int,
    section: MemoryEditableSection,
    rows: List<MemoryOverviewState.Memory>,
    state: MemoryUiState,
    onAction: (MemoryUiAction) -> Unit
) {
    val title = stringResource(titleRes) + "（${rows.size}）"
    SettingsNamedGroup(title) {
        if (rows.isEmpty()) {
            MemoryEmptyRow()
        } else {
            rows.forEachIndexed { index, memory ->
                EditableMemoryRow(
                    memory = memory,
                    iconType = iconType,
                    section = section,
                    selected = memory.id in state.selectedIds,
                    isMultiSelect = state.isMultiSelect,
                    onAction = onAction
                )
                if (index < rows.lastIndex) {
                    SettingsGroupDivider()
                }
            }
        }
    }
}

@Composable
private fun EditableMemoryRow(
    memory: MemoryOverviewState.Memory,
    iconType: Int,
    section: MemoryEditableSection,
    selected: Boolean,
    isMultiSelect: Boolean,
    onAction: (MemoryUiAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(if (selected) LineTheme.ACCENT_MUTED else 0x00000000))
            .pointerInput(memory.id, isMultiSelect) {
                detectTapGestures(
                    onTap = { onAction(MemoryUiAction.OpenMemoryDetail(section, memory.id)) },
                    onLongPress = { onAction(MemoryUiAction.OpenActions(memory.id)) }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemoryRowIcon(iconType)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = memoryPreview(memory.content, 80),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = memoryDescription(memory),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!isMultiSelect) {
            Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 18.sp)
        }
    }
}

@Composable
private fun WorkingMemorySection(
    rows: List<MemoryOverviewState.WorkingMemory>,
    onAction: (MemoryUiAction) -> Unit
) {
    val title = stringResource(R.string.screen_memory_section_short_term) + "（${rows.size}）"
    SettingsNamedGroup(title) {
        if (rows.isEmpty()) {
            MemoryEmptyRow()
        } else {
            rows.forEachIndexed { index, memory ->
                ReadOnlyMemoryRow(
                    iconType = IconButtonView.CLOCK_3,
                    title = memoryPreview(memory.content, 80),
                    description = memoryTime(memory.updatedAt),
                    onClick = { onAction(MemoryUiAction.OpenWorkingDetail(memory.id)) }
                )
                if (index < rows.lastIndex) SettingsGroupDivider()
            }
        }
    }
}

@Composable
private fun HistoryMemorySection(
    rows: List<MemoryOverviewState.HistoryEntry>,
    onAction: (MemoryUiAction) -> Unit
) {
    val title = stringResource(R.string.screen_memory_section_chat_index) + "（${rows.size}）"
    SettingsNamedGroup(title) {
        if (rows.isEmpty()) {
            MemoryEmptyRow()
        } else {
            rows.forEachIndexed { index, entry ->
                val historyTitle = entry.title.ifEmpty { entry.conversationId }
                ReadOnlyMemoryRow(
                    iconType = IconButtonView.BOOK_OPEN,
                    title = memoryPreview(entry.text, 80),
                    description = "$historyTitle · ${entry.role} · ${memoryTime(entry.updatedAt)}",
                    onClick = { onAction(MemoryUiAction.OpenHistoryDetail(entry.id)) }
                )
                if (index < rows.lastIndex) SettingsGroupDivider()
            }
        }
    }
}

@Composable
private fun ReadOnlyMemoryRow(
    iconType: Int,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemoryRowIcon(iconType)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(
                text = title,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 18.sp)
    }
}

@Composable
private fun MemoryRowIcon(iconType: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
            .background(Color(LineTheme.ACCENT_MUTED)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                IconButtonView(context, iconType).apply {
                    setIconColor(LineTheme.ACCENT)
                    setIconSizeDp(32, 17)
                    isClickable = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MemoryEmptyRow() {
    Text(
        text = stringResource(R.string.screen_memory_empty_text),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_SM.sp
    )
}

@Composable
private fun MemoryActionDialog(onAction: (MemoryUiAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(MemoryUiAction.DismissDialog) },
        title = { Text(stringResource(R.string.screen_memory_action_sheet_title)) },
        text = {
            Column {
                MemoryActionText(
                    text = stringResource(R.string.screen_memory_action_edit),
                    color = LineTheme.TEXT,
                    onClick = { onAction(MemoryUiAction.EditActionMemory) }
                )
                MemoryActionText(
                    text = stringResource(R.string.screen_memory_action_multi_select),
                    color = LineTheme.TEXT,
                    onClick = { onAction(MemoryUiAction.MultiSelectActionMemory) }
                )
                MemoryActionText(
                    text = stringResource(R.string.screen_memory_action_delete),
                    color = LineTheme.DANGER,
                    onClick = { onAction(MemoryUiAction.DeleteActionMemory) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onAction(MemoryUiAction.DismissDialog) }) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun MemoryActionText(
    text: String,
    color: Int,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        color = Color(color),
        fontSize = LineTheme.FONT_MD.sp
    )
}

@Composable
private fun MemoryEditorDialog(
    editor: MemoryDialogState.Editor,
    onAction: (MemoryUiAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onAction(MemoryUiAction.DismissDialog) },
        title = {
            Text(
                stringResource(
                    if (editor.editingId == null) R.string.screen_memory_editor_add
                    else R.string.screen_memory_editor_edit
                )
            )
        },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MemoryScopeChoice(
                        label = stringResource(R.string.screen_memory_scope_user),
                        scope = MemoryOverviewState.Memory.SCOPE_USER,
                        selectedScope = editor.draftScope,
                        onAction = onAction
                    )
                    MemoryScopeChoice(
                        label = stringResource(R.string.screen_memory_scope_project),
                        scope = MemoryOverviewState.Memory.SCOPE_PROJECT,
                        selectedScope = editor.draftScope,
                        onAction = onAction
                    )
                    MemoryScopeChoice(
                        label = stringResource(R.string.screen_memory_scope_environment),
                        scope = MemoryOverviewState.Memory.SCOPE_ENVIRONMENT,
                        selectedScope = editor.draftScope,
                        onAction = onAction
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editor.draftContent,
                    onValueChange = { onAction(MemoryUiAction.SetDraftContent(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    placeholder = { Text(stringResource(R.string.screen_memory_hint)) },
                    minLines = 5,
                    singleLine = false
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(MemoryUiAction.SaveEditor) }) {
                Text(stringResource(R.string.common_save), color = Color(LineTheme.ACCENT))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(MemoryUiAction.DismissDialog) }) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun MemoryScopeChoice(
    label: String,
    scope: String,
    selectedScope: String,
    onAction: (MemoryUiAction) -> Unit
) {
    Row(
        modifier = Modifier.clickable { onAction(MemoryUiAction.SetDraftScope(scope)) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selectedScope == scope,
            onClick = { onAction(MemoryUiAction.SetDraftScope(scope)) }
        )
        Text(
            text = label,
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_SM.sp
        )
    }
}

@Composable
private fun MemoryDeleteDialog(
    memory: MemoryOverviewState.Memory,
    onAction: (MemoryUiAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onAction(MemoryUiAction.DismissDialog) },
        title = { Text(stringResource(R.string.screen_memory_delete_title)) },
        text = {
            Text(
                stringResource(
                    R.string.screen_memory_action_body,
                    memoryPreview(memory.content, 120)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onAction(MemoryUiAction.ConfirmDelete) }) {
                Text(stringResource(R.string.common_delete), color = Color(LineTheme.DANGER))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(MemoryUiAction.DismissDialog) }) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun MemoryBatchDeleteDialog(
    count: Int,
    onAction: (MemoryUiAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onAction(MemoryUiAction.DismissDialog) },
        title = { Text(stringResource(R.string.screen_memory_delete_title)) },
        text = { Text(stringResource(R.string.screen_memory_delete_selected_message, count)) },
        confirmButton = {
            TextButton(onClick = { onAction(MemoryUiAction.ConfirmBatchDelete) }) {
                Text(stringResource(R.string.common_delete), color = Color(LineTheme.DANGER))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(MemoryUiAction.DismissDialog) }) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun MemoryDetailDialog(
    detail: MemoryDialogState.MemoryDetail,
    onAction: (MemoryUiAction) -> Unit
) {
    val titleRes = when (detail.section) {
        MemoryEditableSection.LONG_TERM -> R.string.screen_memory_section_long_term
        MemoryEditableSection.PROJECT -> R.string.screen_memory_section_project
        MemoryEditableSection.ENVIRONMENT -> R.string.screen_memory_section_environment
    }
    MemoryTextDialog(
        title = stringResource(titleRes),
        body = memoryDetail(detail.memory),
        onDismiss = { onAction(MemoryUiAction.DismissDialog) }
    )
}

@Composable
private fun WorkingMemoryDetailDialog(
    memory: MemoryOverviewState.WorkingMemory,
    onAction: (MemoryUiAction) -> Unit
) {
    MemoryTextDialog(
        title = stringResource(R.string.screen_memory_section_short_term),
        body = workingMemoryDetail(memory),
        onDismiss = { onAction(MemoryUiAction.DismissDialog) }
    )
}

@Composable
private fun HistoryDetailDialog(
    entry: MemoryOverviewState.HistoryEntry,
    onAction: (MemoryUiAction) -> Unit
) {
    MemoryTextDialog(
        title = stringResource(R.string.screen_memory_section_chat_index),
        body = historyDetail(entry),
        onDismiss = { onAction(MemoryUiAction.DismissDialog) }
    )
}

@Composable
private fun MemoryTextDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = body,
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                color = Color(LineTheme.TEXT_SECONDARY),
                fontSize = LineTheme.FONT_SM.sp,
                lineHeight = (LineTheme.FONT_SM + 4).sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close), color = Color(LineTheme.ACCENT))
            }
        }
    )
}

@Composable
private fun memoryDescription(memory: MemoryOverviewState.Memory): String =
    stringResource(R.string.screen_memory_field_source) + memory.source +
        " · " + stringResource(R.string.screen_memory_field_use_count_prefix) + memory.useCount +
        " " + stringResource(R.string.screen_memory_field_use_count_suffix) +
        " · " + memoryTime(memory.updatedAt)

@Composable
private fun memoryDetail(memory: MemoryOverviewState.Memory): String {
    val scopeLabel = stringResource(R.string.screen_memory_field_scope)
    val sourceLabel = stringResource(R.string.screen_memory_field_source)
    val projectLabel = stringResource(R.string.screen_memory_field_project)
    val confidenceLabel = stringResource(R.string.screen_memory_field_confidence)
    val useCountLabel = stringResource(R.string.screen_memory_field_use_count)
    val createdLabel = stringResource(R.string.screen_memory_field_created)
    val updatedLabel = stringResource(R.string.screen_memory_field_updated)
    val lastUsedLabel = stringResource(R.string.screen_memory_field_last_used)
    val project = memory.projectId.ifEmpty { stringResource(R.string.screen_memory_value_global) }
    val confidence = String.format(Locale.ROOT, "%.2f", memory.confidence)
    val created = memoryTime(memory.createdAt)
    val updated = memoryTime(memory.updatedAt)
    val lastUsed = if (memory.lastUsedAt > 0) {
        memoryTime(memory.lastUsedAt)
    } else {
        stringResource(R.string.screen_memory_value_not_used_yet)
    }
    return buildString {
        append(memory.content)
        append("\n\n").append(scopeLabel).append(memory.scope)
        append('\n').append(sourceLabel).append(memory.source)
        append('\n').append(projectLabel).append(project)
        append('\n').append(confidenceLabel).append(confidence)
        append('\n').append(useCountLabel).append(memory.useCount)
        append('\n').append(createdLabel).append(created)
        append('\n').append(updatedLabel).append(updated)
        append('\n').append(lastUsedLabel).append(lastUsed)
    }
}

@Composable
private fun workingMemoryDetail(memory: MemoryOverviewState.WorkingMemory): String {
    val sourceLabel = stringResource(R.string.screen_memory_field_source)
    val projectLabel = stringResource(R.string.screen_memory_field_project)
    val createdLabel = stringResource(R.string.screen_memory_field_created)
    val updatedLabel = stringResource(R.string.screen_memory_field_updated)
    val expiresLabel = stringResource(R.string.screen_memory_field_expires)
    val project = memory.projectId.ifEmpty { stringResource(R.string.screen_memory_value_global) }
    val created = memoryTime(memory.createdAt)
    val updated = memoryTime(memory.updatedAt)
    val expires = if (memory.expiresAt > 0) {
        memoryTime(memory.expiresAt)
    } else {
        stringResource(R.string.screen_memory_value_no_expiry)
    }
    return buildString {
        append(memory.content)
        append("\n\n").append(sourceLabel).append(memory.source)
        append('\n').append(projectLabel).append(project)
        append('\n').append(createdLabel).append(created)
        append('\n').append(updatedLabel).append(updated)
        append('\n').append(expiresLabel).append(expires)
    }
}

@Composable
private fun historyDetail(entry: MemoryOverviewState.HistoryEntry): String {
    val titleLabel = stringResource(R.string.screen_memory_field_title)
    val conversationLabel = stringResource(R.string.screen_memory_field_conversation)
    val messageLabel = stringResource(R.string.screen_memory_field_message)
    val createdLabel = stringResource(R.string.screen_memory_field_created)
    val updatedLabel = stringResource(R.string.screen_memory_field_updated)
    val created = memoryTime(entry.createdAt)
    val updated = memoryTime(entry.updatedAt)
    return buildString {
        if (entry.title.isNotEmpty()) {
            append(titleLabel).append(entry.title).append('\n')
        }
        append(entry.role).append(": ").append(entry.text)
        append("\n\n").append(conversationLabel).append(entry.conversationId)
        append('\n').append(messageLabel).append(entry.messageId.ifEmpty { "-" })
        append('\n').append(createdLabel).append(created)
        append('\n').append(updatedLabel).append(updated)
    }
}

@Composable
private fun memoryPreview(text: String, maxChars: Int): String {
    var value = text.replace('\n', ' ').replace('\r', ' ').trim()
    while (value.contains("  ")) {
        value = value.replace("  ", " ")
    }
    if (value.isEmpty()) return stringResource(R.string.screen_memory_value_empty)
    if (value.length <= maxChars) return value
    return value.substring(0, (maxChars - 3).coerceAtLeast(0)) + "..."
}

@Composable
private fun memoryTime(value: Long): String {
    if (value <= 0) return stringResource(R.string.screen_memory_value_unknown_time)
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))
}
