package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandViewModelTest {

    @Test
    fun initialStateKeepsExactCommand() {
        val viewModel = ShellCommandViewModel(
            RecordingRepository(ShellCommandSnapshot(command = "pwd", hasCommand = true))
        )
        assertEquals("pwd", viewModel.state.value.command)
        assertTrue(viewModel.state.value.hasCommand)
    }

    @Test
    fun leadingTrailingWhitespaceAndNewlinesArePreserved() {
        val raw = "  pwd\nid  \n"
        val viewModel = ShellCommandViewModel(
            RecordingRepository(ShellCommandSnapshot(command = raw, hasCommand = true))
        )
        assertEquals(raw, viewModel.state.value.command)
        assertTrue(viewModel.state.value.hasCommand)
    }

    @Test
    fun nullAndEmptySnapshotsAreEmptyState() {
        val empty = ShellCommandViewModel(
            RecordingRepository(ShellCommandSnapshot(command = "", hasCommand = false))
        )
        assertEquals("", empty.state.value.command)
        assertFalse(empty.state.value.hasCommand)
    }

    @Test
    fun whitespaceOnlyCommandIsNotEmpty() {
        val viewModel = ShellCommandViewModel(
            RecordingRepository(ShellCommandSnapshot(command = "   ", hasCommand = true))
        )
        assertEquals("   ", viewModel.state.value.command)
        assertTrue(viewModel.state.value.hasCommand)
    }

    @Test
    fun reloadReadsFreshSnapshotWithoutBack() {
        val repository = RecordingRepository(
            ShellCommandSnapshot(command = "pwd", hasCommand = true)
        )
        val viewModel = ShellCommandViewModel(repository)
        repository.snapshotValue = ShellCommandSnapshot(command = "id", hasCommand = true)

        assertNull(viewModel.onAction(ShellCommandUiAction.Reload))
        assertEquals("id", viewModel.state.value.command)
        assertTrue(viewModel.state.value.hasCommand)
        assertEquals(2, repository.snapshotCalls)
    }

    @Test
    fun backReturnsOneEffectPerAction() {
        val viewModel = ShellCommandViewModel(
            RecordingRepository(ShellCommandSnapshot(command = "pwd", hasCommand = true))
        )
        assertEquals(ShellCommandUiEffect.Back, viewModel.onAction(ShellCommandUiAction.Back))
        assertEquals(ShellCommandUiEffect.Back, viewModel.onAction(ShellCommandUiAction.Back))
        assertNull(viewModel.onAction(ShellCommandUiAction.Reload))
        assertEquals("pwd", viewModel.state.value.command)
    }

    @Test
    fun repositoryExceptionYieldsSafeEmptyState() {
        val viewModel = ShellCommandViewModel(object : ShellCommandRepository {
            override fun snapshot(): ShellCommandSnapshot {
                throw IllegalStateException("boom")
            }
        })
        assertEquals("", viewModel.state.value.command)
        assertFalse(viewModel.state.value.hasCommand)
        assertEquals(ShellCommandUiEffect.Back, viewModel.onAction(ShellCommandUiAction.Back))
    }

    @Test
    fun snapshotAndUiStateToStringOmitSecretToken() {
        val secret = "SECRET_TOKEN_FOR_TEST"
        val snapshot = ShellCommandSnapshot(command = secret, hasCommand = true)
        val viewModel = ShellCommandViewModel(RecordingRepository(snapshot))
        assertFalse(snapshot.toString().contains(secret))
        assertFalse(viewModel.state.value.toString().contains(secret))
        assertTrue(snapshot.toString().contains("hasCommand=true"))
        assertTrue(viewModel.state.value.toString().contains("commandLength=${secret.length}"))
    }

    @Test
    fun viewModelClassDoesNotExposeAndroidTypes() {
        val fields = ShellCommandViewModel::class.java.declaredFields.map { it.type.name }
        assertFalse(fields.any { it.startsWith("android.") })
        assertFalse(
            ShellCommandViewModel::class.java.declaredMethods.any { method ->
                method.parameterTypes.any { it.name.startsWith("android.") } ||
                    method.returnType.name.startsWith("android.")
            }
        )
    }

    private class RecordingRepository(
        var snapshotValue: ShellCommandSnapshot = ShellCommandSnapshot()
    ) : ShellCommandRepository {
        var snapshotCalls = 0

        override fun snapshot(): ShellCommandSnapshot {
            snapshotCalls += 1
            return snapshotValue
        }
    }
}
