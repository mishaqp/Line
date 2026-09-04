package cn.lineai.ui.component;

import cn.lineai.model.InputAttachment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Send-queue backing the composer: messages the user typed while the model was still
 * streaming, held until the current generation finishes.
 *
 * <p>Deliberately free of any Android dependency so the ordering / truncation rules can be
 * unit tested on the JVM. {@link ComposerPendingQueueView} renders it, and
 * {@link ComposerView} drains it from {@code render(ChatUiState)} once streaming stops.</p>
 */
final class ComposerQueue {

    /** Number of queued rows rendered before the list collapses into an overflow line. */
    static final int MAX_VISIBLE_ROWS = 4;
    /** Characters of a queued message shown in its preview row before it is ellipsised. */
    static final int PREVIEW_MAX_CHARS = 30;

    /** One queued message: the text as it will be sent plus its frozen attachment list. */
    static final class Item {
        private final String text;
        private final List<InputAttachment> attachments;

        Item(String text, List<InputAttachment> attachments) {
            this.text = text == null ? "" : text;
            this.attachments = attachments == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(attachments));
        }

        String text() {
            return text;
        }

        List<InputAttachment> attachments() {
            return attachments;
        }
    }

    private final List<Item> items = new ArrayList<>();

    /** Appends a message to the tail of the queue. */
    void add(String text, List<InputAttachment> attachments) {
        items.add(new Item(text, attachments));
    }

    /** Removes the message at {@code index}; out-of-range indices are ignored. */
    boolean removeAt(int index) {
        if (index < 0 || index >= items.size()) {
            return false;
        }
        items.remove(index);
        return true;
    }

    /** Removes and returns the head of the queue, or {@code null} when empty. */
    Item poll() {
        return items.isEmpty() ? null : items.remove(0);
    }

    void clear() {
        items.clear();
    }

    int size() {
        return items.size();
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    /** The leading rows that get their own preview row (at most {@link #MAX_VISIBLE_ROWS}). */
    List<Item> visibleItems() {
        return Collections.unmodifiableList(new ArrayList<>(items.subList(0, visibleCount())));
    }

    int visibleCount() {
        return Math.min(items.size(), MAX_VISIBLE_ROWS);
    }

    /** How many queued messages are hidden behind the overflow line; 0 when everything fits. */
    int overflowCount() {
        return Math.max(0, items.size() - MAX_VISIBLE_ROWS);
    }

    /**
     * Numbered, length-capped preview of the queued message at {@code index}
     * (e.g. {@code "2. refactor the parser"}). Returns an empty string for a bad index.
     */
    String previewLabel(int index) {
        if (index < 0 || index >= items.size()) {
            return "";
        }
        return (index + 1) + ". " + truncate(items.get(index).text(), PREVIEW_MAX_CHARS);
    }

    /** Collapses whitespace runs and cuts {@code text} to {@code maxChars} plus an ellipsis. */
    static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        String flattened = text.replaceAll("\\s+", " ").trim();
        if (maxChars <= 0 || flattened.length() <= maxChars) {
            return flattened;
        }
        return flattened.substring(0, maxChars) + "...";
    }
}
