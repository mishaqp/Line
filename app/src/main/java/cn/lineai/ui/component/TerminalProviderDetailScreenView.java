package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.ipc.IpcProviderConfig;
import cn.lineai.ipc.ScannedProvider;
import cn.lineai.ui.model.TerminalProvidersSettingsRepository;
import java.util.List;

public final class TerminalProviderDetailScreenView extends FrameLayout {
    /** Source-compatible legacy listener kept for the registered fallback factory. */
    public interface Listener {
        void onBack();

        void onScanProviders();

        void onProviderAddConfirmed(IpcProviderConfig config);

        void onEnabledChanged(String id, boolean enabled);

        void onDelete(String id);
    }

    private final TerminalProvidersHostView hostView;
    private boolean attachedOnce;

    public TerminalProviderDetailScreenView(
            Context context,
            TerminalProvidersSettingsRepository repository,
            Runnable onBack
    ) {
        super(context);
        hostView = new TerminalProvidersHostView(
                context,
                repository,
                new TerminalProvidersHostView.Listener() {
                    @Override
                    public void onBack() {
                        onBack.run();
                    }
                }
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    /**
     * Compatibility constructor for the old ScreenFactories registration.
     * Normal typed navigation is intercepted by ScreenRegistry and uses live
     * controller reads through TerminalProvidersLegacyBridge.
     */
    public TerminalProviderDetailScreenView(
            Context context,
            List<ScannedProvider> scanResults,
            List<IpcProviderConfig> installed,
            boolean hasScanned,
            Listener listener
    ) {
        this(
                context,
                new TerminalProvidersControllerRepository(new TerminalProvidersLegacyGateway() {
                    @Override
                    public List<IpcProviderConfig> installedProviders() {
                        return installed;
                    }

                    @Override
                    public List<ScannedProvider> scanResults() {
                        return scanResults;
                    }

                    @Override
                    public boolean hasScanned() {
                        return hasScanned;
                    }

                    @Override
                    public void scanProviders() {
                        listener.onScanProviders();
                    }

                    @Override
                    public void saveProvider(IpcProviderConfig config) {
                        listener.onProviderAddConfirmed(config);
                    }

                    @Override
                    public void setProviderEnabled(String providerId, boolean enabled) {
                        listener.onEnabledChanged(providerId, enabled);
                    }

                    @Override
                    public void deleteProvider(String providerId) {
                        listener.onDelete(providerId);
                    }
                }),
                listener::onBack
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (attachedOnce) {
            hostView.refresh();
            return;
        }
        attachedOnce = true;
    }
}
