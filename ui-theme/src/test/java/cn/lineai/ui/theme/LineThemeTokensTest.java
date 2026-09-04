package cn.lineai.ui.theme;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LineThemeTokensTest {

    @Test
    public void shapeScaleMatchesMaterial3Tokens() {
        assertEquals(4, LineTheme.SHAPE_XS);
        assertEquals(8, LineTheme.SHAPE_SM);
        assertEquals(12, LineTheme.SHAPE_MD);
        assertEquals(16, LineTheme.SHAPE_LG);
        assertEquals(28, LineTheme.SHAPE_XL);
        assertEquals(999, LineTheme.SHAPE_FULL);
    }

    @Test
    public void typeScaleMatchesMaterial3Tokens() {
        assertEquals(36, LineTheme.TYPE_DISPLAY);
        assertEquals(24, LineTheme.TYPE_HEADLINE);
        assertEquals(16, LineTheme.TYPE_TITLE);
        assertEquals(14, LineTheme.TYPE_BODY);
        assertEquals(12, LineTheme.TYPE_BODY_SMALL);
        assertEquals(14, LineTheme.TYPE_LABEL);
    }

    @Test
    public void stateLayerAlphaTokensMatchMaterial3() {
        assertEquals(0.08f, LineTheme.STATE_LAYER_ALPHA_HOVER, 0f);
        assertEquals(0.10f, LineTheme.STATE_LAYER_ALPHA_FOCUS, 0f);
        assertEquals(0.10f, LineTheme.STATE_LAYER_ALPHA_PRESSED, 0f);
        assertEquals(0.16f, LineTheme.STATE_LAYER_ALPHA_DRAGGED, 0f);
    }

    @Test
    public void withAlphaRoundsHalfUpLikeJavaMathRound() {
        int color = 0xFF3FB950;
        // Math.round(0.10f * 255f) = 26 = 0x1A (25.5 rounds up)
        assertEquals(0x1A3FB950, LineTheme.withAlpha(color, 0.10f));
        // Math.round(0.08f * 255f) = 20 = 0x14
        assertEquals(0x143FB950, LineTheme.withAlpha(color, 0.08f));
        // Math.round(0.16f * 255f) = 41 = 0x29
        assertEquals(0x293FB950, LineTheme.withAlpha(color, 0.16f));
        assertEquals(0xFF3FB950, LineTheme.withAlpha(color, 1f));
    }

    @Test
    public void stateLayerAndPressedLayerColorsDelegate() {
        int color = 0xFF2F81F7;
        assertEquals(LineTheme.withAlpha(color, LineTheme.STATE_LAYER_ALPHA_PRESSED),
                LineTheme.stateLayerColor(color, LineTheme.STATE_LAYER_ALPHA_PRESSED));
        assertEquals(LineTheme.withAlpha(color, LineTheme.STATE_LAYER_ALPHA_PRESSED),
                LineTheme.pressedLayerColor(color));
        assertEquals(0x1A2F81F7, LineTheme.pressedLayerColor(color));
    }

    @Test
    public void withAlphaKeepsRgbChannels() {
        int result = LineTheme.withAlpha(0xFF123456, 0.5f);
        assertEquals(0x123456, result & 0xFFFFFF);
        // Math.round(127.5f) = 128 — Java rounds .5 up.
        assertEquals(128, (result >>> 24) & 0xFF);
    }
}
