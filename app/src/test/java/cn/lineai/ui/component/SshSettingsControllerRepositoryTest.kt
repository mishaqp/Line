package cn.lineai.ui.component

import cn.lineai.model.SshConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class SshSettingsControllerRepositoryTest {

    @Test
    fun loadReturnsFreshGatewayState() {
        val first = SshConfig("a.example", 22, "one", "pw", "", "")
        val gateway = RecordingGateway(first)
        val repository = SshSettingsControllerRepository(gateway)

        val firstRead = repository.load()
        gateway.stored = SshConfig("b.example", 2200, "two", "", "KEY", "ph")
        val secondRead = repository.load()

        assertEquals("a.example", firstRead.host)
        assertEquals("b.example", secondRead.host)
        assertEquals(2200, secondRead.port)
        assertEquals("KEY", secondRead.privateKey)
        assertEquals(2, gateway.loadCalls)
        assertEquals(0, gateway.saveCalls)
        assertEquals(0, gateway.testCalls)
        assertSame(gateway.stored, secondRead)
    }

    @Test
    fun saveAndTestDelegateExactConfigOnce() {
        val gateway = RecordingGateway(SshConfig.defaultConfig())
        val repository = SshSettingsControllerRepository(gateway)
        val config = SshConfig("host.test", 8022, "user", "pw", "KEY", "ph")
        gateway.testResult = "ok"

        repository.save(config)
        val output = repository.testConnection(config)

        assertEquals(1, gateway.saveCalls)
        assertEquals(1, gateway.testCalls)
        assertSame(config, gateway.lastSaved)
        assertSame(config, gateway.lastTested)
        assertEquals("ok", output)
    }

    @Test
    fun adapterDoesNotInventFields() {
        val original = SshConfig("keep.host", 9, "keep", "", "", "")
        val gateway = RecordingGateway(original)

        val loaded = SshSettingsControllerRepository(gateway).load()

        assertEquals("keep.host", loaded.host)
        assertEquals(9, loaded.port)
        assertEquals("keep", loaded.username)
        assertEquals("", loaded.password)
        assertEquals("", loaded.privateKey)
        assertEquals("", loaded.passphrase)
    }

    private class RecordingGateway(
        var stored: SshConfig
    ) : SshSettingsLegacyGateway {
        var loadCalls = 0
        var saveCalls = 0
        var testCalls = 0
        var lastSaved: SshConfig? = null
        var lastTested: SshConfig? = null
        var testResult: String = ""

        override fun loadConfig(): SshConfig {
            loadCalls++
            return stored
        }

        override fun saveConfig(config: SshConfig) {
            saveCalls++
            lastSaved = config
            stored = config
        }

        override fun testConnection(config: SshConfig): String {
            testCalls++
            lastTested = config
            return testResult
        }
    }
}
