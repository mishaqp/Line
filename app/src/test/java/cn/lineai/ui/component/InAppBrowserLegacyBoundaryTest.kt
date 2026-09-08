package cn.lineai.ui.component

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppBrowserLegacyBoundaryTest {

    @Test
    fun screenViewKeepsLegacyConstructor() {
        val constructor = InAppBrowserScreenView::class.java.getConstructor(
            Context::class.java,
            String::class.java,
            java.lang.Boolean.TYPE,
            InAppBrowserScreenView.Listener::class.java
        )
        assertEquals(4, constructor.parameterTypes.size)
        assertEquals(
            "cn.lineai.ui.component.InAppBrowserHostView",
            InAppBrowserHostView::class.java.name
        )
        assertFalse(
            android.widget.LinearLayout::class.java.isAssignableFrom(
                InAppBrowserScreenView::class.java
            )
        )
    }

    @Test
    fun factoriesKeepExactAndPrefixIds() {
        assertEquals("browser", ScreenFactories.BrowserScreenFactory().screenId())
        assertEquals("browser:", ScreenFactories.BrowserPrefixScreenFactory().screenId())
        assertTrue(ScreenFactories.BrowserPrefixScreenFactory().matches("browser:https://example.com"))
        assertTrue(ScreenFactories.BrowserPrefixScreenFactory().matches("browser:"))
        assertFalse(ScreenFactories.BrowserPrefixScreenFactory().matches("browser"))
        assertFalse(ScreenFactories.BrowserPrefixScreenFactory().matches("toolcall_preview"))
        assertFalse(ScreenFactories.BrowserPrefixScreenFactory().matches("settings"))
        assertEquals(
            "cn.lineai.ui.component.ScreenFactories",
            ScreenFactories::class.java.name
        )
    }

    @Test
    fun typedBrowserRoundTripKeepsFullHttpsUrl() {
        val raw = "https://example.com/path/page?a=1&b=two#frag-3"
        val destination = LineDestinations.fromScreenId("browser:$raw")
        assertTrue(destination is LineDestination.Browser)
        assertEquals(raw, (destination as LineDestination.Browser).url)
        assertEquals("browser:$raw", destination.screenId)
        assertEquals(destination, LineDestinations.fromScreenId(destination.screenId))
        assertEquals(LineDestination.Chat, LineDestinations.parentOf(destination))
        assertEquals(
            LineDestination.Chat,
            LineDestinations.parentOf(LineDestinations.fromScreenId("browser:$raw"))
        )
    }

    @Test
    fun exactBrowserRouteStaysLegacyCompatible() {
        assertEquals("browser", ScreenFactories.BrowserScreenFactory().screenId())
        val exact = LineDestinations.fromScreenId("browser")
        assertFalse(exact is LineDestination.Browser)
        assertEquals(LineDestination.Chat, LineDestinations.parentOf(LineDestination.Browser("https://example.com")))
    }

    @Test
    fun backChainAndEmptyStackFallbackReturnToChat() {
        assertEquals(
            LineDestination.Chat,
            LineDestinations.parentOf(LineDestination.Browser("https://example.com"))
        )
        assertEquals(
            LineDestination.Chat,
            LineDestinations.parentOf(LineDestinations.fromScreenId("browser:https://example.com"))
        )
        assertFalse(
            LineDestinations.fromScreenId("browser:https://example.com") is LineDestination.ToolCallPreview
        )
    }

    @Test
    fun webViewClientKeepsBothOverrideOverloads() {
        val methods = InAppBrowserWebViewClient::class.java.declaredMethods.filter {
            it.name == "shouldOverrideUrlLoading"
        }
        assertTrue(
            methods.any { method ->
                method.parameterTypes.contentEquals(arrayOf(WebView::class.java, WebResourceRequest::class.java))
            }
        )
        assertTrue(
            methods.any { method ->
                method.parameterTypes.contentEquals(arrayOf(WebView::class.java, String::class.java))
            }
        )
    }

    @Test
    fun hardeningAndUnsupportedConstantsStayInProduction() {
        val hardening = InAppBrowserWebViews::class.java.getDeclaredMethod(
            "applyHardening",
            android.webkit.WebSettings::class.java
        )
        val javascript = InAppBrowserWebViews::class.java.getDeclaredMethod(
            "applyJavaScript",
            android.webkit.WebSettings::class.java,
            java.lang.Boolean.TYPE
        )
        assertEquals("applyHardening", hardening.name)
        assertEquals("applyJavaScript", javascript.name)
        assertEquals("Unsupported URL", InAppBrowserNavigationPolicy.UNSUPPORTED_TEXT)
        assertEquals("text/plain", InAppBrowserNavigationPolicy.UNSUPPORTED_MIME)
        assertEquals("utf-8", InAppBrowserNavigationPolicy.UNSUPPORTED_ENCODING)
    }
}
