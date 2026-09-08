package cn.lineai.ui.component

import cn.lineai.security.UrlPolicy
import cn.lineai.ui.model.InAppBrowserRepository
import cn.lineai.ui.model.InAppBrowserSnapshot

class InAppBrowserUrlRepository(
    private val rawUrl: String?,
    private val javaScriptEnabled: Boolean
) : InAppBrowserRepository {

    override fun snapshot(): InAppBrowserSnapshot {
        val normalized = UrlPolicy.normalizeHttpOrLocalCleartextUrl(rawUrl)
        return InAppBrowserSnapshot(
            rawUrl = rawUrl,
            useDefaultTitle = rawUrl == null,
            normalizedUrl = normalized,
            supported = normalized.isNotEmpty(),
            javaScriptEnabled = javaScriptEnabled
        )
    }
}

object InAppBrowserNavigationPolicy {
    const val UNSUPPORTED_TEXT = "Unsupported URL"
    const val UNSUPPORTED_MIME = "text/plain"
    const val UNSUPPORTED_ENCODING = "utf-8"

    fun shouldBlock(nextUrl: String?): Boolean {
        return UrlPolicy.normalizeHttpOrLocalCleartextUrl(nextUrl).isEmpty()
    }
}
