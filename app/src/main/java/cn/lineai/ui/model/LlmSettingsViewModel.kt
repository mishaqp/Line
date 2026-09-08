package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cn.lineai.model.AiBehaviorSettings
import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface LlmSettingsRepository {
    fun settings(): AiBehaviorSettings
    fun setToneMode(toneMode: String)
    fun setReasoningEffort(effort: String)
    fun setThinkingScrollEnabled(enabled: Boolean)
    fun setThinkingAutoExpandEnabled(enabled: Boolean)
    fun setPreserveReasoningEnabled(enabled: Boolean)
    fun setLearningModeEnabled(enabled: Boolean)
    fun setSoftCompactionEnabled(enabled: Boolean)
}

data class LlmSettingsUiState(
    val toneMode: String = AiBehaviorSettings.TONE_CODING,
    val reasoningEffort: String = AiBehaviorSettings.REASONING_MEDIUM,
    val thinkingScrollEnabled: Boolean = true,
    val thinkingAutoExpandEnabled: Boolean = false,
    val preserveReasoningEnabled: Boolean = false,
    val learningModeEnabled: Boolean = false,
    val softCompactionEnabled: Boolean = true
) {
    companion object {
        fun from(settings: AiBehaviorSettings?): LlmSettingsUiState {
            val value = settings ?: AiBehaviorSettings(null, true, false, null, false, false)
            return LlmSettingsUiState(
                toneMode = value.toneMode,
                reasoningEffort = value.reasoningEffort,
                thinkingScrollEnabled = value.isThinkingScrollEnabled,
                thinkingAutoExpandEnabled = value.isThinkingAutoExpandEnabled,
                preserveReasoningEnabled = value.isPreserveReasoningEnabled,
                learningModeEnabled = value.isLearningModeEnabled,
                softCompactionEnabled = value.isSoftCompactionEnabled
            )
        }
    }
}

sealed interface LlmSettingsUiAction {
    data object Back : LlmSettingsUiAction
    data class SetToneMode(val toneMode: String) : LlmSettingsUiAction
    data class SetReasoningEffort(val effort: String) : LlmSettingsUiAction
    data class SetThinkingScroll(val enabled: Boolean) : LlmSettingsUiAction
    data class SetThinkingAutoExpand(val enabled: Boolean) : LlmSettingsUiAction
    data class SetPreserveReasoning(val enabled: Boolean) : LlmSettingsUiAction
    data class SetLearningMode(val enabled: Boolean) : LlmSettingsUiAction
    data class SetSoftCompaction(val enabled: Boolean) : LlmSettingsUiAction
    data object OpenPromptTemplates : LlmSettingsUiAction
}

/**
 * UDF owner for the Compose LLM settings screen. Persistence stays behind
 * [LlmSettingsRepository]; Compose never talks to MainUiController.
 */
class LlmSettingsViewModel(
    private val repository: LlmSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(LlmSettingsUiState.from(repository.settings()))
    val state: StateFlow<LlmSettingsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = LlmSettingsUiState.from(repository.settings())
    }

    fun onAction(action: LlmSettingsUiAction): LineDestination? {
        return when (action) {
            LlmSettingsUiAction.Back -> null
            LlmSettingsUiAction.OpenPromptTemplates -> LineDestination.PromptTemplates
            is LlmSettingsUiAction.SetToneMode -> {
                val tone = AiBehaviorSettings.normalizeTone(action.toneMode)
                _state.update { it.copy(toneMode = tone) }
                repository.setToneMode(tone)
                null
            }
            is LlmSettingsUiAction.SetReasoningEffort -> {
                val effort = AiBehaviorSettings.normalizeReasoningEffort(action.effort)
                _state.update { it.copy(reasoningEffort = effort) }
                repository.setReasoningEffort(effort)
                null
            }
            is LlmSettingsUiAction.SetThinkingScroll -> {
                _state.update { it.copy(thinkingScrollEnabled = action.enabled) }
                repository.setThinkingScrollEnabled(action.enabled)
                null
            }
            is LlmSettingsUiAction.SetThinkingAutoExpand -> {
                _state.update { it.copy(thinkingAutoExpandEnabled = action.enabled) }
                repository.setThinkingAutoExpandEnabled(action.enabled)
                null
            }
            is LlmSettingsUiAction.SetPreserveReasoning -> {
                _state.update { it.copy(preserveReasoningEnabled = action.enabled) }
                repository.setPreserveReasoningEnabled(action.enabled)
                null
            }
            is LlmSettingsUiAction.SetLearningMode -> {
                _state.update { it.copy(learningModeEnabled = action.enabled) }
                repository.setLearningModeEnabled(action.enabled)
                null
            }
            is LlmSettingsUiAction.SetSoftCompaction -> {
                _state.update { it.copy(softCompactionEnabled = action.enabled) }
                repository.setSoftCompactionEnabled(action.enabled)
                null
            }
        }
    }

    companion object {
        fun factory(repository: LlmSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LlmSettingsViewModel(repository) as T
                }
            }
    }
}
