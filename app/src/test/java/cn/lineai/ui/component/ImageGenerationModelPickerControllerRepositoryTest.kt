package cn.lineai.ui.component

import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.ui.model.ImageGenerationModelItemUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageGenerationModelPickerControllerRepositoryTest {

    @Test
    fun eachSnapshotReadsFreshGatewayListAndKeepsOrder() {
        val first = listOf(
            model("id-1", "One", "one-id", ModelProtocolType.OPENAI_COMPATIBLE, "OpenAI"),
            model("id-2", "Two", "two-id", ModelProtocolType.CODEX_RESPONSES, "Codex")
        )
        val gateway = RecordingGateway(first, "id-1")
        val repository = ImageGenerationModelPickerControllerRepository(gateway)

        val one = repository.snapshot()
        gateway.modelList = listOf(
            model("id-2", "Two", "two-id", ModelProtocolType.CODEX_RESPONSES, "Codex"),
            model("id-3", "Three", "three-id", ModelProtocolType.GROK_RESPONSES, "Grok")
        )
        gateway.selectedId = "id-3"
        val two = repository.snapshot()

        assertEquals(2, gateway.listReads)
        assertEquals(listOf("id-1", "id-2"), one.models.map { it.internalId })
        assertEquals(listOf("id-2", "id-3"), two.models.map { it.internalId })
        assertEquals("id-1", one.selectedInternalId)
        assertEquals("id-3", two.selectedInternalId)
    }

    @Test
    fun selectedIdComesFromGenerationSettingNotUnderstanding() {
        val models = listOf(
            model("understand-1", "Vision", "vision-id", ModelProtocolType.OPENAI_COMPATIBLE, "OpenAI"),
            model("generate-9", "Image", "image-id", ModelProtocolType.GROK_RESPONSES, "Grok")
        )
        val gateway = RecordingGateway(models, "generate-9")
        val snapshot = ImageGenerationModelPickerControllerRepository(gateway).snapshot()

        assertEquals("generate-9", snapshot.selectedInternalId)
        assertFalse(snapshot.models[0].selected)
        assertTrue(snapshot.models[1].selected)
        assertEquals("image-id", snapshot.models[1].displayedModelId)
        assertEquals("generate-9", snapshot.models[1].internalId)
    }

    @Test
    fun providerLabelFallbackAndBadgeColorsMatchLegacy() {
        val models = listOf(
            model("c", "Codex", "codex-1", ModelProtocolType.CODEX_RESPONSES, "Codex"),
            model("a", "Claude", "claude-1", ModelProtocolType.ANTHROPIC_MESSAGES, "Anthropic"),
            model("l", "Local", "file.gguf", ModelProtocolType.LOCAL_GGUF, "Local"),
            model("o", "GPT", "gpt-4o", ModelProtocolType.OPENAI_COMPATIBLE, "OpenAI"),
            model("g", "Grok", "grok-1", ModelProtocolType.GROK_RESPONSES, "Grok"),
            model("custom", "Mine", "mine", ModelProtocolType.OPENAI_COMPATIBLE, "自定义"),
            model("blank", "Blank", "blank", ModelProtocolType.ANTHROPIC_MESSAGES, "")
        )
        val snapshot = ImageGenerationModelPickerControllerRepository(
            RecordingGateway(models, "c")
        ).snapshot()

        assertEquals("Codex", snapshot.models[0].badgeLabel)
        assertEquals("Anthropic", snapshot.models[1].badgeLabel)
        assertEquals("Local", snapshot.models[2].badgeLabel)
        assertEquals("OpenAI", snapshot.models[3].badgeLabel)
        assertEquals("Grok", snapshot.models[4].badgeLabel)
        assertEquals("OpenAI", snapshot.models[5].badgeLabel)
        assertEquals("Anthropic", snapshot.models[6].badgeLabel)

        assertEquals(ImageGenerationModelPickerControllerRepository.BADGE_CODEX, snapshot.models[0].badgeColor)
        assertEquals(ImageGenerationModelPickerControllerRepository.BADGE_ANTHROPIC, snapshot.models[1].badgeColor)
        assertEquals(ImageGenerationModelPickerControllerRepository.BADGE_LOCAL, snapshot.models[2].badgeColor)
        assertEquals(ImageGenerationModelPickerControllerRepository.BADGE_DEFAULT, snapshot.models[3].badgeColor)
        assertEquals(ImageGenerationModelPickerControllerRepository.BADGE_DEFAULT, snapshot.models[4].badgeColor)
    }

    @Test
    fun selectedIdAndSelectDelegateUseInternalIdOnce() {
        val models = listOf(
            model("internal-1", "Image", "displayed-image", ModelProtocolType.OPENAI_COMPATIBLE, "OpenAI")
        )
        val gateway = RecordingGateway(models, "internal-1")
        val repository = ImageGenerationModelPickerControllerRepository(gateway)

        val snapshot = repository.snapshot()
        assertEquals("internal-1", snapshot.selectedInternalId)
        assertTrue(snapshot.models.single().selected)
        assertEquals("displayed-image", snapshot.models.single().displayedModelId)

        repository.selectModel("internal-1")
        assertEquals(listOf("internal-1"), gateway.selectedCalls)
    }

    @Test
    fun snapshotDoesNotExposeModelConfigOrSecrets() {
        val secret = "super-secret-token-xyz"
        val models = listOf(
            model("id-1", "Safe", "safe-id", ModelProtocolType.OPENAI_COMPATIBLE, "OpenAI", secret)
        )
        val snapshot = ImageGenerationModelPickerControllerRepository(
            RecordingGateway(models, "id-1")
        ).snapshot()
        val text = snapshot.toString()

        assertFalse(text.contains(secret))
        snapshot.models.forEach { item ->
            assertFalse(item.toString().contains(secret))
            assertTrue(item is ImageGenerationModelItemUi)
        }
        assertFalse(snapshot.models.any { it.internalId == secret })
        assertFalse(snapshot.models.any { it.displayedModelId == secret })
        assertFalse(snapshot.models.any { it.name == secret })
    }

    private class RecordingGateway(
        var modelList: List<ModelConfig>,
        var selectedId: String
    ) : ImageGenerationModelPickerLegacyGateway {
        var listReads = 0
        val selectedCalls = mutableListOf<String>()

        override fun models(): List<ModelConfig> {
            listReads += 1
            return modelList
        }

        override fun selectedInternalId(): String = selectedId

        override fun selectImageGenerationModel(internalId: String) {
            selectedCalls += internalId
            selectedId = internalId
        }
    }

    companion object {
        private fun model(
            id: String,
            name: String,
            modelId: String,
            protocol: ModelProtocolType,
            provider: String,
            apiKey: String = "unused-key"
        ): ModelConfig {
            return ModelConfig.builder(
                id,
                name,
                protocol,
                provider,
                "https://example.invalid",
                apiKey,
                modelId
            ).build()
        }
    }
}
