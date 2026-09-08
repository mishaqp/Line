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
import cn.lineai.ui.model.LinecodeExtensionsRepository
import cn.lineai.ui.model.LinecodeExtensionsUiAction
import cn.lineai.ui.model.LinecodeExtensionsUiEffect
import cn.lineai.ui.model.LinecodeExtensionsViewModel

class LinecodeExtensionsHostView(
    context: Context,
    repository: LinecodeExtensionsRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun openDocumentPicker(
            onPicked: (String, String) -> Unit,
            onCancelled: () -> Unit
        ): Boolean
        fun showPathRequired()
        fun showInvalidFile()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val linecodeExtensions = ViewModelProvider(
        hostViewModelStoreOwner,
        LinecodeExtensionsViewModel.factory(repository)
    )["linecode-extensions", LinecodeExtensionsViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(linecodeExtensions) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        LinecodeExtensionsScreenContent(
                            state = linecodeExtensions.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = linecodeExtensions.onAction(action)) {
                                    LinecodeExtensionsUiEffect.Back -> listener.onBack()
                                    LinecodeExtensionsUiEffect.OpenDocumentPicker -> {
                                        val opened = listener.openDocumentPicker(
                                            onPicked = { uri, displayName ->
                                                linecodeExtensions.onAction(
                                                    LinecodeExtensionsUiAction.DocumentPicked(
                                                        uri,
                                                        displayName
                                                    )
                                                )
                                            },
                                            onCancelled = {
                                                linecodeExtensions.onAction(
                                                    LinecodeExtensionsUiAction.DocumentPickCancelled
                                                )
                                            }
                                        )
                                        if (!opened) {
                                            linecodeExtensions.onAction(
                                                LinecodeExtensionsUiAction.OpenPathInstallFallback
                                            )
                                        }
                                    }
                                    LinecodeExtensionsUiEffect.PathRequired ->
                                        listener.showPathRequired()
                                    LinecodeExtensionsUiEffect.InvalidFile ->
                                        listener.showInvalidFile()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (attachedOnce) {
            linecodeExtensions.onAction(LinecodeExtensionsUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
