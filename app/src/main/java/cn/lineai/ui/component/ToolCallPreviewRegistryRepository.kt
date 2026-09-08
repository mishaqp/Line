package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.model.DiffUiModel
import cn.lineai.tool.ToolDisplayCategory
import cn.lineai.tool.ui.DiffLoader
import cn.lineai.tool.ui.ToolCallViewFactory
import cn.lineai.tool.ui.ToolCallViewFactoryRegistry
import cn.lineai.tool.ui.ToolCallWriteView
import cn.lineai.ui.model.ToolCallPreviewRepository
import cn.lineai.ui.model.ToolCallPreviewRowUi
import cn.lineai.ui.model.ToolCallPreviewSnapshot

interface ToolCallPreviewRegistrySource {
    fun factories(): List<ToolCallViewFactory>?
}

class DefaultToolCallPreviewRegistrySource : ToolCallPreviewRegistrySource {
    override fun factories(): List<ToolCallViewFactory>? {
        return ToolCallViewFactoryRegistry.getDefault()?.allFactories
    }
}

interface ToolCallPreviewCardRenderer {
    fun createCard(context: Context, renderId: String): View?
}

class ToolCallPreviewRegistryRepository(
    private val source: ToolCallPreviewRegistrySource
) : ToolCallPreviewRepository, ToolCallPreviewCardRenderer {

    private var entries: List<RenderEntry> = emptyList()

    override fun snapshot(): ToolCallPreviewSnapshot {
        val factories = source.factories()
        if (factories == null) {
            entries = emptyList()
            return ToolCallPreviewSnapshot(rows = emptyList(), registryAvailable = false)
        }
        entries = buildEntries(factories)
        return ToolCallPreviewSnapshot(
            rows = entries.map { entry ->
                ToolCallPreviewRowUi(
                    renderId = entry.renderId,
                    categoryLabel = entry.category.name,
                    running = entry.running
                )
            },
            registryAvailable = true
        )
    }

    override fun createCard(context: Context, renderId: String): View? {
        val entry = entries.firstOrNull { it.renderId == renderId } ?: return null
        val sample = entry.sample ?: return null
        val card = entry.factory.createView(context) ?: return null
        if (card is ToolCallWriteView) {
            card.setDiffLoader(PREVIEW_DIFF_LOADER)
        }
        card.setProjectPath("")
        card.bind(sample.call, sample.result)
        return card as View
    }

    companion object {
        const val PREVIEW_DIFF_ID = "preview_diff"
        const val PREVIEW_DIFF_PATH = "app/src/main/java/cn/lineai/MainActivity.java"
        const val PREVIEW_DIFF_OLD = "old line\nold line 2"
        const val PREVIEW_DIFF_NEW = "new line\nnew line 2\nnew line 3"

        internal val PREVIEW_DIFF_LOADER = DiffLoader { diffId -> previewDiff(diffId) }

        internal fun previewDiff(diffId: String?): DiffUiModel {
            return DiffUiModel(
                PREVIEW_DIFF_ID,
                PREVIEW_DIFF_PATH,
                PREVIEW_DIFF_OLD,
                PREVIEW_DIFF_NEW,
                false
            )
        }

        internal fun buildEntries(factories: List<ToolCallViewFactory>): List<RenderEntry> {
            val result = ArrayList<RenderEntry>()
            factories.forEachIndexed { index, factory ->
                if (factory == null) {
                    return@forEachIndexed
                }
                val category = factory.category() ?: return@forEachIndexed
                val mainSample = ToolCallPreviewSamples.forCategory(category)
                if (mainSample != null) {
                    result += RenderEntry(
                        renderId = "factory-$index-main",
                        factory = factory,
                        category = category,
                        sample = mainSample,
                        running = false
                    )
                }
                if (category == ToolDisplayCategory.SHELL) {
                    result += RenderEntry(
                        renderId = "factory-$index-running",
                        factory = factory,
                        category = category,
                        sample = ToolCallPreviewSamples.runningShell(),
                        running = true
                    )
                }
            }
            return result
        }
    }

    internal data class RenderEntry(
        val renderId: String,
        val factory: ToolCallViewFactory,
        val category: ToolDisplayCategory,
        val sample: ToolCallPreviewSamples.Sample?,
        val running: Boolean
    )
}
