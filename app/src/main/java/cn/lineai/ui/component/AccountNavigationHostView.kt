package cn.lineai.ui.component

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import cn.lineai.model.ModelConfig
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.AccountModelEditorViewModel
import cn.lineai.ui.model.AccountModelProvider
import cn.lineai.ui.model.AccountProviderKind
import cn.lineai.ui.model.AccountScreenViewModel

/**
 * First live Navigation 3 surface in Line.
 *
 * It owns only the account/model flow. Legacy screens continue to be rendered
 * by ScreenRegistry until each area is migrated and verified on device.
 */
class AccountNavigationHostView(
    context: Context,
    private val provider: AccountModelProvider,
    private val editingModel: ModelConfig?,
    private val startDestination: LineDestination,
    private val listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onExit()
        fun onSave(model: ModelConfig)
        fun onTest(model: ModelConfig)
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

                        NavDisplay(
                            backStack = backStack,
                            onBack = ::navigateBack,
                            entryProvider = { destination ->
                                when (destination) {
                                    LineDestination.CodexAccount,
                                    LineDestination.GrokAccount -> NavEntry(destination) {
                                        val account: AccountScreenViewModel = viewModel(
                                            key = "account-nav:${provider.kind}",
                                            factory = AccountScreenViewModel.factory(context, provider)
                                        )
                                        AccountScreenContent(
                                            state = account.state.collectAsStateWithLifecycle().value,
                                            onBack = ::navigateBack,
                                            onAddModel = {
                                                backStack.add(
                                                    LineDestination.ModelAddPreset(
                                                        providerId = providerId(provider.kind)
                                                    )
                                                )
                                            },
                                            onRefresh = account::refresh,
                                            onLogin = account::login,
                                            onLogout = account::logout
                                        )
                                    }

                                    is LineDestination.ModelAddPreset,
                                    is LineDestination.ModelEdit -> NavEntry(destination) {
                                        val editor: AccountModelEditorViewModel = viewModel(
                                            key = "account-model-nav:${provider.kind}:${editingModel?.id ?: "new"}",
                                            factory = AccountModelEditorViewModel.factory(
                                                context,
                                                provider,
                                                editingModel
                                            )
                                        )
                                        AccountModelEditorScreenContent(
                                            state = editor.state.collectAsStateWithLifecycle().value,
                                            editing = editingModel != null,
                                            provider = provider,
                                            onBack = ::navigateBack,
                                            onOpenAccount = {
                                                backStack.add(accountDestination(provider.kind))
                                            },
                                            onRefresh = editor::refreshModels,
                                            onSelectModel = editor::selectModel,
                                            onNameChanged = editor::setName,
                                            onAdvancedChanged = editor::setAdvancedExpanded,
                                            onUseCustomIdChanged = editor::setUseCustomModelId,
                                            onCustomIdChanged = editor::setCustomModelId,
                                            onToolLimitChanged = editor::setToolCallLimit,
                                            onContextSizeChanged = editor::setContextSize,
                                            onCompressionEnabledChanged = editor::setCompressionEnabled,
                                            onCompressionAutoChanged = editor::setCompressionAuto,
                                            onCompressionModelIdChanged = editor::setCompressionModelId,
                                            onSave = { editor.buildModel()?.let(listener::onSave) },
                                            onTest = { editor.buildModel()?.let(listener::onTest) }
                                        )
                                    }

                                    else -> NavEntry(destination) { navigateBack() }
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    private fun providerId(kind: AccountProviderKind): String = when (kind) {
        AccountProviderKind.CODEX -> "codex"
        AccountProviderKind.GROK -> "grok"
    }

    private fun accountDestination(kind: AccountProviderKind): LineDestination = when (kind) {
        AccountProviderKind.CODEX -> LineDestination.CodexAccount
        AccountProviderKind.GROK -> LineDestination.GrokAccount
    }
}
