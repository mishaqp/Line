package cn.lineai.ui.model

import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DataSettingsViewModelTest {

    @Test
    fun initialStateIsExposedThroughStateFlow() {
        val viewModel = DataSettingsViewModel(FakeRepository())

        assertTrue(viewModel.state.value.actionsEnabled)
        assertSame(viewModel.state.value, viewModel.state.value)
    }

    @Test
    fun exportActionDelegatesExactlyOnce() {
        val repository = FakeRepository()
        val viewModel = DataSettingsViewModel(repository)

        viewModel.onAction(DataSettingsUiAction.ExportAll)

        assertTrue(repository.exportCalls == 1)
        assertTrue(repository.importCalls == 0)
    }

    @Test
    fun importActionDelegatesExactlyOnce() {
        val repository = FakeRepository()
        val viewModel = DataSettingsViewModel(repository)

        viewModel.onAction(DataSettingsUiAction.ImportLineCode)

        assertTrue(repository.exportCalls == 0)
        assertTrue(repository.importCalls == 1)
    }

    @Test
    fun backDoesNotStartArchiveOperation() {
        val repository = FakeRepository()
        val viewModel = DataSettingsViewModel(repository)

        viewModel.onAction(DataSettingsUiAction.Back)

        assertTrue(repository.exportCalls == 0)
        assertTrue(repository.importCalls == 0)
    }

    private class FakeRepository : DataSettingsRepository {
        var exportCalls = 0
        var importCalls = 0

        override fun exportAll() {
            exportCalls += 1
        }

        override fun importLineCode() {
            importCalls += 1
        }
    }
}
