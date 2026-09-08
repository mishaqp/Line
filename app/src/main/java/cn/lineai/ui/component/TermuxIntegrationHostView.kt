package cn.lineai.ui.component

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.lineai.R
import cn.lineai.ssh.TermuxHelper
import cn.lineai.ui.model.TermuxIntegrationRepository
import cn.lineai.ui.model.TermuxIntegrationUiAction
import cn.lineai.ui.model.TermuxIntegrationUiEffect
import cn.lineai.ui.model.TermuxIntegrationViewModel

class TermuxIntegrationHostView(
    context: Context,
    repository: TermuxIntegrationRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()

        @Throws(Exception::class)
        fun onOpenTermux()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val termuxIntegration = ViewModelProvider(
        hostViewModelStoreOwner,
        TermuxIntegrationViewModel.factory(repository)
    )["termux-integration", TermuxIntegrationViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(termuxIntegration) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        TermuxIntegrationScreenContent(
                            state = termuxIntegration.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = termuxIntegration.onAction(action)) {
                                    TermuxIntegrationUiEffect.Back -> listener.onBack()
                                    is TermuxIntegrationUiEffect.CopyToClipboard ->
                                        copyGrantCommand(effect.command)
                                    TermuxIntegrationUiEffect.RequestRunCommandPermission ->
                                        requestRunCommandPermission()
                                    TermuxIntegrationUiEffect.OpenTermux -> openTermux()
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
            termuxIntegration.onAction(TermuxIntegrationUiAction.Reload)
            return
        }
        attachedOnce = true
    }

    private fun copyGrantCommand(command: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.screen_termux_clip_label),
                command
            )
        )
    }

    private fun requestRunCommandPermission() {
        val host = context
        if (host !is Activity) {
            termuxIntegration.onAction(TermuxIntegrationUiAction.PermissionUnavailable)
            return
        }
        host.requestPermissions(
            arrayOf(TermuxHelper.TERMUX_RUN_COMMAND_PERMISSION),
            REQUEST_TERMUX_RUN_COMMAND
        )
        termuxIntegration.onAction(TermuxIntegrationUiAction.PermissionRequested)
    }

    private fun openTermux() {
        try {
            listener.onOpenTermux()
            termuxIntegration.onAction(TermuxIntegrationUiAction.TermuxOpened)
        } catch (error: Exception) {
            termuxIntegration.onAction(TermuxIntegrationUiAction.TermuxOpenFailed(error.message))
        }
    }

    companion object {
        const val REQUEST_TERMUX_RUN_COMMAND: Int = 7104
    }
}
