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
import cn.lineai.ui.model.KeepAliveSettingsUiAction
import cn.lineai.ui.model.KeepAliveSettingsUiState
import cn.lineai.ui.theme.LineTheme

@Composable
internal fun KeepAliveSettingsScreenContent(
    state: KeepAliveSettingsUiState,
    onAction: (KeepAliveSettingsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(LineTheme.BG))
    ) {
        SettingsScreenHeader(
            titleRes = R.string.screen_keep_alive_title,
            onBack = { onAction(KeepAliveSettingsUiAction.Back) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            SettingsGroup(R.string.screen_keep_alive_section_coding) {
                SettingsToggleRow(
                    glyph = "ϟ",
                    titleRes = R.string.screen_keep_alive_wake_lock_label,
                    descRes = R.string.screen_keep_alive_wake_lock_desc,
                    checked = state.wakeLockEnabled,
                    onCheckedChange = { onAction(KeepAliveSettingsUiAction.WakeLockChanged(it)) }
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    glyph = "●",
                    titleRes = R.string.screen_keep_alive_foreground_label,
                    descRes = R.string.screen_keep_alive_foreground_desc,
                    checked = state.foregroundEnabled,
                    onCheckedChange = { onAction(KeepAliveSettingsUiAction.ForegroundChanged(it)) }
                )
                SettingsGroupDivider()
                SettingsToggleRow(
                    glyph = "♪",
                    titleRes = R.string.screen_keep_alive_fake_music_label,
                    descRes = R.string.screen_keep_alive_fake_music_desc,
                    checked = state.fakeAudioEnabled,
                    onCheckedChange = { onAction(KeepAliveSettingsUiAction.FakeAudioChanged(it)) }
                )
            }

            SettingsGroup(R.string.screen_keep_alive_section_system) {
                SettingsToggleRow(
                    glyph = "↯",
                    titleRes = R.string.screen_keep_alive_ignore_battery_label,
                    descRes = R.string.screen_keep_alive_ignore_battery_desc,
                    checked = state.ignoringBatteryOptimizations,
                    onCheckedChange = { onAction(KeepAliveSettingsUiAction.BatteryOptimizationChanged(it)) }
                )
            }
        }
    }
}
