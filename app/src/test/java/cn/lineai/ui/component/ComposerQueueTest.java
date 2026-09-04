package cn.lineai.ui.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import cn.lineai.model.InputAttachment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * Pure-JVM tests for the composer send queue. {@link ComposerQueue} carries no Android
 * dependency precisely so this ordering / truncation logic can be verified without
 * Robolectric.
 */
public class ComposerQueueTest {

    private static InputAttachment attachment(String name) {
        return new InputAttachment(name, "/proj/" + name, InputAttachment.SOURCE_LOCAL);
    }

    @Test
    public void newQueueIsEmpty() {
        ComposerQueue queue = new ComposerQueue();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertEquals(0, queue.visibleCount());
        assertEquals(0, queue.overflowCount());
        assertNull(queue.poll());
    }

    @Test
    public void pollReturnsMessagesInFifoOrder() {
        ComposerQueue queue = new ComposerQueue();
        queue.add("first", null);
        queue.add("second", null);
        queue.add("third", null);

        assertEquals(3, queue.size());
        assertEquals("first", queue.poll().text());
        assertEquals("second", queue.poll().text());
        assertEquals("third", queue.poll().text());
        assertTrue(queue.isEmpty());
    }

    @Test
    public void itemKeepsDefensiveCopyOfAttachments() {
        ComposerQueue queue = new ComposerQueue();
        List<InputAttachment> live = new ArrayList<>(Arrays.asList(attachment("a.java")));
        queue.add("build it", live);
        live.clear();

        ComposerQueue.Item item = queue.poll();
        assertEquals(1, item.attachments().size());
        assertEquals("a.java", item.attachments().get(0).getName());
    }

    @Test
    public void nullTextAndAttachmentsBecomeEmpty() {
        ComposerQueue queue = new ComposerQueue();
        queue.add(null, null);
        ComposerQueue.Item item = queue.poll();
        assertEquals("", item.text());
        assertTrue(item.attachments().isEmpty());
    }

    @Test
    public void removeAtDropsTheRightMessageAndIgnoresBadIndices() {
        ComposerQueue queue = new ComposerQueue();
        queue.add("one", null);
        queue.add("two", null);
        queue.add("three", null);

        assertFalse(queue.removeAt(-1));
        assertFalse(queue.removeAt(3));
        assertEquals(3, queue.size());

        assertTrue(queue.removeAt(1));
        assertEquals(2, queue.size());
        assertEquals("one", queue.poll().text());
        assertEquals("three", queue.poll().text());
    }

    @Test
    public void visibleCountCapsAtMaxVisibleRows() {
        ComposerQueue queue = new ComposerQueue();
        for (int i = 0; i < ComposerQueue.MAX_VISIBLE_ROWS + 3; i++) {
            queue.add("msg " + i, null);
        }
        assertEquals(ComposerQueue.MAX_VISIBLE_ROWS, queue.visibleCount());
        assertEquals(ComposerQueue.MAX_VISIBLE_ROWS, queue.visibleItems().size());
        assertEquals(3, queue.overflowCount());
    }

    @Test
    public void overflowIsZeroWhileEverythingFits() {
        ComposerQueue queue = new ComposerQueue();
        for (int i = 0; i < ComposerQueue.MAX_VISIBLE_ROWS; i++) {
            queue.add("msg " + i, null);
        }
        assertEquals(0, queue.overflowCount());
        assertEquals(ComposerQueue.MAX_VISIBLE_ROWS, queue.visibleCount());
    }

    @Test
    public void previewLabelIsOneBasedAndTruncated() {
        ComposerQueue queue = new ComposerQueue();
        queue.add("short", null);
        // 40 characters -> cut to PREVIEW_MAX_CHARS (30) plus the ellipsis.
        queue.add("0123456789012345678901234567890123456789", null);

        assertEquals("1. short", queue.previewLabel(0));
        assertEquals("2. 012345678901234567890123456789...", queue.previewLabel(1));
        assertEquals("", queue.previewLabel(2));
        assertEquals("", queue.previewLabel(-1));
    }

    @Test
    public void truncateFlattensNewlinesAndTrims() {
        assertEquals("a b", ComposerQueue.truncate("  a\nb  ", 30));
        assertEquals("a b", ComposerQueue.truncate("a\r\nb", 30));
        assertEquals("", ComposerQueue.truncate(null, 30));
        assertEquals("abc", ComposerQueue.truncate("abc", 3));
        assertEquals("ab...", ComposerQueue.truncate("abc", 2));
    }

    @Test
    public void clearEmptiesTheQueue() {
        ComposerQueue queue = new ComposerQueue();
        queue.add("one", null);
        queue.add("two", null);
        queue.clear();
        assertTrue(queue.isEmpty());
        assertNull(queue.poll());
    }
}
