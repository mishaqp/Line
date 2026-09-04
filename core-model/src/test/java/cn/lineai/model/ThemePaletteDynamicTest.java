package cn.lineai.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.Test;

public class ThemePaletteDynamicTest {

    private static final int SEED = 0xFF48B95C;

    @Test
    public void normalizeModeAcceptsDynamicColor() {
        assertEquals("dynamicColor", ThemePalette.MODE_DYNAMIC_COLOR);
        assertEquals(ThemePalette.MODE_DYNAMIC_COLOR, ThemePalette.normalizeMode(ThemePalette.MODE_DYNAMIC_COLOR));
        assertEquals(ThemePalette.MODE_DYNAMIC_COLOR, ThemePalette.normalizeMode(" dynamicColor "));
    }

    @Test
    public void normalizeModeRejectsUnknownDynamicAliases() {
        assertEquals(ThemePalette.MODE_SYSTEM, ThemePalette.normalizeMode("dynamic-color"));
        assertEquals(ThemePalette.MODE_SYSTEM, ThemePalette.normalizeMode("DynamicColor"));
        assertEquals(ThemePalette.MODE_SYSTEM, ThemePalette.normalizeMode(null));
        assertEquals(ThemePalette.MODE_SYSTEM, ThemePalette.normalizeMode(""));
    }

    @Test
    public void forModeDynamicColorFallsBackToDarkWithoutSystemSeed() {
        assertEquals(
                ThemePalette.forMode(ThemePalette.MODE_DARK).editableHexMap(),
                ThemePalette.forMode(ThemePalette.MODE_DYNAMIC_COLOR).editableHexMap());
    }

    @Test
    public void zeroSeedFallsBackToDarkScheme() {
        assertEquals(
                ThemePalette.forMode(ThemePalette.MODE_DARK).editableHexMap(),
                ThemePalette.dynamic(0, true).editableHexMap());
        assertEquals(
                ThemePalette.forMode(ThemePalette.MODE_DARK).editableHexMap(),
                ThemePalette.dynamic(0, false).editableHexMap());
    }

    @Test
    public void dynamicPalettePreservesSeedHue() {
        ThemePalette red = ThemePalette.dynamic(0xFFFF0000, true);
        int redAccent = red.accent;
        assertTrue("red seed keeps red dominant",
                ((redAccent >> 16) & 0xFF) > ((redAccent >> 8) & 0xFF)
                        && ((redAccent >> 16) & 0xFF) > (redAccent & 0xFF));

        ThemePalette blue = ThemePalette.dynamic(0xFF0000FF, true);
        int blueAccent = blue.accent;
        assertTrue("blue seed keeps blue dominant",
                (blueAccent & 0xFF) > ((blueAccent >> 16) & 0xFF)
                        && (blueAccent & 0xFF) > ((blueAccent >> 8) & 0xFF));
    }

    @Test
    public void dynamicDarkAndLightSchemesDiffer() {
        ThemePalette dark = ThemePalette.dynamic(SEED, true);
        ThemePalette light = ThemePalette.dynamic(SEED, false);
        assertNotEquals(dark.bg, light.bg);
        assertNotEquals(dark.surfaceLight, light.surfaceLight);
        assertNotEquals(dark.accent, light.accent);
    }

    @Test
    public void dynamicAccentsAreOpaque() {
        String[] keys = {ThemePalette.KEY_ACCENT, ThemePalette.KEY_USER_BUBBLE, ThemePalette.KEY_BG};
        for (boolean darkScheme : new boolean[] {true, false}) {
            ThemePalette palette = ThemePalette.dynamic(SEED, darkScheme);
            for (String key : keys) {
                assertEquals(key + " must stay opaque", 0xFF, (palette.colorForKey(key) >>> 24) & 0xFF);
            }
        }
    }

    @Test
    public void dynamicMutedAccentsUseStateLayerAlphas() {
        ThemePalette palette = ThemePalette.dynamic(SEED, true);
        // Math.round(0.10f * 255f) = Math.round(25.5f) = 26 (Java rounds .5 up)
        assertEquals(26, (palette.accentMuted >>> 24) & 0xFF);
        // Math.round(0.15f * 255f) = Math.round(38.25f) = 38
        assertEquals(38, (palette.accentMuted2 >>> 24) & 0xFF);
    }

    @Test
    public void dynamicEditableHexMapRoundTrips() {
        ThemePalette palette = ThemePalette.dynamic(SEED, false);
        Map<String, String> map = palette.editableHexMap();
        assertEquals(ThemePalette.EDITABLE_KEYS.length, map.size());
        for (String key : ThemePalette.EDITABLE_KEYS) {
            String value = map.get(key);
            assertTrue(key + " value must be hex", ThemePalette.isHexColor(value));
            assertEquals(key + " must round-trip",
                    palette.colorForKey(key) | 0xFF000000, ThemePalette.parseHex(value, 0));
        }
    }
}
