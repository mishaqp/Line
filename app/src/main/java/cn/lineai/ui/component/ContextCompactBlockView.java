package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

/** Legacy message-row boundary. The compact-status UI/state live in Foundation v2. */
public final class ContextCompactBlockView extends FrameLayout {
    private final ContextCompactHostView hostView;

    public ContextCompactBlockView(Context context) {
        super(context);
        ContextCompactStatusRepository repository = new ContextCompactStatusRepository(
                context.getString(cn.lineai.R.string.context_compact_label)
        );
        hostView = new ContextCompactHostView(context, repository);
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );
    }

    public void bind(String status) {
        hostView.bind(status);
    }
}
