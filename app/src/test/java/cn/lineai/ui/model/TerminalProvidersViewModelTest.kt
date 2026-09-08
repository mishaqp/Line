package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalProvidersViewModelTest {
    @Test
    fun beforeFirstScanResultsRemainHidden() {
        val repository = FakeRepository(
            snapshotValue = snapshot(
                installed = listOf(installed("p1", enabled = true)),
                scanResults = emptyList(),
                hasScanned = false
            )
        )
        val viewModel = TerminalProvidersViewModel(repository)

        assertFalse(viewModel.state.value.hasScanned)
        assertTrue(viewModel.state.value.scanResults.isEmpty())
        assertEquals(1, viewModel.state.value.installed.size)
        assertEquals(0, repository.scanCalls)
    }

    @Test
    fun successfulEmptyScanMarksScannedAndKeepsEmptyResults() {
        val repository = FakeRepository(snapshotValue = snapshot())
        repository.scanSnapshotValue = snapshot(hasScanned = true)
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.Scan)

        assertEquals(1, repository.scanCalls)
        assertTrue(viewModel.state.value.hasScanned)
        assertTrue(viewModel.state.value.scanResults.isEmpty())
        assertFalse(viewModel.state.value.operationFailed)
    }

    @Test
    fun scanPreservesProviderDataAndOrder() {
        val first = scanned("terminal", "One", "pkg.one", "ServiceOne")
        val second = scanned("terminal", "Two", "pkg.two", "ServiceTwo")
        val repository = FakeRepository(snapshotValue = snapshot())
        repository.scanSnapshotValue = snapshot(
            scanResults = listOf(first, second),
            hasScanned = true
        )
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.Scan)

        assertEquals(listOf(first, second), viewModel.state.value.scanResults)
    }

    @Test
    fun reloadReadsFreshStateWithoutScanning() {
        val repository = FakeRepository(snapshotValue = snapshot())
        val viewModel = TerminalProvidersViewModel(repository)
        repository.snapshotValue = snapshot(
            installed = listOf(installed("external", enabled = false)),
            hasScanned = false
        )

        viewModel.onAction(TerminalProvidersUiAction.Reload)

        assertEquals(0, repository.scanCalls)
        assertEquals("external", viewModel.state.value.installed.single().id)
        assertNull(viewModel.state.value.confirmation)
    }

    @Test
    fun requestAddAndDismissDoNotSaveAnything() {
        val provider = scanned("terminal", "Terminal", "pkg", "Service")
        val repository = FakeRepository(
            snapshotValue = snapshot(scanResults = listOf(provider), hasScanned = true)
        )
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.RequestAdd(provider))
        assertTrue(viewModel.state.value.confirmation is TerminalProvidersConfirmation.Add)
        assertEquals(0, repository.addCalls)

        viewModel.onAction(TerminalProvidersUiAction.DismissDialog)
        assertNull(viewModel.state.value.confirmation)
        assertEquals(0, repository.addCalls)
    }

    @Test
    fun confirmAddSendsExactLegacyFieldsAndRereadsState() {
        val provider = scanned("terminal", "Termux Bridge", "pkg.term", "TerminalService")
        val repository = FakeRepository(
            snapshotValue = snapshot(scanResults = listOf(provider), hasScanned = true)
        )
        repository.onAdd = { call ->
            repository.snapshotValue = snapshot(
                installed = listOf(
                    TerminalProviderInstalledUiItem(
                        id = "generated-id",
                        name = call.name,
                        packageName = call.packageName,
                        enabled = call.enabled
                    )
                ),
                scanResults = listOf(provider),
                hasScanned = true
            )
        }
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.RequestAdd(provider))
        viewModel.onAction(TerminalProvidersUiAction.ConfirmAdd)

        assertEquals(1, repository.addCalls)
        assertEquals(
            AddCall("terminal", "Termux Bridge", "pkg.term", "TerminalService", true),
            repository.lastAdd
        )
        assertEquals("generated-id", viewModel.state.value.installed.single().id)
        assertNull(viewModel.state.value.confirmation)
        assertFalse(viewModel.state.value.operationFailed)
    }

    @Test
    fun secondConfirmAfterSuccessfulAddDoesNotCreateDuplicate() {
        val provider = scanned("terminal", "Terminal", "pkg", "Service")
        val repository = FakeRepository(
            snapshotValue = snapshot(scanResults = listOf(provider), hasScanned = true)
        )
        repository.onAdd = { call ->
            repository.snapshotValue = snapshot(
                installed = listOf(installed("generated", call.name, call.packageName, call.enabled)),
                scanResults = listOf(provider),
                hasScanned = true
            )
        }
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.RequestAdd(provider))
        viewModel.onAction(TerminalProvidersUiAction.ConfirmAdd)
        viewModel.onAction(TerminalProvidersUiAction.ConfirmAdd)

        assertEquals(1, repository.addCalls)
    }

    @Test
    fun toggleCallsRepositoryOnceAndReflectsPersistedValue() {
        val repository = FakeRepository(
            snapshotValue = snapshot(installed = listOf(installed("p1", enabled = true)))
        )
        repository.onSetEnabled = { id, enabled ->
            repository.snapshotValue = snapshot(installed = listOf(installed(id, enabled = enabled)))
        }
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.SetEnabled("p1", false))

        assertEquals(1, repository.setEnabledCalls)
        assertEquals("p1" to false, repository.lastEnabled)
        assertFalse(viewModel.state.value.installed.single().enabled)
    }

    @Test
    fun deleteRequiresConfirmationAndPassesExactId() {
        val repository = FakeRepository(
            snapshotValue = snapshot(installed = listOf(installed("p1", name = "One", enabled = true)))
        )
        repository.onDelete = { id ->
            repository.snapshotValue = snapshot(
                installed = repository.snapshotValue.installed.filterNot { it.id == id }
            )
        }
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.ConfirmDelete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(TerminalProvidersUiAction.RequestDelete("p1"))
        assertTrue(viewModel.state.value.confirmation is TerminalProvidersConfirmation.Delete)
        assertEquals(0, repository.deleteCalls)

        viewModel.onAction(TerminalProvidersUiAction.ConfirmDelete)
        assertEquals(1, repository.deleteCalls)
        assertEquals("p1", repository.lastDeletedId)
        assertTrue(viewModel.state.value.installed.isEmpty())
        assertNull(viewModel.state.value.confirmation)
    }

    @Test
    fun cancellingDeleteNeverDeletes() {
        val repository = FakeRepository(
            snapshotValue = snapshot(installed = listOf(installed("p1", enabled = true)))
        )
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.RequestDelete("p1"))
        viewModel.onAction(TerminalProvidersUiAction.DismissDialog)

        assertEquals(0, repository.deleteCalls)
        assertNull(viewModel.state.value.confirmation)
    }

    @Test
    fun failedMutationDoesNotShowFalseSavedStateAndCanBeRetried() {
        val provider = scanned("terminal", "Terminal", "pkg", "Service")
        val repository = FakeRepository(
            snapshotValue = snapshot(scanResults = listOf(provider), hasScanned = true)
        )
        repository.failAdd = true
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.RequestAdd(provider))
        viewModel.onAction(TerminalProvidersUiAction.ConfirmAdd)

        assertEquals(1, repository.addCalls)
        assertTrue(viewModel.state.value.installed.isEmpty())
        assertTrue(viewModel.state.value.operationFailed)
        assertTrue(viewModel.state.value.confirmation is TerminalProvidersConfirmation.Add)

        repository.failAdd = false
        repository.onAdd = { call ->
            repository.snapshotValue = snapshot(
                installed = listOf(installed("generated", call.name, call.packageName, call.enabled)),
                scanResults = listOf(provider),
                hasScanned = true
            )
        }
        viewModel.onAction(TerminalProvidersUiAction.ConfirmAdd)

        assertEquals(2, repository.addCalls)
        assertEquals(1, viewModel.state.value.installed.size)
        assertFalse(viewModel.state.value.operationFailed)
    }

    @Test
    fun reloadClearsOldConfirmationWithoutRepeatingOperation() {
        val provider = scanned("terminal", "Terminal", "pkg", "Service")
        val repository = FakeRepository(
            snapshotValue = snapshot(scanResults = listOf(provider), hasScanned = true)
        )
        val viewModel = TerminalProvidersViewModel(repository)

        viewModel.onAction(TerminalProvidersUiAction.RequestAdd(provider))
        viewModel.onAction(TerminalProvidersUiAction.Reload)

        assertNull(viewModel.state.value.confirmation)
        assertEquals(0, repository.addCalls)
        assertEquals(0, repository.scanCalls)
    }

    @Test
    fun backProducesOneShotBackEffect() {
        val viewModel = TerminalProvidersViewModel(FakeRepository(snapshotValue = snapshot()))

        assertSame(
            TerminalProvidersUiEffect.Back,
            viewModel.onAction(TerminalProvidersUiAction.Back)
        )
        assertNull(viewModel.onAction(TerminalProvidersUiAction.Reload))
    }

    private data class AddCall(
        val providerType: String,
        val name: String,
        val packageName: String,
        val serviceClass: String,
        val enabled: Boolean
    )

    private class FakeRepository(
        var snapshotValue: TerminalProvidersSnapshot
    ) : TerminalProvidersSettingsRepository {
        var scanSnapshotValue: TerminalProvidersSnapshot = snapshotValue
        var scanCalls = 0
        var addCalls = 0
        var setEnabledCalls = 0
        var deleteCalls = 0
        var lastAdd: AddCall? = null
        var lastEnabled: Pair<String, Boolean>? = null
        var lastDeletedId: String? = null
        var failAdd = false
        var failScan = false
        var onAdd: ((AddCall) -> Unit)? = null
        var onSetEnabled: ((String, Boolean) -> Unit)? = null
        var onDelete: ((String) -> Unit)? = null

        override fun snapshot(): TerminalProvidersSnapshot = snapshotValue

        override fun scan(): TerminalProvidersSnapshot {
            scanCalls++
            if (failScan) error("scan failed")
            snapshotValue = scanSnapshotValue
            return snapshotValue
        }

        override fun addProvider(
            providerType: String,
            name: String,
            packageName: String,
            serviceClass: String,
            enabled: Boolean
        ) {
            addCalls++
            val call = AddCall(providerType, name, packageName, serviceClass, enabled)
            lastAdd = call
            if (failAdd) error("add failed")
            onAdd?.invoke(call)
        }

        override fun setProviderEnabled(providerId: String, enabled: Boolean) {
            setEnabledCalls++
            lastEnabled = providerId to enabled
            onSetEnabled?.invoke(providerId, enabled)
        }

        override fun deleteProvider(providerId: String) {
            deleteCalls++
            lastDeletedId = providerId
            onDelete?.invoke(providerId)
        }
    }

    companion object {
        private fun snapshot(
            installed: List<TerminalProviderInstalledUiItem> = emptyList(),
            scanResults: List<TerminalProviderScanUiItem> = emptyList(),
            hasScanned: Boolean = false
        ) = TerminalProvidersSnapshot(installed, scanResults, hasScanned)

        private fun scanned(
            providerType: String,
            label: String,
            packageName: String,
            serviceClass: String
        ) = TerminalProviderScanUiItem(providerType, label, packageName, serviceClass)

        private fun installed(
            id: String,
            name: String = "Provider",
            packageName: String = "pkg.$id",
            enabled: Boolean
        ) = TerminalProviderInstalledUiItem(id, name, packageName, enabled)
    }
}
