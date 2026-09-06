package cn.lineai.ui.component

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.model.AboutAppInfo
import cn.lineai.ui.model.AboutRepository
import cn.lineai.ui.model.AboutUiEffect
import cn.lineai.ui.model.AboutViewModel

class AboutHostView(
    context: Context,
    listener: Listener
) : FrameLayout(context) {

    interface Listener {
        fun onBack()
        fun onOpenGithub()
        fun onOpen(destination: LineDestination)
    }

    init {
        val repository: AboutRepository = AndroidAboutRepository(context.applicationContext)
        addView(
            ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AccountScreenTheme {
                        val about: AboutViewModel = viewModel(
                            key = "about",
                            factory = AboutViewModel.factory(repository)
                        )
                        AboutScreenContent(
                            state = about.state.collectAsStateWithLifecycle().value,
                            onAction = { action ->
                                when (val effect = about.onAction(action)) {
                                    AboutUiEffect.Back -> listener.onBack()
                                    AboutUiEffect.OpenGithub -> listener.onOpenGithub()
                                    is AboutUiEffect.OpenDestination -> listener.onOpen(effect.destination)
                                }
                            }
                        )
                    }
                }
            },
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }
}

private class AndroidAboutRepository(
    private val context: Context
) : AboutRepository {
    @Suppress("DEPRECATION")
    override fun loadAppInfo(): AboutAppInfo {
        var appLabel = "LineCode Pro"
        var versionName: String? = null
        var versionCode = 0L
        try {
            val packageManager = context.packageManager
            val label = packageManager.getApplicationLabel(context.applicationInfo)
            if (label.isNotEmpty()) {
                appLabel = label.toString()
            }
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            versionName = packageInfo.versionName?.takeIf { it.isNotEmpty() }
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
        } catch (_: PackageManager.NameNotFoundException) {
        }
        return AboutAppInfo(
            appLabel = appLabel,
            versionName = versionName,
            versionCode = versionCode
        )
    }
}
