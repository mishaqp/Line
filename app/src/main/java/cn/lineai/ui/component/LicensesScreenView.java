package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

public final class LicensesScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
    }

    public LicensesScreenView(Context context, Listener listener) {
        super(context);
        LicensesHostView hostView = new LicensesHostView(context, new LicensesHostView.Listener() {
            @Override
            public void onBack() {
                listener.onBack();
            }
        });
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }
}
