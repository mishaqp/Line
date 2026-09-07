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
import cn.lineai.ui.model.ModelEditorRepository
import cn.lineai.ui.model.ModelEditorUiEffect
import cn.lineai.ui.model.ModelEditorViewModel
import cn.lineai.ui.util.KeyboardController

class ModelEditorHostView(
    context: Context,
    destination: LineDestination,
    repository: ModelEditorRepository,
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
        ModelEditorViewModel.factory(repository)
    )["model-editor:" + destination.screenId, ModelEditorViewModel::class.java]

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
                val uiState = editor.state.collectAsStateWithLifecycle().value
                ModelEditorScreen(
                    state = uiState,
                    saveEnabled = editor.canSave(uiState),
                    canQueryMain = editor.canQueryMain(uiState),
                    canQueryCompression = editor.canQueryCompression(uiState),
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

    override fun onDetachedFromWindow() {
        KeyboardController.clearFocusAndHide(this)
        super.onDetachedFromWindow()
    }

    private fun handleEffect(effect: ModelEditorUiEffect) {
        if (disposed) return
        when (effect) {
            ModelEditorUiEffect.Back -> listener.onBack()
            ModelEditorUiEffect.OpenLocalFormHint -> toast(R.string.screen_model_add_open_local_form)
            ModelEditorUiEffect.OpenCustomFormHint -> toast(R.string.screen_model_add_open_custom_form)
            ModelEditorUiEffect.LocalPickerPending -> toast(R.string.screen_model_add_local_picker_pending)
            ModelEditorUiEffect.RequireNameId -> toast(R.string.screen_model_add_require_name_id)
            ModelEditorUiEffect.RequireApiKey -> toast(R.string.screen_model_add_require_api_key)
            ModelEditorUiEffect.ToolCallRangeInvalid ->
                toast(R.string.screen_model_add_tool_call_range_invalid)
            ModelEditorUiEffect.RequireCompactionId ->
                toast(R.string.screen_model_add_require_compaction_id)
            ModelEditorUiEffect.TestMissing -> toast(R.string.screen_model_add_test_missing)
            ModelEditorUiEffect.FetchFailed ->
                Toast.makeText(context, R.string.screen_model_add_fetch_failed, Toast.LENGTH_LONG).show()
            is ModelEditorUiEffect.QueryFailed -> Toast.makeText(
                context,
                effect.message.ifBlank { context.getString(R.string.toast_query_failed) },
                Toast.LENGTH_LONG
            ).show()
            is ModelEditorUiEffect.SaveFailed -> Toast.makeText(
                context,
                effect.message.ifBlank { context.getString(R.string.toast_query_failed) },
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
    }
}
