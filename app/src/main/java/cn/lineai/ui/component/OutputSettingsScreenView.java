package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.OutputSettings;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.model.OutputSettingsRepository;

/**
 * Compatibility wrapper around the Compose Output settings screen.
 * Tool Call Preview stays on the ScreenRegistry / typed destination bridge.
 */
public final class OutputSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onCodeWrapChanged(boolean enabled);

        void onBrowserModeChanged(String mode);

        void onBrowserJavaScriptChanged(boolean enabled);

        void onToolCallPreviewClicked();
    }

    public OutputSettingsScreenView(Context context, OutputSettings settings, Listener listener) {
        super(context);
        OutputSettingsRepository repository = new ListenerOutputSettingsRepository(settings, listener);
        addView(
                new OutputSettingsHostView(context, repository, new OutputSettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination instanceof LineDestination.ToolCallPreview) {
                            listener.onToolCallPreviewClicked();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerOutputSettingsRepository implements OutputSettingsRepository {
        private OutputSettings snapshot;
        private final Listener listener;

        ListenerOutputSettingsRepository(OutputSettings settings, Listener listener) {
            this.snapshot = settings == null
                    ? new OutputSettings(false, OutputSettings.BROWSER_BUILTIN)
                    : settings;
            this.listener = listener;
        }

        @Override
        public OutputSettings settings() {
            return snapshot;
        }

        @Override
        public void setCodeWrapEnabled(boolean enabled) {
            snapshot = copy(
                    enabled,
                    snapshot.getBrowserMode(),
                    snapshot.isBrowserJavaScriptEnabled()
            );
            listener.onCodeWrapChanged(enabled);
        }

        @Override
        public void setBrowserMode(String mode) {
            snapshot = copy(
                    snapshot.isCodeWrapEnabled(),
                    mode,
                    snapshot.isBrowserJavaScriptEnabled()
            );
            listener.onBrowserModeChanged(snapshot.getBrowserMode());
        }

        @Override
        public void setBrowserJavaScriptEnabled(boolean enabled) {
            snapshot = copy(
                    snapshot.isCodeWrapEnabled(),
                    snapshot.getBrowserMode(),
                    enabled
            );
            listener.onBrowserJavaScriptChanged(enabled);
        }

        private OutputSettings copy(boolean codeWrap, String mode, boolean javaScript) {
            return new OutputSettings(
                    codeWrap,
                    mode,
                    javaScript,
                    snapshot.isAllowAnyHttp(),
                    snapshot.isBypassPathProtection()
            );
        }
    }
}
