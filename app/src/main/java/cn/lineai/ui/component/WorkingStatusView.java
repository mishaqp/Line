package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

/** Legacy message-row boundary. The working indicator UI/state live in Foundation v2. */
public final class WorkingStatusView extends FrameLayout {
    private final WorkingStatusHostView hostView;

    public WorkingStatusView(Context context) {
        this(
                context,
                context.getString(cn.lineai.R.string.message_assistant_working),
                context.getString(cn.lineai.R.string.message_assistant_thinking)
        );
    }

    WorkingStatusView(Context context, String workingLabel, String thinkingLabel) {
        super(context);
        WorkingStatusLabelRepository repository =
                new WorkingStatusLabelRepository(workingLabel, thinkingLabel);
        hostView = new WorkingStatusHostView(context, repository);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        addView(
                hostView,
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
    }

    public void bind(boolean thinking) {
        hostView.bind(thinking);
    }

    public void startWorking() {
        hostView.startWorking();
    }

    public void stopWorking() {
        hostView.stopWorking();
    }

    public boolean isWorking() {
        return hostView.isWorking();
    }

    static int highlightColor(int baseColor) {
        return WorkingStatusMath.INSTANCE.highlightColor(baseColor);
    }

    static boolean isThinking(String reasoning, String content) {
        return WorkingStatusMath.INSTANCE.isThinking(reasoning, content);
    }
}
