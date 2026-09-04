package cn.lineai.ui.theme;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Tests the pure part of the Material 3 chat bubble geometry.
 *
 * <p>{@link LineTheme#bubbleCornerRadii(float, float, boolean)} takes pixel values instead
 * of a {@code Context} exactly so the corner ordering can be asserted on the JVM without
 * Robolectric.</p>
 */
public class LineThemeBubbleShapeTest {

    private static final float EPS = 0f;

    @Test
    public void userBubbleTightensTheBottomEndCorner() {
        float[] radii = LineTheme.bubbleCornerRadii(16f, 4f, true);
        // order: TL, TL, TR, TR, BR, BR, BL, BL
        assertArrayEquals(new float[] {16f, 16f, 16f, 16f, 4f, 4f, 16f, 16f}, radii, EPS);
    }

    @Test
    public void assistantBubbleMirrorsTheUserBubble() {
        float[] radii = LineTheme.bubbleCornerRadii(16f, 4f, false);
        assertArrayEquals(new float[] {16f, 16f, 16f, 16f, 16f, 16f, 4f, 4f}, radii, EPS);
    }

    @Test
    public void bothSidesAreMirrorImagesOfEachOther() {
        float[] user = LineTheme.bubbleCornerRadii(16f, 4f, true);
        float[] assistant = LineTheme.bubbleCornerRadii(16f, 4f, false);
        // Top corners identical, bottom corners swapped.
        for (int i = 0; i < 4; i++) {
            assertEquals(user[i], assistant[i], EPS);
        }
        assertEquals(user[4], assistant[6], EPS);
        assertEquals(user[5], assistant[7], EPS);
        assertEquals(user[6], assistant[4], EPS);
        assertEquals(user[7], assistant[5], EPS);
    }

    @Test
    public void alwaysReturnsEightRadiiForSetCornerRadii() {
        assertEquals(8, LineTheme.bubbleCornerRadii(1f, 2f, true).length);
        assertEquals(8, LineTheme.bubbleCornerRadii(0f, 0f, false).length);
    }

    @Test
    public void bubbleGeometryUsesTheMaterial3ShapeScale() {
        // The Context-based helpers feed SHAPE_LG / SHAPE_XS through dp() into these radii.
        assertEquals(16, LineTheme.SHAPE_LG);
        assertEquals(4, LineTheme.SHAPE_XS);
        float[] radii = LineTheme.bubbleCornerRadii(LineTheme.SHAPE_LG, LineTheme.SHAPE_XS, true);
        assertEquals(LineTheme.SHAPE_LG, radii[0], EPS);
        assertEquals(LineTheme.SHAPE_XS, radii[4], EPS);
    }
}
