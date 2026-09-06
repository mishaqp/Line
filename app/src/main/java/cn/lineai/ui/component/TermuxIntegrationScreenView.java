package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.R;
import cn.lineai.model.SshConfig;
import cn.lineai.ssh.TermuxHelper;
import cn.lineai.ui.model.TermuxIntegrationRepository;

/**
 * Compatibility wrapper around the Compose Termux integration screen.
 */
public final class TermuxIntegrationScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
        void onOpenTermux() throws Exception;
        TermuxHelper.TermuxSetupResult onSetupTermuxSsh(int timeoutMs) throws Exception;
        String onTestConnection(SshConfig config) throws Exception;
    }

    public TermuxIntegrationScreenView(Context context, Listener listener) {
        super(context);
        TermuxIntegrationRepository repository = new TermuxIntegrationControllerRepository(
                new ListenerGateway(listener),
                TermuxHelper.TERMUX_ALLOW_EXTERNAL_APPS_COMMAND,
                context.getString(R.string.screen_termux_redact_replacement)
        );
        addView(
                new TermuxIntegrationHostView(context, repository, new TermuxIntegrationHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpenTermux() throws Exception {
                        listener.onOpenTermux();
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerGateway implements TermuxIntegrationLegacyGateway {
        private final Listener listener;

        ListenerGateway(Listener listener) {
            this.listener = listener;
        }

        @Override
        public TermuxRawSetup setupTermuxSsh(int timeoutMs) throws Exception {
            TermuxHelper.TermuxSetupResult setup = listener.onSetupTermuxSsh(timeoutMs);
            SshConfig config = setup == null ? SshConfig.defaultConfig() : setup.getConfig();
            return new TermuxRawSetup(
                    config == null ? SshConfig.defaultConfig() : config,
                    setup == null ? "" : setup.getShell(),
                    setup == null ? "" : setup.getRcPath(),
                    setup == null ? "" : setup.getOutput()
            );
        }

        @Override
        public String testConnection(SshConfig config) throws Exception {
            return listener.onTestConnection(config);
        }
    }
}
