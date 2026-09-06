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
import cn.lineai.R
import cn.lineai.ui.model.PhoneControlSettingsRepository
import cn.lineai.ui.model.PhoneControlUiAction
import cn.lineai.ui.model.PhoneControlUiEffect
import cn.lineai.ui.model.PhoneControlViewModel

class PhoneControlHostView(
    context: Context,
    repository: PhoneControlSettingsRepository,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onOpenAccessibilitySettings()
    }

    private val hostViewModelStore = ViewModelStore()

    private val hostViewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = hostViewModelStore
    }

    private val phoneControl = ViewModelProvider(
        hostViewModelStoreOwner,
        PhoneControlViewModel.factory(repository)
    )["phone-control", PhoneControlViewModel::class.java]

    init {
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        DisposableEffect(phoneControl) {
                            onDispose {
                                hostViewModelStore.clear()
                            }
                        }
                        PhoneControlScreenContent(
                            state = phoneControl.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                handleEffect(phoneControl.onAction(action))
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun refresh() {
        phoneControl.onAction(PhoneControlUiAction.Reload)
    }

    private fun handleEffect(effect: PhoneControlUiEffect?) {
        when (effect) {
            null -> Unit
            PhoneControlUiEffect.Back -> listener.onBack()
            PhoneControlUiEffect.OpenAccessibilitySettings -> listener.onOpenAccessibilitySettings()
            PhoneControlUiEffect.ShowDisclaimer -> showDisclaimer()
        }
    }

    private fun showDisclaimer() {
        LegalDialog.show(
            context,
            context.getString(R.string.screen_phone_control_disclaimer_title),
            context.getString(R.string.screen_phone_control_disclaimer_text),
            context.getString(R.string.screen_phone_control_disclaimer_agree),
            context.getString(R.string.screen_phone_control_disclaimer_disagree),
            {
                handleEffect(phoneControl.onAction(PhoneControlUiAction.AcceptDisclaimer))
            },
            {
                phoneControl.onAction(PhoneControlUiAction.RejectDisclaimer)
            }
        )
    }
}
