package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.ui.model.StorageManagementRepository
import cn.lineai.ui.model.StorageManagementViewModel
import cn.lineai.ui.model.StorageUiEffect

class StorageManagementHostView(
    context: Context,
    repository: StorageManagementRepository,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onClearDiffCache()
        fun onClearChatHistory()
    }

    @Volatile
    private var storageViewModel: StorageManagementViewModel? = null

    @Volatile
    private var pendingRefresh: Boolean = false

    @Volatile
    private var suppressExternalRefresh: Boolean = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val storage: StorageManagementViewModel = viewModel(
                            key = "storage-management",
                            factory = StorageManagementViewModel.factory(repository)
                        )

                        DisposableEffect(storage) {
                            storageViewModel = storage
                            val shouldRefresh = pendingRefresh || storage.state.value.stats != null
                            pendingRefresh = false
                            if (shouldRefresh) {
                                storage.refresh()
                            }
                            onDispose {
                                if (storageViewModel === storage) {
                                    storageViewModel = null
                                }
                            }
                        }

                        fun handleEffect(effect: StorageUiEffect?) {
                            when (effect) {
                                null -> Unit
                                StorageUiEffect.Back -> listener.onBack()
                                is StorageUiEffect.ClearSelected -> {
                                    suppressExternalRefresh = true
                                    try {
                                        if (effect.clearDiffCache) {
                                            try {
                                                listener.onClearDiffCache()
                                            } catch (_: RuntimeException) {
                                            }
                                        }
                                        if (effect.clearChatHistory) {
                                            try {
                                                listener.onClearChatHistory()
                                            } catch (_: RuntimeException) {
                                            }
                                        }
                                    } finally {
                                        suppressExternalRefresh = false
                                        pendingRefresh = false
                                        storage.onAction(cn.lineai.ui.model.StorageUiAction.ClearCompleted)
                                    }
                                }
                            }
                        }

                        StorageManagementScreenContent(
                            state = storage.state.collectAsStateWithLifecycle().value,
                            onAction = { action -> handleEffect(storage.onAction(action)) }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun refresh() {
        if (suppressExternalRefresh) {
            pendingRefresh = true
            return
        }
        val current = storageViewModel
        if (current != null) {
            current.refresh()
        } else {
            pendingRefresh = true
        }
    }
}
