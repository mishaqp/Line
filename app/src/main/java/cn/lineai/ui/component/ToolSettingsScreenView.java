package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.McpSettingsState;
import cn.lineai.model.WebSearchConfig;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.model.ToolSettingsRepository;

/**
 * Compatibility wrapper around the Compose Tool Settings screen.
 */
public final class ToolSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onWebSearchConfigChanged(WebSearchConfig config);

        void onOpenImageUnderstandingModelPicker();

        void onOpenImageGenerationModelPicker();

        default McpSettingsState currentMcpSettingsState() {
            return null;
        }

        default String currentImageUnderstandingModelLabel() {
            return "";
        }

        default String currentImageGenerationModelLabel() {
            return "";
        }
    }

    public ToolSettingsScreenView(
            Context context,
            McpSettingsState state,
            String imageUnderstandingModelLabel,
            String imageGenerationModelLabel,
            Listener listener
    ) {
        this(context, listener);
    }

    public ToolSettingsScreenView(Context context, Listener listener) {
        super(context);
        ToolSettingsRepository repository = new ToolSettingsControllerRepository(
                new ListenerGateway(listener)
        );
        addView(
                new ToolSettingsHostView(context, repository, new ToolSettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination instanceof LineDestination.ImageUnderstandingModel) {
                            listener.onOpenImageUnderstandingModelPicker();
                        } else if (destination instanceof LineDestination.ImageGenerationModel) {
                            listener.onOpenImageGenerationModelPicker();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerGateway implements ToolSettingsLegacyGateway {
        private final Listener listener;

        ListenerGateway(Listener listener) {
            this.listener = listener;
        }

        @Override
        public String imageUnderstandingLabel() {
            String label = listener.currentImageUnderstandingModelLabel();
            return label == null ? "" : label.trim();
        }

        @Override
        public String imageGenerationLabel() {
            String label = listener.currentImageGenerationModelLabel();
            return label == null ? "" : label.trim();
        }

        @Override
        public WebSearchConfig webSearchConfig() {
            McpSettingsState state = listener.currentMcpSettingsState();
            if (state == null || state.getWebSearchConfig() == null) {
                return WebSearchConfig.defaultConfig();
            }
            return state.getWebSearchConfig();
        }

        @Override
        public void saveWebSearchConfig(WebSearchConfig config) {
            listener.onWebSearchConfigChanged(config);
        }
    }
}
