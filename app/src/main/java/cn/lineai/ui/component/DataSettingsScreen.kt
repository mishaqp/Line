package cn.lineai.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cn.lineai.R
import cn.lineai.ui.model.DataSettingsUiAction
import cn.lineai.ui.model.DataSettingsUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun DataSettingsScreenContent(
    state: DataSettingsUiState,
    onAction: (DataSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_data_title,
            onBack = { onAction(DataSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_data_section_all) {
                SettingsNavRow(
                    glyph = "↓",
                    titleRes = R.string.screen_data_export_all,
                    descRes = R.string.screen_data_export_all_desc,
                    onClick = {
                        if (state.actionsEnabled) {
                            onAction(DataSettingsUiAction.ExportAll)
                        }
                    }
                )
                SettingsGroupDivider()
                SettingsNavRow(
                    glyph = "↑",
                    titleRes = R.string.screen_data_import_linecode,
                    descRes = R.string.screen_data_import_linecode_desc,
                    onClick = {
                        if (state.actionsEnabled) {
                            onAction(DataSettingsUiAction.ImportLineCode)
                        }
                    }
                )
            }
        }
    }
}
