package cn.lineai.ui.model

import cn.lineai.model.InputSettings
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSettingsViewModelTest {
    @Test
    fun initialStateMirrorsRepositorySettings() {
        val viewModel = InputSettingsViewModel(
            FakeRepository(InputSettings(InputSettings.ENTER_NEWLINE))
        )
        assertEquals(InputSettings.ENTER_NEWLINE, viewModel.state.value.enterKeyBehavior)
    }

    @Test
    fun changingEnterBehaviorUpdatesStateAndRepositoryImmediately() {
        val repository = FakeRepository()
        val viewModel = InputSettingsViewModel(repository)

        viewModel.onAction(InputSettingsUiAction.SetEnterKeyBehavior(InputSettings.ENTER_NEWLINE))
        assertEquals(InputSettings.ENTER_NEWLINE, viewModel.state.value.enterKeyBehavior)
        assertEquals(InputSettings.ENTER_NEWLINE, repository.settings().enterKeyBehavior)
        assertEquals(listOf("newline"), repository.writes)
    }

    @Test
    fun recreatingViewModelReloadsPersistedValue() {
        val repository = FakeRepository()
        val first = InputSettingsViewModel(repository)
        first.onAction(InputSettingsUiAction.SetEnterKeyBehavior(InputSettings.ENTER_NEWLINE))

        val second = InputSettingsViewModel(repository)
        assertEquals(InputSettings.ENTER_NEWLINE, second.state.value.enterKeyBehavior)
        assertEquals(first.state.value, second.state.value)
    }

    @Test
    fun stateSurvivesRepeatedReadsLikeRecomposition() {
        val viewModel = InputSettingsViewModel(FakeRepository())
        viewModel.onAction(InputSettingsUiAction.SetEnterKeyBehavior(InputSettings.ENTER_NEWLINE))
        val first = viewModel.state.value
        val second = viewModel.state.value
        assertEquals(first, second)
        assertEquals(InputSettings.ENTER_NEWLINE, second.enterKeyBehavior)
    }

    @Test
    fun inputParentsToSettingsAndBackDoesNotNavigate() {
        val viewModel = InputSettingsViewModel(FakeRepository())
        assertNull(viewModel.onAction(InputSettingsUiAction.Back))
        assertEquals(LineDestination.Settings, LineDestinations.parentOf(LineDestination.Input))
        assertTrue(LineDestinations.fromScreenId("input") is LineDestination.Input)
    }

    private class FakeRepository(
        initial: InputSettings = InputSettings(InputSettings.ENTER_SEND)
    ) : InputSettingsRepository {
        private var current = initial
        val writes = mutableListOf<String>()

        override fun settings(): InputSettings = current

        override fun setEnterKeyBehavior(behavior: String) {
            writes += behavior
            current = InputSettings(behavior)
        }
    }
}
