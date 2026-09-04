package cn.lineai.ui.component;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import cn.lineai.R;

public final class MessageActionBarView extends LinearLayout {
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_RIGHT = 1;

    /** Height of the action row; also the icon touch-target height. */
    private static final int ROW_HEIGHT_DP = 26;
    /** Width of a single action button. */
    private static final int ICON_WIDTH_DP = 27;
    private final IconButtonView copyButton;
    private final IconButtonView quoteButton;
    private final IconButtonView shareButton;
    private final IconButtonView selectButton;
    private final IconButtonView multiSelectButton;
    private final IconButtonView recallButton;
    /** Whether the secondary actions are revealed; copy alone is the resting state. */
    private boolean expanded;
    /** False while a message streams, when no action applies yet. */
    private boolean actionsAllowed = true;

    public MessageActionBarView(Context context, int align, boolean recallEnabled) {
        this(context, align, recallEnabled, false);
    }

    public MessageActionBarView(Context context, int align, boolean recallEnabled, boolean streaming) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(align == ALIGN_RIGHT ? Gravity.END : Gravity.START);
        setMinimumHeight(LineTheme.chatDp(context, ROW_HEIGHT_DP));
        // No container plate: the row repeats under every message, and a filled pill that
        // wide competes with the bubble above it. The icons carry themselves.
        LineTheme.padding(this, 0, 0, 0, 0);

        copyButton = icon(context, IconButtonView.COPY);
        copyButton.setContentDescription(context.getString(R.string.message_action_copy_desc));
        addView(copyButton, iconParams(context));

        quoteButton = icon(context, IconButtonView.QUOTE);
        quoteButton.setContentDescription(context.getString(R.string.message_action_quote_desc));
        addView(quoteButton, iconParams(context));

        shareButton = icon(context, IconButtonView.SHARE);
        shareButton.setContentDescription(context.getString(R.string.message_action_share_desc));
        addView(shareButton, iconParams(context));

        selectButton = icon(context, IconButtonView.TEXT_CURSOR);
        selectButton.setContentDescription(context.getString(R.string.message_action_select_desc));
        addView(selectButton, iconParams(context));

        multiSelectButton = icon(context, IconButtonView.CHECK_SQUARE);
        multiSelectButton.setContentDescription(context.getString(R.string.message_action_multi_select_desc));
        addView(multiSelectButton, iconParams(context));

        IconButtonView recall = null;
        if (recallEnabled) {
            recall = icon(context, IconButtonView.ROTATE_CCW);
            recall.setContentDescription(context.getString(R.string.message_action_recall_desc));
            addView(recall, iconParams(context));
        }
        recallButton = recall;

        actionsAllowed = !streaming;
        applyVisibility();
    }

    /**
     * Reveals or hides everything except copy.
     *
     * <p>The row sits under every message in the thread, so only the one action worth a
     * permanent slot stays visible; the rest are a long press away.</p>
     */
    public void setExpanded(boolean value) {
        if (expanded == value) {
            return;
        }
        expanded = value;
        applyVisibility();
    }

    public boolean isExpanded() {
        return expanded;
    }

    private void applyVisibility() {
        int secondary = actionsAllowed && expanded ? VISIBLE : GONE;
        quoteButton.setVisibility(secondary);
        shareButton.setVisibility(secondary);
        selectButton.setVisibility(secondary);
        multiSelectButton.setVisibility(secondary);
        if (recallButton != null) {
            recallButton.setVisibility(secondary);
        }
        copyButton.setVisibility(actionsAllowed ? VISIBLE : GONE);
    }

    public void setActionListener(ActionListener listener) {
        copyButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCopy();
            }
        });
        quoteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuote();
            }
        });
        shareButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShare();
            }
        });
    }

    public void setSelectListener(SelectListener listener) {
        selectButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelect();
            }
        });
        multiSelectButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMultiSelect();
            }
        });
    }

    public void setRecallListener(RecallListener listener) {
        if (recallButton != null) {
            recallButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecall();
                }
            });
        }
    }

    public void setActionsVisible(boolean visible) {
        if (actionsAllowed == visible) {
            return;
        }
        actionsAllowed = visible;
        if (!visible) {
            expanded = false;
        }
        applyVisibility();
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

    /**
     * Compact M3 icon button: transparent {@link LineTheme#SHAPE_FULL} container that only
     * becomes visible through its state layer on hover / focus / press.
     */
    private IconButtonView icon(Context context, int type) {
        IconButtonView icon = new IconButtonView(context, type);
        icon.setIconColor(LineTheme.TEXT_SECONDARY);
        icon.setIconPaddingDp(7, 7, 7, 7);
        icon.setClickable(true);
        icon.setFocusable(true);
        icon.setBackground(LineCards.pillBackground(context, Color.TRANSPARENT));
        LineTheme.attachStateLayer(icon);
        return icon;
    }

    private LinearLayout.LayoutParams iconParams(Context context) {
        return new LinearLayout.LayoutParams(
                LineTheme.chatDp(context, ICON_WIDTH_DP), LineTheme.chatDp(context, ROW_HEIGHT_DP));
    }
}
