package cn.lineai.ui.component;
import cn.lineai.ui.theme.LineTheme;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import cn.lineai.R;
import cn.lineai.model.ChatMessage;

public final class UserMessageView extends LinearLayout {
    private static final long ENTRANCE_FADE_MS = 220L;

    private final UserMessageContentView contentView;
    private final UserMessageAttachmentList attachmentList;
    private final MessageActionBarView actionBar;
    private final MessageHeaderView headerView;
    private final int defaultPaddingLeft;
    private final int defaultPaddingTop;
    private final int defaultPaddingRight;
    private final int defaultPaddingBottom;
    private String lastAnimatedMessageId = "";
    private ChatMessage currentMessage;
    private MessageActionListener actionListener;

    public UserMessageView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        LineTheme.chatPadding(this, LineTheme.LG, LineTheme.XS, LineTheme.LG, LineTheme.LG);
        defaultPaddingLeft = getPaddingLeft();
        defaultPaddingTop = getPaddingTop();
        defaultPaddingRight = getPaddingRight();
        defaultPaddingBottom = getPaddingBottom();

        headerView = new MessageHeaderView(context, true);
        headerView.bind(context.getString(R.string.message_header_user));
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        headerParams.bottomMargin = LineTheme.chatDp(context, LineTheme.XS);
        addView(headerView, headerParams);

        contentView = new UserMessageContentView(
                context,
                context.getString(R.string.message_user_attached_files)
        );
        addView(
                contentView,
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );

        attachmentList = new UserMessageAttachmentList(context);
        LinearLayout.LayoutParams attachmentParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        attachmentParams.topMargin = LineTheme.dp(context, LineTheme.XS);
        addView(attachmentList, attachmentParams);

        actionBar = new MessageActionBarView(context, MessageActionBarView.ALIGN_RIGHT, true);
        actionBar.setActionListener(new MessageActionBarView.ActionListener() {
            @Override
            public void onCopy() {
                if (actionListener != null && currentMessage != null) {
                    actionListener.onCopyMessage(currentMessage);
                }
            }

            @Override
            public void onQuote() {
                if (actionListener != null && currentMessage != null) {
                    actionListener.onQuoteMessage(currentMessage);
                }
            }

            @Override
            public void onShare() {
                if (actionListener != null && currentMessage != null) {
                    actionListener.onShareMessage(currentMessage);
                }
            }
        });
        actionBar.setSelectListener(new MessageActionBarView.SelectListener() {
            @Override
            public void onSelect() {
                if (actionListener != null && currentMessage != null) {
                    actionListener.onSelectText(currentMessage);
                }
            }

            @Override
            public void onMultiSelect() {
                if (actionListener != null) {
                    actionListener.onMultiSelectToggle();
                }
            }
        });
        actionBar.setRecallListener(new MessageActionBarView.RecallListener() {
            @Override
            public void onRecall() {
                if (actionListener != null && currentMessage != null) {
                    actionListener.onRecallMessage(currentMessage);
                }
            }
        });
        actionBar.setMoreListener(this::toggleActionBar);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        actionParams.topMargin = LineTheme.dp(context, LineTheme.XS);
        addView(actionBar, actionParams);
    }

    /**
     * Toggles the secondary actions; driven by the list's item long-press.
     *
     * <p>Deliberately not an {@code OnLongClickListener} on this view: that would make the
     * row consume touches and {@code ListView} would stop reporting item clicks, which is
     * how multi-select picks messages.</p>
     *
     * @return {@code true} when the press was consumed.
     */
    public boolean toggleActionBar() {
        if (actionBar.getVisibility() != VISIBLE) {
            return false;
        }
        boolean next = !actionBar.isExpanded();
        actionBar.setExpanded(next);
        if (next) {
            performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
        return true;
    }


    public void setMessageActionListener(MessageActionListener listener) {
        actionListener = listener;
    }

    public void restoreDefaultPadding() {
        setPadding(defaultPaddingLeft, defaultPaddingTop, defaultPaddingRight, defaultPaddingBottom);
    }

    public void bind(ChatMessage message) {
        currentMessage = message;
        String messageId = message.getId() == null ? "" : message.getId();
        if (!lastAnimatedMessageId.equals(messageId)) {
            actionBar.setExpanded(false);
            lastAnimatedMessageId = messageId;
            setAlpha(0f);
            animate().alpha(1f).setDuration(ENTRANCE_FADE_MS).start();
        }
        contentView.bind(message);
        renderAttachments(message);
    }

    private void renderAttachments(ChatMessage message) {
        attachmentList.bind(
                message == null || !message.hasAttachments()
                        ? null
                        : message.getAttachments()
        );
    }
}
