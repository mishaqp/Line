package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.R
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SettingsIcon {
    BOX,
    USER,
    BRAIN,
    MCP,
    SLIDERS,
    PACKAGE,
    ZAP,
    MESSAGE,
    PALETTE,
    MONITOR,
    SHIELD,
    DATABASE,
    BOOK,
    ARCHIVE,
    BUG,
    BATTERY,
    CPU
}

data class SettingsItemUi(
    val destination: LineDestination,
    val titleRes: Int,
    val descRes: Int,
    val icon: SettingsIcon
)

data class SettingsSectionUi(
    val titleRes: Int,
    val items: List<SettingsItemUi>
)

data class SettingsUiState(
    val sections: List<SettingsSectionUi> = emptyList()
) {
    val destinations: List<LineDestination>
        get() = sections.flatMap { section -> section.items.map { it.destination } }

    val screenIds: List<String>
        get() = destinations.map { it.screenId }
}

sealed interface SettingsUiAction {
    data object Back : SettingsUiAction
    data class Open(val destination: LineDestination) : SettingsUiAction
}

object SettingsCatalog {
    fun sections(): List<SettingsSectionUi> = listOf(
        SettingsSectionUi(
            titleRes = R.string.screen_settings_section_ai,
            items = listOf(
                item(LineDestination.Models, R.string.settings_row_models_title, R.string.settings_row_models_desc, SettingsIcon.BOX),
                item(LineDestination.CodexAccount, R.string.settings_row_codex_account_title, R.string.settings_row_codex_account_desc, SettingsIcon.USER),
                item(LineDestination.GrokAccount, R.string.settings_row_grok_account_title, R.string.settings_row_grok_account_desc, SettingsIcon.USER),
                item(LineDestination.Llm, R.string.screen_llm_title, R.string.settings_row_llm_desc, SettingsIcon.BRAIN)
            )
        ),
        SettingsSectionUi(
            titleRes = R.string.screen_settings_section_tools,
            items = listOf(
                item(LineDestination.Mcp, R.string.settings_row_mcp_title, R.string.settings_row_mcp_desc, SettingsIcon.MCP),
                item(LineDestination.ToolSettings, R.string.settings_row_tool_settings_title, R.string.settings_row_tool_settings_desc, SettingsIcon.SLIDERS),
                item(LineDestination.Extensions, R.string.settings_row_extensions_title, R.string.settings_row_extensions_desc, SettingsIcon.PACKAGE),
                item(LineDestination.AdvancedFeatures, R.string.settings_row_advanced_title, R.string.settings_row_advanced_desc, SettingsIcon.ZAP)
            )
        ),
        SettingsSectionUi(
            titleRes = R.string.screen_settings_section_ui,
            items = listOf(
                item(LineDestination.Input, R.string.screen_input_title, R.string.settings_row_input_desc, SettingsIcon.MESSAGE),
                item(LineDestination.Theme, R.string.settings_row_theme_title, R.string.settings_row_theme_desc, SettingsIcon.PALETTE),
                item(LineDestination.Output, R.string.settings_row_output_title, R.string.settings_row_output_desc, SettingsIcon.MONITOR)
            )
        ),
        SettingsSectionUi(
            titleRes = R.string.screen_settings_section_security,
            items = listOf(
                item(LineDestination.Security, R.string.screen_settings_section_security, R.string.settings_row_security_desc, SettingsIcon.SHIELD)
            )
        ),
        SettingsSectionUi(
            titleRes = R.string.screen_settings_section_data,
            items = listOf(
                item(LineDestination.Storage, R.string.settings_row_storage_title, R.string.settings_row_storage_desc, SettingsIcon.DATABASE),
                item(LineDestination.Memory, R.string.settings_row_memory_title, R.string.settings_row_memory_desc, SettingsIcon.BOOK),
                item(LineDestination.Data, R.string.settings_row_data_title, R.string.settings_row_data_desc, SettingsIcon.ARCHIVE),
                item(LineDestination.ErrorLogs, R.string.settings_row_error_logs_title, R.string.settings_row_error_logs_desc, SettingsIcon.BUG),
                item(LineDestination.KeepAlive, R.string.settings_row_keep_alive_title, R.string.settings_row_keep_alive_desc, SettingsIcon.BATTERY)
            )
        ),
        SettingsSectionUi(
            titleRes = R.string.screen_settings_section_info,
            items = listOf(
                item(LineDestination.About, R.string.settings_row_about_title, R.string.settings_row_about_desc, SettingsIcon.CPU)
            )
        )
    )

    fun contains(destination: LineDestination): Boolean =
        sections().any { section -> section.items.any { it.destination == destination } }

    private fun item(
        destination: LineDestination,
        titleRes: Int,
        descRes: Int,
        icon: SettingsIcon
    ) = SettingsItemUi(destination, titleRes, descRes, icon)
}

/**
 * UDF owner for the Compose Settings hub. The catalog is static, so there is
 * no repository — child screens stay on the existing typed ScreenRegistry bridge.
 */
class SettingsViewModel : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState(SettingsCatalog.sections()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun onAction(action: SettingsUiAction): LineDestination? {
        return when (action) {
            SettingsUiAction.Back -> null
            is SettingsUiAction.Open -> {
                action.destination.takeIf(SettingsCatalog::contains)
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel() as T
            }
        }
    }
}
