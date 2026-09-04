package cn.lineai.ui.component;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.model.InputAttachment;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Horizontal strip of pending file attachments above the composer input.
 *
 * <p>Extracted from {@code ComposerView} so attachment bookkeeping (the backing list, chip
 * rendering, per-chip removal) lives in one place. The owner is notified through
 * {@link Listener} whenever the list changes so it can refresh the send button.</p>
 *
 * <p>Chips are M3 assist chips: {@link LineTheme#SHAPE_FULL} outlined container with a
 * state layer on the remove affordance.</p>
 */
final class ComposerAttachmentStrip extends HorizontalScrollView {

    /** Notified after any mutation of the attachment list. */
    interface Listener {
        void onAttachmentsChanged();
    }

    private final LinearLayout chipRow;
    private final List<InputAttachment> attachments = new ArrayList<>();
    private Listener listener;

    ComposerAttachmentStrip(Context context) {
        super(context);
        setHorizontalScrollBarEnabled(false);
        setVisibility(GONE);
        chipRow = new LinearLayout(context);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        addView(chipRow, new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Immutable snapshot of the current attachments, in insertion order. */
    List<InputAttachment> attachments() {
        return Collections.unmodifiableList(new ArrayList<>(attachments));
    }

    boolean isEmpty() {
        return attachments.isEmpty();
    }

    /** Replaces the whole list (used when a draft is restored). */
    void replaceAll(List<InputAttachment> next) {
        attachments.clear();
        if (next != null) {
            attachments.addAll(next);
        }
        render();
    }

    void clear() {
        attachments.clear();
        render();
    }

    /** Adds {@code attachment}, or removes it when the same path/source is already present. */
    void toggle(InputAttachment attachment) {
        if (attachment == null || attachment.getPath().length() == 0) {
            return;
        }
        for (int i = 0; i < attachments.size(); i++) {
            if (attachments.get(i).matches(attachment.getPath(), attachment.getSource())) {
                attachments.remove(i);
                render();
                return;
            }
        }
        attachments.add(attachment);
        render();
    }

    /** Paths of the attachments coming from {@code source} (local or SSH). */
    List<String> pathsForSource(String source) {
        String normalized = InputAttachment.SOURCE_SSH.equals(source)
                ? InputAttachment.SOURCE_SSH
                : InputAttachment.SOURCE_LOCAL;
        List<String> paths = new ArrayList<>();
        for (InputAttachment attachment : attachments) {
            if (attachment.getSource().equals(normalized)) {
                paths.add(attachment.getPath());
            }
        }
        return paths;
    }

    private void render() {
        chipRow.removeAllViews();
        setVisibility(attachments.isEmpty() ? GONE : VISIBLE);
        for (InputAttachment attachment : attachments) {
            chipRow.addView(chip(attachment));
        }
        if (listener != null) {
            listener.onAttachmentsChanged();
        }
    }

    private View chip(InputAttachment attachment) {
        Context context = getContext();
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setBackground(LineCards.pillBackground(context, LineTheme.INPUT_BG, LineTheme.BORDER));
        LineTheme.padding(chip, LineTheme.MD, 0, LineTheme.SM, 0);

        TextView name = LineTheme.textMedium(context, attachment.getName(),
                LineTheme.TYPE_BODY, LineTheme.TEXT_SECONDARY);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        name.setMaxWidth(LineTheme.dp(context, 170));
        chip.addView(name, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        IconButtonView remove = new IconButtonView(context, IconButtonView.CLOSE);
        remove.setContentDescription(context.getString(R.string.composer_attachment_remove_desc));
        remove.setIconColor(LineTheme.TEXT_TERTIARY);
        remove.setIconSizeDp(18, 12);
        remove.setBackground(LineCards.pillBackground(context, android.graphics.Color.TRANSPARENT));
        LineTheme.attachStateLayer(remove);
        remove.setOnClickListener(v -> {
            attachments.remove(attachment);
            render();
        });
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                LineTheme.dp(context, 18), LineTheme.dp(context, 18));
        removeParams.leftMargin = LineTheme.dp(context, LineTheme.SM);
        chip.addView(remove, removeParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LineTheme.dp(context, 34));
        params.rightMargin = LineTheme.dp(context, LineTheme.SM);
        chip.setLayoutParams(params);
        return chip;
    }
}
