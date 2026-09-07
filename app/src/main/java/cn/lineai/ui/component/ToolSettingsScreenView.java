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
        this(
                context,
                new SeededListener(
                        listener,
                        state,
                        imageUnderstandingModelLabel,
                        imageGenerationModelLabel
                )
        );
    }

    public ToolSettingsScreenView(Context context, Listener listener) {
        super(context);
        final Listener hostListener = listener;
        ToolSettingsRepository repository = new ToolSettingsControllerRepository(
                new ListenerGateway(hostListener)
        );
        addView(
                new ToolSettingsHostView(context, repository, new ToolSettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        hostListener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination instanceof LineDestination.ImageUnderstandingModel) {
                            hostListener.onOpenImageUnderstandingModelPicker();
                        } else if (destination instanceof LineDestination.ImageGenerationModel) {
                            hostListener.onOpenImageGenerationModelPicker();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class SeededListener implements Listener {
        private final Listener delegate;
        private McpSettingsState seededState;
        private final String seededUnderstanding;
        private final String seededGeneration;

        SeededListener(
                Listener delegate,
                McpSettingsState seededState,
                String seededUnderstanding,
                String seededGeneration
        ) {
            this.delegate = delegate;
            this.seededState = seededState;
            this.seededUnderstanding = seededUnderstanding == null ? "" : seededUnderstanding.trim();
            this.seededGeneration = seededGeneration == null ? "" : seededGeneration.trim();
        }

        @Override
        public void onBack() {
            delegate.onBack();
        }

        @Override
        public void onWebSearchConfigChanged(WebSearchConfig config) {
            delegate.onWebSearchConfigChanged(config);
        }

        @Override
        public void onOpenImageUnderstandingModelPicker() {
            delegate.onOpenImageUnderstandingModelPicker();
        }

        @Override
        public void onOpenImageGenerationModelPicker() {
            delegate.onOpenImageGenerationModelPicker();
        }

        @Override
        public McpSettingsState currentMcpSettingsState() {
            McpSettingsState live = delegate.currentMcpSettingsState();
            return live != null ? live : seededState;
        }

        @Override
        public String currentImageUnderstandingModelLabel() {
            String live = delegate.currentImageUnderstandingModelLabel();
            if (live != null && live.trim().length() > 0) {
                return live.trim();
            }
            return seededUnderstanding;
        }

        @Override
        public String currentImageGenerationModelLabel() {
            String live = delegate.currentImageGenerationModelLabel();
            if (live != null && live.trim().length() > 0) {
                return live.trim();
            }
            return seededGeneration;
        }
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
