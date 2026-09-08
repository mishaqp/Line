package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.navigation.LineDestination;

/**
 * Compatibility wrapper around the Compose Settings hub.
 * Child settings screens remain on the ScreenRegistry / typed destination bridge.
 */
public final class SettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onItem(String id);
    }

    public SettingsScreenView(Context context, Listener listener) {
        super(context);
        addView(
                new SettingsHostView(context, new SettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination != null) {
                            listener.onItem(destination.getScreenId());
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }
}
