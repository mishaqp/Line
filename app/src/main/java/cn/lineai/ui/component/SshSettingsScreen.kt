package cn.lineai.ui.component

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.ui.model.SshConnectionStatus
import cn.lineai.ui.model.SshSettingsUiAction
import cn.lineai.ui.model.SshSettingsUiState
import cn.lineai.ui.theme.IconButtonView
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun SshSettingsScreenContent(
    state: SshSettingsUiState,
    onAction: (SshSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
            .imePadding()
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_ssh_title,
            onBack = { onAction(SshSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 84.dp)
        ) {
            SshCard {
                Text(
                    text = stringResource(R.string.screen_ssh_section_server),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.screen_ssh_server_desc),
                    color = Color(LineTheme.TEXT_TERTIARY),
                    fontSize = LineTheme.FONT_XS.sp,
                    lineHeight = (LineTheme.FONT_XS + 3).sp
                )
                Spacer(Modifier.height(12.dp))
                SshActionButton(
                    label = stringResource(R.string.screen_ssh_termux),
                    iconType = IconButtonView.SMARTPHONE,
                    primary = false,
                    enabled = true,
                    onClick = { onAction(SshSettingsUiAction.OpenTermuxIntegration) }
                )
            }
            Spacer(Modifier.height(12.dp))
            SshCard {
                Text(
                    text = stringResource(R.string.screen_ssh_form_title),
                    color = Color(LineTheme.TEXT),
                    fontSize = LineTheme.FONT_MD.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                SshTextField(
                    value = state.host,
                    onValueChange = { onAction(SshSettingsUiAction.SetHost(it)) },
                    label = stringResource(R.string.screen_ssh_field_host),
                    hint = stringResource(R.string.screen_ssh_hint_host)
                )
                SshTextField(
                    value = state.port,
                    onValueChange = { onAction(SshSettingsUiAction.SetPort(it)) },
                    label = stringResource(R.string.screen_ssh_field_port),
                    hint = stringResource(R.string.screen_ssh_hint_port),
                    keyboardType = KeyboardType.Number
                )
                SshTextField(
                    value = state.username,
                    onValueChange = { onAction(SshSettingsUiAction.SetUsername(it)) },
                    label = stringResource(R.string.screen_ssh_field_username),
                    hint = stringResource(R.string.screen_ssh_hint_username)
                )
                SshTextField(
                    value = state.password,
                    onValueChange = { onAction(SshSettingsUiAction.SetPassword(it)) },
                    label = stringResource(R.string.screen_ssh_field_password_optional),
                    hint = stringResource(R.string.screen_ssh_hint_password),
                    hidden = true
                )
                SshTextField(
                    value = state.privateKey,
                    onValueChange = { onAction(SshSettingsUiAction.SetPrivateKey(it)) },
                    label = stringResource(R.string.screen_ssh_field_private_key),
                    hint = stringResource(R.string.screen_ssh_hint_private_key),
                    singleLine = false,
                    minLines = 4,
                    monospace = true
                )
                SshTextField(
                    value = state.passphrase,
                    onValueChange = { onAction(SshSettingsUiAction.SetPassphrase(it)) },
                    label = stringResource(R.string.screen_ssh_field_key_passphrase_optional),
                    hint = stringResource(R.string.screen_ssh_hint_passphrase),
                    hidden = true
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    SshActionButton(
                        label = stringResource(R.string.screen_ssh_save),
                        iconType = IconButtonView.SAVE,
                        primary = false,
                        enabled = true,
                        onClick = { onAction(SshSettingsUiAction.Save) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SshActionButton(
                        label = stringResource(R.string.screen_ssh_test),
                        iconType = IconButtonView.TERMINAL,
                        primary = true,
                        enabled = !state.isTesting,
                        onClick = { onAction(SshSettingsUiAction.TestConnection) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.status != SshConnectionStatus.HIDDEN) {
                    Spacer(Modifier.height(8.dp))
                    SshStatusBox(state)
                }
            }
        }
    }
}

@Composable
private fun SshCard(content: @Composable () -> Unit) {
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
private fun SshTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    hint: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    hidden: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    monospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        label = { Text(label) },
        placeholder = { Text(hint) },
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = if (hidden) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (hidden) KeyboardType.Password else keyboardType
        ),
        textStyle = TextStyle(
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontSize = LineTheme.FONT_SM.sp,
            color = Color(LineTheme.TEXT)
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(LineTheme.TEXT),
            unfocusedTextColor = Color(LineTheme.TEXT),
            focusedBorderColor = Color(LineTheme.ACCENT),
            unfocusedBorderColor = Color(LineTheme.BORDER_LIGHT),
            focusedLabelColor = Color(LineTheme.ACCENT),
            unfocusedLabelColor = Color(LineTheme.TEXT_TERTIARY),
            cursorColor = Color(LineTheme.ACCENT),
            focusedPlaceholderColor = Color(LineTheme.TEXT_TERTIARY),
            unfocusedPlaceholderColor = Color(LineTheme.TEXT_TERTIARY)
        )
    )
}

@Composable
private fun SshActionButton(
    label: String,
    iconType: Int,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(42.dp)
            .alpha(if (enabled) 1f else 0.65f)
            .clip(RoundedCornerShape(LineTheme.SHAPE_SM.dp))
            .background(Color(if (primary) LineTheme.ACCENT else LineTheme.SURFACE_LIGHT))
            .border(
                width = 1.dp,
                color = Color(if (primary) LineTheme.ACCENT else LineTheme.BORDER_LIGHT),
                shape = RoundedCornerShape(LineTheme.SHAPE_SM.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))
        AndroidView(
            modifier = Modifier.size(16.dp),
            factory = { context ->
                IconButtonView(context, iconType).apply {
                    setIconColor(if (primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY)
                    setIconSizeDp(16, 16)
                    isClickable = false
                }
            },
            update = { view ->
                view.setIconType(iconType)
                view.setIconColor(if (primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY)
            }
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = Color(if (primary) LineTheme.TEXT_ON_COLOR else LineTheme.TEXT_SECONDARY),
            fontSize = LineTheme.FONT_SM.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
    }
}

@Composable
private fun SshStatusBox(state: SshSettingsUiState) {
    val error = state.status == SshConnectionStatus.FAILED
    val title = stringResource(
        when (state.status) {
            SshConnectionStatus.SAVED -> R.string.screen_ssh_status_saved_title
            SshConnectionStatus.TESTING -> R.string.screen_ssh_status_testing_title
            SshConnectionStatus.SUCCESS -> R.string.screen_ssh_status_success_title
            SshConnectionStatus.FAILED -> R.string.screen_ssh_status_failed_title
            SshConnectionStatus.HIDDEN -> R.string.screen_ssh_status_saved_title
        }
    )
    val fallback = stringResource(
        when (state.status) {
            SshConnectionStatus.SAVED -> R.string.screen_ssh_status_saved_message
            SshConnectionStatus.TESTING -> R.string.screen_ssh_status_testing_message
            SshConnectionStatus.SUCCESS -> R.string.screen_ssh_status_success_message
            SshConnectionStatus.FAILED -> R.string.screen_ssh_status_failed_title
            SshConnectionStatus.HIDDEN -> R.string.screen_ssh_status_saved_message
        }
    )
    val message = state.statusDetail.ifBlank { fallback }
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
