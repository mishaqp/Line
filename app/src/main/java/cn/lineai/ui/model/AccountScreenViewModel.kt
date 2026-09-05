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
 * Provider-specific OAuth and HTTP details stay behind AccountModelProvider.
 */
class AccountScreenViewModel(
    context: Context,
    val provider: AccountModelProvider
) : ViewModel() {
    private val appContext = context.applicationContext ?: context
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
        val identity = provider.identity(appContext)
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
            val usageDeferred = async { runCatching { provider.fetchUsage(appContext) } }
            val modelsDeferred = async { runCatching { provider.fetchModelIds(appContext).distinct() } }
            val usageResult = usageDeferred.await()
            val modelsResult = modelsDeferred.await()

            if (currentGeneration != generation) {
                return@launch
            }

            val unauthorized = usageResult.exceptionOrNull()?.let(provider::isUnauthorized) == true ||
                modelsResult.exceptionOrNull()?.let(provider::isUnauthorized) == true
            if (unauthorized) {
                provider.logout(appContext)
                _state.value = AccountScreenState(
                    providerKind = provider.kind,
                    providerLabel = provider.label,
                    sessionExpired = true
                )
                return@launch
            }

            val refreshedIdentity = provider.identity(appContext)
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
        provider.startLogin(appContext, object : AccountLoginCallback {
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
        provider.logout(appContext)
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
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AccountScreenViewModel(context, provider) as T
            }
        }
    }
}
