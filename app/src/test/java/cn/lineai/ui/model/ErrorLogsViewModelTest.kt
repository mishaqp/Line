package cn.lineai.ui.model

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorLogsViewModelTest {

    @Test
    fun initialStateLoadsRepositorySnapshot() {
        val first = item("first.log")
        val repository = FakeRepository(mutableListOf(first))

        val state = ErrorLogsViewModel(repository).state.value

        assertEquals(listOf(first), state.logs)
        assertNull(state.message)
    }

    @Test
    fun refreshReloadsLogs() {
        val repository = FakeRepository()
        val viewModel = ErrorLogsViewModel(repository)
        val next = item("next.log")
        repository.logs += next

        viewModel.onAction(ErrorLogsUiAction.Refresh)

        assertEquals(listOf(next), viewModel.state.value.logs)
    }

    @Test
    fun clearDelegatesReloadsAndEmitsMessage() {
        val repository = FakeRepository(mutableListOf(item("old.log")))
        val viewModel = ErrorLogsViewModel(repository)

        viewModel.onAction(ErrorLogsUiAction.Clear)

        assertEquals(1, repository.clearCalls)
        assertTrue(viewModel.state.value.logs.isEmpty())
        assertEquals(ErrorLogsMessage.CLEARED, viewModel.state.value.message)
        assertEquals(1L, viewModel.state.value.messageEventId)
    }

    @Test
    fun openUsesExactFile() {
        val item = item("open.log")
        val repository = FakeRepository(mutableListOf(item), openSucceeds = true)
        val viewModel = ErrorLogsViewModel(repository)

        viewModel.onAction(ErrorLogsUiAction.Open(item))

        assertEquals(listOf(item.file), repository.openedFiles)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun failedOpenEmitsAndConsumesErrorMessage() {
        val item = item("broken.log")
        val repository = FakeRepository(mutableListOf(item), openSucceeds = false)
        val viewModel = ErrorLogsViewModel(repository)

        viewModel.onAction(ErrorLogsUiAction.Open(item))

        assertEquals(ErrorLogsMessage.OPEN_FAILED, viewModel.state.value.message)
        assertEquals(1L, viewModel.state.value.messageEventId)

        viewModel.onAction(ErrorLogsUiAction.ConsumeMessage)

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun backDoesNotMutateRepository() {
        val repository = FakeRepository()
        val viewModel = ErrorLogsViewModel(repository)

        viewModel.onAction(ErrorLogsUiAction.Back)

        assertEquals(0, repository.clearCalls)
        assertTrue(repository.openedFiles.isEmpty())
    }

    private fun item(name: String) = ErrorLogItem(
        file = File(name),
        title = name,
        subtitle = "subtitle",
        timestamp = 1L
    )

    private class FakeRepository(
        val logs: MutableList<ErrorLogItem> = mutableListOf(),
        private val openSucceeds: Boolean = true
    ) : ErrorLogsRepository {
        var clearCalls = 0
        val openedFiles = mutableListOf<File>()

        override fun loadLogs(): List<ErrorLogItem> = logs.toList()

        override fun clearLogs() {
            clearCalls += 1
            logs.clear()
        }

        override fun openLog(file: File): Boolean {
            openedFiles += file
            return openSucceeds
        }
    }
}
