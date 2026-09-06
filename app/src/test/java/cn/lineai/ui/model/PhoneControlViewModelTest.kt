package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneControlViewModelTest {
    @Test
    fun statesMatchDisclaimerAndAccessibilityCombinations() {
        val disclaimerRequired = PhoneControlViewModel(FakeRepository(false, false)).state.value
        assertEquals(PhoneControlAccessibilityStatus.DISCLAIMER_REQUIRED, disclaimerRequired.accessibilityStatus)
        assertFalse(disclaimerRequired.showPermissions)

        val disabled = PhoneControlViewModel(FakeRepository(true, false)).state.value
        assertEquals(PhoneControlAccessibilityStatus.DISABLED, disabled.accessibilityStatus)
        assertFalse(disabled.showPermissions)

        val enabled = PhoneControlViewModel(FakeRepository(true, true)).state.value
        assertEquals(PhoneControlAccessibilityStatus.ENABLED, enabled.accessibilityStatus)
        assertTrue(enabled.showPermissions)
    }

    @Test
    fun permissionCatalogKeepsLegacySevenItemOrder() {
        assertEquals(
            listOf(
                "screenshot",
                "click",
                "swipe",
                "longPress",
                "viewHierarchy",
                "viewAction",
                "globalAction"
            ),
            PhoneControlPermissionCatalog.items().map { it.id }
        )
    }

    @Test
    fun openingDisclaimerDoesNotWrite() {
        val repository = FakeRepository(false, false)
        val viewModel = PhoneControlViewModel(repository)

        assertEquals(
            PhoneControlUiEffect.ShowDisclaimer,
            viewModel.onAction(PhoneControlUiAction.AccessibilityClicked)
        )
        assertEquals(0, repository.disclaimerWriteCount)
        assertEquals(0, repository.permissionWriteCount)
    }

    @Test
    fun rejectingDisclaimerDoesNotWriteOrOpenSettings() {
        val repository = FakeRepository(false, false)
        val viewModel = PhoneControlViewModel(repository)

        assertNull(viewModel.onAction(PhoneControlUiAction.RejectDisclaimer))
        assertEquals(0, repository.disclaimerWriteCount)
        assertEquals(0, repository.permissionWriteCount)
    }

    @Test
    fun acceptingDisclaimerPersistsBeforeOpenSettingsEffect() {
        val repository = FakeRepository(false, false)
        val viewModel = PhoneControlViewModel(repository)
        repository.events.clear()

        val effect = viewModel.onAction(PhoneControlUiAction.AcceptDisclaimer)

        assertEquals(PhoneControlUiEffect.OpenAccessibilitySettings, effect)
        assertEquals("saveDisclaimer", repository.events.first())
        assertTrue(viewModel.state.value.disclaimerAccepted)
        assertEquals(1, repository.disclaimerWriteCount)
    }

    @Test
    fun failedDisclaimerSaveDoesNotOpenSettings() {
        val repository = FakeRepository(false, false).apply {
            failDisclaimerSave = true
        }
        val viewModel = PhoneControlViewModel(repository)

        assertNull(viewModel.onAction(PhoneControlUiAction.AcceptDisclaimer))
        assertFalse(viewModel.state.value.disclaimerAccepted)
        assertEquals(1, repository.disclaimerWriteCount)
    }

    @Test
    fun acceptedDisabledAccessibilityEmitsOneOpenEffectAndReloadDoesNotRepeatIt() {
        val repository = FakeRepository(true, false)
        val viewModel = PhoneControlViewModel(repository)

        assertEquals(
            PhoneControlUiEffect.OpenAccessibilitySettings,
            viewModel.onAction(PhoneControlUiAction.AccessibilityClicked)
        )
        assertNull(viewModel.onAction(PhoneControlUiAction.Reload))
        assertEquals(0, repository.disclaimerWriteCount)
        assertEquals(0, repository.permissionWriteCount)
    }

    @Test
    fun acceptedEnabledAccessibilityDoesNothingOnRowClick() {
        val viewModel = PhoneControlViewModel(FakeRepository(true, true))

        assertNull(viewModel.onAction(PhoneControlUiAction.AccessibilityClicked))
    }

    @Test
    fun permissionToggleWritesExactlyOnceAndReflectsPersistedValue() {
        val repository = FakeRepository(true, true)
        val viewModel = PhoneControlViewModel(repository)

        assertNull(
            viewModel.onAction(
                PhoneControlUiAction.SetPermission(PhoneControlPermission.SWIPE, false)
            )
        )

        assertEquals(1, repository.permissionWriteCount)
        assertEquals("swipe", repository.lastWrittenPermission)
        assertFalse(
            viewModel.state.value.permissions
                .first { it.permission == PhoneControlPermission.SWIPE }
                .enabled
        )
    }

    @Test
    fun failedPermissionSaveDoesNotShowFalseSuccess() {
        val repository = FakeRepository(true, true).apply {
            failPermissionSave = true
        }
        val viewModel = PhoneControlViewModel(repository)

        viewModel.onAction(
            PhoneControlUiAction.SetPermission(PhoneControlPermission.CLICK, false)
        )

        assertEquals(1, repository.permissionWriteCount)
        assertTrue(
            viewModel.state.value.permissions
                .first { it.permission == PhoneControlPermission.CLICK }
                .enabled
        )
    }

    @Test
    fun reloadRereadsExternalStateWithoutWritesOrIntentEffect() {
        val repository = FakeRepository(true, true)
        val viewModel = PhoneControlViewModel(repository)
        repository.accessibilityEnabled = false
        repository.permissions["screenshot"] = false

        assertNull(viewModel.onAction(PhoneControlUiAction.Reload))

        assertEquals(PhoneControlAccessibilityStatus.DISABLED, viewModel.state.value.accessibilityStatus)
        assertFalse(viewModel.state.value.showPermissions)
        assertFalse(
            viewModel.state.value.permissions
                .first { it.permission == PhoneControlPermission.SCREENSHOT }
                .enabled
        )
        assertEquals(0, repository.disclaimerWriteCount)
        assertEquals(0, repository.permissionWriteCount)
    }

    @Test
    fun backEmitsBackEffect() {
        val viewModel = PhoneControlViewModel(FakeRepository(true, true))

        assertEquals(
            PhoneControlUiEffect.Back,
            viewModel.onAction(PhoneControlUiAction.Back)
        )
    }

    private class FakeRepository(
        var disclaimerAccepted: Boolean,
        var accessibilityEnabled: Boolean
    ) : PhoneControlSettingsRepository {
        val permissions = linkedMapOf(
            "screenshot" to true,
            "click" to true,
            "swipe" to true,
            "longPress" to true,
            "viewHierarchy" to true,
            "viewAction" to true,
            "globalAction" to true
        )
        val events = mutableListOf<String>()
        var disclaimerWriteCount = 0
        var permissionWriteCount = 0
        var lastWrittenPermission: String? = null
        var failDisclaimerSave = false
        var failPermissionSave = false

        override fun isAccessibilityEnabled(): Boolean = accessibilityEnabled

        override fun isDisclaimerAccepted(): Boolean {
            events += "readDisclaimer"
            return disclaimerAccepted
        }

        override fun setDisclaimerAccepted(accepted: Boolean) {
            disclaimerWriteCount += 1
            events += "saveDisclaimer"
            if (failDisclaimerSave) {
                throw IllegalStateException("save failed")
            }
            disclaimerAccepted = accepted
        }

        override fun isPermissionEnabled(permissionId: String): Boolean =
            permissions[permissionId] ?: true

        override fun setPermissionEnabled(permissionId: String, enabled: Boolean) {
            permissionWriteCount += 1
            lastWrittenPermission = permissionId
            if (failPermissionSave) {
                throw IllegalStateException("save failed")
            }
            permissions[permissionId] = enabled
        }
    }
}
