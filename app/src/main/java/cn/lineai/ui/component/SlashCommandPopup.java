package cn.lineai.ui.component;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;
import cn.lineai.ui.model.SlashCommandPopupSnapshot;
import cn.lineai.ui.model.SlashCommandRowData;
import cn.lineai.ui.theme.LineTheme;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Legacy composer popup boundary. Rendering/state live in Foundation v2. */
public final class SlashCommandPopup {
    public static final class Row {
        public final String label;
        public final String description;
        public final Runnable onClick;

        public Row(
                String label,
                String description,
                Runnable onClick
        ) {
            this.label = label == null ? "" : label;
            this.description =
                    description == null ? "" : description;
            this.onClick = onClick;
        }
    }

    private final Context context;
    private final PopupWindow popup;
    private final SlashCommandPopupHostView hostView;
    private List<Row> callbacks = Collections.emptyList();
    private int selectedIndex = -1;

    public SlashCommandPopup(Context context) {
        this.context = context;
        SlashCommandPopupRepository repository =
                new SlashCommandPopupRepository();
        hostView = new SlashCommandPopupHostView(
                context,
                repository,
                this::onRowSelected
        );
        popup = new PopupWindow(context);
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );
        popup.setFocusable(false);
        popup.setContentView(hostView);
    }

    public void show(String title, List<Row> rows) {
        if (rows == null || rows.isEmpty()) {
            dismiss();
            return;
        }
        callbacks = Collections.unmodifiableList(
                new ArrayList<>(rows)
        );
        List<SlashCommandRowData> data =
                new ArrayList<>(rows.size());
        for (Row row : rows) {
            Row safe = row == null
                    ? new Row("", "", null)
                    : row;
            data.add(
                    new SlashCommandRowData(
                            safe.label,
                            safe.description
                    )
            );
        }
        hostView.bind(title, data, selectedIndex);
    }

    public void showAtAnchor(View anchor) {
        if (
            anchor == null ||
            anchor.getWidth() == 0 ||
            anchor.getHeight() == 0 ||
            !hostView.hasRows()
        ) {
            return;
        }
        int popupWidth = anchor.getWidth() -
                2 * LineTheme.dp(context, LineTheme.LG);
        if (popupWidth <= 0) return;

        hostView.measure(
                View.MeasureSpec.makeMeasureSpec(
                        popupWidth,
                        View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(
                        0,
                        View.MeasureSpec.UNSPECIFIED
                )
        );
        int popupHeight = hostView.getMeasuredHeight();
        if (popupHeight <= 0) return;

        popup.setWidth(popupWidth);
        popup.setHeight(popupHeight);
        if (popup.isShowing()) {
            popup.update(popupWidth, popupHeight);
            return;
        }

        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int x = location[0] +
                LineTheme.dp(context, LineTheme.LG);
        int y = Math.max(
                0,
                location[1] - popupHeight -
                        LineTheme.dp(context, 8)
        );
        popup.showAtLocation(
                anchor,
                Gravity.NO_GRAVITY,
                x,
                y
        );
    }

    public void dismiss() {
        if (popup.isShowing()) popup.dismiss();
        callbacks = Collections.emptyList();
        hostView.clear();
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        hostView.setSelectedIndex(index);
    }

    private void onRowSelected(int index) {
        if (index < 0 || index >= callbacks.size()) return;
        Runnable action = callbacks.get(index).onClick;
        dismiss();
        if (action != null) {
            new Handler(Looper.getMainLooper()).post(action);
        }
    }

    static SlashCommandPopupSnapshot snapshotOf(
            String title,
            List<Row> rows,
            int selectedIndex
    ) {
        SlashCommandPopupRepository repository =
                new SlashCommandPopupRepository();
        List<SlashCommandRowData> data = new ArrayList<>();
        if (rows != null) {
            for (Row row : rows) {
                Row safe = row == null
                        ? new Row("", "", null)
                        : row;
                data.add(
                        new SlashCommandRowData(
                                safe.label,
                                safe.description
                        )
                );
            }
        }
        repository.bind(title, data, selectedIndex);
        return repository.snapshot();
    }
}
