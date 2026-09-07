package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.AiBehaviorSettings;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.model.LlmSettingsRepository;

/**
 * Compatibility wrapper around the Compose LLM settings screen.
 * Prompt Templates remains on the ScreenRegistry / typed destination bridge.
 */
public final class LLMSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onToneModeChanged(String toneMode);

        void onReasoningEffortChanged(String effort);

        void onThinkingScrollChanged(boolean enabled);

        void onThinkingAutoExpandChanged(boolean enabled);

        void onPreserveReasoningChanged(boolean enabled);

        void onLearningModeChanged(boolean enabled);

        void onSoftCompactionChanged(boolean enabled);

        void onOpenPromptTemplates();
    }

    public LLMSettingsScreenView(Context context, AiBehaviorSettings settings, Listener listener) {
        super(context);
        LlmSettingsRepository repository = new ListenerLlmSettingsRepository(settings, listener);
        addView(
                new LlmSettingsHostView(context, repository, new LlmSettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpen(LineDestination destination) {
                        if (destination instanceof LineDestination.PromptTemplates) {
                            listener.onOpenPromptTemplates();
                        }
                    }
                }),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerLlmSettingsRepository implements LlmSettingsRepository {
        private AiBehaviorSettings snapshot;
        private final Listener listener;

        ListenerLlmSettingsRepository(AiBehaviorSettings settings, Listener listener) {
            this.snapshot = settings == null
                    ? new AiBehaviorSettings(null, true, false, null, false, false)
                    : settings;
            this.listener = listener;
        }

        @Override
        public AiBehaviorSettings settings() {
            return snapshot;
        }

        @Override
        public void setToneMode(String toneMode) {
            snapshot = new AiBehaviorSettings(
                    toneMode,
                    snapshot.isThinkingScrollEnabled(),
                    snapshot.isThinkingAutoExpandEnabled(),
                    snapshot.getReasoningEffort(),
                    snapshot.isPreserveReasoningEnabled(),
                    snapshot.isLearningModeEnabled(),
                    snapshot.isSoftCompactionEnabled()
            );
            listener.onToneModeChanged(toneMode);
        }

        @Override
        public void setReasoningEffort(String effort) {
            snapshot = new AiBehaviorSettings(
                    snapshot.getToneMode(),
                    snapshot.isThinkingScrollEnabled(),
                    snapshot.isThinkingAutoExpandEnabled(),
                    effort,
                    snapshot.isPreserveReasoningEnabled(),
                    snapshot.isLearningModeEnabled(),
                    snapshot.isSoftCompactionEnabled()
            );
            listener.onReasoningEffortChanged(effort);
        }

        @Override
        public void setThinkingScrollEnabled(boolean enabled) {
            snapshot = new AiBehaviorSettings(
                    snapshot.getToneMode(),
                    enabled,
                    snapshot.isThinkingAutoExpandEnabled(),
                    snapshot.getReasoningEffort(),
                    snapshot.isPreserveReasoningEnabled(),
                    snapshot.isLearningModeEnabled(),
                    snapshot.isSoftCompactionEnabled()
            );
            listener.onThinkingScrollChanged(enabled);
        }

        @Override
        public void setThinkingAutoExpandEnabled(boolean enabled) {
            snapshot = new AiBehaviorSettings(
                    snapshot.getToneMode(),
                    snapshot.isThinkingScrollEnabled(),
                    enabled,
                    snapshot.getReasoningEffort(),
                    snapshot.isPreserveReasoningEnabled(),
                    snapshot.isLearningModeEnabled(),
                    snapshot.isSoftCompactionEnabled()
            );
            listener.onThinkingAutoExpandChanged(enabled);
        }

        @Override
        public void setPreserveReasoningEnabled(boolean enabled) {
            snapshot = new AiBehaviorSettings(
                    snapshot.getToneMode(),
                    snapshot.isThinkingScrollEnabled(),
                    snapshot.isThinkingAutoExpandEnabled(),
                    snapshot.getReasoningEffort(),
                    enabled,
                    snapshot.isLearningModeEnabled(),
                    snapshot.isSoftCompactionEnabled()
            );
            listener.onPreserveReasoningChanged(enabled);
        }

        @Override
        public void setLearningModeEnabled(boolean enabled) {
            snapshot = new AiBehaviorSettings(
                    snapshot.getToneMode(),
                    snapshot.isThinkingScrollEnabled(),
                    snapshot.isThinkingAutoExpandEnabled(),
                    snapshot.getReasoningEffort(),
                    snapshot.isPreserveReasoningEnabled(),
                    enabled,
                    snapshot.isSoftCompactionEnabled()
            );
            listener.onLearningModeChanged(enabled);
        }

        @Override
        public void setSoftCompactionEnabled(boolean enabled) {
            snapshot = new AiBehaviorSettings(
                    snapshot.getToneMode(),
                    snapshot.isThinkingScrollEnabled(),
                    snapshot.isThinkingAutoExpandEnabled(),
                    snapshot.getReasoningEffort(),
                    snapshot.isPreserveReasoningEnabled(),
                    snapshot.isLearningModeEnabled(),
                    enabled
            );
            listener.onSoftCompactionChanged(enabled);
        }
    }
}
