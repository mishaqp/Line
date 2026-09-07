package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.ui.model.SlashCommandPopupUiAction
import cn.lineai.ui.model.SlashCommandPopupUiEffect
import cn.lineai.ui.model.SlashCommandPopupViewModel
import cn.lineai.ui.model.SlashCommandRowData

class SlashCommandPopupHostView(
    context: Context,
    repository: SlashCommandPopupRepository,
    private val onRowSelected: (Int) -> Unit
) : FrameLayout(context), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner =
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore =
                hostViewModelStore
        }
    private val popup = ViewModelProvider(
        hostViewModelStoreOwner,
        SlashCommandPopupViewModel.factory(repository)
    )["slash-command-popup", SlashCommandPopupViewModel::class.java]

    init {
        lifecycleRegistry.currentState =
            Lifecycle.State.CREATED
        ViewTreeLifecycleOwner.set(this, this)
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy
                        .DisposeOnDetachedFromWindow
                )
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(popup) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        SlashCommandPopupContent(
                            state = popup.state
                                .collectAsStateWithLifecycle()
                                .value,
                            onSelect = { index ->
                                dispatch(
                                    SlashCommandPopupUiAction
                                        .Select(index)
                                )
                            }
                        )
                    }
                }
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState =
            Lifecycle.State.STARTED
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState =
            Lifecycle.State.CREATED
        super.onDetachedFromWindow()
    }

    fun bind(
        title: String?,
        rows: List<SlashCommandRowData>,
        selectedIndex: Int
    ) {
        popup.onAction(
            SlashCommandPopupUiAction.Bind(
                title,
                rows,
                selectedIndex
            )
        )
    }

    fun setSelectedIndex(index: Int) {
        popup.onAction(
            SlashCommandPopupUiAction.SetSelectedIndex(index)
        )
    }

    fun clear() {
        popup.onAction(SlashCommandPopupUiAction.Clear)
    }

    fun hasRows(): Boolean = popup.state.value.visible

    private fun dispatch(action: SlashCommandPopupUiAction) {
        when (val effect = popup.onAction(action)) {
            is SlashCommandPopupUiEffect.RowSelected ->
                onRowSelected(effect.index)
            null -> Unit
        }
    }
}
