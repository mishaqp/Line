package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.ChatScale;
import cn.lineai.model.ThemePalette;
import cn.lineai.model.ThemeSettingsState;
import cn.lineai.ui.model.ThemeSettingsRepository;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compatibility wrapper around the Compose Theme settings screen.
 */
public final class ThemeSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onThemeModeChanged(String mode);

        void onCustomThemeColorsSaved(Map<String, String> colors);

        void onChatScaleModeChanged(String mode);
    }

    public ThemeSettingsScreenView(
            Context context,
            ThemeSettingsState state,
            Listener listener
    ) {
        this(context, state, ChatScale.MODE_NORMAL, listener);
    }

    public ThemeSettingsScreenView(
            Context context,
            ThemeSettingsState state,
            String chatScaleMode,
            Listener listener
    ) {
        super(context);
        ThemeSettingsRepository repository =
                new ListenerThemeSettingsRepository(state, chatScaleMode, listener);
        addView(
                new ThemeSettingsHostView(context, repository, listener::onBack),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerThemeSettingsRepository
            implements ThemeSettingsRepository {
        private ThemeSettingsState snapshot;
        private String chatScaleMode;
        private final Listener listener;

        ListenerThemeSettingsRepository(
                ThemeSettingsState state,
                String chatScaleMode,
                Listener listener
        ) {
            this.snapshot = state == null
                    ? new ThemeSettingsState(
                            ThemePalette.MODE_SYSTEM,
                            ThemePalette.MODE_DARK,
                            null,
                            ThemePalette.forMode(ThemePalette.MODE_DARK)
                    )
                    : state;
            this.chatScaleMode = ChatScale.normalizeMode(chatScaleMode);
            this.listener = listener;
        }

        @Override
        public ThemeSettingsState themeSettings() {
            return snapshot;
        }

        @Override
        public String chatScaleMode() {
            return chatScaleMode;
        }

        @Override
        public void setThemeMode(String mode) {
            String normalized = ThemePalette.normalizeMode(mode);
            snapshot = new ThemeSettingsState(
                    normalized,
                    snapshot.getResolvedMode(),
                    snapshot.getCustomColors(),
                    snapshot.getPalette()
            );
            listener.onThemeModeChanged(normalized);
        }

        @Override
        public void saveCustomColors(Map<String, String> colors) {
            LinkedHashMap<String, String> copy = new LinkedHashMap<>(colors);
            snapshot = new ThemeSettingsState(
                    snapshot.getThemeMode(),
                    snapshot.getResolvedMode(),
                    copy,
                    snapshot.getPalette()
            );
            listener.onCustomThemeColorsSaved(copy);
        }

        @Override
        public void setChatScaleMode(String mode) {
            chatScaleMode = ChatScale.normalizeMode(mode);
            listener.onChatScaleModeChanged(chatScaleMode);
        }
    }
}
