package cn.lineai.ui.component;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class CardViewHelper {

    public interface OnCardClickListener {
        void onCardClick(String id);
    }

    private CardViewHelper() {
    }

    public static void addCard(LinearLayout content, String id, String title, String desc, String badge, int iconType, OnCardClickListener clickListener) {
        Context context = content.getContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> clickListener.onCardClick(id));
        card.setBackground(LineCards.cardBackground(context));
        LineTheme.attachStateLayer(card);
        LineTheme.padding(card, LineTheme.LG, LineTheme.MD, LineTheme.MD, LineTheme.MD);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = LineTheme.dp(context, LineTheme.SM);
        content.addView(card, cardParams);

        FrameLayout iconWrap = new FrameLayout(context);
        iconWrap.setBackground(LineTheme.rounded(context, LineTheme.ACCENT_MUTED, LineTheme.SHAPE_MD));
        IconButtonView icon = new IconButtonView(context, iconType);
        icon.setIconColor(LineTheme.ACCENT);
        icon.setIconSizeDp(44, 22);
        icon.setClickable(false);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(LineTheme.dp(context, 44), LineTheme.dp(context, 44), Gravity.CENTER));
        card.addView(iconWrap, new LinearLayout.LayoutParams(LineTheme.dp(context, 44), LineTheme.dp(context, 44)));

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = LineTheme.dp(context, LineTheme.MD);
        textParams.rightMargin = LineTheme.dp(context, LineTheme.MD);
        card.addView(text, textParams);

        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        text.addView(titleRow, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView titleView = LineCards.title(context, title);
        titleRow.addView(titleView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView badgeView = LineCards.badge(context, badge);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        badgeParams.leftMargin = LineTheme.dp(context, LineTheme.SM);
        titleRow.addView(badgeView, badgeParams);

        TextView descView = LineCards.desc(context, desc);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descParams.topMargin = LineTheme.dp(context, LineTheme.XS);
        text.addView(descView, descParams);

        IconButtonView chevron = new IconButtonView(context, IconButtonView.CHEVRON_RIGHT);
        chevron.setIconColor(LineTheme.TEXT_TERTIARY);
        chevron.setIconSizeDp(20, 17);
        chevron.setClickable(false);
        card.addView(chevron, new LinearLayout.LayoutParams(LineTheme.dp(context, 20), LineTheme.dp(context, 20)));
    }
}
