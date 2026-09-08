package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.lineai.R
import cn.lineai.ui.model.ModelListItemUi
import cn.lineai.ui.model.ModelManagementUiAction
import cn.lineai.ui.model.ModelManagementUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun ModelListScreenContent(
    state: ModelManagementUiState,
    onAction: (ModelManagementUiAction) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        Column(Modifier.fillMaxSize()) {
            ModelListHeader(state, onAction)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)
            ) {
                if (state.models.isEmpty()) {
                    val emptyText = if (state.allowManagement) {
                        stringResource(R.string.screen_models_empty_can_add)
                    } else {
                        stringResource(R.string.screen_models_empty_readonly)
                    }
                    Text(
                        text = emptyText,
                        color = Color(LineTheme.TEXT_TERTIARY),
                        fontSize = LineTheme.FONT_SM.sp,
                        lineHeight = (LineTheme.FONT_SM + 3).sp
                    )
                } else {
                    state.models.forEach { model ->
                        ModelListCard(
                            model = model,
                            checked = state.multiSelectedIds.contains(model.id),
                            multiSelectActive = state.multiSelectActive,
                            onAction = onAction
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        if (state.pendingActionModel != null) {
            ModelActionSheet(
                title = state.pendingActionModel?.name.orEmpty()
                    .ifBlank { stringResource(R.string.screen_models_title) },
                onDismiss = { onAction(ModelManagementUiAction.DismissActions) },
                onEdit = { onAction(ModelManagementUiAction.EditPendingModel) },
                onMultiSelect = { onAction(ModelManagementUiAction.StartMultiSelect) }
            )
        } else if (state.pendingDelete) {
            ModelDeleteSheet(
                count = state.multiSelectedIds.size,
                onDismiss = { onAction(ModelManagementUiAction.CancelDelete) },
                onConfirm = { onAction(ModelManagementUiAction.ConfirmDelete) }
            )
        }
    }
}

@Composable
private fun ModelListHeader(
    state: ModelManagementUiState,
    onAction: (ModelManagementUiAction) -> Unit
) {
    val title = if (state.multiSelectActive) {
        stringResource(R.string.screen_models_selected_count, state.multiSelectedIds.size)
    } else {
        stringResource(R.string.screen_models_title)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(LineTheme.BG))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderGlyph(
            label = if (state.multiSelectActive) "\u00d7" else "\u2039",
            onClick = {
                if (state.multiSelectActive) {
                    onAction(ModelManagementUiAction.ExitMultiSelect)
                } else {
                    onAction(ModelManagementUiAction.Back)
                }
            }
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (state.multiSelectActive) {
            HeaderGlyph(
                label = "\u232b",
                color = Color(LineTheme.DANGER),
                onClick = { onAction(ModelManagementUiAction.RequestDelete) }
            )
        } else if (state.allowManagement) {
            HeaderGlyph(
                label = "+",
                onClick = { onAction(ModelManagementUiAction.AddModel) }
            )
        } else {
            Spacer(Modifier.size(36.dp))
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
private fun HeaderGlyph(
    label: String,
    color: Color = Color(LineTheme.TEXT),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontSize = 22.sp)
    }
}

@Composable
private fun ModelListCard(
    model: ModelListItemUi,
    checked: Boolean,
    multiSelectActive: Boolean,
    onAction: (ModelManagementUiAction) -> Unit
) {
    val background = if (checked) Color(LineTheme.ACCENT_MUTED) else Color(LineTheme.BG)
    val border = if (model.selected || checked) Color(LineTheme.ACCENT) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .combinedClickable(
                onClick = { onAction(ModelManagementUiAction.SelectModel(model.id)) },
                onLongClick = { onAction(ModelManagementUiAction.LongPressModel(model.id)) }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(model.badgeColor))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = model.badgeLabel,
                color = Color(LineTheme.TEXT_ON_COLOR),
                fontSize = LineTheme.FONT_XS.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = model.name,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = model.modelId,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (multiSelectActive) {
            val checkBorder = if (checked) Color(LineTheme.ACCENT) else Color(LineTheme.TEXT_TERTIARY)
            val checkBg = if (checked) Color(LineTheme.ACCENT) else Color.Transparent
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(22.dp)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
                    .background(checkBg)
                    .border(1.dp, checkBorder, RoundedCornerShape(LineTheme.SHAPE_MD.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (checked) {
                    Text("\u2713", color = Color(LineTheme.TEXT_ON_COLOR), fontSize = 12.sp)
                }
            }
        } else if (model.selected) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(8.dp)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_XS.dp))
                    .background(Color(LineTheme.ACCENT))
            )
        }
    }
}

@Composable
private fun ModelActionSheet(
    title: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onMultiSelect: () -> Unit
) {
    BottomSheetScaffold(onDismiss = onDismiss) {
        SheetTitle(title)
        SheetDivider()
        SheetAction(
            label = stringResource(R.string.screen_models_action_modify),
            description = stringResource(R.string.screen_models_action_modify_desc),
            onClick = onEdit
        )
        SheetAction(
            label = stringResource(R.string.screen_models_action_multi_select),
            description = stringResource(R.string.screen_models_action_multi_select_desc),
            onClick = onMultiSelect
        )
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun ModelDeleteSheet(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    BottomSheetScaffold(onDismiss = onDismiss) {
        SheetTitle(stringResource(R.string.screen_models_delete_title))
        Text(
            text = stringResource(R.string.screen_models_delete_message, count),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_SM.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        SheetDivider()
        SheetAction(
            label = stringResource(R.string.common_cancel),
            description = "",
            onClick = onDismiss
        )
        SheetAction(
            label = stringResource(R.string.common_delete),
            description = stringResource(R.string.screen_models_delete_warning),
            labelColor = Color(LineTheme.DANGER),
            onClick = onConfirm
        )
        Spacer(Modifier.height(34.dp))
    }
}

@Composable
internal fun BottomSheetScaffold(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(LineTheme.OVERLAY))
                .clickable(onClick = onDismiss)
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = LineTheme.SHAPE_LG.dp, topEnd = LineTheme.SHAPE_LG.dp))
                .background(Color(LineTheme.SURFACE_ELEVATED))
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_XS.dp))
                    .background(Color(LineTheme.TEXT_TERTIARY))
            )
            content()
        }
    }
}

@Composable
internal fun SheetTitle(title: String) {
    Text(
        text = title,
        color = Color(LineTheme.TEXT),
        fontSize = LineTheme.FONT_LG.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
internal fun SheetDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER_LIGHT))
    )
}

@Composable
internal fun SheetAction(
    label: String,
    description: String,
    labelColor: Color = Color(LineTheme.TEXT),
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = label, color = labelColor, fontSize = LineTheme.FONT_MD.sp)
        if (description.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp
            )
        }
    }
}
