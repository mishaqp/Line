package cn.lineai.ui.model

import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxIntegrationViewModelTest {

    @Test
    fun creatingViewModelDoesNotStartSetupOrEmitEffects() {
        val repository = RecordingRepository()
        val viewModel = TermuxIntegrationViewModel(repository, QueuedDispatcher())

        assertEquals(GRANT_COMMAND, viewModel.state.value.grantCommand)
        assertFalse(viewModel.state.value.isSetupRunning)
        assertEquals(TermuxIntegrationStatus.NONE, viewModel.state.value.status)
        assertEquals(0, repository.setupCalls)
        assertNull(viewModel.onAction(TermuxIntegrationUiAction.Reload))
    }

    @Test
    fun copyCreatesOneEffectAndRecompositionDoesNotRepeatIt() {
        val viewModel = TermuxIntegrationViewModel(RecordingRepository(), QueuedDispatcher())

        val first = viewModel.onAction(TermuxIntegrationUiAction.CopyGrantCommand)
        assertTrue(first is TermuxIntegrationUiEffect.CopyToClipboard)
        assertEquals(
            GRANT_COMMAND,
            (first as TermuxIntegrationUiEffect.CopyToClipboard).command
        )
        assertEquals(TermuxIntegrationStatus.COPIED, viewModel.state.value.status)
        assertNull(viewModel.onAction(TermuxIntegrationUiAction.Reload))
        assertEquals(TermuxIntegrationStatus.COPIED, viewModel.state.value.status)
    }

    @Test
    fun permissionCreatesOneEffectAndReportsRequestedOrUnavailable() {
        val viewModel = TermuxIntegrationViewModel(RecordingRepository(), QueuedDispatcher())

        assertEquals(
            TermuxIntegrationUiEffect.RequestRunCommandPermission,
            viewModel.onAction(TermuxIntegrationUiAction.RequestRunCommandPermission)
        )
        assertEquals(TermuxIntegrationStatus.NONE, viewModel.state.value.status)

        viewModel.onAction(TermuxIntegrationUiAction.PermissionRequested)
        assertEquals(TermuxIntegrationStatus.PERMISSION_REQUESTED, viewModel.state.value.status)

        viewModel.onAction(TermuxIntegrationUiAction.PermissionUnavailable)
        assertEquals(TermuxIntegrationStatus.PERMISSION_UNAVAILABLE, viewModel.state.value.status)
        assertNull(viewModel.onAction(TermuxIntegrationUiAction.Reload))
    }

    @Test
    fun openTermuxCreatesOneEffectAndMapsSuccessAndFailure() {
        val repository = RecordingRepository()
        val viewModel = TermuxIntegrationViewModel(repository, QueuedDispatcher())

        assertEquals(
            TermuxIntegrationUiEffect.OpenTermux,
            viewModel.onAction(TermuxIntegrationUiAction.OpenTermux)
        )
        viewModel.onAction(TermuxIntegrationUiAction.TermuxOpened)
        assertEquals(TermuxIntegrationStatus.TERMUX_OPENED, viewModel.state.value.status)

        viewModel.onAction(
            TermuxIntegrationUiAction.TermuxOpenFailed(
                "LINEAI_PRIVATE_KEY_BEGIN\nSECRET\nLINEAI_PRIVATE_KEY_END"
            )
        )
        assertEquals(TermuxIntegrationStatus.TERMUX_OPEN_FAILED, viewModel.state.value.status)
        assertEquals(REDACTED, viewModel.state.value.error)
        assertFalse(viewModel.state.value.error.contains("SECRET"))
        assertFalse(viewModel.state.value.toString().contains("SECRET"))
    }

    @Test
    fun startSetupSetsRunningImmediatelyAndIgnoresSecondTap() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository()
        repository.outcome = TermuxSetupOutcome.Success("bash", "/data/rc", "ok")
        val viewModel = TermuxIntegrationViewModel(repository, dispatcher)

        viewModel.onAction(TermuxIntegrationUiAction.StartSetup)
        viewModel.onAction(TermuxIntegrationUiAction.StartSetup)

        assertTrue(viewModel.state.value.isSetupRunning)
        assertEquals(TermuxIntegrationStatus.SETUP_RUNNING, viewModel.state.value.status)
        assertEquals(1, dispatcher.queued)
        assertEquals(0, repository.setupCalls)

        dispatcher.runAll()

        assertEquals(1, repository.setupCalls)
        assertEquals(TermuxIntegrationViewModel.SETUP_TIMEOUT_MS, repository.lastTimeout)
        assertEquals(900000, repository.lastTimeout)
        assertFalse(viewModel.state.value.isSetupRunning)
        assertEquals(TermuxIntegrationStatus.SETUP_SUCCESS, viewModel.state.value.status)
        assertEquals("bash", viewModel.state.value.shell)
        assertEquals("/data/rc", viewModel.state.value.rcPath)
        assertEquals("ok", viewModel.state.value.output)
    }

    @Test
    fun emptyShellAndRcPathStayAsRepositoryValuesAndUnknownFallbackIsPreserved() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository()
        repository.outcome = TermuxSetupOutcome.Success("unknown", "unknown", "safe")
        val viewModel = TermuxIntegrationViewModel(repository, dispatcher)

        viewModel.onAction(TermuxIntegrationUiAction.StartSetup)
        dispatcher.runAll()

        assertEquals("unknown", viewModel.state.value.shell)
        assertEquals("unknown", viewModel.state.value.rcPath)
    }

    @Test
    fun setupFailureUnlocksButtonAndKeepsSafeError() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository()
        repository.outcome = TermuxSetupOutcome.Failure("connection refused")
        val viewModel = TermuxIntegrationViewModel(repository, dispatcher)

        viewModel.onAction(TermuxIntegrationUiAction.StartSetup)
        dispatcher.runAll()

        assertFalse(viewModel.state.value.isSetupRunning)
        assertEquals(TermuxIntegrationStatus.SETUP_FAILED, viewModel.state.value.status)
        assertEquals("connection refused", viewModel.state.value.error)
    }

    @Test
    fun unexpectedRepositoryExceptionIsFailureNotRunning() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository()
        repository.throwOnSetup = IllegalStateException("boom")
        val viewModel = TermuxIntegrationViewModel(repository, dispatcher)

        viewModel.onAction(TermuxIntegrationUiAction.StartSetup)
        dispatcher.runAll()

        assertFalse(viewModel.state.value.isSetupRunning)
        assertEquals(TermuxIntegrationStatus.SETUP_FAILED, viewModel.state.value.status)
        assertEquals("boom", viewModel.state.value.error)
    }

    @Test
    fun cancellationDoesNotBecomeFailure() {
        val dispatcher = QueuedDispatcher()
        val repository = RecordingRepository()
        repository.throwOnSetup = java.util.concurrent.CancellationException("cancelled")
        val viewModel = TermuxIntegrationViewModel(repository, dispatcher)

        viewModel.onAction(TermuxIntegrationUiAction.StartSetup)
        dispatcher.runAll()

        assertTrue(viewModel.state.value.isSetupRunning)
        assertEquals(TermuxIntegrationStatus.SETUP_RUNNING, viewModel.state.value.status)
        assertEquals("", viewModel.state.value.error)
    }

    @Test
    fun backDoesNotTouchBackendAndAttachReloadDoesNotRepeatLastAction() {
        val repository = RecordingRepository()
        val viewModel = TermuxIntegrationViewModel(repository, QueuedDispatcher())

        assertEquals(
            TermuxIntegrationUiEffect.CopyToClipboard(GRANT_COMMAND),
            viewModel.onAction(TermuxIntegrationUiAction.CopyGrantCommand)
        )
        assertEquals(TermuxIntegrationUiEffect.Back, viewModel.onAction(TermuxIntegrationUiAction.Back))
        assertNull(viewModel.onAction(TermuxIntegrationUiAction.Reload))
        assertEquals(0, repository.setupCalls)
        assertEquals(TermuxIntegrationStatus.COPIED, viewModel.state.value.status)
    }

    @Test
    fun uiStateDoesNotContainRawPrivateKey() {
        val viewModel = TermuxIntegrationViewModel(RecordingRepository(), QueuedDispatcher())
        viewModel.onAction(
            TermuxIntegrationUiAction.TermuxOpenFailed(
                "LINEAI_PRIVATE_KEY_BEGIN\n-----BEGIN OPENSSH PRIVATE KEY-----\nabc\nLINEAI_PRIVATE_KEY_END"
            )
        )
        val rendered = viewModel.state.value.toString()
        assertFalse(rendered.contains("BEGIN OPENSSH"))
        assertFalse(rendered.contains("abc"))
        assertFalse(viewModel.state.value.error.contains("BEGIN OPENSSH"))
        assertNotNull(viewModel.state.value.grantCommand)
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

    private class RecordingRepository : TermuxIntegrationRepository {
        var setupCalls = 0
        var lastTimeout: Int = -1
        var outcome: TermuxSetupOutcome = TermuxSetupOutcome.Success("sh", "/rc", "out")
        var throwOnSetup: Exception? = null

        override fun grantCommand(): String = GRANT_COMMAND

        override fun setupAndTest(timeoutMs: Int): TermuxSetupOutcome {
            setupCalls++
            lastTimeout = timeoutMs
            throwOnSetup?.let { throw it }
            return outcome
        }

        override fun redact(value: String?): String {
            if (value.isNullOrEmpty()) {
                return ""
            }
            return PRIVATE_KEY.replace(value, REDACTED)
        }

        override fun valueOrUnknown(value: String?): String {
            return if (value.isNullOrEmpty()) "unknown" else value
        }
    }

    companion object {
        private const val GRANT_COMMAND = "mkdir -p ~/.termux"
        private const val REDACTED = "LINEAI_PRIVATE_KEY=[saved to SSH Private key]"
        private val PRIVATE_KEY = Regex("LINEAI_PRIVATE_KEY_BEGIN[\\s\\S]*?LINEAI_PRIVATE_KEY_END")
    }
}
