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
import cn.lineai.ui.model.LinecodeExtensionsSheet
import cn.lineai.ui.model.LinecodeExtensionsUiAction
import cn.lineai.ui.model.LinecodeExtensionsUiState
import cn.lineai.ui.model.LinecodePackageListItem
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun LinecodeExtensionsScreenContent(
    state: LinecodeExtensionsUiState,
    onAction: (LinecodeExtensionsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        LinecodeExtensionsHeader(
            enabled = !state.operationInProgress,
            onBack = { onAction(LinecodeExtensionsUiAction.Back) },
            onAdd = { onAction(LinecodeExtensionsUiAction.OpenInstallMethods) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            LinecodeExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_section_install_other)
            ) {
                LinecodeAddRow(
                    enabled = !state.operationInProgress,
                    onClick = { onAction(LinecodeExtensionsUiAction.OpenInstallMethods) }
                )
            }

            LinecodeExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_section_installed)
            ) {
                if (state.items.isEmpty()) {
                    LinecodeEmptyRow(stringResource(R.string.screen_extension_detail_empty_linecode))
                } else {
                    state.items.forEachIndexed { index, item ->
                        LinecodeInstalledRow(
                            item = item,
                            enabled = !state.operationInProgress,
                            onEnabledChanged = { checked ->
                                onAction(LinecodeExtensionsUiAction.SetEnabled(item.id, checked))
                            },
                            onLongPress = {
                                onAction(LinecodeExtensionsUiAction.RequestDelete(item.id))
                            }
                        )
                        if (index < state.items.lastIndex) LinecodeExtensionsDivider()
                    }
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        null -> Unit
        LinecodeExtensionsSheet.InstallMethods -> LinecodeInstallMethodsSheet(
            enabled = !state.operationInProgress,
            onDismiss = { onAction(LinecodeExtensionsUiAction.DismissSheet) },
            onPickFile = { onAction(LinecodeExtensionsUiAction.PickFile) },
            onPath = { onAction(LinecodeExtensionsUiAction.OpenPathInstall) }
        )
        LinecodeExtensionsSheet.ManualPath -> LinecodePathSheet(
            path = state.sourcePath,
            suggestedPath = state.suggestedPath,
            location = state.installLocation,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(LinecodeExtensionsUiAction.DismissSheet) },
            onPathChanged = { onAction(LinecodeExtensionsUiAction.SetPath(it)) },
            onLocation = { onAction(LinecodeExtensionsUiAction.SetInstallLocation(it)) },
            onInstall = { onAction(LinecodeExtensionsUiAction.ConfirmPathInstall) }
        )
        LinecodeExtensionsSheet.FileTarget -> LinecodeFileTargetSheet(
            displayName = state.pendingDocument?.displayName.orEmpty(),
            location = state.installLocation,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(LinecodeExtensionsUiAction.DismissSheet) },
            onLocation = { onAction(LinecodeExtensionsUiAction.SetInstallLocation(it)) },
            onInstall = { onAction(LinecodeExtensionsUiAction.ConfirmUriInstall) }
        )
        is LinecodeExtensionsSheet.Delete -> LinecodeDeleteSheet(
            packageName = sheet.packageName,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(LinecodeExtensionsUiAction.DismissSheet) },
            onConfirm = { onAction(LinecodeExtensionsUiAction.ConfirmDelete) }
        )
    }
}

@Composable
private fun LinecodeExtensionsHeader(
    enabled: Boolean,
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
            text = stringResource(R.string.screen_extensions_section_linecode),
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
                .clickable(enabled = enabled, onClick = onAdd),
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
    LinecodeExtensionsDivider(color = LineTheme.BORDER)
}

@Composable
private fun LinecodeExtensionsSection(
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
private fun LinecodeAddRow(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinecodePackageIcon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_inline_title_linecode),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.screen_extension_detail_inline_desc_linecode),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 20.sp)
    }
}

@Composable
private fun LinecodeInstalledRow(
    item: LinecodePackageListItem,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(item.id, enabled) {
                detectTapGestures(
                    onLongPress = {
                        if (enabled) onLongPress()
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinecodePackageIcon()
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
        Switch(
            checked = item.enabled,
            enabled = enabled,
            onCheckedChange = onEnabledChanged
        )
    }
}

@Composable
private fun LinecodePackageIcon() {
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
                IconButtonView(context, IconButtonView.PACKAGE).apply {
                    setIconColor(LineTheme.ACCENT)
                    setIconSizeDp(32, 16)
                    isClickable = false
                }
            }
        )
    }
}

@Composable
private fun LinecodeEmptyRow(text: String) {
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
private fun LinecodeExtensionsDivider(color: Int = LineTheme.BORDER_LIGHT) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(color))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinecodeInstallMethodsSheet(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onPath: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.lip_install_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            LinecodeExtensionsDivider()
            LinecodeSheetActionRow(
                label = stringResource(R.string.lip_pick_file),
                description = stringResource(R.string.lip_pick_file_desc),
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onPickFile
            )
            LinecodeSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_field_source_path),
                description = stringResource(R.string.lip_path_desc),
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onPath
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinecodePathSheet(
    path: String,
    suggestedPath: String,
    location: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onPathChanged: (String) -> Unit,
    onLocation: (String) -> Unit,
    onInstall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.lip_install_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = path,
                onValueChange = onPathChanged,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                label = {
                    Text(stringResource(R.string.screen_extension_detail_field_source_path))
                },
                placeholder = {
                    Text(
                        suggestedPath.ifEmpty { "/Download/package.lip" },
                        color = Color(LineTheme.TEXT_TERTIARY)
                    )
                }
            )
            LinecodeLocationRows(location = location, enabled = enabled, onLocation = onLocation)
            LinecodeExtensionsDivider()
            LinecodeSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onDismiss
            )
            LinecodeSheetActionRow(
                label = stringResource(R.string.common_install),
                description = "",
                color = LineTheme.ACCENT,
                enabled = enabled,
                onClick = onInstall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinecodeFileTargetSheet(
    displayName: String,
    location: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onLocation: (String) -> Unit,
    onInstall: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.lip_install_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            if (displayName.isNotEmpty()) {
                Text(
                    text = displayName,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_SM.sp
                )
            }
            LinecodeLocationRows(location = location, enabled = enabled, onLocation = onLocation)
            LinecodeExtensionsDivider()
            LinecodeSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onDismiss
            )
            LinecodeSheetActionRow(
                label = stringResource(R.string.common_install),
                description = "",
                color = LineTheme.ACCENT,
                enabled = enabled,
                onClick = onInstall
            )
        }
    }
}

@Composable
private fun LinecodeLocationRows(
    location: String,
    enabled: Boolean,
    onLocation: (String) -> Unit
) {
    LinecodeSheetActionRow(
        label = stringResource(R.string.screen_extension_detail_position_project),
        description = if (location == SkillRecord.LOCATION_PROJECT) "\u25cf" else "",
        color = LineTheme.TEXT,
        enabled = enabled,
        onClick = { onLocation(SkillRecord.LOCATION_PROJECT) }
    )
    LinecodeSheetActionRow(
        label = stringResource(R.string.screen_extension_detail_position_global),
        description = if (location == SkillRecord.LOCATION_APP) "\u25cf" else "",
        color = LineTheme.TEXT,
        enabled = enabled,
        onClick = { onLocation(SkillRecord.LOCATION_APP) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinecodeDeleteSheet(
    packageName: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 34.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_delete_title),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.screen_extension_detail_delete_confirm,
                    packageName
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_SM.sp,
                lineHeight = (LineTheme.FONT_SM + 3).sp
            )
            LinecodeExtensionsDivider()
            LinecodeSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onDismiss
            )
            LinecodeSheetActionRow(
                label = stringResource(R.string.common_delete),
                description = stringResource(R.string.screen_extension_detail_delete_confirm_desc),
                color = LineTheme.DANGER,
                enabled = enabled,
                onClick = onConfirm
            )
        }
    }
}

@Composable
private fun LinecodeSheetActionRow(
    label: String,
    description: String,
    color: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
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
