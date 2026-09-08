package cn.lineai.ui.component

import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.ui.model.ImageGenerationModelItemUi
import cn.lineai.ui.model.ImageGenerationModelPickerRepository
import cn.lineai.ui.model.ImageGenerationModelPickerSnapshot

interface ImageGenerationModelPickerLegacyGateway {
    fun models(): List<ModelConfig>
    fun selectedInternalId(): String
    fun selectImageGenerationModel(internalId: String)
}

class ImageGenerationModelPickerControllerRepository(
    private val gateway: ImageGenerationModelPickerLegacyGateway
) : ImageGenerationModelPickerRepository {

    override fun snapshot(): ImageGenerationModelPickerSnapshot {
        val selectedId = gateway.selectedInternalId().orEmpty()
        val models = gateway.models().orEmpty().map { model ->
            toItem(model, selectedId)
        }
        return ImageGenerationModelPickerSnapshot(
            models = models,
            selectedInternalId = selectedId
        )
    }

    override fun selectModel(internalId: String) {
        gateway.selectImageGenerationModel(internalId)
    }

    companion object {
        private const val CUSTOM_PROVIDER_LABEL = "自定义"

        internal const val BADGE_CODEX = 0xFF4B8BFF.toInt()
        internal const val BADGE_ANTHROPIC = 0xFFB86F50.toInt()
        internal const val BADGE_LOCAL = 0xFF2E7D62.toInt()
        internal const val BADGE_DEFAULT = 0xFF10A37F.toInt()

        internal fun badgeLabel(model: ModelConfig): String {
            val provider = model.providerLabel
            return if (provider.isNullOrEmpty() || CUSTOM_PROVIDER_LABEL == provider) {
                model.protocolType.label
            } else {
                provider
            }
        }

        internal fun badgeColor(model: ModelConfig): Int = when (model.protocolType) {
            ModelProtocolType.CODEX_RESPONSES -> BADGE_CODEX
            ModelProtocolType.ANTHROPIC_MESSAGES -> BADGE_ANTHROPIC
            ModelProtocolType.LOCAL_GGUF -> BADGE_LOCAL
            ModelProtocolType.OPENAI_COMPATIBLE,
            ModelProtocolType.GROK_RESPONSES -> BADGE_DEFAULT
        }

        private fun toItem(model: ModelConfig, selectedId: String): ImageGenerationModelItemUi {
            return ImageGenerationModelItemUi(
                internalId = model.id,
                name = model.name,
                displayedModelId = model.modelId,
                badgeLabel = badgeLabel(model),
                badgeColor = badgeColor(model),
                selected = model.id == selectedId
            )
        }
    }
}
