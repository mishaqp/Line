package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.model.SkillRecord
import cn.lineai.ui.model.SkillsExtensionListItem
import cn.lineai.ui.model.SkillsExtensionsSheet
import cn.lineai.ui.model.SkillsExtensionsUiAction
import cn.lineai.ui.model.SkillsExtensionsUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun SkillsExtensionsScreenContent(
    state: SkillsExtensionsUiState,
    onAction: (SkillsExtensionsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SkillsExtensionsHeader(
            onBack = { onAction(SkillsExtensionsUiAction.Back) },
            onAdd = { onAction(SkillsExtensionsUiAction.OpenActions) }
        )
        if (state.multiSelect) {
            SkillsSelectionBar(
                count = state.selectedIds.size,
                onCancel = { onAction(SkillsExtensionsUiAction.CancelSelection) },
                onDelete = { onAction(SkillsExtensionsUiAction.RequestDeleteSelected) }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SkillsExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_section_install_other)
            ) {
                SkillsAddRow(
                    onClick = { onAction(SkillsExtensionsUiAction.OpenActions) }
                )
            }

            SkillsExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_workspace_share)
            ) {
                SkillsShareRow(
                    onClick = { onAction(SkillsExtensionsUiAction.ShareWorkspace) }
                )
            }

            SkillsExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_section_installed)
            ) {
                if (state.items.isEmpty()) {
                    SkillsEmptyRow(stringResource(R.string.screen_extension_detail_empty_skills))
                } else {
                    state.items.forEachIndexed { index, item ->
                        SkillsInstalledRow(
                            item = item,
                            multiSelect = state.multiSelect,
                            selected = item.id in state.selectedIds,
                            onEnabledChanged = { checked ->
                                onAction(SkillsExtensionsUiAction.SetEnabled(item.id, checked))
                            },
                            onClick = {
                                if (state.multiSelect) {
                                    onAction(SkillsExtensionsUiAction.ToggleSelection(item.id))
                                }
                            },
                            onLongPress = {
                                onAction(SkillsExtensionsUiAction.EnterMultiSelect(item.id))
                            }
                        )
                        if (index < state.items.lastIndex) SkillsExtensionsDivider()
                    }
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        null -> Unit
        SkillsExtensionsSheet.Actions -> SkillsActionsSheet(
            showMultiSelect = state.canEnterMultiSelect,
            onDismiss = { onAction(SkillsExtensionsUiAction.Dismiss) },
            onPickFile = { onAction(SkillsExtensionsUiAction.PickFile) },
            onGitHub = { onAction(SkillsExtensionsUiAction.OpenGitHubInstall) },
            onCreate = { onAction(SkillsExtensionsUiAction.OpenCreateSkill) },
            onPath = { onAction(SkillsExtensionsUiAction.OpenPathInstall) },
            onMultiSelect = { onAction(SkillsExtensionsUiAction.EnterMultiSelect()) }
        )
        SkillsExtensionsSheet.FileTarget -> SkillsFileTargetSheet(
            displayName = state.pendingDocument?.displayName.orEmpty(),
            onDismiss = { onAction(SkillsExtensionsUiAction.Dismiss) },
            onProject = {
                onAction(SkillsExtensionsUiAction.ChooseFileTarget(SkillRecord.LOCATION_PROJECT))
            },
            onGlobal = {
                onAction(SkillsExtensionsUiAction.ChooseFileTarget(SkillRecord.LOCATION_APP))
            }
        )
        SkillsExtensionsSheet.GitHub -> SkillsGitHubSheet(
            url = state.githubUrl,
            location = state.installLocation,
            onDismiss = { onAction(SkillsExtensionsUiAction.Dismiss) },
            onUrlChanged = { onAction(SkillsExtensionsUiAction.SetGitHubUrl(it)) },
            onLocation = { onAction(SkillsExtensionsUiAction.SetLocation(it)) },
            onInstall = { onAction(SkillsExtensionsUiAction.ConfirmGitHubInstall) }
        )
        SkillsExtensionsSheet.Create -> SkillsCreateSheet(
            name = state.createName,
            description = state.createDescription,
            content = state.createContent,
            location = state.installLocation,
            onDismiss = { onAction(SkillsExtensionsUiAction.Dismiss) },
            onName = { onAction(SkillsExtensionsUiAction.SetCreateName(it)) },
            onDescription = { onAction(SkillsExtensionsUiAction.SetCreateDescription(it)) },
            onContent = { onAction(SkillsExtensionsUiAction.SetCreateContent(it)) },
            onLocation = { onAction(SkillsExtensionsUiAction.SetLocation(it)) },
            onCreate = { onAction(SkillsExtensionsUiAction.ConfirmCreateSkill) }
        )
        SkillsExtensionsSheet.Path -> SkillsPathSheet(
            path = state.sourcePath,
            optionalName = state.optionalName,
            suggestedPath = state.suggestedPath,
            location = state.installLocation,
            onDismiss = { onAction(SkillsExtensionsUiAction.Dismiss) },
            onPathChanged = { onAction(SkillsExtensionsUiAction.SetPath(it)) },
            onNameChanged = { onAction(SkillsExtensionsUiAction.SetOptionalName(it)) },
            onLocation = { onAction(SkillsExtensionsUiAction.SetLocation(it)) },
            onInstall = { onAction(SkillsExtensionsUiAction.ConfirmPathInstall) }
        )
        SkillsExtensionsSheet.DeleteConfirm -> SkillsDeleteManySheet(
            count = state.selectedIds.size,
            onDismiss = { onAction(SkillsExtensionsUiAction.Dismiss) },
            onConfirm = { onAction(SkillsExtensionsUiAction.ConfirmDeleteSelected) }
        )
    }
}

@Composable
private fun SkillsExtensionsHeader(
    onBack: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("\u2039", color = Color(LineTheme.TEXT), fontSize = 22.sp)
        }
        Text(
            text = stringResource(R.string.screen_extensions_section_skills),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                .background(Color(LineTheme.ACCENT_MUTED))
                .clickable(onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    IconButtonView(context, IconButtonView.PLUS).apply {
                        setIconColor(LineTheme.ACCENT)
                        setIconSizeDp(36, 19)
                        isClickable = false
                    }
                }
            )
        }
    }
    SkillsExtensionsDivider(color = LineTheme.BORDER)
}

@Composable
private fun SkillsSelectionBar(
    count: Int,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.screen_extension_detail_selected_count, count),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.common_cancel),
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color(LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(R.string.common_delete),
            modifier = Modifier
                .clickable(onClick = onDelete)
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
            color = Color(LineTheme.DANGER),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SkillsExtensionsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Medium
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(LineTheme.SURFACE_ELEVATED))
                .border(
                    width = 1.dp,
                    color = Color(LineTheme.BORDER_LIGHT),
                    shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SkillsAddRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkillsArchiveIcon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_inline_title_skills),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.screen_extension_detail_inline_desc_skills),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 20.sp)
    }
}

@Composable
private fun SkillsShareRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkillsIcon(IconButtonView.FOLDER_OPEN)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_workspace_share),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.screen_extension_detail_workspace_share_desc),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 20.sp)
    }
}

@Composable
private fun SkillsInstalledRow(
    item: SkillsExtensionListItem,
    multiSelect: Boolean,
    selected: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (multiSelect && selected) Color(LineTheme.ACCENT_MUTED) else Color.Transparent
            )
            .pointerInput(item.id, multiSelect) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkillsArchiveIcon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = item.name,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.subtitle,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        if (!multiSelect) {
            Switch(
                checked = item.enabled,
                onCheckedChange = onEnabledChanged
            )
        }
    }
}

@Composable
private fun SkillsArchiveIcon() {
    SkillsIcon(IconButtonView.ARCHIVE)
}

@Composable
private fun SkillsIcon(iconType: Int) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
            .background(Color(LineTheme.ACCENT_MUTED)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                IconButtonView(context, iconType).apply {
                    setIconColor(LineTheme.ACCENT)
                    setIconSizeDp(32, 16)
                    isClickable = false
                }
            }
        )
    }
}

@Composable
private fun SkillsEmptyRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_SM.sp,
        lineHeight = (LineTheme.FONT_SM + 3).sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SkillsExtensionsDivider(color: Int = LineTheme.BORDER_LIGHT) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(color))
    )
}

@Composable
private fun SkillsSheetActionRow(
    label: String,
    description: String,
    color: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = Color(color),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Medium
        )
        if (description.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsActionsSheet(
    showMultiSelect: Boolean,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onGitHub: () -> Unit,
    onCreate: () -> Unit,
    onPath: () -> Unit,
    onMultiSelect: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_sheet_title_skills),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            SkillsExtensionsDivider()
            SkillsSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_install_zip),
                description = stringResource(R.string.screen_extension_detail_install_zip_desc),
                color = LineTheme.TEXT,
                onClick = onPickFile
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_install_github),
                description = stringResource(R.string.screen_extension_detail_install_github_desc),
                color = LineTheme.TEXT,
                onClick = onGitHub
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_create_skill),
                description = stringResource(R.string.screen_extension_detail_create_skill_desc),
                color = LineTheme.TEXT,
                onClick = onCreate
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_install_path),
                description = stringResource(R.string.screen_extension_detail_install_path_desc),
                color = LineTheme.TEXT,
                onClick = onPath
            )
            if (showMultiSelect) {
                SkillsSheetActionRow(
                    label = stringResource(R.string.screen_extension_detail_multi_select),
                    description = stringResource(R.string.screen_extension_detail_multi_select_desc),
                    color = LineTheme.TEXT,
                    onClick = onMultiSelect
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsFileTargetSheet(
    displayName: String,
    onDismiss: () -> Unit,
    onProject: () -> Unit,
    onGlobal: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_sheet_title_install_target),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            if (displayName.isNotEmpty()) {
                Text(
                    text = displayName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_SM.sp
                )
            }
            SkillsExtensionsDivider()
            SkillsSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_target_project),
                description = stringResource(R.string.screen_extension_detail_target_project_desc),
                color = LineTheme.TEXT,
                onClick = onProject
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_target_global),
                description = stringResource(R.string.screen_extension_detail_target_global_desc),
                color = LineTheme.TEXT,
                onClick = onGlobal
            )
        }
    }
}

@Composable
private fun SkillsLocationRows(
    location: String,
    onLocation: (String) -> Unit
) {
    SkillsSheetActionRow(
        label = stringResource(R.string.screen_extension_detail_position_project),
        description = if (location == SkillRecord.LOCATION_PROJECT) "\u25cf" else "",
        color = LineTheme.TEXT,
        onClick = { onLocation(SkillRecord.LOCATION_PROJECT) }
    )
    SkillsSheetActionRow(
        label = stringResource(R.string.screen_extension_detail_position_global),
        description = if (location == SkillRecord.LOCATION_APP) "\u25cf" else "",
        color = LineTheme.TEXT,
        onClick = { onLocation(SkillRecord.LOCATION_APP) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsGitHubSheet(
    url: String,
    location: String,
    onDismiss: () -> Unit,
    onUrlChanged: (String) -> Unit,
    onLocation: (String) -> Unit,
    onInstall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_dialog_github_skill),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.screen_extension_detail_field_github_url)) },
                placeholder = {
                    Text(
                        stringResource(R.string.screen_extension_detail_hint_github_url),
                        color = Color(LineTheme.TEXT_TERTIARY)
                    )
                },
                supportingText = {
                    Text(stringResource(R.string.screen_extension_detail_helper_github_url))
                }
            )
            SkillsLocationRows(location = location, onLocation = onLocation)
            SkillsExtensionsDivider()
            SkillsSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                onClick = onDismiss
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.common_install),
                description = "",
                color = LineTheme.ACCENT,
                onClick = onInstall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsCreateSheet(
    name: String,
    description: String,
    content: String,
    location: String,
    onDismiss: () -> Unit,
    onName: (String) -> Unit,
    onDescription: (String) -> Unit,
    onContent: (String) -> Unit,
    onLocation: (String) -> Unit,
    onCreate: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_create_skill),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.screen_extension_detail_field_name)) },
                placeholder = {
                    Text("android-native-view", color = Color(LineTheme.TEXT_TERTIARY))
                }
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.screen_extension_detail_field_desc)) },
                placeholder = {
                    Text(
                        stringResource(R.string.screen_extension_detail_hint_skill_desc),
                        color = Color(LineTheme.TEXT_TERTIARY)
                    )
                }
            )
            OutlinedTextField(
                value = content,
                onValueChange = onContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.screen_extension_detail_field_content)) },
                placeholder = {
                    Text(
                        stringResource(R.string.screen_extension_detail_hint_content),
                        color = Color(LineTheme.TEXT_TERTIARY)
                    )
                },
                minLines = 4
            )
            SkillsLocationRows(location = location, onLocation = onLocation)
            SkillsExtensionsDivider()
            SkillsSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                onClick = onDismiss
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.common_create),
                description = "",
                color = LineTheme.ACCENT,
                onClick = onCreate
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsPathSheet(
    path: String,
    optionalName: String,
    suggestedPath: String,
    location: String,
    onDismiss: () -> Unit,
    onPathChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onLocation: (String) -> Unit,
    onInstall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_dialog_install_skill),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = path,
                onValueChange = onPathChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.screen_extension_detail_field_source_path)) },
                placeholder = {
                    Text(
                        suggestedPath.ifEmpty { "/Download/skill.zip" },
                        color = Color(LineTheme.TEXT_TERTIARY)
                    )
                },
                supportingText = {
                    Text(stringResource(R.string.screen_extension_detail_helper_source_path))
                }
            )
            OutlinedTextField(
                value = optionalName,
                onValueChange = onNameChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text(stringResource(R.string.screen_extension_detail_field_name_optional)) },
                placeholder = {
                    Text(
                        stringResource(R.string.screen_extension_detail_hint_optional_name),
                        color = Color(LineTheme.TEXT_TERTIARY)
                    )
                }
            )
            SkillsLocationRows(location = location, onLocation = onLocation)
            SkillsExtensionsDivider()
            SkillsSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                onClick = onDismiss
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.common_install),
                description = "",
                color = LineTheme.ACCENT,
                onClick = onInstall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillsDeleteManySheet(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_delete_multi_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.screen_extension_detail_delete_multi_message, count),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_SM.sp,
                lineHeight = (LineTheme.FONT_SM + 3).sp
            )
            SkillsExtensionsDivider()
            SkillsSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                onClick = onDismiss
            )
            SkillsSheetActionRow(
                label = stringResource(R.string.common_delete),
                description = stringResource(R.string.screen_extension_detail_delete_multi_warning),
                color = LineTheme.DANGER,
                onClick = onConfirm
            )
        }
    }
}
