package cn.lineai.ui.model

import cn.lineai.model.ContextSizeParser
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelEditorViewModelTest {

    @Test
    fun plainCustomStartsUnlockedOpenAiRemote() {
        val viewModel = editor()
        val state = viewModel.state.value
        assertFalse(state.editing)
        assertFalse(state.local)
        assertFalse(state.lockedPreset)
        assertFalse(state.hasPreset)
        assertEquals(ModelProtocolType.OPENAI_COMPATIBLE, state.protocol)
        assertTrue(state.testVisible)
        assertFalse(viewModel.canSave(state))
    }

    @Test
    fun presetLocksProtocolAndBaseUrl() {
        val viewModel = editor(
            snapshot(
                presetId = "deepseek",
                presetProtocol = ModelProtocolType.OPENAI_COMPATIBLE,
                presetBaseUrl = "https://api.deepseek.com",
                presetLabel = "DeepSeek"
            )
        )
        val state = viewModel.state.value
        assertTrue(state.lockedPreset)
        assertTrue(state.hasPreset)
        assertEquals("https://api.deepseek.com", state.baseUrl)
        assertEquals("DeepSeek", state.providerLabel)
        viewModel.onAction(ModelEditorUiAction.SelectProvider(2))
        assertEquals(ModelProtocolType.OPENAI_COMPATIBLE, viewModel.state.value.protocol)
    }

    @Test
    fun editRestoresAllFieldsAndId() {
        val model = config(
            id = "edit-1",
            name = "Existing",
            protocol = ModelProtocolType.CODEX_RESPONSES,
            label = "Codex",
            baseUrl = "https://chatgpt.com/backend-api",
            apiKey = SECRET,
            modelId = "gpt-5",
            toolLimit = 12,
            compressionEnabled = true,
            compressionAuto = false,
            compressionId = "compact-1",
            contextSize = 128000
        )
        val viewModel = editor(
            snapshot(
                editingModel = model,
                cleanedEditingProviderLabel = "Codex"
            )
        )
        val state = viewModel.state.value
        assertTrue(state.editing)
        assertEquals("edit-1", state.editingId)
        assertEquals("Existing", state.name)
        assertEquals("https://chatgpt.com/backend-api", state.baseUrl)
        assertEquals(SECRET, state.apiKey)
        assertEquals("gpt-5", state.effectiveModelId)
        assertTrue(state.mainCatalog.customIdEnabled)
        assertEquals("12", state.toolCallLimitText)
        assertEquals(ContextSizeParser.format(128000), state.contextSizeText)
        assertTrue(state.compression.enabled)
        assertFalse(state.compression.auto)
        assertEquals("compact-1", state.compression.effectiveModelId)
        assertTrue(state.lockedPreset)
    }

    @Test
    fun localRequestHidesTestAndDisablesSave() {
        val viewModel = editor(snapshot(localRequested = true))
        val state = viewModel.state.value
        assertTrue(state.local)
        assertFalse(state.testVisible)
        assertFalse(viewModel.canSave(state))
        assertNull(viewModel.onAction(ModelEditorUiAction.Save))
        assertNull(viewModel.onAction(ModelEditorUiAction.Test))
    }

    @Test
    fun localEditFromGgufStaysLocal() {
        val viewModel = editor(
            snapshot(
                editingModel = config(
                    id = "local-1",
                    protocol = ModelProtocolType.LOCAL_GGUF,
                    modelId = "on-device.gguf"
                )
            )
        )
        assertTrue(viewModel.state.value.local)
        assertFalse(viewModel.state.value.testVisible)
    }

    @Test
    fun explicitContextSizeIsFormatted() {
        val viewModel = editor(
            snapshot(
                editingModel = config(id = "c1", contextSize = 32000, modelId = "m")
            )
        )
        assertEquals(ContextSizeParser.format(32000), viewModel.state.value.contextSizeText)
    }

    @Test
    fun legacyModelIdSuffixFillsContext() {
        val viewModel = editor(
            snapshot(
                editingModel = config(id = "c2", modelId = "gpt-4[128k]", contextSize = 0)
            )
        )
        assertEquals(ContextSizeParser.format(128000), viewModel.state.value.contextSizeText)
    }

    @Test
    fun providerMappingAndLockedIgnore() {
        val viewModel = editor()
        viewModel.onAction(ModelEditorUiAction.SelectProvider(1))
        assertEquals(ModelProtocolType.CODEX_RESPONSES, viewModel.state.value.protocol)
        viewModel.onAction(ModelEditorUiAction.SelectProvider(2))
        assertEquals(ModelProtocolType.ANTHROPIC_MESSAGES, viewModel.state.value.protocol)
        viewModel.onAction(ModelEditorUiAction.SelectProvider(0))
        assertEquals(ModelProtocolType.OPENAI_COMPATIBLE, viewModel.state.value.protocol)
    }

    @Test
    fun localClickOnlyToasts() {
        val viewModel = editor()
        val effect = viewModel.onAction(ModelEditorUiAction.SelectProvider(3))
        assertTrue(effect is ModelEditorUiEffect.OpenLocalFormHint)
        assertFalse(viewModel.state.value.local)
        assertEquals(ModelProtocolType.OPENAI_COMPATIBLE, viewModel.state.value.protocol)
    }

    @Test
    fun remoteClickFromLocalOnlyToasts() {
        val viewModel = editor(snapshot(localRequested = true))
        val effect = viewModel.onAction(ModelEditorUiAction.SelectProvider(0))
        assertTrue(effect is ModelEditorUiEffect.OpenCustomFormHint)
        assertTrue(viewModel.state.value.local)
    }

    @Test
    fun protocolChangeInvalidatesCatalogsWithoutRewritingBaseUrl() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogIdsValue = listOf("keep-order-b", "keep-order-a")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        assertTrue(viewModel.state.value.picker.visible)
        viewModel.onAction(ModelEditorUiAction.SelectProvider(1))
        val state = viewModel.state.value
        assertEquals("https://example.com/v1", state.baseUrl)
        assertTrue(state.mainCatalog.fetchedIds.isEmpty())
        assertEquals("", state.mainCatalog.selectedId)
        assertEquals("", state.mainCatalog.customIdText)
        assertFalse(state.picker.visible)
    }

    @Test
    fun anthropicHidesCompression() {
        val viewModel = editor()
        viewModel.onAction(ModelEditorUiAction.SelectProvider(2))
        assertFalse(viewModel.state.value.compression.supported)
        assertFalse(viewModel.state.value.compression.enabled)
    }

    @Test
    fun queryRequiresPrerequisites() {
        val repository = FakeRepository(snapshot())
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        assertEquals(0, repository.fetchCallCount)
        fillRemote(viewModel, apiKey = "")
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        assertEquals(0, repository.fetchCallCount)
    }

    @Test
    fun singleQueryAndDoubleTapDuringLoad() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogIdsValue = listOf("second", "first")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        assertEquals(1, repository.fetchCallCount)
        assertEquals(listOf("second", "first"), viewModel.state.value.mainCatalog.fetchedIds)
        assertTrue(viewModel.state.value.picker.visible)
    }

    @Test
    fun cachedQueryOpensPickerWithoutSecondCall() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogIdsValue = listOf("one")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        viewModel.onAction(ModelEditorUiAction.DismissPicker)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        assertEquals(1, repository.fetchCallCount)
        assertTrue(viewModel.state.value.picker.visible)
    }

    @Test
    fun emptyCatalogKeepsPickerClosed() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        val collector = EffectCollector()
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        collector.start(viewModel)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        assertTrue(collector.items.any { it is ModelEditorUiEffect.FetchFailed })
        assertFalse(viewModel.state.value.picker.visible)
        assertFalse(viewModel.state.value.mainCatalog.fetching)
        collector.close()
    }

    @Test
    fun catalogExceptionClearsLoading() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogErrorValue = IllegalStateException("boom")
        val collector = EffectCollector()
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        collector.start(viewModel)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        val failed = collector.items.filterIsInstance<ModelEditorUiEffect.QueryFailed>().single()
        assertEquals("boom", failed.message)
        assertFalse(viewModel.state.value.mainCatalog.fetching)
        collector.close()
    }

    @Test
    fun staleCatalogIsRejectedAfterRequestChange() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogIdsValue = listOf("old-id")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        viewModel.onAction(ModelEditorUiAction.SetBaseUrl("https://changed.example/v1"))
        dispatcher.runAll()
        assertTrue(viewModel.state.value.mainCatalog.fetchedIds.isEmpty())
        assertFalse(viewModel.state.value.picker.visible)
    }

    @Test
    fun selectingIdAndCustomRowMatchLegacy() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(
            snapshot(presetId = "openrouter", presetProtocol = ModelProtocolType.OPENAI_COMPATIBLE)
        )
        repository.catalogIdsValue = listOf("picked")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel, name = "")
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        viewModel.onAction(ModelEditorUiAction.SelectMainModel("picked"))
        assertEquals("picked", viewModel.state.value.effectiveModelId)
        assertFalse(viewModel.state.value.mainCatalog.customIdEnabled)
        assertEquals("picked", viewModel.state.value.name)
        viewModel.onAction(ModelEditorUiAction.SelectMainCustomRow)
        assertTrue(viewModel.state.value.mainCatalog.customIdEnabled)
        assertEquals("", viewModel.state.value.effectiveModelId)
    }

    @Test
    fun plainCustomDoesNotAutofillName() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogIdsValue = listOf("picked")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel, name = "")
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        viewModel.onAction(ModelEditorUiAction.SelectMainModel("picked"))
        assertEquals("", viewModel.state.value.name)
    }

    @Test
    fun saveUsesModelIdWhenNameBlank() {
        val repository = FakeRepository(snapshot())
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        fillRemote(viewModel, name = "")
        viewModel.onAction(ModelEditorUiAction.SetCustomModelId(true))
        viewModel.onAction(ModelEditorUiAction.SetModelId("only-id"))
        viewModel.onAction(ModelEditorUiAction.Save)
        assertEquals("only-id", repository.savedConfigs.single().name)
        assertEquals("only-id", repository.savedConfigs.single().modelId)
    }

    @Test
    fun missingNameIdAndApiKeyAndToolLimit() {
        val viewModel = editor()
        assertTrue(viewModel.onAction(ModelEditorUiAction.Save) is ModelEditorUiEffect.RequireNameId)
        fillRemote(viewModel, apiKey = "")
        assertTrue(viewModel.onAction(ModelEditorUiAction.Save) is ModelEditorUiEffect.RequireApiKey)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.SetToolLimit("abc"))
        assertTrue(viewModel.onAction(ModelEditorUiAction.Save) is ModelEditorUiEffect.ToolCallRangeInvalid)
        viewModel.onAction(ModelEditorUiAction.SetToolLimit("-2"))
        assertTrue(viewModel.onAction(ModelEditorUiAction.Save) is ModelEditorUiEffect.ToolCallRangeInvalid)
    }

    @Test
    fun codexOauthAllowsEmptyKeyForQueryAndSave() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot(codexAuthenticated = true))
        repository.catalogIdsValue = listOf("codex-1")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        viewModel.onAction(ModelEditorUiAction.SelectProvider(1))
        fillRemote(viewModel, apiKey = "")
        viewModel.onAction(ModelEditorUiAction.QueryMainCatalog)
        dispatcher.runAll()
        assertEquals(1, repository.fetchCallCount)
        viewModel.onAction(ModelEditorUiAction.SetCustomModelId(true))
        viewModel.onAction(ModelEditorUiAction.SetModelId("codex-1"))
        viewModel.onAction(ModelEditorUiAction.Save)
        assertEquals(1, repository.savedConfigs.size)
    }

    @Test
    fun toolLimitValuesAndContextReachConfig() {
        val repository = FakeRepository(snapshot())
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.SetToolLimit("-1"))
        viewModel.onAction(ModelEditorUiAction.SetContextSize("64k"))
        viewModel.onAction(ModelEditorUiAction.Save)
        val saved = repository.savedConfigs.single()
        assertEquals(ModelConfig.UNLIMITED_TOOL_CALLS, saved.toolCallLimit)
        assertEquals(64000, saved.contextSize)
        val repository2 = FakeRepository(snapshot())
        val viewModel2 = ModelEditorViewModel(repository2, QueuedDispatcher())
        fillRemote(viewModel2)
        viewModel2.onAction(ModelEditorUiAction.SetToolLimit("0"))
        viewModel2.onAction(ModelEditorUiAction.Save)
        assertEquals(0, repository2.savedConfigs.single().toolCallLimit)
    }

    @Test
    fun saveConfigContainsLegacyFieldsAndEditingId() {
        val repository = FakeRepository(
            snapshot(
                editingModel = config(
                    id = "keep-id",
                    name = "Old",
                    protocol = ModelProtocolType.OPENAI_COMPATIBLE,
                    apiKey = "old",
                    modelId = "old-id"
                )
            )
        )
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        viewModel.onAction(ModelEditorUiAction.SetName("New"))
        viewModel.onAction(ModelEditorUiAction.SetBaseUrl("https://example.com/v1"))
        viewModel.onAction(ModelEditorUiAction.SetApiKey("new-key"))
        viewModel.onAction(ModelEditorUiAction.SetModelId("new-id"))
        viewModel.onAction(ModelEditorUiAction.SetToolLimit("200"))
        viewModel.onAction(ModelEditorUiAction.SetContextSize("8k"))
        viewModel.onAction(ModelEditorUiAction.SetCompressionEnabled(true))
        viewModel.onAction(ModelEditorUiAction.SetCompressionAuto(false))
        viewModel.onAction(ModelEditorUiAction.SetCompressionCustom(true))
        viewModel.onAction(ModelEditorUiAction.SetCompressionModelId("cmp"))
        viewModel.onAction(ModelEditorUiAction.Save)
        val saved = repository.savedConfigs.single()
        assertEquals("keep-id", saved.id)
        assertEquals("New", saved.name)
        assertEquals(ModelProtocolType.OPENAI_COMPATIBLE, saved.protocolType)
        assertEquals("https://example.com/v1", saved.baseUrl)
        assertEquals("new-key", saved.apiKey)
        assertEquals("new-id", saved.modelId)
        assertEquals(200, saved.toolCallLimit)
        assertTrue(saved.isCompressionModelEnabled)
        assertFalse(saved.isCompressionModelAuto)
        assertEquals("cmp", saved.compressionModelId)
        assertEquals(8000, saved.contextSize)
    }

    @Test
    fun doubleSaveAndExceptionUnlock() {
        val repository = FakeRepository(snapshot())
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.Save)
        viewModel.onAction(ModelEditorUiAction.Save)
        assertEquals(1, repository.savedConfigs.size)

        val failing = FakeRepository(snapshot())
        failing.saveErrorValue = IllegalStateException("nope")
        val failingVm = ModelEditorViewModel(failing, QueuedDispatcher())
        fillRemote(failingVm)
        val effect = failingVm.onAction(ModelEditorUiAction.Save)
        assertTrue(effect is ModelEditorUiEffect.SaveFailed)
        assertFalse(failingVm.state.value.isSaving)
        failing.saveErrorValue = null
        failingVm.onAction(ModelEditorUiAction.Save)
        assertEquals(1, failing.savedConfigs.size)
    }

    @Test
    fun testMissingInvalidLimitAndLocalNoop() {
        val repository = FakeRepository(snapshot())
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        assertTrue(viewModel.onAction(ModelEditorUiAction.Test) is ModelEditorUiEffect.TestMissing)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.SetToolLimit("nope"))
        viewModel.onAction(ModelEditorUiAction.Test)
        assertEquals(ModelConfig.DEFAULT_TOOL_CALL_LIMIT, repository.testedConfigs.single().toolCallLimit)
        viewModel.onAction(ModelEditorUiAction.Test)
        assertEquals(2, repository.testedConfigs.size)
        val local = editor(snapshot(localRequested = true))
        assertNull(local.onAction(ModelEditorUiAction.Test))
    }

    @Test
    fun compressionReadinessAndKeyRequirement() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot(codexAuthenticated = true))
        repository.catalogIdsValue = listOf("cmp")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.SelectProvider(1))
        viewModel.onAction(ModelEditorUiAction.SetApiKey(""))
        viewModel.onAction(ModelEditorUiAction.SetCompressionEnabled(true))
        viewModel.onAction(ModelEditorUiAction.SetCompressionAuto(false))
        assertTrue(viewModel.onAction(ModelEditorUiAction.Save) is ModelEditorUiEffect.RequireCompactionId)
        viewModel.onAction(ModelEditorUiAction.QueryCompressionCatalog)
        assertEquals(0, repository.fetchCallCount)
        viewModel.onAction(ModelEditorUiAction.SetApiKey("key"))
        viewModel.onAction(ModelEditorUiAction.QueryCompressionCatalog)
        viewModel.onAction(ModelEditorUiAction.QueryCompressionCatalog)
        dispatcher.runAll()
        assertEquals(1, repository.fetchCallCount)
        viewModel.onAction(ModelEditorUiAction.SelectCompressionModel("cmp"))
        viewModel.onAction(ModelEditorUiAction.Save)
        assertEquals("cmp", repository.savedConfigs.single().compressionModelId)
        assertTrue(repository.savedConfigs.single().isCompressionModelEnabled)
    }

    @Test
    fun compressionStaleRejectedAndCustomRow() {
        val dispatcher = QueuedDispatcher()
        val repository = FakeRepository(snapshot())
        repository.catalogIdsValue = listOf("cmp-a", "cmp-b")
        val viewModel = ModelEditorViewModel(repository, dispatcher)
        fillRemote(viewModel)
        viewModel.onAction(ModelEditorUiAction.SetCompressionEnabled(true))
        viewModel.onAction(ModelEditorUiAction.SetCompressionAuto(false))
        viewModel.onAction(ModelEditorUiAction.QueryCompressionCatalog)
        viewModel.onAction(ModelEditorUiAction.SetApiKey("changed-key"))
        dispatcher.runAll()
        assertTrue(viewModel.state.value.compression.catalog.fetchedIds.isEmpty())
        viewModel.onAction(ModelEditorUiAction.QueryCompressionCatalog)
        dispatcher.runAll()
        viewModel.onAction(ModelEditorUiAction.SelectCompressionCustomRow)
        assertTrue(viewModel.state.value.compression.catalog.customIdEnabled)
        assertEquals("", viewModel.state.value.compression.effectiveModelId)
    }

    @Test
    fun toStringRedactsSecretKey() {
        val snapshot = snapshot(
            editingModel = config(id = "s", apiKey = SECRET, modelId = "m")
        )
        val viewModel = editor(snapshot)
        val request = CatalogRequestSnapshot(
            ModelProtocolType.OPENAI_COMPATIBLE,
            "https://example.com/v1",
            SECRET
        )
        assertFalse(viewModel.state.value.toString().contains(SECRET))
        assertFalse(snapshot.toString().contains(SECRET))
        assertFalse(request.toString().contains(SECRET))
        assertFalse(viewModel.state.value.mainCatalog.toString().contains(SECRET))
        assertFalse(viewModel.state.value.compression.toString().contains(SECRET))
    }

    @Test
    fun backDoesNotSave() {
        val repository = FakeRepository(snapshot())
        val viewModel = ModelEditorViewModel(repository, QueuedDispatcher())
        fillRemote(viewModel)
        assertTrue(viewModel.onAction(ModelEditorUiAction.Back) is ModelEditorUiEffect.Back)
        assertTrue(repository.savedConfigs.isEmpty())
    }

    private fun editor(snapshot: ModelEditorSnapshot = snapshot()): ModelEditorViewModel {
        return ModelEditorViewModel(FakeRepository(snapshot), QueuedDispatcher())
    }

    private fun fillRemote(
        viewModel: ModelEditorViewModel,
        name: String = "phone-test-model",
        apiKey: String = "test-only-not-secret"
    ) {
        viewModel.onAction(ModelEditorUiAction.SetName(name))
        viewModel.onAction(ModelEditorUiAction.SetBaseUrl("https://example.com/v1"))
        viewModel.onAction(ModelEditorUiAction.SetApiKey(apiKey))
        viewModel.onAction(ModelEditorUiAction.SetCustomModelId(true))
        viewModel.onAction(ModelEditorUiAction.SetModelId("phone-test-model"))
        viewModel.onAction(ModelEditorUiAction.SetToolLimit("200"))
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val queue = ArrayDeque<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            queue.addLast(block)
        }

        fun runAll() {
            while (queue.isNotEmpty()) {
                queue.removeFirst().run()
            }
        }
    }

    private class EffectCollector {
        val items = CopyOnWriteArrayList<ModelEditorUiEffect>()
        private var scope: CoroutineScope? = null

        fun start(viewModel: ModelEditorViewModel) {
            val next = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            scope = next
            next.launch { viewModel.effects.collect { items += it } }
        }

        fun close() {
            scope?.cancel()
        }
    }

    private class FakeRepository(
        private val snapshotValue: ModelEditorSnapshot
    ) : ModelEditorRepository {
        var catalogIdsValue: List<String> = emptyList()
        var catalogErrorValue: Exception? = null
        var saveErrorValue: Exception? = null
        var fetchCallCount: Int = 0
        val savedConfigs = mutableListOf<ModelConfig>()
        val testedConfigs = mutableListOf<ModelConfig>()

        override fun loadSnapshot(): ModelEditorSnapshot = snapshotValue

        override fun isCodexAuthenticated(): Boolean = snapshotValue.codexAuthenticated

        override fun fetchModelCatalog(
            type: ModelProtocolType,
            baseUrl: String,
            apiKey: String
        ): List<String> {
            fetchCallCount++
            catalogErrorValue?.let { throw it }
            return catalogIdsValue
        }

        override fun saveModel(config: ModelConfig) {
            saveErrorValue?.let { throw it }
            savedConfigs += config
        }

        override fun testModel(config: ModelConfig) {
            testedConfigs += config
        }
    }

    companion object {
        private const val SECRET = "SECRET_MODEL_API_KEY_FOR_TEST"

        fun snapshot(
            presetId: String? = null,
            presetProtocol: ModelProtocolType? = null,
            presetBaseUrl: String = "",
            presetPlaceholder: String = "",
            presetHint: String = "",
            presetLabel: String? = null,
            localRequested: Boolean = false,
            editingModel: ModelConfig? = null,
            cleanedEditingProviderLabel: String? = null,
            customProviderLabelSentinel: String = "Custom",
            localProviderLabel: String = "Local",
            codexAuthenticated: Boolean = false
        ) = ModelEditorSnapshot(
            presetId,
            presetProtocol,
            presetBaseUrl,
            presetPlaceholder,
            presetHint,
            presetLabel,
            localRequested,
            editingModel,
            cleanedEditingProviderLabel,
            customProviderLabelSentinel,
            localProviderLabel,
            codexAuthenticated
        )

        fun config(
            id: String = "",
            name: String = "Name",
            protocol: ModelProtocolType = ModelProtocolType.OPENAI_COMPATIBLE,
            label: String = protocol.label,
            baseUrl: String = "https://api.openai.com/v1",
            apiKey: String = "key",
            modelId: String = "model",
            toolLimit: Int = ModelConfig.DEFAULT_TOOL_CALL_LIMIT,
            compressionEnabled: Boolean = false,
            compressionAuto: Boolean = true,
            compressionId: String = "",
            contextSize: Int = 0
        ) = ModelConfig(
            id,
            name,
            protocol,
            label,
            baseUrl,
            apiKey,
            modelId,
            toolLimit,
            compressionEnabled,
            compressionAuto,
            compressionId,
            contextSize
        )
    }
}
