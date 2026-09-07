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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.TerminalProviderInstalledUiItem
import cn.lineai.ui.model.TerminalProviderScanUiItem
import cn.lineai.ui.model.TerminalProvidersConfirmation
import cn.lineai.ui.model.TerminalProvidersUiAction
import cn.lineai.ui.model.TerminalProvidersUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun TerminalProvidersScreenContent(
    state: TerminalProvidersUiState,
    onAction: (TerminalProvidersUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_terminal_provider_title,
            onBack = { onAction(TerminalProvidersUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            TerminalProviderSection(stringResource(R.string.screen_terminal_provider_scan)) {
                ScanActionRow(
                    enabled = !state.operationInProgress,
                    onClick = { onAction(TerminalProvidersUiAction.Scan) }
                )
            }

            if (state.hasScanned) {
                TerminalProviderSection(stringResource(R.string.screen_terminal_provider_scan_results)) {
                    if (state.scanResults.isEmpty()) {
                        EmptyTerminalProviderRow(stringResource(R.string.screen_terminal_provider_scan_empty))
                    } else {
                        state.scanResults.forEachIndexed { index, provider ->
                            ScannedProviderRow(
                                provider = provider,
                                enabled = !state.operationInProgress,
                                onClick = { onAction(TerminalProvidersUiAction.RequestAdd(provider)) }
                            )
                            if (index < state.scanResults.lastIndex) TerminalProviderDivider()
                        }
                    }
                }
            }

            TerminalProviderSection(stringResource(R.string.screen_terminal_provider_installed)) {
                if (state.installed.isEmpty()) {
                    EmptyTerminalProviderRow(stringResource(R.string.screen_terminal_provider_empty))
                } else {
                    state.installed.forEachIndexed { index, provider ->
                        InstalledProviderRow(
                            provider = provider,
                            enabled = !state.operationInProgress,
                            onEnabledChanged = { checked ->
                                onAction(TerminalProvidersUiAction.SetEnabled(provider.id, checked))
                            },
                            onDeleteRequested = {
                                onAction(TerminalProvidersUiAction.RequestDelete(provider.id))
                            }
                        )
                        if (index < state.installed.lastIndex) TerminalProviderDivider()
                    }
                }
            }
        }
    }

    when (val confirmation = state.confirmation) {
        null -> Unit
        is TerminalProvidersConfirmation.Add -> AddProviderSheet(
            provider = confirmation.provider,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(TerminalProvidersUiAction.DismissDialog) },
            onConfirm = { onAction(TerminalProvidersUiAction.ConfirmAdd) }
        )
        is TerminalProvidersConfirmation.Delete -> DeleteProviderSheet(
            providerName = confirmation.providerName,
            enabled = !state.operationInProgress,
            onDismiss = { onAction(TerminalProvidersUiAction.DismissDialog) },
            onConfirm = { onAction(TerminalProvidersUiAction.ConfirmDelete) }
        )
    }
}

@Composable
private fun TerminalProviderSection(
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
                    1.dp,
                    Color(LineTheme.BORDER_LIGHT),
                    RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
private fun ScanActionRow(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TerminalProviderIcon(IconButtonView.SEARCH)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.screen_terminal_provider_scan),
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.screen_terminal_provider_scan_desc),
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Text("›", color = Color(LineTheme.TEXT_TERTIARY), fontSize = 20.sp)
    }
}

@Composable
private fun ScannedProviderRow(
    provider: TerminalProviderScanUiItem,
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
        TerminalProviderIcon(IconButtonView.TERMINAL)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = provider.label,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = provider.packageName,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        AndroidView(
            modifier = Modifier.size(20.dp),
            factory = { context ->
                IconButtonView(context, IconButtonView.PLUS).apply {
                    setIconColor(LineTheme.ACCENT)
                    setIconSizeDp(20, 20)
                    isClickable = false
                }
            }
        )
    }
}

@Composable
private fun InstalledProviderRow(
    provider: TerminalProviderInstalledUiItem,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onDeleteRequested: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(provider.id, enabled) {
                detectTapGestures(
                    onLongPress = {
                        if (enabled) onDeleteRequested()
                    }
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TerminalProviderIcon(IconButtonView.TERMINAL)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = provider.name,
                color = Color(LineTheme.TEXT),
                fontSize = LineTheme.FONT_MD.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = provider.packageName,
                color = Color(LineTheme.TEXT_TERTIARY),
                fontSize = LineTheme.FONT_XS.sp,
                lineHeight = (LineTheme.FONT_XS + 3).sp
            )
        }
        Switch(
            checked = provider.enabled,
            enabled = enabled,
            onCheckedChange = onEnabledChanged
        )
    }
}

@Composable
private fun TerminalProviderIcon(iconType: Int) {
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
private fun EmptyTerminalProviderRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(LineTheme.TEXT_TERTIARY),
        fontSize = LineTheme.FONT_SM.sp,
        lineHeight = (LineTheme.FONT_SM + 3).sp
    )
}

@Composable
private fun TerminalProviderDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(LineTheme.BORDER_LIGHT))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProviderSheet(
    provider: TerminalProviderScanUiItem,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        SheetBody(
            title = stringResource(R.string.screen_terminal_provider_add_confirm, provider.label),
            description = stringResource(R.string.screen_terminal_provider_add_confirm_desc),
            enabled = enabled,
            confirmLabel = stringResource(R.string.common_add),
            confirmColor = LineTheme.ACCENT,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteProviderSheet(
    providerName: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { if (enabled) onDismiss() },
        containerColor = Color(LineTheme.SURFACE_ELEVATED)
    ) {
        SheetBody(
            title = stringResource(R.string.screen_terminal_provider_delete_title),
            description = stringResource(R.string.screen_terminal_provider_delete_confirm, providerName),
            enabled = enabled,
            confirmLabel = stringResource(R.string.common_delete),
            confirmColor = LineTheme.DANGER,
            onDismiss = onDismiss,
            onConfirm = onConfirm
        )
    }
}

@Composable
private fun SheetBody(
    title: String,
    description: String,
    enabled: Boolean,
    confirmLabel: String,
    confirmColor: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 34.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_LG.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_SM.sp,
            lineHeight = (LineTheme.FONT_SM + 3).sp
        )
        TerminalProviderDivider()
        SheetActionRow(
            label = stringResource(R.string.common_cancel),
            color = LineTheme.TEXT,
            enabled = enabled,
            onClick = onDismiss
        )
        SheetActionRow(
            label = confirmLabel,
            color = confirmColor,
            enabled = enabled,
            onClick = onConfirm
        )
    }
}

@Composable
private fun SheetActionRow(
    label: String,
    color: Int,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = Color(color),
        fontSize = LineTheme.FONT_MD.sp
    )
}
