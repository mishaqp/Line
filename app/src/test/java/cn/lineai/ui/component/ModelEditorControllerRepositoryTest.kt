package cn.lineai.ui.component

import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.model.ModelProviderPreset
import cn.lineai.ui.model.ModelEditorViewModelTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelEditorControllerRepositoryTest {

    @Test
    fun initialInputsAreNotMutated() {
        val preset = ModelProviderPreset(
            "deepseek",
            ModelProtocolType.OPENAI_COMPATIBLE,
            "https://api.deepseek.com",
            "https://api.deepseek.com"
        )
        val editing = ModelEditorViewModelTest.config(id = "id-1", apiKey = SECRET)
        val gateway = FakeGateway(
            presetValue = preset,
            editingModelValue = editing,
            presetLabelValue = "DeepSeek",
            presetHintValue = "hint",
            localProviderLabelValue = "Local",
            customProviderLabelSentinelValue = "Custom",
            codexAuthenticatedValue = true
        )
        val snapshot = ModelEditorControllerRepository(gateway).loadSnapshot()
        assertEquals("deepseek", snapshot.presetId)
        assertEquals(ModelProtocolType.OPENAI_COMPATIBLE, snapshot.presetProtocol)
        assertEquals("https://api.deepseek.com", snapshot.presetBaseUrl)
        assertEquals("DeepSeek", snapshot.presetLabel)
        assertEquals("hint", snapshot.presetHint)
        assertSame(editing, snapshot.editingModel)
        assertTrue(snapshot.codexAuthenticated)
        assertFalse(snapshot.toString().contains(SECRET))
    }

    @Test
    fun fetchSaveAndTestDelegateExactlyOnce() {
        val gateway = FakeGateway(codexAuthenticatedValue = true)
        val repository = ModelEditorControllerRepository(gateway)
        val ids = repository.fetchModelCatalog(
            ModelProtocolType.CODEX_RESPONSES,
            "https://chatgpt.com/backend-api",
            SECRET
        )
        val config = ModelEditorViewModelTest.config(id = "saved", apiKey = SECRET)
        repository.saveModel(config)
        repository.testModel(config)

        assertEquals(1, gateway.fetchCallCount)
        assertEquals(ModelProtocolType.CODEX_RESPONSES, gateway.lastFetchType)
        assertEquals("https://chatgpt.com/backend-api", gateway.lastFetchBaseUrl)
        assertEquals(SECRET, gateway.lastFetchApiKey)
        assertEquals(listOf("one", "two"), ids)
        assertEquals(1, gateway.saveCallCount)
        assertSame(config, gateway.savedConfigValue)
        assertEquals(1, gateway.testCallCount)
        assertSame(config, gateway.testedConfigValue)
        assertTrue(repository.isCodexAuthenticated())
    }

    @Test
    fun fetchExceptionIsRethrownAndDoesNotSwallow() {
        val gateway = FakeGateway(fetchErrorValue = IllegalStateException("catalog down"))
        val repository = ModelEditorControllerRepository(gateway)
        var thrown: Exception? = null
        try {
            repository.fetchModelCatalog(
                ModelProtocolType.OPENAI_COMPATIBLE,
                "https://example.com/v1",
                SECRET
            )
        } catch (error: Exception) {
            thrown = error
        }
        assertEquals("catalog down", thrown?.message)
        assertEquals(1, gateway.fetchCallCount)
    }

    private class FakeGateway(
        private val presetValue: ModelProviderPreset? = null,
        private val localRequestedValue: Boolean = false,
        private val editingModelValue: ModelConfig? = null,
        private val presetLabelValue: String? = null,
        private val presetHintValue: String = "",
        private val localProviderLabelValue: String = "Local",
        private val customProviderLabelSentinelValue: String = "Custom",
        private val codexAuthenticatedValue: Boolean = false,
        var fetchErrorValue: Exception? = null
    ) : ModelEditorLegacyGateway {
        var fetchCallCount: Int = 0
        var saveCallCount: Int = 0
        var testCallCount: Int = 0
        var lastFetchType: ModelProtocolType? = null
        var lastFetchBaseUrl: String = ""
        var lastFetchApiKey: String = ""
        var savedConfigValue: ModelConfig? = null
        var testedConfigValue: ModelConfig? = null

        override fun preset(): ModelProviderPreset? = presetValue

        override fun localRequested(): Boolean = localRequestedValue

        override fun editingModel(): ModelConfig? = editingModelValue

        override fun presetLabel(): String? = presetLabelValue

        override fun presetHint(): String = presetHintValue

        override fun localProviderLabel(): String = localProviderLabelValue

        override fun customProviderLabelSentinel(): String = customProviderLabelSentinelValue

        override fun isCodexAuthenticated(): Boolean = codexAuthenticatedValue

        override fun fetchModelCatalog(
            type: ModelProtocolType,
            baseUrl: String,
            apiKey: String
        ): List<String> {
            fetchCallCount++
            lastFetchType = type
            lastFetchBaseUrl = baseUrl
            lastFetchApiKey = apiKey
            fetchErrorValue?.let { throw it }
            return listOf("one", "two")
        }

        override fun saveModel(config: ModelConfig) {
            saveCallCount++
            savedConfigValue = config
        }

        override fun testModel(config: ModelConfig) {
            testCallCount++
            testedConfigValue = config
        }
    }

    companion object {
        private const val SECRET = "SECRET_MODEL_API_KEY_FOR_TEST"
    }
}
