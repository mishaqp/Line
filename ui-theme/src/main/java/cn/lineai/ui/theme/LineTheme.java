package cn.lineai.ui.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.text.LineBreaker;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.View;
import android.widget.TextView;
import cn.lineai.model.ThemePalette;

public final class LineTheme {
    public static int BG = Color.parseColor("#000000");
    public static int SURFACE = Color.parseColor("#0A0A0A");
    public static int SURFACE_ELEVATED = Color.parseColor("#141414");
    public static int SURFACE_LIGHT = Color.parseColor("#1C1C1E");
    public static int ACCENT = Color.parseColor("#30D158");
    public static int ACCENT_DIM = Color.parseColor("#1A3A2A");
    public static int ACCENT_MUTED = Color.argb(26, 48, 209, 88);
    public static int ACCENT_MUTED_2 = Color.argb(38, 48, 209, 88);
    public static int USER_BUBBLE = Color.parseColor("#0A84FF");
    public static int AI_BUBBLE = Color.parseColor("#1C1C1E");
    public static int TEXT = Color.parseColor("#FFFFFF");
    public static int TEXT_SECONDARY = Color.parseColor("#8E8E93");
    public static int TEXT_TERTIARY = Color.parseColor("#636366");
    public static int TEXT_ON_COLOR = Color.parseColor("#FFFFFF");
    public static int BORDER = Color.parseColor("#1C1C1E");
    public static int BORDER_LIGHT = Color.parseColor("#2C2C2E");
    public static int INPUT_BG = Color.parseColor("#1C1C1E");
    public static int CODE_BG = Color.parseColor("#151515");
    public static int CODE_BORDER = Color.parseColor("#2C2C2E");
    public static int DANGER = Color.parseColor("#F85149");
    public static int DANGER_MUTED = Color.argb(28, 248, 81, 73);
    public static int DANGER_MUTED_2 = Color.argb(61, 248, 81, 73);
    public static int WARNING = Color.parseColor("#FF9F0A");
    public static int SUCCESS = Color.parseColor("#30D158");
    public static int OVERLAY = Color.argb(165, 0, 0, 0);
    public static int DIFF_ADD_BG = Color.argb(46, 48, 209, 88);
    public static int DIFF_DEL_BG = Color.argb(46, 255, 69, 58);
    public static int DIFF_ADD_TEXT = Color.parseColor("#30D158");
    public static int DIFF_DEL_TEXT = Color.parseColor("#FF453A");

    public static final int XS = 4;
    public static final int SM = 8;
    public static final int MD = 12;
    public static final int LG = 16;
    public static final int XL = 20;
    public static final int XXL = 24;

    public static final int FONT_XS = 11;
    public static final int FONT_SM = 13;
    public static final int FONT_MD = 15;
    public static final int FONT_LG = 17;
    public static final int FONT_XL = 20;
    public static final int FONT_TITLE = 24;
    public static final int FONT_XXL = 28;

    /** Material Design 3 shape scale (corner radii in dp). */
    public static final int SHAPE_XS = 4;
    public static final int SHAPE_SM = 8;
    public static final int SHAPE_MD = 12;
    public static final int SHAPE_LG = 16;
    public static final int SHAPE_XL = 28;
    public static final int SHAPE_FULL = 999;

    /** Material Design 3 type scale (sizes in sp). */
    public static final int TYPE_DISPLAY = 36;
    public static final int TYPE_HEADLINE = 24;
    public static final int TYPE_TITLE = 16;
    public static final int TYPE_BODY = 14;
    public static final int TYPE_BODY_SMALL = 12;
    public static final int TYPE_LABEL = 14;

    /** Material Design 3 state layer opacities. */
    public static final float STATE_LAYER_ALPHA_HOVER = 0.08f;
    public static final float STATE_LAYER_ALPHA_FOCUS = 0.10f;
    public static final float STATE_LAYER_ALPHA_PRESSED = 0.10f;
    public static final float STATE_LAYER_ALPHA_DRAGGED = 0.16f;

    private LineTheme() {
    }

    public static void apply(ThemePalette palette) {
        if (palette == null) {
            return;
        }
        BG = palette.bg;
        SURFACE = palette.surface;
        SURFACE_ELEVATED = palette.surfaceElevated;
        SURFACE_LIGHT = palette.surfaceLight;
        ACCENT = palette.accent;
        ACCENT_DIM = palette.accentDim;
        ACCENT_MUTED = palette.accentMuted;
        ACCENT_MUTED_2 = palette.accentMuted2;
        USER_BUBBLE = palette.userBubble;
        AI_BUBBLE = palette.aiBubble;
        TEXT = palette.text;
        TEXT_SECONDARY = palette.textSecondary;
        TEXT_TERTIARY = palette.textTertiary;
        TEXT_ON_COLOR = palette.textOnColor;
        BORDER = palette.border;
        BORDER_LIGHT = palette.borderLight;
        INPUT_BG = palette.inputBg;
        CODE_BG = palette.codeBg;
        CODE_BORDER = palette.codeBorder;
        DANGER = palette.danger;
        DANGER_MUTED = palette.dangerMuted;
        DANGER_MUTED_2 = palette.dangerMuted2;
        WARNING = palette.warning;
        SUCCESS = palette.success;
        OVERLAY = palette.overlay;
        DIFF_ADD_BG = palette.diffAddBg;
        DIFF_DEL_BG = palette.diffDelBg;
        DIFF_ADD_TEXT = palette.diffAddText;
        DIFF_DEL_TEXT = palette.diffDelText;
    }

    /**
     * Chat-only text multiplier, driven by {@link cn.lineai.model.ChatScale}. Applied by
     * {@link #chatSp(float)} / {@link #chatText}; global {@link #text} is deliberately
     * untouched so settings screens keep the system size.
     */
    public static float CHAT_TEXT_SCALE = 1f;

    /** Chat-only spacing multiplier; applied by {@link #chatDp(Context, float)}. */
    public static float CHAT_DENSITY_SCALE = 1f;

    /** Installs the chat scale factors; values are clamped by {@code ChatScale}. */
    public static void applyChatScale(cn.lineai.model.ChatScale scale) {
        if (scale == null) {
            CHAT_TEXT_SCALE = 1f;
            CHAT_DENSITY_SCALE = 1f;
            return;
        }
        CHAT_TEXT_SCALE = scale.getTextScale();
        CHAT_DENSITY_SCALE = scale.getDensityScale();
    }

    /** Scales an sp text size for the chat. Never returns below 1sp. */
    public static float chatSp(float sizeSp) {
        float scaled = sizeSp * CHAT_TEXT_SCALE;
        return scaled < 1f ? 1f : scaled;
    }

    /**
     * Scales a dp spacing value for the chat and converts it to pixels. A positive input
     * never collapses to zero, so hairline dividers and strokes survive the compact preset.
     */
    public static int chatDp(Context context, float value) {
        int scaled = dp(context, value * CHAT_DENSITY_SCALE);
        if (value > 0f && scaled < 1) {
            return 1;
        }
        return scaled;
    }

    /** {@link #text} with the chat text scale applied. */
    public static TextView chatText(Context context, String value, float sizeSp, int color, int style) {
        TextView textView = text(context, value, 0, color, style);
        textView.setTextSize(chatSp(sizeSp));
        return textView;
    }

    /** {@link #textMedium} with the chat text scale applied. */
    public static TextView chatTextMedium(Context context, String value, float sizeSp, int color) {
        TextView textView = textMedium(context, value, 0, color);
        textView.setTextSize(chatSp(sizeSp));
        return textView;
    }

    /** Applies chat padding (in dp, scaled) to a view. */
    public static void chatPadding(View view, int left, int top, int right, int bottom) {
        Context context = view.getContext();
        view.setPadding(chatDp(context, left), chatDp(context, top),
                chatDp(context, right), chatDp(context, bottom));
    }

    public static int dp(Context context, float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static GradientDrawable rounded(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(context, radiusDp));
        return drawable;
    }

    public static GradientDrawable roundedStroke(Context context, int color, float radiusDp, int strokeColor) {
        GradientDrawable drawable = rounded(context, color, radiusDp);
        drawable.setStroke(Math.max(1, dp(context, 1)), strokeColor);
        return drawable;
    }

    public static GradientDrawable roundedTop(Context context, int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        float radius = dp(context, radiusDp);
        drawable.setCornerRadii(new float[] {
                radius, radius,
                radius, radius,
                0, 0,
                0, 0
        });
        return drawable;
    }

    /**
     * Applies a Material Design 3 state layer alpha to {@code color}.
     * Alpha bytes round half-up like {@code Math.round} (0.10f * 255 = 25.5 -&gt; 26).
     */
    public static int withAlpha(int color, float alpha) {
        return (color & 0x00FFFFFF) | (Math.round(alpha * 255f) << 24);
    }

    /** Compositing color of an M3 state layer: {@code color} at the given layer alpha. */
    public static int stateLayerColor(int color, float alpha) {
        return withAlpha(color, alpha);
    }

    /** State layer color for the pressed state (0.10 alpha in M3). */
    public static int pressedLayerColor(int color) {
        return stateLayerColor(color, STATE_LAYER_ALPHA_PRESSED);
    }

    /**
     * Wraps the view background in a framework ripple so hover (.08), focus (.10) and
     * pressed (.10) render as M3 state layers on top of the existing background. The
     * original background is reused as ripple content and mask, so pill/rounded shapes
     * keep their outline. {@code STATE_LAYER_ALPHA_DRAGGED} is exposed for callers that
     * drive drag-and-drop affordances manually.
     */
    public static void attachStateLayer(View view) {
        attachStateLayer(view, ACCENT);
    }

    /** See {@link #attachStateLayer(View)}. */
    public static void attachStateLayer(View view, int layerColor) {
        if (view == null) {
            return;
        }
        ColorStateList stateLayers = new ColorStateList(
                new int[][] {
                        new int[] {android.R.attr.state_pressed},
                        new int[] {android.R.attr.state_focused},
                        new int[] {android.R.attr.state_hovered},
                        new int[0]
                },
                new int[] {
                        stateLayerColor(layerColor, STATE_LAYER_ALPHA_PRESSED),
                        stateLayerColor(layerColor, STATE_LAYER_ALPHA_FOCUS),
                        stateLayerColor(layerColor, STATE_LAYER_ALPHA_HOVER),
                        Color.TRANSPARENT
                });
        Drawable base = view.getBackground();
        Drawable mask;
        if (base != null && base.getConstantState() != null) {
            mask = base.getConstantState().newDrawable();
        } else {
            mask = new ColorDrawable(Color.WHITE);
        }
        view.setBackground(new RippleDrawable(stateLayers, base, mask));
    }

    /**
     * Corner radii for an asymmetric Material 3 chat bubble, in the order expected by
     * {@link GradientDrawable#setCornerRadii(float[])} (top-left, top-right, bottom-right,
     * bottom-left, each as an x/y pair).
     *
     * <p>The "tail" is the single tightened corner on the bottom edge: bottom-right for an
     * outgoing (user) bubble, bottom-left for an incoming (assistant) bubble. Pure function
     * of its arguments — it takes pixel values so it can be unit tested without a Context.</p>
     *
     * @param largePx radius applied to the three rounded corners (M3 {@code SHAPE_LG}).
     * @param smallPx radius applied to the tail corner (M3 {@code SHAPE_XS}).
     * @param tailOnEnd {@code true} for the outgoing/user side, {@code false} for incoming/AI.
     */
    public static float[] bubbleCornerRadii(float largePx, float smallPx, boolean tailOnEnd) {
        float bottomRight = tailOnEnd ? smallPx : largePx;
        float bottomLeft = tailOnEnd ? largePx : smallPx;
        return new float[] {
                largePx, largePx,
                largePx, largePx,
                bottomRight, bottomRight,
                bottomLeft, bottomLeft
        };
    }

    /** Outgoing (user) chat bubble: {@code SHAPE_LG} with a {@code SHAPE_XS} tail bottom-right. */
    public static GradientDrawable userBubble(Context context) {
        return bubble(context, USER_BUBBLE, true);
    }

    /** Incoming (assistant) chat bubble: mirror of {@link #userBubble(Context)}. */
    public static GradientDrawable assistantBubble(Context context) {
        return bubble(context, AI_BUBBLE, false);
    }

    /** Chat bubble drawable in {@code color}; see {@link #bubbleCornerRadii(float, float, boolean)}. */
    public static GradientDrawable bubble(Context context, int color, boolean tailOnEnd) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadii(bubbleCornerRadii(dp(context, SHAPE_LG), dp(context, SHAPE_XS), tailOnEnd));
        return drawable;
    }

    public static void padding(View view, int left, int top, int right, int bottom) {
        Context context = view.getContext();
        view.setPadding(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
    }

    public static TextView text(Context context, String value, int sizeSp, int color, int style) {
        TextView textView = new TextView(context);
        textView.setText(value);
        textView.setTextColor(color);
        textView.setTextSize(sizeSp);
        textView.setIncludeFontPadding(false);
        textView.setLineSpacing(dp(context, 2), 1.0f);
        if (style != Typeface.NORMAL) {
            textView.setTypeface(Typeface.DEFAULT, style);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textView.setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE);
        }
        return textView;
    }

    public static TextView textMedium(Context context, String value, int sizeSp, int color) {
        TextView textView = text(context, value, sizeSp, color, Typeface.NORMAL);
        textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return textView;
    }
}
