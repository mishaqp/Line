package cn.lineai.ui.component;

import android.content.Context;
import android.widget.LinearLayout;
import cn.lineai.ui.model.MessageActionBarSnapshot;

/** Legacy message-row boundary. Action UI/state live in Foundation v2. */
public final class MessageActionBarView extends LinearLayout {
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_RIGHT = 1;

    private final MessageActionBarHostView hostView;
    private ActionListener actionListener;
    private SelectListener selectListener;
    private RecallListener recallListener;
    private Runnable moreListener;

    public MessageActionBarView(Context context, int align, boolean recallEnabled) {
        this(context, align, recallEnabled, false);
    }

    public MessageActionBarView(
            Context context,
            int align,
            boolean recallEnabled,
            boolean streaming
    ) {
        super(context);
        MessageActionBarRepository repository =
                new MessageActionBarRepository(
                        align == ALIGN_RIGHT,
                        recallEnabled,
                        !streaming
                );
        hostView = new MessageActionBarHostView(
                context,
                repository,
                new MessageActionBarHostView.Listener() {
                    @Override
                    public void onCopy() {
                        if (actionListener != null) actionListener.onCopy();
                    }

                    @Override
                    public void onQuote() {
                        if (actionListener != null) actionListener.onQuote();
                    }

                    @Override
                    public void onShare() {
                        if (actionListener != null) actionListener.onShare();
                    }

                    @Override
                    public void onSelect() {
                        if (selectListener != null) selectListener.onSelect();
                    }

                    @Override
                    public void onMultiSelect() {
                        if (selectListener != null) selectListener.onMultiSelect();
                    }

                    @Override
                    public void onRecall() {
                        if (recallListener != null) recallListener.onRecall();
                    }

                    @Override
                    public void onMore() {
                        if (moreListener != null) moreListener.run();
                    }
                }
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
    }

    public void setExpanded(boolean value) {
        hostView.setExpanded(value);
    }

    public boolean isExpanded() {
        return hostView.isExpanded();
    }

    public void setActionListener(ActionListener listener) {
        actionListener = listener;
    }

    public void setSelectListener(SelectListener listener) {
        selectListener = listener;
    }

    public void setRecallListener(RecallListener listener) {
        recallListener = listener;
    }

    public void setActionsVisible(boolean visible) {
        hostView.setActionsVisible(visible);
    }

    public void setMoreListener(Runnable onMore) {
        moreListener = onMore;
    }

    static MessageActionBarSnapshot initialSnapshot(
            int align,
            boolean recallEnabled,
            boolean streaming
    ) {
        return new MessageActionBarSnapshot(
                align == ALIGN_RIGHT,
                recallEnabled,
                !streaming,
                false
        );
    }

    public interface ActionListener {
        void onCopy();
        void onQuote();
        void onShare();
    }

    public interface SelectListener {
        void onSelect();
        void onMultiSelect();
    }

    public interface RecallListener {
        void onRecall();
    }
}
