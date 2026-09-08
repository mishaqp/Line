package cn.lineai.ui.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppBrowserViewModelTest {

    @Test
    fun initialSnapshotIsReadOnceAndKeepsJavaScriptFlag() {
        val repository = RecordingRepository(
            InAppBrowserSnapshot(
                rawUrl = "https://example.com/path",
                useDefaultTitle = false,
                normalizedUrl = "https://example.com/path",
                supported = true,
                javaScriptEnabled = true
            )
        )
        val viewModel = InAppBrowserViewModel(repository)

        assertEquals(1, repository.snapshotCalls)
        assertTrue(viewModel.state.value.supported)
        assertTrue(viewModel.state.value.javaScriptEnabled)
        assertFalse(viewModel.state.value.useDefaultTitle)
        assertEquals("https://example.com/path", viewModel.state.value.headerUrl)
        assertEquals("https://example.com/path", viewModel.state.value.normalizedUrl)
        viewModel.state.value
        assertEquals(1, repository.snapshotCalls)
    }

    @Test
    fun supportedAndUnsupportedInitialUrlsAreDistinguished() {
        val supported = InAppBrowserViewModel(
            RecordingRepository(
                InAppBrowserSnapshot(
                    rawUrl = "https://example.com",
                    useDefaultTitle = false,
                    normalizedUrl = "https://example.com",
                    supported = true,
                    javaScriptEnabled = false
                )
            )
        )
        val unsupported = InAppBrowserViewModel(
            RecordingRepository(
                InAppBrowserSnapshot(
                    rawUrl = "about:blank",
                    useDefaultTitle = false,
                    normalizedUrl = "",
                    supported = false,
                    javaScriptEnabled = false
                )
            )
        )
        assertTrue(supported.state.value.supported)
        assertFalse(unsupported.state.value.supported)
        assertEquals("", unsupported.state.value.normalizedUrl)
        assertFalse(unsupported.state.value.javaScriptEnabled)
    }

    @Test
    fun nullUrlUsesDefaultTitleFlagWithoutResources() {
        val viewModel = InAppBrowserViewModel(
            RecordingRepository(
                InAppBrowserSnapshot(
                    rawUrl = null,
                    useDefaultTitle = true,
                    normalizedUrl = "",
                    supported = false,
                    javaScriptEnabled = true
                )
            )
        )
        assertTrue(viewModel.state.value.useDefaultTitle)
        assertEquals("", viewModel.state.value.headerUrl)
        assertTrue(viewModel.state.value.javaScriptEnabled)
    }

    @Test
    fun backIsOneShotAndDoesNotReload() {
        val repository = RecordingRepository()
        val viewModel = InAppBrowserViewModel(repository)
        assertEquals(InAppBrowserUiEffect.Back, viewModel.onAction(InAppBrowserUiAction.Back))
        assertEquals(InAppBrowserUiEffect.Back, viewModel.onAction(InAppBrowserUiAction.Back))
        assertEquals(1, repository.snapshotCalls)
    }

    @Test
    fun snapshotExceptionYieldsSafeUnsupportedState() {
        val viewModel = InAppBrowserViewModel(object : InAppBrowserRepository {
            override fun snapshot(): InAppBrowserSnapshot {
                throw IllegalStateException("boom")
            }
        })
        assertFalse(viewModel.state.value.supported)
        assertTrue(viewModel.state.value.useDefaultTitle)
        assertEquals("", viewModel.state.value.normalizedUrl)
        assertFalse(viewModel.state.value.javaScriptEnabled)
        assertEquals(InAppBrowserUiEffect.Back, viewModel.onAction(InAppBrowserUiAction.Back))
    }

    @Test
    fun uiStateToStringRedactsQueryToken() {
        val secret = "super-secret-token-xyz"
        val viewModel = InAppBrowserViewModel(
            RecordingRepository(
                InAppBrowserSnapshot(
                    rawUrl = "https://example.com/cb?token=$secret#frag",
                    useDefaultTitle = false,
                    normalizedUrl = "https://example.com/cb?token=$secret#frag",
                    supported = true,
                    javaScriptEnabled = false
                )
            )
        )
        val text = viewModel.state.value.toString()
        assertFalse(text.contains(secret))
        assertFalse(text.contains("android."))
        assertFalse(text.contains("WebView"))
        assertFalse(text.contains("Context"))
    }

    @Test
    fun viewModelClassDoesNotExposeAndroidTypes() {
        val fields = InAppBrowserViewModel::class.java.declaredFields.map { it.type.name }
        assertFalse(fields.any { it.startsWith("android.") })
        assertFalse(
            InAppBrowserViewModel::class.java.declaredMethods.any { method ->
                method.parameterTypes.any { it.name.startsWith("android.") } ||
                    method.returnType.name.startsWith("android.")
            }
        )
    }

    private class RecordingRepository(
        var snapshotValue: InAppBrowserSnapshot = InAppBrowserSnapshot()
    ) : InAppBrowserRepository {
        var snapshotCalls = 0

        override fun snapshot(): InAppBrowserSnapshot {
            snapshotCalls += 1
            return snapshotValue
        }
    }
}
