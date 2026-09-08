package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.navigation.LineDestination;

public final class AboutScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onOpenGithub();

        void onOpenLicenses();
    }

    public AboutScreenView(Context context, Listener listener) {
        super(context);
        addView(
                new AboutHostView(context, new AboutHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpenGithub() {
                        listener.onOpenGithub();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination != null) {
                            listener.onOpenLicenses();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }
}
