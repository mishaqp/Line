package cn.lineai.ui.model

import cn.lineai.model.StorageStatsUiModel
import java.util.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageManagementViewModelTest {
    @Test
    fun initialStateIsLoadingBeforeAutomaticLoadRuns() {
        val dispatcher = ManualDispatcher()
        val repository = FakeRepository(listOf(snapshotA()))
        val viewModel = StorageManagementViewModel(repository, dispatcher)

        val state: StateFlow<StorageUiState> = viewModel.state
        assertTrue(state.value.isInitialLoading)
        assertFalse(state.value.isRefreshing)
        assertNull(state.value.stats)
        assertEquals(0, repository.loadCalls)
    }

    @Test
    fun automaticLoadStartsAndAppliesStats() {
        val dispatcher = ManualDispatcher()
        val expected = snapshotA()
        val repository = FakeRepository(listOf(expected))
        val viewModel = StorageManagementViewModel(repository, dispatcher)

        dispatcher.runAll()

        assertEquals(1, repository.loadCalls)
        assertSame(expected, viewModel.state.value.stats)
        assertFalse(viewModel.state.value.isInitialLoading)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun loadedSnapshotPreservesAllFiveFormattedSizes() {
        val dispatcher = ManualDispatcher()
        val repository = FakeRepository(listOf(snapshotA()))
        val viewModel = StorageManagementViewModel(repository, dispatcher)

        dispatcher.runAll()
        val stats = viewModel.state.value.stats!!

        assertEquals("14 KB", stats.formatTotalSize())
        assertEquals("2 KB", stats.formatDiffCacheSize())
        assertEquals("3 KB", stats.formatChatSize())
        assertEquals("4 KB", stats.formatConfigSize())
        assertEquals("5 KB", stats.formatHomeSize())
    }

    @Test
    fun loadedSnapshotPreservesAllFourCounts() {
        val dispatcher = ManualDispatcher()
        val repository = FakeRepository(listOf(snapshotA()))
        val viewModel = StorageManagementViewModel(repository, dispatcher)

        dispatcher.runAll()
        val stats = viewModel.state.value.stats!!

        assertEquals(11, stats.diffCacheCount)
        assertEquals(22, stats.chatCount)
        assertEquals(33, stats.configCount)
        assertEquals(44, stats.homeCount)
    }

    @Test
    fun refreshActionStartsAnotherLoadAndMarksRefreshDuringLoad() {
        val dispatcher = ManualDispatcher()
        val first = snapshotA()
        val second = snapshotB()
        val repository = FakeRepository(listOf(first, second))
        lateinit var viewModel: StorageManagementViewModel
        repository.onLoad = { call ->
            if (call == 2) {
                assertTrue(viewModel.state.value.isRefreshing)
                assertSame(first, viewModel.state.value.stats)
            }
        }
        viewModel = StorageManagementViewModel(repository, dispatcher)
        dispatcher.runAll()

        viewModel.onAction(StorageUiAction.Refresh)
        dispatcher.runAll()

        assertEquals(2, repository.loadCalls)
        assertSame(second, viewModel.state.value.stats)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun newSnapshotReplacesOldSnapshotWithoutMixingFields() {
        val dispatcher = ManualDispatcher()
        val first = snapshotA()
        val second = snapshotB()
        val repository = FakeRepository(listOf(first, second))
        val viewModel = StorageManagementViewModel(repository, dispatcher)
        dispatcher.runAll()

        viewModel.refresh()
        dispatcher.runAll()

        val stats = viewModel.state.value.stats!!
        assertSame(second, stats)
        assertEquals(second.totalSize, stats.totalSize)
        assertEquals(second.diffCacheSize, stats.diffCacheSize)
        assertEquals(second.chatSize, stats.chatSize)
        assertEquals(second.configSize, stats.configSize)
        assertEquals(second.homeSize, stats.homeSize)
        assertEquals(second.diffCacheCount, stats.diffCacheCount)
        assertEquals(second.chatCount, stats.chatCount)
        assertEquals(second.configCount, stats.configCount)
        assertEquals(second.homeCount, stats.homeCount)
    }

    @Test
    fun rapidRefreshRequestsAreConflatedAndNeverRunInParallel() {
        val dispatcher = ManualDispatcher()
        val repository = FakeRepository(listOf(snapshotA(), snapshotB(), snapshotC()))
        val viewModel = StorageManagementViewModel(repository, dispatcher)
        dispatcher.runAll()

        repeat(10) { viewModel.refresh() }
        dispatcher.runAll()

        assertTrue(repository.loadCalls in 2..3)
        assertEquals(1, repository.maxConcurrentLoads)
        assertFalse(viewModel.state.value.isInitialLoading)
        assertFalse(viewModel.state.value.isRefreshing)
        assertTrue(viewModel.state.value.stats != null)
    }

    @Test
    fun backDoesNotStartAnotherLoadOrExposeDestructiveAction() {
        val dispatcher = ManualDispatcher()
        val repository = FakeRepository(listOf(snapshotA()))
        val viewModel = StorageManagementViewModel(repository, dispatcher)
        dispatcher.runAll()
        val callsBeforeBack = repository.loadCalls

        val effect = viewModel.onAction(StorageUiAction.Back)
        dispatcher.runAll()

        assertSame(StorageUiEffect.Back, effect)
        assertEquals(callsBeforeBack, repository.loadCalls)
        val repositoryMethods = StorageManagementRepository::class.java.declaredMethods.map { it.name }
        assertEquals(listOf("loadStats"), repositoryMethods.sorted())
    }

    @Test
    fun publicRefreshUpdatesState() {
        val dispatcher = ManualDispatcher()
        val first = snapshotA()
        val second = snapshotB()
        val repository = FakeRepository(listOf(first, second))
        val viewModel = StorageManagementViewModel(repository, dispatcher)
        dispatcher.runAll()
        assertSame(first, viewModel.state.value.stats)

        viewModel.refresh()
        dispatcher.runAll()

        assertSame(second, viewModel.state.value.stats)
        assertEquals(2, repository.loadCalls)
    }

    private class ManualDispatcher : CoroutineDispatcher() {
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

    private class FakeRepository(
        private val snapshots: List<StorageStatsUiModel>
    ) : StorageManagementRepository {
        var loadCalls: Int = 0
        var maxConcurrentLoads: Int = 0
        var onLoad: ((Int) -> Unit)? = null
        private var activeLoads: Int = 0

        override fun loadStats(): StorageStatsUiModel {
            activeLoads++
            maxConcurrentLoads = maxOf(maxConcurrentLoads, activeLoads)
            val call = loadCalls + 1
            loadCalls = call
            return try {
                onLoad?.invoke(call)
                snapshots[minOf(call - 1, snapshots.lastIndex)]
            } finally {
                activeLoads--
            }
        }
    }

    private fun snapshotA(): StorageStatsUiModel = StorageStatsUiModel(
        14L * 1024,
        110,
        2L * 1024,
        11,
        3L * 1024,
        22,
        4L * 1024,
        33,
        5L * 1024,
        44
    )

    private fun snapshotB(): StorageStatsUiModel = StorageStatsUiModel(
        30L * 1024,
        210,
        6L * 1024,
        51,
        7L * 1024,
        52,
        8L * 1024,
        53,
        9L * 1024,
        54
    )

    private fun snapshotC(): StorageStatsUiModel = StorageStatsUiModel(
        46L * 1024,
        310,
        10L * 1024,
        61,
        11L * 1024,
        62,
        12L * 1024,
        63,
        13L * 1024,
        64
    )
}
