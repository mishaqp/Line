package cn.lineai.ui.component

import cn.lineai.ipc.IpcProviderConfig
import cn.lineai.ipc.IpcProviderType
import cn.lineai.ipc.ScannedProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalProvidersControllerRepositoryTest {
    @Test
    fun snapshotMapsFreshGatewayDataAndPreservesOrder() {
        val terminalOne = config("one", "One", "pkg.one", enabled = true)
        val nonTerminal = IpcProviderConfig.builder()
            .id("other")
            .providerType("other")
            .name("Other")
            .packageName("pkg.other")
            .serviceClass("OtherService")
            .enabled(true)
            .build()
        val terminalTwo = config("two", "Two", "pkg.two", enabled = false)
        val firstScan = ScannedProvider("scan.one", "ServiceOne", "Scan One", "terminal")
        val secondScan = ScannedProvider("scan.two", "ServiceTwo", "Scan Two", "terminal")
        val gateway = FakeGateway(
            installedValue = listOf(terminalOne, nonTerminal, terminalTwo),
            scanValue = listOf(firstScan, secondScan),
            hasScannedValue = true
        )
        val repository = TerminalProvidersControllerRepository(gateway)

        val snapshot = repository.snapshot()

        assertEquals(listOf("one", "two"), snapshot.installed.map { it.id })
        assertEquals(listOf("scan.one", "scan.two"), snapshot.scanResults.map { it.packageName })
        assertTrue(snapshot.hasScanned)
        assertTrue(snapshot.installed.first().enabled)
        assertFalse(snapshot.installed.last().enabled)
    }

    @Test
    fun scanDelegatesOnceThenReadsUpdatedGatewayState() {
        val gateway = FakeGateway()
        gateway.onScan = {
            gateway.hasScannedValue = true
            gateway.scanValue = listOf(
                ScannedProvider("pkg.scan", "ScanService", "Scanned", "terminal")
            )
        }
        val repository = TerminalProvidersControllerRepository(gateway)

        val snapshot = repository.scan()

        assertEquals(1, gateway.scanCalls)
        assertTrue(snapshot.hasScanned)
        assertEquals("pkg.scan", snapshot.scanResults.single().packageName)
    }

    @Test
    fun addBuildsExactTerminalConfigAndDelegatesExistingSaveOperation() {
        val gateway = FakeGateway()
        val repository = TerminalProvidersControllerRepository(gateway)

        repository.addProvider(
            providerType = "unexpected-input",
            name = "Terminal App",
            packageName = "pkg.term",
            serviceClass = "TerminalService",
            enabled = true
        )

        assertEquals(1, gateway.saveCalls)
        val saved = gateway.lastSaved!!
        assertEquals(IpcProviderType.TERMINAL.id, saved.providerType)
        assertEquals("Terminal App", saved.name)
        assertEquals("pkg.term", saved.packageName)
        assertEquals("TerminalService", saved.serviceClass)
        assertTrue(saved.isEnabled)
        assertEquals("", saved.id)
    }

    @Test
    fun toggleAndDeleteDelegateOnlyToLegacyGatewayOperations() {
        val gateway = FakeGateway()
        val repository = TerminalProvidersControllerRepository(gateway)

        repository.setProviderEnabled("p1", false)
        repository.deleteProvider("p1")

        assertEquals(1, gateway.setEnabledCalls)
        assertEquals("p1" to false, gateway.lastEnabled)
        assertEquals(1, gateway.deleteCalls)
        assertEquals("p1", gateway.lastDeletedId)
        assertEquals(0, gateway.saveCalls)
    }

    private class FakeGateway(
        var installedValue: List<IpcProviderConfig> = emptyList(),
        var scanValue: List<ScannedProvider> = emptyList(),
        var hasScannedValue: Boolean = false
    ) : TerminalProvidersLegacyGateway {
        var scanCalls = 0
        var saveCalls = 0
        var setEnabledCalls = 0
        var deleteCalls = 0
        var lastSaved: IpcProviderConfig? = null
        var lastEnabled: Pair<String, Boolean>? = null
        var lastDeletedId: String? = null
        var onScan: (() -> Unit)? = null

        override fun installedProviders(): List<IpcProviderConfig> = installedValue

        override fun scanResults(): List<ScannedProvider> = scanValue

        override fun hasScanned(): Boolean = hasScannedValue

        override fun scanProviders() {
            scanCalls++
            onScan?.invoke()
        }

        override fun saveProvider(config: IpcProviderConfig) {
            saveCalls++
            lastSaved = config
        }

        override fun setProviderEnabled(providerId: String, enabled: Boolean) {
            setEnabledCalls++
            lastEnabled = providerId to enabled
        }

        override fun deleteProvider(providerId: String) {
            deleteCalls++
            lastDeletedId = providerId
        }
    }

    companion object {
        private fun config(
            id: String,
            name: String,
            packageName: String,
            enabled: Boolean
        ): IpcProviderConfig = IpcProviderConfig.builder()
            .id(id)
            .providerType(IpcProviderType.TERMINAL.id)
            .name(name)
            .packageName(packageName)
            .serviceClass("${name}Service")
            .enabled(enabled)
            .build()
    }
}
