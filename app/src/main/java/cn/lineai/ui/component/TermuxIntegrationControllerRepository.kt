package cn.lineai.ui.component

import cn.lineai.model.SshConfig
import cn.lineai.ui.model.TermuxIntegrationRepository
import cn.lineai.ui.model.TermuxSetupOutcome
import java.util.regex.Pattern

data class TermuxRawSetup(
    val config: SshConfig,
    val shell: String,
    val rcPath: String,
    val output: String
)

interface TermuxIntegrationLegacyGateway {
    @Throws(Exception::class)
    fun setupTermuxSsh(timeoutMs: Int): TermuxRawSetup

    @Throws(Exception::class)
    fun testConnection(config: SshConfig): String
}

class TermuxIntegrationControllerRepository(
    private val gateway: TermuxIntegrationLegacyGateway,
    private val grantCommand: String,
    private val redactReplacement: String
) : TermuxIntegrationRepository {

    override fun grantCommand(): String = grantCommand

    override fun setupAndTest(timeoutMs: Int): TermuxSetupOutcome {
        return try {
            val setup = gateway.setupTermuxSsh(timeoutMs)
            val testOutput = gateway.testConnection(setup.config)
            TermuxSetupOutcome.Success(
                shell = valueOrUnknown(setup.shell),
                rcPath = valueOrUnknown(setup.rcPath),
                output = redact(testOutput)
            )
        } catch (error: Exception) {
            TermuxSetupOutcome.Failure(redact(describe(error)))
        }
    }

    override fun redact(value: String?): String {
        val source = value ?: ""
        val replaced = PRIVATE_KEY_PATTERN.matcher(source).replaceAll(redactReplacement)
        return MARKER_PATTERN.matcher(replaced).replaceAll(redactReplacement)
    }

    override fun valueOrUnknown(value: String?): String {
        return if (value.isNullOrEmpty()) UNKNOWN_FALLBACK else value
    }

    private fun describe(error: Exception): String {
        val message = error.message
        if (!message.isNullOrBlank()) {
            return message.trim()
        }
        val name = error.javaClass.simpleName
        return if (name.isEmpty()) UNKNOWN_ERROR else name
    }

    companion object {
        const val UNKNOWN_FALLBACK: String = "unknown"
        const val UNKNOWN_ERROR: String = "未知错误"

        private val PRIVATE_KEY_PATTERN: Pattern = Pattern.compile(
            "LINEAI_PRIVATE_KEY_BEGIN[\\s\\S]*?LINEAI_PRIVATE_KEY_END"
        )
        private val MARKER_PATTERN: Pattern = Pattern.compile(
            "LINEAI_PRIVATE_KEY_BEGIN|LINEAI_PRIVATE_KEY_END"
        )
    }
}
