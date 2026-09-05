package cn.lineai.ui.component;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.model.ChatMode;
import cn.lineai.model.ChatUiState;

public final class HeaderView extends LinearLayout {
    public interface Listener {
        void onMenuClick();

        void onProjectClick();

        void onModeChanged(String mode);

        void onPermissionClick();

        void onNewConversationClick();

        void onMoreClick();
    }

    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextView projectText;
    private final TextView targetText;
    private final TextView modeText;
    private final LinearLayout modeButton;
    private Listener listener;
    private String chatMode = ChatMode.DEFAULT;
    private PopupWindow modePopup;

    public HeaderView(Context context) {
        super(context);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundColor(LineTheme.BG);
        setWillNotDraw(false);
        LineTheme.padding(this, LineTheme.MD, LineTheme.MD, LineTheme.MD, LineTheme.MD);
        setMinimumHeight(LineTheme.dp(context, 58));

        IconButtonView menu = icon(context, IconButtonView.MENU, LineTheme.TEXT, 20);
        menu.setContentDescription(context.getString(R.string.header_menu_desc));
        menu.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMenuClick();
            }
        });
        addView(menu);

        LinearLayout projectButton = new LinearLayout(context);
        projectButton.setOrientation(HORIZONTAL);
        projectButton.setGravity(Gravity.CENTER_VERTICAL);
        projectButton.setClickable(true);
        projectButton.setFocusable(true);
        projectButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProjectClick();
            }
        });
        LinearLayout.LayoutParams projectParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        projectParams.leftMargin = LineTheme.dp(context, 4);
        projectParams.rightMargin = LineTheme.dp(context, 6);
        addView(projectButton, projectParams);

        View dot = new View(context);
        dot.setBackground(LineTheme.rounded(context, LineTheme.ACCENT, LineTheme.SHAPE_XS));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(LineTheme.dp(context, 7), LineTheme.dp(context, 7));
        projectButton.addView(dot, dotParams);

        LinearLayout titles = new LinearLayout(context);
        titles.setOrientation(VERTICAL);
        LinearLayout.LayoutParams titlesParams = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        titlesParams.leftMargin = LineTheme.dp(context, 6);
        projectButton.addView(titles, titlesParams);

        projectText = LineTheme.textMedium(context, context.getString(R.string.header_project_default), LineTheme.FONT_MD, LineTheme.TEXT);
        projectText.setSingleLine(true);
        titles.addView(projectText);

        targetText = LineTheme.textMedium(context, "", LineTheme.FONT_XS, LineTheme.TEXT_SECONDARY);
        targetText.setSingleLine(true);
        titles.addView(targetText);

        IconButtonView chevron = icon(context, IconButtonView.CHEVRON_DOWN, LineTheme.TEXT_SECONDARY, 14);
        chevron.setIconSizeDp(20, 14);
        projectButton.addView(chevron, new LinearLayout.LayoutParams(LineTheme.dp(context, 20), LineTheme.dp(context, 20)));

        modeButton = new LinearLayout(context);
        modeButton.setOrientation(HORIZONTAL);
        modeButton.setGravity(Gravity.CENTER_VERTICAL);
        modeButton.setClickable(true);
        modeButton.setFocusable(true);
        modeButton.setBackground(LineCards.chipBackground(context));
        LineTheme.attachStateLayer(modeButton);
        LineTheme.padding(modeButton, LineTheme.SM, 0, LineTheme.SM, 0);
        modeButton.setOnClickListener(v -> showModePopup(modeButton));
        modeText = LineTheme.textMedium(context, modeLabel(chatMode), LineTheme.FONT_XS, LineTheme.TEXT);
        modeText.setSingleLine(true);
        modeButton.addView(modeText);
        IconButtonView modeChevron = icon(context, IconButtonView.CHEVRON_DOWN, LineTheme.TEXT_SECONDARY, 12);
        modeChevron.setIconSizeDp(16, 12);
        modeChevron.setClickable(false);
        LinearLayout.LayoutParams modeChevronParams = new LinearLayout.LayoutParams(LineTheme.dp(context, 16), LineTheme.dp(context, 16));
        modeChevronParams.leftMargin = LineTheme.dp(context, 2);
        modeButton.addView(modeChevron, modeChevronParams);
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LineTheme.dp(context, 28));
        modeParams.rightMargin = LineTheme.dp(context, 4);
        addView(modeButton, modeParams);

        IconButtonView shield = icon(context, IconButtonView.SHIELD, LineTheme.TEXT_SECONDARY, 18);
        shield.setContentDescription(context.getString(R.string.header_permission_desc));
        shield.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPermissionClick();
            }
        });
        addView(shield);

        IconButtonView plus = icon(context, IconButtonView.PLUS, LineTheme.TEXT_SECONDARY, 20);
        plus.setContentDescription(context.getString(R.string.header_new_conversation_desc));
        plus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNewConversationClick();
            }
        });
        addView(plus);

        IconButtonView more = icon(context, IconButtonView.MORE, LineTheme.TEXT_SECONDARY, 18);
        more.setContentDescription(context.getString(R.string.header_more_desc));
        more.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMoreClick();
            }
        });
        addView(more);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void render(ChatUiState state) {
        projectText.setText(state.getProjectLabel());
        String target = state.getExecutionTargetLabel();
        targetText.setText(target);
        targetText.setVisibility(target == null || target.length() == 0 ? GONE : VISIBLE);
        chatMode = state.getChatMode();
        modeText.setText(modeLabel(chatMode));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        borderPaint.setColor(LineTheme.BORDER);
        borderPaint.setStrokeWidth(1f);
        canvas.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1, borderPaint);
    }

    private void showModePopup(View anchor) {
        if (modePopup != null && modePopup.isShowing()) {
            modePopup.dismiss();
        }
        Context context = getContext();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(VERTICAL);
        content.setBackground(LineTheme.roundedStroke(context, LineTheme.SURFACE_ELEVATED, LineTheme.SHAPE_LG, LineTheme.BORDER));
        LineTheme.padding(content, LineTheme.SM, LineTheme.SM, LineTheme.SM, LineTheme.SM);
        int rowHeight = LineTheme.dp(context, 36);
        content.addView(modeOption(context, modeLabel(ChatMode.CHAT), ChatMode.CHAT),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        content.addView(modeOption(context, modeLabel(ChatMode.PLAN), ChatMode.PLAN),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        content.addView(modeOption(context, modeLabel(ChatMode.AGENT), ChatMode.AGENT),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        content.addView(modeOption(context, modeLabel(ChatMode.CONTROL), ChatMode.CONTROL),
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, rowHeight));
        int popupWidth = LineTheme.dp(context, 160);
        modePopup = new PopupWindow(content, popupWidth, LayoutParams.WRAP_CONTENT, true);
        modePopup.setOutsideTouchable(true);
        modePopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        modePopup.showAsDropDown(anchor, 0, LineTheme.dp(context, 6));
    }

    private TextView modeOption(Context context, String label, String mode) {
        boolean selected = mode.equals(chatMode);
        TextView item = LineTheme.textMedium(context, label, LineTheme.FONT_SM,
                selected ? LineTheme.TEXT_ON_COLOR : LineTheme.TEXT);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setSingleLine(true);
        item.setPadding(LineTheme.dp(context, LineTheme.MD), 0, LineTheme.dp(context, LineTheme.MD), 0);
        item.setBackground(LineTheme.rounded(context, selected ? LineTheme.ACCENT : android.graphics.Color.TRANSPARENT, LineTheme.SHAPE_MD));
        item.setClickable(true);
        item.setOnClickListener(v -> {
            if (modePopup != null) {
                modePopup.dismiss();
            }
            if (!mode.equals(chatMode) && listener != null) {
                listener.onModeChanged(mode);
            }
        });
        return item;
    }

    private String modeLabel(String mode) {
        if (ChatMode.CHAT.equals(mode)) {
            return getContext().getString(R.string.header_mode_chat);
        }
        if (ChatMode.PLAN.equals(mode)) {
            return getContext().getString(R.string.header_mode_plan);
        }
        if (ChatMode.CONTROL.equals(mode)) {
            return getContext().getString(R.string.header_mode_control);
        }
        return getContext().getString(R.string.header_mode_agent);
    }

    private IconButtonView icon(Context context, int type, int color, int iconDp) {
        IconButtonView view = new IconButtonView(context, type);
        view.setIconColor(color);
        view.setIconSizeDp(34, iconDp);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LineTheme.dp(context, 34), LineTheme.dp(context, 34));
        view.setLayoutParams(params);
        return view;
    }
}
