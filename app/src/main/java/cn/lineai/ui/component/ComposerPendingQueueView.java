package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

/** Legacy ComposerView boundary. The pending-queue UI/state live in Foundation v2. */
final class ComposerPendingQueueView extends FrameLayout {
    interface Listener {
        void onQueueChanged();
    }

    private final ComposerPendingQueueHostView hostView;
    private Listener listener;

    ComposerPendingQueueView(Context context, ComposerQueue queue) {
        super(context);
        ComposerQueueRepository repository = new ComposerQueueRepository(queue);
        hostView = new ComposerPendingQueueHostView(
                context,
                repository,
                new ComposerPendingQueueHostView.Listener() {
                    @Override
                    public void onQueueChanged(boolean visible) {
                        setVisibility(visible ? VISIBLE : GONE);
                        if (listener != null) {
                            listener.onQueueChanged();
                        }
                    }
                }
        );
        setVisibility(GONE);
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void refresh() {
        setVisibility(hostView.refresh() ? VISIBLE : GONE);
    }
}
