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
import cn.lineai.ui.model.SkillsExtensionsRepository
import cn.lineai.ui.model.SkillsExtensionsUiAction
import cn.lineai.ui.model.SkillsExtensionsUiEffect
import cn.lineai.ui.model.SkillsExtensionsViewModel

class SkillsExtensionsHostView(
    context: Context,
    repository: SkillsExtensionsRepository,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun openDocumentPicker(
            onPicked: (String, String) -> Unit,
            onCancelled: () -> Unit
        ): Boolean
        fun shareWorkspace()
        fun showInvalidFile()
        fun showInvalidGitHubUrl()
    }

    private val hostViewModelStore = ViewModelStore()
    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }
    private val skillsExtensions = ViewModelProvider(
        hostViewModelStoreOwner,
        SkillsExtensionsViewModel.factory(repository)
    )["skills-extensions", SkillsExtensionsViewModel::class.java]
    private var attachedOnce = false

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(skillsExtensions) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        SkillsExtensionsScreenContent(
                            state = skillsExtensions.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = skillsExtensions.onAction(action)) {
                                    SkillsExtensionsUiEffect.Back -> listener.onBack()
                                    SkillsExtensionsUiEffect.OpenDocumentPicker -> {
                                        val opened = listener.openDocumentPicker(
                                            onPicked = { uri, displayName ->
                                                skillsExtensions.onAction(
                                                    SkillsExtensionsUiAction.DocumentPicked(
                                                        uri,
                                                        displayName
                                                    )
                                                )
                                            },
                                            onCancelled = {
                                                skillsExtensions.onAction(
                                                    SkillsExtensionsUiAction.DocumentPickCancelled
                                                )
                                            }
                                        )
                                        if (!opened) {
                                            skillsExtensions.onAction(
                                                SkillsExtensionsUiAction.OpenPathInstallFallback
                                            )
                                        }
                                    }
                                    SkillsExtensionsUiEffect.ShareWorkspace ->
                                        listener.shareWorkspace()
                                    SkillsExtensionsUiEffect.InvalidFile ->
                                        listener.showInvalidFile()
                                    SkillsExtensionsUiEffect.InvalidGitHubUrl ->
                                        listener.showInvalidGitHubUrl()
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
            skillsExtensions.onAction(SkillsExtensionsUiAction.Reload)
            return
        }
        attachedOnce = true
    }
}
