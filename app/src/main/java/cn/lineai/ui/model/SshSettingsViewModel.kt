package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lineai.model.SshConfig
import cn.lineai.navigation.LineDestination
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface SshSettingsRepository {
    fun load(): SshConfig
    fun save(config: SshConfig)

    @Throws(Exception::class)
    fun testConnection(config: SshConfig): String
}

enum class SshConnectionStatus {
    HIDDEN,
    SAVED,
    TESTING,
    SUCCESS,
    FAILED
}

data class SshSettingsUiState(
    val host: String = SshConfig.DEFAULT_HOST,
    val port: String = SshConfig.DEFAULT_PORT.toString(),
    val username: String = "",
    val password: String = "",
    val privateKey: String = "",
    val passphrase: String = "",
    val dirty: Boolean = false,
    val isTesting: Boolean = false,
    val status: SshConnectionStatus = SshConnectionStatus.HIDDEN,
    val statusDetail: String = ""
) {
    override fun toString(): String {
        return "SshSettingsUiState(host=$host, port=$port, username=$username, " +
            "password=${redact(password)}, privateKey=${redact(privateKey)}, " +
            "passphrase=${redact(passphrase)}, dirty=$dirty, isTesting=$isTesting, " +
            "status=$status, statusDetail=${statusDetail.length})"
    }

    private fun redact(value: String): String = if (value.isEmpty()) "" else "***"
}

sealed interface SshSettingsUiAction {
    data object Back : SshSettingsUiAction
    data object Reload : SshSettingsUiAction
    data class SetHost(val value: String) : SshSettingsUiAction
    data class SetPort(val value: String) : SshSettingsUiAction
    data class SetUsername(val value: String) : SshSettingsUiAction
    data class SetPassword(val value: String) : SshSettingsUiAction
    data class SetPrivateKey(val value: String) : SshSettingsUiAction
    data class SetPassphrase(val value: String) : SshSettingsUiAction
    data object Save : SshSettingsUiAction
    data object TestConnection : SshSettingsUiAction
    data object OpenTermuxIntegration : SshSettingsUiAction
}

sealed interface SshSettingsUiEffect {
    data object Back : SshSettingsUiEffect
    data class Navigate(val destination: LineDestination) : SshSettingsUiEffect
}

class SshSettingsViewModel(
    private val repository: SshSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var persisted = readConfig()
    private val _state = MutableStateFlow(fromConfig(persisted))
    val state: StateFlow<SshSettingsUiState> = _state.asStateFlow()

    fun onAction(action: SshSettingsUiAction): SshSettingsUiEffect? = when (action) {
        SshSettingsUiAction.Back -> SshSettingsUiEffect.Back
        SshSettingsUiAction.Reload -> {
            reload()
            null
        }
        is SshSettingsUiAction.SetHost -> {
            mutateDraft { it.copy(host = action.value) }
            null
        }
        is SshSettingsUiAction.SetPort -> {
            mutateDraft { it.copy(port = action.value) }
            null
        }
        is SshSettingsUiAction.SetUsername -> {
            mutateDraft { it.copy(username = action.value) }
            null
        }
        is SshSettingsUiAction.SetPassword -> {
            mutateDraft { it.copy(password = action.value) }
            null
        }
        is SshSettingsUiAction.SetPrivateKey -> {
            mutateDraft { it.copy(privateKey = action.value) }
            null
        }
        is SshSettingsUiAction.SetPassphrase -> {
            mutateDraft { it.copy(passphrase = action.value) }
            null
        }
        SshSettingsUiAction.Save -> {
            saveDraft()
            null
        }
        SshSettingsUiAction.TestConnection -> {
            testConnection()
            null
        }
        SshSettingsUiAction.OpenTermuxIntegration ->
            SshSettingsUiEffect.Navigate(LineDestination.TermuxIntegration)
    }

    private fun reload() {
        val current = _state.value
        if (current.dirty || current.isTesting) {
            return
        }
        persisted = readConfig()
        _state.value = fromConfig(persisted)
    }

    private fun mutateDraft(transform: (SshSettingsUiState) -> SshSettingsUiState) {
        _state.update { current ->
            val next = transform(current)
            next.copy(dirty = !sameDraft(next, persisted))
        }
    }

    private fun saveDraft(): SshConfig {
        val config = toConfig(_state.value)
        repository.save(config)
        persisted = config
        _state.update {
            fromConfig(config).copy(
                status = SshConnectionStatus.SAVED,
                statusDetail = ""
            )
        }
        return config
    }

    private fun testConnection() {
        val current = _state.value
        if (current.isTesting) {
            return
        }
        val config = toConfig(current)
        repository.save(config)
        persisted = config
        _state.update {
            it.copy(
                dirty = false,
                isTesting = true,
                status = SshConnectionStatus.TESTING,
                statusDetail = ""
            )
        }
        viewModelScope.launch(ioDispatcher) {
            try {
                val output = repository.testConnection(config)
                val detail = output.trim()
                _state.update { state ->
                    state.copy(
                        isTesting = false,
                        status = SshConnectionStatus.SUCCESS,
                        statusDetail = detail
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update { state ->
                    state.copy(
                        isTesting = false,
                        status = SshConnectionStatus.FAILED,
                        statusDetail = describeException(error)
                    )
                }
            }
        }
    }

    private fun readConfig(): SshConfig = runCatching {
        repository.load()
    }.getOrElse {
        SshConfig.defaultConfig()
    } ?: SshConfig.defaultConfig()

    companion object {
        const val UNKNOWN_ERROR: String = "未知错误"

        fun parsePort(raw: String?): Int {
            return try {
                val port = raw.orEmpty().trim().toInt()
                if (port > 0) port else SshConfig.DEFAULT_PORT
            } catch (_: Exception) {
                SshConfig.DEFAULT_PORT
            }
        }

        fun toConfig(state: SshSettingsUiState): SshConfig = SshConfig(
            state.host,
            parsePort(state.port),
            state.username,
            state.password,
            state.privateKey,
            state.passphrase
        )

        fun fromConfig(config: SshConfig): SshSettingsUiState = SshSettingsUiState(
            host = config.host,
            port = config.port.toString(),
            username = config.username,
            password = config.password,
            privateKey = config.privateKey,
            passphrase = config.passphrase,
            dirty = false
        )

        fun sameDraft(state: SshSettingsUiState, config: SshConfig): Boolean {
            return state.host == config.host &&
                parsePort(state.port) == config.port &&
                state.username == config.username &&
                state.password == config.password &&
                state.privateKey == config.privateKey &&
                state.passphrase == config.passphrase
        }

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
            repository: SshSettingsRepository,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SshSettingsViewModel::class.java)) {
                    return SshSettingsViewModel(repository, ioDispatcher) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
            }
        }
    }
}
