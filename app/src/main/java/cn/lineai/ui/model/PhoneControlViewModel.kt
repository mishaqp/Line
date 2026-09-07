package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PhoneControlPermission(val id: String) {
    SCREENSHOT("screenshot"),
    CLICK("click"),
    SWIPE("swipe"),
    LONG_PRESS("longPress"),
    VIEW_HIERARCHY("viewHierarchy"),
    VIEW_ACTION("viewAction"),
    GLOBAL_ACTION("globalAction")
}

data class PhoneControlPermissionUiItem(
    val permission: PhoneControlPermission,
    val enabled: Boolean
)

enum class PhoneControlAccessibilityStatus {
    DISCLAIMER_REQUIRED,
    DISABLED,
    ENABLED
}

data class PhoneControlUiState(
    val disclaimerAccepted: Boolean,
    val accessibilityEnabled: Boolean,
    val accessibilityStatus: PhoneControlAccessibilityStatus,
    val permissions: List<PhoneControlPermissionUiItem>
) {
    val showPermissions: Boolean
        get() = disclaimerAccepted && accessibilityEnabled
}

sealed interface PhoneControlUiAction {
    data object Back : PhoneControlUiAction
    data object Reload : PhoneControlUiAction
    data object AccessibilityClicked : PhoneControlUiAction
    data object AcceptDisclaimer : PhoneControlUiAction
    data object RejectDisclaimer : PhoneControlUiAction
    data class SetPermission(
        val permission: PhoneControlPermission,
        val enabled: Boolean
    ) : PhoneControlUiAction
}

sealed interface PhoneControlUiEffect {
    data object Back : PhoneControlUiEffect
    data object ShowDisclaimer : PhoneControlUiEffect
    data object OpenAccessibilitySettings : PhoneControlUiEffect
}

interface PhoneControlSettingsRepository {
    fun isAccessibilityEnabled(): Boolean
    fun isDisclaimerAccepted(): Boolean
    fun setDisclaimerAccepted(accepted: Boolean)
    fun isPermissionEnabled(permissionId: String): Boolean
    fun setPermissionEnabled(permissionId: String, enabled: Boolean)
}

object PhoneControlPermissionCatalog {
    private val orderedPermissions = listOf(
        PhoneControlPermission.SCREENSHOT,
        PhoneControlPermission.CLICK,
        PhoneControlPermission.SWIPE,
        PhoneControlPermission.LONG_PRESS,
        PhoneControlPermission.VIEW_HIERARCHY,
        PhoneControlPermission.VIEW_ACTION,
        PhoneControlPermission.GLOBAL_ACTION
    )

    fun items(): List<PhoneControlPermission> = orderedPermissions
}

class PhoneControlViewModel(
    private val repository: PhoneControlSettingsRepository
) : ViewModel() {
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<PhoneControlUiState> = _state.asStateFlow()

    fun onAction(action: PhoneControlUiAction): PhoneControlUiEffect? = when (action) {
        PhoneControlUiAction.Back -> PhoneControlUiEffect.Back
        PhoneControlUiAction.Reload -> {
            reload()
            null
        }
        PhoneControlUiAction.AccessibilityClicked -> when {
            !_state.value.disclaimerAccepted -> PhoneControlUiEffect.ShowDisclaimer
            !_state.value.accessibilityEnabled -> PhoneControlUiEffect.OpenAccessibilitySettings
            else -> null
        }
        PhoneControlUiAction.AcceptDisclaimer -> acceptDisclaimer()
        PhoneControlUiAction.RejectDisclaimer -> null
        is PhoneControlUiAction.SetPermission -> {
            runCatching {
                repository.setPermissionEnabled(action.permission.id, action.enabled)
            }
            reload()
            null
        }
    }

    private fun acceptDisclaimer(): PhoneControlUiEffect? {
        val writeSucceeded = runCatching {
            repository.setDisclaimerAccepted(true)
        }.isSuccess
        reload()
        return if (writeSucceeded && _state.value.disclaimerAccepted) {
            PhoneControlUiEffect.OpenAccessibilitySettings
        } else {
            null
        }
    }

    private fun reload() {
        _state.value = readState()
    }

    private fun readState(): PhoneControlUiState {
        val disclaimerAccepted = repository.isDisclaimerAccepted()
        val accessibilityEnabled = repository.isAccessibilityEnabled()
        val status = when {
            !disclaimerAccepted -> PhoneControlAccessibilityStatus.DISCLAIMER_REQUIRED
            !accessibilityEnabled -> PhoneControlAccessibilityStatus.DISABLED
            else -> PhoneControlAccessibilityStatus.ENABLED
        }
        val permissions = PhoneControlPermissionCatalog.items().map { permission ->
            PhoneControlPermissionUiItem(
                permission = permission,
                enabled = repository.isPermissionEnabled(permission.id)
            )
        }
        return PhoneControlUiState(
            disclaimerAccepted = disclaimerAccepted,
            accessibilityEnabled = accessibilityEnabled,
            accessibilityStatus = status,
            permissions = permissions
        )
    }

    companion object {
        fun factory(repository: PhoneControlSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PhoneControlViewModel::class.java)) {
                        return PhoneControlViewModel(repository) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
    }
}
