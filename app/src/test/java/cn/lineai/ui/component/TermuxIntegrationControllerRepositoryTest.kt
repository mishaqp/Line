package cn.lineai.ui.component

import cn.lineai.model.SshConfig
import cn.lineai.ui.model.TermuxSetupOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxIntegrationControllerRepositoryTest {

    @Test
    fun setupThenTestUsesExactTimeoutAndSameConfig() {
        val config = SshConfig("127.0.0.1", 8022, "u0", "", "SECRET_KEY_MATERIAL", "")
        val gateway = RecordingGateway(
            TermuxRawSetup(config, "bash", "/data/data/com.termux/files/home/.bashrc", "raw")
        )
        gateway.testResult = "sshd running"
        val repository = repository(gateway)

        val outcome = repository.setupAndTest(900000)

        assertTrue(outcome is TermuxSetupOutcome.Success)
        val success = outcome as TermuxSetupOutcome.Success
        assertEquals(1, gateway.setupCalls)
        assertEquals(1, gateway.testCalls)
        assertEquals(900000, gateway.lastTimeout)
        assertSame(config, gateway.lastTested)
        assertEquals("bash", success.shell)
        assertEquals("/data/data/com.termux/files/home/.bashrc", success.rcPath)
        assertEquals("sshd running", success.output)
        assertFalse(success.toString().contains("SECRET_KEY_MATERIAL"))
    }

    @Test
    fun emptyShellAndRcPathBecomeUnknown() {
        val gateway = RecordingGateway(
            TermuxRawSetup(SshConfig.defaultConfig(), "", "", "ok")
        )
        val outcome = repository(gateway).setupAndTest(15 * 60 * 1000)

        val success = outcome as TermuxSetupOutcome.Success
        assertEquals("unknown", success.shell)
        assertEquals("unknown", success.rcPath)
    }

    @Test
    fun multilinePrivateKeyBlockAndMarkersAreRemoved() {
        val gateway = RecordingGateway(
            TermuxRawSetup(SshConfig.defaultConfig(), "zsh", "/rc", "")
        )
        gateway.testResult = """
            before
            LINEAI_PRIVATE_KEY_BEGIN
            -----BEGIN OPENSSH PRIVATE KEY-----
            abcdef
            -----END OPENSSH PRIVATE KEY-----
            LINEAI_PRIVATE_KEY_END
            after
        """.trimIndent()

        val success = repository(gateway).setupAndTest(900000) as TermuxSetupOutcome.Success

        assertFalse(success.output.contains("BEGIN OPENSSH"))
        assertFalse(success.output.contains("abcdef"))
        assertFalse(success.output.contains("LINEAI_PRIVATE_KEY_BEGIN"))
        assertFalse(success.output.contains("LINEAI_PRIVATE_KEY_END"))
        assertTrue(success.output.contains("before"))
        assertTrue(success.output.contains("after"))
        assertTrue(success.output.contains(REDACT_REPLACEMENT))
    }

    @Test
    fun errorsAreRedactedAndDoNotLeakKey() {
        val gateway = RecordingGateway(
            TermuxRawSetup(SshConfig.defaultConfig(), "sh", "/rc", "")
        )
        gateway.setupError = IllegalStateException(
            "failed\nLINEAI_PRIVATE_KEY_BEGIN\nPRIVATE_KEY_PAYLOAD\nLINEAI_PRIVATE_KEY_END"
        )

        val failure = repository(gateway).setupAndTest(900000) as TermuxSetupOutcome.Failure

        assertEquals(1, gateway.setupCalls)
        assertEquals(0, gateway.testCalls)
        assertFalse(failure.message.contains("PRIVATE_KEY_PAYLOAD"))
        assertFalse(failure.message.contains("LINEAI_PRIVATE_KEY_BEGIN"))
        assertFalse(failure.message.contains("LINEAI_PRIVATE_KEY_END"))
        assertTrue(failure.message.contains(REDACT_REPLACEMENT))
    }

    @Test
    fun grantCommandIsTheInjectedExactText() {
        val gateway = RecordingGateway(
            TermuxRawSetup(SshConfig.defaultConfig(), "sh", "/rc", "")
        )
        val command = "mkdir -p ~/.termux\nallow-external-apps=true"
        val repository = TermuxIntegrationControllerRepository(
            gateway,
            command,
            REDACT_REPLACEMENT
        )
        assertEquals(command, repository.grantCommand())
        assertEquals(0, gateway.setupCalls)
    }

    @Test
    fun valueOrUnknownMatchesLegacyFallback() {
        val repository = repository(
            RecordingGateway(TermuxRawSetup(SshConfig.defaultConfig(), "sh", "/rc", ""))
        )
        assertEquals("unknown", repository.valueOrUnknown(null))
        assertEquals("unknown", repository.valueOrUnknown(""))
        assertEquals("zsh", repository.valueOrUnknown("zsh"))
    }

    private fun repository(gateway: RecordingGateway): TermuxIntegrationControllerRepository {
        return TermuxIntegrationControllerRepository(
            gateway,
            "grant",
            REDACT_REPLACEMENT
        )
    }

    private class RecordingGateway(
        var setup: TermuxRawSetup
    ) : TermuxIntegrationLegacyGateway {
        var setupCalls = 0
        var testCalls = 0
        var lastTimeout: Int = -1
        var lastTested: SshConfig? = null
        var testResult: String = ""
        var setupError: Exception? = null

        override fun setupTermuxSsh(timeoutMs: Int): TermuxRawSetup {
            setupCalls++
            lastTimeout = timeoutMs
            setupError?.let { throw it }
            return setup
        }

        override fun testConnection(config: SshConfig): String {
            testCalls++
            lastTested = config
            return testResult
        }
    }

    companion object {
        private const val REDACT_REPLACEMENT = "LINEAI_PRIVATE_KEY=[saved to SSH Private key]"
    }
}
