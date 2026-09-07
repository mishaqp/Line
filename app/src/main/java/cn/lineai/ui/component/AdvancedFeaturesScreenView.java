package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.navigation.LineDestination;

public final class AdvancedFeaturesScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onOpen(String id);
    }

    public AdvancedFeaturesScreenView(Context context, Listener listener) {
        super(context);
        AdvancedFeaturesHostView hostView = new AdvancedFeaturesHostView(
                context,
                new AdvancedFeaturesHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        listener.onOpen(destination.getScreenId());
                    }
                }
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }
}
