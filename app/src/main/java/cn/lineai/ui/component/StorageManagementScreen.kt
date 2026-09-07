package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.StorageUiAction
import cn.lineai.ui.model.StorageUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun StorageManagementScreenContent(
    state: StorageUiState,
    onAction: (StorageUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        StorageHeader(
            busy = state.isInitialLoading || state.isRefreshing || state.isClearing,
            clearEnabled = state.stats != null && !state.isClearing,
            onBack = { onAction(StorageUiAction.Back) },
            onClear = { onAction(StorageUiAction.OpenClearDialog) },
            onRefresh = { onAction(StorageUiAction.Refresh) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            StorageSummary(state)

            StorageCard(
                iconType = IconButtonView.GIT_COMPARE,
                titleRes = R.string.screen_storage_row_diff_cache,
                descRes = R.string.screen_storage_desc_diff,
                sizeText = state.stats?.formatDiffCacheSize() ?: "-",
                count = state.stats?.diffCacheCount
            )
            StorageCard(
                iconType = IconButtonView.MESSAGE_SQUARE,
                titleRes = R.string.screen_storage_row_chat,
                descRes = R.string.screen_storage_desc_chat,
                sizeText = state.stats?.formatChatSize() ?: "-",
                count = state.stats?.chatCount
            )
            StorageCard(
                iconType = IconButtonView.SETTINGS,
                titleRes = R.string.screen_storage_row_config,
                descRes = R.string.screen_storage_desc_config,
                sizeText = state.stats?.formatConfigSize() ?: "-",
                count = state.stats?.configCount
            )
            StorageCard(
                iconType = IconButtonView.FOLDER,
                titleRes = R.string.screen_storage_row_home,
                descRes = R.string.screen_storage_desc_home,
                sizeText = state.stats?.formatHomeSize() ?: "-",
                count = state.stats?.homeCount
            )
        }
    }

    if (state.showClearDialog) {
        StorageClearDialog(
            clearDiffCache = state.clearDiffCacheSelected,
            clearChatHistory = state.clearChatHistorySelected,
            onClearDiffCacheChanged = { onAction(StorageUiAction.SetClearDiffCache(it)) },
            onClearChatHistoryChanged = { onAction(StorageUiAction.SetClearChatHistory(it)) },
            onConfirm = { onAction(StorageUiAction.ConfirmClear) },
            onDismiss = { onAction(StorageUiAction.DismissClearDialog) }
        )
    }
}

@Composable
private fun StorageHeader(
    busy: Boolean,
    clearEnabled: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit
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
            text = stringResource(R.string.screen_storage_title),
            modifier = Modifier.weight(1f),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold
        )
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
                strokeWidth = 2.dp
            )
        }
        TextButton(onClick = onClear, enabled = clearEnabled) {
            Text(stringResource(R.string.common_clear))
        }
        AndroidView(
            factory = { context ->
                RefreshCwButtonView(context, 18).apply {
                    setOnClickListener { onRefresh() }
                }
            },
            update = { button -> button.setOnClickListener { onRefresh() } },
            modifier = Modifier.size(36.dp)
        )
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER))
    )
}

@Composable
private fun StorageSummary(state: StorageUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_storage_counted),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(LineTheme.XS.dp))
        Text(
            text = state.stats?.formatTotalSize()
                ?: stringResource(R.string.screen_storage_calculating),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_XXL.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(LineTheme.XS.dp))
        Text(
            text = stringResource(R.string.screen_storage_summary),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp
        )
    }
}

@Composable
private fun StorageCard(
    iconType: Int,
    titleRes: Int,
    descRes: Int,
    sizeText: String,
    count: Int?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                .background(Color(LineTheme.ACCENT_MUTED)),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    IconButtonView(context, iconType).apply {
                        setIconColor(LineTheme.ACCENT)
                        setIconSizeDp(38, 19)
                        isClickable = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(descRes),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = sizeText,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = count?.let {
                    "$it${stringResource(R.string.screen_storage_unit_items)}"
                } ?: "-",
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun StorageClearDialog(
    clearDiffCache: Boolean,
    clearChatHistory: Boolean,
    onClearDiffCacheChanged: (Boolean) -> Unit,
    onClearChatHistoryChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_storage_clear_title)) },
        text = {
            Column {
                Text(stringResource(R.string.screen_storage_clear_message))
                Spacer(Modifier.height(12.dp))
                StorageClearChoice(
                    checked = clearDiffCache,
                    titleRes = R.string.screen_storage_row_diff_cache,
                    descRes = R.string.screen_storage_desc_diff,
                    onCheckedChange = onClearDiffCacheChanged
                )
                StorageClearChoice(
                    checked = clearChatHistory,
                    titleRes = R.string.screen_storage_row_chat,
                    descRes = R.string.screen_storage_desc_chat,
                    onCheckedChange = onClearChatHistoryChanged
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = clearDiffCache || clearChatHistory
            ) {
                Text(stringResource(R.string.common_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun StorageClearChoice(
    checked: Boolean,
    titleRes: Int,
    descRes: Int,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = stringResource(titleRes),
                color = Color(LineTheme.TEXT),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(descRes),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp
            )
        }
    }
}
