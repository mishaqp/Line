package cn.lineai.ui.component;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineCards;
import cn.lineai.ui.theme.LineTheme;

/**
 * Renders the composer's {@link ComposerQueue} as a stack of compact rows inside the input
 * panel: one row per queued message (up to {@link ComposerQueue#MAX_VISIBLE_ROWS}), then a
 * single overflow line.
 *
 * <p>Extracted from {@code ComposerView}; the queue model itself stays pure so the ordering
 * and truncation rules are unit tested. Rows are M3 {@link LineTheme#SHAPE_SM} containers
 * tinted with the "queued" warning color.</p>
 */
final class ComposerPendingQueueView extends LinearLayout {

    /** Notified when the user removes a queued message so the composer can restyle send. */
    interface Listener {
        void onQueueChanged();
    }

    private final ComposerQueue queue;
    private Listener listener;

    ComposerPendingQueueView(Context context, ComposerQueue queue) {
        super(context);
        this.queue = queue;
        setOrientation(VERTICAL);
        setVisibility(GONE);
        LineTheme.padding(this, LineTheme.SM, LineTheme.XS, LineTheme.SM, 0);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Rebuilds the rows from the current queue contents. */
    void refresh() {
        removeAllViews();
        if (queue.isEmpty()) {
            setVisibility(GONE);
            return;
        }
        Context context = getContext();
        int visible = queue.visibleCount();
        for (int i = 0; i < visible; i++) {
            addView(row(context, i), new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        int overflow = queue.overflowCount();
        if (overflow > 0) {
            TextView more = LineTheme.text(context,
                    context.getString(R.string.composer_queue_overflow, overflow),
                    LineTheme.TYPE_BODY_SMALL, LineTheme.WARNING, Typeface.ITALIC);
            more.setAlpha(0.8f);
            LineTheme.padding(more, LineTheme.MD, 2, 0, LineTheme.XS);
            addView(more, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        setVisibility(VISIBLE);
    }

    private View row(Context context, int index) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(LineTheme.rounded(context,
                LineTheme.withAlpha(LineTheme.WARNING, LineTheme.STATE_LAYER_ALPHA_HOVER),
                LineTheme.SHAPE_SM));
        LineTheme.padding(row, LineTheme.SM, LineTheme.XS, LineTheme.XS, LineTheme.XS);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = LineTheme.dp(context, 2);
        row.setLayoutParams(rowParams);

        View bar = new View(context);
        bar.setBackground(LineTheme.rounded(context, LineTheme.WARNING, LineTheme.SHAPE_XS));
        row.addView(bar, new LinearLayout.LayoutParams(
                LineTheme.dp(context, 3), LineTheme.dp(context, 20)));

        TextView preview = LineTheme.text(context, queue.previewLabel(index),
                LineTheme.TYPE_BODY_SMALL, LineTheme.WARNING, Typeface.NORMAL);
        preview.setSingleLine(true);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                0, LayoutParams.WRAP_CONTENT, 1f);
        previewParams.leftMargin = LineTheme.dp(context, LineTheme.SM);
        row.addView(preview, previewParams);

        IconButtonView remove = new IconButtonView(context, IconButtonView.CLOSE);
        remove.setContentDescription(context.getString(R.string.composer_queue_remove_desc));
        remove.setIconColor(LineTheme.TEXT_TERTIARY);
        remove.setIconSizeDp(20, 12);
        remove.setBackground(LineCards.pillBackground(context, android.graphics.Color.TRANSPARENT));
        LineTheme.attachStateLayer(remove, LineTheme.WARNING);
        remove.setOnClickListener(v -> {
            if (queue.removeAt(index)) {
                refresh();
                if (listener != null) {
                    listener.onQueueChanged();
                }
            }
        });
        row.addView(remove, new LinearLayout.LayoutParams(
                LineTheme.dp(context, 20), LineTheme.dp(context, 20)));
        return row;
    }
}
