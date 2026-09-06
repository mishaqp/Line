package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.TermuxIntegrationStatus
import cn.lineai.ui.model.TermuxIntegrationUiAction
import cn.lineai.ui.model.TermuxIntegrationUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun TermuxIntegrationScreenContent(
    state: TermuxIntegrationUiState,
    onAction: (TermuxIntegrationUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_termux_title,
            onBack = { onAction(TermuxIntegrationUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            TermuxCard {
                Text(
                    text = stringResource(R.string.screen_termux_section_use),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text(
                    text = stringResource(R.string.screen_termux_use_desc),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_XS.sp,
                    lineHeight = (LineTheme.FONT_XS + 3).sp
                )
            }
            Spacer(Modifier.padding(top = 12.dp))
            TermuxCard {
                Text(
                    text = stringResource(R.string.screen_termux_section_steps),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                TermuxStep("1", stringResource(R.string.screen_termux_step_1))
                TermuxStep("2", stringResource(R.string.screen_termux_step_2))
                TermuxStep("3", stringResource(R.string.screen_termux_step_3))
            }
            Spacer(Modifier.padding(top = 12.dp))
            TermuxCard {
                Text(
                    text = stringResource(R.string.screen_termux_section_intent),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.padding(top = 8.dp))
                SelectionContainer {
                    Text(
                        text = state.grantCommand,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
                            .background(Color(LineTheme.CODE_BG))
                            .border(
                                width = 1.dp,
                                color = Color(LineTheme.CODE_BORDER),
                                shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
                            )
                            .padding(12.dp),
                        color = Color(LineTheme.TEXT_SECONDARY),
                        fontSize = LineTheme.FONT_XS.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = (LineTheme.FONT_XS + 3).sp
                    )
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            TermuxCard {
                Text(
                    text = stringResource(R.string.screen_termux_actions_title),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.padding(top = 8.dp))
                TermuxActionGrid(
                    setupEnabled = !state.isSetupRunning,
                    onCopy = { onAction(TermuxIntegrationUiAction.CopyGrantCommand) },
                    onPermission = { onAction(TermuxIntegrationUiAction.RequestRunCommandPermission) },
                    onOpen = { onAction(TermuxIntegrationUiAction.OpenTermux) },
                    onSetup = { onAction(TermuxIntegrationUiAction.StartSetup) }
                )
                if (state.status != TermuxIntegrationStatus.NONE) {
                    Spacer(Modifier.padding(top = 8.dp))
                    TermuxStatusBox(state)
                }
            }
        }
    }
}

@Composable
private fun TermuxCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
            .background(Color(LineTheme.SURFACE_ELEVATED))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun TermuxStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(LineTheme.SHAPE_MD.dp))
                .background(Color(LineTheme.ACCENT)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = Color(LineTheme.TEXT_ON_COLOR),
                fontSize = LineTheme.FONT_XS.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = text,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            color = Color(LineTheme.TEXT_TERTIARY),
            fontSize = LineTheme.FONT_XS.sp,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
    }
}

@Composable
private fun TermuxActionGrid(
    setupEnabled: Boolean,
    onCopy: () -> Unit,
    onPermission: () -> Unit,
    onOpen: () -> Unit,
    onSetup: () -> Unit
) {
    val stacked = LocalDensity.current.fontScale >= 1.15f
    val buttons = listOf(
        TermuxActionSpec(
            stringResource(R.string.screen_termux_copy_intent),
            IconButtonView.COPY,
            false,
            true,
            onCopy
        ),
        TermuxActionSpec(
            stringResource(R.string.screen_termux_run_command_perm),
            IconButtonView.SHIELD_CHECK,
            false,
            true,
            onPermission
        ),
        TermuxActionSpec(
            stringResource(R.string.screen_termux_open_termux),
            IconButtonView.EXTERNAL_LINK,
            false,
            true,
            onOpen
        ),
        TermuxActionSpec(
            stringResource(R.string.screen_termux_auto_ssh),
            IconButtonView.DOWNLOAD,
            true,
            setupEnabled,
            onSetup
        )
    )
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttons.forEach { spec ->
                TermuxActionButton(spec, Modifier.fillMaxWidth())
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TermuxActionButton(buttons[0], Modifier.weight(1f))
                TermuxActionButton(buttons[1], Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TermuxActionButton(buttons[2], Modifier.weight(1f))
                TermuxActionButton(buttons[3], Modifier.weight(1f))
            }
        }
    }
}

private data class TermuxActionSpec(
    val label: String,
    val iconType: Int,
    val primary: Boolean,
    val enabled: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun TermuxActionButton(
    spec: TermuxActionSpec,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 42.dp)
            .alpha(if (spec.enabled) 1f else 0.65f)
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(Color(if (spec.primary) LineTheme.ACCENT else LineTheme.SURFACE_LIGHT))
            .border(
                width = 1.dp,
                color = Color(if (spec.primary) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .clickable(enabled = spec.enabled, onClick = spec.onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AndroidView(
            modifier = Modifier.size(15.dp),
            factory = { context ->
                IconButtonView(context, spec.iconType).apply {
                    setIconColor(if (spec.primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY)
                    setIconSizeDp(15, 15)
                    isClickable = false
                }
            },
            update = { view ->
                view.setIconType(spec.iconType)
                view.setIconColor(if (spec.primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY)
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = spec.label,
            color = Color(if (spec.primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun TermuxStatusBox(state: TermuxIntegrationUiState) {
    val error = state.status == TermuxIntegrationStatus.SETUP_FAILED ||
        state.status == TermuxIntegrationStatus.TERMUX_OPEN_FAILED ||
        state.status == TermuxIntegrationStatus.PERMISSION_UNAVAILABLE
    val title = stringResource(
        when (state.status) {
            TermuxIntegrationStatus.COPIED -> R.string.screen_termux_status_copied_title
            TermuxIntegrationStatus.PERMISSION_REQUESTED -> R.string.screen_termux_status_requested_title
            TermuxIntegrationStatus.PERMISSION_UNAVAILABLE -> R.string.screen_termux_status_unable_title
            TermuxIntegrationStatus.TERMUX_OPENED -> R.string.screen_termux_status_opened_title
            TermuxIntegrationStatus.TERMUX_OPEN_FAILED -> R.string.screen_termux_status_open_failed_title
            TermuxIntegrationStatus.SETUP_RUNNING -> R.string.screen_termux_status_setup_title
            TermuxIntegrationStatus.SETUP_SUCCESS -> R.string.screen_termux_status_setup_done_title
            TermuxIntegrationStatus.SETUP_FAILED -> R.string.screen_termux_status_setup_failed_title
            TermuxIntegrationStatus.NONE -> R.string.screen_termux_status_copied_title
        }
    )
    val message = when (state.status) {
        TermuxIntegrationStatus.COPIED -> stringResource(R.string.screen_termux_status_copied_message)
        TermuxIntegrationStatus.PERMISSION_REQUESTED ->
            stringResource(R.string.screen_termux_status_requested_message)
        TermuxIntegrationStatus.PERMISSION_UNAVAILABLE ->
            stringResource(R.string.screen_termux_status_unable_message)
        TermuxIntegrationStatus.TERMUX_OPENED ->
            stringResource(R.string.screen_termux_status_opened_message)
        TermuxIntegrationStatus.TERMUX_OPEN_FAILED ->
            state.error.ifBlank { stringResource(R.string.screen_termux_status_open_failed_title) }
        TermuxIntegrationStatus.SETUP_RUNNING ->
            stringResource(R.string.screen_termux_status_setup_message)
        TermuxIntegrationStatus.SETUP_SUCCESS -> stringResource(
            R.string.screen_termux_status_setup_done_message,
            state.shell,
            state.rcPath,
            state.output
        )
        TermuxIntegrationStatus.SETUP_FAILED ->
            state.error.ifBlank { stringResource(R.string.screen_termux_status_setup_failed_title) }
        TermuxIntegrationStatus.NONE -> ""
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(Color(if (error) LineTheme.DANGER_MUTED else LineTheme.CODE_BG))
            .border(
                width = 1.dp,
                color = Color(if (error) LineTheme.DANGER else LineTheme.CODE_BORDER),
                shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.status_title_message, title, message),
            color = Color(if (error) LineTheme.DANGER else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_XS.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = (LineTheme.FONT_XS + 3).sp
        )
    }
}
