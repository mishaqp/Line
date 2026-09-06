package cn.lineai.ui.component

import android.graphics.Typeface
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.McpSettingsUiAction
import cn.lineai.ui.model.McpSettingsUiState
import cn.lineai.ui.model.McpSettingsViewModel
import cn.lineai.ui.model.McpToolGroupUiModel
import cn.lineai.ui.theme.FlowLayoutView
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun McpSettingsScreenContent(
    state: McpSettingsUiState,
    onAction: (McpSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_mcp_title,
            onBack = { onAction(McpSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 84.dp)
        ) {
            McpExecutionCard(
                executionMode = state.executionMode,
                onMode = { onAction(McpSettingsUiAction.SetExecutionMode(it)) }
            )
            Spacer(Modifier.height(12.dp))
            if (state.showSshActions) {
                McpSshCard(
                    onSshSettings = { onAction(McpSettingsUiAction.OpenSshSettings) },
                    onTermux = { onAction(McpSettingsUiAction.OpenTermuxIntegration) }
                )
                Spacer(Modifier.height(12.dp))
            }
            state.groups.forEach { group ->
                McpToolGroupCard(
                    group = group,
                    onEnabledChanged = { enabled ->
                        onAction(McpSettingsUiAction.SetToolGroupEnabled(group.id, enabled))
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun McpExecutionCard(
    executionMode: String,
    onMode: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_mcp_section_execution),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                .background(Color(LineTheme.SURFACE_LIGHT))
                .padding(3.dp)
        ) {
            McpModeButton(
                label = stringResource(R.string.screen_mcp_execution_local),
                selected = executionMode == McpSettingsViewModel.MODE_LOCAL,
                onClick = { onMode(McpSettingsViewModel.MODE_LOCAL) },
                modifier = Modifier.weight(1f)
            )
            McpModeButton(
                label = stringResource(R.string.screen_mcp_execution_ssh),
                selected = executionMode == McpSettingsViewModel.MODE_SSH,
                onClick = { onMode(McpSettingsViewModel.MODE_SSH) },
                modifier = Modifier.weight(1f)
            )
            McpModeButton(
                label = stringResource(R.string.screen_mcp_execution_terminal_provider),
                selected = executionMode == McpSettingsViewModel.MODE_TERMINAL_PROVIDER,
                onClick = { onMode(McpSettingsViewModel.MODE_TERMINAL_PROVIDER) },
                modifier = Modifier.weight(1f)
            )
            McpModeButton(
                label = stringResource(R.string.screen_mcp_execution_root),
                selected = executionMode == McpSettingsViewModel.MODE_ROOT,
                onClick = { onMode(McpSettingsViewModel.MODE_ROOT) },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(descriptionForMode(executionMode)),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
    }
}

@Composable
private fun McpModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(if (selected) Color(LineTheme.ACCENT) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(if (selected) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun McpSshCard(
    onSshSettings: () -> Unit,
    onTermux: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_mcp_section_ssh),
            color = Color(LineTheme.TEXT),
            fontSize = LineTheme.FONT_MD.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.screen_mcp_ssh_overview),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            McpActionChip(
                label = stringResource(R.string.screen_mcp_ssh_settings),
                iconType = IconButtonView.SERVER,
                primary = true,
                onClick = onSshSettings,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            McpActionChip(
                label = stringResource(R.string.screen_mcp_termux_integration),
                iconType = IconButtonView.SMARTPHONE,
                primary = false,
                onClick = onTermux,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun McpActionChip(
    label: String,
    iconType: Int,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(LineTheme.SHAPE_FULL.dp))
            .background(Color(if (primary) LineTheme.ACCENT else LineTheme.SURFACE_LIGHT))
            .border(
                width = 1.dp,
                color = Color(if (primary) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                shape = RoundedCornerShape(LineTheme.SHAPE_FULL.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.size(15.dp),
            factory = { context ->
                IconButtonView(context, iconType).apply {
                    setIconColor(if (primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY)
                    setIconSizeDp(15, 15)
                    isClickable = false
                }
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color(if (primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun McpToolGroupCard(
    group: McpToolGroupUiModel,
    onEnabledChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(LineTheme.SHAPE_LG.dp))
                    .background(Color(LineTheme.ACCENT_MUTED)),
                contentAlignment = Alignment.Center
            ) {
                val iconType = iconFor(group.iconKey)
                val iconColor = if (group.enabled) LineTheme.ACCENT else LineTheme.TEXT_TERTIARY
                AndroidView(
                    modifier = Modifier.size(36.dp),
                    factory = { context ->
                        IconButtonView(context, iconType).apply {
                            setIconColor(iconColor)
                            setIconSizeDp(36, 18)
                            isClickable = false
                        }
                    },
                    update = { view ->
                        view.setIconType(iconType)
                        view.setIconColor(iconColor)
                    }
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = group.name,
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = group.description,
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_XS.sp,
                    lineHeight = (LineTheme.FONT_XS + 3).sp
                )
            }
            Switch(
                checked = group.enabled,
                onCheckedChange = onEnabledChanged
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(LineTheme.BORDER_LIGHT))
        )
        Spacer(Modifier.height(8.dp))
        val tools = group.tools
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                FlowLayoutView(context).apply {
                    setSpacingDp(LineTheme.SM, LineTheme.SM)
                }
            },
            update = { wrap ->
                wrap.removeAllViews()
                val context = wrap.context
                tools.forEach { tool ->
                    val badge = LineTheme.text(
                        context,
                        tool,
                        LineTheme.FONT_XS,
                        LineTheme.TEXT_SECONDARY,
                        Typeface.NORMAL
                    )
                    badge.typeface = Typeface.MONOSPACE
                    badge.background = LineTheme.rounded(
                        context,
                        LineTheme.SURFACE_LIGHT,
                        LineTheme.SHAPE_XS
                    )
                    LineTheme.padding(badge, LineTheme.SM, 2, LineTheme.SM, 2)
                    wrap.addView(
                        badge,
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    )
                }
            }
        )
    }
}

private fun descriptionForMode(mode: String): Int = when (mode) {
    McpSettingsViewModel.MODE_LOCAL -> R.string.screen_mcp_execution_local_desc
    McpSettingsViewModel.MODE_TERMINAL_PROVIDER -> R.string.screen_mcp_execution_terminal_provider_desc
    McpSettingsViewModel.MODE_ROOT -> R.string.screen_mcp_execution_root_desc
    else -> R.string.screen_mcp_execution_ssh_desc
}

private fun iconFor(iconKey: String): Int = when (iconKey) {
    "shell" -> IconButtonView.TERMINAL
    "web_search" -> IconButtonView.SEARCH
    "image_understanding" -> IconButtonView.PAINTBRUSH
    "image_generation" -> IconButtonView.SPARKLES
    "agent" -> IconButtonView.BRAIN
    "todo" -> IconButtonView.SCROLL_TEXT
    else -> IconButtonView.MCP
}
