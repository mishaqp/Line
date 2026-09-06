package cn.lineai.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.lineai.model.StorageStatsUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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
    val isRefreshing: Boolean = false
)

sealed interface StorageUiAction {
    data object Back : StorageUiAction
    data object Refresh : StorageUiAction
}

sealed interface StorageUiEffect {
    data object Back : StorageUiEffect
}

class StorageManagementViewModel(
    private val repository: StorageManagementRepository,
    private val loadDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val _state = MutableStateFlow(StorageUiState())
    val state: StateFlow<StorageUiState> = _state.asStateFlow()

    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        viewModelScope.launch(loadDispatcher) {
            for (ignored in refreshRequests) {
                val currentStats = _state.value.stats
                _state.value = _state.value.copy(
                    isInitialLoading = currentStats == null,
                    isRefreshing = currentStats != null
                )
                try {
                    val loaded = repository.loadStats()
                    currentCoroutineContext().ensureActive()
                    _state.value = StorageUiState(
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
        }
        refresh()
    }

    fun onAction(action: StorageUiAction): StorageUiEffect? = when (action) {
        StorageUiAction.Back -> StorageUiEffect.Back
        StorageUiAction.Refresh -> {
            refresh()
            null
        }
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
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
