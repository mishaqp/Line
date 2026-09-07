package cn.lineai.ui.component

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

/**
 * Navigation 3 owner for the complete model-management flow.
 *
 * Model list and provider chooser are native Compose. Non-account editors
 * remain Java Views during this migration step, with navigation owned by
 * one typed back stack.
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
        addView(
            ComposeView(context).apply {
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
                                        LaunchedEffect(destination) {
                                            management.refresh()
                                        }
                                        ModelListScreenContent(
                                            state = management.state.collectAsStateWithLifecycle().value,
                                            onAction = ::handleAction
                                        )
                                    }

                                    LineDestination.ModelAddOptions -> NavEntry(destination) {
                                        ModelAddOptionsScreenContent(
                                            state = management.state.collectAsStateWithLifecycle().value,
                                            onAction = ::handleAction
                                        )
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
