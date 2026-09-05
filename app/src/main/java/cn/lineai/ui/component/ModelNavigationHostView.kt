package cn.lineai.ui.component

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProtocolType
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.AccountModelProviders

/**
 * Navigation 3 owner for the complete model-management flow.
 *
 * Model list, provider chooser and non-account editors remain Java Views during
 * this migration step, but all navigation and back behavior is owned by one
 * typed back stack.
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
        fun createLegacyEditor(
            context: Context,
            destination: LineDestination,
            onBack: Runnable
        ): View
    }

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val backStack = remember { mutableStateListOf(startDestination) }

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

                        @Suppress("DEPRECATION")
                        fun legacyEditor(destination: LineDestination): @androidx.compose.runtime.Composable () -> Unit = {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { viewContext ->
                                    listener.createLegacyEditor(
                                        viewContext,
                                        destination,
                                        Runnable { navigateBack() }
                                    )
                                }
                            )
                        }

                        NavDisplay(
                            backStack = backStack,
                            onBack = ::navigateBack,
                            entryProvider = { destination ->
                                when (destination) {
                                    LineDestination.Models -> NavEntry(destination) {
                                        AndroidView(
                                            modifier = Modifier.fillMaxSize(),
                                            factory = { viewContext ->
                                                ModelListScreenView(
                                                    viewContext,
                                                    models,
                                                    selectedModelId,
                                                    object : ModelListScreenView.Listener {
                                                        override fun onBack() = navigateBack()

                                                        override fun onAddModel() {
                                                            backStack.add(LineDestination.ModelAddOptions)
                                                        }

                                                        override fun onSelectModel(id: String) {
                                                            listener.onSelectModel(id)
                                                        }

                                                        override fun onEditModel(id: String) {
                                                            openEditor(id)
                                                        }

                                                        override fun onDeleteModels(ids: List<String>) {
                                                            listener.onDeleteModels(ids)
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    LineDestination.ModelAddOptions -> NavEntry(destination) {
                                        AndroidView(
                                            modifier = Modifier.fillMaxSize(),
                                            factory = { viewContext ->
                                                ModelAddOptionsScreenView(
                                                    viewContext,
                                                    object : ModelAddOptionsScreenView.Listener {
                                                        override fun onBack() = navigateBack()

                                                        override fun onCustom() {
                                                            backStack.add(LineDestination.ModelAdd)
                                                        }

                                                        override fun onLocal() {
                                                            backStack.add(LineDestination.ModelAddLocal)
                                                        }

                                                        override fun onProvider(id: String) {
                                                            backStack.add(
                                                                LineDestination.ModelAddPreset(id)
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        )
                                    }

                                    LineDestination.ModelAdd,
                                    LineDestination.ModelAddLocal -> NavEntry(
                                        destination,
                                        content = legacyEditor(destination)
                                    )

                                    is LineDestination.ModelAddPreset -> NavEntry(destination) {
                                        val provider = providerFor(destination.providerId)
                                        if (provider == null) {
                                            legacyEditor(destination).invoke()
                                        } else {
                                            AndroidView(
                                                modifier = Modifier.fillMaxSize(),
                                                factory = { viewContext ->
                                                    AccountNavigationHostView(
                                                        viewContext,
                                                        provider,
                                                        null,
                                                        destination,
                                                        accountListener(::navigateBack)
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    is LineDestination.ModelEdit -> NavEntry(destination) {
                                        val model = listener.getModel(destination.modelId)
                                        val provider =
                                            AccountModelProviders.fromProtocol(model?.protocolType)
                                        if (model == null || provider == null) {
                                            legacyEditor(destination).invoke()
                                        } else {
                                            AndroidView(
                                                modifier = Modifier.fillMaxSize(),
                                                factory = { viewContext ->
                                                    AccountNavigationHostView(
                                                        viewContext,
                                                        provider,
                                                        model,
                                                        destination,
                                                        accountListener(::navigateBack)
                                                    )
                                                }
                                            )
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
}
