package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.OutputSettings;
import cn.lineai.ui.model.SecuritySettingsRepository;

/**
 * Compatibility wrapper around the Compose Security settings screen.
 */
public final class SecuritySettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onAllowAnyHttpChanged(boolean enabled);

        void onBrowserJavaScriptChanged(boolean enabled);

        void onBypassPathProtectionChanged(boolean enabled);

        void onFullAccessChanged(boolean enabled);
    }

    public SecuritySettingsScreenView(
            Context context,
            OutputSettings settings,
            boolean fullAccessEnabled,
            Listener listener
    ) {
        super(context);
        SecuritySettingsRepository repository =
                new ListenerSecuritySettingsRepository(settings, fullAccessEnabled, listener);
        addView(
                new SecuritySettingsHostView(context, repository, listener::onBack),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerSecuritySettingsRepository
            implements SecuritySettingsRepository {
        private OutputSettings snapshot;
        private boolean fullAccessEnabled;
        private final Listener listener;

        ListenerSecuritySettingsRepository(
                OutputSettings settings,
                boolean fullAccessEnabled,
                Listener listener
        ) {
            this.snapshot = settings == null
                    ? new OutputSettings(false, OutputSettings.BROWSER_BUILTIN)
                    : settings;
            this.fullAccessEnabled = fullAccessEnabled;
            this.listener = listener;
        }

        @Override
        public OutputSettings outputSettings() {
            return snapshot;
        }

        @Override
        public boolean fullAccessEnabled() {
            return fullAccessEnabled;
        }

        @Override
        public void setAllowAnyHttp(boolean enabled) {
            snapshot = copy(
                    snapshot.isCodeWrapEnabled(),
                    snapshot.getBrowserMode(),
                    snapshot.isBrowserJavaScriptEnabled(),
                    enabled,
                    snapshot.isBypassPathProtection()
            );
            listener.onAllowAnyHttpChanged(enabled);
        }

        @Override
        public void setBrowserJavaScriptEnabled(boolean enabled) {
            snapshot = copy(
                    snapshot.isCodeWrapEnabled(),
                    snapshot.getBrowserMode(),
                    enabled,
                    snapshot.isAllowAnyHttp(),
                    snapshot.isBypassPathProtection()
            );
            listener.onBrowserJavaScriptChanged(enabled);
        }

        @Override
        public void setBypassPathProtection(boolean enabled) {
            snapshot = copy(
                    snapshot.isCodeWrapEnabled(),
                    snapshot.getBrowserMode(),
                    snapshot.isBrowserJavaScriptEnabled(),
                    snapshot.isAllowAnyHttp(),
                    enabled
            );
            listener.onBypassPathProtectionChanged(enabled);
        }

        @Override
        public void setFullAccessEnabled(boolean enabled) {
            fullAccessEnabled = enabled;
            listener.onFullAccessChanged(enabled);
        }

        private OutputSettings copy(
                boolean codeWrap,
                String browserMode,
                boolean browserJavaScript,
                boolean allowAnyHttp,
                boolean bypassPathProtection
        ) {
            return new OutputSettings(
                    codeWrap,
                    browserMode,
                    browserJavaScript,
                    allowAnyHttp,
                    bypassPathProtection
            );
        }
    }
}
