package cn.lineai.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ChatScaleTest {

    private static final float EPS = 0.0001f;

    @Test
    public void normalIsTheNeutralPreset() {
        ChatScale scale = ChatScale.forMode(ChatScale.MODE_NORMAL);
        assertEquals(ChatScale.MODE_NORMAL, scale.getMode());
        assertEquals(1f, scale.getTextScale(), EPS);
        assertEquals(1f, scale.getDensityScale(), EPS);
        assertTrue(scale.isDefault());
    }

    @Test
    public void compactShrinksDensityMoreThanText() {
        ChatScale scale = ChatScale.forMode(ChatScale.MODE_COMPACT);
        assertEquals(ChatScale.MODE_COMPACT, scale.getMode());
        assertTrue(scale.getTextScale() < 1f);
        assertTrue(scale.getDensityScale() < 1f);
        // Padding tolerates more shrinking than glyphs do, so it must lead.
        assertTrue(scale.getDensityScale() < scale.getTextScale());
        assertFalse(scale.isDefault());
    }

    @Test
    public void largeGrowsBothFactors() {
        ChatScale scale = ChatScale.forMode(ChatScale.MODE_LARGE);
        assertEquals(ChatScale.MODE_LARGE, scale.getMode());
        assertTrue(scale.getTextScale() > 1f);
        assertTrue(scale.getDensityScale() > 1f);
        assertFalse(scale.isDefault());
    }

    @Test
    public void everyPresetStaysInsideTheClampWindow() {
        for (String mode : ChatScale.MODES) {
            ChatScale scale = ChatScale.forMode(mode);
            assertTrue(mode, scale.getTextScale() >= ChatScale.MIN_SCALE);
            assertTrue(mode, scale.getTextScale() <= ChatScale.MAX_SCALE);
            assertTrue(mode, scale.getDensityScale() >= ChatScale.MIN_SCALE);
            assertTrue(mode, scale.getDensityScale() <= ChatScale.MAX_SCALE);
        }
    }

    @Test
    public void modesArrayIsTheThreePresetsInOrder() {
        assertEquals(3, ChatScale.MODES.length);
        assertEquals(ChatScale.MODE_COMPACT, ChatScale.MODES[0]);
        assertEquals(ChatScale.MODE_NORMAL, ChatScale.MODES[1]);
        assertEquals(ChatScale.MODE_LARGE, ChatScale.MODES[2]);
    }

    @Test
    public void unknownAndNullModesFallBackToNormal() {
        assertEquals(ChatScale.MODE_NORMAL, ChatScale.normalizeMode(null));
        assertEquals(ChatScale.MODE_NORMAL, ChatScale.normalizeMode(""));
        assertEquals(ChatScale.MODE_NORMAL, ChatScale.normalizeMode("   "));
        assertEquals(ChatScale.MODE_NORMAL, ChatScale.normalizeMode("gigantic"));
        assertEquals(ChatScale.MODE_NORMAL, ChatScale.normalizeMode("{}"));
        assertTrue(ChatScale.forMode("gigantic").isDefault());
        assertTrue(ChatScale.forMode(null).isDefault());
    }

    @Test
    public void storedModesAreReadCaseAndWhitespaceInsensitively() {
        assertEquals(ChatScale.MODE_COMPACT, ChatScale.normalizeMode("COMPACT"));
        assertEquals(ChatScale.MODE_COMPACT, ChatScale.normalizeMode("  Compact  "));
        assertEquals(ChatScale.MODE_LARGE, ChatScale.normalizeMode("\tLarge\n"));
        assertEquals(ChatScale.MODE_LARGE, ChatScale.forMode("LARGE").getMode());
    }

    @Test
    public void clampBoundsAnyFactor() {
        assertEquals(ChatScale.MIN_SCALE, ChatScale.clamp(0f), EPS);
        assertEquals(ChatScale.MIN_SCALE, ChatScale.clamp(-4f), EPS);
        assertEquals(ChatScale.MAX_SCALE, ChatScale.clamp(9f), EPS);
        assertEquals(ChatScale.MAX_SCALE, ChatScale.clamp(Float.POSITIVE_INFINITY), EPS);
        assertEquals(ChatScale.MIN_SCALE, ChatScale.clamp(Float.NEGATIVE_INFINITY), EPS);
        assertEquals(1f, ChatScale.clamp(1f), EPS);
        assertEquals(ChatScale.MIN_SCALE, ChatScale.clamp(ChatScale.MIN_SCALE), EPS);
        assertEquals(ChatScale.MAX_SCALE, ChatScale.clamp(ChatScale.MAX_SCALE), EPS);
    }

    @Test
    public void clampTreatsNaNAsNeutral() {
        assertEquals(1f, ChatScale.clamp(Float.NaN), EPS);
    }

    @Test
    public void resolvingIsStableAcrossRoundTrips() {
        for (String mode : ChatScale.MODES) {
            ChatScale first = ChatScale.forMode(mode);
            ChatScale second = ChatScale.forMode(first.getMode());
            assertNotNull(second);
            assertEquals(first.getMode(), second.getMode());
            assertEquals(first.getTextScale(), second.getTextScale(), EPS);
            assertEquals(first.getDensityScale(), second.getDensityScale(), EPS);
        }
    }

    @Test
    public void presetsAreDistinctFromEachOther() {
        float compact = ChatScale.forMode(ChatScale.MODE_COMPACT).getTextScale();
        float normal = ChatScale.forMode(ChatScale.MODE_NORMAL).getTextScale();
        float large = ChatScale.forMode(ChatScale.MODE_LARGE).getTextScale();
        assertTrue(compact < normal);
        assertTrue(normal < large);
    }
}
