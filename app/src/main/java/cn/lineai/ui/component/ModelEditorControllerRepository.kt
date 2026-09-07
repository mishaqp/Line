package cn.lineai.ui.component

import cn.lineai.log.ErrorLog
import cn.lineai.log.ErrorLogRedactor
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.model.ModelProviderPreset
import cn.lineai.ui.model.ModelEditorRepository
import cn.lineai.ui.model.ModelEditorSnapshot
import cn.lineai.ui.model.ModelEditorViewModel

interface ModelEditorLegacyGateway {
    fun preset(): ModelProviderPreset?

    fun localRequested(): Boolean

    fun editingModel(): ModelConfig?

    fun presetLabel(): String?

    fun presetHint(): String

    fun localProviderLabel(): String

    fun customProviderLabelSentinel(): String

    fun isCodexAuthenticated(): Boolean

    @Throws(Exception::class)
    fun fetchModelCatalog(type: ModelProtocolType, baseUrl: String, apiKey: String): List<String>

    fun saveModel(config: ModelConfig)

    fun testModel(config: ModelConfig)
}

class ModelEditorControllerRepository(
    private val gateway: ModelEditorLegacyGateway
) : ModelEditorRepository {

    override fun loadSnapshot(): ModelEditorSnapshot {
        val preset = gateway.preset()
        val editing = gateway.editingModel()
        val sentinel = gateway.customProviderLabelSentinel()
        return ModelEditorSnapshot(
            presetId = preset?.id,
            presetProtocol = preset?.protocolType,
            presetBaseUrl = preset?.baseUrl.orEmpty(),
            presetPlaceholder = preset?.placeholder.orEmpty(),
            presetHint = gateway.presetHint(),
            presetLabel = gateway.presetLabel(),
            localRequested = gateway.localRequested(),
            editingModel = editing,
            cleanedEditingProviderLabel = ModelEditorViewModel.cleanProviderLabel(
                editing?.providerLabel,
                sentinel
            ),
            customProviderLabelSentinel = sentinel,
            localProviderLabel = gateway.localProviderLabel(),
            codexAuthenticated = gateway.isCodexAuthenticated()
        )
    }

    override fun isCodexAuthenticated(): Boolean = gateway.isCodexAuthenticated()

    override fun fetchModelCatalog(
        type: ModelProtocolType,
        baseUrl: String,
        apiKey: String
    ): List<String> {
        return try {
            gateway.fetchModelCatalog(type, baseUrl, apiKey) ?: emptyList()
        } catch (error: Exception) {
            try {
                ErrorLog.record(
                    "model_catalog",
                    "model catalog query failed",
                    error,
                    "protocol=" + type + ", baseUrl=" + baseUrl +
                        ", apiKey=" + ErrorLogRedactor.redact("Authorization=Bearer " + apiKey)
                )
            } catch (_: Exception) {
                // Logging must not hide the catalog failure.
            }
            throw error
        }
    }

    override fun saveModel(config: ModelConfig) {
        gateway.saveModel(config)
    }

    override fun testModel(config: ModelConfig) {
        gateway.testModel(config)
    }
}
