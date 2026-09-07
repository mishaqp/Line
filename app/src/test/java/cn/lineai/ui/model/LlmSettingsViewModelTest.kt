package cn.lineai.ui.model

import cn.lineai.model.AiBehaviorSettings
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LlmSettingsViewModelTest {
    @Test
    fun initialStateMirrorsRepositorySettings() {
        val stored = AiBehaviorSettings(
            AiBehaviorSettings.TONE_CHAT,
            false,
            true,
            AiBehaviorSettings.REASONING_HIGH,
            true,
            true,
            false
        )
        val viewModel = LlmSettingsViewModel(FakeRepository(stored))

        val state = viewModel.state.value
        assertEquals(AiBehaviorSettings.TONE_CHAT, state.toneMode)
        assertEquals(AiBehaviorSettings.REASONING_HIGH, state.reasoningEffort)
        assertFalse(state.thinkingScrollEnabled)
        assertTrue(state.thinkingAutoExpandEnabled)
        assertTrue(state.preserveReasoningEnabled)
        assertTrue(state.learningModeEnabled)
        assertFalse(state.softCompactionEnabled)
    }

    @Test
    fun eachSettingChangeUpdatesStateAndRepositoryImmediately() {
        val repository = FakeRepository()
        val viewModel = LlmSettingsViewModel(repository)

        viewModel.onAction(LlmSettingsUiAction.SetToneMode(AiBehaviorSettings.TONE_CHAT))
        assertEquals(AiBehaviorSettings.TONE_CHAT, viewModel.state.value.toneMode)
        assertEquals(AiBehaviorSettings.TONE_CHAT, repository.settings().toneMode)
        assertEquals(listOf("tone:chat"), repository.writes)

        viewModel.onAction(LlmSettingsUiAction.SetReasoningEffort(AiBehaviorSettings.REASONING_MAX))
        assertEquals(AiBehaviorSettings.REASONING_MAX, viewModel.state.value.reasoningEffort)
        assertEquals(AiBehaviorSettings.REASONING_MAX, repository.settings().reasoningEffort)

        viewModel.onAction(LlmSettingsUiAction.SetThinkingScroll(false))
        assertFalse(viewModel.state.value.thinkingScrollEnabled)
        assertFalse(repository.settings().isThinkingScrollEnabled)

        viewModel.onAction(LlmSettingsUiAction.SetThinkingAutoExpand(true))
        assertTrue(viewModel.state.value.thinkingAutoExpandEnabled)
        assertTrue(repository.settings().isThinkingAutoExpandEnabled)

        viewModel.onAction(LlmSettingsUiAction.SetPreserveReasoning(true))
        assertTrue(viewModel.state.value.preserveReasoningEnabled)
        assertTrue(repository.settings().isPreserveReasoningEnabled)

        viewModel.onAction(LlmSettingsUiAction.SetLearningMode(true))
        assertTrue(viewModel.state.value.learningModeEnabled)
        assertTrue(repository.settings().isLearningModeEnabled)

        viewModel.onAction(LlmSettingsUiAction.SetSoftCompaction(false))
        assertFalse(viewModel.state.value.softCompactionEnabled)
        assertFalse(repository.settings().isSoftCompactionEnabled)

        assertEquals(
            listOf(
                "tone:chat",
                "reasoning:max",
                "scroll:false",
                "autoExpand:true",
                "preserve:true",
                "learning:true",
                "soft:false"
            ),
            repository.writes
        )
    }

    @Test
    fun openPromptTemplatesReturnsTypedDestination() {
        val viewModel = LlmSettingsViewModel(FakeRepository())

        assertEquals(
            LineDestination.PromptTemplates,
            viewModel.onAction(LlmSettingsUiAction.OpenPromptTemplates)
        )
        assertEquals(
            "promptTemplates",
            viewModel.onAction(LlmSettingsUiAction.OpenPromptTemplates)?.screenId
        )
        assertFalse(
            viewModel.onAction(LlmSettingsUiAction.OpenPromptTemplates) is LineDestination.Legacy
        )
        assertEquals(
            LineDestination.Llm,
            LineDestinations.parentOf(LineDestination.PromptTemplates)
        )
        assertNull(viewModel.onAction(LlmSettingsUiAction.Back))
    }

    @Test
    fun recreatingViewModelReadsPersistedValues() {
        val repository = FakeRepository()
        val first = LlmSettingsViewModel(repository)
        first.onAction(LlmSettingsUiAction.SetToneMode(AiBehaviorSettings.TONE_CHAT))
        first.onAction(LlmSettingsUiAction.SetReasoningEffort(AiBehaviorSettings.REASONING_LOW))
        first.onAction(LlmSettingsUiAction.SetThinkingScroll(false))
        first.onAction(LlmSettingsUiAction.SetSoftCompaction(false))

        val second = LlmSettingsViewModel(repository)
        assertEquals(AiBehaviorSettings.TONE_CHAT, second.state.value.toneMode)
        assertEquals(AiBehaviorSettings.REASONING_LOW, second.state.value.reasoningEffort)
        assertFalse(second.state.value.thinkingScrollEnabled)
        assertFalse(second.state.value.softCompactionEnabled)
        assertEquals(first.state.value, second.state.value)
    }

    @Test
    fun stateSurvivesRepeatedReadsLikeRecomposition() {
        val viewModel = LlmSettingsViewModel(FakeRepository())
        viewModel.onAction(LlmSettingsUiAction.SetReasoningEffort(AiBehaviorSettings.REASONING_AUTO))
        viewModel.onAction(LlmSettingsUiAction.SetLearningMode(true))

        val first = viewModel.state.value
        val second = viewModel.state.value
        assertEquals(first, second)
        assertEquals(AiBehaviorSettings.REASONING_AUTO, second.reasoningEffort)
        assertTrue(second.learningModeEnabled)
    }

    private class FakeRepository(
        initial: AiBehaviorSettings = AiBehaviorSettings(
            AiBehaviorSettings.TONE_CODING,
            true,
            false,
            AiBehaviorSettings.REASONING_MEDIUM,
            false,
            false,
            true
        )
    ) : LlmSettingsRepository {
        private var current = initial
        val writes = mutableListOf<String>()

        override fun settings(): AiBehaviorSettings = current

        override fun setToneMode(toneMode: String) {
            writes += "tone:$toneMode"
            current = copy(toneMode = toneMode)
        }

        override fun setReasoningEffort(effort: String) {
            writes += "reasoning:$effort"
            current = copy(reasoningEffort = effort)
        }

        override fun setThinkingScrollEnabled(enabled: Boolean) {
            writes += "scroll:$enabled"
            current = copy(thinkingScrollEnabled = enabled)
        }

        override fun setThinkingAutoExpandEnabled(enabled: Boolean) {
            writes += "autoExpand:$enabled"
            current = copy(thinkingAutoExpandEnabled = enabled)
        }

        override fun setPreserveReasoningEnabled(enabled: Boolean) {
            writes += "preserve:$enabled"
            current = copy(preserveReasoningEnabled = enabled)
        }

        override fun setLearningModeEnabled(enabled: Boolean) {
            writes += "learning:$enabled"
            current = copy(learningModeEnabled = enabled)
        }

        override fun setSoftCompactionEnabled(enabled: Boolean) {
            writes += "soft:$enabled"
            current = copy(softCompactionEnabled = enabled)
        }

        private fun copy(
            toneMode: String = current.toneMode,
            thinkingScrollEnabled: Boolean = current.isThinkingScrollEnabled,
            thinkingAutoExpandEnabled: Boolean = current.isThinkingAutoExpandEnabled,
            reasoningEffort: String = current.reasoningEffort,
            preserveReasoningEnabled: Boolean = current.isPreserveReasoningEnabled,
            learningModeEnabled: Boolean = current.isLearningModeEnabled,
            softCompactionEnabled: Boolean = current.isSoftCompactionEnabled
        ): AiBehaviorSettings {
            return AiBehaviorSettings(
                toneMode,
                thinkingScrollEnabled,
                thinkingAutoExpandEnabled,
                reasoningEffort,
                preserveReasoningEnabled,
                learningModeEnabled,
                softCompactionEnabled
            )
        }
    }
}
