package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import cn.lineai.R
import cn.lineai.model.OutputSettings
import cn.lineai.ui.markdown.MarkdownView
import cn.lineai.ui.model.OutputSettingsUiAction
import cn.lineai.ui.model.OutputSettingsUiState
import cn.lineai.ui.theme.LineTheme

private const val GLYPH_WRAP = "\u2630"
private const val GLYPH_GLOBE = "\u25ef"
private const val GLYPH_EXTERNAL = "\u2197"
private const val GLYPH_JS = "\u26a1"
private const val GLYPH_FILE = "\u270e"

@Composable
internal fun OutputSettingsScreenContent(
    state: OutputSettingsUiState,
    onAction: (OutputSettingsUiAction) -> Unit
) {
    val previewMarkdown = stringResource(R.string.screen_output_preview_markdown)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_output_title,
            onBack = { onAction(OutputSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_output_section_code) {
                SettingsToggleRow(
                    glyph = GLYPH_WRAP,
                    titleRes = R.string.screen_output_code_wrap_label,
                    descRes = R.string.screen_output_code_wrap_desc,
                    checked = state.codeWrapEnabled,
                    onCheckedChange = { onAction(OutputSettingsUiAction.SetCodeWrap(it)) }
                )
            }
            SettingsGroup(R.string.screen_output_section_browser) {
                SettingsChoiceRow(
                    glyph = GLYPH_GLOBE,
                    titleRes = R.string.screen_output_browser_internal_label,
                    descRes = R.string.screen_output_browser_internal_desc,
                    selected = state.browserMode == OutputSettings.BROWSER_BUILTIN,
                    onClick = {
                        onAction(
                            OutputSettingsUiAction.SetBrowserMode(OutputSettings.BROWSER_BUILTIN)
                        )
                    }
                )
                SettingsGroupDivider()
                SettingsChoiceRow(
                    glyph = GLYPH_EXTERNAL,
                    titleRes = R.string.screen_output_browser_external_label,
                    descRes = R.string.screen_output_browser_external_desc,
                    selected = state.browserMode == OutputSettings.BROWSER_EXTERNAL,
                    onClick = {
                        onAction(
                            OutputSettingsUiAction.SetBrowserMode(OutputSettings.BROWSER_EXTERNAL)
                        )
                    }
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    glyph = GLYPH_JS,
                    titleRes = R.string.screen_output_browser_js_label,
                    descRes = R.string.screen_output_browser_js_desc,
                    checked = state.browserJavaScriptEnabled,
                    onCheckedChange = {
                        onAction(OutputSettingsUiAction.SetBrowserJavaScript(it))
                    }
                )
            }
            SettingsGroup(R.string.screen_output_section_preview) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LineTheme.LG.dp),
                    factory = { context ->
                        MarkdownView(context).apply {
                            setLinkHandler { }
                            setMarkdown(previewMarkdown)
                            setCodeWrapEnabled(state.codeWrapEnabled)
                        }
                    },
                    update = { view ->
                        view.setCodeWrapEnabled(state.codeWrapEnabled)
                    }
                )
            }
            SettingsGroup(R.string.screen_output_section_toolcall) {
                SettingsNavRow(
                    glyph = GLYPH_FILE,
                    titleRes = R.string.screen_output_toolcall_preview_label,
                    descRes = R.string.screen_output_toolcall_preview_desc,
                    onClick = { onAction(OutputSettingsUiAction.OpenToolCallPreview) }
                )
            }
        }
    }
}
