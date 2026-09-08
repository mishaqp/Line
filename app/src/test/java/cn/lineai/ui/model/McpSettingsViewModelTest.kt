package cn.lineai.ui.model

import cn.lineai.model.McpSettingsState
import cn.lineai.model.McpToolConfig
import cn.lineai.navigation.LineDestination
import java.util.LinkedHashSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpSettingsViewModelTest {

    @Test
    fun initialReadDoesNotWrite() {
        val repository = RecordingRepository(snapshot("ssh", group("shell", true)))

        val viewModel = McpSettingsViewModel(repository)

        assertEquals(McpSettingsViewModel.MODE_SSH, viewModel.state.value.executionMode)
        assertEquals(0, repository.modeWrites)
        assertEquals(0, repository.groupWrites)
        assertEquals(1, repository.snapshotReads)
    }

    @Test
    fun groupOrderAndToolNamesAreIndependentCopies() {
        val firstTools = arrayOf("bash", "read")
        val repository = RecordingRepository(
            snapshot(
                "local",
                group("shell", true, firstTools),
                group("todo", false, arrayOf("todo_write"))
            )
        )

        val viewModel = McpSettingsViewModel(repository)
        val tools = viewModel.state.value.groups.first().tools
        firstTools[0] = "mutated"

        assertEquals(listOf("shell", "todo"), viewModel.state.value.groups.map { it.id })
        assertEquals(listOf("bash", "read"), tools)
        assertEquals(listOf("todo_write"), viewModel.state.value.groups[1].tools)
    }

    @Test
    fun groupsAreFilteredByExecutionMode() {
        val repository = RecordingRepository(
            snapshot(
                "local",
                group("shell", true, arrayOf("bash"), setOf("local", "ssh", "root")),
                group("provider_only", true, arrayOf("pty"), setOf("terminal_provider"))
            )
        )

        val viewModel = McpSettingsViewModel(repository)

        assertEquals(listOf("shell"), viewModel.state.value.groups.map { it.id })

        viewModel.onAction(
            McpSettingsUiAction.SetExecutionMode(McpSettingsViewModel.MODE_TERMINAL_PROVIDER)
        )

        assertEquals(listOf("provider_only"), viewModel.state.value.groups.map { it.id })
        assertEquals(1, repository.modeWrites)
        assertEquals(0, repository.groupWrites)
    }

    @Test
    fun unknownModeNormalizesToLocalWithoutWriting() {
        val viewModel = McpSettingsViewModel(RecordingRepository(snapshot("weird", group("shell", true))))

        assertEquals(McpSettingsViewModel.MODE_LOCAL, viewModel.state.value.executionMode)
        assertEquals(listOf("shell"), viewModel.state.value.groups.map { it.id })
    }

    @Test
    fun eachModeWritesExactValueOnceAndReloads() {
        val modes = listOf(
            McpSettingsViewModel.MODE_LOCAL,
            McpSettingsViewModel.MODE_SSH,
            McpSettingsViewModel.MODE_TERMINAL_PROVIDER,
            McpSettingsViewModel.MODE_ROOT
        )
        val repository = RecordingRepository(snapshot("local", group("shell", true)))
        val viewModel = McpSettingsViewModel(repository)
        val readsAfterInit = repository.snapshotReads

        modes.forEach { mode ->
            viewModel.onAction(McpSettingsUiAction.SetExecutionMode(mode))
            assertEquals(mode, repository.lastRequestedMode)
            assertEquals(mode, viewModel.state.value.executionMode)
        }

        assertEquals(4, repository.modeWrites)
        assertEquals(0, repository.groupWrites)
        assertEquals(readsAfterInit + 4, repository.snapshotReads)
        assertTrue(viewModel.state.value.showSshActions.not())
    }

    @Test
    fun sshModeShowsSshActions() {
        val repository = RecordingRepository(snapshot("local"))
        val viewModel = McpSettingsViewModel(repository)

        viewModel.onAction(McpSettingsUiAction.SetExecutionMode(McpSettingsViewModel.MODE_SSH))

        assertTrue(viewModel.state.value.showSshActions)
    }

    @Test
    fun toggleWritesExactIdAndEnabledThenReloads() {
        val repository = RecordingRepository(snapshot("local", group("web_search", true)))
        val viewModel = McpSettingsViewModel(repository)

        viewModel.onAction(McpSettingsUiAction.SetToolGroupEnabled("web_search", false))

        assertEquals(1, repository.groupWrites)
        assertEquals("web_search", repository.lastRequestedGroupId)
        assertEquals(false, repository.lastRequestedEnabled)
        assertFalse(viewModel.state.value.groups.single().enabled)
    }

    @Test
    fun reloadOnlyReads() {
        val repository = RecordingRepository(snapshot("local", group("shell", true)))
        val viewModel = McpSettingsViewModel(repository)
        repository.snapshotValue = snapshot("root", group("shell", false))

        assertNull(viewModel.onAction(McpSettingsUiAction.Reload))

        assertEquals(McpSettingsViewModel.MODE_ROOT, viewModel.state.value.executionMode)
        assertFalse(viewModel.state.value.groups.single().enabled)
        assertEquals(0, repository.modeWrites)
        assertEquals(0, repository.groupWrites)
    }

    @Test
    fun navigationEffectsAreOneShotAndNotEmittedOnCreateOrReload() {
        val viewModel = McpSettingsViewModel(RecordingRepository(snapshot("ssh")))

        assertNull(viewModel.onAction(McpSettingsUiAction.Reload))
        assertEquals(McpSettingsUiEffect.Back, viewModel.onAction(McpSettingsUiAction.Back))
        val ssh = viewModel.onAction(McpSettingsUiAction.OpenSshSettings)
        val termux = viewModel.onAction(McpSettingsUiAction.OpenTermuxIntegration)

        assertTrue(ssh is McpSettingsUiEffect.Navigate)
        assertEquals(
            LineDestination.SshSettings,
            (ssh as McpSettingsUiEffect.Navigate).destination
        )
        assertEquals(
            LineDestination.TermuxIntegration,
            (termux as McpSettingsUiEffect.Navigate).destination
        )
        assertNull(viewModel.onAction(McpSettingsUiAction.Reload))
    }

    private class RecordingRepository(
        var snapshotValue: McpSettingsState
    ) : McpSettingsRepository {
        var snapshotReads = 0
        var modeWrites = 0
        var groupWrites = 0
        var lastRequestedMode: String? = null
        var lastRequestedGroupId: String? = null
        var lastRequestedEnabled: Boolean? = null

        override fun snapshot(): McpSettingsState {
            snapshotReads++
            return snapshotValue
        }

        override fun setExecutionMode(mode: String) {
            modeWrites++
            lastRequestedMode = mode
            snapshotValue = McpSettingsState(mode, snapshotValue.configs)
        }

        override fun setToolGroupEnabled(id: String, enabled: Boolean) {
            groupWrites++
            lastRequestedGroupId = id
            lastRequestedEnabled = enabled
            snapshotValue = McpSettingsState(
                snapshotValue.executionMode,
                snapshotValue.configs.map { config ->
                    if (config.id == id) {
                        McpToolConfig(
                            config.id,
                            config.name,
                            config.description,
                            enabled,
                            config.tools,
                            config.supportedExecutionModes,
                            config.iconKey
                        )
                    } else {
                        config
                    }
                }
            )
        }
    }

    companion object {
        private fun snapshot(mode: String, vararg groups: McpToolConfig): McpSettingsState =
            McpSettingsState(mode, groups.toList())

        private fun group(
            id: String,
            enabled: Boolean,
            tools: Array<String> = arrayOf(id),
            modes: Set<String>? = null
        ): McpToolConfig = McpToolConfig(
            id,
            id,
            "$id desc",
            enabled,
            tools,
            modes?.let { LinkedHashSet(it) },
            id
        )
    }
}
