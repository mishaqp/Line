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
    }

    @Volatile
    private var storageViewModel: StorageManagementViewModel? = null

    @Volatile
    private var pendingRefresh: Boolean = false

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
                            if (pendingRefresh) {
                                pendingRefresh = false
                                storage.refresh()
                            }
                            onDispose {
                                if (storageViewModel === storage) {
                                    storageViewModel = null
                                }
                            }
                        }

                        StorageManagementScreenContent(
                            state = storage.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (storage.onAction(action)) {
                                    StorageUiEffect.Back -> listener.onBack()
                                    null -> Unit
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun refresh() {
        val current = storageViewModel
        if (current != null) {
            current.refresh()
        } else {
            pendingRefresh = true
        }
    }
}
