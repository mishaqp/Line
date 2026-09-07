package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.InputAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Legacy ComposerView boundary for pending file attachments.
 *
 * <p>Attachment state and rendering live in Foundation v2. This class keeps the Java API used by
 * {@link ComposerView} stable while delegating state to {@link ComposerAttachmentRepository} and
 * UI to Compose.</p>
 */
final class ComposerAttachmentStrip extends FrameLayout {

    /** Notified after any mutation of the attachment list. */
    interface Listener {
        void onAttachmentsChanged();
    }

    private final ComposerAttachmentRepository repository;
    private final ComposerAttachmentStripHostView hostView;
    private Listener listener;

    ComposerAttachmentStrip(Context context) {
        super(context);
        repository = new ComposerAttachmentRepository();
        hostView = new ComposerAttachmentStripHostView(
                context,
                repository,
                new ComposerAttachmentStripHostView.Listener() {
                    @Override
                    public void onAttachmentsChanged(boolean visible) {
                        setVisibility(visible ? VISIBLE : GONE);
                        notifyListener();
                    }
                }
        );
        setVisibility(GONE);
        addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Immutable snapshot of the current attachments, in insertion order. */
    List<InputAttachment> attachments() {
        return Collections.unmodifiableList(new ArrayList<>(repository.attachments()));
    }

    boolean isEmpty() {
        return repository.isEmpty();
    }

    /** Replaces the whole list (used when a draft is restored). */
    void replaceAll(List<InputAttachment> next) {
        repository.replaceAll(next);
        refreshAndNotify();
    }

    void clear() {
        repository.clear();
        refreshAndNotify();
    }

    /** Adds {@code attachment}, or removes it when the same path/source is already present. */
    void toggle(InputAttachment attachment) {
        if (repository.toggle(attachment)) {
            refreshAndNotify();
        }
    }

    /** Paths of the attachments coming from {@code source} (local or SSH). */
    List<String> pathsForSource(String source) {
        return repository.pathsForSource(source);
    }

    private void refreshAndNotify() {
        setVisibility(hostView.refresh() ? VISIBLE : GONE);
        notifyListener();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onAttachmentsChanged();
        }
    }
}
