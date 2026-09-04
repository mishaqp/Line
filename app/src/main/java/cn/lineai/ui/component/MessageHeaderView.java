package cn.lineai.ui.component;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineTheme;

/**
 * The line that sits above a message: who is speaking.
 *
 * <p>Modelled on RikkaHub, which puts a 28dp avatar next to a label — the model's display
 * name on the assistant side, the user's name on the other — and mirrors the row so the
 * avatar always hugs the outer edge of the column.</p>
 *
 * <p>The avatar is a monogram rather than a brand logo: the app has no icon set keyed by
 * model id, and a letter in a tonal circle carries the same "which model answered" signal
 * without shipping artwork for every provider.</p>
 */
public final class MessageHeaderView extends LinearLayout {

    /** RikkaHub uses 28dp avatars; matched so the two chats read at the same scale. */
    private static final int AVATAR_DP = 28;
    private static final int GAP_DP = 8;

    private final TextView avatarText;
    private final IconButtonView avatarIcon;
    private final TextView label;
    private final boolean outgoing;
    private String lastName = "";

    public MessageHeaderView(Context context, boolean outgoing) {
        super(context);
        this.outgoing = outgoing;
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL | (outgoing ? Gravity.END : Gravity.START));

        int avatarPx = LineTheme.chatDp(context, AVATAR_DP);

        avatarText = new TextView(context);
        avatarText.setGravity(Gravity.CENTER);
        avatarText.setTextColor(LineTheme.ACCENT);
        avatarText.setTextSize(LineTheme.chatSp(LineTheme.TYPE_BODY_SMALL));
        avatarText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        avatarText.setIncludeFontPadding(false);
        avatarText.setBackground(LineTheme.rounded(context, LineTheme.ACCENT_MUTED, LineTheme.SHAPE_FULL));

        avatarIcon = new IconButtonView(context, IconButtonView.USER);
        avatarIcon.setIconColor(LineTheme.ACCENT);
        avatarIcon.setIconSizeDp(AVATAR_DP, 15);
        avatarIcon.setClickable(false);
        avatarIcon.setBackground(LineTheme.rounded(context, LineTheme.ACCENT_MUTED, LineTheme.SHAPE_FULL));

        label = LineTheme.chatTextMedium(context, "", LineTheme.TYPE_LABEL, LineTheme.TEXT_SECONDARY);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);

        LayoutParams avatarParams = new LayoutParams(avatarPx, avatarPx);
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        int gap = LineTheme.chatDp(context, GAP_DP);

        // Outgoing rows read name-then-avatar so the avatar lands against the right edge,
        // the mirror of the assistant side.
        if (outgoing) {
            labelParams.rightMargin = gap;
            addView(label, labelParams);
            addView(avatarIcon, avatarParams);
        } else {
            avatarText.setLayoutParams(avatarParams);
            labelParams.leftMargin = gap;
            addView(avatarText, avatarParams);
            addView(label, labelParams);
        }
    }

    /** Sets the display name; the monogram follows it on the assistant side. */
    public void bind(String name) {
        String safe = name == null ? "" : name.trim();
        if (safe.equals(lastName)) {
            return;
        }
        lastName = safe;
        label.setText(safe);
        if (!outgoing) {
            avatarText.setText(monogram(safe));
        }
    }

    /** First letter that carries meaning, uppercased; empty names fall back to a dot. */
    static String monogram(String name) {
        if (name == null) {
            return "\u2022";
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                return String.valueOf(Character.toUpperCase(c));
            }
        }
        return "\u2022";
    }
}
