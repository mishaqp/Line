package cn.lineai.ui.model

import cn.lineai.model.WebSearchConfig
import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSettingsViewModelTest {

    @Test
    fun initialSnapshotDoesNotSave() {
        val repository = RecordingRepository(snapshot("Vision", "Draw", tavily("secret-key")))
        val viewModel = ToolSettingsViewModel(repository)

        assertEquals("Vision", viewModel.state.value.imageUnderstandingLabel)
        assertEquals("Draw", viewModel.state.value.imageGenerationLabel)
        assertEquals(WebSearchConfig.PROVIDER_TAVILY, viewModel.state.value.provider)
        assertEquals("secret-key", viewModel.state.value.apiKey)
        assertTrue(viewModel.state.value.showSearchFields)
        assertEquals(1, repository.snapshotReads)
        assertEquals(0, repository.saveCount)
        assertFalse(viewModel.state.value.toString().contains("secret-key"))
    }

    @Test
    fun backIsEmittedOnceAndReloadDoesNotRepeatIt() {
        val viewModel = ToolSettingsViewModel(RecordingRepository())

        assertEquals(ToolSettingsUiEffect.Back, viewModel.onAction(ToolSettingsUiAction.Back))
        assertNull(viewModel.onAction(ToolSettingsUiAction.Reload))
    }

    @Test
    fun modelButtonsEmitTypedDestinationsOnce() {
        val viewModel = ToolSettingsViewModel(RecordingRepository())

        val understanding = viewModel.onAction(ToolSettingsUiAction.OpenImageUnderstandingModel)
        val generation = viewModel.onAction(ToolSettingsUiAction.OpenImageGenerationModel)

        assertTrue(understanding is ToolSettingsUiEffect.Navigate)
        assertEquals(
            LineDestination.ImageUnderstandingModel,
            (understanding as ToolSettingsUiEffect.Navigate).destination
        )
        assertEquals(
            LineDestination.ImageGenerationModel,
            (generation as ToolSettingsUiEffect.Navigate).destination
        )
        assertNull(viewModel.onAction(ToolSettingsUiAction.Reload))
    }

    @Test
    fun reloadReadsFreshLabelsAndConfigWithoutSavingOrNavigating() {
        val repository = RecordingRepository(snapshot("Old A", "Old B", tavily("keep-me")))
        val viewModel = ToolSettingsViewModel(repository)
        repository.stored = snapshot("New A · id-a", "New B · id-b", brave("next-key"))

        assertNull(viewModel.onAction(ToolSettingsUiAction.Reload))

        assertEquals("New A · id-a", viewModel.state.value.imageUnderstandingLabel)
        assertEquals("New B · id-b", viewModel.state.value.imageGenerationLabel)
        assertEquals(WebSearchConfig.PROVIDER_BRAVE, viewModel.state.value.provider)
        assertEquals("next-key", viewModel.state.value.apiKey)
        assertEquals(0, repository.saveCount)
        assertEquals(2, repository.snapshotReads)
        assertFalse(viewModel.state.value.toString().contains("next-key"))
    }

    @Test
    fun eachFieldChangeSavesCompleteConfigOnce() {
        val repository = RecordingRepository(snapshot("", "", tavily("old")))
        val viewModel = ToolSettingsViewModel(repository)

        viewModel.onAction(ToolSettingsUiAction.ChangeBaseUrl("https://search.example"))
        viewModel.onAction(ToolSettingsUiAction.ChangeApiKey("k2"))
        viewModel.onAction(ToolSettingsUiAction.ChangeModel("advanced"))
        viewModel.onAction(ToolSettingsUiAction.ChangeQueryParam("q"))
        viewModel.onAction(ToolSettingsUiAction.ChangeApiKeyHeader("Authorization"))
        viewModel.onAction(ToolSettingsUiAction.ChangeApiKeyParam("api_key"))

        assertEquals(6, repository.saveCount)
        val last = repository.saved.last()
        assertEquals(WebSearchConfig.PROVIDER_TAVILY, last.provider)
        assertEquals("https://search.example", last.baseUrl)
        assertEquals("k2", last.apiKey)
        assertEquals("advanced", last.model)
        assertEquals("q", last.queryParam)
        assertEquals("Authorization", last.apiKeyHeader)
        assertEquals("api_key", last.apiKeyParam)
    }

    @Test
    fun selectingProviderAppliesExactDefaultsAndClearsApiKeyWithOneSave() {
        val repository = RecordingRepository(snapshot("", "", tavily("should-clear")))
        val viewModel = ToolSettingsViewModel(repository)

        viewModel.onAction(ToolSettingsUiAction.SelectProvider(WebSearchConfig.PROVIDER_BRAVE))

        val expected = WebSearchConfig.defaultConfig(WebSearchConfig.PROVIDER_BRAVE)
        assertEquals(1, repository.saveCount)
        assertConfig(expected, repository.saved.single())
        assertEquals(expected.provider, viewModel.state.value.provider)
        assertEquals(expected.baseUrl, viewModel.state.value.baseUrl)
        assertEquals("", viewModel.state.value.apiKey)
        assertEquals(expected.model, viewModel.state.value.model)
        assertEquals(expected.queryParam, viewModel.state.value.queryParam)
        assertEquals(expected.apiKeyHeader, viewModel.state.value.apiKeyHeader)
        assertEquals(expected.apiKeyParam, viewModel.state.value.apiKeyParam)
        assertTrue(viewModel.state.value.showSearchFields)
    }

    @Test
    fun bingRssFreeHidesFieldsAndOtherProvidersShowThem() {
        val repository = RecordingRepository(snapshot("", "", tavily("hidden")))
        val viewModel = ToolSettingsViewModel(repository)
        assertTrue(viewModel.state.value.showSearchFields)

        viewModel.onAction(ToolSettingsUiAction.SelectProvider(WebSearchConfig.PROVIDER_BING_RSS_FREE))
        assertFalse(viewModel.state.value.showSearchFields)
        assertEquals("", viewModel.state.value.apiKey)

        val others = listOf(
            WebSearchConfig.PROVIDER_TAVILY,
            WebSearchConfig.PROVIDER_BRAVE,
            WebSearchConfig.PROVIDER_SERPAPI,
            WebSearchConfig.PROVIDER_BING,
            WebSearchConfig.PROVIDER_CUSTOM
        )
        others.forEach { provider ->
            viewModel.onAction(ToolSettingsUiAction.SelectProvider(provider))
            assertTrue(provider, viewModel.state.value.showSearchFields)
            assertEquals(provider, WebSearchConfig.defaultConfig(provider).provider, viewModel.state.value.provider)
            assertEquals("", viewModel.state.value.apiKey)
            assertConfig(WebSearchConfig.defaultConfig(provider), repository.saved.last())
        }
    }

    private fun assertConfig(expected: WebSearchConfig, actual: WebSearchConfig) {
        assertEquals(expected.provider, actual.provider)
        assertEquals(expected.baseUrl, actual.baseUrl)
        assertEquals(expected.apiKey, actual.apiKey)
        assertEquals(expected.model, actual.model)
        assertEquals(expected.queryParam, actual.queryParam)
        assertEquals(expected.apiKeyHeader, actual.apiKeyHeader)
        assertEquals(expected.apiKeyParam, actual.apiKeyParam)
    }

    private class RecordingRepository(
        var stored: ToolSettingsSnapshot = snapshot("", "", WebSearchConfig.defaultConfig())
    ) : ToolSettingsRepository {
        var snapshotReads = 0
        val saved = mutableListOf<WebSearchConfig>()
        val saveCount: Int get() = saved.size

        override fun snapshot(): ToolSettingsSnapshot {
            snapshotReads++
            return stored
        }

        override fun saveWebSearchConfig(config: WebSearchConfig) {
            saved += config
            stored = stored.copy(webSearch = config)
        }
    }

    companion object {
        private fun snapshot(
            understanding: String,
            generation: String,
            config: WebSearchConfig
        ): ToolSettingsSnapshot = ToolSettingsSnapshot(
            imageUnderstandingLabel = understanding,
            imageGenerationLabel = generation,
            webSearch = config
        )

        private fun tavily(apiKey: String): WebSearchConfig {
            val defaults = WebSearchConfig.defaultConfig(WebSearchConfig.PROVIDER_TAVILY)
            return WebSearchConfig(
                defaults.provider,
                defaults.baseUrl,
                apiKey,
                defaults.model,
                defaults.queryParam,
                defaults.apiKeyHeader,
                defaults.apiKeyParam
            )
        }

        private fun brave(apiKey: String): WebSearchConfig {
            val defaults = WebSearchConfig.defaultConfig(WebSearchConfig.PROVIDER_BRAVE)
            return WebSearchConfig(
                defaults.provider,
                defaults.baseUrl,
                apiKey,
                defaults.model,
                defaults.queryParam,
                defaults.apiKeyHeader,
                defaults.apiKeyParam
            )
        }
    }
}
