package cn.lineai.ui.component

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.AccountModelProviders
import cn.lineai.ui.model.ModelManagementRepository
import cn.lineai.ui.model.ModelManagementUiAction
import cn.lineai.ui.model.ModelManagementViewModel
import cn.lineai.ui.theme.LineTheme

/**
 * Navigation 3 owner for the complete model-management flow.
 *
 * Model list and provider chooser are native Compose. Non-account editors
 * remain Java Views during this migration step, with navigation owned by
 * one typed back stack.
 *
 * Scenes are opaque and slide horizontally without any fade: crossfades let
 * the previous scene (e.g. the editor behind "Add model") bleed through,
 * while slides keep exactly one fully opaque scene visible at any moment.
 */
class ModelNavigationHostView(
    context: Context,
    private val models: List<ModelConfig>,
    private val selectedModelId: String,
    private val startDestination: LineDestination,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onExit()
        fun onSelectModel(id: String)
        fun onDeleteModels(ids: List<String>)
        fun onSave(model: ModelConfig)
        fun onTest(model: ModelConfig)
        fun getModel(id: String): ModelConfig?
        fun models(): List<ModelConfig>
        fun selectedModelId(): String
        fun createLegacyEditor(
            context: Context,
            destination: LineDestination,
            onBack: Runnable
        ): View
    }

    init {
        setBackgroundColor(LineTheme.BG)
        addView(
            ComposeView(context).apply {
                setBackgroundColor(LineTheme.BG)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val backStack = remember { mutableStateListOf(startDestination) }
                        val repository = remember {
                            object : ModelManagementRepository {
                                override fun models(): List<ModelConfig> {
                                    val live = listener.models()
                                    return if (live.isNullOrEmpty() && models.isNotEmpty()) models else live
                                }

                                override fun selectedModelId(): String {
                                    val live = listener.selectedModelId()
                                    return live.ifEmpty { selectedModelId }
                                }

                                override fun selectModel(id: String) = listener.onSelectModel(id)

                                override fun deleteModels(ids: List<String>) = listener.onDeleteModels(ids)
                            }
                        }
                        val management: ModelManagementViewModel = viewModel(
                            key = "model-management",
                            factory = ModelManagementViewModel.factory(repository)
                        )

                        fun navigateBack() {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            } else {
                                listener.onExit()
                            }
                        }

                        fun openEditor(modelId: String) {
                            if (listener.getModel(modelId) != null) {
                                backStack.add(LineDestination.ModelEdit(modelId))
                            }
                        }

                        fun handleAction(action: ModelManagementUiAction) {
                            if (action == ModelManagementUiAction.Back) {
                                navigateBack()
                                return
                            }
                            val destination = management.onAction(action) ?: return
                            if (destination is LineDestination.ModelEdit) {
                                openEditor(destination.modelId)
                            } else {
                                backStack.add(destination)
                            }
                        }

                        @Suppress("DEPRECATION")
                        fun legacyEditor(destination: LineDestination): @Composable () -> Unit = {
                            OpaqueScene {
                                AndroidView(
                                    modifier = opaqueFill(),
                                    factory = { viewContext ->
                                        val editorView = listener.createLegacyEditor(
                                            viewContext,
                                            destination,
                                            Runnable { navigateBack() }
                                        )
                                        editorView.setBackgroundColor(LineTheme.BG)
                                        editorView
                                    }
                                )
                            }
                        }

                        NavDisplay(
                            backStack = backStack,
                            onBack = ::navigateBack,
                            modifier = Modifier.fillMaxSize().clipToBounds(),
                            // Push: the new scene slides in from the right edge while the
                            // previous opaque scene slides out to the left. No alpha fade.
                            transitionSpec = {
                                slideInHorizontally(
                                    animationSpec = tween(SCREEN_TRANSITION_MS),
                                    initialOffsetX = { it }
                                ) togetherWith slideOutHorizontally(
                                    animationSpec = tween(SCREEN_TRANSITION_MS),
                                    targetOffsetX = { -it }
                                )
                            },
                            // Pop: the top scene (editor) slides out to the right while the
                            // underlying opaque scene slides in from the left.
                            popTransitionSpec = {
                                slideInHorizontally(
                                    animationSpec = tween(SCREEN_TRANSITION_MS),
                                    initialOffsetX = { -it }
                                ) togetherWith slideOutHorizontally(
                                    animationSpec = tween(SCREEN_TRANSITION_MS),
                                    targetOffsetX = { it }
                                )
                            },
                            entryProvider = { destination ->
                                when (destination) {
                                    LineDestination.Models -> NavEntry(destination) {
                                        OpaqueScene {
                                            LaunchedEffect(destination) {
                                                management.refresh()
                                            }
                                            ModelListScreenContent(
                                                state = management.state.collectAsStateWithLifecycle().value,
                                                onAction = ::handleAction
                                            )
                                        }
                                    }

                                    LineDestination.ModelAddOptions -> NavEntry(destination) {
                                        OpaqueScene {
                                            ModelAddOptionsScreenContent(
                                                state = management.state.collectAsStateWithLifecycle().value,
                                                onAction = ::handleAction
                                            )
                                        }
                                    }

                                    LineDestination.ModelAdd,
                                    LineDestination.ModelAddLocal -> NavEntry(destination) {
                                        legacyEditor(destination).invoke()
                                    }

                                    is LineDestination.ModelAddPreset -> NavEntry(destination) {
                                        val provider = providerFor(destination.providerId)
                                        if (provider == null) {
                                            legacyEditor(destination).invoke()
                                        } else {
                                            OpaqueScene {
                                                AndroidView(
                                                    modifier = opaqueFill(),
                                                    factory = { viewContext ->
                                                        val hostView = AccountNavigationHostView(
                                                            viewContext,
                                                            provider,
                                                            null,
                                                            destination,
                                                            accountListener(::navigateBack)
                                                        )
                                                        hostView.setBackgroundColor(LineTheme.BG)
                                                        hostView
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    is LineDestination.ModelEdit -> NavEntry(destination) {
                                        val model = listener.getModel(destination.modelId)
                                        val provider =
                                            AccountModelProviders.fromProtocol(model?.protocolType)
                                        if (model == null || provider == null) {
                                            legacyEditor(destination).invoke()
                                        } else {
                                            OpaqueScene {
                                                AndroidView(
                                                    modifier = opaqueFill(),
                                                    factory = { viewContext ->
                                                        val hostView = AccountNavigationHostView(
                                                            viewContext,
                                                            provider,
                                                            model,
                                                            destination,
                                                            accountListener(::navigateBack)
                                                        )
                                                        hostView.setBackgroundColor(LineTheme.BG)
                                                        hostView
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    else -> NavEntry(destination) {}
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    private fun accountListener(onExit: () -> Unit) =
        object : AccountNavigationHostView.Listener {
            override fun onExit() = onExit()
            override fun onSave(model: ModelConfig) = listener.onSave(model)
            override fun onTest(model: ModelConfig) = listener.onTest(model)
        }

    private fun providerFor(id: String) = when (id.lowercase()) {
        "codex" -> AccountModelProviders.fromProtocol(ModelProtocolType.CODEX_RESPONSES)
        "grok" -> AccountModelProviders.fromProtocol(ModelProtocolType.GROK_RESPONSES)
        else -> null
    }

    private companion object {
        const val SCREEN_TRANSITION_MS = 280
    }
}

@Composable
private fun OpaqueScene(content: @Composable () -> Unit) {
    Box(modifier = opaqueFill(), content = { content() })
}

private fun opaqueFill(): Modifier =
    Modifier.fillMaxSize().background(Color(LineTheme.BG)).clipToBounds()
