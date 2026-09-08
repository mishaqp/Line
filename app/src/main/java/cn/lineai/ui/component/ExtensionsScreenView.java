package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.navigation.LineDestination;

public final class ExtensionsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onOpen(String id);
    }

    public ExtensionsScreenView(Context context, Listener listener) {
        super(context);
        ExtensionsHostView hostView = new ExtensionsHostView(
                context,
                new ExtensionsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        listener.onOpen(legacyOpenId(destination));
                    }
                }
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    static String legacyOpenId(LineDestination destination) {
        if (destination instanceof LineDestination.Extension) {
            return ((LineDestination.Extension) destination).getKind();
        }
        if (destination instanceof LineDestination.TerminalProvider) {
            return destination.getScreenId();
        }
        throw new IllegalArgumentException("Unsupported Extensions destination: " + destination);
    }
}
