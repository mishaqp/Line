package cn.lineai.ui.component

import cn.lineai.security.UrlPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InAppBrowserUrlRepositoryTest {

    private var previousRelaxed = false

    @Before
    fun disableRelaxedHttp() {
        previousRelaxed = UrlPolicy.isRelaxedHttpEnabled()
        UrlPolicy.setRelaxedHttpEnabled(false)
    }

    @After
    fun restoreRelaxedHttp() {
        UrlPolicy.setRelaxedHttpEnabled(previousRelaxed)
    }

    @Test
    fun trimsAndAcceptsHttps() {
        val snapshot = InAppBrowserUrlRepository(" https://example.com/path ", true).snapshot()
        assertEquals("https://example.com/path", snapshot.normalizedUrl)
        assertTrue(snapshot.supported)
        assertTrue(snapshot.javaScriptEnabled)
        assertFalse(snapshot.useDefaultTitle)
        assertEquals(" https://example.com/path ", snapshot.rawUrl)
    }

    @Test
    fun allowsLocalAndPrivateCleartextHttp() {
        assertTrue(InAppBrowserUrlRepository("http://localhost:8080", false).snapshot().supported)
        assertTrue(InAppBrowserUrlRepository("http://127.0.0.1:8080", false).snapshot().supported)
        assertTrue(InAppBrowserUrlRepository("http://10.0.2.2:3000", false).snapshot().supported)
        assertTrue(InAppBrowserUrlRepository("http://192.168.1.10/", false).snapshot().supported)
        assertTrue(InAppBrowserUrlRepository("http://10.1.2.3/", false).snapshot().supported)
        assertTrue(InAppBrowserUrlRepository("http://172.16.0.4/", false).snapshot().supported)
    }

    @Test
    fun blocksRemoteCleartextWhenNotRelaxed() {
        val snapshot = InAppBrowserUrlRepository("http://example.com", false).snapshot()
        assertFalse(snapshot.supported)
        assertEquals("", snapshot.normalizedUrl)
    }

    @Test
    fun blocksForbiddenSchemesAndMalformed() {
        assertFalse(InAppBrowserUrlRepository("javascript:alert(1)", false).snapshot().supported)
        assertFalse(InAppBrowserUrlRepository("file:///tmp/index.html", false).snapshot().supported)
        assertFalse(InAppBrowserUrlRepository("content://media/external", false).snapshot().supported)
        assertFalse(InAppBrowserUrlRepository("intent://scan/#Intent;end", false).snapshot().supported)
        assertFalse(InAppBrowserUrlRepository("about:blank", false).snapshot().supported)
        assertFalse(InAppBrowserUrlRepository("not a url", false).snapshot().supported)
    }

    @Test
    fun nullAndEmptyAreUnsupportedAndKeepTitleSemantics() {
        val missing = InAppBrowserUrlRepository(null, true).snapshot()
        assertTrue(missing.useDefaultTitle)
        assertFalse(missing.supported)
        assertTrue(missing.javaScriptEnabled)
        assertEquals(null, missing.rawUrl)

        val empty = InAppBrowserUrlRepository("", false).snapshot()
        assertFalse(empty.useDefaultTitle)
        assertFalse(empty.supported)
        assertFalse(empty.javaScriptEnabled)
        assertEquals("", empty.rawUrl)
    }

    @Test
    fun javaScriptFlagPassesThroughUnchanged() {
        assertTrue(InAppBrowserUrlRepository("https://example.com", true).snapshot().javaScriptEnabled)
        assertFalse(InAppBrowserUrlRepository("https://example.com", false).snapshot().javaScriptEnabled)
    }

    @Test
    fun navigationPolicyBlocksTheSameSchemes() {
        assertFalse(InAppBrowserNavigationPolicy.shouldBlock("https://example.com/next"))
        assertFalse(InAppBrowserNavigationPolicy.shouldBlock("http://127.0.0.1/"))
        assertTrue(InAppBrowserNavigationPolicy.shouldBlock("http://example.com"))
        assertTrue(InAppBrowserNavigationPolicy.shouldBlock("javascript:alert(1)"))
        assertTrue(InAppBrowserNavigationPolicy.shouldBlock("about:blank"))
        assertTrue(InAppBrowserNavigationPolicy.shouldBlock(null))
        assertEquals("Unsupported URL", InAppBrowserNavigationPolicy.UNSUPPORTED_TEXT)
        assertEquals("text/plain", InAppBrowserNavigationPolicy.UNSUPPORTED_MIME)
        assertEquals("utf-8", InAppBrowserNavigationPolicy.UNSUPPORTED_ENCODING)
    }

    @Test
    fun snapshotToStringRedactsQueryToken() {
        val secret = "super-secret-token-xyz"
        val text = InAppBrowserUrlRepository(
            "https://example.com/cb?token=$secret",
            false
        ).snapshot().toString()
        assertFalse(text.contains(secret))
        assertFalse(text.contains("WebView"))
    }
}
