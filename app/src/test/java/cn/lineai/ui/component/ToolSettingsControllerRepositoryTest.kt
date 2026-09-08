package cn.lineai.ui.component

import cn.lineai.model.WebSearchConfig
import cn.lineai.ui.model.ToolSettingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ToolSettingsControllerRepositoryTest {

    @Test
    fun snapshotReadsFreshGatewayValuesEveryTime() {
        val first = WebSearchConfig.defaultConfig(WebSearchConfig.PROVIDER_TAVILY)
        val second = WebSearchConfig.defaultConfig(WebSearchConfig.PROVIDER_BRAVE)
        val gateway = RecordingGateway("Vision", "Draw", first)
        val repository = ToolSettingsControllerRepository(gateway)

        val one = repository.snapshot()
        gateway.understanding = "Vision · gpt-4o"
        gateway.generation = "Draw · flux"
        gateway.config = second
        val two = repository.snapshot()

        assertEquals(2, gateway.snapshotCalls)
        assertSnapshot("Vision", "Draw", first, one)
        assertSnapshot("Vision · gpt-4o", "Draw · flux", second, two)
    }

    @Test
    fun nullAndBlankLabelsBecomeEmptyAndNullConfigUsesDefault() {
        val gateway = RecordingGateway("  ", "", WebSearchConfig.defaultConfig())
        val snapshot = ToolSettingsControllerRepository(gateway).snapshot()

        assertEquals("", snapshot.imageUnderstandingLabel)
        assertEquals("", snapshot.imageGenerationLabel)
        assertEquals(WebSearchConfig.PROVIDER_BING_RSS_FREE, snapshot.webSearch.provider)
        assertEquals("https://www.bing.com/search?format=rss", snapshot.webSearch.baseUrl)
    }

    @Test
    fun saveDelegatesExactConfig() {
        val gateway = RecordingGateway("", "", WebSearchConfig.defaultConfig())
        val repository = ToolSettingsControllerRepository(gateway)
        val next = WebSearchConfig.defaultConfig(WebSearchConfig.PROVIDER_SERPAPI)

        repository.saveWebSearchConfig(next)

        assertEquals(1, gateway.saveCalls)
        assertSame(next, gateway.lastSaved)
    }

    private fun assertSnapshot(
        understanding: String,
        generation: String,
        config: WebSearchConfig,
        snapshot: ToolSettingsSnapshot
    ) {
        assertEquals(understanding, snapshot.imageUnderstandingLabel)
        assertEquals(generation, snapshot.imageGenerationLabel)
        assertEquals(config.provider, snapshot.webSearch.provider)
        assertEquals(config.baseUrl, snapshot.webSearch.baseUrl)
        assertEquals(config.apiKey, snapshot.webSearch.apiKey)
    }

    private class RecordingGateway(
        var understanding: String?,
        var generation: String?,
        var config: WebSearchConfig
    ) : ToolSettingsLegacyGateway {
        var snapshotCalls = 0
        var saveCalls = 0
        var lastSaved: WebSearchConfig? = null

        override fun imageUnderstandingLabel(): String {
            snapshotCalls += 1
            return understanding ?: ""
        }

        override fun imageGenerationLabel(): String = generation ?: ""

        override fun webSearchConfig(): WebSearchConfig = config

        override fun saveWebSearchConfig(config: WebSearchConfig) {
            saveCalls += 1
            lastSaved = config
            this.config = config
        }
    }
}
