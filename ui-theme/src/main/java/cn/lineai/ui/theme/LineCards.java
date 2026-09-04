package cn.lineai.ui.theme;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Shared Material Design 3 surface / button factories.
 *
 * <p>Before this class every screen view re-implemented the same four widgets by hand —
 * a stroked card, a filled "primary" pill, an outlined "secondary" pill and a
 * title/description text pair — each with its own hardcoded corner radius. The helpers
 * here centralise those recipes on the M3 shape scale
 * ({@link LineTheme#SHAPE_SM} / {@link LineTheme#SHAPE_MD} / {@link LineTheme#SHAPE_FULL})
 * and the M3 type scale, and attach state layers where a widget is interactive.</p>
 *
 * <p>Everything is a plain framework view built in Java; no XML, no Material Components
 * dependency. Callers own layout params — these factories only produce configured views
 * and drawables.</p>
 */
public final class LineCards {

    private LineCards() {
    }

    /** Filled card surface on {@link LineTheme#SHAPE_MD}, outlined with the theme border. */
    public static GradientDrawable cardBackground(Context context) {
        return cardBackground(context, LineTheme.SURFACE_ELEVATED, LineTheme.BORDER);
    }

    /** {@link #cardBackground(Context)} with explicit fill / stroke colors. */
    public static GradientDrawable cardBackground(Context context, int fill, int stroke) {
        return LineTheme.roundedStroke(context, fill, LineTheme.SHAPE_MD, stroke);
    }

    /**
     * Container styled as an M3 outlined card: {@link LineTheme#SHAPE_MD} corners, standard
     * inner padding, vertical stacking. Add children and layout params at the call site.
     */
    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBackground(context));
        LineTheme.padding(card, LineTheme.LG, LineTheme.MD, LineTheme.LG, LineTheme.MD);
        return card;
    }

    /** Clickable variant of {@link #card(Context)} carrying an M3 state layer. */
    public static LinearLayout clickableCard(Context context) {
        LinearLayout card = card(context);
        card.setClickable(true);
        card.setFocusable(true);
        LineTheme.attachStateLayer(card);
        return card;
    }

    /** Pill background on {@link LineTheme#SHAPE_FULL}. */
    public static GradientDrawable pillBackground(Context context, int color) {
        return LineTheme.rounded(context, color, LineTheme.SHAPE_FULL);
    }

    /** Outlined pill background on {@link LineTheme#SHAPE_FULL}. */
    public static GradientDrawable pillBackground(Context context, int fill, int stroke) {
        return LineTheme.roundedStroke(context, fill, LineTheme.SHAPE_FULL, stroke);
    }

    /** Compact segment background on {@link LineTheme#SHAPE_SM} (segmented buttons, chips in a row). */
    public static GradientDrawable segmentBackground(Context context, boolean selected) {
        return selected
                ? LineTheme.roundedStroke(context, LineTheme.ACCENT_MUTED, LineTheme.SHAPE_SM, LineTheme.ACCENT)
                : LineTheme.roundedStroke(context, LineTheme.SURFACE_LIGHT, LineTheme.SHAPE_SM, LineTheme.BORDER_LIGHT);
    }

    /**
     * Styles an icon button as an M3 icon button with a visible container.
     *
     * <p>Use this instead of hand-setting a background: several palettes define
     * {@code inputBg} and {@code surfaceLight} as the <em>same</em> color, so a
     * "surface" pill drawn on the composer panel is literally invisible. Passing an
     * explicit container colour keeps the affordance readable on every palette.</p>
     */
    public static void applyIconButton(View button, int container, int iconColor) {
        if (button == null) {
            return;
        }
        button.setBackground(pillBackground(button.getContext(), container));
        button.setFocusable(true);
        LineTheme.attachStateLayer(button, iconColor);
    }

    /**
     * M3 <em>filled tonal</em> icon button: accent-tinted container with an accent icon.
     * The standard treatment for secondary actions such as attach / pick image.
     */
    public static void applyTonalIconButton(View button) {
        applyIconButton(button, LineTheme.ACCENT_MUTED, LineTheme.ACCENT);
    }

    /**
     * Outlined chip container ({@link LineTheme#SHAPE_FULL} + border), for tappable
     * inline selectors such as the model and chat-mode pickers.
     */
    public static GradientDrawable chipBackground(Context context) {
        return pillBackground(context, LineTheme.SURFACE_ELEVATED, LineTheme.BORDER_LIGHT);
    }

    /**
     * M3 filled button: accent pill, on-color label, pressed/focus/hover state layer.
     * The state layer is drawn in {@link LineTheme#TEXT_ON_COLOR} so it stays visible on
     * top of the accent fill.
     */
    public static TextView primaryButton(Context context, String label) {
        TextView button = buttonBase(context, label, LineTheme.TEXT_ON_COLOR);
        button.setBackground(pillBackground(context, LineTheme.ACCENT));
        LineTheme.attachStateLayer(button, LineTheme.TEXT_ON_COLOR);
        return button;
    }

    /** M3 outlined button: neutral pill with a border and an accent state layer. */
    public static TextView secondaryButton(Context context, String label) {
        TextView button = buttonBase(context, label, LineTheme.TEXT);
        button.setBackground(pillBackground(context, LineTheme.SURFACE_LIGHT, LineTheme.BORDER_LIGHT));
        LineTheme.attachStateLayer(button);
        return button;
    }

    /** M3 text button: no container until pressed, accent label. */
    public static TextView textButton(Context context, String label) {
        TextView button = buttonBase(context, label, LineTheme.ACCENT);
        button.setBackground(pillBackground(context, Color.TRANSPARENT));
        LineTheme.attachStateLayer(button);
        return button;
    }

    /** Destructive filled button on the danger color. */
    public static TextView dangerButton(Context context, String label) {
        TextView button = buttonBase(context, label, LineTheme.TEXT_ON_COLOR);
        button.setBackground(pillBackground(context, LineTheme.DANGER));
        LineTheme.attachStateLayer(button, LineTheme.TEXT_ON_COLOR);
        return button;
    }

    /** Card / section title on the M3 {@code title} size. */
    public static TextView title(Context context, String value) {
        return LineTheme.text(context, value, LineTheme.TYPE_TITLE, LineTheme.TEXT, Typeface.BOLD);
    }

    /** Supporting text under a {@link #title(Context, String)}, on the M3 {@code body} size. */
    public static TextView desc(Context context, String value) {
        TextView desc = LineTheme.text(context, value, LineTheme.TYPE_BODY, LineTheme.TEXT_TERTIARY, Typeface.NORMAL);
        desc.setLineSpacing(LineTheme.dp(context, 3), 1f);
        return desc;
    }

    /** Small emphasised label (badges, counters) on the M3 {@code body-small} size. */
    public static TextView badge(Context context, String value) {
        TextView badge = LineTheme.text(context, value, LineTheme.TYPE_BODY_SMALL, LineTheme.ACCENT, Typeface.BOLD);
        badge.setBackground(pillBackground(context, LineTheme.ACCENT_MUTED));
        LineTheme.padding(badge, LineTheme.SM, 3, LineTheme.SM, 3);
        return badge;
    }

    /**
     * Styles {@code view} as an M3 FAB: {@link LineTheme#SHAPE_LG} container in {@code fill},
     * level-3 elevation and a state layer. Size and placement stay with the caller.
     */
    public static void applyFab(View view, int fill) {
        if (view == null) {
            return;
        }
        Context context = view.getContext();
        view.setBackground(LineTheme.rounded(context, fill, LineTheme.SHAPE_LG));
        view.setElevation(LineTheme.dp(context, LineTheme.SM));
        LineTheme.attachStateLayer(view, LineTheme.TEXT_ON_COLOR);
    }

    private static TextView buttonBase(Context context, String label, int textColor) {
        TextView button = LineTheme.textMedium(context, label, LineTheme.TYPE_LABEL, textColor);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setSingleLine(true);
        LineTheme.padding(button, LineTheme.XL, LineTheme.SM, LineTheme.XL, LineTheme.SM);
        return button;
    }
}
