package cn.lineai.ui.model

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountScreenState(
    val providerKind: AccountProviderKind,
    val providerLabel: String,
    val authenticated: Boolean = false,
    val accountId: String = "",
    val email: String = "",
    val plan: String = "",
    val loading: Boolean = false,
    val loginInProgress: Boolean = false,
    val deviceCode: String = "",
    val loginError: String = "",
    val sessionExpired: Boolean = false,
    val usage: AccountUsageSnapshot? = null,
    val usageError: Boolean = false,
    val models: List<String> = emptyList(),
    val modelsError: Boolean = false
)

/**
 * Lifecycle-aware state holder shared by the Codex and Grok account screens.
 * Android Context stays behind AccountRepository instead of leaking into the
 * ViewModel.
 */
class AccountScreenViewModel(
    private val repository: AccountRepository
) : ViewModel() {
    private val provider = repository.provider
    private var generation = 0

    private val _state = MutableStateFlow(
        AccountScreenState(
            providerKind = provider.kind,
            providerLabel = provider.label
        )
    )
    val state: StateFlow<AccountScreenState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val currentGeneration = ++generation
        val identity = repository.identity()
        if (!identity.authenticated) {
            _state.update {
                AccountScreenState(
                    providerKind = provider.kind,
                    providerLabel = provider.label,
                    sessionExpired = it.sessionExpired
                )
            }
            return
        }

        _state.update {
            it.copy(
                authenticated = true,
                accountId = identity.accountId,
                email = identity.email,
                plan = identity.plan,
                loading = true,
                loginInProgress = false,
                deviceCode = "",
                loginError = "",
                sessionExpired = false,
                usageError = false,
                modelsError = false
            )
        }

        viewModelScope.launch {
            val usageDeferred = async { runCatching { repository.fetchUsage() } }
            val modelsDeferred = async { runCatching { repository.fetchModelIds().distinct() } }
            val usageResult = usageDeferred.await()
            val modelsResult = modelsDeferred.await()

            if (currentGeneration != generation) {
                return@launch
            }

            val unauthorized = usageResult.exceptionOrNull()?.let(repository::isUnauthorized) == true ||
                modelsResult.exceptionOrNull()?.let(repository::isUnauthorized) == true
            if (unauthorized) {
                repository.logout()
                _state.value = AccountScreenState(
                    providerKind = provider.kind,
                    providerLabel = provider.label,
                    sessionExpired = true
                )
                return@launch
            }

            val refreshedIdentity = repository.identity()
            if (!refreshedIdentity.authenticated) {
                _state.value = AccountScreenState(
                    providerKind = provider.kind,
                    providerLabel = provider.label,
                    sessionExpired = true
                )
                return@launch
            }

            _state.update {
                it.copy(
                    authenticated = true,
                    accountId = refreshedIdentity.accountId,
                    email = refreshedIdentity.email,
                    plan = refreshedIdentity.plan,
                    loading = false,
                    usage = usageResult.getOrNull(),
                    usageError = usageResult.isFailure,
                    models = modelsResult.getOrDefault(emptyList()),
                    modelsError = modelsResult.isFailure,
                    sessionExpired = false
                )
            }
        }
    }

    fun login() {
        if (_state.value.loginInProgress) {
            return
        }
        ++generation
        _state.update {
            it.copy(
                loginInProgress = true,
                loginError = "",
                sessionExpired = false,
                deviceCode = ""
            )
        }
        repository.startLogin(object : AccountLoginCallback {
            override fun onDeviceCode(userCode: String, verificationUri: String) {
                _state.update {
                    it.copy(
                        loginInProgress = true,
                        deviceCode = userCode,
                        loginError = ""
                    )
                }
            }

            override fun onComplete(success: Boolean, message: String) {
                if (success) {
                    _state.update { it.copy(loginInProgress = false, deviceCode = "", loginError = "") }
                    refresh()
                } else {
                    _state.update {
                        it.copy(
                            loginInProgress = false,
                            deviceCode = "",
                            loginError = message
                        )
                    }
                }
            }
        })
    }

    fun logout() {
        ++generation
        repository.logout()
        _state.value = AccountScreenState(
            providerKind = provider.kind,
            providerLabel = provider.label
        )
    }

    companion object {
        @JvmStatic
        fun factory(
            context: Context,
            provider: AccountModelProvider
        ): ViewModelProvider.Factory = factory(AndroidAccountRepository(context, provider))

        internal fun factory(repository: AccountRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AccountScreenViewModel(repository) as T
                }
            }
    }
}
