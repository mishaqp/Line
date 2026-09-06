package cn.lineai.ui.model

import cn.lineai.model.SshConfig
import cn.lineai.navigation.LineDestination
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SshSettingsViewModelTest {

    @Test
    fun initialReadDoesNotWrite() {
        val repository = RecordingRepository(config("10.0.0.2", 22, "root", "secret", "KEY", "phrase"))

        val viewModel = SshSettingsViewModel(repository, QueuedDispatcher())

        assertEquals("10.0.0.2", viewModel.state.value.host)
        assertEquals("22", viewModel.state.value.port)
        assertEquals("root", viewModel.state.value.username)
        assertEquals("secret", viewModel.state.value.password)
        assertEquals("KEY", viewModel.state.value.privateKey)
        assertEquals("phrase", viewModel.state.value.passphrase)
        assertFalse(viewModel.state.value.dirty)
        assertEquals(1, repository.loadCalls)
        assertEquals(0, repository.saveCalls)
        assertEquals(0, repository.testCalls)
        assertFalse(viewModel.state.value.toString().contains("secret"))
        assertFalse(viewModel.state.value.toString().contains("phrase"))
        assertFalse(viewModel.state.value.toString().contains("KEY"))
    }

    @Test
    fun draftEditsStayUntilSave() {
        val repository = RecordingRepository(SshConfig.defaultConfig())
        val viewModel = SshSettingsViewModel(repository, QueuedDispatcher())

        viewModel.onAction(SshSettingsUiAction.SetHost("example.test"))
        viewModel.onAction(SshSettingsUiAction.SetPort("2222"))
        viewModel.onAction(SshSettingsUiAction.SetUsername("alice"))
        viewModel.onAction(SshSettingsUiAction.SetPassword("pw"))
        viewModel.onAction(SshSettingsUiAction.SetPrivateKey("PRIV"))
        viewModel.onAction(SshSettingsUiAction.SetPassphrase("ph"))

        assertTrue(viewModel.state.value.dirty)
        assertEquals(0, repository.saveCalls)
        assertEquals("example.test", viewModel.state.value.host)
        assertEquals("2222", viewModel.state.value.port)

        viewModel.onAction(SshSettingsUiAction.Save)

        assertEquals(1, repository.saveCalls)
        assertEquals("example.test", repository.lastSaved!!.host)
        assertEquals(2222, repository.lastSaved!!.port)
        assertEquals("alice", repository.lastSaved!!.username)
        assertEquals("pw", repository.lastSaved!!.password)
        assertEquals("PRIV", repository.lastSaved!!.privateKey)
        assertEquals("ph", repository.lastSaved!!.passphrase)
        assertFalse(viewModel.state.value.dirty)
        assertEquals(SshConnectionStatus.SAVED, viewModel.state.value.status)
    }

    @Test
    fun invalidPortFallsBackToDefaultOnSave() {
        val repository = RecordingRepository(SshConfig.defaultConfig())
        val viewModel = SshSettingsViewModel(repository, QueuedDispatcher())

        viewModel.onAction(SshSettingsUiAction.SetPort("nope"))
        viewModel.onAction(SshSettingsUiAction.Save)

        assertEquals(SshConfig.DEFAULT_PORT, repository.lastSaved!!.port)
        assertEquals(SshConfig.DEFAULT_PORT.toString(), viewModel.state.value.port)
    }

    @Test
    fun dirtyReloadKeepsDraftAndCleanReloadReadsRepository() {
        val repository = RecordingRepository(config("old.host", 8022, "old", "", "", ""))
        val viewModel = SshSettingsViewModel(repository, QueuedDispatcher())

        viewModel.onAction(SshSettingsUiAction.SetHost("draft.host"))
        repository.stored = config("fresh.host", 22, "fresh", "hidden", "", "")
        assertNull(viewModel.onAction(SshSettingsUiAction.Reload))

        assertEquals("draft.host", viewModel.state.value.host)
        assertTrue(viewModel.state.value.dirty)
        assertEquals(1, repository.loadCalls)

        viewModel.onAction(SshSettingsUiAction.Save)
        repository.stored = config("next.host", 2200, "next", "x", "", "")
        viewModel.onAction(SshSettingsUiAction.Reload)

        assertEquals("next.host", viewModel.state.value.host)
        assertEquals("2200", viewModel.state.value.port)
        assertEquals("next", viewModel.state.value.username)
        assertEquals("x", viewModel.state.value.password)
        assertFalse(viewModel.state.value.dirty)
    }

    @Test
    fun testSavesFirstThenUsesIoDispatcherAndBlocksDoubleStart() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository(SshConfig.defaultConfig())
        repository.testResult = "uname -a"
        val viewModel = SshSettingsViewModel(repository, dispatcher)

        viewModel.onAction(SshSettingsUiAction.SetHost("termux.local"))
        viewModel.onAction(SshSettingsUiAction.SetUsername("u0_a123"))
        viewModel.onAction(SshSettingsUiAction.SetPassword("hidden-pass"))
        viewModel.onAction(SshSettingsUiAction.TestConnection)
        viewModel.onAction(SshSettingsUiAction.TestConnection)

        assertEquals(1, repository.saveCalls)
        assertEquals(1, dispatcher.queued)
        assertTrue(viewModel.state.value.isTesting)
        assertEquals(SshConnectionStatus.TESTING, viewModel.state.value.status)
        assertEquals("termux.local", repository.lastSaved!!.host)
        assertEquals("hidden-pass", repository.lastSaved!!.password)
        assertEquals(0, repository.testCalls)

        dispatcher.runAll()

        assertEquals(1, repository.testCalls)
        assertEquals("hidden-pass", repository.lastTested!!.password)
        assertFalse(viewModel.state.value.isTesting)
        assertEquals(SshConnectionStatus.SUCCESS, viewModel.state.value.status)
        assertEquals("uname -a", viewModel.state.value.statusDetail)
        assertFalse(viewModel.state.value.dirty)
    }

    @Test
    fun testFailureKeepsDraftAndDoesNotLeakSecretsInStatus() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository(SshConfig.defaultConfig())
        repository.testError = IllegalStateException("auth failed")
        val viewModel = SshSettingsViewModel(repository, dispatcher)

        viewModel.onAction(SshSettingsUiAction.SetPassword("super-secret"))
        viewModel.onAction(SshSettingsUiAction.TestConnection)
        dispatcher.runAll()

        assertEquals("super-secret", viewModel.state.value.password)
        assertEquals(SshConnectionStatus.FAILED, viewModel.state.value.status)
        assertEquals("auth failed", viewModel.state.value.statusDetail)
        assertFalse(viewModel.state.value.toString().contains("super-secret"))
    }

    @Test
    fun blankExceptionFallsBackToLegacyUnknownLabel() {
        assertEquals(
            SshSettingsViewModel.UNKNOWN_ERROR,
            SshSettingsViewModel.describeException(IllegalStateException("  "))
        )
        assertEquals(
            "IllegalStateException",
            SshSettingsViewModel.describeException(IllegalStateException())
        )
    }

    @Test
    fun navigationEffectsAreOneShotAndNotEmittedOnCreateOrReload() {
        val viewModel = SshSettingsViewModel(RecordingRepository(SshConfig.defaultConfig()), QueuedDispatcher())

        assertNull(viewModel.onAction(SshSettingsUiAction.Reload))
        assertEquals(SshSettingsUiEffect.Back, viewModel.onAction(SshSettingsUiAction.Back))
        val termux = viewModel.onAction(SshSettingsUiAction.OpenTermuxIntegration)
        assertTrue(termux is SshSettingsUiEffect.Navigate)
        assertEquals(
            LineDestination.TermuxIntegration,
            (termux as SshSettingsUiEffect.Navigate).destination
        )
        assertNull(viewModel.onAction(SshSettingsUiAction.Reload))
    }

    @Test
    fun emptyTestOutputUsesSuccessStatusWithoutDetail() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository(SshConfig.defaultConfig())
        repository.testResult = "   "
        val viewModel = SshSettingsViewModel(repository, dispatcher)

        viewModel.onAction(SshSettingsUiAction.TestConnection)
        dispatcher.runAll()

        assertEquals(SshConnectionStatus.SUCCESS, viewModel.state.value.status)
        assertEquals("", viewModel.state.value.statusDetail)
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()
        val queued: Int get() = queue.size

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.addLast(block)
        }

        fun runAll() {
            while (queue.isNotEmpty()) {
                queue.removeFirst().run()
            }
        }
    }

    private class RecordingRepository(
        var stored: SshConfig
    ) : SshSettingsRepository {
        var loadCalls = 0
        var saveCalls = 0
        var testCalls = 0
        var lastSaved: SshConfig? = null
        var lastTested: SshConfig? = null
        var testResult: String = ""
        var testError: Exception? = null

        override fun load(): SshConfig {
            loadCalls++
            return stored
        }

        override fun save(config: SshConfig) {
            saveCalls++
            lastSaved = config
            stored = config
        }

        override fun testConnection(config: SshConfig): String {
            testCalls++
            lastTested = config
            testError?.let { throw it }
            return testResult
        }
    }

    companion object {
        private fun config(
            host: String,
            port: Int,
            username: String,
            password: String,
            privateKey: String,
            passphrase: String
        ): SshConfig = SshConfig(host, port, username, password, privateKey, passphrase)
    }
}
