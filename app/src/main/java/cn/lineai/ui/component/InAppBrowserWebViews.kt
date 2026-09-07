package cn.lineai.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import cn.lineai.R
import cn.lineai.ui.model.InAppBrowserUiState
import cn.lineai.ui.theme.LineTheme

internal class InAppBrowserWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val nextUrl = if (request == null || request.url == null) "" else request.url.toString()
        return InAppBrowserNavigationPolicy.shouldBlock(nextUrl)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, nextUrl: String?): Boolean {
        return InAppBrowserNavigationPolicy.shouldBlock(nextUrl)
    }
}

internal object InAppBrowserWebViews {
    fun create(context: Context, state: InAppBrowserUiState): WebView {
        val webView = WebView(context)
        webView.setBackgroundColor(LineTheme.BG)
        webView.contentDescription = context.getString(R.string.in_app_browser_content_desc)
        applyHardening(webView.settings)
        applyJavaScript(webView.settings, state.javaScriptEnabled)
        webView.settings.domStorageEnabled = true
        webView.webViewClient = InAppBrowserWebViewClient()
        loadInitial(webView, state)
        return webView
    }

    fun applyHardening(settings: WebSettings) {
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun applyJavaScript(settings: WebSettings, enabled: Boolean) {
        settings.javaScriptEnabled = enabled
    }

    fun loadInitial(webView: WebView, state: InAppBrowserUiState) {
        if (state.supported && state.normalizedUrl.isNotEmpty()) {
            webView.loadUrl(state.normalizedUrl)
        } else {
            webView.loadDataWithBaseURL(
                null,
                InAppBrowserNavigationPolicy.UNSUPPORTED_TEXT,
                InAppBrowserNavigationPolicy.UNSUPPORTED_MIME,
                InAppBrowserNavigationPolicy.UNSUPPORTED_ENCODING,
                null
            )
        }
    }
}
