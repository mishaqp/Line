package cn.lineai.ui.model

import android.content.Context
import cn.lineai.data.codex.CodexApiException
import cn.lineai.data.codex.CodexAuthManager
import cn.lineai.data.codex.CodexModelsRepository
import cn.lineai.data.codex.CodexUsageRepository
import cn.lineai.data.grok.GrokApiException
import cn.lineai.data.grok.GrokAuthManager
import cn.lineai.data.grok.GrokModelsRepository
import cn.lineai.data.grok.GrokUsageRepository
import cn.lineai.model.ModelProtocolType
import cn.lineai.model.ModelProviderPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AccountProviderKind {
    CODEX,
    GROK
}

enum class AccountQuotaKind {
    PRIMARY,
    SECONDARY,
    SUBSCRIPTION
}

data class AccountIdentity(
    val authenticated: Boolean,
    val accountId: String = "",
    val email: String = "",
    val plan: String = ""
)

data class AccountQuotaWindow(
    val kind: AccountQuotaKind,
    val hasUsagePercent: Boolean,
    val usedPercent: Double = 0.0,
    val remainingPercent: Double = 0.0,
    val windowMinutes: Long = 0L,
    val resetsAtEpochSeconds: Long = 0L,
    val periodType: String = ""
)

data class AccountUsageSnapshot(
    val windows: List<AccountQuotaWindow> = emptyList()
)

interface AccountLoginCallback {
    fun onDeviceCode(userCode: String, verificationUri: String) = Unit
    fun onComplete(success: Boolean, message: String)
}

/**
 * Shared contract for account-backed model providers.
 *
 * The Compose model editor and account screens both depend on this contract,
 * while provider-specific OAuth, encrypted token storage and HTTP details stay
 * in the data layer.
 */
interface AccountModelProvider {
    val kind: AccountProviderKind
    val label: String
    val protocolType: ModelProtocolType
    val baseUrl: String
    val accountScreenId: String

    fun identity(context: Context): AccountIdentity
    fun isAuthenticated(context: Context): Boolean = identity(context).authenticated
    fun email(context: Context): String = identity(context).email
    fun plan(context: Context): String = identity(context).plan
    fun accountId(context: Context): String = identity(context).accountId

    fun startLogin(context: Context, callback: AccountLoginCallback)
    fun logout(context: Context)
    fun isUnauthorized(error: Throwable): Boolean

    suspend fun fetchModelIds(context: Context): List<String>
    suspend fun fetchUsage(context: Context): AccountUsageSnapshot
}

object AccountModelProviders {
    private object CodexProvider : AccountModelProvider {
        override val kind = AccountProviderKind.CODEX
        override val label = "Codex"
        override val protocolType = ModelProtocolType.CODEX_RESPONSES
        override val baseUrl = "https://api.openai.com/v1"
        override val accountScreenId = "codexAccount"

        override fun identity(context: Context): AccountIdentity {
            val auth = CodexAuthManager(context)
            return AccountIdentity(
                authenticated = auth.isAuthenticated,
                accountId = auth.accountId.orEmpty(),
                email = auth.email.orEmpty(),
                plan = auth.planType.orEmpty()
            )
        }

        override fun startLogin(context: Context, callback: AccountLoginCallback) {
            CodexAuthManager(context).startLogin { success, message ->
                callback.onComplete(success, message.orEmpty())
            }
        }

        override fun logout(context: Context) {
            CodexAuthManager(context).logout()
        }

        override fun isUnauthorized(error: Throwable): Boolean =
            (error as? CodexApiException)?.isUnauthorized == true

        override suspend fun fetchModelIds(context: Context): List<String> = withContext(Dispatchers.IO) {
            CodexModelsRepository.fetchModelIds(context)
        }

        override suspend fun fetchUsage(context: Context): AccountUsageSnapshot = withContext(Dispatchers.IO) {
            val snapshot = CodexUsageRepository.fetch(context).usage
            val windows = buildList {
                snapshot.primary?.let { window ->
                    add(
                        AccountQuotaWindow(
                            kind = AccountQuotaKind.PRIMARY,
                            hasUsagePercent = true,
                            usedPercent = window.usedPercent,
                            remainingPercent = window.remainingPercent,
                            windowMinutes = window.windowMinutes,
                            resetsAtEpochSeconds = window.resetsAtEpochSeconds
                        )
                    )
                }
                snapshot.secondary?.let { window ->
                    add(
                        AccountQuotaWindow(
                            kind = AccountQuotaKind.SECONDARY,
                            hasUsagePercent = true,
                            usedPercent = window.usedPercent,
                            remainingPercent = window.remainingPercent,
                            windowMinutes = window.windowMinutes,
                            resetsAtEpochSeconds = window.resetsAtEpochSeconds
                        )
                    )
                }
            }
            AccountUsageSnapshot(windows)
        }
    }

    private object GrokProvider : AccountModelProvider {
        override val kind = AccountProviderKind.GROK
        override val label = "Grok"
        override val protocolType = ModelProtocolType.GROK_RESPONSES
        override val baseUrl = GrokAuthManager.API_BASE_URL
        override val accountScreenId = "grokAccount"

        override fun identity(context: Context): AccountIdentity {
            val auth = GrokAuthManager(context)
            return AccountIdentity(
                authenticated = auth.isAuthenticated,
                accountId = auth.userId.orEmpty(),
                email = auth.email.orEmpty(),
                plan = auth.planType.orEmpty()
            )
        }

        override fun startLogin(context: Context, callback: AccountLoginCallback) {
            GrokAuthManager(context).startLogin(object : GrokAuthManager.LoginCallback {
                override fun onUserCode(userCode: String, verificationUri: String) {
                    callback.onDeviceCode(userCode, verificationUri)
                }

                override fun onComplete(success: Boolean, message: String) {
                    callback.onComplete(success, message.orEmpty())
                }
            })
        }

        override fun logout(context: Context) {
            GrokAuthManager(context).logout()
        }

        override fun isUnauthorized(error: Throwable): Boolean =
            (error as? GrokApiException)?.isUnauthorized == true

        override suspend fun fetchModelIds(context: Context): List<String> = withContext(Dispatchers.IO) {
            GrokModelsRepository.fetchModelIds(context)
        }

        override suspend fun fetchUsage(context: Context): AccountUsageSnapshot = withContext(Dispatchers.IO) {
            val window = GrokUsageRepository.fetch(context).usage
            AccountUsageSnapshot(
                windows = listOfNotNull(
                    window?.let {
                        AccountQuotaWindow(
                            kind = AccountQuotaKind.SUBSCRIPTION,
                            hasUsagePercent = it.hasUsagePercent(),
                            usedPercent = it.usedPercent,
                            remainingPercent = it.remainingPercent,
                            resetsAtEpochSeconds = it.resetsAtEpochSeconds,
                            periodType = it.periodType.orEmpty()
                        )
                    }
                )
            )
        }
    }

    @JvmStatic
    fun fromProtocol(type: ModelProtocolType?): AccountModelProvider? = when (type) {
        ModelProtocolType.CODEX_RESPONSES -> CodexProvider
        ModelProtocolType.GROK_RESPONSES -> GrokProvider
        else -> null
    }

    @JvmStatic
    fun fromPreset(preset: ModelProviderPreset?): AccountModelProvider? =
        fromProtocol(preset?.protocolType)
}
