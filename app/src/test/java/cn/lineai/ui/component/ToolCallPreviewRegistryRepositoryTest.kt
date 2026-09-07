package cn.lineai.ui.component

import android.content.Context
import cn.lineai.tool.ToolCallCardView
import cn.lineai.tool.ToolDisplayCategory
import cn.lineai.tool.ui.ToolCallViewFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallPreviewRegistryRepositoryTest {

    @Test
    fun snapshotKeepsFactoryOrderAndDuplicateCategories() {
        val source = RecordingSource(
            listOf(
                RecordingFactory(ToolDisplayCategory.WRITE),
                RecordingFactory(ToolDisplayCategory.SHELL),
                RecordingFactory(ToolDisplayCategory.WRITE)
            )
        )
        val repository = ToolCallPreviewRegistryRepository(source)

        val snapshot = repository.snapshot()

        assertEquals(1, source.listReads)
        assertEquals(
            listOf("WRITE", "SHELL", "SHELL", "WRITE"),
            snapshot.rows.map { it.categoryLabel }
        )
        assertEquals(
            listOf("factory-0-main", "factory-1-main", "factory-1-running", "factory-2-main"),
            snapshot.rows.map { it.renderId }
        )
        assertEquals(4, snapshot.rows.map { it.renderId }.toSet().size)
        assertTrue(snapshot.rows[2].running)
        assertFalse(snapshot.rows[1].running)
        assertTrue(snapshot.registryAvailable)
        assertEquals(0, source.factories!![0].createCalls)
        assertEquals(0, source.factories!![1].createCalls)
    }

    @Test
    fun runningRowIsAddedOnlyForShellFactories() {
        val repository = ToolCallPreviewRegistryRepository(
            RecordingSource(
                listOf(
                    RecordingFactory(ToolDisplayCategory.READ),
                    RecordingFactory(ToolDisplayCategory.SHELL),
                    RecordingFactory(ToolDisplayCategory.AGENT)
                )
            )
        )
        val snapshot = repository.snapshot()
        val running = snapshot.rows.filter { it.running }
        assertEquals(1, running.size)
        assertEquals("SHELL", running.single().categoryLabel)
        assertFalse(snapshot.rows.any { it.categoryLabel == "READ" && it.running })
        assertFalse(snapshot.rows.any { it.categoryLabel == "AGENT" && it.running })
    }

    @Test
    fun nullRegistryGivesEmptySnapshot() {
        val repository = ToolCallPreviewRegistryRepository(RecordingSource(null))
        val snapshot = repository.snapshot()
        assertTrue(snapshot.rows.isEmpty())
        assertFalse(snapshot.registryAvailable)
    }

    @Test
    fun uniqueRenderIdsSurviveRepeatedCategory() {
        val snapshot = ToolCallPreviewRegistryRepository(
            RecordingSource(
                listOf(
                    RecordingFactory(ToolDisplayCategory.GENERIC),
                    RecordingFactory(ToolDisplayCategory.GENERIC)
                )
            )
        ).snapshot()
        assertEquals(2, snapshot.rows.size)
        assertNotEquals(snapshot.rows[0].renderId, snapshot.rows[1].renderId)
        assertEquals(listOf("GENERIC", "GENERIC"), snapshot.rows.map { it.categoryLabel })
    }

    @Test
    fun previewDiffMatchesLegacyFake() {
        val diff = ToolCallPreviewRegistryRepository.previewDiff("ignored")
        assertEquals(ToolCallPreviewRegistryRepository.PREVIEW_DIFF_ID, diff.id)
        assertEquals(ToolCallPreviewRegistryRepository.PREVIEW_DIFF_PATH, diff.filePath)
        assertEquals(ToolCallPreviewRegistryRepository.PREVIEW_DIFF_OLD, diff.oldContent)
        assertEquals(ToolCallPreviewRegistryRepository.PREVIEW_DIFF_NEW, diff.newContent)
        assertFalse(diff.isReverted)
    }

    @Test
    fun snapshotDoesNotCreateCardsOrTouchExecutor() {
        val factory = RecordingFactory(ToolDisplayCategory.WRITE)
        val repository = ToolCallPreviewRegistryRepository(RecordingSource(listOf(factory)))
        val text = repository.snapshot().toString()
        assertEquals(0, factory.createCalls)
        assertFalse(text.contains("super-secret-token-xyz"))
        assertFalse(text.contains("ToolCallCardView"))
    }

    private class RecordingSource(
        var factories: List<RecordingFactory>?
    ) : ToolCallPreviewRegistrySource {
        var listReads = 0

        override fun factories(): List<ToolCallViewFactory>? {
            listReads += 1
            return factories
        }
    }

    private class RecordingFactory(
        private val displayCategory: ToolDisplayCategory
    ) : ToolCallViewFactory {
        var createCalls = 0

        override fun category(): ToolDisplayCategory = displayCategory

        override fun createView(context: Context): ToolCallCardView? {
            createCalls += 1
            return null
        }
    }
}
