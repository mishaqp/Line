package cn.lineai.ui.component;

import android.content.Context;
import android.widget.LinearLayout;

/** Legacy message-row boundary. Header UI/state live in Foundation v2. */
public final class MessageHeaderView extends LinearLayout {
    private final MessageHeaderHostView hostView;

    public MessageHeaderView(Context context, boolean outgoing) {
        super(context);
        hostView = new MessageHeaderHostView(
                context,
                new MessageHeaderRepository(outgoing)
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        );
    }

    /** Sets the display name; the assistant monogram follows it. */
    public void bind(String name) {
        hostView.bind(name);
    }

    /** Preserved legacy boundary used by JVM tests and message rows. */
    static String monogram(String name) {
        return MessageHeaderText.monogram(name);
    }
}
