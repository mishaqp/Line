package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.McpSettingsState;
import cn.lineai.model.McpToolConfig;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.model.McpSettingsRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility wrapper around the Compose Tools & execution screen.
 */
public final class MCPSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onExecutionModeChanged(String mode);

        void onToolGroupChanged(String id, boolean enabled);

        void onOpenSshSettings();

        void onOpenTermuxIntegration();
    }

    public MCPSettingsScreenView(Context context, McpSettingsState state, Listener listener) {
        super(context);
        McpSettingsRepository repository = new McpSettingsControllerRepository(
                new ListenerGateway(state, listener)
        );
        addView(
                new McpSettingsHostView(context, repository, new McpSettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination instanceof LineDestination.SshSettings) {
                            listener.onOpenSshSettings();
                        } else if (destination instanceof LineDestination.TermuxIntegration) {
                            listener.onOpenTermuxIntegration();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerGateway implements McpSettingsLegacyGateway {
        private final Listener listener;
        private McpSettingsState snapshot;

        ListenerGateway(McpSettingsState state, Listener listener) {
            this.snapshot = state == null ? new McpSettingsState("local", null) : state;
            this.listener = listener;
        }

        @Override
        public McpSettingsState mcpSettingsState() {
            return snapshot;
        }

        @Override
        public void setExecutionMode(String mode) {
            listener.onExecutionModeChanged(mode);
            snapshot = new McpSettingsState(
                    mode,
                    snapshot.getConfigs(),
                    snapshot.getWebSearchConfig(),
                    snapshot.getImageUnderstandingModelId(),
                    snapshot.getImageGenerationModelId()
            );
        }

        @Override
        public void setToolGroupEnabled(String id, boolean enabled) {
            listener.onToolGroupChanged(id, enabled);
            List<McpToolConfig> configs = new ArrayList<>();
            for (McpToolConfig config : snapshot.getConfigs()) {
                if (id.equals(config.getId())) {
                    configs.add(new McpToolConfig(
                            config.getId(),
                            config.getName(),
                            config.getDescription(),
                            enabled,
                            config.getTools(),
                            config.getSupportedExecutionModes(),
                            config.getIconKey()
                    ));
                } else {
                    configs.add(config);
                }
            }
            snapshot = new McpSettingsState(
                    snapshot.getExecutionMode(),
                    configs,
                    snapshot.getWebSearchConfig(),
                    snapshot.getImageUnderstandingModelId(),
                    snapshot.getImageGenerationModelId()
            );
        }
    }
}
