package cn.lineai.model;

/**
 * Density / text scale applied to the chat screen only.
 *
 * <p>The rest of the app keeps the system density: this is a deliberate scope choice, so a
 * user who finds the conversation "too big and heavy" can tighten it without shrinking
 * settings screens and dialogs where the extra room aids legibility.</p>
 *
 * <p>Two independent factors are exposed because they do not scale at the same rate: text
 * has a comfortable floor below which it stops being readable, while padding can shrink
 * further without harm. Presets keep the pair in a combination that was chosen to stay
 * balanced.</p>
 *
 * <p>Pure data + pure functions, no Android types, so the arithmetic is unit tested on the
 * JVM.</p>
 */
public final class ChatScale {

    /** The densest preset: one step below compact, for fitting the most on screen. */
    public static final String MODE_ULTRA_COMPACT = "ultra_compact";
    /** Tighter padding and slightly smaller text — more messages on screen. */
    public static final String MODE_COMPACT = "compact";
    /** Untouched: matches the system density exactly. */
    public static final String MODE_NORMAL = "normal";
    /** Roomier padding and larger text. */
    public static final String MODE_LARGE = "large";

    /** All selectable modes, in the order the settings screen lists them. */
    public static final String[] MODES = {MODE_ULTRA_COMPACT, MODE_COMPACT, MODE_NORMAL, MODE_LARGE};

    private static final float ULTRA_COMPACT_TEXT = 0.80f;
    private static final float ULTRA_COMPACT_DENSITY = 0.68f;
    private static final float COMPACT_TEXT = 0.88f;
    private static final float COMPACT_DENSITY = 0.82f;
    private static final float LARGE_TEXT = 1.15f;
    private static final float LARGE_DENSITY = 1.12f;

    /** Lower bound for any scale factor; below this the UI starts to break. */
    public static final float MIN_SCALE = 0.65f;
    /** Upper bound for any scale factor. */
    public static final float MAX_SCALE = 1.30f;

    private final String mode;
    private final float textScale;
    private final float densityScale;

    private ChatScale(String mode, float textScale, float densityScale) {
        this.mode = mode;
        this.textScale = clamp(textScale);
        this.densityScale = clamp(densityScale);
    }

    /** Resolves a stored mode string (unknown / null values fall back to normal). */
    public static ChatScale forMode(String rawMode) {
        String normalized = normalizeMode(rawMode);
        if (MODE_ULTRA_COMPACT.equals(normalized)) {
            return new ChatScale(normalized, ULTRA_COMPACT_TEXT, ULTRA_COMPACT_DENSITY);
        }
        if (MODE_COMPACT.equals(normalized)) {
            return new ChatScale(normalized, COMPACT_TEXT, COMPACT_DENSITY);
        }
        if (MODE_LARGE.equals(normalized)) {
            return new ChatScale(normalized, LARGE_TEXT, LARGE_DENSITY);
        }
        return new ChatScale(MODE_NORMAL, 1f, 1f);
    }

    /** Maps any input onto one of {@link #MODES}, defaulting to {@link #MODE_NORMAL}. */
    public static String normalizeMode(String rawMode) {
        if (rawMode == null) {
            return MODE_NORMAL;
        }
        String trimmed = rawMode.trim().toLowerCase(java.util.Locale.ROOT);
        for (String candidate : MODES) {
            if (candidate.equals(trimmed)) {
                return candidate;
            }
        }
        return MODE_NORMAL;
    }

    /** Keeps a factor inside [{@link #MIN_SCALE}, {@link #MAX_SCALE}]. */
    public static float clamp(float scale) {
        if (Float.isNaN(scale)) {
            return 1f;
        }
        if (scale < MIN_SCALE) {
            return MIN_SCALE;
        }
        return scale > MAX_SCALE ? MAX_SCALE : scale;
    }

    public String getMode() {
        return mode;
    }

    /** Multiplier for sp text sizes inside the chat. */
    public float getTextScale() {
        return textScale;
    }

    /** Multiplier for dp spacing / sizing inside the chat. */
    public float getDensityScale() {
        return densityScale;
    }

    /** {@code true} when nothing should be scaled at all. */
    public boolean isDefault() {
        return MODE_NORMAL.equals(mode);
    }
}
