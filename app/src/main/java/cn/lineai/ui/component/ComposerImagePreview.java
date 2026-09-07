package cn.lineai.ui.component;

import android.content.Context;
import android.net.Uri;
import android.widget.FrameLayout;

/** Legacy ComposerView boundary. Image preview state/UI live in Foundation v2. */
final class ComposerImagePreview extends FrameLayout {
    interface Listener {
        void onImageStateChanged();
    }

    private final ComposerImagePreviewHostView hostView;
    private Listener listener;

    ComposerImagePreview(Context context) {
        super(context);
        hostView = new ComposerImagePreviewHostView(
                context,
                new ComposerImagePreviewHostView.Listener() {
                    @Override
                    public void onImageStateChanged(boolean visible) {
                        setVisibility(visible ? VISIBLE : GONE);
                        if (listener != null) {
                            listener.onImageStateChanged();
                        }
                    }
                }
        );
        setVisibility(GONE);
        addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void show(Uri uri, String encodedBase64, String encodedMimeType, String displayName) {
        setVisibility(hostView.show(uri, encodedBase64, encodedMimeType, displayName)
                ? VISIBLE : GONE);
    }

    void clear() {
        setVisibility(hostView.clear() ? VISIBLE : GONE);
    }

    boolean hasImage() {
        return hostView.hasImage();
    }

    Uri uri() {
        return hostView.uri();
    }

    String base64() {
        return hostView.base64();
    }

    String mimeType() {
        return hostView.mimeType();
    }

    String name() {
        return hostView.name();
    }
}
