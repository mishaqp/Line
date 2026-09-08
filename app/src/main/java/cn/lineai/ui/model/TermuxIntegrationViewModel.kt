package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface TermuxIntegrationRepository {
    fun grantCommand(): String
    fun setupAndTest(timeoutMs: Int): TermuxSetupOutcome
    fun redact(value: String?): String
    fun valueOrUnknown(value: String?): String
}

sealed interface TermuxSetupOutcome {
    data class Success(
        val shell: String,
        val rcPath: String,
        val output: String
    ) : TermuxSetupOutcome

    data class Failure(
        val message: String
    ) : TermuxSetupOutcome
}

enum class TermuxIntegrationStatus {
    NONE,
    COPIED,
    PERMISSION_REQUESTED,
    PERMISSION_UNAVAILABLE,
    TERMUX_OPENED,
    TERMUX_OPEN_FAILED,
    SETUP_RUNNING,
    SETUP_SUCCESS,
    SETUP_FAILED
}

data class TermuxIntegrationUiState(
    val grantCommand: String = "",
    val isSetupRunning: Boolean = false,
    val status: TermuxIntegrationStatus = TermuxIntegrationStatus.NONE,
    val shell: String = "",
    val rcPath: String = "",
    val output: String = "",
    val error: String = ""
) {
    override fun toString(): String {
        return "TermuxIntegrationUiState(" +
            "grantCommandLength=${grantCommand.length}, " +
            "isSetupRunning=$isSetupRunning, " +
            "status=$status, " +
            "shell=$shell, " +
            "rcPath=$rcPath, " +
            "outputLength=${output.length}, " +
            "errorLength=${error.length})"
    }
}

sealed interface TermuxIntegrationUiAction {
    data object Back : TermuxIntegrationUiAction
    data object Reload : TermuxIntegrationUiAction
    data object CopyGrantCommand : TermuxIntegrationUiAction
    data object RequestRunCommandPermission : TermuxIntegrationUiAction
    data object PermissionRequested : TermuxIntegrationUiAction
    data object PermissionUnavailable : TermuxIntegrationUiAction
    data object OpenTermux : TermuxIntegrationUiAction
    data object TermuxOpened : TermuxIntegrationUiAction
    data class TermuxOpenFailed(val message: String?) : TermuxIntegrationUiAction
    data object StartSetup : TermuxIntegrationUiAction
}

sealed interface TermuxIntegrationUiEffect {
    data object Back : TermuxIntegrationUiEffect
    data class CopyToClipboard(val command: String) : TermuxIntegrationUiEffect
    data object RequestRunCommandPermission : TermuxIntegrationUiEffect
    data object OpenTermux : TermuxIntegrationUiEffect
}

class TermuxIntegrationViewModel(
    private val repository: TermuxIntegrationRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(
        TermuxIntegrationUiState(grantCommand = repository.grantCommand())
    )
    val state: StateFlow<TermuxIntegrationUiState> = _state.asStateFlow()

    fun onAction(action: TermuxIntegrationUiAction): TermuxIntegrationUiEffect? = when (action) {
        TermuxIntegrationUiAction.Back -> TermuxIntegrationUiEffect.Back
        TermuxIntegrationUiAction.Reload -> null
        TermuxIntegrationUiAction.CopyGrantCommand -> {
            _state.update {
                it.copy(
                    status = TermuxIntegrationStatus.COPIED,
                    error = ""
                )
            }
            TermuxIntegrationUiEffect.CopyToClipboard(_state.value.grantCommand)
        }
        TermuxIntegrationUiAction.RequestRunCommandPermission ->
            TermuxIntegrationUiEffect.RequestRunCommandPermission
        TermuxIntegrationUiAction.PermissionRequested -> {
            _state.update {
                it.copy(
                    status = TermuxIntegrationStatus.PERMISSION_REQUESTED,
                    error = ""
                )
            }
            null
        }
        TermuxIntegrationUiAction.PermissionUnavailable -> {
            _state.update {
                it.copy(
                    status = TermuxIntegrationStatus.PERMISSION_UNAVAILABLE,
                    error = ""
                )
            }
            null
        }
        TermuxIntegrationUiAction.OpenTermux -> TermuxIntegrationUiEffect.OpenTermux
        TermuxIntegrationUiAction.TermuxOpened -> {
            _state.update {
                it.copy(
                    status = TermuxIntegrationStatus.TERMUX_OPENED,
                    error = ""
                )
            }
            null
        }
        is TermuxIntegrationUiAction.TermuxOpenFailed -> {
            _state.update {
                it.copy(
                    status = TermuxIntegrationStatus.TERMUX_OPEN_FAILED,
                    error = repository.redact(action.message)
                )
            }
            null
        }
        TermuxIntegrationUiAction.StartSetup -> {
            startSetup()
            null
        }
    }

    private fun startSetup() {
        if (_state.value.isSetupRunning) {
            return
        }
        _state.update {
            it.copy(
                isSetupRunning = true,
                status = TermuxIntegrationStatus.SETUP_RUNNING,
                error = "",
                output = ""
            )
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val outcome = repository.setupAndTest(SETUP_TIMEOUT_MS)
                if (!isActive) {
                    return@launch
                }
                when (outcome) {
                    is TermuxSetupOutcome.Success -> _state.update { state ->
                        state.copy(
                            isSetupRunning = false,
                            status = TermuxIntegrationStatus.SETUP_SUCCESS,
                            shell = outcome.shell,
                            rcPath = outcome.rcPath,
                            output = outcome.output,
                            error = ""
                        )
                    }
                    is TermuxSetupOutcome.Failure -> _state.update { state ->
                        state.copy(
                            isSetupRunning = false,
                            status = TermuxIntegrationStatus.SETUP_FAILED,
                            error = outcome.message,
                            output = ""
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!isActive) {
                    return@launch
                }
                _state.update { state ->
                    state.copy(
                        isSetupRunning = false,
                        status = TermuxIntegrationStatus.SETUP_FAILED,
                        error = repository.redact(describeException(error)),
                        output = ""
                    )
                }
            }
        }
    }

    companion object {
        const val SETUP_TIMEOUT_MS: Int = 15 * 60 * 1000
        const val UNKNOWN_ERROR: String = "未知错误"

        fun describeException(error: Exception?): String {
            if (error == null) {
                return UNKNOWN_ERROR
            }
            val message = error.message
            if (!message.isNullOrBlank()) {
                return message.trim()
            }
            val name = error.javaClass.simpleName
            return if (name.isEmpty()) UNKNOWN_ERROR else name
        }

        fun factory(
            repository: TermuxIntegrationRepository,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TermuxIntegrationViewModel::class.java)) {
                    return TermuxIntegrationViewModel(repository, ioDispatcher) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
            }
        }
    }
}
