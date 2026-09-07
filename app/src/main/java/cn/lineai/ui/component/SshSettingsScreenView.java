package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.SshConfig;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.model.SshSettingsRepository;

/**
 * Compatibility wrapper around the Compose SSH connection screen.
 */
public final class SshSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
        void onOpenTermuxIntegration();
        SshConfig onLoadConfig();
        void onSaveConfig(SshConfig config);
        String onTestConnection(SshConfig config) throws Exception;
    }

    public SshSettingsScreenView(Context context, Listener listener) {
        super(context);
        SshSettingsRepository repository = new SshSettingsControllerRepository(
                new ListenerGateway(listener)
        );
        addView(
                new SshSettingsHostView(context, repository, new SshSettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination instanceof LineDestination.TermuxIntegration) {
                            listener.onOpenTermuxIntegration();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerGateway implements SshSettingsLegacyGateway {
        private final Listener listener;
        private SshConfig snapshot;

        ListenerGateway(Listener listener) {
            this.listener = listener;
            SshConfig loaded = listener.onLoadConfig();
            this.snapshot = loaded == null ? SshConfig.defaultConfig() : loaded;
        }

        @Override
        public SshConfig loadConfig() {
            return snapshot;
        }

        @Override
        public void saveConfig(SshConfig config) {
            SshConfig next = config == null ? SshConfig.defaultConfig() : config;
            listener.onSaveConfig(next);
            snapshot = next;
        }

        @Override
        public String testConnection(SshConfig config) throws Exception {
            return listener.onTestConnection(config);
        }
    }
}
