package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * Legacy constructor boundary for {@code browser} / {@code browser:<url>}.
 * The screen itself is hosted by {@link InAppBrowserHostView}.
 */
public final class InAppBrowserScreenView extends FrameLayout {

    public interface Listener {
        void onBack();
    }

    public InAppBrowserScreenView(
            Context context,
            String url,
            boolean javaScriptEnabled,
            Listener listener
    ) {
        super(context);
        InAppBrowserUrlRepository repository = new InAppBrowserUrlRepository(url, javaScriptEnabled);
        InAppBrowserHostView host = new InAppBrowserHostView(
                context,
                repository,
                new InAppBrowserHostView.Listener() {
                    @Override
                    public void onBack() {
                        if (listener != null) {
                            listener.onBack();
                        }
                    }
                }
        );
        addView(host, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }
}
