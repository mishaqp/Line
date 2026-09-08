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
import cn.lineai.ui.model.AgentExtensionListItem
import cn.lineai.ui.model.AgentExtensionsSheet
import cn.lineai.ui.model.AgentExtensionsUiAction
import cn.lineai.ui.model.AgentExtensionsUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun AgentExtensionsScreenContent(
    state: AgentExtensionsUiState,
    onAction: (AgentExtensionsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        AgentExtensionsHeader(
            enabled = !state.operationInProgress,
            onBack = { onAction(AgentExtensionsUiAction.Back) },
            onAdd = { onAction(AgentExtensionsUiAction.Add) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            AgentExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_section_install_other)
            ) {
                AgentAddRow(
                    enabled = !state.operationInProgress,
                    onClick = { onAction(AgentExtensionsUiAction.Add) }
                )
            }

            AgentExtensionsSection(
                title = stringResource(R.string.screen_extension_detail_section_installed)
            ) {
                if (state.items.isEmpty()) {
                    AgentEmptyRow(stringResource(R.string.screen_extension_detail_empty_agent))
                } else {
                    state.items.forEachIndexed { index, item ->
                        AgentInstalledRow(
                            item = item,
                            enabled = !state.operationInProgress,
                            onEnabledChanged = { checked ->
                                onAction(AgentExtensionsUiAction.SetEnabled(item.id, checked))
                            },
                            onLongPress = {
                                onAction(AgentExtensionsUiAction.RequestActions(item.id))
                            }
                        )
                        if (index < state.items.lastIndex) AgentExtensionsDivider()
                    }
                }
            }
        }
    }

    when (val sheet = state.sheet) {
        null -> Unit
        is AgentExtensionsSheet.Actions -> AgentActionsSheet(
            extensionName = sheet.extensionName,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(AgentExtensionsUiAction.DismissSheet) },
            onModify = { onAction(AgentExtensionsUiAction.Modify(sheet.extensionId)) },
            onDelete = { onAction(AgentExtensionsUiAction.RequestDelete(sheet.extensionId)) }
        )
        is AgentExtensionsSheet.Delete -> AgentDeleteSheet(
            extensionName = sheet.extensionName,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(AgentExtensionsUiAction.DismissSheet) },
            onConfirm = { onAction(AgentExtensionsUiAction.ConfirmDelete) }
        )
    }
}

@Composable
private fun AgentExtensionsHeader(
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
            text = stringResource(R.string.screen_extensions_section_agent),
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
    AgentExtensionsDivider(color = LineTheme.BORDER)
}

@Composable
private fun AgentExtensionsSection(
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
private fun AgentAddRow(
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
        AgentExtensionIcon()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_extension_detail_inline_title_agent),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.screen_extension_detail_inline_desc_agent),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("\u203a", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 20.sp)
    }
}

@Composable
private fun AgentInstalledRow(
    item: AgentExtensionListItem,
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
        AgentExtensionIcon()
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
                text = item.slug + " \u00b7 " + item.toolCount.toString() + " tools",
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
private fun AgentExtensionIcon() {
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
                IconButtonView(context, IconButtonView.BRAIN).apply {
                    setIconColor(LineTheme.ACCENT)
                    setIconSizeDp(32, 16)
                    isClickable = false
                }
            }
        )
    }
}

@Composable
private fun AgentEmptyRow(text: String) {
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
private fun AgentExtensionsDivider(color: Int = LineTheme.BORDER_LIGHT) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(color))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentActionsSheet(
    extensionName: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onModify: () -> Unit,
    onDelete: () -> Unit
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
                text = extensionName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_LG.sp,
                fontWeight = FontWeight.Bold
            )
            AgentExtensionsDivider()
            AgentSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_modify),
                description = stringResource(R.string.screen_extension_detail_modify_desc),
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onModify
            )
            AgentSheetActionRow(
                label = stringResource(R.string.screen_extension_detail_delete),
                description = stringResource(R.string.screen_extension_detail_delete_desc),
                color = LineTheme.DANGER,
                enabled = enabled,
                onClick = onDelete
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentDeleteSheet(
    extensionName: String,
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
                    extensionName
                ),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_SM.sp,
                lineHeight = (LineTheme.FONT_SM + 3).sp
            )
            AgentExtensionsDivider()
            AgentSheetActionRow(
                label = stringResource(R.string.common_cancel),
                description = "",
                color = LineTheme.TEXT,
                enabled = enabled,
                onClick = onDismiss
            )
            AgentSheetActionRow(
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
private fun AgentSheetActionRow(
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
