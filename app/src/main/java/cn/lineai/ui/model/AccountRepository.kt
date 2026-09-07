package cn.lineai.ui.model

import android.content.Context

/**
 * Android boundary for account-backed providers.
 *
 * ViewModels depend on this repository instead of Android Context. The concrete
 * implementation owns the application Context and delegates provider-specific
 * OAuth, token storage and HTTP work to AccountModelProvider.
 */
interface AccountRepository {
    val provider: AccountModelProvider

    fun identity(): AccountIdentity
    fun startLogin(callback: AccountLoginCallback)
    fun logout()
    fun isUnauthorized(error: Throwable): Boolean

    suspend fun fetchModelIds(): List<String>
    suspend fun fetchUsage(): AccountUsageSnapshot
}

class AndroidAccountRepository(
    context: Context,
    override val provider: AccountModelProvider
) : AccountRepository {
    private val appContext = context.applicationContext ?: context

    override fun identity(): AccountIdentity = provider.identity(appContext)

    override fun startLogin(callback: AccountLoginCallback) {
        provider.startLogin(appContext, callback)
    }

    override fun logout() {
        provider.logout(appContext)
    }

    override fun isUnauthorized(error: Throwable): Boolean = provider.isUnauthorized(error)

    override suspend fun fetchModelIds(): List<String> = provider.fetchModelIds(appContext)

    override suspend fun fetchUsage(): AccountUsageSnapshot = provider.fetchUsage(appContext)
}
