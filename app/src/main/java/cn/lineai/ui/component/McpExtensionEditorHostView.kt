package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.R
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.McpExtensionEditorRepository
import cn.lineai.ui.model.McpExtensionEditorUiEffect
import cn.lineai.ui.model.McpExtensionEditorViewModel

class McpExtensionEditorHostView(
    context: Context,
    destination: LineDestination.McpEdit,
    repository: McpExtensionEditorRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val editor = ViewModelProvider(
        hostViewModelStoreOwner,
        McpExtensionEditorViewModel.factory(repository)
    )["mcp-extension-editor:${destination.screenId}", McpExtensionEditorViewModel::class.java]

    private var disposed = false
    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AccountScreenTheme {
                DisposableEffect(editor) {
                    onDispose {
                        hostViewModelStore.clear()
                    }
                }
                LaunchedEffect(editor) {
                    editor.effects.collect { effect -> handleEffect(effect) }
                }
                McpExtensionEditorScreen(
                    state = editor.state.collectAsStateWithLifecycle().value,
                    onAction = { action -> editor.onAction(action)?.let(::handleEffect) }
                )
            }
        }
    }

    init {
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun disposeEditor() {
        if (disposed) return
        disposed = true
        composeView.disposeComposition()
        hostViewModelStore.clear()
    }

    private fun handleEffect(effect: McpExtensionEditorUiEffect) {
        if (disposed) return
        when (effect) {
            McpExtensionEditorUiEffect.Back -> listener.onBack()
            McpExtensionEditorUiEffect.UrlInvalid -> toast(R.string.screen_mcp_url_invalid)
            McpExtensionEditorUiEffect.SaveRequiresNameAndUrl -> toast(R.string.screen_mcp_save_require_name_url)
            McpExtensionEditorUiEffect.SaveRequiresCurrentTools -> toast(R.string.screen_mcp_save_require_query)
            is McpExtensionEditorUiEffect.QueryCompleted -> Toast.makeText(
                context,
                context.getString(R.string.screen_mcp_query_done_toast, effect.count),
                Toast.LENGTH_SHORT
            ).show()
            is McpExtensionEditorUiEffect.QueryFailed -> Toast.makeText(
                context,
                effect.message.ifBlank { context.getString(R.string.toast_query_failed) },
                Toast.LENGTH_LONG
            ).show()
            is McpExtensionEditorUiEffect.SaveFailed -> Toast.makeText(
                context,
                effect.message.ifBlank { context.getString(R.string.screen_mcp_save_require_name_url) },
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
