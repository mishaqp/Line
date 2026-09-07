package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.ui.model.DrawerTab
import cn.lineai.ui.model.DrawerUiAction
import cn.lineai.ui.model.DrawerUiEffect
import cn.lineai.ui.model.DrawerViewModel

class DrawerHostView(
    context: Context,
    repository: DrawerControllerRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onClosed()
        fun onNewConversation()
        fun onConversationSelected(id: String)
        fun onConversationDeleted(id: String)
        fun onCurrentProjectRemoveRequested()
        fun onFileNodeSelected(path: String, directory: Boolean)
        fun onFileNodeLongPressed(path: String, name: String, directory: Boolean, root: Boolean)
        fun onFileTreeActivated()
        fun onFileTreeRefresh()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val drawer = ViewModelProvider(
        hostViewModelStoreOwner,
        DrawerViewModel.factory(repository)
    )["drawer", DrawerViewModel::class.java]
    private var closePending = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(drawer) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        DrawerScreenContent(
                            state = drawer.state.collectAsStateWithLifecycle().value,
                            onAction = ::handleAction,
                            onFullyClosed = ::finishClose
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun reload() {
        handleAction(DrawerUiAction.Reload)
    }

    fun open() {
        closePending = false
        handleAction(DrawerUiAction.Open)
    }

    fun close() {
        if (!drawer.state.value.isOpen) return
        handleAction(DrawerUiAction.Close)
    }

    fun isFilesTabActive(): Boolean = drawer.state.value.activeTab == DrawerTab.FILES

    private fun handleAction(action: DrawerUiAction) {
        val wasOpen = drawer.state.value.isOpen
        val effect = drawer.onAction(action)
        if (wasOpen && !drawer.state.value.isOpen) {
            closePending = true
        }
        when (effect) {
            DrawerUiEffect.NewConversation -> listener.onNewConversation()
            is DrawerUiEffect.SelectConversation ->
                listener.onConversationSelected(effect.id)
            is DrawerUiEffect.DeleteConversation ->
                listener.onConversationDeleted(effect.id)
            DrawerUiEffect.FileTreeActivated -> listener.onFileTreeActivated()
            DrawerUiEffect.RefreshFiles -> listener.onFileTreeRefresh()
            is DrawerUiEffect.SelectFile ->
                listener.onFileNodeSelected(effect.path, effect.directory)
            is DrawerUiEffect.LongPressFile ->
                listener.onFileNodeLongPressed(
                    effect.path,
                    effect.name,
                    effect.directory,
                    effect.root
                )
            DrawerUiEffect.RemoveCurrentProject ->
                listener.onCurrentProjectRemoveRequested()
            null -> Unit
        }
    }

    private fun finishClose() {
        if (!closePending || drawer.state.value.isOpen) return
        closePending = false
        listener.onClosed()
    }
}
