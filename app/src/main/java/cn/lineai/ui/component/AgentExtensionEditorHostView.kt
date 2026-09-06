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
import cn.lineai.ui.model.AgentExtensionEditorRepository
import cn.lineai.ui.model.AgentExtensionEditorUiEffect
import cn.lineai.ui.model.AgentExtensionEditorViewModel

class AgentExtensionEditorHostView(
    context: Context,
    destination: LineDestination.AgentEdit,
    repository: AgentExtensionEditorRepository,
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
        AgentExtensionEditorViewModel.factory(repository)
    )["agent-extension-editor:" + destination.screenId, AgentExtensionEditorViewModel::class.java]

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
                AgentExtensionEditorScreen(
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

    private fun handleEffect(effect: AgentExtensionEditorUiEffect) {
        if (disposed) return
        when (effect) {
            AgentExtensionEditorUiEffect.Back -> listener.onBack()
            AgentExtensionEditorUiEffect.GenerateRequiresDescription ->
                toast(R.string.screen_agent_require_description)
            AgentExtensionEditorUiEffect.DraftGenerated ->
                toast(R.string.screen_agent_ai_filled)
            AgentExtensionEditorUiEffect.DraftMissing -> Toast.makeText(
                context,
                context.getString(R.string.toast_ai_generate_failed_no_result),
                Toast.LENGTH_LONG
            ).show()
            is AgentExtensionEditorUiEffect.GenerateFailed -> Toast.makeText(
                context,
                effect.message.ifBlank { context.getString(R.string.toast_ai_generate_failed) },
                Toast.LENGTH_LONG
            ).show()
            AgentExtensionEditorUiEffect.SaveRequiresFields ->
                toast(R.string.screen_agent_save_require)
            is AgentExtensionEditorUiEffect.SaveFailed -> Toast.makeText(
                context,
                effect.message.ifBlank { context.getString(R.string.screen_agent_save_require) },
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
