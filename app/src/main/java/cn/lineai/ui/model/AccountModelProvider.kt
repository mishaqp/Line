package cn.lineai.ui.model

import android.content.Context
import cn.lineai.data.codex.CodexAuthManager
import cn.lineai.data.codex.CodexModelsRepository
import cn.lineai.data.grok.GrokAuthManager
import cn.lineai.data.grok.GrokModelsRepository
import cn.lineai.model.ModelProtocolType
import cn.lineai.model.ModelProviderPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AccountProviderKind {
    CODEX,
    GROK
}

interface AccountModelProvider {
    val kind: AccountProviderKind
    val label: String
    val protocolType: ModelProtocolType
    val baseUrl: String
    val accountScreenId: String

    fun isAuthenticated(context: Context): Boolean
    fun email(context: Context): String
    fun plan(context: Context): String
    suspend fun fetchModelIds(context: Context): List<String>
}

object AccountModelProviders {
    private object CodexProvider : AccountModelProvider {
        override val kind = AccountProviderKind.CODEX
        override val label = "Codex"
        override val protocolType = ModelProtocolType.CODEX_RESPONSES
        override val baseUrl = "https://api.openai.com/v1"
        override val accountScreenId = "codexAccount"

        override fun isAuthenticated(context: Context): Boolean = CodexAuthManager.isAuthenticated(context)
        override fun email(context: Context): String = CodexAuthManager(context).email.orEmpty()
        override fun plan(context: Context): String = CodexAuthManager(context).planType.orEmpty()
        override suspend fun fetchModelIds(context: Context): List<String> = withContext(Dispatchers.IO) {
            CodexModelsRepository.fetchModelIds(context)
        }
    }

    private object GrokProvider : AccountModelProvider {
        override val kind = AccountProviderKind.GROK
        override val label = "Grok"
        override val protocolType = ModelProtocolType.GROK_RESPONSES
        override val baseUrl = GrokAuthManager.API_BASE_URL
        override val accountScreenId = "grokAccount"

        override fun isAuthenticated(context: Context): Boolean = GrokAuthManager.isAuthenticated(context)
        override fun email(context: Context): String = GrokAuthManager(context).email.orEmpty()
        override fun plan(context: Context): String = GrokAuthManager(context).planType.orEmpty()
        override suspend fun fetchModelIds(context: Context): List<String> = withContext(Dispatchers.IO) {
            GrokModelsRepository.fetchModelIds(context)
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
