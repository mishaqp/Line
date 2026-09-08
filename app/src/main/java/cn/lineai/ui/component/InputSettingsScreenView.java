package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.InputSettings;
import cn.lineai.ui.model.InputSettingsRepository;

/**
 * Compatibility wrapper around the Compose Input settings screen.
 */
public final class InputSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onEnterKeyBehaviorChanged(String behavior);
    }

    public InputSettingsScreenView(Context context, InputSettings settings, Listener listener) {
        super(context);
        InputSettingsRepository repository = new ListenerInputSettingsRepository(settings, listener);
        addView(
                new InputSettingsHostView(context, repository, listener::onBack),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerInputSettingsRepository implements InputSettingsRepository {
        private InputSettings snapshot;
        private final Listener listener;

        ListenerInputSettingsRepository(InputSettings settings, Listener listener) {
            this.snapshot = settings == null ? new InputSettings(InputSettings.ENTER_SEND) : settings;
            this.listener = listener;
        }

        @Override
        public InputSettings settings() {
            return snapshot;
        }

        @Override
        public void setEnterKeyBehavior(String behavior) {
            snapshot = new InputSettings(behavior);
            listener.onEnterKeyBehaviorChanged(snapshot.getEnterKeyBehavior());
        }
    }
}
