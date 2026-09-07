package cn.lineai.ui.component

import cn.lineai.ui.model.ComposerImagePreviewSnapshot
import cn.lineai.ui.model.ComposerImagePreviewStateRepository

class ComposerImagePreviewRepository : ComposerImagePreviewStateRepository {
    private var current = ComposerImagePreviewSnapshot()

    override fun snapshot(): ComposerImagePreviewSnapshot = current

    override fun show(
        uri: String?,
        base64: String?,
        mimeType: String?,
        name: String?
    ) {
        current = ComposerImagePreviewSnapshot(
            visible = true,
            uri = uri,
            base64 = base64.orEmpty(),
            mimeType = mimeType.orEmpty(),
            name = name.orEmpty(),
            revision = current.revision + 1L
        )
    }

    override fun clear() {
        current = ComposerImagePreviewSnapshot(revision = current.revision + 1L)
    }
}
