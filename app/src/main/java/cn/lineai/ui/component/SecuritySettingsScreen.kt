package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.lineai.R
import cn.lineai.ui.model.SecuritySettingsUiAction
import cn.lineai.ui.model.SecuritySettingsUiState
import cn.lineai.ui.theme.LineTheme

private const val GLYPH_HTTP_SHIELD = "\u2713"
private const val GLYPH_BROWSER_CODE = "</>"
private const val GLYPH_PATH_SHIELD = "\u26e8"
private const val GLYPH_AGENT_SHIELD = "\u25c8"

@Composable
internal fun SecuritySettingsScreenContent(
    state: SecuritySettingsUiState,
    onAction: (SecuritySettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_security_title,
            onBack = { onAction(SecuritySettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_security_section_http) {
                SettingsToggleRow(
                    glyph = GLYPH_HTTP_SHIELD,
                    titleRes = R.string.settings_row_security_allow_any_http_title,
                    descRes = R.string.settings_row_security_allow_any_http_desc,
                    checked = state.allowAnyHttp,
                    onCheckedChange = {
                        onAction(SecuritySettingsUiAction.SetAllowAnyHttp(it))
                    }
                )
            }

            SettingsGroup(R.string.screen_security_section_browser) {
                SettingsToggleRow(
                    glyph = GLYPH_BROWSER_CODE,
                    titleRes = R.string.screen_output_browser_js_label,
                    descRes = R.string.screen_output_browser_js_desc,
                    checked = state.browserJavaScriptEnabled,
                    onCheckedChange = {
                        onAction(SecuritySettingsUiAction.SetBrowserJavaScript(it))
                    }
                )
            }

            SettingsGroup(R.string.screen_security_section_path) {
                SettingsToggleRow(
                    glyph = GLYPH_PATH_SHIELD,
                    titleRes = R.string.settings_row_security_bypass_path_title,
                    descRes = R.string.settings_row_security_bypass_path_desc,
                    checked = state.bypassPathProtection,
                    onCheckedChange = {
                        onAction(SecuritySettingsUiAction.SetBypassPathProtection(it))
                    }
                )
            }

            SettingsGroup(R.string.screen_security_section_agent) {
                SettingsToggleRow(
                    glyph = GLYPH_AGENT_SHIELD,
                    titleRes = R.string.settings_row_security_full_access_title,
                    descRes = R.string.settings_row_security_full_access_desc,
                    checked = state.fullAccessEnabled,
                    onCheckedChange = {
                        onAction(SecuritySettingsUiAction.SetFullAccess(it))
                    }
                )
            }
        }
    }

    if (state.showBypassWarning) {
        AlertDialog(
            onDismissRequest = {
                onAction(SecuritySettingsUiAction.DismissBypassWarning)
            },
            title = {
                Text(stringResource(R.string.settings_row_security_bypass_path_warning_title))
            },
            text = {
                Text(stringResource(R.string.settings_row_security_bypass_path_warning_message))
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onAction(SecuritySettingsUiAction.DismissBypassWarning)
                    }
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(SecuritySettingsUiAction.ConfirmBypassPathProtection)
                    }
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            }
        )
    }
}
