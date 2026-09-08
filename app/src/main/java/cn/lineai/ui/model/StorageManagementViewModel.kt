package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lineai.model.StorageStatsUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface StorageManagementRepository {
    fun loadStats(): StorageStatsUiModel
}

data class StorageUiState(
    val stats: StorageStatsUiModel? = null,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showClearDialog: Boolean = false,
    val clearDiffCacheSelected: Boolean = false,
    val clearChatHistorySelected: Boolean = false,
    val isClearing: Boolean = false
)

sealed interface StorageUiAction {
    data object Back : StorageUiAction
    data object Refresh : StorageUiAction
    data object OpenClearDialog : StorageUiAction
    data object DismissClearDialog : StorageUiAction
    data class SetClearDiffCache(val selected: Boolean) : StorageUiAction
    data class SetClearChatHistory(val selected: Boolean) : StorageUiAction
    data object ConfirmClear : StorageUiAction
    data object ClearCompleted : StorageUiAction
}

sealed interface StorageUiEffect {
    data object Back : StorageUiEffect
    data class ClearSelected(
        val clearDiffCache: Boolean,
        val clearChatHistory: Boolean
    ) : StorageUiEffect
}

class StorageManagementViewModel(
    private val repository: StorageManagementRepository,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow(StorageUiState())
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    private val workLock = Any()
    private var workerRunning = false
    private var refreshPending = false

    init {
        refresh()
    }

    fun onAction(action: StorageUiAction): StorageUiEffect? = when (action) {
        StorageUiAction.Back -> StorageUiEffect.Back
        StorageUiAction.Refresh -> {
            refresh()
            null
        }
        StorageUiAction.OpenClearDialog -> {
            if (!_state.value.isClearing) {
                _state.value = _state.value.copy(
                    showClearDialog = true,
                    clearDiffCacheSelected = false,
                    clearChatHistorySelected = false
                )
            }
            null
        }
        StorageUiAction.DismissClearDialog -> {
            _state.value = _state.value.copy(
                showClearDialog = false,
                clearDiffCacheSelected = false,
                clearChatHistorySelected = false
            )
            null
        }
        is StorageUiAction.SetClearDiffCache -> {
            _state.value = _state.value.copy(clearDiffCacheSelected = action.selected)
            null
        }
        is StorageUiAction.SetClearChatHistory -> {
            _state.value = _state.value.copy(clearChatHistorySelected = action.selected)
            null
        }
        StorageUiAction.ConfirmClear -> confirmClear()
        StorageUiAction.ClearCompleted -> {
            _state.value = _state.value.copy(
                showClearDialog = false,
                clearDiffCacheSelected = false,
                clearChatHistorySelected = false,
                isClearing = false
            )
            refresh()
            null
        }
    }

    fun refresh() {
        synchronized(workLock) {
            refreshPending = true
            if (workerRunning) {
                return
            }
            workerRunning = true
            viewModelScope.launch(loadDispatcher) {
                drainRefreshRequests()
            }
        }
    }

    private suspend fun drainRefreshRequests() {
        while (true) {
            currentCoroutineContext().ensureActive()
            val shouldLoad = synchronized(workLock) {
                if (refreshPending) {
                    refreshPending = false
                    true
                } else {
                    workerRunning = false
                    false
                }
            }
            if (!shouldLoad) {
                return
            }
            loadOnce()
        }
    }

    private suspend fun loadOnce() {
        val currentStats = _state.value.stats
        _state.value = _state.value.copy(
            isInitialLoading = currentStats == null,
            isRefreshing = currentStats != null
        )
        try {
            val loaded = repository.loadStats()
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                stats = loaded,
                isInitialLoading = false,
                isRefreshing = false
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            _state.value = _state.value.copy(
                isInitialLoading = false,
                isRefreshing = false
            )
        }
    }

    private fun confirmClear(): StorageUiEffect? {
        val current = _state.value
        if (current.isClearing || (!current.clearDiffCacheSelected && !current.clearChatHistorySelected)) {
            return null
        }
        _state.value = current.copy(
            showClearDialog = false,
            isClearing = true
        )
        return StorageUiEffect.ClearSelected(
            clearDiffCache = current.clearDiffCacheSelected,
            clearChatHistory = current.clearChatHistorySelected
        )
    }

    companion object {
        fun factory(
            repository: StorageManagementRepository,
            loadDispatcher: CoroutineDispatcher = Dispatchers.IO
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(StorageManagementViewModel::class.java)) {
                    return StorageManagementViewModel(repository, loadDispatcher) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}
